"use client";
import { useEffect, useState } from "react";
import type { Anomaly } from "../types";
import { API } from "../utils";

const RISK_CHIP: Record<string, string> = {
  LOW: "risk-low", MEDIUM: "risk-medium", HIGH: "risk-high", CRITICAL: "risk-critical",
};

export default function AnomaliesPage() {
  const [anomalies, setAnomalies] = useState<Anomaly[]>([]);
  const [loading, setLoading]     = useState(true);
  const [msg, setMsg]             = useState("");

  useEffect(() => {
    fetch(API + "/api/v1/anomalies")
      .then(r => r.ok ? r.json() : [])
      .then(setAnomalies)
      .finally(() => setLoading(false));
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  async function review(id: string, outcome: string) {
    const r = await fetch(API + "/api/v1/anomalies/" + id + "/review", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ outcome }),
    });
    if (r.ok) {
      const updated = await r.json();
      setAnomalies(prev => prev.map(a => a.id === id ? updated : a));
      setMsg("Marked as " + outcome.replace("_", " ").toLowerCase());
    }
  }

  if (loading) return <div className="loading-state">Loading anomalies…</div>;

  const critical   = anomalies.filter(a => a.riskLevel === "CRITICAL").length;
  const high       = anomalies.filter(a => a.riskLevel === "HIGH").length;
  const unreviewed = anomalies.filter(a => !a.reviewOutcome).length;

  return (
    <div className="page-content">
      <div className="page-header">
        <div className="page-title">AI Anomaly Detection</div>
        <div className="page-subtitle">Real-time risk analysis across all processed invoices</div>
      </div>

      {msg && <div className="page-notice page-notice-info">{msg}</div>}

      <div className="stats-grid" style={{ marginBottom: 24 }}>
        <div className="stat-card err">
          <div className="stat-label">Critical</div>
          <div className="stat-value">{critical}</div>
        </div>
        <div className="stat-card warn">
          <div className="stat-label">High Risk</div>
          <div className="stat-value">{high}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Unreviewed</div>
          <div className="stat-value">{unreviewed}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Total</div>
          <div className="stat-value">{anomalies.length}</div>
        </div>
      </div>

      {anomalies.length === 0 ? (
        <div className="empty-state">No anomalies yet. Process some invoices to see risk analysis.</div>
      ) : (
        <div className="table-section">
          {anomalies.map(a => (
            <div key={a.id} style={{ padding: "16px 20px", borderBottom: "1px solid #f3f4f6" }}>
              <div className="row-between" style={{ marginBottom: 8 }}>
                <div className="row-gap">
                  <span className={`chip ${RISK_CHIP[a.riskLevel] ?? "risk-low"}`}>{a.riskLevel}</span>
                  <span style={{ fontSize: 12, color: "#6b7280", fontFamily: "monospace" }}>Score {a.riskScore}/100</span>
                  {a.reviewOutcome && (
                    <span className="chip" style={{ background: "#ede9fe", color: "#5b21b6" }}>
                      {a.reviewOutcome.replace("_", " ")}
                    </span>
                  )}
                  <div className="row-gap" style={{ gap: 6 }}>
                    {a.anomalyTypes.map(t => (
                      <span key={t} className="chip" style={{ background: "#fef2f2", color: "#b91c1c", fontSize: 10 }}>
                        {t.replace(/_/g, " ")}
                      </span>
                    ))}
                  </div>
                </div>
                {/* Risk score ring */}
                <svg width="44" height="44" viewBox="0 0 36 36" style={{ transform: "rotate(-90deg)", flexShrink: 0 }}>
                  <circle cx="18" cy="18" r="15" fill="none" stroke="#e5e7eb" strokeWidth="4" />
                  <circle cx="18" cy="18" r="15" fill="none"
                    stroke={a.riskScore >= 75 ? "#ef4444" : a.riskScore >= 50 ? "#f97316" : a.riskScore >= 25 ? "#eab308" : "#22c55e"}
                    strokeWidth="4" strokeDasharray={`${a.riskScore * 0.942} 94.2`} />
                </svg>
              </div>

              <div style={{ fontSize: 11, color: "#9ca3af", marginBottom: 8 }}>
                Invoice {a.invoiceId} · {new Date(a.detectedAt).toLocaleString()}
              </div>

              <ul style={{ margin: 0, padding: 0, listStyle: "none", fontSize: 13, color: "#374151", marginBottom: 10 }}>
                {a.reasons.map((r, i) => (
                  <li key={i} style={{ display: "flex", gap: 6, marginBottom: 2 }}>
                    <span style={{ color: "#ef4444" }}>•</span>{r}
                  </li>
                ))}
              </ul>

              {!a.reviewOutcome && (
                <div className="row-gap">
                  <button className="btn btn-secondary" style={{ fontSize: 12, padding: "5px 12px" }}
                    onClick={() => review(a.id, "FALSE_POSITIVE")}>False positive</button>
                  <button className="btn btn-danger" style={{ fontSize: 12, padding: "5px 12px" }}
                    onClick={() => review(a.id, "CONFIRMED")}>Confirm anomaly</button>
                  <button className="btn btn-secondary" style={{ fontSize: 12, padding: "5px 12px", color: "#d97706", borderColor: "#fcd34d" }}
                    onClick={() => review(a.id, "ESCALATED")}>Escalate</button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
