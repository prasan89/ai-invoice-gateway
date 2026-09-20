"use client";
import {useEffect,useState} from "react";
import type {Subscription,SubscriptionPlan,UsageRecord} from "../types";
import {API,money} from "../utils";

export default function BillingPage(){
  const[sub,setSub]=useState<Subscription|null>(null);
  const[plans,setPlans]=useState<SubscriptionPlan[]>([]);
  const[usage,setUsage]=useState<UsageRecord[]>([]);
  const[loading,setLoading]=useState(true);
  const[msg,setMsg]=useState("");

  useEffect(()=>{
    Promise.all([
      fetch(API+"/api/v1/billing/subscription").then(r=>r.json()),
      fetch(API+"/api/v1/billing/plans").then(r=>r.json()),
      fetch(API+"/api/v1/billing/usage").then(r=>r.json()),
    ]).then(([s,p,u])=>{setSub(s);setPlans(p);setUsage(u)}).finally(()=>setLoading(false));
  },[]);

  async function changePlan(planName:string){
    setMsg("");
    const r=await fetch(API+"/api/v1/billing/subscription/plan",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({planName})});
    if(r.ok){setSub(await r.json());setMsg("Plan updated successfully.")}
    else setMsg("Failed to change plan.");
  }

  async function cancel(){
    if(!confirm("Cancel subscription? You will be downgraded at end of billing period."))return;
    await fetch(API+"/api/v1/billing/subscription/cancel",{method:"POST"});
    setMsg("Subscription cancelled.");
    const r=await fetch(API+"/api/v1/billing/subscription");
    if(r.ok)setSub(await r.json());
  }

  if(loading)return <div className="p-8 text-gray-500">Loading billing...</div>;

  const statusColor:Record<string,string>={TRIALING:"bg-blue-100 text-blue-800",ACTIVE:"bg-green-100 text-green-800",PAST_DUE:"bg-yellow-100 text-yellow-800",CANCELLED:"bg-red-100 text-red-800",EXPIRED:"bg-gray-100 text-gray-800"};

  return(
    <main className="max-w-4xl mx-auto p-6 space-y-8">
      <h1 className="text-2xl font-bold">Billing & Subscription</h1>
      {msg&&<div className="p-3 bg-blue-50 border border-blue-200 rounded text-blue-800 text-sm">{msg}</div>}

      {sub&&(
        <section className="border rounded-xl p-6 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg font-semibold">{sub.planDisplayName} Plan</h2>
              <span className={`inline-block mt-1 px-2 py-0.5 rounded text-xs font-medium ${statusColor[sub.status]??'bg-gray-100 text-gray-700'}`}>{sub.status}</span>
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold">{sub.monthlyPricePaise===0?"Free":money(sub.monthlyPricePaise/100)}<span className="text-sm font-normal text-gray-500">/mo</span></div>
            </div>
          </div>
          <div className="grid grid-cols-3 gap-4 text-sm">
            <div className="bg-gray-50 rounded p-3">
              <div className="text-gray-500">Invoices this period</div>
              <div className="font-semibold text-lg">{sub.invoiceCountCurrent} / {sub.invoiceLimit===-1?"∞":sub.invoiceLimit}</div>
            </div>
            <div className="bg-gray-50 rounded p-3">
              <div className="text-gray-500">Period ends</div>
              <div className="font-semibold">{sub.currentPeriodEnd?new Date(sub.currentPeriodEnd).toLocaleDateString():"—"}</div>
            </div>
            {sub.trialEnd&&(
              <div className="bg-blue-50 rounded p-3">
                <div className="text-blue-600">Trial ends</div>
                <div className="font-semibold text-blue-800">{new Date(sub.trialEnd).toLocaleDateString()}</div>
              </div>
            )}
          </div>
          <div>
            <h3 className="text-sm font-medium text-gray-600 mb-2">Features</h3>
            <div className="flex flex-wrap gap-2">
              {sub.features.map(f=><span key={f} className="px-2 py-0.5 bg-green-50 text-green-700 text-xs rounded">{f.replace(/_/g," ")}</span>)}
            </div>
          </div>
          {sub.status!=="CANCELLED"&&(
            <button onClick={cancel} className="text-sm text-red-600 hover:underline">Cancel subscription</button>
          )}
        </section>
      )}

      <section>
        <h2 className="text-lg font-semibold mb-4">Available Plans</h2>
        <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
          {plans.map(p=>(
            <div key={p.id} className={`border rounded-xl p-4 space-y-3 ${sub?.planName===p.name?"border-blue-500 bg-blue-50":""}`}>
              <h3 className="font-semibold">{p.displayName}</h3>
              <div className="text-xl font-bold">{p.monthlyPricePaise===0?"Free":money(p.monthlyPricePaise/100)}<span className="text-xs font-normal text-gray-500">/mo</span></div>
              <div className="text-xs text-gray-500">{p.invoiceLimit===-1?"Unlimited":p.invoiceLimit} invoices/mo</div>
              <div className="text-xs text-gray-500">{p.userLimit===-1?"Unlimited":p.userLimit} users</div>
              {sub?.planName!==p.name?(
                <button onClick={()=>changePlan(p.name)} className="w-full py-1.5 text-sm bg-blue-600 text-white rounded hover:bg-blue-700">
                  Switch
                </button>
              ):<div className="text-xs text-blue-600 font-medium text-center py-1">Current plan</div>}
            </div>
          ))}
        </div>
      </section>

      {usage.length>0&&(
        <section>
          <h2 className="text-lg font-semibold mb-4">Usage History</h2>
          <table className="w-full text-sm border-collapse">
            <thead><tr className="border-b text-left text-gray-500"><th className="py-2 pr-4">Period</th><th className="py-2 pr-4">Invoices</th><th className="py-2 pr-4">API Calls</th><th className="py-2">AI Extractions</th></tr></thead>
            <tbody>
              {usage.map(u=>(
                <tr key={u.id} className="border-b hover:bg-gray-50">
                  <td className="py-2 pr-4">{u.periodStart} – {u.periodEnd}</td>
                  <td className="py-2 pr-4">{u.invoiceCount}</td>
                  <td className="py-2 pr-4">{u.apiCalls.toLocaleString()}</td>
                  <td className="py-2">{u.aiExtractions}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}
    </main>
  );
}
