"use client";

import { useCarebridgeConsole, type MenuKey } from "@/features/console/hooks/use-carebridge-console";

const MENU_ITEMS: { key: MenuKey; label: string }[] = [
  { key: "patients", label: "환자" },
  { key: "orders", label: "검사 오더" },
  { key: "results", label: "검사 결과" },
  { key: "hl7-logs", label: "HL7 로그" },
  { key: "devices", label: "의료기기" },
  { key: "simulator", label: "시뮬레이터" },
];

const SOCKET_STATE_LABEL: Record<"DISCONNECTED" | "CONNECTING" | "CONNECTED", string> = {
  DISCONNECTED: "끊김",
  CONNECTING: "연결 중",
  CONNECTED: "연결됨",
};

function formatTime(value: string | null | undefined) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export function CarebridgeConsole() {
  const state = useCarebridgeConsole();

  if (!state.token || !state.currentUser) {
    return (
      <main className="emrLogin">
        <section className="emrLoginPanel">
          <h1>CareBridge EMR 인터페이스 서버</h1>
          <p>HL7 ORU^R01 검사결과를 환자와 검사오더에 매칭하는 운영 콘솔입니다.</p>
          <form onSubmit={(event) => { event.preventDefault(); void state.submitLogin(); }}>
            <label>
              아이디
              <input value={state.loginForm.username} onChange={(event) => state.setLoginForm((current) => ({ ...current, username: event.target.value }))} />
            </label>
            <label>
              비밀번호
              <input type="password" value={state.loginForm.password} onChange={(event) => state.setLoginForm((current) => ({ ...current, password: event.target.value }))} />
            </label>
            <button disabled={state.busy} type="submit">{state.busy ? "로그인 중…" : "로그인"}</button>
          </form>
          {state.error ? <p className="emrError">{state.error}</p> : null}
        </section>
      </main>
    );
  }

  return (
    <main className="emrShell">
      <aside className="emrSidebar">
        <div className="emrBrand">
          <strong>CareBridge</strong>
          <span>EMR 인터페이스</span>
        </div>
        <nav>
          {MENU_ITEMS.map((item) => (
            <button key={item.key} className={state.activeMenu === item.key ? "active" : ""} onClick={() => state.setActiveMenu(item.key)}>
              {item.label}
            </button>
          ))}
        </nav>
      </aside>

      <section className="emrMain">
        <header className="emrTopbar">
          <div>
            <h1>{MENU_ITEMS.find((item) => item.key === state.activeMenu)?.label}</h1>
            <p>TCP 9093 · REST HL7 수신 · 웹소켓 {SOCKET_STATE_LABEL[state.socketState]}</p>
          </div>
          <div className="emrActions">
            <span>{state.currentUser.displayName}</span>
            <button onClick={() => void state.refreshConsole()}>새로고침</button>
            <button onClick={() => void state.signOut()}>로그아웃</button>
          </div>
        </header>

        {state.error ? <p className="emrError">{state.error}</p> : null}

        {state.activeMenu === "patients" ? (
          <div className="emrGrid two">
            <section className="emrPanel">
              <h2>환자 목록</h2>
              <table>
                <thead>
                  <tr><th>환자번호</th><th>이름</th><th>생년월일</th><th>성별</th><th>결과 건수</th><th>최근 수신</th></tr>
                </thead>
                <tbody>
                  {state.patients.map((patient) => (
                    <tr key={patient.patientNo} onClick={() => void state.selectPatient(patient.patientNo)}>
                      <td>{patient.patientNo}</td><td>{patient.name}</td><td>{patient.birthDate}</td><td>{patient.gender}</td><td>{patient.recentResultCount}</td><td>{formatTime(patient.lastReceivedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
            <PatientDetailPanel state={state} />
          </div>
        ) : null}

        {state.activeMenu === "orders" ? (
          <section className="emrPanel">
            <h2>검사 오더</h2>
            <table>
              <thead><tr><th>오더번호</th><th>환자</th><th>검사</th><th>상태</th><th>오더 시각</th><th>완료 시각</th></tr></thead>
              <tbody>
                {state.orders.map((order) => (
                  <tr key={order.orderNo}><td>{order.orderNo}</td><td>{order.patientNo}</td><td>{order.examCode} / {order.examName}</td><td><span className={`status ${order.status.toLowerCase()}`}>{order.status}</span></td><td>{formatTime(order.orderedAt)}</td><td>{formatTime(order.completedAt)}</td></tr>
                ))}
              </tbody>
            </table>
          </section>
        ) : null}

        {state.activeMenu === "results" ? <PatientDetailPanel state={state} resultsOnly /> : null}

        {state.activeMenu === "hl7-logs" ? (
          <section className="emrPanel">
            <h2>HL7 메시지 로그</h2>
            <div className="logList">
              {state.hl7Logs.map((log) => (
                <article key={log.messageControlId} className="logCard">
                  <div><strong>{log.messageControlId}</strong><span className={`status ${log.processStatus.toLowerCase()}`}>{log.processStatus}</span></div>
                  <p>{log.messageType} / {log.deviceCode} / {log.patientNo ?? "-"} / {log.orderNo ?? "-"}</p>
                  {log.errorCode ? <p className="emrError compact">{log.errorCode}: {log.errorMessage}</p> : null}
                  <pre>{log.rawMessage}</pre>
                  <pre>{log.ackMessage}</pre>
                </article>
              ))}
            </div>
          </section>
        ) : null}

        {state.activeMenu === "devices" ? (
          <section className="emrPanel">
            <h2>의료기기</h2>
            <table>
              <thead><tr><th>코드</th><th>이름</th><th>유형</th><th>엔드포인트</th><th>상태</th><th>최근 연결</th></tr></thead>
              <tbody>
                {state.devices.map((device) => (
                  <tr key={device.deviceCode}><td>{device.deviceCode}</td><td>{device.deviceName}</td><td>{device.deviceType}</td><td>{device.ip}:{device.port}</td><td>{device.status}</td><td>{formatTime(device.lastConnectedAt)}</td></tr>
                ))}
              </tbody>
            </table>
          </section>
        ) : null}

        {state.activeMenu === "simulator" ? (
          <div className="emrGrid two">
            <section className="emrPanel">
              <h2>가상 심전도 장비</h2>
              <textarea value={state.rawHl7} onChange={(event) => state.setRawHl7(event.target.value)} />
              <button onClick={() => void state.submitRawHl7()} disabled={state.busy}>REST로 HL7 전송</button>
              <button onClick={() => void state.submitSimulation()} disabled={state.busy}>ECG 샘플 생성·전송</button>
            </section>
            <section className="emrPanel">
              <h2>HL7 ACK 및 API 응답</h2>
              {state.lastResponse ? <pre>{JSON.stringify(state.lastResponse, null, 2)}</pre> : <p className="muted">아직 응답이 없습니다.</p>}
            </section>
          </div>
        ) : null}
      </section>
    </main>
  );
}

function PatientDetailPanel({ state, resultsOnly = false }: { state: ReturnType<typeof useCarebridgeConsole>; resultsOnly?: boolean }) {
  const detail = state.patientDetail;
  return (
    <section className="emrPanel">
      <h2>{resultsOnly ? "검사 결과" : "환자 상세"}</h2>
      {detail ? (
        <>
          {!resultsOnly ? (
            <div className="patientHeader">
              <strong>{detail.patient.patientNo}</strong>
              <span>{detail.patient.name} / {detail.patient.gender} / {detail.patient.birthDate}</span>
            </div>
          ) : null}
          {!resultsOnly ? (
            <div className="miniList">
              {detail.examOrders.map((order) => (
                <div key={order.orderNo}>{order.orderNo} / {order.examCode} / <span className={`status ${order.status.toLowerCase()}`}>{order.status}</span></div>
              ))}
            </div>
          ) : null}
          <div className="resultList">
            {detail.observationResults.map((result) => (
              <article key={result.id}>
                <strong>{result.observationCode}: {result.value} {result.unit}</strong>
                <span>{result.observationName} / 참고 {result.referenceRange} / 플래그 {result.abnormalFlag}</span>
                <span>{result.deviceCode} / {formatTime(result.createdAt)} / {result.messageControlId}</span>
              </article>
            ))}
          </div>
        </>
      ) : <p className="muted">환자를 선택하세요.</p>}
    </section>
  );
}
