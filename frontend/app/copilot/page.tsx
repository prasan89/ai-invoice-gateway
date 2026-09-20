"use client";
import { useEffect, useRef, useState } from "react";
import type { CopilotSession } from "../types";
import { API } from "../utils";

const SUGGESTIONS = [
  "How much GST did we pay this month?",
  "Show unusual invoices this month",
  "Which vendor has the highest spend?",
  "Why was the last invoice rejected?",
  "How much is pending approval?",
  "Show duplicate invoices detected",
];

export default function CopilotPage() {
  const [sessions, setSessions] = useState<CopilotSession[]>([]);
  const [active, setActive]     = useState<CopilotSession | null>(null);
  const [input, setInput]       = useState("");
  const [loading, setLoading]   = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    fetch(API + "/api/v1/copilot/sessions")
      .then(r => r.ok ? r.json() : [])
      .then(setSessions);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [active?.messages]);

  async function ask(question?: string) {
    const q = question ?? input.trim();
    if (!q) return;
    setInput("");
    setLoading(true);
    const body: Record<string, string> = { question: q };
    if (active) body.sessionId = active.id;
    try {
      const r = await fetch(API + "/api/v1/copilot/ask", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      if (r.ok) {
        const session: CopilotSession = await r.json();
        setActive(session);
        setSessions(prev => {
          const idx = prev.findIndex(s => s.id === session.id);
          return idx >= 0
            ? [session, ...prev.filter(s => s.id !== session.id)]
            : [session, ...prev];
        });
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="copilot-shell">
      {/* Sidebar */}
      <aside className="copilot-sidebar">
        <div className="copilot-sidebar-top">
          <button className="btn btn-primary copilot-new-btn" onClick={() => setActive(null)}>
            + New Chat
          </button>
        </div>
        <div className="copilot-session-list">
          {sessions.length === 0 && (
            <div className="copilot-session-empty">No previous chats</div>
          )}
          {sessions.map(s => (
            <button
              key={s.id}
              className={`copilot-session-item${active?.id === s.id ? " active" : ""}`}
              onClick={() => setActive(s)}
            >
              {s.title || "Untitled"}
            </button>
          ))}
        </div>
      </aside>

      {/* Main chat */}
      <div className="copilot-main">
        <div className="copilot-messages">

          {/* Empty state */}
          {!active && (
            <div className="copilot-welcome">
              <div className="copilot-welcome-icon">✦</div>
              <h2 className="copilot-welcome-title">AI Finance Copilot</h2>
              <p className="copilot-welcome-sub">
                Ask questions about your invoices, spend, GST, and more
              </p>
              <div className="copilot-suggestions">
                {SUGGESTIONS.map(s => (
                  <button key={s} className="copilot-suggestion" onClick={() => ask(s)}>
                    {s}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Messages */}
          {active?.messages.map(m => (
            <div key={m.id} className={`copilot-msg copilot-msg-${m.role.toLowerCase()}`}>
              <div className="copilot-bubble">{m.content}</div>
            </div>
          ))}

          {/* Thinking */}
          {loading && (
            <div className="copilot-msg copilot-msg-assistant">
              <div className="copilot-bubble copilot-thinking">Thinking…</div>
            </div>
          )}

          <div ref={bottomRef} />
        </div>

        {/* Input bar */}
        <div className="copilot-input-bar">
          <input
            className="copilot-input"
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); ask(); } }}
            placeholder="Ask about invoices, spend, GST, vendors…"
            disabled={loading}
          />
          <button
            className="btn btn-primary"
            onClick={() => ask()}
            disabled={!input.trim() || loading}
          >
            Send
          </button>
        </div>
      </div>
    </div>
  );
}
