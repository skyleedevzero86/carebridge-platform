export type UserRole = "ADMIN" | "OPERATOR";

export type MemberPresence = {
  id: string;
  username: string;
  displayName: string;
  role: UserRole;
  online: number;
};

export type AuthResponse = {
  accessToken: string;
  user: MemberPresence;
};

export type ChatMessage = {
  id: string;
  senderId: string;
  senderName: string;
  senderRole: UserRole;
  content: string;
  sentAt: string;
};

export type DeviceEvent = {
  id: string;
  deviceCode: string;
  protocol: "KEY_VALUE" | "HL7";
  patientCode: string | null;
  summary: string;
  payload: string;
  sourceIp: string;
  ackCode: string;
  receivedAt: string;
};

export type DeviceOverview = {
  tcpPort: number;
  totalMessages: number;
  lastReceivedAt: string | null;
  simulatorEnabled: boolean;
  simulatorIntervalMillis: number;
};

export type SocketEnvelope = {
  type: "CONNECTED" | "PRESENCE_SNAPSHOT" | "CHAT_MESSAGE" | "DEVICE_EVENT" | "PONG" | "ERROR";
  payload: unknown;
};