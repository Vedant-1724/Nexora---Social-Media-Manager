# Nexora Platform Runbook

> Comprehensive operational guide covering local development setup, social provider linking, and post scheduling validation.

---

## Table of Contents

1. [Local Development](#local-development)
2. [Social App Linking Checklist](#social-app-linking-checklist)
3. [Post Scheduling Validation](#post-scheduling-validation)

---

## Local Development

### Prerequisites

- Node.js 24+
- Java 21
- Docker Desktop / Docker Engine with Compose

### Install And Build

1. Run `npm install`
2. Run `npm run build`
3. Ensure Docker is running because backend tests use Testcontainers for isolated Postgres validation
4. Run `.\mvnw.cmd test` on Windows or `./mvnw test` on macOS/Linux

### Start Local Platform

1. Run `docker compose -f docker-compose.dev.yml up -d`
2. Optional search stack: `docker compose -f docker-compose.dev.yml --profile search up -d`

### Useful Endpoints

- API Gateway: `http://localhost:18080`
- Auth Service: `http://localhost:8081`
- User Service: `http://localhost:8082`
- Social Integration Service: `http://localhost:8083`
- Post Scheduler Service: `http://localhost:8084`
- Analytics Service: `http://localhost:8085`
- Notification Service: `http://localhost:8086`
- Billing Service: `http://localhost:8087`
- PostgreSQL: `localhost:15432`
- Redis: `localhost:16379`
- RabbitMQ UI: `http://localhost:25672`
- RabbitMQ AMQP: `localhost:15673`
- MinIO API: `http://localhost:19000`
- MinIO Console: `http://localhost:19001`
- Mailpit SMTP: `localhost:11025`
- Mailpit UI: `http://localhost:18025`

### Frontend Development

1. Run `npm run dev:web`
2. Open `http://localhost:5173`

### Validation Checklist

- `npm run build`
- `npm run test`
- `.\mvnw.cmd test`
- `docker compose -f docker-compose.dev.yml config`
- `http://localhost:18080/api/v1/system/services/catalog`
- `http://localhost:8081/api-docs`

### Phase-Specific Development Notes

**Phase 2 (Data Model):**
- Database migrations live under each service in `src/main/resources/db/migration`
- Seed data for migration validation lives in `src/test/resources/db/seed`
- Tenant and service ownership rules are documented in `docs/architecture.md`
- On some Windows Docker Desktop setups, Testcontainers may not attach through the Java Docker client; the schema tests skip in that case, while Linux CI or direct container-based validation should execute the migrations fully

**Phase 3 (Backend Platform):**
- Shared Spring Boot platform code lives in `packages/java`
- Messaging is disabled by default outside Docker Compose; set `NEXORA_MESSAGING_ENABLED=true` when you want RabbitMQ wiring active
- The gateway service catalog endpoint aggregates `/api/v1/system/info` and `/api-docs` from all downstream services for local smoke verification

**Phase 5 (Social Integration):**
- Social Integration Service now owns OAuth state, encrypted provider tokens, normalized publishing, and webhook ingestion foundations
- Set `NEXORA_SOCIAL_CALLBACK_BASE_URL` to the gateway origin that providers can reach; on this machine the local gateway is `http://localhost:18080`
- Set `NEXORA_SOCIAL_ENCRYPTION_ACTIVE_SECRET` to a 32+ character secret before linking real provider accounts
- Provider-specific client IDs, client secrets, and webhook verify tokens can be injected through `NEXORA_SOCIAL_META_*`, `NEXORA_SOCIAL_LINKEDIN_*`, and `NEXORA_SOCIAL_X_*`

**Phase 6 (Post Scheduling):**
- Post Scheduler Service now owns draft composition, media metadata links, approval request state, scheduled jobs, and publish retry tracking
- The scheduler dispatch worker calls the Social Integration Service through `NEXORA_SCHEDULER_SOCIAL_BASE_URL`; in local Docker Compose this resolves to `http://social-integration-service:8080`
- Workspace-scoped scheduler APIs now flow through the gateway under `/api/v1/workspaces/{workspaceId}/posts/...` and `/api/v1/workspaces/{workspaceId}/calendar/posts`
- Composer and calendar UI flows expect at least one connected social account before scheduling platform variants
- Scheduling can be queued before approval, but dispatch only proceeds once the stored approval request reaches `approved`

---

## Social App Linking Checklist

Use this checklist when validating Meta, LinkedIn, and X integrations against sandbox or developer apps.

### Shared Prerequisites

- Ensure the gateway is reachable at `http://localhost:18080`
- Set `NEXORA_SOCIAL_CALLBACK_BASE_URL=http://localhost:18080`
- Set `NEXORA_SOCIAL_ENCRYPTION_ACTIVE_SECRET` to a 32+ character secret
- Start the platform with `docker compose -f docker-compose.dev.yml up -d`
- Obtain a valid workspace-scoped JWT with `workspace.manage` and `posts.create`

### Generic Linking Flow

1. Create a link session:
   `POST http://localhost:18080/api/v1/workspaces/{workspaceId}/social/link-sessions`
2. Use a body like:
   `{"provider":"meta"}` or `{"provider":"linkedin"}` or `{"provider":"x"}`
3. Copy the `authorizationUrl` from the response and complete provider consent in the browser
4. Confirm the provider callback returns `{"status":"linked", ...}`
5. List linked accounts:
   `GET http://localhost:18080/api/v1/workspaces/{workspaceId}/social/accounts`
6. Publish a sandbox post:
   `POST http://localhost:18080/api/v1/workspaces/{workspaceId}/social/publications`
7. Refresh tokens manually if needed:
   `POST http://localhost:18080/api/v1/workspaces/{workspaceId}/social/accounts/{connectedAccountId}/refresh`

### Meta Checklist

- Configure the OAuth redirect URI:
  `http://localhost:18080/api/v1/social/oauth/meta/callback`
- Configure the webhook callback:
  `http://localhost:18080/api/v1/social/webhooks/meta`
- Configure the webhook verify token to match `NEXORA_SOCIAL_META_WEBHOOK_VERIFY_TOKEN`
- Validate GET verification with `hub.challenge`
- Send a signed sample comment or messaging payload and confirm `acceptedEvents` increments

### LinkedIn Checklist

- Configure the OAuth redirect URI:
  `http://localhost:18080/api/v1/social/oauth/linkedin/callback`
- Configure the webhook callback:
  `http://localhost:18080/api/v1/social/webhooks/linkedin`
- Send an `X-Li-Signature` signed sample payload and confirm normalized inbox ingestion works
- Validate account listing shows LinkedIn capabilities such as `publish.text` and `inbox.comments`

### X Checklist

- Configure the OAuth redirect URI:
  `http://localhost:18080/api/v1/social/oauth/x/callback`
- Configure the webhook callback:
  `http://localhost:18080/api/v1/social/webhooks/x`
- Validate CRC handling by calling the GET webhook endpoint with `crc_token`
- Send a signed `x-twitter-webhooks-signature` payload for `tweet_create_events` or `direct_message_events`
- Confirm the adapter-generated auth URL includes PKCE flow parameters

### Manual Validation Notes

- If Docker/Testcontainers cannot attach on Windows, migration tests may skip while service and adapter tests still run
- Provider sandbox apps often require allow-listed redirect URIs and explicit tester accounts
- For local webhook testing through public providers, tunnel the gateway port externally and update the callback base URL accordingly

---

## Post Scheduling Validation

### Preconditions

- Start the local stack with `docker compose -f docker-compose.dev.yml up -d`
- Confirm the gateway is reachable at `http://localhost:18080`
- Sign in through the web app and switch into a workspace that already has Phase 5 connected accounts

### Manual Flow

1. Open `/app/composer`
2. Create or load a draft, then save it with at least one media asset row and one provider variant
3. Select one or more connected accounts for the target provider variants
4. Submit the draft for approval with a valid `approvalRouteId`
5. Schedule the draft into the future
6. Open `/app/calendar` and verify the scheduled item appears in the live range query
7. Approve the draft through the scheduler approval decision endpoint or an API client using `posts.approve`
8. Optionally trigger `POST /api/v1/workspaces/{workspaceId}/posts/jobs/{jobId}/dispatch` for controlled local dispatch testing

### Expected Results

- Draft persistence includes body, metadata, linked media assets, and per-provider variants
- Scheduling produces a queued scheduler job and calendar-ready response
- Publish attempts are recorded per connected account with retry-safe dedupe on successful targets
- Failed dispatches retain successful account attempts and only retry the remaining failed targets
