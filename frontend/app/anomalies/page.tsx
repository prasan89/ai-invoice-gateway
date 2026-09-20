"use client";
import {useEffect,useState} from "react";
import type {Anomaly} from "../types";
import {API} from "../utils";

const LEVEL_COLOR:Record<string,string>={LOW:"bg-gray-100 text-gray-700",MEDIUM:"bg-yellow-100 text-yellow-800",HIGH:"bg-orange-100 text-orange-800",CRITICAL:"bg-red-100 text-red-800"};

export default function AnomaliesPage(){
  const[anomalies,setAnomalies]=useState<Anomaly[]>([]);
  const[loading,setLoading]=useState(true);
  const[msg,setMsg]=useState("");

  useEffect(()=>{
    fetch(API+"/api/v1/anomalies").then(r=>r.ok?r.json():[]).then(setAnomalies).finally(()=>setLoading(false));
  },[]);

  async function review(id:string,outcome:string){
    const r=await fetch(API+"/api/v1/anomalies/"+id+"/review",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({outcome})});
    if(r.ok){
      const updated=await r.json();
      setAnomalies(prev=>prev.map(a=>a.id===id?updated:a));
      setMsg("Reviewed as "+outcome);
    }
  }

  if(loading)return <div className="p-8 text-gray-500">Loading anomalies...</div>;

  const stats={critical:anomalies.filter(a=>a.riskLevel==="CRITICAL").length,high:anomalies.filter(a=>a.riskLevel==="HIGH").length,unreviewed:anomalies.filter(a=>!a.reviewOutcome).length};

  return(
    <main className="max-w-5xl mx-auto p-6 space-y-6">
      <h1 className="text-2xl font-bold">AI Anomaly Detection</h1>
      {msg&&<div className="p-3 bg-blue-50 border border-blue-200 rounded text-sm text-blue-800">{msg}</div>}

      <div className="grid grid-cols-3 gap-4">
        <div className="border rounded-xl p-4"><div className="text-2xl font-bold text-red-600">{stats.critical}</div><div className="text-sm text-gray-500">Critical</div></div>
        <div className="border rounded-xl p-4"><div className="text-2xl font-bold text-orange-600">{stats.high}</div><div className="text-sm text-gray-500">High Risk</div></div>
        <div className="border rounded-xl p-4"><div className="text-2xl font-bold">{stats.unreviewed}</div><div className="text-sm text-gray-500">Unreviewed</div></div>
      </div>

      {anomalies.length===0?(
        <div className="text-center py-16 text-gray-400">No anomaly records yet. Process some invoices to see risk analysis.</div>
      ):(
        <div className="space-y-3">
          {anomalies.map(a=>(
            <div key={a.id} className="border rounded-xl p-4 space-y-3">
              <div className="flex items-start justify-between gap-4">
                <div className="space-y-1 flex-1">
                  <div className="flex items-center gap-2">
                    <span className={`px-2 py-0.5 rounded text-xs font-bold ${LEVEL_COLOR[a.riskLevel]??'bg-gray-100'}`}>{a.riskLevel}</span>
                    <span className="text-sm font-mono text-gray-400">Score: {a.riskScore}/100</span>
                    {a.reviewOutcome&&<span className="px-2 py-0.5 rounded text-xs bg-purple-100 text-purple-800">{a.reviewOutcome}</span>}
                  </div>
                  <div className="text-xs text-gray-400">Invoice: {a.invoiceId} · Detected: {new Date(a.detectedAt).toLocaleString()}</div>
                  <div className="flex flex-wrap gap-1">
                    {a.anomalyTypes.map(t=><span key={t} className="px-1.5 py-0.5 bg-red-50 text-red-700 text-xs rounded">{t.replace(/_/g," ")}</span>)}
                  </div>
                </div>
                <div className="w-16 h-16 flex-shrink-0">
                  <svg viewBox="0 0 36 36" className="rotate-[-90deg]">
                    <circle cx="18" cy="18" r="15" fill="none" stroke="#e5e7eb" strokeWidth="4"/>
                    <circle cx="18" cy="18" r="15" fill="none"
                      stroke={a.riskScore>=75?"#ef4444":a.riskScore>=50?"#f97316":a.riskScore>=25?"#eab308":"#22c55e"}
                      strokeWidth="4" strokeDasharray={`${a.riskScore*0.942} 94.2`}/>
                  </svg>
                </div>
              </div>
              <ul className="text-sm text-gray-600 space-y-0.5 pl-1">
                {a.reasons.map((r,i)=><li key={i} className="flex gap-2"><span className="text-red-400">•</span>{r}</li>)}
              </ul>
              {!a.reviewOutcome&&(
                <div className="flex gap-2 pt-1">
                  <button onClick={()=>review(a.id,"FALSE_POSITIVE")} className="text-xs px-3 py-1 border rounded hover:bg-gray-50">False Positive</button>
                  <button onClick={()=>review(a.id,"CONFIRMED")} className="text-xs px-3 py-1 border border-red-200 text-red-700 rounded hover:bg-red-50">Confirm Anomaly</button>
                  <button onClick={()=>review(a.id,"ESCALATED")} className="text-xs px-3 py-1 border border-orange-200 text-orange-700 rounded hover:bg-orange-50">Escalate</button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </main>
  );
}
