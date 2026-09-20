import "./globals.css";
import Link from "next/link";
export const metadata={title:"AI Invoice Gateway",description:"AI-powered invoice processing"};
const NAV=[
  {href:"/",label:"Invoices"},
  {href:"/bulk",label:"Bulk"},
  {href:"/email",label:"Email"},
  {href:"/anomalies",label:"Anomalies"},
  {href:"/copilot",label:"Copilot"},
  {href:"/billing",label:"Billing"},
  {href:"/erp",label:"ERP"},
  {href:"/enterprise",label:"Enterprise"},
];
export default function RootLayout({children}:{readonly children:React.ReactNode}){
  return(
    <html lang="en">
      <body>
        <nav className="border-b bg-white sticky top-0 z-10">
          <div className="max-w-7xl mx-auto px-4 flex items-center gap-1 h-12">
            <span className="font-bold text-blue-700 mr-4 text-sm">AI Invoice</span>
            {NAV.map(n=>(
              <Link key={n.href} href={n.href} className="px-3 py-1.5 text-sm rounded text-gray-600 hover:bg-gray-100 hover:text-gray-900 transition-colors">
                {n.label}
              </Link>
            ))}
          </div>
        </nav>
        <div className="min-h-screen bg-white">
          {children}
        </div>
      </body>
    </html>
  );
}
