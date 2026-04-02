import type {
  AuthResponse,
  ChatMessage,
  DeviceEvent,
  DeviceOverview,
  MemberPresence,
} from "@/features/console/model/carebridge";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
export const WS_BASE_URL = process.env.NEXT_PUBLIC_WS_BASE_URL ?? "ws://localhost:8080/ws/chat";

type RefreshResponse = {
  accessToken: string;
  refreshed: boolean;
};

async function parseErrorMessage(response: Response, fallback: string) {
  const body = await response.json().catch(() => ({}));
  return (body as { message?: string }).message ?? fallback;
}

function authHeaders(token: string): HeadersInit {
  return {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
  };
}

export async function login(username: string, password: string): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });

  if (!response.ok) {
    throw new Error(await parseErrorMessage(response, "로그인에 실패했습니다."));
  }

  return response.json();
}

export async function register(username: string, displayName: string, password: string): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/api/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, displayName, password }),
  });

  if (!response.ok) {
    throw new Error(await parseErrorMessage(response, "회원가입에 실패했습니다."));
  }

  return response.json();
}

export async function me(token: string): Promise<MemberPresence> {
  const response = await fetch(`${API_BASE_URL}/api/auth/me`, {
    cache: "no-store",
    headers: authHeaders(token),
  });

  if (!response.ok) {
    throw new Error(await parseErrorMessage(response, "내 정보를 불러오지 못했습니다."));
  }

  return response.json();
}

export async function logout(token: string): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/auth/logout`, {
    method: "POST",
    headers: authHeaders(token),
  });

  if (!response.ok) {
    throw new Error(await parseErrorMessage(response, "로그아웃에 실패했습니다."));
  }
}

export async function refreshToken(token: string): Promise<RefreshResponse> {
  const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    throw new Error(await parseErrorMessage(response, "토큰 갱신에 실패했습니다."));
  }

  return response.json();
}

export async function listPresence(token: string): Promise<MemberPresence[]> {
  const response = await fetch(`${API_BASE_URL}/api/users/presence`, {
    cache: "no-store",
    headers: authHeaders(token),
  });

  if (!response.ok) {
    throw new Error(await parseErrorMessage(response, "접속 현황을 불러오지 못했습니다."));
  }

  return response.json();
}

export async function pingPresence(token: string): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/users/presence/ping`, {
    method: "POST",
    headers: authHeaders(token),
  });

  if (!response.ok) {
    throw new Error(await parseErrorMessage(response, "접속 상태 갱신에 실패했습니다."));
  }
}

export async function recentMessages(token: string): Promise<ChatMessage[]> {
  const response = await fetch(`${API_BASE_URL}/api/chat/messages`, {
    cache: "no-store",
    headers: authHeaders(token),
  });

  if (!response.ok) {
    throw new Error(await parseErrorMessage(response, "채팅 메시지를 불러오지 못했습니다."));
  }

  return response.json();
}

export async function sendChatMessage(token: string, content: string): Promise<ChatMessage> {
  const response = await fetch(`${API_BASE_URL}/api/chat/messages`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({ content }),
  });

  if (!response.ok) {
    throw new Error(await parseErrorMessage(response, "채팅 메시지 전송에 실패했습니다."));
  }

  return response.json();
}

export async function recentDeviceEvents(token: string): Promise<DeviceEvent[]> {
  const response = await fetch(`${API_BASE_URL}/api/device-interface/events`, {
    cache: "no-store",
    headers: authHeaders(token),
  });

  if (!response.ok) {
    throw new Error(await parseErrorMessage(response, "장비 이벤트를 불러오지 못했습니다."));
  }

  return response.json();
}

export async function deviceOverview(token: string): Promise<DeviceOverview> {
  const response = await fetch(`${API_BASE_URL}/api/device-interface/overview`, {
    cache: "no-store",
    headers: authHeaders(token),
  });

  if (!response.ok) {
    throw new Error(await parseErrorMessage(response, "장비 요약 정보를 불러오지 못했습니다."));
  }

  return response.json();
}

export async function simulateDeviceEvent(token: string, payload: string): Promise<DeviceEvent> {
  const response = await fetch(`${API_BASE_URL}/api/device-interface/simulate`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({ payload }),
  });

  if (!response.ok) {
    throw new Error(await parseErrorMessage(response, "장비 이벤트 생성에 실패했습니다."));
  }

  return response.json();
}
