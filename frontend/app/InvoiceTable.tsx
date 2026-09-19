"use client";

import type {Invoice} from "./types";
import {Confidence,ArithBadge,GstinBadge} from "./components";
import {money} from "./utils";

interface Props{
  invoices:Invoice[];
  query:string;
  statusFilter:string;
  onQueryChange:(q:string)=>void;
  onStatusChange:(s:string)=>void;
  onSelect:(id:string)=>void;
}

export default function InvoiceTable({invoices,query,statusFilter,onQueryChange,onStatusChange,onSelect}:Props){
  return <section>
    <div className="section-toolbar">
      <h2>Invoices</h2>
      <div className="filters">
        <input value={query} onChange={e=>onQueryChange(e.target.value)} placeholder="Search invoice, supplier, GSTIN…"/>
        <select value={statusFilter} onChange={e=>onStatusChange(e.target.value)}>
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
    <table className="table">
      <thead><tr><th>Invoice</th><th>Supplier</th><th>Date</th><th>Total</th><th>Confidence</th><th>Arith</th><th>GSTIN</th><th>Status</th></tr></thead>
      <tbody>
        {invoices.map(i=><tr key={i.id} className="clickable" onClick={()=>onSelect(i.id)}>
          <td>{i.invoiceNumber||"Unidentified"}{(i.duplicateScore??0)>=90&&<span className="dup-dot dup-confirmed" title="Confirmed duplicate (score ≥ 90)">🚫</span>}{(i.duplicateScore??0)>=60&&(i.duplicateScore??0)<90&&<span className="dup-dot" title={`Potential duplicate (score ${i.duplicateScore}/100)`}>⚠</span>}</td>
          <td>{i.supplierName??"-"}</td>
          <td>{i.invoiceDate??"-"}</td>
          <td>{money(i.totalAmount)}</td>
          <td><Confidence value={i.extractionConfidence}/></td>
          <td>{i.arithmeticStatus?<ArithBadge status={i.arithmeticStatus}/>:"-"}</td>
          <td><GstinBadge status={i.supplierGstinStatus}/></td>
          <td><span className={`badge badge-${i.status.toLowerCase()}`}>{i.status.replace("_"," ")}</span></td>
        </tr>)}
        {invoices.length===0&&<tr><td colSpan={8} className="muted">No matching invoices.</td></tr>}
      </tbody>
    </table>
  </section>;
}
