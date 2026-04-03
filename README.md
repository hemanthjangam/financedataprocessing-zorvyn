# Finance Data Processing and Access Control Backend

Backend API for a finance dashboard system with role-based access control, financial record management, and summary analytics.

## Tech Stack

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Security
- Spring Data JPA
- H2 Database

## Features

- PASETO-based authentication
- User management with roles and active/inactive status
- Role-based access control for `VIEWER`, `ANALYST`, and `ADMIN`
- Financial record CRUD operations
- Record filtering by type, category, and date range
- Pagination for record listing
- Soft delete for financial records
- In-memory rate limiting for API protection
- Dashboard summary APIs for income, expenses, net balance, category totals, recent activity, and monthly trends
- Input validation and centralized error handling

## Access Model

- `VIEWER`: dashboard summary only
- `ANALYST`: dashboard summary and record viewing
- `ADMIN`: full access to users, records, and dashboard data

## Demo Credentials

- `admin@zorvyn.local` / `Admin@123`
- `analyst@zorvyn.local` / `Analyst@123`
- `viewer@zorvyn.local` / `Viewer@123`

## Authentication

Use `POST /api/auth/login` to get a PASETO bearer token, then send `Authorization: Bearer <token>` with protected requests.

## Run the Project

```bash
./mvnw spring-boot:run
```

Application runs on `http://localhost:8080`.

## Main Endpoints

- `GET /api/health`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/dashboard/summary`
- `GET /api/records`
- `POST /api/records`
- `PUT /api/records/{recordId}`
- `DELETE /api/records/{recordId}`
- `GET /api/users`
- `POST /api/users`
- `PUT /api/users/{userId}`

## Example Request

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@zorvyn.local",
    "password": "Admin@123"
  }'
```

## Notes

- The project is intentionally backend-only because the assignment focuses on backend design and API implementation.
- H2 is used for simple local execution without external database setup.
# financedataprocessing-zorvyn
