"use client";

import { useMemo, useState } from "react";

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

type MenuKey = "members" | "presence" | "chat" | "device-overview" | "device-events" | "work-items";

const MENU_ITEMS: { key: MenuKey; label: string }[] = [
  { key: "members", label: "회원 관리" },
  { key: "presence", label: "접속 현황" },
  { key: "chat", label: "채팅" },
  { key: "device-overview", label: "장비 현황" },
  { key: "device-events", label: "이벤트 피드" },
  { key: "work-items", label: "워크아이템" },
];

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
  const [activeMenu, setActiveMenu] = useState<MenuKey>("members");
  const [memberPage, setMemberPage] = useState(1);

  const pageSize = 10;
  const memberTotalPages = Math.max(1, Math.ceil(members.length / pageSize));
  const pagedMembers = useMemo(() => {
    const safePage = Math.min(memberPage, memberTotalPages);
    const start = (safePage - 1) * pageSize;
    return members.slice(start, start + pageSize);
  }, [memberPage, memberTotalPages, members]);

  
  if (!token || !currentUser) {
    return (
      <main className="consoleShell landingShell">
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
                <p className="subtle" style={{ marginTop: -4 }}>
                  로그인 힌트: 운영자 <strong>operator / Operator1234!</strong>, 관리자 <strong>admin / Admin1234!</strong>
                </p>
                <label className="field">
                  <span>사용자명 (Username)</span>
                  <input
                    value={loginForm.username}
                    placeholder="예: operator"
                    onChange={(e) => setLoginForm((c) => ({ ...c, username: e.target.value }))}
                  />
                </label>
                <label className="field">
                  <span>비밀번호 (Password)</span>
                  <input
                    type="password"
                    value={loginForm.password}
                    placeholder="예: Operator1234!"
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
    <main className="legacyAdminPage">
      <aside className="legacySidebar">
        <div className="legacyBrand">CareBridge Platform</div>
        <nav className="legacyMenu">
          {MENU_ITEMS.map((item) => (
            <button
              key={item.key}
              className={`legacyMenuItem ${activeMenu === item.key ? "active" : ""}`}
              onClick={() => {
                setActiveMenu(item.key);
                if (item.key === "members") setMemberPage(1);
              }}
            >
              {item.label}
            </button>
          ))}
        </nav>
      </aside>

      <section className="legacyMain">
        <header className="legacyTopbar">
          <div className="legacyTopbarRight">
            <span className={`socketBadge ${socketTone}`}>{socketState}</span>
            <button className="ghostButton" onClick={() => void refreshConsole()}>새로고침</button>
            <button className="ghostButton" onClick={() => void signOut()}>로그아웃</button>
          </div>
        </header>

        <div className="legacyContent">
          <div className="legacyBreadcrumb">홈 / {MENU_ITEMS.find((item) => item.key === activeMenu)?.label}</div>
          <h1 className="legacyTitle">{MENU_ITEMS.find((item) => item.key === activeMenu)?.label}</h1>

          {error ? <p className="state error">{error}</p> : null}

          {activeMenu === "members" ? (
            <section className="legacyPanel" id="section-members">
              <div className="legacyPanelHead">
                <div className="legacyMeta">총 {members.length}명 / 온라인 {connectedCount}명</div>
              </div>
              <div className="legacyTableWrap">
                <table className="legacyTable">
                  <thead>
                    <tr>
                      <th>유저 ID</th>
                      <th>닉네임</th>
                      <th>이메일</th>
                      <th>메모</th>
                      <th>작성자</th>
                      <th>작성일시</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pagedMembers.map((member) => (
                      <tr key={member.id}>
                        <td>{member.username}</td>
                        <td>{member.displayName}</td>
                        <td>{member.username}@email.com</td>
                        <td>{member.online === 1 ? "온라인" : "오프라인"}</td>
                        <td>{currentUser.displayName}</td>
                        <td>{formatTime(new Date().toISOString())}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="legacyPagination">
                <button
                  className="legacyPageBtn"
                  onClick={() => setMemberPage((prev) => Math.max(prev - 1, 1))}
                  disabled={memberPage <= 1}
                >
                  이전
                </button>
                <span className="legacyPageInfo">{memberPage} / {memberTotalPages}</span>
                <button
                  className="legacyPageBtn"
                  onClick={() => setMemberPage((prev) => Math.min(prev + 1, memberTotalPages))}
                  disabled={memberPage >= memberTotalPages}
                >
                  다음
                </button>
              </div>
            </section>
          ) : null}

          {activeMenu === "presence" ? (
            <section className="panel" id="section-presence" style={{ marginTop: 16 }}>
              <div className="panelHead">
                <div>
                  <h2>접속 상태</h2>
                  <p className="subtle">실시간 Presence</p>
                </div>
                <span className="badge">Redis</span>
              </div>
              <div className="presenceList">
                {members.map((member) => {
                  const online = member.online === 1;
                  return (
                    <div className="presenceRow" key={`presence-${member.id}`}>
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
            </section>
          ) : null}

          {activeMenu === "chat" ? (
            <section className="panel chatPanel" id="section-chat" style={{ marginTop: 16 }}>
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
                onSubmit={(e) => {
                  e.preventDefault();
                  submitChat();
                }}
              >
                <textarea
                  value={chatDraft}
                  onChange={(e) => setChatDraft(e.target.value)}
                  placeholder="운영자에게 메시지를 보냅니다."
                />
                <button className="primaryButton" type="submit">메시지 전송</button>
              </form>
            </section>
          ) : null}

          {activeMenu === "device-overview" ? (
            <section className="panel" id="section-device-overview" style={{ marginTop: 16 }}>
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
                  <strong style={{ color: overview?.simulatorEnabled ? "var(--success)" : "var(--text-muted)" }}>
                    {overview?.simulatorEnabled ? "작동 중" : "중지"}
                  </strong>
                </div>
                <div className="statCard">
                  <span>전송 간격</span>
                  <strong>{formatInterval(overview?.simulatorIntervalMillis)}</strong>
                </div>
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
          ) : null}

          {activeMenu === "device-events" ? (
            <section className="panel" id="section-device-events" style={{ marginTop: 16 }}>
              <div className="panelHead">
                <div>
                  <h2>장비 이벤트 피드</h2>
                  <p className="subtle">최근 수신 이벤트</p>
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
          ) : null}

          {activeMenu === "work-items" ? (
            <section id="section-work-items" style={{ marginTop: 16 }}>
              <WorkItemBoard token={token} />
            </section>
          ) : null}
        </div>
      </section>
    </main>
  );
}