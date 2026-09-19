# AI Invoice Gateway

AI-powered Indian invoice automation.

## Current implementation

### Phase 2 — AI extraction + human review
- PDF/PNG/JPG upload with local document storage
- OpenAI, Claude and Hyperspace AI extraction providers
- Structured Indian GST invoice fields and line-level tax breakdown
- Field-level AI confidence
- Deterministic invoice arithmetic/GST validation
- Side-by-side original document and extracted data review
- Edit/save review
- Approve workflow
- Demo extractor for local development without an API key

### Phase 3 — Operations
- Immutable-style invoice event history for upload, extraction, validation, review and approval
- CSV export with header and line-level GST data
- Review UI exposes CGST/SGST/IGST/Cess and line-level HSN/SAC/tax fields
- Search and status filtering in the invoice dashboard
- Audit history visible during invoice review

## Run locally

### 1. Database
```bash
docker compose up -d
```

### 2. Backend
Java 21 and Maven are required.

For demo extraction:
```bash
cd backend
mvn spring-boot:run
```

For Hyperspace:
```bash
export AI_PROVIDER=hyperspace
export HYPERSPACE_API_KEY="your_key"
export HYPERSPACE_BASE_URL="http://localhost:6655/anthropic"
export HYPERSPACE_MODEL="claude-sonnet-4-6"
mvn spring-boot:run
```

For Claude:
```bash
export AI_PROVIDER=claude
export CLAUDE_API_KEY="your_key"
export CLAUDE_MODEL="claude-sonnet-4-6"
mvn spring-boot:run
```

For OpenAI:
```bash
export AI_PROVIDER=openai
export OPENAI_API_KEY="your_key"
export OPENAI_MODEL="gpt-5.6-luna"
mvn spring-boot:run
```

The backend runs on http://localhost:8080.

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:3000.

## Current flow
Upload invoice -> store original -> AI extraction -> field confidence -> deterministic GST validation -> human review/edit -> approve -> audit trail/export.

The original document is stored under `INVOICE_STORAGE_DIR` (default `./data/invoices`). For production, replace this with object storage such as S3-compatible storage.

Do not commit API keys. Use environment variables or a secrets manager.
