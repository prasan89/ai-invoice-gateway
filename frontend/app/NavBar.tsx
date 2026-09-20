"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";

const NAV = [
  { href: "/",           label: "Invoices"   },
  { href: "/bulk",       label: "Bulk"       },
  { href: "/email",      label: "Email"      },
  { href: "/anomalies",  label: "Anomalies"  },
  { href: "/copilot",    label: "Copilot"    },
  { href: "/billing",    label: "Billing"    },
  { href: "/erp",        label: "ERP"        },
  { href: "/enterprise", label: "Enterprise" },
];

export default function NavBar() {
  const pathname = usePathname();
  return (
    <nav className="nav">
      <span className="nav-brand">AI Invoice</span>
      {NAV.map(n => {
        const active = n.href === "/" ? pathname === "/" : pathname.startsWith(n.href);
        return (
          <Link key={n.href} href={n.href} className={`nav-link${active ? " active" : ""}`}>
            {n.label}
          </Link>
        );
      })}
    </nav>
  );
}
