"use client";

import type { Invoice } from "./types";
import { Confidence, ArithBadge, GstinBadge } from "./components";
import { money } from "./utils";

interface Props {
  invoices: Invoice[];
  query: string;
  statusFilter: string;
  onQueryChange: (q: string) => void;
  onStatusChange: (s: string) => void;
  onSelect: (id: string) => void;
}

export default function InvoiceTable({ invoices, query, statusFilter, onQueryChange, onStatusChange, onSelect }: Props) {
  return (
    <div className="table-section">
      <div className="table-toolbar">
        <span className="table-toolbar-title">Invoices <span style={{ color: "#9ca3af", fontWeight: 400, fontSize: 13 }}>({invoices.length})</span></span>
        <div className="table-filters">
          <input
            value={query}
            onChange={e => onQueryChange(e.target.value)}
            placeholder="Search invoice, supplier, GSTIN…"
          />
          <select value={statusFilter} onChange={e => onStatusChange(e.target.value)}>
            <option value="ALL">All statuses</option>
            <option value="REVIEW_REQUIRED">Review required</option>
            <option value="APPROVED">Approved</option>
            <option value="AUTO_APPROVED">Auto-approved</option>
            <option value="REJECTED">Rejected</option>
            <option value="FAILED">Failed</option>
            <option value="PROCESSING">Processing</option>
          </select>
        </div>
      </div>

      <table className="data-table">
        <thead>
          <tr>
            <th>Invoice</th>
            <th>Supplier</th>
            <th>Date</th>
            <th>Total</th>
            <th>Confidence</th>
            <th>Arithmetic</th>
            <th>GSTIN</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {invoices.map(i => (
            <tr key={i.id} onClick={() => onSelect(i.id)}>
              <td>
                <span className="invoice-num">{i.invoiceNumber || "Unidentified"}</span>
                {(i.duplicateScore ?? 0) >= 90 && (
                  <span className="dup-dot" title="Confirmed duplicate (score ≥ 90)">🚫</span>
                )}
                {(i.duplicateScore ?? 0) >= 60 && (i.duplicateScore ?? 0) < 90 && (
                  <span className="dup-dot" title={`Potential duplicate (score ${i.duplicateScore}/100)`}>⚠</span>
                )}
              </td>
              <td>{i.supplierName ?? "—"}</td>
              <td style={{ whiteSpace: "nowrap", color: "#6b7280" }}>{i.invoiceDate ?? "—"}</td>
              <td style={{ fontWeight: 600 }}>{money(i.totalAmount)}</td>
              <td><Confidence value={i.extractionConfidence} /></td>
              <td>{i.arithmeticStatus ? <ArithBadge status={i.arithmeticStatus} /> : "—"}</td>
              <td><GstinBadge status={i.supplierGstinStatus} /></td>
              <td>
                <span className={`badge badge-${i.status.toLowerCase()}`}>
                  {i.status.replace(/_/g, " ")}
                </span>
              </td>
            </tr>
          ))}
          {invoices.length === 0 && (
            <tr className="empty-row">
              <td colSpan={8}>No matching invoices.</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
