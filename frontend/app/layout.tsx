import "./globals.css";
import NavBar from "./NavBar";

export const metadata = { title: "AI Invoice Gateway", description: "AI-powered invoice processing" };

export default function RootLayout({ children }: { readonly children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <NavBar />
        <div style={{ minHeight: "calc(100vh - 52px)", background: "#f5f6fa" }}>
          {children}
        </div>
      </body>
    </html>
  );
}
