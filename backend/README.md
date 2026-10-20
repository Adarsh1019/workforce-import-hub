# Workforce Backend (Spring Boot 3 · Spring Batch · PostgreSQL)

## Run

```bash
# from the repository root: PostgreSQL + API
docker compose up --build
```

- API: http://localhost:8081/api
- Swagger UI: http://localhost:8081/swagger-ui.html
- Demo users: `admin / admin123` (ADMIN), `hr / hr123` (HR)

Run locally without Docker for the API (needs Java 21 + Maven and a running PostgreSQL):

```bash
cd backend && mvn spring-boot:run
```

Tests (`mvn test`): unit tests run anywhere; `WorkforceIntegrationTest` needs Docker (Testcontainers).

## Layout

```text
controller/  REST endpoints (thin)
service/     business rules, transactions, import orchestration
repository/  Spring Data JPA
entity/      JPA entities (schema owned by Flyway)
dto/         request/response records with Bean Validation
batch/       Spring Batch job: reader -> validator/processor -> JPA writer
security/    JWT issue/verify + filter
config/      Security, OpenAPI, demo user seeding
exception/   Global exception handler -> JSON ErrorResponse
db/migration V1 schema, V2 Spring Batch metadata, V3 seed data
```

## Roles

| Action | ADMIN | HR |
|---|---|---|
| View dashboard, employees, departments, imports | yes | yes |
| Create / edit employees, upload CSV, restart import | yes | yes |
| Delete employees | yes | no |
| Create / edit / delete departments | yes | no |

## API

| Method | Path | Notes |
|---|---|---|
| POST | /api/auth/login | `{username,password}` -> `{token,username,role}` |
| GET | /api/dashboard/summary | cards, headcount by department, recent imports |
| GET | /api/employees | `page,size,sort=field,asc|desc,search,departmentId` |
| POST/PUT/DELETE | /api/employees[/{id}] | DTO-validated |
| GET/POST/PUT/DELETE | /api/departments[/{id}] | delete blocked if department has employees |
| POST | /api/imports | multipart `file` |
| GET | /api/imports, /api/imports/{id} | job status and counts |
| GET | /api/imports/{id}/errors.csv | rejected rows + reasons |
| POST | /api/imports/{id}/restart | resume a FAILED/STOPPED job |

Errors are always `{timestamp,status,message,fieldErrors?}`.

## CSV import

Header must be exactly `name,email,department,role,salary,joiningDate` (date `YYYY-MM-DD`; department must exist).

- **Chunking:** 500 rows per transaction (`app.import.chunk-size`).
- **Validation:** required fields, email format, non-negative salary, valid past date, known department.
- **Duplicates:** rejected if the email already exists in the database or appeared earlier in the same file; also enforced by a unique index on `lower(email)`.
- **Errors:** every rejected row is stored in `import_errors` with its line number, raw text and reason.
- **Restarts:** job metadata lives in the Spring Batch tables. Restart relaunches with the same parameters, so the reader resumes after the last committed chunk. Counts are derived from the database, and errors are de-duplicated per line, so a restart never double-counts. Jobs left running by a crash are marked FAILED on startup so they can be restarted.

A 3,000-row sample derived from real HR data is in the frontend at `public/samples/employees_sample.csv` (includes a few deliberately invalid and duplicate rows).
