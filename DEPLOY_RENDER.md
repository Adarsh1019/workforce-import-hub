# Render deployment

This repository contains the TanStack Start frontend at the repository root and the Spring Boot backend in `backend/`. Deploy them as separate Render web services and create a Render PostgreSQL database in the same region.

## Before deploying

- Choose and reserve the frontend and backend service names/origins. The frontend API setting is public and is compiled into the frontend bundle at **build time**.
- The backend production profile requires database credentials, a strong JWT secret, and the exact frontend origin. It disables the demo-user seeder.
- Review migration `V3__seed_data.sql` before the first production migration. It inserts sample employee records. Flyway runs automatically at backend startup; do not use a production database containing incompatible schema or unreviewed data.
- Admin and HR demo accounts are **not** created in production. Use the explicit one-time bootstrap procedure below for the first accounts; do not enable the demo seeder in production.

## Render PostgreSQL

Create a managed PostgreSQL instance and use its **internal** host/connection information for services in the same Render region. Copy the database name, username, password, host, and port from the Render database dashboard into the backend service's environment. `DB_URL` must be a JDBC URL, for example:

`jdbc:postgresql://<internal-host>:<port>/<database>?sslmode=require`

Use Render's provided values, never the example placeholders. `DB_USER` and `DB_PASSWORD` are supplied separately to Spring. Do not put these values in frontend variables. Render-managed PostgreSQL persists independently of application restarts; configure and periodically verify database backups/exports in the database dashboard, and test restoration to a separate database before relying on backups.

## Frontend web service

- **Runtime:** Node
- **Root Directory:** repository root (leave blank in Render)
- **Build Command:** `npm ci && npm run build`
- **Start Command:** `npm start`
- **Health Check Path:** `/login`
- **Environment:** `VITE_API_URL=https://<backend-service>.onrender.com/api`

The build pins Nitro's Node server preset. `npm start` launches `.output/server/index.mjs`; Nitro's Node server listens on Render's `PORT` and `0.0.0.0`. `VITE_API_URL` is read during the build, so configure it before building and rebuild after changing the backend URL. It is browser-visible and must contain no credentials or secrets.

## Backend web service

- **Runtime:** Docker
- **Root Directory:** `backend`
- **Dockerfile Path:** `Dockerfile`
- **Build and start:** Render builds the existing multi-stage Java 21 image; its entry point runs the packaged Spring Boot JAR.
- **Health Check Path:** `/actuator/health`
- **Persistent disk:** mount at `/data` (the production profile stores upload files in `/data/uploads`). Keep one backend instance: uploaded CSV files are local to this disk and not shared across multiple service instances.

Set these backend service environment variables in Render:

| Variable | Value |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `production` |
| `DB_URL` | JDBC URL for the Render database's internal host and port |
| `DB_USER` | Render PostgreSQL username |
| `DB_PASSWORD` | Render PostgreSQL password (secret) |
| `JWT_SECRET` | Unique, random secret with at least 32 bytes of entropy (secret) |
| `CORS_ORIGINS` | Exact frontend origin, e.g. `https://<frontend-service>.onrender.com`; no path or trailing slash |
| `UPLOAD_DIR` | `/data/uploads` (optional; production default) |
| `BOOTSTRAP_USERS_ENABLED` | Temporarily `true` only for initial account creation; default `false` |
| `BOOTSTRAP_ADMIN_USERNAME` / `BOOTSTRAP_ADMIN_PASSWORD` | Temporary Admin account credentials, set as protected variables/secrets |
| `BOOTSTRAP_HR_USERNAME` / `BOOTSTRAP_HR_PASSWORD` | Optional temporary HR credentials; provide both or neither |

The existing `server.port` setting honors Render's assigned `PORT`. Production uses Flyway and Hibernate schema validation; no migration drops or resets data. `/actuator/health` exposes status only, while Spring Security permits that path.

### One-time account bootstrap

For the first backend deploy, generate unique credentials outside source control (passwords must be at least 12 characters). Set `BOOTSTRAP_USERS_ENABLED=true`, `BOOTSTRAP_ADMIN_USERNAME`, and `BOOTSTRAP_ADMIN_PASSWORD` as backend Render environment variables; optionally set both HR variables as well. Deploy once. The production-only bootstrap hashes passwords with the app's BCrypt encoder and only creates accounts whose usernames do not already exist; it never resets an existing password. After startup succeeds, set `BOOTSTRAP_USERS_ENABLED=false`, remove all bootstrap username/password variables, and redeploy/restart. Keep the resulting credentials in an approved password manager. Never put these values in `.env.example`, logs, or frontend environment variables.

## Deployment order

1. Commit and push the reviewed changes to the connected Git branch. Do not rewrite published history.
2. Create Render PostgreSQL and record its internal connection details securely.
3. Create the backend Docker web service, attach the persistent disk, set its environment variables (including `SPRING_PROFILES_ACTIVE=production`), and deploy. Confirm Flyway completed and `/actuator/health` is healthy.
4. Create the frontend Node web service with the root build/start settings above. Set `VITE_API_URL` to the deployed backend's HTTPS URL before its first build.
5. For first-time accounts, follow the one-time bootstrap steps above and immediately disable/remove its temporary variables after deployment.
6. Update backend `CORS_ORIGINS` to the exact frontend HTTPS origin and redeploy the backend if that value was not present at its initial deploy.
7. Verify `/login`, refresh/direct navigation, browser static assets, authenticated API calls for Admin and HR, role restrictions, dashboard requests, CSV upload, and import status/error downloads. Check browser network requests for HTTPS backend URLs and backend logs for startup, migration, and health status.

## Important behavior and limitations

- The current JWT is stored in browser `localStorage`; protected dashboard routes are client-rendered. This deployment preserves that behavior and does not add SSR authentication or change the auth model.
- CSV upload sources and batch job metadata are split between the persistent disk and PostgreSQL. A disk is required for restart/recovery. Keep a single backend instance unless upload storage is changed to shared/object storage.
- Migration V3 adds sample departments/employees once. Flyway does not rerun an already-applied version, but the sample rows remain in a newly initialized production database.
- Local development continues using the existing localhost API fallback only in Vite development mode. Production builds fail clearly at request time if `VITE_API_URL` is missing.
- The production JWT secret and CORS origin have no permissive production defaults. Do not enable wildcard CORS or the demo-user seeder to work around configuration errors.

See `.env.example` for placeholder formats only. Do not commit real environment values.