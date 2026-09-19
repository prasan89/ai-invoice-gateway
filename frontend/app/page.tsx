"use client";

import type {ReactNode} from "react";
import {useEffect, useMemo, useState} from "react";

type Line = {
  id?: string; description: string; quantity: number; unitPrice: number;
  discount: number; taxRate: number; taxAmount: number; lineTotal: number;
};
type Invoice = {
  id:string; invoiceNumber:string; invoiceDate?:string; currency:string;
  supplierName?:string; supplierGstin?:string; customerName?:string; customerGstin?:string;
  subtotal:number; taxAmount:number; totalAmount:number; extractionConfidence?:number;
  status:string; validationMessage?:string; lines:Line[];
  fieldConfidence?:Record<string,number>; sourceFileName?:string; sourceContentType?:string;
  documentUrl?:string;
};

const API=process.env.NEXT_PUBLIC_API_URL??"http://localhost:8080";
const money=(n:number)=>"₹"+Number(n||0).toLocaleString("en-IN",{minimumFractionDigits:2,maximumFractionDigits:2});

function Confidence({value}:{value?:number}) {
  if(value===undefined || value===null) return null;
  const cls=value>=90?"confidence high":value>=70?"confidence medium":"confidence low";
  return <span className={cls}>{Number(value).toFixed(0)}%</span>;
}

export default function Home(){
  const[invoices,setInvoices]=useState<Invoice[]>([]);
  const[file,setFile]=useState<File|null>(null);
  const[selected,setSelected]=useState<Invoice|null>(null);
  const[uploading,setUploading]=useState(false);
  const[saving,setSaving]=useState(false);
  const[error,setError]=useState("");

  async function loadInvoices(){
    const r=await fetch(API+"/api/v1/invoices",{cache:"no-store"});
    if(!r.ok) throw new Error("Could not load invoices");
    setInvoices(await r.json());
  }

  useEffect(()=>{loadInvoices().catch(e=>setError(e.message))},[]);

  async function openInvoice(id:string){
    setError("");
    const r=await fetch(API+"/api/v1/invoices/"+id);
    if(!r.ok) throw new Error("Could not load invoice");
    setSelected(await r.json());
  }

  async function upload(){
    if(!file)return;
    setUploading(true); setError("");
    try{
      const f=new FormData(); f.append("file",file);
      const r=await fetch(API+"/api/v1/invoices",{method:"POST",body:f});
      if(!r.ok){const b=await r.json().catch(()=>({}));throw new Error(b.message??"Upload failed")}
      const created=await r.json();
      setFile(null);
      await loadInvoices();
      setSelected(created);
    }catch(e){setError(e instanceof Error?e.message:"Upload failed")}
    finally{setUploading(false)}
  }

  async function saveReview(){
    if(!selected)return;
    setSaving(true); setError("");
    try{
      const body={
        invoiceNumber:selected.invoiceNumber, invoiceDate:selected.invoiceDate??null,
        currency:selected.currency, supplierName:selected.supplierName??null,
        supplierGstin:selected.supplierGstin??null, customerName:selected.customerName??null,
        customerGstin:selected.customerGstin??null, subtotal:Number(selected.subtotal),
        taxAmount:Number(selected.taxAmount), totalAmount:Number(selected.totalAmount),
        lines:selected.lines
      };
      const r=await fetch(API+"/api/v1/invoices/"+selected.id+"/review",{
        method:"PUT",headers:{"Content-Type":"application/json"},body:JSON.stringify(body)
      });
      if(!r.ok){const b=await r.json().catch(()=>({}));throw new Error(b.message??"Save failed")}
      setSelected(await r.json()); await loadInvoices();
    }catch(e){setError(e instanceof Error?e.message:"Save failed")}
    finally{setSaving(false)}
  }

  async function approve(){
    if(!selected)return;
    setSaving(true);setError("");
    try{
      const r=await fetch(API+"/api/v1/invoices/"+selected.id+"/approve",{method:"POST"});
      if(!r.ok){const b=await r.json().catch(()=>({}));throw new Error(b.message??"Approval failed")}
      setSelected(await r.json()); await loadInvoices();
    }catch(e){setError(e instanceof Error?e.message:"Approval failed")}
    finally{setSaving(false)}
  }

  const total=useMemo(()=>invoices.reduce((s,i)=>s+Number(i.totalAmount||0),0),[invoices]);
  const needsReview=invoices.filter(i=>i.status==="REVIEW_REQUIRED").length;

  function updateField<K extends keyof Invoice>(key:K,value:Invoice[K]){
    setSelected(v=>v?{...v,[key]:value}:v);
  }

  function updateLine(index:number,key:keyof Line,value:string){
    setSelected(v=>{
      if(!v)return v;
      const lines=[...v.lines];
      const old=lines[index];
      const numeric=["quantity","unitPrice","discount","taxRate","taxAmount","lineTotal"].includes(key);
      lines[index]={...old,[key]:numeric?Number(value):value};
      return {...v,lines};
    });
  }

  return <main className="page">
    <header className="header">
      <div><div className="brand">AI Invoice Gateway</div><div className="muted">AI-powered invoice processing</div></div>
      <span className="badge">Phase 2</span>
    </header>

    <section className="cards">
      <div className="card"><div className="muted">Invoices</div><div className="metric">{invoices.length}</div></div>
      <div className="card"><div className="muted">Invoice value</div><div className="metric">{money(total)}</div></div>
      <div className="card"><div className="muted">Needs review</div><div className="metric">{needsReview}</div></div>
    </section>

    <section className="upload">
      <h2>Upload invoice</h2>
      <p className="muted">PDF, PNG or JPG — AI extraction + validation + review.</p>
      <input type="file" accept=".pdf,.png,.jpg,.jpeg" onChange={e=>setFile(e.target.files?.[0]??null)}/>
      <div><button className="btn" disabled={!file||uploading} onClick={upload}>{uploading?"AI processing…":"Upload & Extract"}</button></div>
    </section>

    {error&&<div className="error">{error}</div>}

    <section>
      <h2>Recent invoices</h2>
      <table className="table"><thead><tr><th>Invoice</th><th>Supplier</th><th>Date</th><th>Total</th><th>AI confidence</th><th>Status</th></tr></thead>
      <tbody>{invoices.map(i=><tr key={i.id} className="clickable" onClick={()=>openInvoice(i.id)}>
        <td>{i.invoiceNumber}</td><td>{i.supplierName??"-"}</td><td>{i.invoiceDate??"-"}</td>
        <td>{money(i.totalAmount)}</td><td><Confidence value={i.extractionConfidence}/></td><td><span className="badge">{i.status}</span></td>
      </tr>)}{invoices.length===0&&<tr><td colSpan={6} className="muted">No invoices yet.</td></tr>}</tbody></table>
    </section>

    {selected&&<div className="review-overlay">
      <div className="review">
        <div className="review-header">
          <div><h2>Invoice review</h2><div className="muted">{selected.sourceFileName??selected.invoiceNumber}</div></div>
          <button className="icon-btn" onClick={()=>setSelected(null)}>×</button>
        </div>

        <div className="review-grid">
          <div className="document-panel">
            <div className="panel-title">Original document</div>
            {selected.sourceContentType?.startsWith("image/")?
              <img className="document-image" src={API+(selected.documentUrl??"")} alt="Invoice"/>:
              <iframe className="document-frame" src={API+(selected.documentUrl??"")} title="Invoice document"/>
            }
          </div>

          <div className="form-panel">
            <div className="confidence-summary">
              <span>AI confidence</span><strong>{Number(selected.extractionConfidence??0).toFixed(1)}%</strong>
            </div>

            <Field label="Invoice number" confidence={selected.fieldConfidence?.invoiceNumber}>
              <input value={selected.invoiceNumber??""} onChange={e=>updateField("invoiceNumber",e.target.value)}/>
            </Field>

            <div className="two">
              <Field label="Invoice date" confidence={selected.fieldConfidence?.invoiceDate}>
                <input type="date" value={selected.invoiceDate??""} onChange={e=>updateField("invoiceDate",e.target.value)}/>
              </Field>
              <Field label="Currency" confidence={selected.fieldConfidence?.currency}>
                <input value={selected.currency??""} onChange={e=>updateField("currency",e.target.value)}/>
              </Field>
            </div>

            <Field label="Supplier" confidence={selected.fieldConfidence?.supplierName}>
              <input value={selected.supplierName??""} onChange={e=>updateField("supplierName",e.target.value)}/>
            </Field>
            <Field label="Supplier GSTIN" confidence={selected.fieldConfidence?.supplierGstin}>
              <input value={selected.supplierGstin??""} onChange={e=>updateField("supplierGstin",e.target.value)}/>
            </Field>
            <Field label="Customer" confidence={selected.fieldConfidence?.customerName}>
              <input value={selected.customerName??""} onChange={e=>updateField("customerName",e.target.value)}/>
            </Field>
            <Field label="Customer GSTIN" confidence={selected.fieldConfidence?.customerGstin}>
              <input value={selected.customerGstin??""} onChange={e=>updateField("customerGstin",e.target.value)}/>
            </Field>

            <div className="two">
              <Field label="Subtotal" confidence={selected.fieldConfidence?.subtotal}>
                <input type="number" value={selected.subtotal??0} onChange={e=>updateField("subtotal",Number(e.target.value))}/>
              </Field>
              <Field label="Tax" confidence={selected.fieldConfidence?.taxAmount}>
                <input type="number" value={selected.taxAmount??0} onChange={e=>updateField("taxAmount",Number(e.target.value))}/>
              </Field>
            </div>

            <Field label="Total" confidence={selected.fieldConfidence?.totalAmount}>
              <input type="number" value={selected.totalAmount??0} onChange={e=>updateField("totalAmount",Number(e.target.value))}/>
            </Field>

            <div className="panel-title line-title">Line items</div>
            {selected.lines.map((l,idx)=><div className="line-card" key={l.id??idx}>
              <input className="wide" value={l.description??""} onChange={e=>updateLine(idx,"description",e.target.value)}/>
              <div className="line-fields">
                <input type="number" step="0.0001" value={l.quantity??0} onChange={e=>updateLine(idx,"quantity",e.target.value)} placeholder="Qty"/>
                <input type="number" value={l.unitPrice??0} onChange={e=>updateLine(idx,"unitPrice",e.target.value)} placeholder="Unit price"/>
                <input type="number" value={l.discount??0} onChange={e=>updateLine(idx,"discount",e.target.value)} placeholder="Discount"/>
                <input type="number" value={l.taxRate??0} onChange={e=>updateLine(idx,"taxRate",e.target.value)} placeholder="Tax %"/>
                <input type="number" value={l.taxAmount??0} onChange={e=>updateLine(idx,"taxAmount",e.target.value)} placeholder="Tax"/>
                <input type="number" value={l.lineTotal??0} onChange={e=>updateLine(idx,"lineTotal",e.target.value)} placeholder="Line total"/>
              </div>
            </div>)}

            {selected.validationMessage&&<div className="validation">{selected.validationMessage}</div>}

            <div className="actions">
              <button className="btn secondary" onClick={saveReview} disabled={saving}>{saving?"Saving…":"Save changes"}</button>
              <button className="btn success" onClick={approve} disabled={saving||selected.status==="APPROVED"}>
                {selected.status==="APPROVED"?"Approved":"Approve invoice"}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>}
  </main>
}

function Field({label,confidence,children}:{label:string;confidence?:number;children:ReactNode}){
  return <label className="field"><span>{label} <Confidence value={confidence}/></span>{children}</label>
}
