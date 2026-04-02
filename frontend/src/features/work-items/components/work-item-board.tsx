"use client";

import { useWorkItemBoard } from "@/features/work-items/hooks/use-work-item-board";
import type { WorkItemPriority, WorkItemStatus } from "@/features/work-items/model/work-item";

const STATUS_FLOW: WorkItemStatus[] = ["BACKLOG", "IN_PROGRESS", "DONE"];

const STATUS_LABEL: Record<WorkItemStatus, string> = {
  BACKLOG: "대기",
  IN_PROGRESS: "진행 중",
  DONE: "완료",
};

const PRIORITY_LABEL: Record<WorkItemPriority, string> = {
  HIGH: "높음",
  MEDIUM: "보통",
  LOW: "낮음",
};

interface WorkItemBoardProps {
  token: string;
}

export function WorkItemBoard({ token }: WorkItemBoardProps) {
  const {
    board,
    loading,
    error,
    sortBy,
    title,
    description,
    priority,
    setTitle,
    setDescription,
    setPriority,
    changeSort,
    createWorkItem,
    updateStatus,
  } = useWorkItemBoard(token);

  return (
    <div className="workItemShell">
      
      <div className="workItemHeader">
        <div>
          <p className="workItemEyebrow">작업 보드 — Redis 캐시 · PostgreSQL 영속화</p>
          <h2 className="workItemTitle">Work Items</h2>
        </div>
        <div className="workItemHeaderRight">
          <span className="badge">{board.totalCount}건</span>
          <span className={`badge ${board.cached ? "" : "neutral"}`}>
            캐시 {board.cached ? "HIT" : "MISS"}
          </span>
        </div>
      </div>

      <div className="workItemGrid">
        
        <section className="panel workItemCreatePanel">
          <div className="panelHead">
            <h2>새 작업 추가</h2>
          </div>

          <div className="formStack">
            <label className="field">
              <span>제목 (최대 80자)</span>
              <input
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="예: TCP 수신 어댑터 구현"
                maxLength={80}
              />
            </label>

            <label className="field">
              <span>설명</span>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="페이로드 파싱, 유효성 검사, PostgreSQL 저장 구현..."
              />
            </label>

            <label className="field">
              <span>우선순위</span>
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value as WorkItemPriority)}
              >
                <option value="HIGH">🔴 높음 (HIGH)</option>
                <option value="MEDIUM">🟡 보통 (MEDIUM)</option>
                <option value="LOW">🟢 낮음 (LOW)</option>
              </select>
            </label>

            <button
              className="primaryButton"
              onClick={() => void createWorkItem()}
              disabled={!title.trim()}
            >
              작업 추가
            </button>
          </div>
        </section>

        
        <section className="panel workItemListPanel">
          <div className="panelHead">
            <div>
              <h2>작업 목록</h2>
              <p className="subtle">{board.totalCount}개 항목</p>
            </div>
            <div className="segmented">
              <button
                className={sortBy === "RECENT" ? "selected" : ""}
                onClick={() => changeSort("RECENT")}
              >
                최신순
              </button>
              <button
                className={sortBy === "PRIORITY" ? "selected" : ""}
                onClick={() => changeSort("PRIORITY")}
              >
                우선순위순
              </button>
            </div>
          </div>

          {error && <p className="state error">{error}</p>}
          {loading && <p className="subtle" style={{ padding: "12px 0" }}>불러오는 중...</p>}

          <div className="workItemCardList">
            {board.items.map((item) => (
              <div className="workItemCard" key={item.id}>
                <div className="workItemCardTop">
                  <span className={`workItemPill priority-${item.priority.toLowerCase()}`}>
                    {PRIORITY_LABEL[item.priority]}
                  </span>
                  <span className={`workItemPill status-${item.status.toLowerCase()}`}>
                    {STATUS_LABEL[item.status]}
                  </span>
                </div>
                <p className="workItemCardTitle">{item.title}</p>
                {item.description && (
                  <p className="workItemCardDesc">{item.description}</p>
                )}
                <div className="workItemActions">
                  {STATUS_FLOW.filter((s) => s !== item.status).map((status) => (
                    <button
                      key={status}
                      className="workItemActionBtn"
                      onClick={() => void updateStatus(item.id, status)}
                    >
                      → {STATUS_LABEL[status]}
                    </button>
                  ))}
                </div>
              </div>
            ))}

            {!loading && board.items.length === 0 && (
              <p className="subtle" style={{ padding: "20px 0", textAlign: "center" }}>
                등록된 작업이 없습니다.
              </p>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}