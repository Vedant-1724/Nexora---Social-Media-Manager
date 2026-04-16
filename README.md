# Nexora

Nexora is a production-oriented, microservices-based social media management platform built for premium brand and agency workflows. This repository is the greenfield monorepo foundation for the phased Nexora build, starting with the Phase 1 scaffold for the web app, backend services, local infrastructure, CI, and cloud-ready deployment structure.

## Repository Layout

```text
apps/
  web/                       React + TypeScript + Vite application
packages/
  contracts/                 Shared API and event contracts
  java/                      Shared Spring Boot platform modules
  ui/                        Shared premium UI component primitives
services/
  api-gateway/               Spring Cloud Gateway entrypoint
  auth-service/              Identity, session, and auth flows
  user-service/              Workspaces, users, and roles
  social-integration-service/ Provider connectors and OAuth callbacks
  post-scheduler-service/    Drafts, scheduling, and publishing workers
  analytics-service/         Metrics ingestion and reporting
  notification-service/      In-app, email, and event-driven notifications
  billing-service/           Plans, subscriptions, and entitlements
infra/
  docker/                    Docker build assets
  k8s/                       Kustomize base and local overlays
  terraform/aws/             AWS platform module and environment scaffold
docs/
  architecture.md            Architecture decisions, C4 context, data model, and ERD
  runbook.md                 Local setup, social linking, and scheduling validation
.github/workflows/           CI smoke pipeline
```

## Local Stack

The local development stack now includes shared infrastructure plus all backend services via `docker-compose.dev.yml`:

- PostgreSQL
- Redis
- RabbitMQ
- MinIO
- Mailpit
- OpenSearch (optional profile)
- API Gateway
- Auth, User, Social Integration, Post Scheduler, Analytics, Notification, and Billing services

The React web app still runs locally via Vite, and the Java services can be managed from the Maven root or as a full Docker Compose stack.

## Quick Start

1. Install frontend dependencies:
   `npm install`
2. Build shared packages and the web app:
   `npm run build`
3. Start the local platform stack:
   `docker compose -f docker-compose.dev.yml up -d`
4. Run backend validation:
   `.\mvnw.cmd test`
5. Start the web app in development:
   `npm run dev:web`

Detailed setup and validation steps live in [docs/runbook.md](docs/runbook.md).

Default host ports are intentionally offset to avoid common local conflicts:

- PostgreSQL: `15432`
- Redis: `16379`
- RabbitMQ AMQP: `15673`
- RabbitMQ UI: `25672`
- MinIO API: `19000`
- MinIO Console: `19001`
- Mailpit SMTP: `11025`
- Mailpit UI: `18025`
- API Gateway (Docker): `18080`

## Phase 1 Deliverables

- Monorepo scaffold for apps, packages, services, infra, docs, and CI
- Premium React shell with marketing and product route templates
- Spring Boot service templates with health endpoints and service metadata
- Local Docker dependencies and container build templates
- Kustomize and Terraform skeletons for later deployment phases

## Phase 2 Deliverables

- Service-owned Postgres schemas and Flyway migrations for auth, user, social, scheduler, analytics, notification, and billing domains
- Seed data for representative workspace, account, scheduling, analytics, notification, and billing records
- Shared persistence ownership contracts in `packages/contracts`
- Architecture docs covering tenancy boundaries and logical ERD
- Testcontainers-based migration validation for each persistence-owning service

## Phase 3 Deliverables

- Shared Spring Boot platform modules for core configuration, MVC/WebFlux API conventions, and RabbitMQ messaging
- Standardized `/api/v1/system/info`, `/api/v1/system/request-context`, and `/api/v1/system/messaging/*` endpoints across the platform
- Gateway downstream service catalog endpoint for cross-service smoke verification
- Consistent OpenAPI metadata, correlation-aware logging, and common error envelopes across all services
- Docker Compose runtime for the gateway and all seven domain services with RabbitMQ-backed platform messaging

## Phase 12 Deliverables

- Production-grade Kustomize base manifests with ConfigMap/Secret injection, resource limits, and health probes
- ClusterIP Service definitions for all 9 workloads (7 backend + gateway + web frontend)
- Local overlay with `imagePullPolicy: Never` for local Kubernetes development
- Production overlay with ECR image references, scaled replicas, ALB Ingress, and TLS termination
- Full AWS Terraform platform module: VPC, EKS, RDS PostgreSQL, ElastiCache Redis, ECR, and S3
- Dev environment wiring with `ap-south-1` region defaults and output forwarding
- NGINX SPA routing config and multi-stage web Dockerfile for containerized React deployment
- `docker-compose.prod.yml` for full-stack local emulation including the web frontend

## Phase 13 Deliverables

- Multi-stage CI pipeline with frontend lint/typecheck, build/test, backend build/test with Postgres + RabbitMQ service containers
- Docker Compose and Kustomize manifest validation in CI
- CD pipeline with matrix Docker builds for all 9 services, push to Amazon ECR
- Automated EKS deployment via Kustomize image patching with rollout verification
- Git SHA and tag-based image versioning strategy
- Concurrency controls preventing duplicate pipeline runs
