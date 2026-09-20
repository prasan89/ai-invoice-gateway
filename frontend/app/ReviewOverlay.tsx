"use client";

import type {Invoice,HistoryEvent,Line} from "./types";
import {Confidence,Field,GstinBadge,ArithBadge} from "./components";
import {API,money,TERMINAL} from "./utils";

interface Props{
  selected:Invoice;
  events:HistoryEvent[];
  saving:boolean;
  showReject:boolean;
  rejectReason:string;
  onClose:()=>void;
  onUpdateField:<K extends keyof Invoice>(key:K,value:Invoice[K])=>void;
  onUpdateLine:(index:number,key:keyof Line,value:string)=>void;
  onSaveReview:()=>void;
  onApprove:()=>void;
  onReject:()=>void;
  onRejectReasonChange:(r:string)=>void;
  onShowReject:(show:boolean)=>void;
  onReprocess:()=>void;
  onExport:(format:"csv"|"json")=>void;
}

const NUMERIC_LINE_FIELDS=new Set(["quantity","unitPrice","discount","taxableValue","taxRate","taxAmount","cgstRate","cgstAmount","sgstRate","sgstAmount","igstRate","igstAmount","cessRate","cessAmount","lineTotal"]);

export default function ReviewOverlay(props:Props){
  const{selected,events,saving,showReject,rejectReason,onClose,onUpdateField,onUpdateLine,onSaveReview,onApprove,onReject,onRejectReasonChange,onShowReject,onReprocess,onExport}=props;

  const isTerminal=TERMINAL.has(selected.status);
  const dupScore=selected.duplicateScore??0;
  const dupLabel=dupScore>=90?"CONFIRMED":dupScore>=60?"POTENTIAL":null;
  const hasDuplicate=dupScore>=60;

  const autoApprovalBlocks:string[]=[];
  if(selected.status==="REVIEW_REQUIRED"){
    if((selected.extractionConfidence??0)<95)autoApprovalBlocks.push(`Confidence ${Number(selected.extractionConfidence??0).toFixed(0)}% < 95%`);
    if(selected.arithmeticStatus==="FAIL")autoApprovalBlocks.push("Arithmetic check failed");
    if(selected.supplierGstinStatus&&selected.supplierGstinStatus!=="VALID")autoApprovalBlocks.push(`Supplier GSTIN ${selected.supplierGstinStatus}`);
    if(selected.customerGstinStatus==="INVALID")autoApprovalBlocks.push(`Customer GSTIN ${selected.customerGstinStatus}`);
    if(dupScore>=80)autoApprovalBlocks.push(`Duplicate score ${dupScore}/100`);
    if((selected.totalAmount??0)>=500000)autoApprovalBlocks.push("Invoice total ≥ ₹5,00,000 (manual review required)");
  }

  const passCount=selected.validationResults?.filter(r=>r.status==="PASS").length??0;
  const failCount=selected.validationResults?.filter(r=>r.status==="FAIL").length??0;

  return <div className="review-overlay"><div className="review">
    <div className="review-header">
      <div>
        <h2>Invoice review — <span className={`badge badge-${selected.status.toLowerCase()}`}>{selected.status.replace("_"," ")}</span></h2>
        <div className="muted">{selected.sourceFileName??selected.invoiceNumber}</div>
      </div>
      <div className="header-actions">
        <button className="btn secondary" onClick={()=>onExport("csv")}>Export CSV</button>
        <button className="btn secondary" onClick={()=>onExport("json")}>Export JSON</button>
        <button className="icon-btn" onClick={onClose}>×</button>
      </div>
    </div>

    {hasDuplicate&&<div className={`duplicate-warning dup-${dupLabel?.toLowerCase()}`}>
      {dupLabel==="CONFIRMED"?"🚫 Confirmed duplicate":"⚠ Potential duplicate"} — score {selected.duplicateScore}/100
      {dupLabel==="CONFIRMED"?" (same document or same invoice identity — blocked)":" (strong similarity — requires review)"}
      {selected.duplicateInvoiceId&&<> · matches <code>{selected.duplicateInvoiceId.slice(0,8)}…</code></>}
      {selected.duplicateReason&&<> · <em>{selected.duplicateReason}</em></>}
    </div>}

    {autoApprovalBlocks.length>0&&<div className="review-blocked">
      <span className="review-blocked-title">Auto-approval blocked by:</span>
      {autoApprovalBlocks.map((b,i)=><span key={i} className="review-blocked-item">{b}</span>)}
    </div>}

    <div className="review-grid">
      <div className="document-panel">
        <div className="panel-title">Original document</div>
        {selected.sourceContentType?.startsWith("image/")
          // eslint-disable-next-line @next/next/no-img-element
          ?<img className="document-image" src={API+(selected.documentUrl??"")} alt="Invoice"/>
          :<iframe className="document-frame" src={API+(selected.documentUrl??"")} title="Invoice document"/>}
      </div>

      <div className="form-panel">
        <div className="confidence-summary">
          <div><span>AI confidence</span><strong> {Number(selected.extractionConfidence??0).toFixed(1)}%</strong></div>
          <div style={{display:"flex",gap:8,alignItems:"center"}}>
            {selected.arithmeticStatus&&<ArithBadge status={selected.arithmeticStatus}/>}
            {selected.vendorNormalizedName&&<span className="vendor-info">Vendor: {selected.vendorNormalizedName}</span>}
            {selected.vendorAnomalyFlag&&<span className="vendor-anomaly">⚠ Unusual amount for this vendor</span>}
            {selected.vendorRiskTier&&selected.vendorRiskTier!=="NORMAL"&&<span className={`risk-tier risk-${selected.vendorRiskTier.toLowerCase()}`}>{selected.vendorRiskTier} RISK</span>}
            {selected.vendorTypicalGstRate!=null&&<span className="vendor-info">Typical GST: {selected.vendorTypicalGstRate.toFixed(1)}%</span>}
            {selected.poMatchStatus&&<span className={`po-match po-match-${selected.poMatchStatus.toLowerCase()}`}>PO: {selected.poMatchStatus.replace("_"," ")}</span>}
          </div>
        </div>

        {selected.validationResults&&selected.validationResults.length>0&&<div className="validation-results">
          <div className="section-heading">Validation — {passCount} passed, {failCount} failed</div>
          {selected.validationResults.map((vr,i)=><div key={i} className={`val-rule ${vr.status.toLowerCase()}`}>
            <span className="val-icon">{vr.status==="PASS"?"✓":"✗"}</span>
            {vr.field&&<span className="val-field">{vr.field}</span>}
            <span className="val-msg">{vr.message}</span>
          </div>)}
        </div>}

        <div className="review-section">
          <div className="section-heading">Invoice details</div>
          <Field label="Invoice number" confidence={selected.fieldConfidence?.invoiceNumber}><input value={selected.invoiceNumber??""} onChange={e=>onUpdateField("invoiceNumber",e.target.value)} readOnly={isTerminal}/></Field>
          <div className="two">
            <Field label="Invoice date" confidence={selected.fieldConfidence?.invoiceDate}><input type="date" value={selected.invoiceDate??""} onChange={e=>onUpdateField("invoiceDate",e.target.value)} readOnly={isTerminal}/></Field>
            <Field label="Currency" confidence={selected.fieldConfidence?.currency}><input value={selected.currency??""} onChange={e=>onUpdateField("currency",e.target.value)} readOnly={isTerminal}/></Field>
          </div>
          <Field label="Supplier" confidence={selected.fieldConfidence?.supplierName}><input value={selected.supplierName??""} onChange={e=>onUpdateField("supplierName",e.target.value)} readOnly={isTerminal}/></Field>
          <div className="gstin-row">
            <Field label="Supplier GSTIN" confidence={selected.fieldConfidence?.supplierGstin}><input value={selected.supplierGstin??""} onChange={e=>onUpdateField("supplierGstin",e.target.value)} readOnly={isTerminal}/></Field>
            <GstinBadge status={selected.supplierGstinStatus}/>
          </div>
          {(selected.supplierLegalName||selected.supplierPortalStatus)&&<div className="portal-info">
            {selected.supplierPortalStatus&&<span className={`portal-status portal-${selected.supplierPortalStatus.toLowerCase()}`}>{selected.supplierPortalStatus}</span>}
            {selected.supplierLegalName&&<span className="portal-legal-name">{selected.supplierLegalName}</span>}
            {selected.supplierTradeName&&selected.supplierTradeName!==selected.supplierLegalName&&<span className="portal-trade-name">({selected.supplierTradeName})</span>}
          </div>}
          <Field label="Customer" confidence={selected.fieldConfidence?.customerName}><input value={selected.customerName??""} onChange={e=>onUpdateField("customerName",e.target.value)} readOnly={isTerminal}/></Field>
          <div className="gstin-row">
            <Field label="Customer GSTIN" confidence={selected.fieldConfidence?.customerGstin}><input value={selected.customerGstin??""} onChange={e=>onUpdateField("customerGstin",e.target.value)} readOnly={isTerminal}/></Field>
            <GstinBadge status={selected.customerGstinStatus}/>
          </div>
        </div>

        <div className="review-section">
          <div className="section-heading">GST &amp; totals</div>
          <div className="two">
            <Field label="Taxable subtotal" confidence={selected.fieldConfidence?.taxableSubtotal??selected.fieldConfidence?.subtotal}><input type="number" value={selected.subtotal??0} onChange={e=>onUpdateField("subtotal",Number(e.target.value))} readOnly={isTerminal}/></Field>
            <Field label="Total tax" confidence={selected.fieldConfidence?.taxAmount}><input type="number" value={selected.taxAmount??0} onChange={e=>onUpdateField("taxAmount",Number(e.target.value))} readOnly={isTerminal}/></Field>
            <Field label="CGST" confidence={selected.fieldConfidence?.cgstAmount}><input type="number" value={selected.cgstAmount??0} onChange={e=>onUpdateField("cgstAmount",Number(e.target.value))} readOnly={isTerminal}/></Field>
            <Field label="SGST" confidence={selected.fieldConfidence?.sgstAmount}><input type="number" value={selected.sgstAmount??0} onChange={e=>onUpdateField("sgstAmount",Number(e.target.value))} readOnly={isTerminal}/></Field>
            <Field label="IGST" confidence={selected.fieldConfidence?.igstAmount}><input type="number" value={selected.igstAmount??0} onChange={e=>onUpdateField("igstAmount",Number(e.target.value))} readOnly={isTerminal}/></Field>
            <Field label="Cess" confidence={selected.fieldConfidence?.cessAmount}><input type="number" value={selected.cessAmount??0} onChange={e=>onUpdateField("cessAmount",Number(e.target.value))} readOnly={isTerminal}/></Field>
          </div>
          <Field label="Grand total" confidence={selected.fieldConfidence?.totalAmount}><input type="number" value={selected.totalAmount??0} onChange={e=>onUpdateField("totalAmount",Number(e.target.value))} readOnly={isTerminal}/></Field>
        </div>

        <div className="panel-title line-title">Line items</div>
        {selected.lines.map((l,idx)=><div className="line-card" key={l.id??idx}>
          <input className="wide" value={l.description??""} onChange={e=>onUpdateLine(idx,"description",e.target.value)} placeholder="Description" readOnly={isTerminal}/>
          <div className="line-fields">
            <input value={l.hsnSac??""} onChange={e=>onUpdateLine(idx,"hsnSac",e.target.value)} placeholder="HSN/SAC" readOnly={isTerminal}/>
            <input type="number" step="0.0001" value={l.quantity??""} onChange={e=>onUpdateLine(idx,"quantity",e.target.value)} placeholder="Qty" readOnly={isTerminal}/>
            <input type="number" value={l.unitPrice??""} onChange={e=>onUpdateLine(idx,"unitPrice",e.target.value)} placeholder="Unit price" readOnly={isTerminal}/>
            <input type="number" value={l.discount??""} onChange={e=>onUpdateLine(idx,"discount",e.target.value)} placeholder="Discount" readOnly={isTerminal}/>
            <input type="number" value={l.taxableValue??""} onChange={e=>onUpdateLine(idx,"taxableValue",e.target.value)} placeholder="Taxable value" readOnly={isTerminal}/>
            <input type="number" value={l.taxRate??""} onChange={e=>onUpdateLine(idx,"taxRate",e.target.value)} placeholder="GST %" readOnly={isTerminal}/>
            <input type="number" value={l.cgstRate??""} onChange={e=>onUpdateLine(idx,"cgstRate",e.target.value)} placeholder="CGST %" readOnly={isTerminal}/>
            <input type="number" value={l.cgstAmount??""} onChange={e=>onUpdateLine(idx,"cgstAmount",e.target.value)} placeholder="CGST" readOnly={isTerminal}/>
            <input type="number" value={l.sgstRate??""} onChange={e=>onUpdateLine(idx,"sgstRate",e.target.value)} placeholder="SGST %" readOnly={isTerminal}/>
            <input type="number" value={l.sgstAmount??""} onChange={e=>onUpdateLine(idx,"sgstAmount",e.target.value)} placeholder="SGST" readOnly={isTerminal}/>
            <input type="number" value={l.igstRate??""} onChange={e=>onUpdateLine(idx,"igstRate",e.target.value)} placeholder="IGST %" readOnly={isTerminal}/>
            <input type="number" value={l.igstAmount??""} onChange={e=>onUpdateLine(idx,"igstAmount",e.target.value)} placeholder="IGST" readOnly={isTerminal}/>
            <input type="number" value={l.cessAmount??""} onChange={e=>onUpdateLine(idx,"cessAmount",e.target.value)} placeholder="Cess" readOnly={isTerminal}/>
            <input type="number" value={l.taxAmount??""} onChange={e=>onUpdateLine(idx,"taxAmount",e.target.value)} placeholder="Total tax" readOnly={isTerminal}/>
            <input type="number" value={l.lineTotal??""} onChange={e=>onUpdateLine(idx,"lineTotal",e.target.value)} placeholder="Line total" readOnly={isTerminal}/>
          </div>
        </div>)}

        {showReject&&<div className="reject-dialog">
          <div className="section-heading">Reject reason</div>
          <textarea value={rejectReason} onChange={e=>onRejectReasonChange(e.target.value)} placeholder="Enter reason for rejection (optional)" rows={3}/>
          <div className="reject-actions">
            <button className="btn" onClick={()=>onShowReject(false)}>Cancel</button>
            <button className="btn danger" onClick={onReject} disabled={saving}>{saving?"Rejecting…":"Confirm reject"}</button>
          </div>
        </div>}

        <div className="actions">
          {selected.status==="FAILED"&&<>
            <button className="btn secondary" onClick={onReprocess} disabled={saving}>{saving?"Processing…":"Reprocess"}</button>
          </>}
          {!isTerminal&&selected.status!=="FAILED"&&<>
            <button className="btn secondary" onClick={onSaveReview} disabled={saving}>{saving?"Saving…":"Save changes"}</button>
            <button className="btn success" onClick={onApprove} disabled={saving}>{saving?"Approving…":"Approve"}</button>
            <button className="btn danger" onClick={()=>onShowReject(true)} disabled={saving||showReject}>Reject</button>
          </>}
          {isTerminal&&<div className="muted">Invoice is {selected.status.replace("_"," ").toLowerCase()} — no further changes allowed.</div>}
        </div>

        <div className="history">
          <div className="section-heading">Audit history</div>
          {events.length===0?<div className="muted">No events yet.</div>:events.map(e=><div className="event" key={e.id}>
            <div><strong>{e.eventType}</strong>{e.actor&&e.actor!=="SYSTEM"&&<span className="event-actor"> by {e.actor}</span>}<div>{e.message}</div></div>
            <time>{new Date(e.createdAt).toLocaleString()}</time>
          </div>)}
        </div>
      </div>
    </div>
  </div></div>;
}
