# Open Surveys

## What this software is

Open Surveys is a full-stack survey (polling) web app built as a school learning project. Users can register, sign in, create and edit surveys, and share them so others can fill them out without an account. Admins can manage user accounts.

It is a **monorepo**: React frontend and Spring Boot backend live in one Git repository.

| Folder | Purpose |
|--------|---------|
| `rtgpoll-backend/` | Java REST API, MySQL access, JWT auth; can also serve the built React app |
| `rtgpoll-frontend/` | React SPA — UI, routing, i18n, API calls |
| `installinstructions.md` | One-time installs and first-time setup |
| `runinstructions.md` | How to start backend + frontend locally |

### ⚠️ VERY IMPORTANT — Navbar logo and `.env` files

The navbar brand image (`logo.png`) and real secrets (`.env`) are **gitignored** and are **not** in the repo. If you clone or host OpenSurveys yourself, you **must**:

1. Add your own logo at `rtgpoll-frontend/public/logo.png`
2. Copy each `.env.example` to `.env` and fill in real values (`rtgpoll-backend/.env` and `rtgpoll-frontend/.env`)

Without these, the navbar logo is broken and auth / database / email will not work. See [`installinstructions.md`](installinstructions.md), then [`runinstructions.md`](runinstructions.md).

For Google Sign-In, use the **same** Google Cloud Web Client ID in both `.env` files (`REACT_APP_GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_ID`), then restart the React dev server after changing the frontend `.env`.

---

## Current functionality

### Authentication

- Sign up and login with username/password (JWT returned)
- Google Sign-In (ID token verified on the backend; can create or link an account)
- JWT stored in the browser and sent on protected API calls
- Route guards: protected (must be logged in), guest-only (login/signup), admin-only
- Logout clears local auth (stateless JWT — no backend logout endpoint)

### Roles and admin

- Roles: `USER` (default) and `ADMIN`
- A seeded admin account is ensured on backend startup (from `.env`)
- Admins can list, edit, and delete users in the `/admin` UI

### Surveys

- Signed-in users create, list, and edit their own surveys
- Question types: skippable text, text, multiple choice, rating
- Creators can make response results public or private
- Anyone can open a survey by access code and submit answers (no account required)
- Results are public when allowed; otherwise only the creator can view them

### Account settings

- Signed-in users can update profile, email, and password
- Sensitive changes can use emailed verification codes
- Account deletion is supported (owned surveys removed first)

### UI extras

- English / German UI strings (survey content itself is not auto-translated)
- Light/dark theme and accent color (persisted locally)
- Main pages: home, login/signup, dashboard, surveys list, editor, fill-out/results, settings, admin

---

## Architecture notes

- **Stateless JWT** — Spring Security validates tokens per request; the frontend also checks expiry and clears bad tokens
- **Secrets via `.env`** — DB, JWT, admin seed, Google client ID, and mail settings are gitignored; the backend loads them at startup
- **Google OAuth** — browser gets a Google ID token; backend verifies it, then issues the app’s own JWT
- **Roles** — UI hides admin pages; backend still enforces `ROLE_ADMIN` on admin APIs
- **Dev** — React on port 3000 proxies API calls to Spring Boot on port 8080
- **Prod-style** — React build can be served from the backend `static` folder; SPA routes fall back to `index.html`
- **Data model** — `User` → `Form` → `Question` → `Answer` (MySQL via JPA)

Setup and run details: [`installinstructions.md`](installinstructions.md) · [`runinstructions.md`](runinstructions.md)

---

## Plugins / languages used

### Languages

| Language | Where |
|----------|-------|
| **Java 25** | Backend |
| **JavaScript** | Frontend (React) |
| **SQL** | MySQL (via JPA/Hibernate) |
| **HTML / CSS** | Frontend markup and styling |

### Backend (Maven / Spring Boot)

| Dependency | Role |
|------------|------|
| **Spring Boot 4.1** | Application framework |
| **Spring Web MVC** | REST API |
| **Spring Data JPA** | Entities and repositories |
| **Spring Security** | Route protection, BCrypt |
| **Spring Validation** | Request validation |
| **Spring Mail** | Verification-code emails |
| **MySQL Connector** | Database driver |
| **JJWT** | JWT create / verify |
| **Google API Client** | Verify Google ID tokens |
| **dotenv-java** | Load `.env` at startup |
| **Lombok** | Less boilerplate on models/DTOs |
| **Maven** | Build and dependencies |

### Frontend (npm)

| Package | Role |
|---------|------|
| **React 19** | UI |
| **React Router DOM** | Client routing and guards |
| **Create React App** | Dev server and build |
| **Bootstrap 5** | Layout / components |
| **animejs** | Motion (theme wipe, page enter, blobs, hover) |
| **Google Identity Services** | Browser Google Sign-In |
| **Custom i18n** (`src/i18n/`) | EN / DE UI strings |
| **Testing Library** | Component tests |

### Database

- **MySQL** — users, forms, questions, answers, verification codes

### Current Team

- ConcreteYt
- stoli9000-source
- steinbaron11
