"use client";

import { startTransition, useCallback, useEffect, useMemo, useRef, useState } from "react";

import type {
  ChatMessage,
  ExamOrder,
  Hl7MessageLog,
  MedicalDevice,
  MemberPresence,
  Patient,
  PatientDetail,
  RegisterObservationResultResponse,
  SocketEnvelope,
} from "@/features/console/model/carebridge";
import {
  WS_BASE_URL,
  getPatient,
  listChatMessages,
  listDevices,
  listExamOrders,
  listHl7Logs,
  listPatients,
  login,
  logout,
  me,
  refreshSession,
  sendChatMessage,
  sendRawHl7,
  simulateHl7,
} from "@/features/console/repository/carebridge-api";

const STORAGE_KEY = "carebridge.access-token";
const SAMPLE_HL7 = `MSH|^~\\&|ECG-001|DEVICE|EMR|HOSPITAL|20260514103000||ORU^R01|MSG00001|P|2.5
PID|||P0001||HONG^GILDONG||19800101|M
OBR|1|ORD-001||ECG^Electrocardiogram
OBX|1|NM|HR^Heart Rate||78|bpm|60-100|N|||F`;

export type MenuKey =
  | "work-board"
  | "chat"
  | "patients"
  | "orders"
  | "results"
  | "hl7-logs"
  | "devices"
  | "simulator";

export function useCarebridgeConsole() {
  const [token, setToken] = useState<string | null>(null);
  const [currentUser, setCurrentUser] = useState<MemberPresence | null>(null);
  const [loginForm, setLoginForm] = useState({ username: "operator", password: "Operator1234!" });
  const [activeMenu, setActiveMenu] = useState<MenuKey>("patients");
  const [patients, setPatients] = useState<Patient[]>([]);
  const [orders, setOrders] = useState<ExamOrder[]>([]);
  const [patientDetail, setPatientDetail] = useState<PatientDetail | null>(null);
  const [selectedPatientNo, setSelectedPatientNo] = useState("P0001");
  const [hl7Logs, setHl7Logs] = useState<Hl7MessageLog[]>([]);
  const [devices, setDevices] = useState<MedicalDevice[]>([]);
  const [rawHl7, setRawHl7] = useState(SAMPLE_HL7);
  const [lastResponse, setLastResponse] = useState<RegisterObservationResultResponse | null>(null);
  const [socketState, setSocketState] = useState<"DISCONNECTED" | "CONNECTING" | "CONNECTED">("DISCONNECTED");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [chatSending, setChatSending] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);

  const simulatorForm = useMemo(() => ({
    messageControlId: `MSG${Date.now().toString().slice(-6)}`,
    deviceCode: "ECG-001",
    patientNo: selectedPatientNo || "P0001",
    orderNo: "ORD-001",
    examCode: "ECG",
    examName: "Electrocardiogram",
    observationCode: "HR",
    observationName: "Heart Rate",
    value: "78",
    unit: "bpm",
    referenceRange: "60-100",
    abnormalFlag: "N",
  }), [selectedPatientNo]);

  const refreshConsole = useCallback(async (accessToken = token, patientNo = selectedPatientNo) => {
    if (!accessToken) return;
    const [patientsResponse, ordersResponse, logsResponse, devicesResponse] = await Promise.all([
      listPatients(accessToken),
      listExamOrders(accessToken),
      listHl7Logs(accessToken),
      listDevices(accessToken),
    ]);
    const nextPatientNo = patientNo || patientsResponse[0]?.patientNo || "P0001";
    const detailResponse = nextPatientNo ? await getPatient(accessToken, nextPatientNo) : null;
    startTransition(() => {
      setPatients(patientsResponse);
      setOrders(ordersResponse);
      setHl7Logs(logsResponse);
      setDevices(devicesResponse);
      setSelectedPatientNo(nextPatientNo);
      setPatientDetail(detailResponse);
      setError(null);
    });
  }, [selectedPatientNo, token]);

  useEffect(() => {
    const savedToken = window.localStorage.getItem(STORAGE_KEY);
    if (savedToken) setToken(savedToken);
  }, []);

  useEffect(() => {
    if (token) window.localStorage.setItem(STORAGE_KEY, token);
    else {
      window.localStorage.removeItem(STORAGE_KEY);
      setChatMessages([]);
    }
  }, [token]);

  useEffect(() => {
    if (!token) {
      setCurrentUser(null);
      return;
    }
    let cancelled = false;
    void (async () => {
      try {
        const user = await me(token);
        if (cancelled) return;
        setCurrentUser(user);
        await refreshConsole(token);
        setError(null);
      } catch (loadError) {
        if (cancelled) return;
        setError(loadError instanceof Error ? loadError.message : "세션을 불러오지 못했습니다.");
        setToken(null);
        setCurrentUser(null);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [token, refreshConsole]);

  useEffect(() => {
    if (!token || !currentUser) return;
    const socket = new WebSocket(`${WS_BASE_URL}?token=${encodeURIComponent(token)}`);
    wsRef.current = socket;
    setSocketState("CONNECTING");
    socket.onopen = () => setSocketState("CONNECTED");
    socket.onclose = () => setSocketState("DISCONNECTED");
    socket.onerror = () => setSocketState("DISCONNECTED");
    socket.onmessage = (event) => {
      const envelope = JSON.parse(event.data) as SocketEnvelope;
      if (envelope.type === "HL7_MESSAGE") {
        const log = envelope.payload as Hl7MessageLog;
        setHl7Logs((current) => [log, ...current.filter((item) => item.messageControlId !== log.messageControlId)].slice(0, 50));
        return;
      }
      if (envelope.type === "CHAT_MESSAGE") {
        const message = envelope.payload as ChatMessage;
        setChatMessages((current) => {
          if (current.some((item) => item.id === message.id)) {
            return current;
          }
          return [...current, message];
        });
      }
    };
    return () => socket.close();
  }, [token, currentUser]);

  useEffect(() => {
    if (!token || activeMenu !== "chat") return;
    let cancelled = false;
    void listChatMessages(token, 0)
      .then((rows) => {
        if (!cancelled) setChatMessages(rows);
      })
      .catch((loadError) => {
        if (!cancelled) {
          setError(loadError instanceof Error ? loadError.message : "채팅을 불러오지 못했습니다.");
        }
      });
    return () => {
      cancelled = true;
    };
  }, [token, activeMenu]);

  useEffect(() => {
    if (!token || !currentUser) return;
    const intervalId = window.setInterval(() => {
      void refreshSession(token)
        .then((result) => setToken(result.accessToken))
        .catch(() => {
          setToken(null);
          setCurrentUser(null);
        });
    }, 10 * 60 * 1000);
    return () => window.clearInterval(intervalId);
  }, [token, currentUser]);

  const submitLogin = async () => {
    setBusy(true);
    setError(null);
    try {
      const response = await login(loginForm.username, loginForm.password);
      setToken(response.accessToken);
      setCurrentUser(response.user);
    } catch (loginError) {
      setError(loginError instanceof Error ? loginError.message : "로그인에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const signOut = async () => {
    const activeToken = token;
    setToken(null);
    setCurrentUser(null);
    if (activeToken) await logout(activeToken).catch(() => undefined);
  };

  const selectPatient = async (patientNo: string) => {
    if (!token) return;
    setSelectedPatientNo(patientNo);
    setPatientDetail(await getPatient(token, patientNo));
  };

  const submitRawHl7 = async () => {
    if (!token) return;
    setBusy(true);
    setError(null);
    try {
      const response = await sendRawHl7(token, rawHl7);
      setLastResponse(response);
      await refreshConsole(token, response.patientNo ?? selectedPatientNo);
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "HL7 전송에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const submitSimulation = async () => {
    if (!token) return;
    setBusy(true);
    setError(null);
    try {
      const response = await simulateHl7(token, simulatorForm);
      setLastResponse(response);
      await refreshConsole(token, response.patientNo ?? selectedPatientNo);
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "시뮬레이션에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const resetToHome = useCallback(() => {
    startTransition(() => {
      setActiveMenu("patients");
      setError(null);
    });
  }, []);

  const sendChat = useCallback(
    async (content: string) => {
      if (!token) return;
      const ws = wsRef.current;
      if (ws?.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: "CHAT", content }));
        return;
      }
      setChatSending(true);
      setError(null);
      try {
        const message = await sendChatMessage(token, content);
        setChatMessages((current) => (current.some((item) => item.id === message.id) ? current : [...current, message]));
      } catch (sendError) {
        setError(sendError instanceof Error ? sendError.message : "메시지 전송에 실패했습니다.");
      } finally {
        setChatSending(false);
      }
    },
    [token],
  );

  return {
    activeMenu,
    busy,
    chatMessages,
    chatSending,
    currentUser,
    devices,
    error,
    hl7Logs,
    lastResponse,
    loginForm,
    orders,
    patientDetail,
    patients,
    rawHl7,
    selectedPatientNo,
    simulatorForm,
    socketState,
    token,
    refreshConsole,
    resetToHome,
    selectPatient,
    setActiveMenu,
    setLoginForm,
    setRawHl7,
    signOut,
    sendChat,
    submitLogin,
    submitRawHl7,
    submitSimulation,
  };
}
