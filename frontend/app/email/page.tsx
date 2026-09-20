"use client";
import { useEffect, useState } from "react";
import type { EmailPollLog } from "../types";
import { API } from "../utils";

export default function EmailPage() {
  const [logs, setLogs]           = useState<EmailPollLog[]>([]);
  const [triggering, setTriggering] = useState(false);
  const [err, setErr]             = useState<string | null>(null);

  async function load() {
    try {
      const r = await fetch(API + "/api/v1/email-poll/logs");
      if (r.ok) setLogs(await r.json());
    } catch { setErr("Could not load poll logs"); }
  }

  useEffect(() => { load(); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  async function trigger() {
    setTriggering(true);
    try {
      await fetch(API + "/api/v1/email-poll/trigger", { method: "POST" });
      await load();
    } finally { setTriggering(false); }
  }

  const statusColor: Record<string, string> = {
    SUCCESS: "#16a34a", PARTIAL: "#d97706", FAILED: "#dc2626", NO_MESSAGES: "#6b7280",
  };

  return (
    <div className="page-content">
      <div className="page-header">
        <div className="page-title">Email Invoice Automation</div>
        <div className="page-subtitle">Automatically extract invoices from your mailbox attachments</div>
      </div>

      {err && <div className="page-notice page-notice-error">{err}</div>}

      {/* Config card */}
      <div className="card">
        <div className="card-title">IMAP Configuration</div>
        <div className="card-sub">Set these environment variables to enable automatic email polling every 5 minutes</div>
        <div style={{ background: "#f8fafc", border: "1px solid #e5e7eb", borderRadius: 8, padding: "12px 16px", fontFamily: "monospace", fontSize: 12, color: "#374151", marginBottom: 16, lineHeight: 2 }}>
          INVOICE_EMAIL_ENABLED=true<br />
          INVOICE_EMAIL_HOST=imap.gmail.com<br />
          INVOICE_EMAIL_USER=your@email.com<br />
          INVOICE_EMAIL_PASSWORD=app-password<br />
          INVOICE_EMAIL_FOLDER=INBOX
        </div>
        <button className="btn btn-primary" onClick={trigger} disabled={triggering}>
          {triggering ? "Polling…" : "Trigger manual poll"}
        </button>
      </div>

      {/* Poll logs */}
      <div className="table-section">
        <div className="table-toolbar">
          <span className="table-toolbar-title">
            Recent Poll Logs <span style={{ color: "#9ca3af", fontWeight: 400, fontSize: 13 }}>({logs.length})</span>
          </span>
        </div>
        {logs.length === 0 ? (
          <div className="empty-state">No poll logs yet. Configure email polling and trigger a run.</div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Time</th>
                <th>Mailbox</th>
                <th>Found</th>
                <th>Created</th>
                <th>Errors</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {logs.map(l => (
                <tr key={l.id}>
                  <td style={{ whiteSpace: "nowrap", color: "#6b7280" }}>{new Date(l.polledAt).toLocaleString()}</td>
                  <td>{l.mailboxUser ?? "—"}</td>
                  <td>{l.messagesFound}</td>
                  <td>{l.invoicesCreated}</td>
                  <td style={{ color: l.errors > 0 ? "#dc2626" : undefined }}>{l.errors}</td>
                  <td>
                    <span className="chip" style={{
                      background: statusColor[l.status] ? statusColor[l.status] + "20" : "#f3f4f6",
                      color: statusColor[l.status] ?? "#6b7280",
                    }}>{l.status}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
