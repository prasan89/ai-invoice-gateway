# AI Invoice Gateway

AI-powered Indian invoice automation.

## Phase 2

- PDF/PNG/JPG upload with local document storage
- OpenAI Responses API invoice extraction
- Structured invoice fields and line items
- Field-level AI confidence
- Deterministic invoice arithmetic validation
- Side-by-side original document and extracted data review
- Edit/save review
- Approve workflow
- Demo extractor remains available for local development without an API key

## Run locally

### 1. Database

\`\`\`bash
docker compose up -d
\`\`\`

### 2. Backend

Java 21 and Maven are required.

For demo extraction:

\`\`\`bash
cd backend
mvn spring-boot:run
\`\`\`

For real AI extraction, configure an OpenAI API key:

\`\`\`bash
export AI_PROVIDER=openai
export OPENAI_API_KEY="your_api_key"
export OPENAI_MODEL="gpt-5.6-luna"
mvn spring-boot:run
\`\`\`

The backend runs on http://localhost:8080.

### 3. Frontend

\`\`\`bash
cd frontend
npm install
npm run dev
\`\`\`

Open http://localhost:3000.

## Phase 2 flow

Upload invoice -> store original -> AI extraction -> field confidence -> arithmetic validation -> review/edit -> approve.

The original document is stored under \`INVOICE_STORAGE_DIR\` (default \`./data/invoices\`). For production this should be replaced with object storage such as S3-compatible storage.

## AI provider

The current implementation uses OpenAI's Responses API for PDF/image input and structured JSON output. The application sends \`store=false\` for extraction requests.

Do not commit API keys. Use environment variables or a secrets manager.
