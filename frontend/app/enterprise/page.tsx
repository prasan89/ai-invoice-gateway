"use client";
import { useEffect, useState } from "react";
import { API } from "../utils";

type SsoConfig = { id?: string; provider: string; entityId?: string; metadataUrl?: string; clientId?: string; issuer?: string; enabled: boolean };
type Chain = { id: string; name: string; description?: string; minAmount?: number; maxAmount?: number; active: boolean };
type Retention = { invoiceRetentionDays: number; auditRetentionDays: number; storageRetentionDays: number };

export default function EnterprisePage() {
  const [tab, setTab]           = useState<"sso" | "approval" | "retention">("sso");
  const [sso, setSso]           = useState<SsoConfig>({ provider: "OIDC", enabled: false });
  const [chains, setChains]     = useState<Chain[]>([]);
  const [retention, setRetention] = useState<Retention>({ invoiceRetentionDays: 2555, auditRetentionDays: 2555, storageRetentionDays: 2555 });
  const [msg, setMsg]           = useState("");

  useEffect(() => {
    fetch(API + "/api/v1/enterprise/sso").then(r => r.ok ? r.json() : null).then(d => { if (d) setSso(d); });
    fetch(API + "/api/v1/enterprise/approval-chains").then(r => r.ok ? r.json() : []).then(setChains);
    fetch(API + "/api/v1/enterprise/retention").then(r => r.ok ? r.json() : null).then(d => { if (d) setRetention(d); });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  async function saveSso() {
    const r = await fetch(API + "/api/v1/enterprise/sso", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(sso) });
    if (r.ok) { setSso(await r.json()); setMsg("SSO configuration saved."); } else setMsg("Failed to save.");
  }

  async function saveRetention() {
    const r = await fetch(API + "/api/v1/enterprise/retention", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(retention) });
    if (r.ok) { setRetention(await r.json()); setMsg("Retention policy saved."); } else setMsg("Failed to save.");
  }

  const TABS = [
    { key: "sso" as const, label: "Single Sign-On" },
    { key: "approval" as const, label: "Approval Chains" },
    { key: "retention" as const, label: "Data Retention" },
  ];

  return (
    <div className="page-content">
      <div className="page-header">
        <div className="page-title">Enterprise Settings</div>
        <div className="page-subtitle">SSO, approval workflows, and data retention policies</div>
      </div>

      {msg && <div className="page-notice page-notice-info">{msg}</div>}

      <div className="tab-bar">
        {TABS.map(t => (
          <button key={t.key} className={`tab-btn${tab === t.key ? " active" : ""}`} onClick={() => setTab(t.key)}>
            {t.label}
          </button>
        ))}
      </div>

      {/* SSO */}
      {tab === "sso" && (
        <div className="card">
          <div className="card-title">Single Sign-On (SSO)</div>
          <div className="card-sub">Allow users to log in with your corporate identity provider</div>
          <div className="form-grid form-grid-2" style={{ marginBottom: 14 }}>
            <div>
              <label className="field-label">Provider</label>
              <select className="field-select" value={sso.provider} onChange={e => setSso(s => ({ ...s, provider: e.target.value }))}>
                <option value="OIDC">OIDC (Google, Azure AD, Okta)</option>
                <option value="SAML">SAML 2.0</option>
              </select>
            </div>
            <div>
              <label className="field-label">Issuer / Entity ID</label>
              <input className="field-input" value={sso.issuer ?? ""} onChange={e => setSso(s => ({ ...s, issuer: e.target.value }))} placeholder="https://accounts.google.com" />
            </div>
            <div>
              <label className="field-label">Client ID</label>
              <input className="field-input" value={sso.clientId ?? ""} onChange={e => setSso(s => ({ ...s, clientId: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">Metadata URL</label>
              <input className="field-input" value={sso.metadataUrl ?? ""} onChange={e => setSso(s => ({ ...s, metadataUrl: e.target.value }))} />
            </div>
          </div>
          <label style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13, cursor: "pointer", marginBottom: 16 }}>
            <input type="checkbox" checked={sso.enabled} onChange={e => setSso(s => ({ ...s, enabled: e.target.checked }))} style={{ width: 16, height: 16 }} />
            Enable SSO — users will be redirected to your IdP on login
          </label>
          <button className="btn btn-primary" onClick={saveSso}>Save SSO Config</button>
        </div>
      )}

      {/* Approval chains */}
      {tab === "approval" && (
        <div className="card">
          <div className="card-title">Approval Chains</div>
          <div className="card-sub">Configure multi-step approval workflows for invoices above certain amounts</div>
          {chains.length === 0 ? (
            <div className="empty-state" style={{ padding: "32px 0" }}>No approval chains configured. Use the API to create one.</div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              {chains.map(c => (
                <div key={c.id} style={{ background: "#f9fafb", border: "1px solid #e5e7eb", borderRadius: 8, padding: "12px 16px" }}>
                  <div style={{ fontWeight: 600, marginBottom: 2 }}>{c.name}</div>
                  {c.description && <div style={{ fontSize: 12, color: "#6b7280" }}>{c.description}</div>}
                  <div style={{ fontSize: 11, color: "#9ca3af", marginTop: 4 }}>
                    {c.minAmount != null ? `₹${c.minAmount.toLocaleString("en-IN")} – ` : ""}
                    {c.maxAmount != null ? `₹${c.maxAmount.toLocaleString("en-IN")}` : "unlimited"}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Retention */}
      {tab === "retention" && (
        <div className="card">
          <div className="card-title">Data Retention Policy</div>
          <div className="card-sub">Minimum 7 years (2555 days) recommended for Indian GST compliance</div>
          <div className="form-grid form-grid-3" style={{ marginBottom: 16 }}>
            {([
              ["Invoice Records", "invoiceRetentionDays"],
              ["Audit Trail",     "auditRetentionDays"],
              ["File Storage",    "storageRetentionDays"],
            ] as [string, keyof Retention][]).map(([label, key]) => (
              <div key={key}>
                <label className="field-label">{label} (days)</label>
                <input type="number" min={365} className="field-input"
                  value={retention[key]}
                  onChange={e => setRetention(r => ({ ...r, [key]: parseInt(e.target.value) || 2555 }))} />
                <div className="field-hint">{Math.round(retention[key] / 365 * 10) / 10} years</div>
              </div>
            ))}
          </div>
          <button className="btn btn-primary" onClick={saveRetention}>Save Policy</button>
        </div>
      )}
    </div>
  );
}
