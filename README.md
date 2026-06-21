# ATM Booth — full-stack banking project

Spring Boot backend and Vue 3 frontend for an InHolland-style banking assignment. Run both locally during development; the frontend talks to the backend through a **Vite proxy** (no manual CORS setup when using `npm run dev`).

---

## What you need installed

- **Java 21** (LTS) — Spring Boot compiles and runs on Java 21.
- **Node.js 20+** (LTS recommended) — for `npm` and the Vue/Vite dev server.

You do not need a global Maven install: the backend includes the Maven Wrapper (`mvnw` / `mvnw.cmd`).

---

## Repository layout

| Folder      | Role                                            |
| ----------- | ----------------------------------------------- |
| `backend/`  | Spring Boot API (port **8080**)                 |
| `frontend/` | Vue + Vite + vue-router + Pinia (port **5173**) |

---

## Demo users (seeded on startup)

These accounts are created by `DataLoader` when the backend starts:

| Role     | Email                   | Password       |
| -------- | ----------------------- | -------------- |
| Employee | `employee@inholland.nl` | `Password123!` |
| Customer | `customer@inholland.nl` | `Password123!` |

Use the employee account for admin routes (`GET /users`, `POST /accounts`, etc.). The customer account is pre-approved for day-to-day banking flows.

---

## 1. Run the backend (Spring Boot)

From the `backend` folder:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Wait until you see something like “Started BackendApplication”. The API listens on **http://localhost:8080**.

### Useful backend URLs

| URL                                         | Purpose                                                                                                    |
| ------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| http://localhost:8080/api/health            | JSON health check (no auth)                                                                                |
| http://localhost:8080/auth/register         | Public customer registration (`POST` JSON body)                                                            |
| http://localhost:8080/auth/login            | Public login (`POST` JSON body, returns JWT)                                                               |
| http://localhost:8080/swagger-ui/index.html | Swagger UI — title **ATM Booth Banking API**; use **Authorize** with `Bearer <token>` for protected routes |
| http://localhost:8080/h2-console            | H2 console (in-memory DB)                                                                                  |

**H2 console login (dev):**

- JDBC URL: `jdbc:h2:mem:testdb`
- User name: `sa`
- Password: _(leave empty)_

### Run tests

```powershell
cd backend
.\mvnw.cmd test
```

Integration tests live under `backend/src/test/java/com/example/backend/controllers/` as `*RestControllerTest.java` (grouped with `@Nested`). Unit tests for rules and services sit in `domain/rules/` and `services/`.

---

## 2. Run the frontend (Vue)

First time only — install dependencies:

```powershell
cd frontend
npm install
```

Start the dev server:

```powershell
npm run dev
```

Open the URL Vite prints (http://localhost:5173).

### How the frontend reaches the API

- In development, API calls go to **http://localhost:8080** via the Vite proxy.
- From the Vue dev server you can call `fetch('/api/health')`, `fetch('/auth/login', …)`, etc.; Vite proxies `/api`, `/auth`, `/users`, `/accounts`, and `/transactions` to port **8080** (see `frontend/vite.config.ts`).
- Start the **backend first** when testing login/register.

### Production build (optional)

```powershell
cd frontend
npm run build
npm run preview
```

For **GitHub Pages** (static hosting) you cannot use the Vite dev proxy: set the API base URL (e.g. your **Render** backend URL) via env at build time and use that in `fetch`.

---

## 3. Typical daily workflow

1. Terminal A: `cd backend` → `.\mvnw.cmd spring-boot:run`
2. Terminal B: `cd frontend` → `npm run dev`
3. Browser: open the Vite URL and work on features.

---

## 4. Where to add your own code

| Area               | Location                                                  |
| ------------------ | --------------------------------------------------------- |
| REST controllers   | `backend/src/main/java/com/example/backend/controllers/`  |
| Business rules     | `backend/src/main/java/com/example/backend/domain/rules/` |
| Entities & enums   | `backend/src/main/java/com/example/backend/entities/`     |
| Repositories       | `backend/src/main/java/com/example/backend/repositories/` |
| Security / JWT     | `backend/src/main/java/com/example/backend/config/`       |
| Config (DB, ports) | `backend/src/main/resources/application.properties`       |
| Vue pages          | `frontend/src/views/`                                     |
| Routes             | `frontend/src/router/index.ts`                            |
| Global state       | `frontend/src/stores/`                                    |
