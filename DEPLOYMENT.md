# Deployment (US-29 & US-30)

## US-29 — Backend on Render

1. Push this repo to GitHub.
2. In [Render](https://render.com): **New → Web Service** → connect the repo.
3. **Runtime:** Docker · **Dockerfile path:** `./Dockerfile` (repo root).
4. **Environment variables:**
   - `SPRING_PROFILES_ACTIVE` = `prod`
   - `JWT_SECRET` = long random string (32+ chars)
   - `CORS_ALLOWED_ORIGINS` = `https://<your-github-username>.github.io`  
     (comma-separated if you need several origins)
5. Deploy. Note the HTTPS URL, e.g. `https://code-generation-backend.onrender.com`.
6. Check:
   - `https://<your-service>.onrender.com/api/health`
   - `https://<your-service>.onrender.com/swagger-ui/index.html`

Render sets `PORT` automatically; the app listens on `${PORT}`.

---

## US-30 — Frontend on GitHub Pages

1. Repo **Settings → Pages → Build and deployment:** **GitHub Actions**.
2. Repo **Settings → Secrets and variables → Actions** → **New repository secret:**
   - Name: `VITE_API_BASE_URL`
   - Value: your Render backend URL (no trailing slash), e.g. `https://code-generation-backend.onrender.com`
3. Push to `main` (or run workflow **Deploy frontend to GitHub Pages** manually).
4. Site URL: `https://<username>.github.io/Code_Generation_ATM-BOOTH/`

The workflow is [`.github/workflows/deploy-frontend.yml`](.github/workflows/deploy-frontend.yml).

---

## Order of operations

1. Deploy **backend** on Render first.
2. Set `VITE_API_BASE_URL` and `CORS_ALLOWED_ORIGINS` to match.
3. Deploy **frontend** (push to `main` or re-run Actions).

---

## Local vs production

| | Local dev | Production |
|---|-----------|------------|
| Frontend | `npm run dev` (proxy to :8080) | GitHub Pages |
| API base | empty (`VITE_API_BASE_URL` unset) | Render URL in secret |
| CORS | localhost:5173 | GitHub Pages origin in Render env |
