# Code Generation — full-stack project

This repository contains a **Spring Boot** backend and a **Vue 3** frontend. Run both locally during development: the frontend talks to the backend through a **Vite proxy** (no manual CORS setup needed when using `npm run dev`).

\---

## What you need installed

Java 21 (LTS) Spring Boot compiles and runs on Java 21.

Node.js 20+ (LTS recommended)  For `npm` and the Vue/Vite dev server.

You do not need a global Maven install: the backend includes the Maven Wrapper(`mvnw` / `mvnw.cmd`).

\---

## Repository layout

backend/          ← Spring Boot API (port 8080)

frontend/         ← Vue + Vite + vue-router + Pinia (port 5173)

## 1. Run the backend (Spring Boot)

From the `backend` folder:

Windows (PowerShell or CMD):

powershell

cd backend

.\\mvnw.cmd spring-boot:run

Wait until you see something like “Started BackendApplication”. The API listens on **http://localhost:8080**.

### Useful backend URLs

http://localhost:8080/api/health

 Simple JSON health check

http://localhost:8080/api/messages  

Example JPA endpoint (list messages)

http://localhost:8080/swagger-ui/index.html

Swagger UI (OpenAPI docs)

http://localhost:8080/h2-console

H2 console (in-memory DB)

H2 console login (dev):

JDBC URL: jdbc:h2:mem:appdb

User name: sa

Password: (leave empty)

### Run tests

powershell

cd backend

.\\mvnw.cmd test

## 2. Run the frontend (Vue)

First time only — install dependencies:

```powershell

cd frontend

npm install

```

Start the dev server:

powershell

npm run dev

Open the URL Vite prints (http://localhost:5173).

### How the frontend reaches the API

- In development,  http://localhost:8080

- So from browser code you can call **`fetch('/api/health')`** and it goes to the Spring Boot app.

- Important:start the backend first(or at least before you expect data on the home page). If the backend is off, the home page will show an error until you start it and click Retry.

### Production build (optional)

```powershell

cd frontend

npm run build

npm run preview

```

For a real deployment you would point the frontend at your deployed API URL (env variable or server config), not rely on the Vite proxy.

\---

## 3. Typical daily workflow

1. Terminal A: `cd backend` → `.\\mvnw.cmd spring-boot:run` (or `./mvnw`).
2. Terminal B: `cd frontend` → `npm run dev`.
3. Browser: open the Vite URL and work on features.

\---

## 5. Where to add your own code

 REST controllers

   `backend/src/main/java/.../web/`

Entities

    `backend/src/main/java/.../domain/`

Repositories

     `backend/src/main/java/.../repository/`

Security / CORS

     `backend/src/main/java/.../config/SecurityConfig.java`

Config (DB, ports)

    `backend/src/main/resources/application.yml`

Vue pages

    `frontend/src/views/`

Routes

    `frontend/src/router/index.ts`

 Global state

    `frontend/src/stores/`
