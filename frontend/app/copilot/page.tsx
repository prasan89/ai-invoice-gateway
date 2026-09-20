"use client";
import {useEffect,useRef,useState} from "react";
import type {CopilotSession,CopilotMessage} from "../types";
import {API} from "../utils";

const SUGGESTIONS=["How much GST did we pay this month?","Show unusual invoices this month","Which vendor has the highest spend?","Why was the last invoice rejected?","How much is pending approval?","Show duplicate invoices detected"];

export default function CopilotPage(){
  const[sessions,setSessions]=useState<CopilotSession[]>([]);
  const[active,setActive]=useState<CopilotSession|null>(null);
  const[input,setInput]=useState("");
  const[loading,setLoading]=useState(false);
  const bottomRef=useRef<HTMLDivElement>(null);

  useEffect(()=>{
    fetch(API+"/api/v1/copilot/sessions").then(r=>r.ok?r.json():[]).then(setSessions);
  },[]);

  useEffect(()=>{
    bottomRef.current?.scrollIntoView({behavior:"smooth"});
  },[active?.messages]);

  async function ask(question?:string){
    const q=question??input.trim();
    if(!q)return;
    setInput("");
    setLoading(true);
    const body:Record<string,string>={question:q};
    if(active)body.sessionId=active.id;
    const r=await fetch(API+"/api/v1/copilot/ask",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(body)});
    if(r.ok){
      const session:CopilotSession=await r.json();
      setActive(session);
      setSessions(prev=>{const idx=prev.findIndex(s=>s.id===session.id);return idx>=0?[session,...prev.filter(s=>s.id!==session.id)]:[session,...prev]});
    }
    setLoading(false);
  }

  function newSession(){setActive(null);}

  return(
    <main className="flex h-[calc(100vh-4rem)] overflow-hidden">
      {/* Sidebar */}
      <aside className="w-56 border-r flex flex-col bg-gray-50">
        <div className="p-3 border-b">
          <button onClick={newSession} className="w-full py-2 text-sm bg-blue-600 text-white rounded hover:bg-blue-700">+ New Chat</button>
        </div>
        <div className="flex-1 overflow-y-auto p-2 space-y-1">
          {sessions.map(s=>(
            <button key={s.id} onClick={()=>setActive(s)}
              className={`w-full text-left px-3 py-2 rounded text-xs truncate ${active?.id===s.id?"bg-blue-100 text-blue-900":"hover:bg-gray-100 text-gray-700"}`}>
              {s.title||"Untitled"}
            </button>
          ))}
        </div>
      </aside>

      {/* Chat area */}
      <div className="flex-1 flex flex-col overflow-hidden">
        <div className="flex-1 overflow-y-auto p-6 space-y-4">
          {!active&&(
            <div className="text-center pt-12 space-y-6">
              <div>
                <h2 className="text-2xl font-bold">AI Finance Copilot</h2>
                <p className="text-gray-500 mt-2">Ask questions about your invoices, spend, GST, and more</p>
              </div>
              <div className="grid grid-cols-2 gap-3 max-w-xl mx-auto">
                {SUGGESTIONS.map(s=>(
                  <button key={s} onClick={()=>ask(s)}
                    className="text-left px-4 py-3 border rounded-xl text-sm hover:bg-gray-50 text-gray-700">
                    {s}
                  </button>
                ))}
              </div>
            </div>
          )}
          {active?.messages.map(m=>(
            <div key={m.id} className={`flex ${m.role==="USER"?"justify-end":"justify-start"}`}>
              <div className={`max-w-2xl px-4 py-3 rounded-2xl text-sm whitespace-pre-wrap ${
                m.role==="USER"?"bg-blue-600 text-white rounded-br-sm":"bg-gray-100 text-gray-800 rounded-bl-sm"
              }`}>{m.content}</div>
            </div>
          ))}
          {loading&&(
            <div className="flex justify-start">
              <div className="bg-gray-100 px-4 py-3 rounded-2xl rounded-bl-sm text-sm text-gray-400 animate-pulse">Thinking…</div>
            </div>
          )}
          <div ref={bottomRef}/>
        </div>

        <div className="border-t p-4">
          <div className="flex gap-2">
            <input value={input} onChange={e=>setInput(e.target.value)}
              onKeyDown={e=>{if(e.key==="Enter"&&!e.shiftKey){e.preventDefault();ask();}}}
              placeholder="Ask about your invoices, spend, GST…"
              className="flex-1 border rounded-xl px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300"/>
            <button onClick={()=>ask()} disabled={!input.trim()||loading}
              className="px-4 py-2 bg-blue-600 text-white rounded-xl text-sm hover:bg-blue-700 disabled:opacity-50">
              Send
            </button>
          </div>
        </div>
      </div>
    </main>
  );
}
