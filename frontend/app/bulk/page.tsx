"use client";
import { useRef, useState } from "react";
import { API } from "../utils";

interface BulkProgress {
  id: string;
  status: string;
  totalCount: number;
  processedCount: number;
  failedCount: number;
  progressPct: number;
  createdAt: string;
}

const STATUS_COLOR: Record<string, string> = {
  COMPLETED: "#16a34a", COMPLETED_WITH_ERRORS: "#d97706", FAILED: "#dc2626", PROCESSING: "#1d4ed8",
};

export default function BulkPage() {
  const [files, setFiles]   = useState<File[]>([]);
  const [job, setJob]       = useState<BulkProgress | null>(null);
  const [polling, setPolling] = useState(false);
  const [error, setError]   = useState("");
  const inputRef = useRef<HTMLInputElement>(null);
  const pollRef  = useRef<ReturnType<typeof setInterval> | null>(null);

  function handleFiles(incoming: FileList | null) {
    if (!incoming) return;
    setFiles(Array.from(incoming));
  }

  async function startJob() {
    if (files.length === 0) return;
    setError("");
    const form = new FormData();
    files.forEach(f => form.append("files", f));
    try {
      const res = await fetch(API + "/api/v1/invoices/bulk", { method: "POST", body: form });
      if (!res.ok) throw new Error(await res.text());
      const data: BulkProgress = await res.json();
      setJob(data);
      setPolling(true);
      pollRef.current = setInterval(async () => {
        const r = await fetch(API + "/api/v1/bulk-jobs/" + data.id + "/progress");
        const d: BulkProgress = await r.json();
        setJob(d);
        if (d.status === "COMPLETED" || d.status === "COMPLETED_WITH_ERRORS" || d.status === "FAILED") {
          clearInterval(pollRef.current!);
          setPolling(false);
        }
      }, 2000);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Upload failed");
    }
  }

  return (
    <div className="page-content">
      <div className="page-header">
        <div className="page-title">Bulk Invoice Upload</div>
        <div className="page-subtitle">Upload up to 100 PDFs at once — processing happens in the background</div>
      </div>

      {error && <div className="page-notice page-notice-error">{error}</div>}

      {/* Drop zone */}
      <div className="card" style={{ marginBottom: 20 }}>
        <div
          onClick={() => inputRef.current?.click()}
          onDragOver={e => e.preventDefault()}
          onDrop={e => { e.preventDefault(); handleFiles(e.dataTransfer.files); }}
          style={{
            border: `2px dashed ${files.length > 0 ? "#86efac" : "#d1d5db"}`,
            borderRadius: 10,
            padding: "48px 24px",
            textAlign: "center",
            cursor: "pointer",
            background: files.length > 0 ? "#f0fdf4" : "#fafafa",
            transition: "border-color 0.15s, background 0.15s",
            marginBottom: 16,
          }}
        >
          <input ref={inputRef} type="file" multiple accept=".pdf" style={{ display: "none" }}
            onChange={e => handleFiles(e.target.files)} />
          <div style={{ fontSize: 32, marginBottom: 8 }}>📄</div>
          {files.length === 0 ? (
            <>
              <div style={{ fontWeight: 600, marginBottom: 4 }}>Drag PDFs here or click to select</div>
              <div style={{ fontSize: 12, color: "#9ca3af" }}>Up to 100 PDF files</div>
            </>
          ) : (
            <div style={{ color: "#16a34a", fontWeight: 700, fontSize: 16 }}>
              {files.length} file{files.length !== 1 ? "s" : ""} selected
            </div>
          )}
        </div>

        {files.length > 0 && !job && (
          <button className="btn btn-primary" onClick={startJob}>
            Process {files.length} invoice{files.length !== 1 ? "s" : ""}
          </button>
        )}
      </div>

      {/* Job progress */}
      {job && (
        <div className="card">
          <div className="row-between" style={{ marginBottom: 14 }}>
            <div>
              <div className="card-title">Job {job.id.slice(0, 8)}…</div>
              <div style={{ fontSize: 12, color: "#6b7280" }}>{new Date(job.createdAt).toLocaleString()}</div>
            </div>
            <span style={{ fontWeight: 700, color: STATUS_COLOR[job.status] ?? "#374151" }}>
              {job.status.replace(/_/g, " ")}
            </span>
          </div>

          {/* Progress bar */}
          <div style={{ background: "#e5e7eb", borderRadius: 999, height: 10, marginBottom: 16, overflow: "hidden" }}>
            <div style={{
              background: job.status === "COMPLETED" ? "#16a34a" : "#1d4ed8",
              width: job.progressPct + "%", height: "100%", borderRadius: 999,
              transition: "width 0.4s ease",
            }} />
          </div>

          <div className="form-grid form-grid-4">
            <div className="stat-card"><div className="stat-label">Total</div><div className="stat-value sm">{job.totalCount}</div></div>
            <div className="stat-card ok"><div className="stat-label">Processed</div><div className="stat-value sm">{job.processedCount}</div></div>
            <div className="stat-card err"><div className="stat-label">Failed</div><div className="stat-value sm">{job.failedCount}</div></div>
            <div className="stat-card"><div className="stat-label">Progress</div><div className="stat-value sm">{job.progressPct}%</div></div>
          </div>

          {polling && <div style={{ fontSize: 12, color: "#6b7280", marginTop: 12 }}>Polling every 2s…</div>}
        </div>
      )}
    </div>
  );
}
