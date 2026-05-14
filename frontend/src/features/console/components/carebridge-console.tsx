"use client";

import { useCarebridgeConsole, type MenuKey } from "@/features/console/hooks/use-carebridge-console";

const MENU_ITEMS: { key: MenuKey; label: string }[] = [
  { key: "patients", label: "Patients" },
  { key: "orders", label: "Exam Orders" },
  { key: "results", label: "Results" },
  { key: "hl7-logs", label: "HL7 Logs" },
  { key: "devices", label: "Devices" },
  { key: "simulator", label: "Simulator" },
];

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
          <h1>CareBridge EMR Interface Server</h1>
          <p>HL7 ORU^R01 검사결과를 환자와 검사오더에 매칭하는 운영 콘솔입니다.</p>
          <form onSubmit={(event) => { event.preventDefault(); void state.submitLogin(); }}>
            <label>
              Username
              <input value={state.loginForm.username} onChange={(event) => state.setLoginForm((current) => ({ ...current, username: event.target.value }))} />
            </label>
            <label>
              Password
              <input type="password" value={state.loginForm.password} onChange={(event) => state.setLoginForm((current) => ({ ...current, password: event.target.value }))} />
            </label>
            <button disabled={state.busy} type="submit">{state.busy ? "Signing in..." : "Sign in"}</button>
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
          <span>EMR Interface</span>
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
            <p>TCP 9093, REST /api/interface/hl7/messages, WebSocket {state.socketState}</p>
          </div>
          <div className="emrActions">
            <span>{state.currentUser.displayName}</span>
            <button onClick={() => void state.refreshConsole()}>Refresh</button>
            <button onClick={() => void state.signOut()}>Sign out</button>
          </div>
        </header>

        {state.error ? <p className="emrError">{state.error}</p> : null}

        {state.activeMenu === "patients" ? (
          <div className="emrGrid two">
            <section className="emrPanel">
              <h2>Patient List</h2>
              <table>
                <thead>
                  <tr><th>No</th><th>Name</th><th>Birth</th><th>Gender</th><th>Results</th><th>Last</th></tr>
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
            <h2>Exam Orders</h2>
            <table>
              <thead><tr><th>Order</th><th>Patient</th><th>Exam</th><th>Status</th><th>Ordered</th><th>Completed</th></tr></thead>
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
            <h2>HL7 Message Logs</h2>
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
            <h2>Medical Devices</h2>
            <table>
              <thead><tr><th>Code</th><th>Name</th><th>Type</th><th>Endpoint</th><th>Status</th><th>Last</th></tr></thead>
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
              <h2>Fake ECG Device</h2>
              <textarea value={state.rawHl7} onChange={(event) => state.setRawHl7(event.target.value)} />
              <button onClick={() => void state.submitRawHl7()} disabled={state.busy}>Send REST HL7</button>
              <button onClick={() => void state.submitSimulation()} disabled={state.busy}>Generate and Send ECG</button>
            </section>
            <section className="emrPanel">
              <h2>ACK / API Response</h2>
              {state.lastResponse ? <pre>{JSON.stringify(state.lastResponse, null, 2)}</pre> : <p className="muted">No response yet.</p>}
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
      <h2>{resultsOnly ? "Observation Results" : "Patient Detail"}</h2>
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
                <span>{result.observationName} / Ref {result.referenceRange} / Flag {result.abnormalFlag}</span>
                <span>{result.deviceCode} / {formatTime(result.createdAt)} / {result.messageControlId}</span>
              </article>
            ))}
          </div>
        </>
      ) : <p className="muted">Select a patient.</p>}
    </section>
  );
}
