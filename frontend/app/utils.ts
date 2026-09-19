export const API=process.env.NEXT_PUBLIC_API_URL??"http://localhost:8080";
export const money=(n:number)=>"₹"+Number(n||0).toLocaleString("en-IN",{minimumFractionDigits:2,maximumFractionDigits:2});
export const TERMINAL=new Set(["APPROVED","AUTO_APPROVED","REJECTED"]);
