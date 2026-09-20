"use client";

import {useCallback, useEffect, useRef, useState} from "react";
import Link from "next/link";
import type {Invoice,HistoryEvent,DashboardStats,Line} from "./types";
import {API,money} from "./utils";
import InvoiceTable from "./InvoiceTable";
import ReviewOverlay from "./ReviewOverlay";

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

  async function reprocess(){
    if(!selected)return;
    setSaving(true);setError("");
    try{
      const r=await fetch(API+"/api/v1/invoices/"+selected.id+"/reprocess",{method:"POST"});
      if(!r.ok){const b=await r.json().catch(()=>({}));throw new Error(b.message??"Reprocess failed")}
      const saved=await r.json();setSelected(saved);
      await Promise.all([loadInvoices(),loadStats()]);
      const h=await fetch(API+"/api/v1/invoices/"+saved.id+"/history");
      setEvents(h.ok?await h.json():[]);
    }catch(e){setError(e instanceof Error?e.message:"Reprocess failed")}finally{setSaving(false)}
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

  return <main className="page">
    <header className="header">
      <div><div className="brand">AI Invoice Gateway</div><div className="muted">AI-powered Indian GST invoice processing</div></div>
      <div style={{display:"flex",gap:12,alignItems:"center"}}>
        <Link href="/email" style={{color:"#6b7280",fontSize:13}}>Email Automation</Link>
        <span className="badge">Phase 7</span>
      </div>
    </header>

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

    <InvoiceTable
      invoices={invoices}
      query={query}
      statusFilter={statusFilter}
      onQueryChange={setQuery}
      onStatusChange={setStatusFilter}
      onSelect={openInvoice}
    />

    {selected&&<ReviewOverlay
      selected={selected}
      events={events}
      saving={saving}
      showReject={showReject}
      rejectReason={rejectReason}
      onClose={()=>setSelected(null)}
      onUpdateField={updateField}
      onUpdateLine={updateLine}
      onSaveReview={saveReview}
      onApprove={approve}
      onReject={reject}
      onRejectReasonChange={setRejectReason}
      onShowReject={setShowReject}
      onReprocess={reprocess}
      onExport={(format)=>{if(selected)window.open(API+"/api/v1/invoices/"+selected.id+"/export?format="+format,"_blank")}}
    />}
  </main>;
}
