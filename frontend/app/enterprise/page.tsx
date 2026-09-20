"use client";
import {useEffect,useState} from "react";
import {API} from "../utils";

type SsoConfig={id?:string;provider:string;entityId?:string;metadataUrl?:string;clientId?:string;issuer?:string;enabled:boolean};
type Chain={id:string;name:string;description?:string;minAmount?:number;maxAmount?:number;active:boolean};
type Retention={invoiceRetentionDays:number;auditRetentionDays:number;storageRetentionDays:number};

export default function EnterprisePage(){
  const[tab,setTab]=useState<"sso"|"approval"|"retention">("sso");
  const[sso,setSso]=useState<SsoConfig>({provider:"OIDC",enabled:false});
  const[chains,setChains]=useState<Chain[]>([]);
  const[retention,setRetention]=useState<Retention>({invoiceRetentionDays:2555,auditRetentionDays:2555,storageRetentionDays:2555});
  const[msg,setMsg]=useState("");

  useEffect(()=>{
    fetch(API+"/api/v1/enterprise/sso").then(r=>r.ok?r.json():null).then(d=>{if(d)setSso(d)});
    fetch(API+"/api/v1/enterprise/approval-chains").then(r=>r.ok?r.json():[]).then(setChains);
    fetch(API+"/api/v1/enterprise/retention").then(r=>r.ok?r.json():null).then(d=>{if(d)setRetention(d)});
  },[]);

  async function saveSso(){
    const r=await fetch(API+"/api/v1/enterprise/sso",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(sso)});
    if(r.ok){setSso(await r.json());setMsg("SSO configuration saved.");}else setMsg("Failed.");
  }

  async function saveRetention(){
    const r=await fetch(API+"/api/v1/enterprise/retention",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(retention)});
    if(r.ok){setRetention(await r.json());setMsg("Retention policy saved.");}else setMsg("Failed.");
  }

  const TABS:{key:"sso"|"approval"|"retention";label:string}[]=[{key:"sso",label:"SSO"},{key:"approval",label:"Approval Chains"},{key:"retention",label:"Data Retention"}];

  return(
    <main className="max-w-3xl mx-auto p-6 space-y-6">
      <h1 className="text-2xl font-bold">Enterprise Settings</h1>
      {msg&&<div className="p-3 bg-blue-50 border border-blue-200 rounded text-sm text-blue-800">{msg}</div>}

      <div className="flex gap-2 border-b">
        {TABS.map(t=>(
          <button key={t.key} onClick={()=>setTab(t.key)}
            className={`px-4 py-2 text-sm border-b-2 transition-colors ${tab===t.key?"border-blue-600 text-blue-600 font-medium":"border-transparent text-gray-500 hover:text-gray-800"}`}>
            {t.label}
          </button>
        ))}
      </div>

      {tab==="sso"&&(
        <div className="space-y-4">
          <h2 className="font-semibold">Single Sign-On (SSO)</h2>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs text-gray-500 block mb-1">Provider</label>
              <select value={sso.provider} onChange={e=>setSso(s=>({...s,provider:e.target.value}))} className="w-full border rounded px-3 py-2 text-sm">
                <option value="OIDC">OIDC (Google, Azure AD, Okta)</option>
                <option value="SAML">SAML 2.0</option>
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-500 block mb-1">Issuer / Entity ID</label>
              <input value={sso.issuer||""} onChange={e=>setSso(s=>({...s,issuer:e.target.value}))} placeholder="https://accounts.google.com" className="w-full border rounded px-3 py-2 text-sm"/>
            </div>
            <div>
              <label className="text-xs text-gray-500 block mb-1">Client ID</label>
              <input value={sso.clientId||""} onChange={e=>setSso(s=>({...s,clientId:e.target.value}))} className="w-full border rounded px-3 py-2 text-sm"/>
            </div>
            <div>
              <label className="text-xs text-gray-500 block mb-1">Metadata URL</label>
              <input value={sso.metadataUrl||""} onChange={e=>setSso(s=>({...s,metadataUrl:e.target.value}))} className="w-full border rounded px-3 py-2 text-sm"/>
            </div>
          </div>
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input type="checkbox" checked={sso.enabled} onChange={e=>setSso(s=>({...s,enabled:e.target.checked}))} className="w-4 h-4"/>
            Enable SSO (users will be redirected to your IdP on login)
          </label>
          <button onClick={saveSso} className="px-4 py-2 bg-blue-600 text-white rounded text-sm hover:bg-blue-700">Save SSO Config</button>
        </div>
      )}

      {tab==="approval"&&(
        <div className="space-y-4">
          <h2 className="font-semibold">Approval Chains</h2>
          <p className="text-sm text-gray-500">Configure multi-step approval workflows for invoices above certain amounts.</p>
          {chains.length===0?<div className="text-gray-400 text-sm py-8 text-center">No approval chains configured. Use the API to create one.</div>:(
            <div className="space-y-2">
              {chains.map(c=>(
                <div key={c.id} className="border rounded-xl p-4">
                  <div className="font-semibold">{c.name}</div>
                  {c.description&&<div className="text-sm text-gray-500">{c.description}</div>}
                  <div className="text-xs text-gray-400 mt-1">
                    {c.minAmount!=null?`₹${c.minAmount.toLocaleString('en-IN')} – `:""}{c.maxAmount!=null?`₹${c.maxAmount.toLocaleString('en-IN')}`:"unlimited"}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {tab==="retention"&&(
        <div className="space-y-4">
          <h2 className="font-semibold">Data Retention Policy</h2>
          <p className="text-sm text-gray-500">Set how long data is retained (minimum 7 years recommended for Indian GST compliance).</p>
          <div className="grid grid-cols-3 gap-4">
            {([["Invoice Records","invoiceRetentionDays"],["Audit Trail","auditRetentionDays"],["File Storage","storageRetentionDays"]] as [string,keyof Retention][]).map(([label,key])=>(
              <div key={key}>
                <label className="text-xs text-gray-500 block mb-1">{label} (days)</label>
                <input type="number" min={365} value={retention[key]} onChange={e=>setRetention(r=>({...r,[key]:parseInt(e.target.value)||2555}))}
                  className="w-full border rounded px-3 py-2 text-sm"/>
                <div className="text-xs text-gray-400 mt-0.5">{Math.round(retention[key]/365 * 10)/10} years</div>
              </div>
            ))}
          </div>
          <button onClick={saveRetention} className="px-4 py-2 bg-blue-600 text-white rounded text-sm hover:bg-blue-700">Save Policy</button>
        </div>
      )}
    </main>
  );
}
