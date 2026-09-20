"use client";
import { useEffect, useState } from "react";
import type { ErpConnection } from "../types";
import { API } from "../utils";

const ERP_SYSTEMS = ["TALLY", "ZOHO_BOOKS", "SAP", "GENERIC_REST"];

export default function ErpPage() {
  const [connections, setConnections] = useState<ErpConnection[]>([]);
  const [showAdd, setShowAdd]         = useState(false);
  const [form, setForm]               = useState({ erpSystem: "TALLY", displayName: "", config: "{}" });
  const [msg, setMsg]                 = useState("");

  useEffect(() => {
    fetch(API + "/api/v1/erp/connections").then(r => r.ok ? r.json() : []).then(setConnections);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  async function save() {
    let config: Record<string, unknown> = {};
    try { config = JSON.parse(form.config); } catch { setMsg("Config must be valid JSON"); return; }
    const r = await fetch(API + "/api/v1/erp/connections", {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ erpSystem: form.erpSystem, displayName: form.displayName || form.erpSystem, config }),
    });
    if (r.ok) {
      const saved: ErpConnection = await r.json();
      setConnections(prev => {
        const exists = prev.find(x => x.erpSystem === form.erpSystem);
        return exists ? prev.map(x => x.erpSystem === form.erpSystem ? saved : x) : [...prev, saved];
      });
      setShowAdd(false); setMsg("Connection saved.");
    } else setMsg("Failed to save connection.");
  }

  const STATUS_CLS: Record<string, string> = {
    CONNECTED: "status-connected", CONFIGURED: "status-configured",
    DISCONNECTED: "status-disconnected", FAILED: "status-failed",
  };

  const CONFIG_EXAMPLES: Record<string, string> = {
    TALLY:       `{"tally_host":"localhost","tally_port":"9000","company":"My Company"}`,
    ZOHO_BOOKS:  `{"zoho_access_token":"...","zoho_organization_id":"..."}`,
    SAP:         `{"sap_host":"https://...","sap_client":"100","sap_username":"...","sap_password":"..."}`,
    GENERIC_REST:`{"rest_endpoint":"https://...","rest_auth_header":"Bearer ..."}`,
  };

  return (
    <div className="page-content">
      <div className="page-header">
        <div className="row-between">
          <div>
            <div className="page-title">ERP Integrations</div>
            <div className="page-subtitle">Connect Tally, Zoho Books, SAP, or any REST endpoint</div>
          </div>
          <button className="btn btn-primary" onClick={() => setShowAdd(!showAdd)}>
            {showAdd ? "Cancel" : "+ Add Connection"}
          </button>
        </div>
      </div>

      {msg && <div className="page-notice page-notice-info">{msg}</div>}

      {/* Add form */}
      {showAdd && (
        <div className="card">
          <div className="card-title">New ERP Connection</div>
          <div className="form-grid form-grid-2" style={{ marginBottom: 14 }}>
            <div>
              <label className="field-label">ERP System</label>
              <select className="field-select" value={form.erpSystem}
                onChange={e => setForm(f => ({ ...f, erpSystem: e.target.value, config: CONFIG_EXAMPLES[e.target.value] ?? "{}" }))}>
                {ERP_SYSTEMS.map(s => <option key={s} value={s}>{s.replace(/_/g, " ")}</option>)}
              </select>
            </div>
            <div>
              <label className="field-label">Display Name</label>
              <input className="field-input" value={form.displayName}
                onChange={e => setForm(f => ({ ...f, displayName: e.target.value }))}
                placeholder="My Tally Instance" />
            </div>
          </div>
          <div style={{ marginBottom: 16 }}>
            <label className="field-label">Connection Config (JSON)</label>
            <textarea className="field-textarea" rows={5} value={form.config}
              onChange={e => setForm(f => ({ ...f, config: e.target.value }))} />
            <div className="field-hint">Edit the example config above with your actual credentials</div>
          </div>
          <div className="row-gap">
            <button className="btn btn-primary" onClick={save}>Save Connection</button>
            <button className="btn btn-secondary" onClick={() => setShowAdd(false)}>Cancel</button>
          </div>
        </div>
      )}

      {/* Connection list */}
      {connections.length === 0 ? (
        <div className="empty-state">No ERP connections configured. Click &quot;Add Connection&quot; above.</div>
      ) : (
        <div className="table-section">
          <div className="table-toolbar">
            <span className="table-toolbar-title">
              Connections <span style={{ color: "#9ca3af", fontWeight: 400, fontSize: 13 }}>({connections.length})</span>
            </span>
          </div>
          <table className="data-table">
            <thead>
              <tr>
                <th>System</th><th>Display Name</th><th>Last Sync</th><th>Status</th>
              </tr>
            </thead>
            <tbody>
              {connections.map(c => (
                <tr key={c.id}>
                  <td><span style={{ fontFamily: "monospace", fontSize: 12 }}>{c.erpSystem}</span></td>
                  <td style={{ fontWeight: 600 }}>{c.displayName}</td>
                  <td style={{ color: "#6b7280", fontSize: 12 }}>
                    {c.lastSyncedAt ? new Date(c.lastSyncedAt).toLocaleString() : "Never"}
                  </td>
                  <td>
                    <span className={`chip ${STATUS_CLS[c.status] ?? "status-disconnected"}`}>{c.status}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
