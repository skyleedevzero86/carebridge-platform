"use client";

import { useState } from "react";

import type { ChatMessage } from "@/features/console/model/carebridge";

function formatChatTime(value: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

type ChatPanelProps = {
  messages: ChatMessage[];
  onSend: (content: string) => void;
  sending: boolean;
};

export function ChatPanel({ messages, onSend, sending }: ChatPanelProps) {
  const [draft, setDraft] = useState("");

  return (
    <div className="chatPanel">
      <div className="chatMessages">
        {messages.map((message) => (
          <div key={message.id} className="chatRow">
            <div className="chatRowMeta">
              <strong>{message.senderName}</strong>
              <span className="muted">{message.senderRole}</span>
              <span className="muted">{formatChatTime(message.sentAt)}</span>
            </div>
            <p className="chatRowBody">{message.content}</p>
          </div>
        ))}
        {messages.length === 0 ? <p className="muted">아직 메시지가 없습니다.</p> : null}
      </div>
      <form
        className="chatComposer"
        onSubmit={(event) => {
          event.preventDefault();
          const trimmed = draft.trim();
          if (!trimmed) return;
          onSend(trimmed);
          setDraft("");
        }}
      >
        <input
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          placeholder="메시지 입력 (최대 1000자)"
          maxLength={1000}
          disabled={sending}
        />
        <button type="submit" disabled={sending || !draft.trim()}>
          전송
        </button>
      </form>
    </div>
  );
}
