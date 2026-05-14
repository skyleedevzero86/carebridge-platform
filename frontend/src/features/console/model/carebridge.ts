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

export type Patient = {
  patientNo: string;
  name: string;
  birthDate: string;
  gender: string;
  createdAt: string;
  recentResultCount: number;
  lastReceivedAt: string | null;
};

export type ExamOrder = {
  orderNo: string;
  patientNo: string;
  examCode: string;
  examName: string;
  status: "ORDERED" | "COMPLETED" | "CANCELED" | "FAILED";
  orderedAt: string;
  completedAt: string | null;
};

export type ObservationResult = {
  id: string;
  patientNo: string;
  orderNo: string;
  messageControlId: string;
  deviceCode: string;
  observationCode: string;
  observationName: string;
  value: string;
  unit: string;
  referenceRange: string;
  abnormalFlag: string;
  resultStatus: string;
  observedAt: string;
  createdAt: string;
};

export type PatientDetail = {
  patient: Patient;
  examOrders: ExamOrder[];
  observationResults: ObservationResult[];
};

export type Hl7MessageLog = {
  messageControlId: string;
  messageType: string;
  deviceCode: string;
  patientNo: string | null;
  orderNo: string | null;
  rawMessage: string;
  parsedMessageJson: string | null;
  processStatus: "SUCCESS" | "FAILED" | "DUPLICATE";
  errorCode: string | null;
  errorMessage: string | null;
  ackCode: string;
  ackMessage: string;
  receivedAt: string;
  processedAt: string;
};

export type MedicalDevice = {
  deviceCode: string;
  deviceName: string;
  deviceType: string;
  ip: string;
  port: number;
  status: string;
  lastConnectedAt: string | null;
};

export type RegisterObservationResultResponse = {
  messageControlId: string;
  status: "SUCCESS" | "FAILED" | "DUPLICATE";
  patientNo: string | null;
  orderNo: string | null;
  savedResultCount: number;
  errorCode: string | null;
  message: string | null;
  ackMessage: string;
};

export type SocketEnvelope = {
  type: "CONNECTED" | "PRESENCE_SNAPSHOT" | "HL7_MESSAGE" | "CHAT_MESSAGE" | "DEVICE_EVENT" | "PONG" | "ERROR";
  payload: unknown;
};

export type ChatMessage = {
  id: string;
  senderId: string;
  senderName: string;
  senderRole: UserRole;
  content: string;
  sentAt: string;
};
