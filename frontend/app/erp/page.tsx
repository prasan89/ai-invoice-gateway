"use client";
import {useEffect,useState} from "react";
import type {ErpConnection} from "../types";
import {API} from "../utils";

const ERP_SYSTEMS=["TALLY","ZOHO_BOOKS","SAP","GENERIC_REST"];

export default function ErpPage(){
  const[connections,setConnections]=useState<ErpConnection[]>([]);
  const[showAdd,setShowAdd]=useState(false);
  const[form,setForm]=useState({erpSystem:"TALLY",displayName:"",config:"{}"});
  const[msg,setMsg]=useState("");

  useEffect(()=>{
    fetch(API+"/api/v1/erp/connections").then(r=>r.ok?r.json():[]).then(setConnections);
  },[]);

  async function save(){
    let config:Record<string,unknown>={};
    try{config=JSON.parse(form.config)}catch{setMsg("Config must be valid JSON");return;}
    const r=await fetch(API+"/api/v1/erp/connections",{method:"POST",headers:{"Content-Type":"application/json"},
      body:JSON.stringify({erpSystem:form.erpSystem,displayName:form.displayName||form.erpSystem,config})});
    if(r.ok){
      const saved:ErpConnection=await r.json();
      setConnections(prev=>{const exists=prev.find(x=>x.erpSystem===form.erpSystem);return exists?prev.map(x=>x.erpSystem===form.erpSystem?saved:x):[...prev,saved]});
      setShowAdd(false);setMsg("Connection saved.");
    }
    else setMsg("Failed to save connection.");
  }

  const STATUS_COLOR:Record<string,string>={CONNECTED:"bg-green-100 text-green-800",CONFIGURED:"bg-blue-100 text-blue-800",DISCONNECTED:"bg-gray-100 text-gray-600",FAILED:"bg-red-100 text-red-800"};

  return(
    <main className="max-w-4xl mx-auto p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">ERP Integrations</h1>
        <button onClick={()=>setShowAdd(!showAdd)} className="px-4 py-2 bg-blue-600 text-white rounded text-sm hover:bg-blue-700">+ Add Connection</button>
      </div>
      {msg&&<div className="p-3 bg-blue-50 border border-blue-200 rounded text-sm text-blue-800">{msg}</div>}

      {showAdd&&(
        <div className="border rounded-xl p-5 space-y-4 bg-gray-50">
          <h2 className="font-semibold">New ERP Connection</h2>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs text-gray-500 block mb-1">ERP System</label>
              <select value={form.erpSystem} onChange={e=>setForm(f=>({...f,erpSystem:e.target.value}))}
                className="w-full border rounded px-3 py-2 text-sm">
                {ERP_SYSTEMS.map(s=><option key={s} value={s}>{s.replace(/_/g," ")}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-500 block mb-1">Display Name</label>
              <input value={form.displayName} onChange={e=>setForm(f=>({...f,displayName:e.target.value}))}
                placeholder="My Tally Instance" className="w-full border rounded px-3 py-2 text-sm"/>
            </div>
          </div>
          <div>
            <label className="text-xs text-gray-500 block mb-1">Connection Config (JSON)</label>
            <textarea value={form.config} onChange={e=>setForm(f=>({...f,config:e.target.value}))}
              rows={5} className="w-full border rounded px-3 py-2 text-sm font-mono text-xs"/>
            <p className="text-xs text-gray-400 mt-1">
              Tally: {`{"tally_host":"localhost","tally_port":"9000","company":"My Company"}`}<br/>
              Zoho: {`{"zoho_access_token":"...","zoho_organization_id":"..."}`}<br/>
              Generic REST: {`{"rest_endpoint":"https://...","rest_auth_header":"Bearer ..."}`}
            </p>
          </div>
          <div className="flex gap-2">
            <button onClick={save} className="px-4 py-2 bg-blue-600 text-white rounded text-sm hover:bg-blue-700">Save</button>
            <button onClick={()=>setShowAdd(false)} className="px-4 py-2 border rounded text-sm hover:bg-gray-100">Cancel</button>
          </div>
        </div>
      )}

      {connections.length===0?(
        <div className="text-center py-16 text-gray-400">No ERP connections configured. Add one above.</div>
      ):(
        <div className="space-y-3">
          {connections.map(c=>(
            <div key={c.id} className="border rounded-xl p-4 flex items-center justify-between">
              <div>
                <div className="font-semibold">{c.displayName}</div>
                <div className="text-xs text-gray-500">{c.erpSystem}{c.lastSyncedAt?` · Last sync: ${new Date(c.lastSyncedAt).toLocaleString()}`:""}</div>
              </div>
              <span className={`px-2 py-0.5 rounded text-xs font-medium ${STATUS_COLOR[c.status]??'bg-gray-100'}`}>{c.status}</span>
            </div>
          ))}
        </div>
      )}
    </main>
  );
}
