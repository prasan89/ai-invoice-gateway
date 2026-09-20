# AI Invoice Gateway

AI-powered Indian GST invoice automation — end-to-end from upload to ERP sync, with anomaly detection, finance copilot, billing, and enterprise features.

---

## Features

| Phase | What's included |
|---|---|
| 1–3 | PDF/image upload, AI extraction (Claude/OpenAI/demo), GST validation, human review, audit trail |
| 4–7 | Bulk processing, GSTIN lookup, email IMAP polling, CSV export |
| 8–14 | Multi-tenant orgs, RBAC, ERP sync jobs, IRP/e-invoice, dashboard analytics |
| 15 | SaaS billing — Starter/Growth/Business/Enterprise plans, Razorpay, usage metering |
| 16 | AI anomaly detection — vendor/amount/GST/duplicate signals, risk scoring (LOW→CRITICAL) |
| 17 | AI Finance Copilot — natural-language spend, GST, cash-flow queries (Claude-backed) |
| 18 | Production hardening — CI/CD, object storage abstraction (local/S3), Prometheus metrics |
| 19 | Real ERP integrations — TallyPrime, Zoho Books, Generic REST |
| 20 | Enterprise — SSO (OIDC/SAML), approval chains, data retention policies |

---

## Run locally (Docker — recommended)

Everything starts with a single command. No Java or Node required on your machine.

```bash
git clone https://github.com/prasan89/ai-invoice-gateway.git
cd ai-invoice-gateway

# Optional: set your Claude API key for AI features
export ANTHROPIC_API_KEY=sk-ant-...

docker compose up --build
```

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Health check | http://localhost:8080/actuator/health |

**Stop:**
```bash
docker compose down
```

**View logs:**
```bash
docker compose logs -f           # all services
docker compose logs -f backend   # backend only
docker compose logs -f frontend  # frontend only
```

**Rebuild after code changes:**
```bash
docker compose build && docker compose up -d
```

---

## Run locally (dev mode — hot reload)

Use this if you are actively developing and want fast feedback.

### Prerequisites
- Java 21
- Maven 3.9+
- Node.js 20+
- PostgreSQL (or use `docker compose up postgres -d` for just the DB)

### 1. Start the database
```bash
docker compose up postgres -d
```

### 2. Backend
```bash
cd backend

# Demo mode — no API key needed
mvn spring-boot:run

# With Claude (recommended for AI features)
export AI_PROVIDER=claude
export ANTHROPIC_API_KEY=sk-ant-...
mvn spring-boot:run
```

Backend runs on http://localhost:8080.

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
```

Frontend runs on http://localhost:3000 with hot reload.

---

## Deploy to AWS (free tier)

The deploy pipeline builds Docker images, pushes them to Amazon ECR, and SSHes into EC2 to restart the stack. It triggers automatically on a version tag push.

### AWS resources to create (one-time)

#### 1. RDS PostgreSQL (free tier)
- Engine: PostgreSQL 16, Template: **Free tier** (t3.micro)
- DB name: `invoice_gateway`, Username: `invoice`
- Note the **endpoint** — you will need it as `RDS_HOST`
- Keep "Publicly accessible: No" — EC2 connects via the same VPC

#### 2. EC2 instance (free tier)
- AMI: **Ubuntu 24.04 LTS**, Instance type: **t2.micro**
- Create a key pair and download the `.pem` file
- Security group — open inbound ports: `22` (SSH), `8080` (backend), `3000` (frontend)
- Edit the **RDS security group** to allow port `5432` from the EC2 security group

#### 3. Bootstrap EC2 (run once)
```bash
chmod 400 your-key.pem
scp -i your-key.pem scripts/ec2-bootstrap.sh ubuntu@<EC2_IP>:~
ssh -i your-key.pem ubuntu@<EC2_IP>
sudo bash ~/ec2-bootstrap.sh
```

This installs Docker, AWS CLI, and registers a systemd service so the app restarts on reboot.

#### 4. IAM user for GitHub Actions
- Create an IAM user and attach policies:
  - `AmazonEC2ContainerRegistryFullAccess`
  - `AmazonEC2FullAccess`
- Generate an access key and save the CSV

### GitHub Secrets

Go to **Settings → Secrets and variables → Actions** in the repo and add:

| Secret | Value |
|---|---|
| `AWS_ACCESS_KEY_ID` | from IAM CSV |
| `AWS_SECRET_ACCESS_KEY` | from IAM CSV |
| `AWS_REGION` | e.g. `us-east-1` |
| `EC2_HOST` | EC2 public IP |
| `EC2_SSH_KEY` | full contents of `.pem` file |
| `RDS_HOST` | RDS endpoint |
| `RDS_USERNAME` | `invoice` |
| `RDS_PASSWORD` | your RDS password |
| `ANTHROPIC_API_KEY` | Claude API key (for Finance Copilot) |
| `RAZORPAY_KEY_ID` | *(optional)* Razorpay key |
| `RAZORPAY_KEY_SECRET` | *(optional)* Razorpay secret |
| `RAZORPAY_WEBHOOK_SECRET` | *(optional)* Razorpay webhook secret |

### Trigger a deploy

```bash
git tag v1.0.0
git push origin v1.0.0
```

The GitHub Actions workflow will:
1. Build backend JAR and frontend (Next.js standalone)
2. Push Docker images to Amazon ECR (repos auto-created if missing)
3. SSH into EC2, write `.env`, pull new images, restart with `docker compose`

After deploy the app is live at:
- Frontend: `http://<EC2_IP>:3000`
- Backend API: `http://<EC2_IP>:8080`

---

## Environment variables reference

| Variable | Default | Description |
|---|---|---|
| `AI_PROVIDER` | `demo` | `demo`, `claude`, `openai`, `hyperspace` |
| `ANTHROPIC_API_KEY` | — | Required for Claude extraction and Finance Copilot |
| `STORAGE_BACKEND` | `local` | `local` or `s3` |
| `S3_BUCKET` / `S3_REGION` | — | Required when `STORAGE_BACKEND=s3` |
| `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` | — | Required for billing |
| `RAZORPAY_WEBHOOK_SECRET` | — | Required for payment webhooks |

---

## Architecture

```
Browser → Next.js (port 3000)
            ↓ /api/* proxy
         Spring Boot (port 8080)
            ↓
         PostgreSQL (port 5432)
```

- **Backend**: Spring Boot 3.5.5, Java 21, JPA/Hibernate, Flyway migrations
- **Frontend**: Next.js 16 (Turbopack), TypeScript
- **AI**: Claude API (`claude-sonnet-4-6`) for extraction and Finance Copilot
- **Multi-tenant**: `TenantContext` (ThreadLocal UUID) per request
- **Storage**: local filesystem (dev) or S3-compatible (prod)

---

> Do not commit API keys. Use environment variables or a secrets manager.
