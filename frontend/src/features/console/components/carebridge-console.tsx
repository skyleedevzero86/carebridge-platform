"use client";

import { useCarebridgeConsole } from "@/features/console/hooks/use-carebridge-console";
import { WorkItemBoard } from "@/features/work-items/components/work-item-board";

function formatTime(value: string | null) {
  if (!value) return "수신 없음";
  return new Intl.DateTimeFormat("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  }).format(new Date(value));
}

function formatInterval(value: number | undefined) {
  if (!value) return "-";
  return `${Math.round(value / 1000)}초 간격`;
}

export function CarebridgeConsole() {
  const {
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
  } = useCarebridgeConsole();

  const socketTone =
    socketState === "CONNECTED" ? "live" : socketState === "CONNECTING" ? "warm" : "muted";

  
  if (!token || !currentUser) {
    return (
      <main className="consoleShell landingShell">
        
        <nav className="siteHeader">
          <div className="siteHeaderInner">
            <div className="siteLogo">
              <div className="siteLogoMark">CB</div>
              <span className="siteLogoText">CareBridge</span>
            </div>
          </div>
        </nav>

        
        <section className="landingHero" style={{ marginTop: 40 }}>
          <div className="heroBadge">
            <span className="heroBadgeDot" />
            CareBridge Interface Server · 의료 장비 통합 콘솔
          </div>
          <h1 className="heroTitle">
            의료 장비 인터페이스,<br />
            <strong>실시간</strong> 운영자 콘솔
          </h1>
          <p className="heroCopy">
            TCP 장비 메시지를 수신·해석·저장하고, WebSocket으로 운영자 콘솔에 실시간 브로드캐스트합니다.
            Redis Presence로 접속 현황을 관리하며 채팅 기능을 제공합니다.
          </p>

          <div className="heroCards">
            <article className="infoCard">
              <h2>🔐 데모 계정</h2>
              <p><strong>admin</strong> / Admin1234!</p>
              <p><strong>operator</strong> / Operator1234!</p>
            </article>
            <article className="infoCard">
              <h2>📡 TCP 장비 메시지 샘플</h2>
              <code>DEVICE=XRAY-01|PATIENT=P-1004|RESULT=NORMAL|STATUS=READY</code>
            </article>
            <article className="infoCard">
              <h2>💡 Presence 규칙</h2>
              <p>온라인: <strong>TTL 70초</strong> 자동 갱신 (20초 주기 PING)</p>
              <p>오프라인: Redis TTL 만료 시 자동 처리</p>
            </article>
          </div>
        </section>

        
        <section className="authGrid">
          <article className="authPanel">
            <div className="authPanelHead">
              <h2>{authMode === "login" ? "로그인" : "회원가입"}</h2>
              <p>운영자 콘솔에 접속합니다.</p>
            </div>

            <div className="segmented" style={{ marginBottom: 20 }}>
              <button
                className={authMode === "login" ? "selected" : ""}
                onClick={() => setAuthMode("login")}
              >
                로그인
              </button>
              <button
                className={authMode === "register" ? "selected" : ""}
                onClick={() => setAuthMode("register")}
              >
                회원가입
              </button>
            </div>

            {authMode === "login" ? (
              <form
                className="formStack"
                onSubmit={(e) => { e.preventDefault(); void submitLogin(); }}
              >
                <label className="field">
                  <span>사용자명 (Username)</span>
                  <input
                    value={loginForm.username}
                    placeholder="admin"
                    onChange={(e) => setLoginForm((c) => ({ ...c, username: e.target.value }))}
                  />
                </label>
                <label className="field">
                  <span>비밀번호 (Password)</span>
                  <input
                    type="password"
                    value={loginForm.password}
                    placeholder="••••••••"
                    onChange={(e) => setLoginForm((c) => ({ ...c, password: e.target.value }))}
                  />
                </label>
                <button className="primaryButton" disabled={busy} type="submit">
                  {busy ? "로그인 중..." : "로그인"}
                </button>
              </form>
            ) : (
              <form
                className="formStack"
                onSubmit={(e) => { e.preventDefault(); void submitRegister(); }}
              >
                <label className="field">
                  <span>사용자명 (3-30자)</span>
                  <input
                    value={registerForm.username}
                    placeholder="username"
                    onChange={(e) => setRegisterForm((c) => ({ ...c, username: e.target.value }))}
                  />
                </label>
                <label className="field">
                  <span>표시 이름 (2-20자)</span>
                  <input
                    value={registerForm.displayName}
                    placeholder="홍길동"
                    onChange={(e) => setRegisterForm((c) => ({ ...c, displayName: e.target.value }))}
                  />
                </label>
                <label className="field">
                  <span>비밀번호 (8-40자)</span>
                  <input
                    type="password"
                    value={registerForm.password}
                    placeholder="••••••••"
                    onChange={(e) => setRegisterForm((c) => ({ ...c, password: e.target.value }))}
                  />
                </label>
                <button className="primaryButton" disabled={busy} type="submit">
                  {busy ? "처리 중..." : "회원가입"}
                </button>
              </form>
            )}

            {error ? <p className="state error" style={{ marginTop: 12 }}>{error}</p> : null}
          </article>
        </section>
      </main>
    );
  }

  
  return (
    <>
      
      <nav className="siteHeader">
        <div className="siteHeaderInner">
          <div className="siteLogo">
            <div className="siteLogoMark">CB</div>
            <span className="siteLogoText">CareBridge Console</span>
          </div>
          <div className="headerNav">
            <span className={`socketBadge ${socketTone}`}>{socketState}</span>
            <span className="identityBadge">{currentUser.displayName} · {currentUser.role}</span>
            <button className="ghostButton" onClick={() => void refreshConsole()}>새로고침</button>
            <button className="ghostButton" onClick={() => void signOut()}>로그아웃</button>
          </div>
        </div>
      </nav>

      <main className="consoleShell">
        
        <div className="consoleHeader">
          <div>
            <p className="eyebrow" style={{ fontSize: 12, fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', color: 'var(--primary)', marginBottom: 4 }}>
              Medical Device Interface Console
            </p>
            <h1 style={{ fontSize: 20, fontWeight: 700, color: 'var(--text)', fontFamily: 'var(--font-heading)', letterSpacing: '-0.04em' }}>
              실시간 장비 이벤트 · 운영자 채팅 · 접속 현황
            </h1>
          </div>
          <div style={{ fontSize: 13, color: 'var(--text-muted)' }}>
            총 멤버 {members.length}명 · 온라인 {connectedCount}명
          </div>
        </div>

        {error ? <p className="state error headerError">{error}</p> : null}

        
        <section className="consoleGrid">

          
          <aside className="panel">
            <div className="panelHead">
              <div>
                <h2>접속 현황</h2>
                <p className="subtle">멤버 {members.length}명 / 온라인 {connectedCount}명</p>
              </div>
              <span className="badge">Redis 기반</span>
            </div>

            <div className="presenceList">
              {members.map((member) => {
                const online = member.online === 1;
                return (
                  <div className="presenceRow" key={member.id}>
                    <div className={`presenceDot ${online ? "online" : "offline"}`} />
                    <div className="presenceMeta">
                      <strong>{member.displayName}</strong>
                      <span>{member.username} · {member.role}</span>
                    </div>
                    <span className={`presenceBadge ${online ? "online" : "offline"}`}>
                      {online ? "ON" : "OFF"}
                    </span>
                  </div>
                );
              })}
            </div>
          </aside>

          
          <section className="panel chatPanel">
            <div className="panelHead">
              <div>
                <h2>운영자 채팅</h2>
                <p className="subtle">WebSocket 실시간 메시지</p>
              </div>
              <span className={`badge ${socketState !== "CONNECTED" ? "neutral" : ""}`}>
                {socketState === "CONNECTED" ? "LIVE" : "대기 중"}
              </span>
            </div>

            <div className="chatStream">
              {messages.map((message) => (
                <article
                  className={`chatBubble ${message.senderId === currentUser.id ? "mine" : ""}`}
                  key={message.id}
                >
                  <div className="chatMeta">
                    <strong>{message.senderName}</strong>
                    <span>{message.senderRole}</span>
                    <time>{formatTime(message.sentAt)}</time>
                  </div>
                  <p>{message.content}</p>
                </article>
              ))}
            </div>

            <form
              className="chatComposer"
              onSubmit={(e) => { e.preventDefault(); submitChat(); }}
            >
              <textarea
                value={chatDraft}
                onChange={(e) => setChatDraft(e.target.value)}
                placeholder="운영자에게 메시지를 보냅니다. (Shift+Enter: 줄바꿈)"
              />
              <button className="primaryButton" type="submit">
                메시지 전송
              </button>
            </form>
          </section>

          
          <aside className="sideStack">
            
            <section className="panel">
              <div className="panelHead">
                <div>
                  <h2>장비 인터페이스 현황</h2>
                  <p className="subtle">TCP :{overview?.tcpPort ?? "-"} 수신 대기 중</p>
                </div>
                <span className="badge">PostgreSQL</span>
              </div>

              <div className="statGrid">
                <div className="statCard">
                  <span>총 수신 건수</span>
                  <strong>{overview?.totalMessages ?? 0}</strong>
                </div>
                <div className="statCard">
                  <span>마지막 수신</span>
                  <strong style={{ fontSize: 12 }}>{formatTime(overview?.lastReceivedAt ?? null)}</strong>
                </div>
              </div>

              <div className="statGrid">
                <div className="statCard">
                  <span>시뮬레이터</span>
                  <strong style={{ color: overview?.simulatorEnabled ? 'var(--success)' : 'var(--text-muted)' }}>
                    {overview?.simulatorEnabled ? "작동 중" : "중지"}
                  </strong>
                </div>
                <div className="statCard">
                  <span>전송 간격</span>
                  <strong>{formatInterval(overview?.simulatorIntervalMillis)}</strong>
                </div>
              </div>

              <div className="protocolCard">
                <p>KEY_VALUE 프로토콜 샘플 (파이프 구분자)</p>
                <span className="mono">DEVICE=XRAY-01|PATIENT=P-1004|RESULT=NORMAL|STATUS=READY</span>
              </div>

              <div className="compactStack formStack">
                <label className="field">
                  <span>수동 장비 시뮬레이션 {currentUser.role !== "ADMIN" ? "(ADMIN 전용)" : ""}</span>
                  <textarea
                    value={simulationPayload}
                    onChange={(e) => setSimulationPayload(e.target.value)}
                    disabled={currentUser.role !== "ADMIN"}
                    placeholder={currentUser.role === "ADMIN" ? "페이로드를 입력하세요..." : "ADMIN 계정만 사용 가능합니다."}
                  />
                </label>
                <button
                  className="primaryButton"
                  disabled={busy || currentUser.role !== "ADMIN"}
                  onClick={() => void submitSimulation()}
                >
                  {currentUser.role === "ADMIN" ? "페이로드 전송" : "ADMIN 전용"}
                </button>
              </div>
            </section>

            
            <section className="panel">
              <div className="panelHead">
                <div>
                  <h2>장비 이벤트 피드</h2>
                  <p className="subtle">최근 수신 이벤트 (실시간 갱신)</p>
                </div>
              </div>

              <div className="eventList">
                {deviceEvents.map((event) => (
                  <article className="eventCard" key={event.id}>
                    <div className="eventHeadline">
                      <strong>{event.deviceCode}</strong>
                      <span className="badge neutral">{event.protocol}</span>
                    </div>
                    <p>{event.summary}</p>
                    <p className="subtle">환자: {event.patientCode ?? "미확인"} · IP: {event.sourceIp}</p>
                    <span className="mono">{event.ackCode}</span>
                    <br />
                    <time>{formatTime(event.receivedAt)}</time>
                  </article>
                ))}
              </div>
            </section>
          </aside>
        </section>

        
        <WorkItemBoard token={token} />
      </main>
    </>
  );
}