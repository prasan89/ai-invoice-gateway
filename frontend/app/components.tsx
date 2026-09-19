import type {ReactNode} from "react";
import type {ArithmeticStatus,GstinStatus} from "./types";

export function Confidence({value}:{value?:number}){
  if(value===undefined||value===null)return null;
  const cls=value>=90?"confidence high":value>=70?"confidence medium":"confidence low";
  return <span className={cls}>{Number(value).toFixed(0)}%</span>;
}
export function Field({label,confidence,children}:{label:string;confidence?:number;children:ReactNode}){
  return <label className="field"><span>{label} <Confidence value={confidence}/></span>{children}</label>;
}
export function GstinBadge({status}:{status?:GstinStatus}){
  if(!status||status==="PENDING")return null;
  return <span className={`gstin-badge ${status.toLowerCase()}`}>{status==="NOT_PROVIDED"?"—":status}</span>;
}
export function ArithBadge({status}:{status?:ArithmeticStatus}){
  if(!status||status==="PENDING")return null;
  return <span className={`arith-${status.toLowerCase()}`}>Arithmetic: {status}</span>;
}
