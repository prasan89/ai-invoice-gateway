"use client";
import { useEffect, useState } from "react";
import type { Subscription, SubscriptionPlan, UsageRecord } from "../types";
import { API, money } from "../utils";

export default function BillingPage() {
  const [sub, setSub]       = useState<Subscription | null>(null);
  const [plans, setPlans]   = useState<SubscriptionPlan[]>([]);
  const [usage, setUsage]   = useState<UsageRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [msg, setMsg]       = useState("");

  useEffect(() => {
    Promise.all([
      fetch(API + "/api/v1/billing/subscription").then(r => r.json()),
      fetch(API + "/api/v1/billing/plans").then(r => r.json()),
      fetch(API + "/api/v1/billing/usage").then(r => r.json()),
    ]).then(([s, p, u]) => { setSub(s); setPlans(p); setUsage(u); })
      .finally(() => setLoading(false));
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  async function changePlan(planName: string) {
    setMsg("");
    const r = await fetch(API + "/api/v1/billing/subscription/plan", {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ planName }),
    });
    if (r.ok) { setSub(await r.json()); setMsg("Plan updated successfully."); }
    else setMsg("Failed to change plan.");
  }

  async function cancel() {
    if (!confirm("Cancel subscription? You will be downgraded at end of billing period.")) return;
    await fetch(API + "/api/v1/billing/subscription/cancel", { method: "POST" });
    setMsg("Subscription cancelled.");
    const r = await fetch(API + "/api/v1/billing/subscription");
    if (r.ok) setSub(await r.json());
  }

  if (loading) return <div className="loading-state">Loading billing…</div>;

  const subStatusCls: Record<string, string> = {
    TRIALING: "sub-trialing", ACTIVE: "sub-active", PAST_DUE: "sub-past_due",
    CANCELLED: "sub-cancelled", EXPIRED: "sub-expired",
  };

  return (
    <div className="page-content">
      <div className="page-header">
        <div className="page-title">Billing &amp; Subscription</div>
        <div className="page-subtitle">Manage your plan, usage, and payment</div>
      </div>

      {msg && <div className="page-notice page-notice-info">{msg}</div>}

      {/* Current subscription */}
      {sub && (
        <div className="card">
          <div className="row-between" style={{ marginBottom: 16 }}>
            <div>
              <div className="card-title">{sub.planDisplayName} Plan</div>
              <span className={`chip ${subStatusCls[sub.status] ?? "sub-expired"}`} style={{ marginTop: 4 }}>
                {sub.status}
              </span>
            </div>
            <div style={{ textAlign: "right" }}>
              <div style={{ fontSize: 26, fontWeight: 800, color: "#111827" }}>
                {sub.monthlyPricePaise === 0 ? "Free" : money(sub.monthlyPricePaise / 100)}
              </div>
              <div style={{ fontSize: 12, color: "#6b7280" }}>per month</div>
            </div>
          </div>

          <div className="form-grid form-grid-3" style={{ marginBottom: 16 }}>
            <div className="stat-card">
              <div className="stat-label">Invoices this period</div>
              <div className="stat-value sm">{sub.invoiceCountCurrent} / {sub.invoiceLimit === -1 ? "∞" : sub.invoiceLimit}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Period ends</div>
              <div className="stat-value sm">{sub.currentPeriodEnd ? new Date(sub.currentPeriodEnd).toLocaleDateString() : "—"}</div>
            </div>
            {sub.trialEnd && (
              <div className="stat-card">
                <div className="stat-label">Trial ends</div>
                <div className="stat-value sm" style={{ color: "#1d4ed8" }}>{new Date(sub.trialEnd).toLocaleDateString()}</div>
              </div>
            )}
          </div>

          <div style={{ marginBottom: 12 }}>
            <div style={{ fontSize: 11, fontWeight: 600, color: "#6b7280", textTransform: "uppercase", letterSpacing: "0.4px", marginBottom: 8 }}>Features</div>
            <div className="row-gap">
              {(sub.features ?? []).map(f => (
                <span key={f} className="chip" style={{ background: "#d1fae5", color: "#065f46" }}>
                  {f.replace(/_/g, " ")}
                </span>
              ))}
            </div>
          </div>

          {sub.status !== "CANCELLED" && (
            <button onClick={cancel} style={{ fontSize: 12, color: "#b91c1c", background: "none", border: "none", cursor: "pointer", padding: 0, textDecoration: "underline" }}>
              Cancel subscription
            </button>
          )}
        </div>
      )}

      {/* Plans */}
      <div style={{ marginBottom: 20 }}>
        <div style={{ fontSize: 15, fontWeight: 700, marginBottom: 14 }}>Available Plans</div>
        <div className="form-grid form-grid-4">
          {plans.map(p => (
            <div key={p.id} className="card" style={{
              marginBottom: 0,
              border: sub?.planName === p.name ? "2px solid #1d4ed8" : undefined,
              background: sub?.planName === p.name ? "#eff6ff" : undefined,
            }}>
              <div style={{ fontWeight: 700, marginBottom: 4 }}>{p.displayName}</div>
              <div style={{ fontSize: 20, fontWeight: 800, marginBottom: 4 }}>
                {p.monthlyPricePaise === 0 ? "Free" : money(p.monthlyPricePaise / 100)}
                <span style={{ fontSize: 11, fontWeight: 400, color: "#6b7280" }}>/mo</span>
              </div>
              <div style={{ fontSize: 12, color: "#6b7280", marginBottom: 4 }}>
                {p.invoiceLimit === -1 ? "Unlimited" : p.invoiceLimit} invoices/mo
              </div>
              <div style={{ fontSize: 12, color: "#6b7280", marginBottom: 14 }}>
                {p.userLimit === -1 ? "Unlimited" : p.userLimit} users
              </div>
              {sub?.planName !== p.name ? (
                <button className="btn btn-primary" style={{ width: "100%", justifyContent: "center" }}
                  onClick={() => changePlan(p.name)}>Switch</button>
              ) : (
                <div style={{ fontSize: 12, color: "#1d4ed8", fontWeight: 600, textAlign: "center" }}>Current plan</div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Usage history */}
      {usage.length > 0 && (
        <div className="table-section">
          <div className="table-toolbar">
            <span className="table-toolbar-title">Usage History</span>
          </div>
          <table className="data-table">
            <thead>
              <tr>
                <th>Period</th><th>Invoices</th><th>API Calls</th><th>AI Extractions</th>
              </tr>
            </thead>
            <tbody>
              {usage.map(u => (
                <tr key={u.id}>
                  <td>{u.periodStart} – {u.periodEnd}</td>
                  <td>{u.invoiceCount}</td>
                  <td>{u.apiCalls.toLocaleString()}</td>
                  <td>{u.aiExtractions}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
