# DentalCare

DentalCare is a Vietnamese-language dental clinic management and online appointment system. It is organized as a monorepo with a React patient/clinic web app, a Spring Boot REST API, a MySQL database, and a separate FastAPI symptom-information service.

> The application provides informational symptom suggestions only. It does not diagnose or replace care from a licensed dental professional.

## Architecture

```text
Browser (React + TypeScript + Vite)
       │ REST / JSON + JWT-authenticated STOMP/WebSocket
       ▼
Spring Boot 3 API ─────────────── MySQL 8
       │                          users, schedules, appointments,
       ├── SMTP (Mailpit locally) medical records, tokens, follow-ups
       ├── FastAPI AI service ─── OpenAI-compatible LLM endpoint
       └── MySQL-backed messaging shared by the clinic roles; new messages are pushed over WebSocket
```

### Repository structure

```text
backend/
  src/main/java/vn/dentalcare/
    config/       seed data
    controller/   REST endpoints
    dto/          validated request and response records
    entity/       JPA entities and enums
    exception/    API errors and global handling
    repository/   Spring Data repositories
    security/     JWT filter, RBAC, Google OAuth2
    service/      business rules and transactions
  src/main/resources/ application configuration and SQL schema
  src/test/       backend unit tests
ai-service/
  app/main.py     FastAPI endpoint, prompt and safe fallback
  tests/          API validation and fallback tests
frontend/
  public/images/ locally bundled, optimized WebP clinic photography
  src/api/        typed API clients and auth-aware Axios client
  src/components/ shared layouts, route protection and notifications
  src/features/   authentication
  src/pages/      public, patient and clinic workspaces
docker-compose.yml
```

### Website visuals

The public clinic website uses a responsive editorial photo layout, locally bundled WebP imagery, lazy-loaded below-the-fold photos, and a custom DentalCare favicon. The four photos are optimized static assets so the landing page does not depend on third-party image CDNs at runtime. Their original photo sources are Unsplash (`photo-1629909613654-28e377c37b09`, `photo-1606811971618-4486d14f3f99`, and `photo-1609840114035-3c981b782dfe`) and Pexels (`3845766`); see [`frontend/public/images/`](frontend/public/images/).

## Main flows and access control

- **PATIENT** manages their own account and appointments: browse services and approved doctor profiles, request or cancel eligible bookings, view only their own treatment records and follow-ups, contact the clinic, receive notifications, and use the consent-based symptom assistant.
- **RECEPTIONIST** owns front-desk operations: search active patients, create appointments on a patient's behalf, approve/reject pending booking requests, handle clinic-level cancellations, coordinate the daily/weekly clinic schedule, review operational statistics, and communicate with patients and staff. Receptionists do not write clinical records.
- **DOCTOR** owns clinical care for assigned patients: review only their assigned appointments, complete or mark attendance for confirmed visits, create treatment records and follow-up recommendations for visits they handled, and maintain a professional public profile with qualifications and achievements. Profile changes remain private drafts until an administrator approves them.
- **ADMIN** owns account and clinic configuration: search all users, assign roles and activate/deactivate accounts; configure services, specialties, doctor schedules and unavailable periods; view aggregate operational statistics; and review or reject doctor profile submissions. Admins do not approve appointment requests or access patient clinical records.
- Each signed-in role can view its own recent notifications, see the unread count, and mark one or all notifications as read. New booking requests notify every active receptionist and link directly to the pending-request queue, which refreshes automatically without a date filter. Notifications include an in-app destination; message alerts open the conversation with the sender.
- Patients can message doctors, receptionists and administrators. Clinic staff can message active patients and other staff. Conversations are stored by Spring Boot in MySQL and scoped to their participants; new messages are pushed to both participants over JWT-authenticated STOMP/WebSocket, with REST polling retained only as a reconnect fallback.
- Administrators, receptionists, and doctors use a role-scoped vertical workspace sidebar. Patients use a dedicated horizontal top navigation; all roles have notification and sign-out controls.
- Signed-in users can open messaging from a floating shortcut. Patients also get a separate floating shortcut to the dental AI assistant.
- Workspace accent colors distinguish roles: red for administrators, yellow for receptionists, green for doctors, and blue for patients.
- A patient booking is created as `PENDING`; only receptionists can confirm or reject it. Doctors can complete or mark a confirmed appointment as a no-show, and patients can cancel eligible appointments. Admins cannot use the appointment workflow.
- Doctors submit their profile, credentials, introduction and achievements as a reviewable draft. Admin approval publishes the submitted content; rejection requires a note so the doctor knows what to correct. Pending edits never replace the currently approved public profile.
- All role checks are enforced by Spring Security and service-level ownership checks; frontend route protection is only a navigation aid.

Appointment availability is derived from a doctor's weekday schedule, break interval, one-off unavailability periods, service duration, existing `PENDING`/`CONFIRMED` bookings and the current time. Creating a booking takes pessimistic locks on the matching doctor/day schedule and patient rows, then checks doctor and patient interval overlaps in the same `READ_COMMITTED` transaction. This ensures a booking waiting on the schedule lock sees an appointment committed by the previous transaction, preventing simultaneous double booking under MySQL's default repeatable-read isolation. `CLINIC_TIMEZONE` defaults to `Asia/Ho_Chi_Minh`; appointment instants are stored and transferred in UTC while opening hours use the configured clinic timezone.

## Database / ERD

```text
users 1──0..1 doctor_profiles ──0..1 specialties
users 1──0..1 doctor_profile_submissions ──N──1 specialties
users 1──N doctor_schedules
users 1──N doctor_unavailabilities
users (patient) 1──N appointments N──1 users (doctor)
appointments N──1 dental_services
appointments 1──0..1 medical_records
users (patient/doctor) 1──N follow_ups N──1 previous appointments
users 1──N refresh_tokens
users 1──N password_reset_tokens
users 1──N notifications
users (sender) 1──N chat_messages N──1 users (recipient)
```

The `users` table stores account identity, BCrypt password hash and one role. `doctor_profiles` contains the approved public profile for doctor-role users; `doctor_profile_submissions` stores the doctor's pending or rejected draft and review note. Appointment pricing is derived from the referenced service price when calculating completed-service revenue; there is no online payment or payment gateway. Foreign keys, unique keys, and query indexes are represented in JPA and in [`backend/src/main/resources/schema.sql`](backend/src/main/resources/schema.sql). Hibernate `ddl-auto=update` is used by default for local development; manage schema migrations explicitly before production.

## API overview

The REST base URL is `/api`. Successful responses use `{ "success": true, "data": ..., "message": null }`; validation and domain errors use the same envelope with an appropriate HTTP status.

| Area | Routes |
| --- | --- |
| Authentication | `POST /auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/forgot-password`, `/auth/reset-password`; authenticated `GET /auth/me` |
| Users | `GET/PUT /users/me`; receptionist/admin `GET /users/patients?search=`; admin `GET /users?search=` |
| Public catalog | `GET /public/doctors`, `/public/services`, `/public/specialties` |
| Doctor profile | Doctor-only `GET/PUT /doctor-profiles/me`; admin-only `GET /doctor-profiles/pending` and `PATCH /doctor-profiles/{doctorId}/review` |
| Appointments | `GET /appointments/page?page=&size=&date=&status=&search=`; date-range `from/to` is inclusive/exclusive and limited to 31 days. Existing `GET /appointments?date=` and `GET /appointments?from=&to=` return arrays for dashboards/calendars. `POST /appointments`, `PATCH /appointments/{id}/status`, `GET /appointments/available-slots?doctorId=&serviceId=&date=` |
| Medical records | `GET/POST /medical-records` with patient/doctor scoping |
| Follow-ups | `GET/POST /follow-ups` with patient/doctor scoping |
| Notifications | `GET /notifications`, `PATCH /notifications/{id}/read`, `PATCH /notifications/read-all` (current user only); each item includes an optional internal `actionPath` for direct navigation |
| Chat | `GET /chat/contacts?search=`, `GET /chat/conversations/{contactId}`, `POST /chat/messages`, `PATCH /chat/conversations/{contactId}/read`; JWT-authenticated STOMP endpoint `/ws/chat` pushes persisted messages to `/user/queue/messages` |
| Profile avatar | Authenticated `POST /users/me/avatar` multipart field `file` (PNG/JPEG, max 5 MB) |
| Clinic administration | Admin-only `GET/POST/PUT /services`, `PATCH /services/{id}/active`, `POST /specialties`, `POST /schedules`, `GET/POST /unavailabilities`, `PATCH /users/{id}/role`, `PATCH /users/{id}/active` |
| Statistics | Receptionist/admin `GET /statistics/overview` |
| AI assistant | Authenticated patient `POST /ai/symptoms` (Spring API proxies FastAPI; configure `LLM_API_KEY` for LLM-backed analysis; without it or if the provider fails, a symptom-aware safe fallback offers general guidance and escalates emergency warning signs) |
| API documentation | `/swagger-ui.html` and `/v3/api-docs` |
| AI service | FastAPI `/analyze`, `/health`, `/docs` |

## Run with Docker Compose

1. Install Docker Desktop, copy `.env.example` to `.env`, and set `JWT_SECRET` to a unique base64-encoded secret of at least 32 random bytes before starting the API. The default Compose database URL uses an existing XAMPP/MariaDB instance on the host. For a self-contained stack that does not modify XAMPP, use the bundled Compose override below.
2. Start the services:

   ```bash
   docker compose up --build
   ```

   To use the bundled MySQL instead of XAMPP (the backend creates its schema and inserts development seed data in the isolated Docker database):

   ```bash
   docker compose -f docker-compose.yml -f docker-compose.bundled.yml --profile bundled-db up --build
   ```

   The bundled MySQL is published on host port `3307`, so it does not take XAMPP's `3306`. For Docker-to-XAMPP connections, use a separate password-protected database account with only `dentalcare` schema privileges and allow only the local Docker host/network through Windows Firewall. Do not expose the XAMPP database port to the public internet.

3. Open:
   - Web app: <http://localhost:5173>
   - Spring API Swagger: <http://localhost:8080/swagger-ui.html>
   - FastAPI Swagger: <http://localhost:8000/docs>
   - Local email inbox: <http://localhost:8025>
   - Bundled MySQL (optional): `localhost:3307`

The first backend start creates the JPA schema and loads seed accounts, catalog, schedules and linked sample appointment/medical-record/follow-up/notification data if `SEED_ENABLED=true`. Seed data is inserted only once and does not delete existing rows. MySQL data is persisted in the `dentalcare_mysql` named volume. To reset local state, stop the stack and remove that named volume intentionally with `docker compose down -v`.

## Seed accounts

All seeded accounts, including the synthetic sample patients/doctors, use the development-only password `DentalCare@2025`.

| Role | Email |
| --- | --- |
| ADMIN | `admin@dentalcare.vn` |
| RECEPTIONIST | `le.tan@dentalcare.vn` |
| DOCTOR | `bacsi.tuan@dentalcare.vn` |
| DOCTOR | `bacsi.mai@dentalcare.vn` |
| PATIENT | `benhnhan@dentalcare.vn` |

Additional synthetic accounts are generated with emails `demo.patient01@dentalcare.local` through `demo.patient08@dentalcare.local` and `demo.doctor01@dentalcare.local` through `demo.doctor03@dentalcare.local`. They use the same development password. The demo seed creates at least five specialties, services, doctor profiles, schedules, unavailable periods, appointments, medical records, follow-ups and patient notifications when applied to the existing starter database. **Self-registration always assigns the `PATIENT` role**; only an administrator can grant staff roles. It never fabricates refresh or password-reset credentials; those rows are created by actual authentication flows.

Do not use these credentials in a deployed environment. Set `SEED_ENABLED=false` after provisioning production accounts.

## Local development

For a local preview without Docker or MySQL, run the backend with the `local` Spring profile. It uses an in-memory H2 database and development seed accounts; start the AI service and local SMTP listener first:

```powershell
# Terminal 1 (ai-service):
py -3.13 -m pip install -r requirements.txt
py -3.13 -m uvicorn app.main:app --reload --port 8000

# Terminal 2 (backend):
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.useTestClasspath=true

# Terminal 3:
cd frontend
npm run dev
```

The local profile uses H2 only for preview/testing. Its data disappears when the backend stops. For registration/booking email previews, run a local SMTP test listener such as Mailpit on `localhost:1025`. Emails are sent after the database transaction commits, so an unavailable SMTP server does not undo a booking; delivery failures are logged by the backend and require SMTP configuration to resolve.

### Frontend

Requirements: Node.js 20+ and npm.

```bash
cd frontend
npm install
npm run dev
npm test
npm run build
```

Set `VITE_API_URL` to the Spring API base URL (including `/api`) in the frontend environment when it differs from `http://localhost:8080/api`.

### Backend

Requirements: JDK 21+, Maven 3.9+, MySQL 8, and an SMTP server (Mailpit is included in Compose).

```bash
cd backend
mvn spring-boot:run
mvn test
```

Configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `AI_SERVICE_URL`, SMTP settings and OAuth credentials in the environment. Swagger is available while the API is running.

For local development with XAMPP/MariaDB, use the existing `dentalcare` database and its dedicated `dentalcare` user in phpMyAdmin. The Spring datasource defaults to `dentalcare` with an empty password for this local setup; set `DB_PASSWORD` only if you assigned a password. Do not use an empty database password outside a local development machine.

#### XAMPP / MariaDB local setup

1. Start **MySQL** in XAMPP Control Panel and open `http://localhost/phpmyadmin`.
2. If setting up a new database, run the following SQL in the SQL tab while signed in as the local XAMPP administrator. Skip this step when the schema and account already exist. The app connects as its own least-privilege user, not as `root`:

   ```sql
   CREATE DATABASE IF NOT EXISTS dentalcare
     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER IF NOT EXISTS 'dentalcare'@'localhost' IDENTIFIED BY '';
   GRANT ALL PRIVILEGES ON dentalcare.* TO 'dentalcare'@'localhost';
   ```

3. From the repository's `backend` directory, start Spring Boot with JDK 21+ and Maven. The default datasource URL, username and blank password are configured for `localhost:3306`. For an existing clinic database, first use `DDL_AUTO=validate` and `SEED_ENABLED=false` to verify the schema without altering or seeding data.
4. The frontend's local image uploads are saved under `backend/uploads/avatars`; Docker deployments use the `dentalcare_uploads` volume. The API validates image bytes, format, file size and dimensions.

Docker containers access XAMPP as remote database clients. The default `dentalcare@localhost` account is intentionally not granted remote access; create a separate password-protected account restricted to the `dentalcare` schema, set it as `DB_USERNAME`/`DB_PASSWORD`, and allow only the local Docker host/network through Windows Firewall. For example, choose a strong password and run `CREATE USER 'dentalcare_docker'@'%' IDENTIFIED BY '<your-password>'; GRANT ALL PRIVILEGES ON dentalcare.* TO 'dentalcare_docker'@'%';` in phpMyAdmin, then keep port 3306 private to the local machine. Running the backend directly on Windows does not require a remote database grant.

The XAMPP setup was tested with MariaDB 10.4.32. It can run this development schema, but MariaDB 10.4 is an old unsupported release; for production use a maintained database version, a non-empty app password, backups and schema migrations.

### AI service

Requirements: Python 3.11+.

```bash
cd ai-service
python -m venv .venv
# Windows PowerShell:
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --env-file ..\.env --reload --port 8000
pytest
```

The default provider is Google's Gemini API (`gemini-3.1-flash-lite`), accessed through Google's native `generateContent` endpoint. It offers a free tier subject to Google's availability, quotas, and terms. Create an API key in [Google AI Studio](https://aistudio.google.com/apikey), copy `.env.example` to the project-root `.env`, and set `LLM_API_KEY` there. The local `uvicorn` command above loads that file; Docker Compose reads it automatically. Never paste the key into the frontend or commit `.env`. `LLM_API_URL` is the Gemini models endpoint base, and `LLM_MODEL` selects the model. AI requests time out after 45 seconds and retry transient provider/network failures once; the Spring proxy allows up to 100 seconds for a retry.

The API key is read only by the Python service and is never sent to the browser. The patient must explicitly consent before symptom text is sent to Google Gemini. Google's free-tier terms say submitted content may be used to improve its products; the UI discloses this, and patients should not enter names, phone numbers, or other identifying details. Emergency warning signs bypass the external model and return immediate-care guidance. If the key is missing or the provider fails, the service responds with a conservative, non-diagnostic fallback. Review Google's current [Gemini API pricing and free-tier terms](https://ai.google.dev/gemini-api/docs/pricing) before enabling it for real patient data.

## Configuration

See [`.env.example`](.env.example) for the Compose environment. Spring configuration also supports `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_STARTTLS`, `FRONTEND_URL`, `CLINIC_TIMEZONE`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_ACCESS_EXPIRATION_MS`, `JWT_REFRESH_EXPIRATION_MS`, `AI_SERVICE_URL`, `DDL_AUTO`, and `SEED_ENABLED`.

For Google sign-in, configure the OAuth consent screen and add `http://localhost:8080/login/oauth2/code/google` as an authorized redirect URI. Google email must be verified. Successful login provisions or reuses a patient account and returns tokens to the frontend callback fragment; the callback immediately removes the fragment from browser history.

## Tests

- Backend: JUnit/Mockito tests for appointment overlap rejection, medical-record role enforcement, API authentication, notification ownership and post-commit email behavior (`mvn test`).
- Frontend: Vitest/Testing Library protected-route test (`npm test`).
- AI: FastAPI request validation, health and provider-fallback tests (`pytest`).

## Deployment notes

- Terminate TLS at a reverse proxy/load balancer and restrict CORS to the deployed frontend origin.
- Replace development secrets and seed credentials; configure a real SMTP relay and production Google OAuth credentials.
- Use a managed MySQL database with encrypted backups, least-privilege credentials, migrations, monitoring and retention policies for sensitive health data.
- Keep LLM keys exclusively in the AI service environment. Review provider data-retention terms before sending patient-provided symptoms to an external model.
- Add operational rate limits, audit trails, object storage and a formal migration/deployment pipeline before production use. Local avatar upload currently uses validated files on disk; Docker stores them in a named volume.
