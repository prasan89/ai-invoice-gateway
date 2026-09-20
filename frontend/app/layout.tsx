import "./globals.css";
import Link from "next/link";

export const metadata = { title: "AI Invoice Gateway", description: "AI-powered invoice processing" };

const NAV = [
  { href: "/",          label: "Invoices"   },
  { href: "/bulk",      label: "Bulk"       },
  { href: "/email",     label: "Email"      },
  { href: "/anomalies", label: "Anomalies"  },
  { href: "/copilot",   label: "Copilot"    },
  { href: "/billing",   label: "Billing"    },
  { href: "/erp",       label: "ERP"        },
  { href: "/enterprise",label: "Enterprise" },
];

export default function RootLayout({ children }: { readonly children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <nav className="nav">
          <span className="nav-brand">AI Invoice</span>
          {NAV.map(n => (
            <Link key={n.href} href={n.href} className="nav-link">
              {n.label}
            </Link>
          ))}
        </nav>
        <div style={{ minHeight: "calc(100vh - 52px)", background: "#f5f6fa" }}>
          {children}
        </div>
      </body>
    </html>
  );
}
