"use client";

import type {ReactNode} from "react";
import {useCallback, useEffect, useRef, useState} from "react";

type Line={id?:string;description:string;hsnSac?:string;quantity?:number;unitPrice?:number;discount?:number;taxableValue?:number;taxRate?:number;taxAmount?:number;cgstRate?:number;cgstAmount?:number;sgstRate?:number;sgstAmount?:number;igstRate?:number;igstAmount?:number;cessRate?:number;cessAmount?:number;lineTotal?:number};
type ValidationResult={field:string|null;status:"PASS"|"FAIL";message:string};
type ArithmeticStatus="PASS"|"FAIL"|"WARN"|"PENDING";
type GstinStatus="VALID"|"INVALID"|"NOT_PROVIDED"|"PENDING";
type Invoice={id:string;invoiceNumber:string;invoiceDate?:string;currency:string;supplierName?:string;supplierGstin?:string;customerName?:string;customerGstin?:string;subtotal:number;taxAmount:number;cgstAmount?:number;sgstAmount?:number;igstAmount?:number;cessAmount?:number;totalAmount:number;extractionConfidence?:number;status:string;validationMessage?:string;lines:Line[];fieldConfidence?:Record<string,number>;sourceFileName?:string;sourceContentType?:string;documentUrl?:string;validationResults?:ValidationResult[];supplierGstinStatus?:GstinStatus;customerGstinStatus?:GstinStatus;arithmeticStatus?:ArithmeticStatus;duplicateScore?:number;duplicateInvoiceId?:string;vendorId?:string;vendorNormalizedName?:string};
type HistoryEvent={id:string;eventType:string;message:string;createdAt:string;actor?:string};
type DashboardStats={totalCount:number;totalValue:number;reviewRequiredCount:number;approvedCount:number;failedCount:number;rejectedCount:number;autoApprovedCount:number;potentialDuplicatesCount:number;averageConfidence:number};

const API=process.env.NEXT_PUBLIC_API_URL??"http://localhost:8080";
const money=(n:number)=>"₹"+Number(n||0).toLocaleString("en-IN",{minimumFractionDigits:2,maximumFractionDigits:2});
const TERMINAL=new Set(["APPROVED","AUTO_APPROVED","REJECTED"]);

function Confidence({value}:{value?:number}){
  if(value===undefined||value===null)return null;
  const cls=value>=90?"confidence high":value>=70?"confidence medium":"confidence low";
  return <span className={cls}>{Number(value).toFixed(0)}%</span>;
}
function Field({label,confidence,children}:{label:string;confidence?:number;children:ReactNode}){
  return <label className="field"><span>{label} <Confidence value={confidence}/></span>{children}</label>;
}
function GstinBadge({status}:{status?:GstinStatus}){
  if(!status||status==="PENDING")return null;
  return <span className={`gstin-badge ${status.toLowerCase()}`}>{status==="NOT_PROVIDED"?"—":status}</span>;
}
function ArithBadge({status}:{status?:ArithmeticStatus}){
  if(!status||status==="PENDING")return null;
  return <span className={`arith-${status.toLowerCase()}`}>Arithmetic: {status}</span>;
}

export default function Home(){
  const[invoices,setInvoices]=useState<Invoice[]>([]);
  const[stats,setStats]=useState<DashboardStats|null>(null);
  const[file,setFile]=useState<File|null>(null);
  const[selected,setSelected]=useState<Invoice|null>(null);
  const[events,setEvents]=useState<HistoryEvent[]>([]);
  const[uploading,setUploading]=useState(false);
  const[saving,setSaving]=useState(false);
  const[error,setError]=useState("");
  const[query,setQuery]=useState("");
  const[statusFilter,setStatusFilter]=useState("ALL");
  const[rejectReason,setRejectReason]=useState("");
  const[showReject,setShowReject]=useState(false);
  const debounceRef=useRef<ReturnType<typeof setTimeout>|null>(null);
  const mountedRef=useRef(false);

  const loadStats=useCallback(async()=>{
    try{const r=await fetch(API+"/api/v1/invoices/stats",{cache:"no-store"});if(r.ok)setStats(await r.json())}catch{/* silent */}
  },[]);

  const loadInvoices=useCallback(async(q?:string,sf?:string)=>{
    const params=new URLSearchParams();
    const qs=(q??query).trim();
    const sfs=sf??statusFilter;
    if(qs)params.set("search",qs);
    if(sfs&&sfs!=="ALL")params.set("status",sfs);
    const url=API+"/api/v1/invoices"+(params.toString()?"?"+params.toString():"");
    const r=await fetch(url,{cache:"no-store"});
    if(!r.ok)throw new Error("Could not load invoices");
    setInvoices(await r.json());
  },[query,statusFilter]);

  useEffect(()=>{
    Promise.all([loadInvoices(),loadStats()])
      .catch(e=>setError(e instanceof Error?e.message:"Load failed"))
      .finally(()=>{mountedRef.current=true});
  },[]);// eslint-disable-line react-hooks/exhaustive-deps

  // Debounced search — skip on first mount
  useEffect(()=>{
    if(!mountedRef.current)return;
    if(debounceRef.current)clearTimeout(debounceRef.current);
    debounceRef.current=setTimeout(()=>{
      loadInvoices(query,statusFilter).catch(e=>setError(e instanceof Error?e.message:"Search failed"));
    },300);
    return()=>{if(debounceRef.current)clearTimeout(debounceRef.current)};
  },[query,statusFilter]);// eslint-disable-line react-hooks/exhaustive-deps

  async function openInvoice(id:string){
    setError("");
    try{
      const[r,h]=await Promise.all([fetch(API+"/api/v1/invoices/"+id),fetch(API+"/api/v1/invoices/"+id+"/history")]);
      if(!r.ok)throw new Error("Could not load invoice");
      setSelected(await r.json());
      setEvents(h.ok?await h.json():[]);
      setShowReject(false);setRejectReason("");
    }catch(e){setError(e instanceof Error?e.message:"Could not load invoice")}
  }

  async function upload(){
    if(!file)return;
    setUploading(true);setError("");
    try{
      const f=new FormData();f.append("file",file);
      const r=await fetch(API+"/api/v1/invoices",{method:"POST",body:f});
      if(!r.ok){const b=await r.json().catch(()=>({}));throw new Error(b.message??"Upload failed")}
      const created=await r.json();
      setFile(null);
      await Promise.all([loadInvoices(),loadStats()]);
      setSelected(created);
      const h=await fetch(API+"/api/v1/invoices/"+created.id+"/history");
      setEvents(h.ok?await h.json():[]);
    }catch(e){setError(e instanceof Error?e.message:"Upload failed")}finally{setUploading(false)}
  }

  async function saveReview(){
    if(!selected)return;
    setSaving(true);setError("");
    try{
      const body={invoiceNumber:selected.invoiceNumber,invoiceDate:selected.invoiceDate??null,currency:selected.currency,supplierName:selected.supplierName??null,supplierGstin:selected.supplierGstin??null,customerName:selected.customerName??null,customerGstin:selected.customerGstin??null,subtotal:Number(selected.subtotal),taxAmount:Number(selected.taxAmount),cgstAmount:Number(selected.cgstAmount??0),sgstAmount:Number(selected.sgstAmount??0),igstAmount:Number(selected.igstAmount??0),cessAmount:Number(selected.cessAmount??0),totalAmount:Number(selected.totalAmount),lines:selected.lines};
      const r=await fetch(API+"/api/v1/invoices/"+selected.id+"/review",{method:"PUT",headers:{"Content-Type":"application/json"},body:JSON.stringify(body)});
      if(!r.ok){const b=await r.json().catch(()=>({}));throw new Error(b.message??"Save failed")}
      const saved=await r.json();setSelected(saved);
      await Promise.all([loadInvoices(),loadStats()]);
      const h=await fetch(API+"/api/v1/invoices/"+saved.id+"/history");
      setEvents(h.ok?await h.json():[]);
    }catch(e){setError(e instanceof Error?e.message:"Save failed")}finally{setSaving(false)}
  }

  async function approve(){
    if(!selected)return;
    setSaving(true);setError("");
    try{
      const r=await fetch(API+"/api/v1/invoices/"+selected.id+"/approve",{method:"POST"});
      if(!r.ok){const b=await r.json().catch(()=>({}));throw new Error(b.message??"Approval failed")}
      const saved=await r.json();setSelected(saved);
      await Promise.all([loadInvoices(),loadStats()]);
      const h=await fetch(API+"/api/v1/invoices/"+saved.id+"/history");
      setEvents(h.ok?await h.json():[]);
    }catch(e){setError(e instanceof Error?e.message:"Approval failed")}finally{setSaving(false)}
  }

  async function reject(){
    if(!selected)return;
    setSaving(true);setError("");
    try{
      const r=await fetch(API+"/api/v1/invoices/"+selected.id+"/reject",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({reason:rejectReason||null})});
      if(!r.ok){const b=await r.json().catch(()=>({}));throw new Error(b.message??"Reject failed")}
      const saved=await r.json();setSelected(saved);setShowReject(false);setRejectReason("");
      await Promise.all([loadInvoices(),loadStats()]);
      const h=await fetch(API+"/api/v1/invoices/"+saved.id+"/history");
      setEvents(h.ok?await h.json():[]);
    }catch(e){setError(e instanceof Error?e.message:"Reject failed")}finally{setSaving(false)}
  }

  function exportAs(format:"csv"|"json"){
    if(selected)window.open(API+"/api/v1/invoices/"+selected.id+"/export?format="+format,"_blank");
  }

  function updateField<K extends keyof Invoice>(key:K,value:Invoice[K]){
    setSelected(v=>v?{...v,[key]:value}:v);
  }
  function updateLine(index:number,key:keyof Line,value:string){
    setSelected(v=>{
      if(!v)return v;
      const lines=[...v.lines],old=lines[index];
      const numeric=["quantity","unitPrice","discount","taxableValue","taxRate","taxAmount","cgstRate","cgstAmount","sgstRate","sgstAmount","igstRate","igstAmount","cessRate","cessAmount","lineTotal"].includes(key);
      lines[index]={...old,[key]:numeric?(value===""?undefined:Number(value)):value};
      return{...v,lines};
    });
  }

    const isTerminal=selected?TERMINAL.has(selected.status):false;
  const dupScore=selected?.duplicateScore??0;
  const dupLabel=dupScore>=90?"CONFIRMED":dupScore>=60?"POTENTIAL":null;
  const hasDuplicate=dupScore>=60;

  // Compute which gates are blocking auto-approval (for REVIEW_REQUIRED invoices)
  const autoApprovalBlocks:string[]=[];
  if(selected&&selected.status==="REVIEW_REQUIRED"){
    if((selected.extractionConfidence??0)<95)autoApprovalBlocks.push(`Confidence ${Number(selected.extractionConfidence??0).toFixed(0)}% < 95%`);
    if(selected.arithmeticStatus&&selected.arithmeticStatus!=="PASS")autoApprovalBlocks.push("Arithmetic check failed");
    if(selected.supplierGstinStatus&&selected.supplierGstinStatus!=="VALID")autoApprovalBlocks.push(`Supplier GSTIN ${selected.supplierGstinStatus}`);
    if(selected.customerGstinStatus&&selected.customerGstinStatus==="INVALID")autoApprovalBlocks.push(`Customer GSTIN ${selected.customerGstinStatus}`);
    if(dupScore>=80)autoApprovalBlocks.push(`Duplicate score ${dupScore}/100`);
    if((selected.totalAmount??0)>=500000)autoApprovalBlocks.push("Invoice total ≥ ₹5,00,000 (manual review required)");
  }

  const passCount=selected?.validationResults?.filter(r=>r.status==="PASS").length??0;
  const failCount=selected?.validationResults?.filter(r=>r.status==="FAIL").length??0;

  return <main className="page">
    <header className="header">
      <div><div className="brand">AI Invoice Gateway</div><div className="muted">AI-powered Indian GST invoice processing</div></div>
      <span className="badge">Phase 4</span>
    </header>

    {/* 8 KPI cards — fills 4×2 grid */}
    <section className="cards cards-7">
      <div className="card"><div className="muted">Total invoices</div><div className="metric">{stats?.totalCount??invoices.length}</div></div>
      <div className="card"><div className="muted">Total value</div><div className="metric metric-sm">{money(stats?.totalValue??0)}</div></div>
      <div className="card card-warn"><div className="muted">Needs review</div><div className="metric">{stats?.reviewRequiredCount??0}</div></div>
      <div className="card card-ok"><div className="muted">Approved</div><div className="metric">{stats?.approvedCount??0}</div></div>
      <div className="card card-ok"><div className="muted">Auto-approved</div><div className="metric">{stats?.autoApprovedCount??0}</div></div>
      <div className="card card-err"><div className="muted">Failed/Rejected</div><div className="metric">{(stats?.failedCount??0)+(stats?.rejectedCount??0)}</div></div>
      <div className="card card-dup"><div className="muted">Duplicates</div><div className="metric">{stats?.potentialDuplicatesCount??0}</div></div>
      <div className="card"><div className="muted">Avg confidence</div><div className="metric">{stats?Number(stats.averageConfidence).toFixed(1)+"%":"—"}</div></div>
    </section>

    <section className="upload">
      <h2>Upload invoice</h2>
      <p className="muted">PDF, PNG or JPG — AI extraction + GSTIN validation + arithmetic check + duplicate detection.</p>
      <input type="file" accept=".pdf,.png,.jpg,.jpeg" onChange={e=>setFile(e.target.files?.[0]??null)}/>
      <div><button className="btn" disabled={!file||uploading} onClick={upload}>{uploading?"AI processing…":"Upload & Extract"}</button></div>
    </section>

    {error&&<div className="error">{error}</div>}

    <section>
      <div className="section-toolbar">
        <h2>Invoices</h2>
        <div className="filters">
          <input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Search invoice, supplier, GSTIN…"/>
          <select value={statusFilter} onChange={e=>setStatusFilter(e.target.value)}>
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
          {invoices.map(i=><tr key={i.id} className="clickable" onClick={()=>openInvoice(i.id)}>
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
    </section>

    {selected&&<div className="review-overlay"><div className="review">
      <div className="review-header">
        <div>
          <h2>Invoice review — <span className={`badge badge-${selected.status.toLowerCase()}`}>{selected.status.replace("_"," ")}</span></h2>
          <div className="muted">{selected.sourceFileName??selected.invoiceNumber}</div>
        </div>
        <div className="header-actions">
          <button className="btn secondary" onClick={()=>exportAs("csv")}>Export CSV</button>
          <button className="btn secondary" onClick={()=>exportAs("json")}>Export JSON</button>
          <button className="icon-btn" onClick={()=>setSelected(null)}>×</button>
        </div>
      </div>

      {hasDuplicate&&<div className={`duplicate-warning dup-${dupLabel?.toLowerCase()}`}>
        {dupLabel==="CONFIRMED"?"🚫 Confirmed duplicate":"⚠ Potential duplicate"} — score {selected.duplicateScore}/100
        {dupLabel==="CONFIRMED"?" (same document or same invoice identity — blocked)":" (strong similarity — requires review)"}
        {selected.duplicateInvoiceId&&<> · matches <code>{selected.duplicateInvoiceId.slice(0,8)}…</code></>}
      </div>}

      {autoApprovalBlocks.length>0&&<div className="review-blocked">
        <span className="review-blocked-title">Auto-approval blocked by:</span>
        {autoApprovalBlocks.map((b,i)=><span key={i} className="review-blocked-item">{b}</span>)}
      </div>}

      <div className="review-grid">
        <div className="document-panel">
          <div className="panel-title">Original document</div>
          {selected.sourceContentType?.startsWith("image/")
            ?<img className="document-image" src={API+(selected.documentUrl??"")} alt="Invoice"/>
            :<iframe className="document-frame" src={API+(selected.documentUrl??"")} title="Invoice document"/>}
        </div>

        <div className="form-panel">
          {/* Confidence summary */}
          <div className="confidence-summary">
            <div><span>AI confidence</span><strong> {Number(selected.extractionConfidence??0).toFixed(1)}%</strong></div>
            <div style={{display:"flex",gap:8,alignItems:"center"}}>
              {selected.arithmeticStatus&&<ArithBadge status={selected.arithmeticStatus}/>}
              {selected.vendorNormalizedName&&<span className="vendor-info">Vendor: {selected.vendorNormalizedName}</span>}
            </div>
          </div>

          {/* Structured validation results */}
          {selected.validationResults&&selected.validationResults.length>0&&<div className="validation-results">
            <div className="section-heading">Validation — {passCount} passed, {failCount} failed</div>
            {selected.validationResults.map((vr,i)=><div key={i} className={`val-rule ${vr.status.toLowerCase()}`}>
              <span className="val-icon">{vr.status==="PASS"?"✓":"✗"}</span>
              {vr.field&&<span className="val-field">{vr.field}</span>}
              <span className="val-msg">{vr.message}</span>
            </div>)}
          </div>}

          {/* Invoice details */}
          <div className="review-section">
            <div className="section-heading">Invoice details</div>
            <Field label="Invoice number" confidence={selected.fieldConfidence?.invoiceNumber}><input value={selected.invoiceNumber??""} onChange={e=>updateField("invoiceNumber",e.target.value)} readOnly={isTerminal}/></Field>
            <div className="two">
              <Field label="Invoice date" confidence={selected.fieldConfidence?.invoiceDate}><input type="date" value={selected.invoiceDate??""} onChange={e=>updateField("invoiceDate",e.target.value)} readOnly={isTerminal}/></Field>
              <Field label="Currency" confidence={selected.fieldConfidence?.currency}><input value={selected.currency??""} onChange={e=>updateField("currency",e.target.value)} readOnly={isTerminal}/></Field>
            </div>
            <Field label="Supplier" confidence={selected.fieldConfidence?.supplierName}><input value={selected.supplierName??""} onChange={e=>updateField("supplierName",e.target.value)} readOnly={isTerminal}/></Field>
            <div className="gstin-row">
              <Field label="Supplier GSTIN" confidence={selected.fieldConfidence?.supplierGstin}><input value={selected.supplierGstin??""} onChange={e=>updateField("supplierGstin",e.target.value)} readOnly={isTerminal}/></Field>
              <GstinBadge status={selected.supplierGstinStatus}/>
            </div>
            <Field label="Customer" confidence={selected.fieldConfidence?.customerName}><input value={selected.customerName??""} onChange={e=>updateField("customerName",e.target.value)} readOnly={isTerminal}/></Field>
            <div className="gstin-row">
              <Field label="Customer GSTIN" confidence={selected.fieldConfidence?.customerGstin}><input value={selected.customerGstin??""} onChange={e=>updateField("customerGstin",e.target.value)} readOnly={isTerminal}/></Field>
              <GstinBadge status={selected.customerGstinStatus}/>
            </div>
          </div>

          {/* Totals */}
          <div className="review-section">
            <div className="section-heading">GST &amp; totals</div>
            <div className="two">
              <Field label="Taxable subtotal" confidence={selected.fieldConfidence?.taxableSubtotal??selected.fieldConfidence?.subtotal}><input type="number" value={selected.subtotal??0} onChange={e=>updateField("subtotal",Number(e.target.value))} readOnly={isTerminal}/></Field>
              <Field label="Total tax" confidence={selected.fieldConfidence?.taxAmount}><input type="number" value={selected.taxAmount??0} onChange={e=>updateField("taxAmount",Number(e.target.value))} readOnly={isTerminal}/></Field>
              <Field label="CGST" confidence={selected.fieldConfidence?.cgstAmount}><input type="number" value={selected.cgstAmount??0} onChange={e=>updateField("cgstAmount",Number(e.target.value))} readOnly={isTerminal}/></Field>
              <Field label="SGST" confidence={selected.fieldConfidence?.sgstAmount}><input type="number" value={selected.sgstAmount??0} onChange={e=>updateField("sgstAmount",Number(e.target.value))} readOnly={isTerminal}/></Field>
              <Field label="IGST" confidence={selected.fieldConfidence?.igstAmount}><input type="number" value={selected.igstAmount??0} onChange={e=>updateField("igstAmount",Number(e.target.value))} readOnly={isTerminal}/></Field>
              <Field label="Cess" confidence={selected.fieldConfidence?.cessAmount}><input type="number" value={selected.cessAmount??0} onChange={e=>updateField("cessAmount",Number(e.target.value))} readOnly={isTerminal}/></Field>
            </div>
            <Field label="Grand total" confidence={selected.fieldConfidence?.totalAmount}><input type="number" value={selected.totalAmount??0} onChange={e=>updateField("totalAmount",Number(e.target.value))} readOnly={isTerminal}/></Field>
          </div>

          {/* Lines */}
          <div className="panel-title line-title">Line items</div>
          {selected.lines.map((l,idx)=><div className="line-card" key={l.id??idx}>
            <input className="wide" value={l.description??""} onChange={e=>updateLine(idx,"description",e.target.value)} placeholder="Description" readOnly={isTerminal}/>
            <div className="line-fields">
              <input value={l.hsnSac??""} onChange={e=>updateLine(idx,"hsnSac",e.target.value)} placeholder="HSN/SAC" readOnly={isTerminal}/>
              <input type="number" step="0.0001" value={l.quantity??""} onChange={e=>updateLine(idx,"quantity",e.target.value)} placeholder="Qty" readOnly={isTerminal}/>
              <input type="number" value={l.unitPrice??""} onChange={e=>updateLine(idx,"unitPrice",e.target.value)} placeholder="Unit price" readOnly={isTerminal}/>
              <input type="number" value={l.discount??""} onChange={e=>updateLine(idx,"discount",e.target.value)} placeholder="Discount" readOnly={isTerminal}/>
              <input type="number" value={l.taxableValue??""} onChange={e=>updateLine(idx,"taxableValue",e.target.value)} placeholder="Taxable value" readOnly={isTerminal}/>
              <input type="number" value={l.taxRate??""} onChange={e=>updateLine(idx,"taxRate",e.target.value)} placeholder="GST %" readOnly={isTerminal}/>
              <input type="number" value={l.cgstRate??""} onChange={e=>updateLine(idx,"cgstRate",e.target.value)} placeholder="CGST %" readOnly={isTerminal}/>
              <input type="number" value={l.cgstAmount??""} onChange={e=>updateLine(idx,"cgstAmount",e.target.value)} placeholder="CGST" readOnly={isTerminal}/>
              <input type="number" value={l.sgstRate??""} onChange={e=>updateLine(idx,"sgstRate",e.target.value)} placeholder="SGST %" readOnly={isTerminal}/>
              <input type="number" value={l.sgstAmount??""} onChange={e=>updateLine(idx,"sgstAmount",e.target.value)} placeholder="SGST" readOnly={isTerminal}/>
              <input type="number" value={l.igstRate??""} onChange={e=>updateLine(idx,"igstRate",e.target.value)} placeholder="IGST %" readOnly={isTerminal}/>
              <input type="number" value={l.igstAmount??""} onChange={e=>updateLine(idx,"igstAmount",e.target.value)} placeholder="IGST" readOnly={isTerminal}/>
              <input type="number" value={l.cessAmount??""} onChange={e=>updateLine(idx,"cessAmount",e.target.value)} placeholder="Cess" readOnly={isTerminal}/>
              <input type="number" value={l.taxAmount??""} onChange={e=>updateLine(idx,"taxAmount",e.target.value)} placeholder="Total tax" readOnly={isTerminal}/>
              <input type="number" value={l.lineTotal??""} onChange={e=>updateLine(idx,"lineTotal",e.target.value)} placeholder="Line total" readOnly={isTerminal}/>
            </div>
          </div>)}

          {/* Reject dialog */}
          {showReject&&<div className="reject-dialog">
            <div className="section-heading">Reject reason</div>
            <textarea value={rejectReason} onChange={e=>setRejectReason(e.target.value)} placeholder="Enter reason for rejection (optional)" rows={3}/>
            <div className="reject-actions">
              <button className="btn" onClick={()=>setShowReject(false)}>Cancel</button>
              <button className="btn danger" onClick={reject} disabled={saving}>{saving?"Rejecting…":"Confirm reject"}</button>
            </div>
          </div>}

          {/* Action bar */}
          <div className="actions">
            {!isTerminal&&<>
              <button className="btn secondary" onClick={saveReview} disabled={saving}>{saving?"Saving…":"Save changes"}</button>
              <button className="btn success" onClick={approve} disabled={saving}>{saving?"Approving…":"Approve"}</button>
              <button className="btn danger" onClick={()=>setShowReject(true)} disabled={saving||showReject}>Reject</button>
            </>}
            {isTerminal&&<div className="muted">Invoice is {selected.status.replace("_"," ").toLowerCase()} — no further changes allowed.</div>}
          </div>

          {/* Audit history */}
          <div className="history">
            <div className="section-heading">Audit history</div>
            {events.length===0?<div className="muted">No events yet.</div>:events.map(e=><div className="event" key={e.id}>
              <div><strong>{e.eventType}</strong>{e.actor&&e.actor!=="SYSTEM"&&<span className="event-actor"> by {e.actor}</span>}<div>{e.message}</div></div>
              <time>{new Date(e.createdAt).toLocaleString()}</time>
            </div>)}
          </div>
        </div>
      </div>
    </div></div>}
  </main>;
}
