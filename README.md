# Finance Data Processing Backend

Spring Boot backend for a finance dashboard with role-based access control, PASETO authentication, financial record management, and summary analytics.

## Overview

This project exposes a secured REST API for:

- authenticating users with bearer tokens
- managing users with role and status controls
- creating, updating, listing, and soft-deleting financial records
- retrieving dashboard summaries for income, expense, balance, trends, and recent activity

The application is backend-only and uses an in-memory H2 database for local execution.

## Tech Stack

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Security
- Spring Data JPA
- H2 Database
- Maven

## Core Features

- PASETO-based authentication
- Role-based access control for `ADMIN`, `ANALYST`, and `VIEWER`
- User management with active and inactive account status
- Financial record CRUD with soft delete support
- Filtering by type, category, and date range
- Pagination for record listing
- Dashboard summary with totals, category breakdowns, monthly trends, and recent activity
- Centralized exception handling with consistent API error responses
- In-memory request rate limiting
- Seed data for local demo and testing

## Access Model

| Role | Access |
| --- | --- |
| `VIEWER` | Dashboard summary only |
| `ANALYST` | Dashboard summary and record viewing |
| `ADMIN` | Full access to users, records, and dashboard data |

## Demo Credentials

| Role | Email | Password |
| --- | --- | --- |
| Admin | `admin@zorvyn.local` | `Admin@123` |
| Analyst | `analyst@zorvyn.local` | `Analyst@123` |
| Viewer | `viewer@zorvyn.local` | `Viewer@123` |

## Running Locally

Start the application:

```bash
./mvnw spring-boot:run
```

The API runs on `http://localhost:8080`.

Useful local endpoints:

- API health: `GET /api/health`
- H2 console: `http://localhost:8080/h2-console`

## Running Tests

Run the full test suite:

```bash
./mvnw test
```

The suite includes service, acceptance, and system-level endpoint coverage.

## Authentication Flow

1. Call `POST /api/auth/login` with an email and password.
2. Read the returned `accessToken`.
3. Send `Authorization: Bearer <token>` on protected requests.
4. Call `POST /api/auth/logout` to revoke existing tokens for the current user.

Example login request:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@zorvyn.local",
    "password": "Admin@123"
  }'
```

## API Summary

### Public

- `GET /api/health`
- `POST /api/auth/login`

### Authenticated

- `POST /api/auth/logout`

### Dashboard

- `GET /api/dashboard/summary`

### Records

- `GET /api/records`
- `POST /api/records`
- `PUT /api/records/{recordId}`
- `DELETE /api/records/{recordId}`

### Users

- `GET /api/users`
- `POST /api/users`
- `PUT /api/users/{userId}`

## Record Query Parameters

`GET /api/records` supports:

- `type`
- `category`
- `from`
- `to`
- `page`
- `size`

## Configuration Notes

Current local defaults in `application.yaml` include:

- H2 in-memory database
- `ddl-auto: create-drop`
- enabled seed data
- enabled H2 console
- 12-hour token TTL
- 60 requests per 60-second rate-limit window

## Implementation Notes

- Soft delete is used for financial records instead of hard deletion.
- Logout works by invalidating tokens issued before the recorded logout time.
- Error responses are returned in a consistent JSON structure across controller and security failures.
- The project is designed for local development and review, not production deployment as-is.
