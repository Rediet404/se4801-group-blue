# clinic - Clinic Appointment & Medical Records Management System

This repository contains the Clinic application: a Java Spring Boot backend and a Next.js frontend. The README below documents the detailed folder structure, responsibilities for each folder, core technologies and how to run/test the project.

Quick links

- Backend README: [backend/README.md](backend/README.md)
- Frontend README: [frontend/README.md](frontend/README.md)

Repository — high level

```
clinic/
├── backend/                     # Spring Boot API, persistence, services
├── frontend/                    # Next.js App Router, React + TypeScript UI
├── target/                      # build artifacts (ignored in VCS)
└── README.md                    # this file
```

Detailed folder structure and responsibilities

Backend (`backend/`) — responsibilities and key files

- `pom.xml` — Maven build/config and dependency management
- `src/main/java/com/clinic/`:
  - `controller/` — REST controllers (API surface). Example controllers: `AuthController`, `AppointmentController`, `PrescriptionOrderController`.
  - `service/` and `service/impl/` — business logic and service implementations (e.g. `PrescriptionOrderServiceImpl`).
  - `repository/` — Spring Data JPA repositories for data access.
  - `entity/` — JPA entities mapped to DB tables (e.g. `User`, `Appointment`, `MedicalRecord`, `PrescriptionOrder`).
  - `dto/` and `mapper/` — DTOs and mappers for API models and entity conversion.
  - `security/` — JWT and security configuration.
  - `config/` — app configuration classes.
  - `ClinicApplication.java` — main application entry point.
- `src/main/resources/`:
  - `application.yml`, `application-dev.yml`, `application-test.yml` — environment configurations.
  - `db/migration/` — Flyway migration scripts (V1**\*, V2**\*, ...). These are the source-of-truth for production schema changes.
- `src/test/`:
  - `resources/schema.sql` — in-memory test schema used by `@DataJpaTest`. IMPORTANT: keep this in sync with Flyway migrations (causes Hibernate `validate` errors if not in sync).
  - `java/com/clinic/...` — unit/integration tests (repository/controller/service tests).

Backend core technologies

- Java 21, Spring Boot 3.x, Spring Data JPA (Hibernate)
- PostgreSQL (production), H2 in-memory (tests)
- Flyway for DB migrations
- Spring Security with JWT, BCrypt
- Maven (build), JUnit5 + Mockito (tests), JaCoCo (coverage)

Frontend (`frontend/`) — responsibilities and key files

- `package.json` — npm scripts and dependencies
- `src/app/` — Next.js App Router routes and pages. Route groups include:
  - `(auth)/` — login/register/forgot password pages
  - `(protected)/` — protected, role-scoped routes for admin, doctor, patient, pharmacist
  - `pharmacist/`, `doctor/`, `patient/` — feature entry pages
- `src/components/` — shared UI building blocks (UI primitives, form components, charts)
- `src/features/` — feature-specific pages and hooks (e.g. `doctor/doctor-pages.tsx`, `prescriptions/` service hooks)
- `src/services/api/` — API client wrappers (`api-client.ts`) and per-resource services (`prescription-orders.ts`, `medical-records.ts`, `users.ts`)
- `src/store/` — Zustand stores (auth state)
- `src/styles/` — Tailwind theme and global styles
- `public/` — static assets (logos, favicon)

Frontend core technologies

- Next.js (App Router) + React 19 + TypeScript
- Tailwind CSS for styling
- Radix UI primitives, Lucide icons
- TanStack Query for data fetching and caching
- Axios for HTTP client
- React Hook Form + Zod for forms and validation
- Zustand for local auth state

How the pieces fit together

- Backend exposes REST endpoints that the frontend consumes (via `src/services/api/*`).
- Backend stores data in PostgreSQL and applies schema changes via Flyway. Tests run against H2 with a static `schema.sql` for deterministic `@DataJpaTest` behavior.

Run & development (quick commands)

- Backend dev (requires a running DB or environment variables pointing to one):

```bash
# from clinic/backend
mvn clean package
mvn spring-boot:run
```

- Frontend dev:

```bash
# from clinic/frontend
npm install
npm run dev
```

Testing and validation

- Backend tests: `mvn test` (unit + repository tests). If tests fail with schema validation errors, update `backend/src/test/resources/schema.sql` to match Flyway migrations.
- Frontend type-check: `npx tsc --noEmit`
- Frontend build: `npm run build`

Environment variables (common)

- Backend: configured in `application-*.yml` and supports overriding via env vars. Important values:
  - `SPRING_DATASOURCE_URL` — JDBC URL for PostgreSQL
  - `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`
  - `APP_JWT_SECRET` (or `spring.security.jwt.secret`) — JWT signing secret
- Frontend: in `env` or `next.config.ts` / runtime environment. Typical variables:
  - `NEXT_PUBLIC_API_BASE` — API base URL (e.g. `http://localhost:8080`)

Notes, tips and gotchas

- Keep Flyway migrations in `backend/src/main/resources/db/migration/` as the source-of-truth. Whenever schema changes, add a new migration and, if needed, update `backend/src/test/resources/schema.sql` for tests.
- Tests run with `spring.jpa.hibernate.ddl-auto=validate` in `test` profile; schema drift causes ApplicationContext failures.
- The frontend build may log an ESLint patching warning in some environments — it is not blocking for `next build` in this repo, but you can run `npx eslint .` to surface issues.
