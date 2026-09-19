'use client';
import { useState, useRef } from 'react';

interface BulkProgress {
  id: string;
  status: string;
  totalCount: number;
  processedCount: number;
  failedCount: number;
  progressPct: number;
  createdAt: string;
}

export default function BulkPage() {
  const [files, setFiles] = useState<File[]>([]);
  const [job, setJob] = useState<BulkProgress | null>(null);
  const [polling, setPolling] = useState(false);
  const [error, setError] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const handleFiles = (incoming: FileList | null) => {
    if (!incoming) return;
    setFiles(Array.from(incoming));
  };

  const startJob = async () => {
    if (files.length === 0) return;
    setError('');
    const form = new FormData();
    files.forEach(f => form.append('files', f));
    try {
      const res = await fetch('/api/v1/invoices/bulk', { method: 'POST', body: form });
      if (!res.ok) throw new Error(await res.text());
      const data: BulkProgress = await res.json();
      setJob(data);
      setPolling(true);
      pollRef.current = setInterval(async () => {
        const r = await fetch(`/api/v1/bulk-jobs/${data.id}/progress`);
        const d: BulkProgress = await r.json();
        setJob(d);
        if (d.status === 'COMPLETED' || d.status === 'COMPLETED_WITH_ERRORS') {
          clearInterval(pollRef.current!);
          setPolling(false);
        }
      }, 2000);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Upload failed');
    }
  };

  const statusColor = (s: string) =>
    s === 'COMPLETED' ? '#16a34a' : s === 'COMPLETED_WITH_ERRORS' ? '#d97706' : s === 'FAILED' ? '#dc2626' : '#6366f1';

  return (
    <main style={{ maxWidth: 700, margin: '40px auto', padding: '0 16px', fontFamily: 'system-ui, sans-serif' }}>
      <h1 style={{ fontSize: 22, fontWeight: 700, marginBottom: 8 }}>Bulk Invoice Upload</h1>
      <p style={{ color: '#6b7280', marginBottom: 24 }}>Upload up to 100 PDFs at once. Processing happens in the background.</p>

      <div
        onClick={() => inputRef.current?.click()}
        onDragOver={e => e.preventDefault()}
        onDrop={e => { e.preventDefault(); handleFiles(e.dataTransfer.files); }}
        style={{
          border: '2px dashed #d1d5db', borderRadius: 12, padding: '40px 24px',
          textAlign: 'center', cursor: 'pointer', marginBottom: 16,
          background: files.length > 0 ? '#f0fdf4' : '#fafafa',
        }}
      >
        <input ref={inputRef} type="file" multiple accept=".pdf" style={{ display: 'none' }}
          onChange={e => handleFiles(e.target.files)} />
        {files.length === 0 ? (
          <span style={{ color: '#9ca3af' }}>Drag PDFs here or click to select</span>
        ) : (
          <span style={{ color: '#16a34a', fontWeight: 600 }}>{files.length} file{files.length !== 1 ? 's' : ''} selected</span>
        )}
      </div>

      {files.length > 0 && !job && (
        <button onClick={startJob} style={{
          background: '#6366f1', color: '#fff', border: 'none', borderRadius: 8,
          padding: '10px 28px', fontSize: 15, cursor: 'pointer', fontWeight: 600,
        }}>
          Process {files.length} Invoice{files.length !== 1 ? 's' : ''}
        </button>
      )}

      {error && <p style={{ color: '#dc2626', marginTop: 12 }}>{error}</p>}

      {job && (
        <div style={{ marginTop: 32, border: '1px solid #e5e7eb', borderRadius: 12, padding: 24 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
            <span style={{ fontWeight: 600 }}>Job {job.id.slice(0, 8)}…</span>
            <span style={{ color: statusColor(job.status), fontWeight: 700 }}>{job.status}</span>
          </div>
          <div style={{ background: '#e5e7eb', borderRadius: 999, height: 12, marginBottom: 16 }}>
            <div style={{
              background: job.status === 'COMPLETED' ? '#16a34a' : '#6366f1',
              width: `${job.progressPct}%`, height: '100%', borderRadius: 999,
              transition: 'width 0.4s ease',
            }} />
          </div>
          <div style={{ display: 'flex', gap: 24, fontSize: 14, color: '#374151' }}>
            <span>Total: <strong>{job.totalCount}</strong></span>
            <span>Processed: <strong>{job.processedCount}</strong></span>
            <span>Failed: <strong style={{ color: job.failedCount > 0 ? '#dc2626' : undefined }}>{job.failedCount}</strong></span>
            <span>Progress: <strong>{job.progressPct}%</strong></span>
          </div>
          {polling && <p style={{ fontSize: 13, color: '#6b7280', marginTop: 12 }}>Polling every 2s…</p>}
        </div>
      )}
    </main>
  );
}
