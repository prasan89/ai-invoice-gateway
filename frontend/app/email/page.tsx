"use client";

import {useEffect,useState} from "react";
import Link from "next/link";
import type {EmailPollLog} from "../types";
import {API} from "../utils";

export default function EmailPage(){
  const[logs,setLogs]=useState<EmailPollLog[]>([]);
  const[triggering,setTriggering]=useState(false);
  const[err,setErr]=useState<string|null>(null);

  const load=async()=>{
    try{
      const r=await fetch(API+"/api/v1/email-poll/logs");
      if(r.ok)setLogs(await r.json());
    }catch(e){setErr("Could not load poll logs")}
  };

  useEffect(()=>{load();},[]);

  const trigger=async()=>{
    setTriggering(true);
    try{
      await fetch(API+"/api/v1/email-poll/trigger",{method:"POST"});
      await load();
    }finally{setTriggering(false);}
  };

  return <div className="page">
    <div className="header">
      <div className="brand">Email Invoice Automation</div>
      <Link href="/" style={{color:"#6b7280",fontSize:14}}>← Back to invoices</Link>
    </div>

    <div className="card" style={{marginBottom:24}}>
      <div style={{fontWeight:700,marginBottom:8}}>IMAP configuration</div>
      <div className="muted" style={{fontSize:13,marginBottom:12}}>
        Configure via environment variables: <code>INVOICE_EMAIL_ENABLED=true</code>, <code>INVOICE_EMAIL_HOST</code>, <code>INVOICE_EMAIL_USER</code>, <code>INVOICE_EMAIL_PASSWORD</code>, <code>INVOICE_EMAIL_FOLDER</code> (default: INBOX). The poller runs every 5 minutes and extracts PDF/image attachments as invoices.
      </div>
      <button className="btn" onClick={trigger} disabled={triggering}>
        {triggering?"Polling…":"Trigger manual poll"}
      </button>
    </div>

    {err&&<div className="error">{err}</div>}

    <div style={{fontWeight:700,marginBottom:12}}>Recent poll logs</div>
    {logs.length===0?<div className="muted">No poll logs yet. Enable email polling and trigger a run.</div>
    :<table className="table">
      <thead><tr>
        <th>Time</th><th>Mailbox</th><th>Found</th><th>Created</th><th>Errors</th><th>Status</th>
      </tr></thead>
      <tbody>{logs.map(l=><tr key={l.id}>
        <td>{new Date(l.polledAt).toLocaleString()}</td>
        <td>{l.mailboxUser??"-"}</td>
        <td>{l.messagesFound}</td>
        <td>{l.invoicesCreated}</td>
        <td>{l.errors}</td>
        <td><span className={`email-status-${l.status.toLowerCase()}`}>{l.status}</span>
          {l.errorDetail&&<div className="muted" style={{fontSize:11,marginTop:2}}>{l.errorDetail}</div>}
        </td>
      </tr>)}</tbody>
    </table>}
  </div>;
}
