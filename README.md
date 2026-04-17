# Code Generation — ATM Booth (full-stack)

This repository contains a **Spring Boot** backend and a **Vue 3** frontend. Run both locally during development: the frontend talks to the backend through a **Vite proxy** (no manual CORS setup needed when using `npm run dev`).

---

## What you need installed

| Tool | Version (expected) | Why |
|------|-------------------|-----|
| **Java** | **21** (LTS) | Spring Boot compiles and runs on Java 21. |
| **Node.js** | **20+** (LTS recommended) | For `npm` and the Vue/Vite dev server. |
| **Git** | Any recent | Clone and collaborate. |

You do **not** need a global Maven install: the backend includes the **Maven Wrapper** (`mvnw` / `mvnw.cmd`).

---

## Repository layout

```
backend/          ← Spring Boot API (port 8080)
frontend/         ← Vue + Vite + vue-router + Pinia (port 5173)
```

---

## 1. Run the backend (Spring Boot)

From the `backend` folder:

**Windows (PowerShell or CMD):**

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

**macOS / Linux:**

```bash
cd backend
chmod +x ./mvnw   # only if Git did not preserve execute permission
./mvnw spring-boot:run
```

Wait until you see something like “Started BackendApplication”. The API listens on **http://localhost:8080**.

### Useful backend URLs

| URL | What it is |
|-----|------------|
| http://localhost:8080/api/health | Simple JSON health check |
| http://localhost:8080/api/messages | Example JPA endpoint (list messages) |
| http://localhost:8080/swagger-ui/index.html | **Swagger UI** (OpenAPI docs) |
| http://localhost:8080/h2-console | **H2 console** (in-memory DB) |

**H2 console login (dev):**

- **JDBC URL:** `jdbc:h2:mem:appdb`
- **User name:** `sa`
- **Password:** *(leave empty)*

### Run tests (optional)

```powershell
cd backend
.\mvnw.cmd test
```

---

## 2. Run the frontend (Vue)

**First time only** — install dependencies:

```powershell
cd frontend
npm install
```

**Start the dev server:**

```powershell
npm run dev
```

Open the URL Vite prints (usually **http://localhost:5173**).

### How the frontend reaches the API

- In development, `frontend/vite.config.ts` **proxies** requests from `/api` to **http://localhost:8080**.
- So from browser code you can call **`fetch('/api/health')`** and it goes to the Spring Boot app.
- **Important:** start the **backend first** (or at least before you expect data on the home page). If the backend is off, the home page will show an error until you start it and click **Retry**.

### Production build (optional)

```powershell
cd frontend
npm run build
npm run preview
```

For a real deployment you would point the frontend at your deployed API URL (env variable or server config), not rely on the Vite proxy.

---

## 3. Typical daily workflow

1. Terminal A: `cd backend` → `.\mvnw.cmd spring-boot:run` (or `./mvnw`).
2. Terminal B: `cd frontend` → `npm run dev`.
3. Browser: open the Vite URL and work on features.

---

## 4. Docker (backend image)

The `backend/Dockerfile` builds and runs the API inside **Ubuntu** using **OpenJDK 25** and the Maven wrapper. **Build context must be the `backend` folder** (where `pom.xml` and `mvnw` live):

```bash
cd backend
docker build -t code-generation-backend .
docker run -p 8080:8080 code-generation-backend
```

**Note:** `RUN chmod +x ./mvnw` is required on Linux so `./mvnw` can run. If `openjdk-25-jdk` is not available in your Ubuntu image, switch the Dockerfile to a JDK version that exists there (for example **OpenJDK 21**) or use an official **Eclipse Temurin** base image.

---

## 5. Where to add your own code

| Area | Suggested place |
|------|------------------|
| REST controllers | `backend/src/main/java/.../web/` |
| Entities | `backend/src/main/java/.../domain/` |
| Repositories | `backend/src/main/java/.../repository/` |
| Security / CORS | `backend/src/main/java/.../config/SecurityConfig.java` |
| Config (DB, ports) | `backend/src/main/resources/application.yml` |
| Vue pages | `frontend/src/views/` |
| Routes | `frontend/src/router/index.ts` |
| Global state | `frontend/src/stores/` |

---

## 6. Tech stack (quick reference)

**Backend:** Spring Boot 3.5, Spring Web, Spring Data JPA, H2, Spring Security, SpringDoc OpenAPI (Swagger), Validation.

**Frontend:** Vue 3, Vite, TypeScript, vue-router, Pinia.

If anything fails (wrong Java version, port already in use, `npm` not found), fix the prerequisite or free the port and try again. For backend errors, check the terminal where Spring Boot is running.
