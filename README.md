# MindOps 🧠☁️

**AI-Driven Cloud Orchestration & Proactive FinOps Platform**

MindOps is an enterprise-grade platform that bridges the semantic gap between natural language intent and physical cloud infrastructure. By abstracting the steep learning curve of IaC (Infrastructure as Code) syntax, MindOps allows engineers to deploy secure, cost-optimized architectures to AWS and Microsoft Azure using plain English prompts.

---

## 🚀 Enterprise Novelty & Key Features

* **Intent-Based Provisioning:** Powered by Google Gemini Pro, user prompts are wrapped in strict system guardrails to generate deterministic, machine-readable JSON blueprints, eliminating "Console Fatigue."
* **Proactive FinOps (Shift-Left Billing):** Prevents budget overruns and "Ghost Servers" by intercepting the AI blueprint, calculating estimated monthly compute costs, and enforcing a financial approval gate *before* deployment.
* **Ephemeral Zero-Trust Vault:** Cloud credentials are treated as radioactive. They are encrypted at rest in MySQL using military-grade **AES-256-GCM**, decrypted ephemerally in server RAM for the exact millisecond of deployment, and instantly wiped by the Java Garbage Collector.
* **Asynchronous Multi-Cloud State Machine:** Avoids HTTP timeouts and thread starvation. The Spring Boot backend offloads AWS/Azure SDK execution to background worker pools while the Next.js frontend utilizes a lightweight polling architecture to stream live progress logs.
* **Azure Landing Zone Architecture:** Enforces rigid network boundaries by deploying compute resources into pre-existing Virtual Networks, rejecting public IP exposure by default.

---

## 🛠️ Technology Stack

**Frontend (Client Architecture)**
* Next.js 14 (React)
* Tailwind CSS (with `next-themes` Light/Dark mode)
* Axios (for async state polling)

**Backend (Execution Engine)**
* Java 17+ / Spring Boot 3
* Spring Security (JWT Authentication)
* Spring Data JPA / Hibernate
* Bucket4j (Rate Limiting & API Shielding)

**Infrastructure & AI Services**
* Google Gemini Pro API (LLM Orchestration)
* AWS SDK for Java v2 (EC2 Provisioning)
* Azure Resource Manager SDK (VM Provisioning)
* MySQL 8.0 (Relational Database)

---

## ⚙️ Local Development Setup

Follow these instructions to run MindOps locally. **Do not commit `.env` or `application-secret.yml` files to version control.**

### Prerequisites
* Java 17+
* Node.js v18+
* MySQL 8.0
* Git

### 1. Database Setup
Log into your local MySQL instance and create the database:
```sql
CREATE DATABASE mindops_db;
```

### 2. Backend Setup (Spring Boot)
Navigate to the backend directory (if separate) or the root of the Spring Boot project.

Create an `application-secret.yml` (or set environment variables) for your secure keys. *This file is ignored by `.gitignore`.*
```yaml
# src/main/resources/application-secret.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mindops_db
    username: root
    password: your_local_db_password

mindops:
  security:
    jwt:
      secret: "replace_with_a_highly_secure_random_base64_string_min_256_bits"
      expiration: 86400000 # 24 hours
    vault:
      master-key: "replace_with_exactly_32_byte_aes_key"

gemini:
  api:
    key: "replace_with_your_google_gemini_api_key"
```

Run the backend:
```bash
./mvnw clean install -DskipTests
./mvnw spring-boot:run
```
*The backend will start on `http://localhost:8080`.*

### 3. Frontend Setup (Next.js)
Navigate to your frontend directory.

Install dependencies:
```bash
npm install
```

Create a `.env.local` file in the root of the frontend project:
```env
# .env.local
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
```

Run the development server:
```bash
npm run dev
```
*The frontend will start on `http://localhost:3000`.*

---

## 🗺️ Roadmap (Phase 2)
As MindOps matures, the following enterprise capabilities are slated for development:
1. **Dynamic IaC Generation:** Transitioning from imperative SDK calls to dynamic Terraform (`.tf`) file generation to prevent vendor lock-in.
2. **Continuous State Drift Detection:** Backend CRON jobs to detect out-of-band manual changes made in the AWS/Azure consoles.
3. **Telemetry-Driven AI Auto-Remediation:** Automated downsizing of severely over-provisioned infrastructure based on live CPU metrics.
4. **Autonomous Spot Arbitrage:** Machine learning algorithms to live-migrate workloads between cloud providers to capture the lowest real-time compute pricing.

---

## 🛡️ License & Disclaimer
This project was developed as a Capstone Engineering prototype. **Use caution when attaching live AWS/Azure credentials.** The authors are not responsible for unintended cloud provider billing charges resulting from the use of this platform.

---

## 👥 The Engineering Team

MindOps was architected and developed as a Engineering Project by:

* **Thiruppathi R** – Lead Cloud Solutions Architect (Multi-Cloud State Machine & SDKs)
* **Devasri S P** – Frontend & UX Architect (Next.js Client & Async Polling)
* **Rishikesh R** – Security & Zero-Trust Architect (AES-256 Vault & API Shielding)
* **Mohammed Nadheer** – AI & Prompt Engineer (LLM Guardrails & FinOps Estimation)