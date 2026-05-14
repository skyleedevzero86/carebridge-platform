import type {
  AuthResponse,
  ExamOrder,
  Hl7MessageLog,
  MedicalDevice,
  MemberPresence,
  Patient,
  PatientDetail,
  RegisterObservationResultResponse,
} from "@/features/console/model/carebridge";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
export const WS_BASE_URL = process.env.NEXT_PUBLIC_WS_BASE_URL ?? "ws://localhost:8080/ws/chat";

async function parseErrorMessage(response: Response, fallback: string) {
  const body = await response.json().catch(() => ({}));
  return (body as { message?: string }).message ?? fallback;
}

function authHeaders(token: string, contentType = "application/json"): HeadersInit {
  return {
    Authorization: `Bearer ${token}`,
    "Content-Type": contentType,
  };
}

export async function login(username: string, password: string): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  if (!response.ok) throw new Error(await parseErrorMessage(response, "로그인에 실패했습니다."));
  return response.json();
}

export async function logout(token: string): Promise<void> {
  await fetch(`${API_BASE_URL}/api/auth/logout`, { method: "POST", headers: authHeaders(token) });
}

export async function me(token: string): Promise<MemberPresence> {
  const response = await fetch(`${API_BASE_URL}/api/auth/me`, { cache: "no-store", headers: authHeaders(token) });
  if (!response.ok) throw new Error(await parseErrorMessage(response, "사용자 정보를 불러오지 못했습니다."));
  return response.json();
}

export type RefreshSessionResult = { accessToken: string; refreshed: boolean };

export async function refreshSession(token: string): Promise<RefreshSessionResult> {
  const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, { method: "POST", headers: authHeaders(token) });
  if (!response.ok) throw new Error(await parseErrorMessage(response, "세션을 갱신하지 못했습니다."));
  return response.json();
}

export async function listPatients(token: string): Promise<Patient[]> {
  const response = await fetch(`${API_BASE_URL}/api/patients`, { cache: "no-store", headers: authHeaders(token) });
  if (!response.ok) throw new Error(await parseErrorMessage(response, "환자 목록을 불러오지 못했습니다."));
  return response.json();
}

export async function getPatient(token: string, patientNo: string): Promise<PatientDetail> {
  const response = await fetch(`${API_BASE_URL}/api/patients/${patientNo}`, { cache: "no-store", headers: authHeaders(token) });
  if (!response.ok) throw new Error(await parseErrorMessage(response, "환자 상세를 불러오지 못했습니다."));
  return response.json();
}

export async function listExamOrders(token: string): Promise<ExamOrder[]> {
  const response = await fetch(`${API_BASE_URL}/api/exam-orders`, { cache: "no-store", headers: authHeaders(token) });
  if (!response.ok) throw new Error(await parseErrorMessage(response, "검사 오더를 불러오지 못했습니다."));
  return response.json();
}

export async function listHl7Logs(token: string): Promise<Hl7MessageLog[]> {
  const response = await fetch(`${API_BASE_URL}/api/interface/hl7/messages`, { cache: "no-store", headers: authHeaders(token) });
  if (!response.ok) throw new Error(await parseErrorMessage(response, "HL7 로그를 불러오지 못했습니다."));
  return response.json();
}

export async function listDevices(token: string): Promise<MedicalDevice[]> {
  const response = await fetch(`${API_BASE_URL}/api/devices`, { cache: "no-store", headers: authHeaders(token) });
  if (!response.ok) throw new Error(await parseErrorMessage(response, "의료기기 목록을 불러오지 못했습니다."));
  return response.json();
}

export async function sendRawHl7(token: string, rawMessage: string): Promise<RegisterObservationResultResponse> {
  const response = await fetch(`${API_BASE_URL}/api/interface/hl7/messages`, {
    method: "POST",
    headers: authHeaders(token, "text/plain"),
    body: rawMessage,
  });
  if (!response.ok) throw new Error(await parseErrorMessage(response, "HL7 수신 처리에 실패했습니다."));
  return response.json();
}

export async function simulateHl7(token: string, payload: Record<string, string>): Promise<RegisterObservationResultResponse> {
  const response = await fetch(`${API_BASE_URL}/api/device-interface/simulate/hl7`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(payload),
  });
  if (!response.ok) throw new Error(await parseErrorMessage(response, "HL7 시뮬레이션에 실패했습니다."));
  return response.json();
}
