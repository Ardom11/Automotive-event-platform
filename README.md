# 🏎 Automotive Festival Platform — Backend API

A production-grade REST API for a car festival platform where users purchase event tickets and apply to exhibit their vehicles.

---

## Table of Contents

- [Domain Overview](#domain-overview)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Features](#features)
- [Architecture](#architecture)
- [Application State Machine](#application-state-machine)
- [API Overview](#api-overview)

---

## Domain Overview

The platform serves three types of users:

| Role                | Capabilities                                                                              |
|---------------------|-------------------------------------------------------------------------------------------|
| **Guest**           | Browse events, purchase tickets (name + email collected at checkout)                      |
| **Registered User** | Everything a guest can do + submit car exhibition applications + track application status |
| **Admin**           | Manage events (create, publish, unpublish, cancel) + approve or reject car applications   |

The core business flow has two main tracks:

**Ticket track:** Guest or registered user selects an event → chooses quantity → Stripe Checkout → webhook confirms → individual ticket records created → PDF ticket emailed.

**Application track:** Registered user creates a draft → fills car details (auto-saved every 3 seconds) → submits → admin reviews → approved applications enter a payment window → user pays via Stripe → webhook confirms → status marked PAID → receipt PDF available for download.

---

## Tech Stack

| Layer            | Technology                            |
|------------------|---------------------------------------|
| Language         | Java 21                               |
| Framework        | Spring Boot 4                         |
| Security         | Spring Security + JWT + Google OAuth2 |
| Database         | PostgreSQL 18                         |
| Migrations       | Flyway                                |
| Payments         | Stripe Checkout Sessions + Webhooks   |
| Image Storage    | Amazon S3                             |
| Email            | Spring Mail + Mailtrap                |
| PDF Generation   | OpenPDF                               |
| Containerisation | Docker                                |
| Build Tool       | Maven                                 |

---

## Getting Started

### Prerequisites

- Docker
- A Stripe account (free, test mode is sufficient)
- A Mailtrap account (free) for email catching in dev
- An AWS account for image uploads
- A Google Cloud project with OAuth2 credentials (for Google login)

### 1. Clone the repository

```bash
git clone https://github.com/yourusername/automotive-event-api.git
cd automotive-event-api
```

### 2. Configure environment variables

```bash
cp .env.example .env
```

Fill in `.env`:

```bash
# General
APPLICATION_BASE_URL=http://localhost:8080

# Database
POSTGRES_DB=AutomotiveEventAPI
POSTGRES_USER=postgres
POSTGRES_PASSWORD=mysecretpassword

SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/AutomotiveEventAPI
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=mysecretpassword

# JWT
SECRET_KEY=your_super_secret_key_at_least_32_chars

# Google OAuth
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

# AWS
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_S3_BUCKET_NAME=
AWS_S3_REGION=

# Stripe
STRIPE_SECRET_KEY=
STRIPE_WEBHOOK_SECRET=

# Mailtrap
MAIL_PASSWORD=
MAIL_USERNAME=
```

### 3. Set up Stripe webhook forwarding (for local payment testing)

```bash
# Install Stripe CLI: https://stripe.com/docs/stripe-cli
stripe login
stripe listen --forward-to localhost:8080/webhooks/stripe
# Copy the webhook signing secret → STRIPE_WEBHOOK_SECRET in .env
```

### 4. Start the application

```bash
docker-compose up --build
```

---

## Features

**Auth**
- JWT access tokens + refresh token rotation
- Google OAuth2 for regular users
- Admin accounts seeded directly in DB — no self-registration path
- Role-based route protection (`ROLE_USER`, `ROLE_ADMIN`) enforced at the security filter chain level

**Events**
- Full event lifecycle: `DRAFT → PUBLISHED → UNPUBLISHED → PUBLISHED → CANCELLED`
- DRAFT events invisible to the public
- PUBLISHED events can be unpublished for editing without losing data
- Application and payment deadlines computed from event date, never stored:
  ```
  applicationDeadline = event.dateStart - 2 weeks
  paymentDeadline     = event.dateStart - 7 days
  ```

**Car Applications**
- Draft auto-save via debounced `PATCH` (frontend every 3s - to be implemented, backend accepts partial updates)
- One active application per user per event enforced at DB level via partial unique index
- Full state machine with 6 statuses (see below)
- Admin approve/reject with optional rejection reason
- Scheduled midnight job expires unpaid approved applications automatically

**Payments**
- Stripe Checkout Session for both ticket and application payments
- Webhook-driven fulfillment — frontend redirect is never trusted for business logic
- Idempotency enforced via `stripeSessionId` unique check before processing
- Separate payment entities for tickets and applications (clean schema, no nullable columns)
- Guest ticket purchases capture name/surname/email before Stripe session creation

**Notifications**
- Async email on every application status transition (5 triggers)
- Ticket purchase confirmation with PDF attachment
- Application payment receipt email

**PDFs**
- On-demand PDF generation (no storage overhead)
- Ticket PDF: event details, holder name, unique ticket code
- Receipt PDF: car details, amount paid, payment date

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        HTTP Clients                         │
│              (Frontend / Postman / Stripe Webhooks)         │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                    Spring Security Layer                     │
│         JWT Filter → Role Check → Route Protection          │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                      Controller Layer                        │
│   AuthController · EventController · ApplicationController  │
│   TicketController · PaymentController · WebhookController  │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                       Service Layer                          │
│  Business logic · State transitions · Deadline validation   │
│  Stripe session creation · Idempotency · Email dispatch     │
└──────┬─────────────────┬────────────────┬───────────────────┘
       │                 │                │
┌──────▼──────┐  ┌───────▼──────┐  ┌─────▼──────────────────┐
│ Repository  │  │ Stripe SDK   │  │ External Services       │
│ Layer (JPA) │  │              │  │ Cloudinary · Mail · PDF │
└──────┬──────┘  └──────────────┘  └────────────────────────┘
       │
┌──────▼──────┐
│ PostgreSQL  │
│ (Flyway)    │
└─────────────┘
```

**Package structure follows domain slicing, not technical layering:**
```
auth/        → JWT, OAuth2, refresh tokens
user/        → user profile management
event/       → event CRUD, status transitions
application/ → car applications, state machine
ticket/      → ticket creation and management
payment/     → Stripe sessions, webhook routing, fulfillment
common/      → email, PDF, scheduling, exception handling, config
```

---

## Application State Machine

The car exhibition application follows a strict state machine enforced at the service layer:

```
DRAFT
  └──→ PENDING                (user submits — only before applicationDeadline)
          ├──→ REJECTED        
          └──→ APPROVED_WAITING_PAYMENT
                  ├──→ PAID   (Stripe webhook confirms — terminal ✅)
                  └──→ EXPIRED (paymentDeadline passed, scheduled job runs — terminal)
```

**Rules enforced:**
- Only `PENDING` applications can be reviewed by admin
- Only `APPROVED_WAITING_PAYMENT` applications can initiate payment
- Only `DRAFT` applications can be edited or deleted
- `REJECTED` and `EXPIRED` applications free the user to reapply
- Invalid transitions return `409 Conflict` with a descriptive message

---

---

## API Overview

Full documentation is available via **Swagger UI** at `http://localhost:8080/swagger-ui.html` when running in `dev` profile.

You can test API on Postman with this collection. Import:

- `postman/Automotive-Event-API.postman_collection.json`
- `postman/Automotive-Event-API.postman_environment.json`

Full list of API endpoints:

| Domain           | Method | Path                               | Access |
|------------------|--------|------------------------------------|--------|
| **Auth**         | POST   | `/auth/register`                   | PUBLIC |
|                  | POST   | `/auth/login`                      | PUBLIC |
|                  | POST   | `/auth/google`                     | PUBLIC |
|                  | POST   | `/auth/refresh`                    | PUBLIC |
| **Users**        | GET    | `/users/me`                        | USER   |
|                  | PATCH  | `/users/me`                        | USER   |
| **Events**       | GET    | `/events`                          | PUBLIC |
|                  | GET    | `/events/{id}`                     | PUBLIC |
|                  | POST   | `/admin/events`                    | ADMIN  |
|                  | GET    | `/admin/events`                    | ADMIN  |
|                  | GET    | `/admin/events/{id}`               | ADMIN  |
|                  | PATCH  | `/admin/events/{id}`               | ADMIN  |
|                  | PATCH  | `/admin/events/{id}/status`        | ADMIN  |
|                  | DELETE | `/admin/events/{id}`               | ADMIN  |
| **Applications** | POST   | `/applications`                    | USER   |
|                  | GET    | `/applications`                    | USER   |
|                  | GET    | `/applications/{id}`               | USER   |
|                  | PATCH  | `/applications/{id}`               | USER   |
|                  | DELETE | `/applications/{id}`               | USER   |
|                  | POST   | `/applications/{id}/submit`        | USER   |
|                  | GET    | `/applications/{id}/payment/pdf`   | USER   |
|                  | GET    | `/admin/applications`              | ADMIN  |
|                  | GET    | `/admin/applications/{id}`         | ADMIN  |
|                  | POST   | `/admin/applications/{id}/approve` | ADMIN  |
|                  | POST   | `/admin/applications/{id}/reject`  | ADMIN  |
| **Tickets**      | GET    | `/tickets`                         | USER   |
|                  | GET    | `/tickets/{id}`                    | USER   |
|                  | GET    | `/tickets/{id}/pdf`                | USER   |
| **Payments**     | POST   | `/payments/tickets`                | USER   |
|                  | POST   | `/payments/tickets/guest`          | PUBLIC |
|                  | POST   | `/applications/{id}/payment`       | USER   |
| **Webhooks**     | POST   | `/webhooks/stripe`                 | STRIPE |

---

## Future Improvements

- **Frontend** — React/Next.js client implementing the user flows designed during planning
- **Integration test suite** — Full `@SpringBootTest` coverage for the payment and webhook flows
- **CI/CD pipeline** — GitHub Actions building and pushing Docker image on merge to main
- **Admin dashboard metrics** — Tickets sold per event, application conversion rate, revenue summary