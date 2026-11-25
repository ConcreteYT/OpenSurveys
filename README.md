# Open Surveys

# Status

Currently on hold due to exams for all involved devs.
Estimated time of return : Mid December(17-20)

Email : [contact@opensurveys.de](mailto:contact@opensurveys.de)(will take a few days to answer. Please include "Opensurveys" in your title)

The demo website is currently down due to a few changes that will be made when we have time. We cant guarantee when exactly it will return.

## What this software is

Open Surveys is a full-stack survey (polling) web app built as a school learning project. Users can register, sign in, create and edit surveys, and share them so others can fill them out without an account. Admins can manage user accounts.

It is a **monorepo**: React frontend and Spring Boot backend live in one Git repository.

| Folder | Purpose |
|--------|---------|
| `opensurveys-backend/` | Java REST API, MySQL access, JWT auth; can also serve the built React app |
| `opensurveys-frontend/` | React SPA — UI, routing, i18n, API calls |
| `installinstructions.md` | One-time installs and first-time setup |
| `runinstructions.md` | How to start backend + frontend locally |

### ⚠️ VERY IMPORTANT — Navbar logo and `.env` files

The navbar brand image (`logo.png`) and real secrets (`.env`) are **gitignored** and are **not** in the repo. If you clone or host OpenSurveys yourself, you **must**:

1. Add your own logo at `opensurveys-frontend/public/logo.png`
2. Copy each `.env.example` to `.env` and fill in real values (`opensurveys-backend/.env` and `opensurveys-frontend/.env`)

Without these, the navbar logo is broken and auth / database / email will not work. See [`installinstructions.md`](installinstructions.md), then [`runinstructions.md`](runinstructions.md).

For Google Sign-In, use the **same** Google Cloud Web Client ID in both `.env` files (`REACT_APP_GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_ID`), then restart the React dev server after changing the frontend `.env`.

---

## Current functionality

### Authentication

- Sign up and login with username/password
- Google Sign-In (ID token verified on the backend; can create or link an account)
- Session JWT is an httpOnly cookie. The browser script cannot read it. `localStorage` only keeps UI hints (session flag, username, role)
- Route guards: protected (must be logged in), guest-only (login/signup), admin-only
- Logout calls `POST /auth/logout`, which clears the cookie, and the frontend clears the local hints

### Roles and admin

- Roles: `USER` (default) and `ADMIN`
- A seeded admin account is ensured on backend startup (from `.env`)
- Admins can list, edit, and delete users in the `/admin` UI

### Surveys

- Signed-in users create, list, and edit their own surveys
- Question types: skippable text, text, multiple choice, rating, image upload
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
- Public legal pages: `/impressum` and `/datenschutz` (German and English). Personal details are still placeholders until the operator fills them in
- First-visit dialog on `/home`: says this is a demo and links to those legal pages. Agreeing is remembered in this browser
- Student-project banner fixed to the top of every page. Turn it off with `SHOW_PROJECT_BANNER` in `opensurveys-frontend/src/layout/ProjectBanner.js` (see [`installinstructions.md`](installinstructions.md))

---

## Architecture notes

- **Session cookie** — the JWT lives in an httpOnly cookie. Spring Security validates it per request. The frontend keeps only a local hint of the session and clears it on logout
- **Secrets via** `.env` — DB, JWT, admin seed, Google client ID, and mail settings are gitignored; the backend loads them at startup
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
| **animejs** | DOM motion (page enter, blobs, button hover, icon flips) |
| **Google Identity Services** | Browser Google Sign-In |
| **Custom i18n** (`src/i18n/`) | EN / DE UI strings |
| **Testing Library** | Component tests |

### Possible change: anime.js → Motion

The anime.js animations can be moved to [Motion](https://motion.dev/). That means rewriting the current `animate()` calls. `ScrambleText.js` and `themeWipe.js` are separate and would stay as they are.

### Database

- **MySQL** — users, forms, questions, answers, verification codes

### Current Team

- ConcreteYt
- stoli9000-source
- steinbaron11

---

## License

Open Surveys is licensed under the [MIT License](LICENSE).

Copyright (c) 2026 **OpenSurveysDev**. You may use, modify, and distribute this software freely. If you use this project or substantial portions of it, please retain the copyright notice in the [LICENSE](LICENSE) file.
