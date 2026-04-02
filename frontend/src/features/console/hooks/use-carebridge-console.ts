"use client";

import { startTransition, useEffect, useEffectEvent, useMemo, useRef, useState } from "react";

import type { AuthResponse, ChatMessage, DeviceEvent, DeviceOverview, MemberPresence, SocketEnvelope } from "@/features/console/model/carebridge";
import {
  WS_BASE_URL,
  deviceOverview,
  listPresence,
  login,
  logout,
  me,
  pingPresence,
  recentDeviceEvents,
  recentMessages,
  refreshToken,
  register,
  sendChatMessage,
  simulateDeviceEvent,
} from "@/features/console/repository/carebridge-api";

const STORAGE_KEY = "carebridge.access-token";
const DEFAULT_SIMULATION_PAYLOAD = "DEVICE=XRAY-01|PATIENT=P-1004|RESULT=NORMAL|STATUS=READY";

type AuthMode = "login" | "register";
type SocketState = "DISCONNECTED" | "CONNECTING" | "CONNECTED";

type LoginForm = {
  username: string;
  password: string;
};

type RegisterForm = {
  username: string;
  displayName: string;
  password: string;
};

function sortMembers(members: MemberPresence[]) {
  return [...members].sort((left, right) => {
    if (left.online !== right.online) {
      return right.online - left.online;
    }

    return left.displayName.localeCompare(right.displayName, "ko");
  });
}

export function useCarebridgeConsole() {
  const [isHydrated, setIsHydrated] = useState(false);
  const [token, setToken] = useState<string | null>(null);
  const [authMode, setAuthMode] = useState<AuthMode>("login");
  const [socketState, setSocketState] = useState<SocketState>("DISCONNECTED");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [currentUser, setCurrentUser] = useState<MemberPresence | null>(null);
  const [members, setMembers] = useState<MemberPresence[]>([]);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [deviceEvents, setDeviceEvents] = useState<DeviceEvent[]>([]);
  const [overview, setOverview] = useState<DeviceOverview | null>(null);
  const [chatDraft, setChatDraft] = useState("");
  const [simulationPayload, setSimulationPayload] = useState(DEFAULT_SIMULATION_PAYLOAD);
  const [loginForm, setLoginForm] = useState<LoginForm>({ username: "operator", password: "Operator1234!" });
  const [registerForm, setRegisterForm] = useState<RegisterForm>({ username: "", displayName: "", password: "" });
  const [socketReconnectTick, setSocketReconnectTick] = useState(0);

  const wsRef = useRef<WebSocket | null>(null);

  const connectedCount = useMemo(() => members.filter((member) => member.online === 1).length, [members]);

  const applyAuth = (result: AuthResponse) => {
    setToken(result.accessToken);
    startTransition(() => {
      setCurrentUser(result.user);
      setMembers((current) => sortMembers([result.user, ...current.filter((member) => member.id !== result.user.id)]));
    });
  };

  const bootstrapEffect = useEffectEvent(async (accessToken: string) => {
    const [meResponse, presenceResponse, messagesResponse, overviewResponse, deviceEventsResponse] = await Promise.all([
      me(accessToken),
      listPresence(accessToken),
      recentMessages(accessToken),
      deviceOverview(accessToken),
      recentDeviceEvents(accessToken),
    ]);

    startTransition(() => {
      setCurrentUser(meResponse);
      setMembers(sortMembers(presenceResponse));
      setMessages(messagesResponse);
      setOverview(overviewResponse);
      setDeviceEvents(deviceEventsResponse);
      setError(null);
    });
  });

  const refreshConsoleEffect = useEffectEvent(async () => {
    if (!token) {
      return;
    }

    try {
      const [presenceResponse, overviewResponse, deviceEventsResponse] = await Promise.all([
        listPresence(token),
        deviceOverview(token),
        recentDeviceEvents(token),
      ]);

      startTransition(() => {
        setMembers(sortMembers(presenceResponse));
        setOverview(overviewResponse);
        setDeviceEvents(deviceEventsResponse);
      });
    } catch (refreshError) {
      setError(refreshError instanceof Error ? refreshError.message : "콘솔 새로고침에 실패했습니다.");
    }
  });

  const handleSocketEnvelopeEffect = useEffectEvent((envelope: SocketEnvelope) => {
    if (envelope.type === "PRESENCE_SNAPSHOT") {
      startTransition(() => {
        setMembers(sortMembers(envelope.payload as MemberPresence[]));
      });
      return;
    }

    if (envelope.type === "CHAT_MESSAGE") {
      startTransition(() => {
        setMessages((current) => [...current, envelope.payload as ChatMessage].slice(-50));
      });
      return;
    }

    if (envelope.type === "DEVICE_EVENT") {
      const nextEvent = envelope.payload as DeviceEvent;
      startTransition(() => {
        setDeviceEvents((current) => [nextEvent, ...current.filter((item) => item.id !== nextEvent.id)].slice(0, 25));
        setOverview((current) => current ? {
          ...current,
          totalMessages: current.totalMessages + 1,
          lastReceivedAt: nextEvent.receivedAt,
        } : current);
      });
      return;
    }

    if (envelope.type === "ERROR") {
      const payload = envelope.payload as { message?: string };
      setError(payload.message ?? "실시간 메시지 처리 중 오류가 발생했습니다.");
    }
  });

  useEffect(() => {
    const savedToken = window.localStorage.getItem(STORAGE_KEY);
    if (savedToken) {
      setToken(savedToken);
    }
    setIsHydrated(true);
  }, []);

  useEffect(() => {
    if (!isHydrated) {
      return;
    }

    if (token) {
      window.localStorage.setItem(STORAGE_KEY, token);
      return;
    }

    window.localStorage.removeItem(STORAGE_KEY);
  }, [isHydrated, token]);

  useEffect(() => {
    if (!token) {
      startTransition(() => {
        setCurrentUser(null);
        setMembers([]);
        setMessages([]);
        setDeviceEvents([]);
        setOverview(null);
        setSocketState("DISCONNECTED");
      });
      return;
    }

    void bootstrapEffect(token).catch((bootstrapError) => {
      setToken(null);
      setError(bootstrapError instanceof Error ? bootstrapError.message : "초기 데이터를 불러오지 못했습니다.");
    });
  }, [token]);

  
  useEffect(() => {
    if (!token) {
      return;
    }

    const intervalId = window.setInterval(() => {
      void pingPresence(token).catch(() => undefined);
      void refreshConsoleEffect();
    }, 20000);

    return () => window.clearInterval(intervalId);
  }, [token]);

  
  useEffect(() => {
    if (!token) {
      return;
    }

    const REFRESH_INTERVAL_MS = 10 * 60 * 1000; 

    const intervalId = window.setInterval(async () => {
      try {
        const result = await refreshToken(token);
        if (result.refreshed) {
          setToken(result.accessToken);
        }
      } catch (error) {
      }
    }, REFRESH_INTERVAL_MS);

    return () => window.clearInterval(intervalId);
  }, [token]);

  useEffect(() => {
    if (!token) {
      return;
    }

    const intervalId = window.setInterval(async () => {
      try {
        const latest = await recentMessages(token);
        startTransition(() => {
          setMessages(latest);
        });
      } catch {
      }
    }, 3000);

    return () => window.clearInterval(intervalId);
  }, [token]);

  useEffect(() => {
    if (!token) {
      return;
    }

    let closedByCleanup = false;
    let reconnectTimer: number | null = null;
    const socket = new WebSocket(`${WS_BASE_URL}?token=${encodeURIComponent(token)}`);
    wsRef.current = socket;
    setSocketState("CONNECTING");

    const sendPing = () => {
      if (socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ type: "PING", content: "" }));
      }
    };

    const intervalId = window.setInterval(sendPing, 20000);

    socket.onopen = () => {
      setSocketState("CONNECTED");
      sendPing();
    };

    socket.onmessage = (event) => {
      try {
        handleSocketEnvelopeEffect(JSON.parse(event.data) as SocketEnvelope);
      } catch {
        setError("실시간 메시지를 해석하지 못했습니다.");
      }
    };

    socket.onerror = () => {
      setSocketState("DISCONNECTED");
    };

    socket.onclose = () => {
      setSocketState("DISCONNECTED");
      if (closedByCleanup) {
        return;
      }
      reconnectTimer = window.setTimeout(() => {
        setSocketReconnectTick((current) => current + 1);
      }, 1500);
    };

    return () => {
      closedByCleanup = true;
      window.clearInterval(intervalId);
      if (reconnectTimer !== null) {
        window.clearTimeout(reconnectTimer);
      }
      if (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING) {
        socket.close();
      }
      if (wsRef.current === socket) {
        wsRef.current = null;
      }
    };
  }, [token, socketReconnectTick]);

  const submitLogin = async () => {
    setBusy(true);
    setError(null);

    try {
      const result = await login(loginForm.username, loginForm.password);
      applyAuth(result);
    } catch (loginError) {
      setError(loginError instanceof Error ? loginError.message : "로그인에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const submitRegister = async () => {
    setBusy(true);
    setError(null);

    try {
      const result = await register(registerForm.username, registerForm.displayName, registerForm.password);
      applyAuth(result);
    } catch (registerError) {
      setError(registerError instanceof Error ? registerError.message : "회원가입에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const submitChat = async () => {
    if (!chatDraft.trim()) {
      return;
    }

    if (!token || !currentUser) {
      setError("채팅 전송에 필요한 인증 정보가 없습니다.");
      return;
    }

    const content = chatDraft.trim();
    const socket = wsRef.current;

    if (socket && socket.readyState === WebSocket.OPEN) {
      try {
        socket.send(JSON.stringify({ type: "CHAT", content }));
        setChatDraft("");
        setError(null);
        return;
      } catch {
      }
    }

    try {
      const saved = await sendChatMessage(token, content);
      startTransition(() => {
        setMessages((current) => [...current, saved].slice(-50));
      });
      setChatDraft("");
      setError(null);
    } catch (chatError) {
      setError(chatError instanceof Error ? chatError.message : "채팅 메시지 전송에 실패했습니다.");
    }
  };

  const submitSimulation = async () => {
    if (!token) {
      return;
    }

    setBusy(true);
    setError(null);

    try {
      const created = await simulateDeviceEvent(token, simulationPayload);
      startTransition(() => {
        setDeviceEvents((current) => [created, ...current.filter((event) => event.id !== created.id)].slice(0, 25));
        setOverview((current) => current ? {
          ...current,
          totalMessages: current.totalMessages + 1,
          lastReceivedAt: created.receivedAt,
        } : current);
      });
    } catch (simulationError) {
      setError(simulationError instanceof Error ? simulationError.message : "장비 이벤트 생성에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const signOut = async () => {
    const activeToken = token;
    setToken(null);
    setCurrentUser(null);
    setMembers([]);
    setMessages([]);
    setDeviceEvents([]);
    setOverview(null);
    setSocketState("DISCONNECTED");

    if (activeToken) {
      await logout(activeToken).catch(() => undefined);
    }
  };

  const refreshConsole = async () => {
    if (!token) {
      return;
    }

    try {
      const [presenceResponse, overviewResponse, deviceEventsResponse] = await Promise.all([
        listPresence(token),
        deviceOverview(token),
        recentDeviceEvents(token),
      ]);

      startTransition(() => {
        setMembers(sortMembers(presenceResponse));
        setOverview(overviewResponse);
        setDeviceEvents(deviceEventsResponse);
      });
    } catch (refreshError) {
      setError(refreshError instanceof Error ? refreshError.message : "콘솔 새로고침에 실패했습니다.");
    }
  };

  return {
    authMode,
    busy,
    chatDraft,
    connectedCount,
    currentUser,
    deviceEvents,
    error,
    loginForm,
    members,
    messages,
    overview,
    registerForm,
    simulationPayload,
    socketState,
    token,
    setAuthMode,
    setChatDraft,
    setLoginForm,
    setRegisterForm,
    setSimulationPayload,
    signOut,
    submitChat,
    submitLogin,
    submitRegister,
    submitSimulation,
    refreshConsole,
  };
}