# RTGPoll

## What this software is

RTGPoll is a full-stack survey (polling) web application built as a school learning project. Users can register, sign in, create and edit surveys with questions, and share them so others can fill them out — without needing an account to respond. Admins can manage user accounts.

The project is organized as a **monorepo**: the React frontend and Spring Boot backend live in one Git repository.

| Folder | Purpose |
|--------|---------|
| `rtgpoll-backend/` | Java REST API, database access, JWT authentication, and (in production) serving the built React app |
| `rtgpoll-frontend/` | React single-page application, forms, routing, i18n, and API calls |
| `docs/` | Course documentation, learning tasks, API notes, and deployment guides |
| `teacher/` | Teaching materials (lesson plans, demos, common mistakes) |
| `installinstructions.md` | One-time installs: Java, Node, MySQL, Workbench, `.env`, logo, optional Google/SMTP |
| `runinstructions.md` | How to start backend + frontend locally (and optional Cloudflare Tunnel demos) |

### ⚠️ VERY IMPORTANT — Navbar logo and `.env` files

The navbar brand image (`logo.png`) and real secrets (`.env`) are **gitignored** and are **not** included in the repo. If you clone or host RTGPoll yourself, you **must**:

1. Add your own logo at `rtgpoll-frontend/public/logo.png`
2. Copy each `.env.example` to `.env` and fill in real values (`rtgpoll-backend/.env` and `rtgpoll-frontend/.env`)

Without these, the navbar logo is broken and auth / database / email will not work. See [`installinstructions.md`](installinstructions.md) for tool installs and first-time setup, then [`runinstructions.md`](runinstructions.md) to start the servers.

---

## Goals

- Build a working **full-stack** application with a clear separation between frontend and backend
- Connect React and Spring Boot over a **REST API** using JSON
- Practice **team development** with Git (branches, pull requests, shared ownership)
- Learn core backend concepts: entities, repositories, controllers, DTOs, and validation
- Learn core frontend concepts: components, routing, forms, and authenticated API calls
- Model real-world access rules: only signed-in users create/edit surveys; anyone can view and submit answers; survey creators can choose public or private response visibility; admins manage accounts

---

## Current functionality

### Authentication

- **Sign up** (`POST /auth/signup`) — creates a user account (role `USER`) with a BCrypt-hashed password and returns a JWT plus role/username
- **Login** (`POST /auth/login`) — verifies credentials and returns a JWT plus role/username
- **Google Sign-In** (`POST /auth/google`) — frontend gets a Google ID token via Google Identity Services, backend verifies it, then finds/links/creates a user and returns the same JWT shape as login/signup
  - **New Google user** — account is created with `googleId`, email, name, a unique username derived from the email local-part, and `password = null` (password login is not available until they set one)
  - **Returning Google user** — matched by `googleId` and logged in
  - **Account linking** — if a password account already exists with the same verified email, `googleId` is attached to that user (no duplicate account); username/password login still works afterward
  - **Conflict** — if that email is already linked to a different Google account, the request is rejected (`409`)
- **Client ID / secrets setup** — copy the example env files and fill in real values (`.env` is gitignored):
  - `rtgpoll-frontend/.env.example` → `rtgpoll-frontend/.env` (`REACT_APP_GOOGLE_CLIENT_ID`)
  - `rtgpoll-backend/.env.example` → `rtgpoll-backend/.env` (`GOOGLE_CLIENT_ID`, plus DB / JWT / admin secrets)
  - Use the **same** Google Cloud **Web** Client ID in both `.env` files
  - In Google Cloud Console, add authorized JavaScript origins (e.g. `http://localhost:3000`, and `http://localhost:8080` if serving the built SPA from Spring)
  - Restart the React dev server after changing frontend `.env` (CRA reads env vars at startup)
- Tokens are stored in the browser (`localStorage` keys: `token`, `username`, `role`, …) and sent as `Authorization: Bearer <token>` on protected requests
- Auth helpers live in `rtgpoll-frontend/src/api/client.js` (`isAuthenticated`, `isAdmin`, `logout`, `clearAuth`); Google helper in `rtgpoll-frontend/src/auth/googleSignIn.js`
- **Protected routes** (`/login-home`, `/surveys`, `/editor`, `/editor/:formId`, `/user-view`) require a non-expired JWT; otherwise the user is redirected to `/home`
- **Admin-only route** (`/admin`) requires an authenticated user with `role === 'ADMIN'`; non-admins go to `/login-home`
- **Guest-only routes** (`/auth/login`, `/auth/signup`) redirect already authenticated users to `/login-home`
- After login, **admins** are sent to `/admin`; everyone else to `/login-home`
- **Token validity on the client** — `isAuthenticated()` checks JWT expiry (`exp`); missing, malformed, or expired tokens are cleared from `localStorage`
- **Logout** — account menu “Log out” clears auth keys from `localStorage` and navigates to `/home` (stateless JWT; no backend logout endpoint)
- **401 on authenticated API calls** — session is cleared when a request that sent a bearer token receives `401` (login/signup credential errors do not clear storage)

### Roles and admin

- Users have a `role` of `USER` or `ADMIN` (stored on the `USER` table; new signups default to `USER`)
- On startup, `AdminBootstrap` ensures a testable admin account exists (credentials from `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD` in `rtgpoll-backend/.env`; defaults username `admin`)
- **List users** (`GET /users`) — admin only; passwords are never exposed
- **Update user** (`PUT /users/{id}`) — admin only; edit name, username, email, and role (cannot demote the last admin; username uniqueness enforced)
- **Delete user** (`DELETE /users/{id}`) — admin only; cannot delete own account or the last admin; owned surveys are removed first
- **Admin UI** (`/admin`) — wired to the user APIs: list, edit, and delete accounts

### Surveys (forms)

**Backend (implemented)**

- **Create a survey** (`POST /forms`) — authenticated; the logged-in user becomes the form creator; accepts `responsesPublic` (defaults to `true` if omitted)
- **List my surveys** (`GET /forms`) — authenticated; returns forms created by the logged-in user (newest first)
- **Update a survey** (`PUT /forms/{id}`) — authenticated; only the creator may edit; questions with an existing id are updated in place (answers stay linked); new questions are added; omitted questions are removed; `responsesPublic` can be toggled
- **View a survey** (`GET /forms/{id}`) — public; includes questions, the creator’s username, and `responsesPublic`
- **Submit answers** (`POST /forms/{id}/answers`) — public; anonymous users can respond without logging in
- **View results** (`GET /forms/{id}/responses`) — depends on the form’s `responsesPublic` flag:
  - **Public responses** (`responsesPublic: true`, default) — anyone with the form id can load aggregated answers
  - **Private responses** (`responsesPublic: false`) — only the authenticated form creator may view results (`403` otherwise); filling the survey stays public either way
- **Question types** (stored as `questionType` integers):
  - `0` — skippable text (display-only prompt; no answer field)
  - `1` — text (user must type their own answer)
  - `2` — multiple choice (`questionOptions` uses `;` separators; optional trailing `N!` sets how many must be chosen, default 1). Examples: `"Math;Physics;Informatik"` (pick 1), `"Math;Physics;Informatik;2!"` (pick 2 of 3), `"Art;Music;Karate;Biology;3!"` (pick 3 of 4)
  - `3` — rating (star scale from 1 to max; `questionOptions` is the max as a whole number from **5 to 10**, e.g. `"5"` or `"10"`)

**Frontend (implemented)**

- **Access code on `/home`** — enter an 8-digit code (the form id, leading zeros allowed) to open a survey
- **Fill-out page** (`/access?formId=…`) — loads the form, renders all question types, submits answers, and can show aggregated responses afterward when responses are public (`?view=responses` for creators opening results from `/surveys`); for private surveys, respondents see a short note instead of “View responses”
- Completion is remembered per form in `localStorage` (`survey-completed-{formId}`) so the same browser is guided toward results after submitting (when responses are public)
- **View surveys** (`/surveys`) — authenticated list of the user’s own surveys (name, 8-digit access code, question count) with links to open, view results, or edit (owners can always open results for private surveys while logged in)
- **Survey editor** (`/editor` create, `/editor/:formId` edit) — authenticated create/edit UI for all question types plus a **Public responses** toggle; create uses `POST /forms`, edit uses `PUT /forms/{id}`; after save, navigates to `/surveys`
- Dashboard **Open editor** / **View surveys** buttons and nav **Survey Editor** / **Surveys** links are wired

### Internationalization (i18n)

- UI strings support **English** and **German** (`rtgpoll-frontend/src/i18n/`)
- Locale is cycled from the navbar and persisted in `localStorage` as `rtg-locale`
- Survey content from the API is not auto-translated (only chrome / labels)

### Site footer (planned — not implemented yet)

A shared site footer is planned for legal and informational links. It is **not in the UI yet**; this section records the intended design so it can be built later.

**Intended contents**

- **About us** — project / team overview
- **Legal** — imprint / legal notice (Impressum-style information as required)
- **EULA** — end-user license agreement
- **Privacy policy** — how user and survey data are handled
- Optional extra links (e.g. contact, terms of use) as needed

**Intended implementation**

- Add a shared `Footer` component under `rtgpoll-frontend/src/layout/`
- Render it once in `App.js` inside `app-shell` (alongside the per-route navbars) so it appears on most routes
- Add public pages/routes such as `/about`, `/legal`, `/eula`, and `/privacy`, linked from the footer
- Style with existing theme tokens (`--app-navbar-bg`, `--app-text`, `--accent`) so light/dark mode and accent colors stay consistent
- Optionally hide the footer on focused flows such as survey fill-out (`/access`)

### Still stubbed / placeholder

- **`/user-view`** — static sample table; does not call `GET /users` (admin directory is `/admin` instead)
- Some secondary UI actions (e.g. account **Settings**, **GitHub** login) still show “This feature is not yet available”

### Frontend pages

| Path | Access | Role |
|------|--------|------|
| `/home` | Public | Landing page + survey access-code entry |
| `/access` | Public | Fill out a survey / view responses (results only if the survey allows public responses, or if the owner is logged in) |
| `/auth/login` | Guest only | Login form (password + Google Sign-In) |
| `/auth/signup` | Guest only | Sign-up form (password + Google Sign-In) |
| `/login-home` | Authenticated | Post-login dashboard (open editor / view surveys) |
| `/surveys` | Authenticated | List of the user’s surveys (`GET /forms`) |
| `/editor` | Authenticated | Create a new survey (`POST /forms`) |
| `/editor/:formId` | Authenticated | Edit an existing survey (`PUT /forms/{id}`) |
| `/admin` | Admin only | User directory and management |
| `/user-view` | Authenticated | Placeholder user table |

Also:

- Light/dark theme toggle (persisted as `rtg-theme`) with a wipe animation
- Accent color cycle (persisted as `rtg-accent`) with the same wipe animation
- Language toggle EN ↔ DE (persisted as `rtg-locale`), with scramble text on label changes
- Glass-style Bootstrap UI, animated background blobs, page enter transitions, and button hover motion (`animejs`)
- Client routing guards: `ProtectedRoute`, `GuestRoute`, and `AdminRoute` in `App.js`

### Architecture notes

- **Stateless JWT auth** — no server-side sessions; Spring Security validates tokens on each request; the frontend additionally checks expiry and clears bad tokens
- **Secrets via `.env`** — DB, JWT, admin password, and Google Client ID live in gitignored `.env` files (see `.env.example`); backend loads them at startup via dotenv-java
- **Google OAuth** — Google Identity Services issues an ID token in the browser; `GoogleTokenVerifier` checks signature and audience; the app still issues its own JWT afterward (no Spring session for Google)
- **Role enforcement** — UI gates admin pages with `isAdmin()`; backend still requires `ROLE_ADMIN` on `/users/**`
- **Development**: React dev server (port 3000) proxies API calls to Spring Boot (port 8080) via `package.json` `"proxy"`
- **Production**: the React build can be served from the backend’s `static` folder on port 8080 (`SpaController` falls back to `index.html` for SPA routes including `/editor` and `/admin`)
- **How to run**: see [`runinstructions.md`](runinstructions.md) — includes the **required** `logo.png` and `.env` setup for anyone hosting their own instance

---

## Plugins / languages used

### Languages

| Language | Where |
|----------|-------|
| **Java 25** | Backend application code |
| **JavaScript** | Frontend (React) |
| **SQL** | MySQL database (via JPA/Hibernate) |
| **HTML / CSS** | Frontend markup and styling |

### Backend (Maven / Spring Boot)

| Dependency | Role |
|------------|------|
| **Spring Boot 4.1** | Application framework |
| **Spring Web MVC** | REST controllers and HTTP handling |
| **Spring Data JPA** | Database repositories and entity mapping |
| **Spring Security** | Route protection and password encoding (BCrypt) |
| **Spring Validation** | Request validation |
| **MySQL Connector** | MySQL database driver |
| **JJWT** (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) | JWT creation and verification |
| **Google API Client** (`google-api-client`) | Verifies Google ID tokens (`GoogleTokenVerifier`) |
| **Lombok** | Boilerplate reduction on DTOs and models |
| **Maven** | Build and dependency management |

### Frontend (npm)

| Package | Role |
|---------|------|
| **React 19** | UI components and state |
| **React DOM** | Rendering |
| **React Router DOM** | Client-side routing and protected routes |
| **Create React App** (`react-scripts`) | Dev server, build tooling, and test runner |
| **Bootstrap 5** | Layout and UI components |
| **animejs** | Theme wipe, page transitions, blobs, and hover motion |
| **fetch API** | HTTP calls to the backend (`src/api/client.js`) |
| **Google Identity Services** | Browser Google Sign-In (script in `public/index.html`; helper in `src/auth/googleSignIn.js`) |
| **Custom i18n** (`src/i18n/`) | English / German UI strings |
| **Testing Library** | Component tests |

### Database

- **MySQL** — stores users (including `role` and optional `google_id`), forms (including `responses_public`), questions, and answers (`USER`, `FORM`, `QUESTION`, `ANSWER`)

---

# RTGPoll (Deutsch)

## Was diese Software ist

RTGPoll ist eine Full-Stack-Umfrage-Webanwendung, die als Schul-Lernprojekt entwickelt wird. Nutzer können sich registrieren, anmelden, Umfragen mit Fragen erstellen und bearbeiten und diese teilen, sodass andere sie ausfüllen können — ohne dafür ein Konto zu benötigen. Admins können Benutzerkonten verwalten.

Das Projekt ist als **Monorepo** organisiert: React-Frontend und Spring-Boot-Backend liegen in einem Git-Repository.

| Ordner | Zweck |
|--------|-------|
| `rtgpoll-backend/` | Java-REST-API, Datenbankzugriff, JWT-Authentifizierung und (in Produktion) Auslieferung der gebauten React-App |
| `rtgpoll-frontend/` | React-Single-Page-Application, Formulare, Routing, i18n und API-Aufrufe |
| `docs/` | Kursdokumentation, Lernaufgaben, API-Hinweise und Deployment-Anleitungen |
| `teacher/` | Unterrichtsmaterialien (Unterrichtspläne, Demos, häufige Fehler) |
| `runinstructions.md` | Anleitung zum lokalen Start von Backend + Frontend (und optionale Cloudflare-Tunnel-Demos) |

### ⚠️ SEHR WICHTIG — Navbar-Logo und `.env`-Dateien

Das Navbar-Markenbild (`logo.png`) und echte Secrets (`.env`) sind **gitignored** und **nicht** im Repo enthalten. Wenn du RTGPoll selbst klonst oder hostest, **musst** du:

1. Dein eigenes Logo unter `rtgpoll-frontend/public/logo.png` ablegen
2. Jede `.env.example` nach `.env` kopieren und echte Werte eintragen (`rtgpoll-backend/.env` und `rtgpoll-frontend/.env`)

Ohne diese Schritte ist das Navbar-Logo kaputt und Auth / Datenbank / E-Mail funktionieren nicht. Die vollständigen Schritte stehen in [`runinstructions.md`](runinstructions.md).

---

## Ziele

- Eine funktionierende **Full-Stack**-Anwendung mit klarer Trennung von Frontend und Backend aufbauen
- React und Spring Boot über eine **REST-API** mit JSON verbinden
- **Teamarbeit** mit Git üben (Branches, Pull Requests, gemeinsame Verantwortung)
- Zentrale Backend-Konzepte lernen: Entities, Repositories, Controller, DTOs und Validierung
- Zentrale Frontend-Konzepte lernen: Komponenten, Routing, Formulare und authentifizierte API-Aufrufe
- Realistische Zugriffsregeln umsetzen: Nur angemeldete Nutzer erstellen/bearbeiten Umfragen; jeder kann Umfragen ansehen und Antworten abgeben; Umfrage-Ersteller können öffentliche oder private Antwortsichtbarkeit wählen; Admins verwalten Konten

---

## Aktuelle Funktionalität

### Authentifizierung

- **Registrierung** (`POST /auth/signup`) — erstellt ein Benutzerkonto (Rolle `USER`) mit BCrypt-gehashtem Passwort und gibt JWT plus Rolle/Benutzername zurück
- **Anmeldung** (`POST /auth/login`) — prüft Zugangsdaten und gibt JWT plus Rolle/Benutzername zurück
- **Google-Anmeldung** (`POST /auth/google`) — das Frontend holt ein Google-ID-Token über Google Identity Services; das Backend prüft es, findet/verknüpft/erstellt den Nutzer und gibt dasselbe JWT-Format wie bei Login/Signup zurück
  - **Neuer Google-Nutzer** — Konto mit `googleId`, E-Mail, Name, eindeutigem Benutzernamen (aus dem E-Mail-Lokalteil) und `password = null` (Passwort-Login erst nach späterem Setzen möglich)
  - **Wiederkehrender Google-Nutzer** — Zuordnung über `googleId` und Anmeldung
  - **Kontoverknüpfung** — existiert bereits ein Passwort-Konto mit derselben verifizierten E-Mail, wird `googleId` an dieses Konto gehängt (kein Duplikat); Passwort-Login bleibt danach möglich
  - **Konflikt** — ist die E-Mail bereits mit einem anderen Google-Konto verknüpft, wird die Anfrage abgelehnt (`409`)
- **Client-ID / Secrets einrichten** — Beispiel-Env-Dateien kopieren und echte Werte eintragen (`.env` ist gitignored):
  - `rtgpoll-frontend/.env.example` → `rtgpoll-frontend/.env` (`REACT_APP_GOOGLE_CLIENT_ID`)
  - `rtgpoll-backend/.env.example` → `rtgpoll-backend/.env` (`GOOGLE_CLIENT_ID`, plus DB- / JWT- / Admin-Secrets)
  - Dieselbe Google-Cloud-**Web**-Client-ID in beiden `.env`-Dateien verwenden
  - In der Google Cloud Console autorisierte JavaScript-Origins setzen (z. B. `http://localhost:3000`, und `http://localhost:8080` falls die gebaute SPA von Spring ausgeliefert wird)
  - React-Dev-Server nach Änderungen an der Frontend-`.env` neu starten (CRA liest Env-Vars beim Start)
- Tokens werden im Browser gespeichert (`localStorage`-Schlüssel: `token`, `username`, `role`, …) und bei geschützten Anfragen als `Authorization: Bearer <token>` mitgesendet
- Auth-Hilfsfunktionen liegen in `rtgpoll-frontend/src/api/client.js` (`isAuthenticated`, `isAdmin`, `logout`, `clearAuth`); Google-Helfer in `rtgpoll-frontend/src/auth/googleSignIn.js`
- **Geschützte Routen** (`/login-home`, `/surveys`, `/editor`, `/editor/:formId`, `/user-view`) erfordern ein nicht abgelaufenes JWT; sonst Weiterleitung nach `/home`
- **Admin-Route** (`/admin`) erfordert einen angemeldeten Nutzer mit `role === 'ADMIN'`; Nicht-Admins gehen nach `/login-home`
- **Gast-Routen** (`/auth/login`, `/auth/signup`) leiten bereits angemeldete Nutzer nach `/login-home` um
- Nach dem Login werden **Admins** nach `/admin` geleitet, alle anderen nach `/login-home`
- **Token-Gültigkeit im Client** — `isAuthenticated()` prüft die JWT-Ablaufzeit (`exp`); fehlende, ungültige oder abgelaufene Tokens werden aus dem `localStorage` entfernt
- **Abmelden** — „Log out“ im Account-Menü löscht Auth-Schlüssel aus dem `localStorage` und navigiert nach `/home` (zustandsloses JWT; kein Backend-Logout-Endpunkt)
- **401 bei authentifizierten API-Aufrufen** — die Sitzung wird gelöscht, wenn eine Anfrage mit Bearer-Token `401` erhält (fehlerhafte Login-/Signup-Zugangsdaten löschen den Speicher nicht)

### Rollen und Admin

- Nutzer haben die Rolle `USER` oder `ADMIN` (in der `USER`-Tabelle; neue Registrierungen starten als `USER`)
- Beim Start stellt `AdminBootstrap` ein testbares Admin-Konto sicher (Zugangsdaten aus `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD` in `rtgpoll-backend/.env`; Standard-Benutzername `admin`)
- **Benutzer auflisten** (`GET /users`) — nur Admin; Passwörter werden nie ausgegeben
- **Benutzer aktualisieren** (`PUT /users/{id}`) — nur Admin; Name, Benutzername, E-Mail und Rolle (letzter Admin darf nicht degradiert werden; Benutzernamen müssen eindeutig sein)
- **Benutzer löschen** (`DELETE /users/{id}`) — nur Admin; eigenes Konto und letzter Admin sind geschützt; eigene Umfragen werden vorher entfernt
- **Admin-UI** (`/admin`) — an die User-APIs angebunden: auflisten, bearbeiten und löschen

### Umfragen (Formulare)

**Backend (implementiert)**

- **Umfrage erstellen** (`POST /forms`) — authentifiziert; der angemeldete Nutzer wird Ersteller der Umfrage; akzeptiert `responsesPublic` (Standard `true`, falls weggelassen)
- **Meine Umfragen auflisten** (`GET /forms`) — authentifiziert; liefert Umfragen des angemeldeten Nutzers (neueste zuerst)
- **Umfrage aktualisieren** (`PUT /forms/{id}`) — authentifiziert; nur der Ersteller darf bearbeiten; bestehende Fragen-IDs werden aktualisiert (Antworten bleiben verknüpft); neue Fragen werden hinzugefügt; weggelassene Fragen werden entfernt; `responsesPublic` kann umgeschaltet werden
- **Umfrage anzeigen** (`GET /forms/{id}`) — öffentlich; enthält Fragen, den Benutzernamen des Erstellers und `responsesPublic`
- **Antworten absenden** (`POST /forms/{id}/answers`) — öffentlich; anonyme Nutzer können ohne Anmeldung antworten
- **Ergebnisse anzeigen** (`GET /forms/{id}/responses`) — hängt vom Flag `responsesPublic` der Umfrage ab:
  - **Öffentliche Antworten** (`responsesPublic: true`, Standard) — jeder mit der Form-ID kann aggregierte Antworten laden
  - **Private Antworten** (`responsesPublic: false`) — nur der authentifizierte Ersteller darf Ergebnisse sehen (sonst `403`); das Ausfüllen bleibt in beiden Fällen öffentlich
- **Fragetypen** (gespeichert als `questionType`-Ganzzahlen):
  - `0` — überspringbarer Text (nur Anzeige; kein Antwortfeld)
  - `1` — Text (Nutzer muss eine eigene Antwort tippen)
  - `2` — Multiple Choice (`questionOptions` mit `;` getrennt; optionales `N!` am Ende = Pflichtanzahl, Standard 1). Beispiele: `"Math;Physics;Informatik"` (1 wählen), `"Math;Physics;Informatik;2!"` (2 von 3), `"Art;Music;Karate;Biology;3!"` (3 von 4)
  - `3` — Bewertung (Sterne von 1 bis max; `questionOptions` ist das Maximum als ganze Zahl von **5 bis 10**, z. B. `"5"` oder `"10"`)

**Frontend (implementiert)**

- **Zugangscode auf `/home`** — 8-stelligen Code eingeben (Form-ID, führende Nullen erlaubt), um eine Umfrage zu öffnen
- **Ausfüllseite** (`/access?formId=…`) — lädt das Formular, rendert alle Fragetypen, sendet Antworten und kann danach aggregierte Ergebnisse zeigen, wenn Antworten öffentlich sind (`?view=responses` für Ersteller von `/surveys`); bei privaten Umfragen sehen Teilnehmende einen kurzen Hinweis statt „Antworten anzeigen“
- Abschluss wird pro Formular im `localStorage` gemerkt (`survey-completed-{formId}`), damit derselbe Browser nach dem Absenden zu den Ergebnissen geführt wird (wenn Antworten öffentlich sind)
- **Umfragen anzeigen** (`/surveys`) — authentifizierte Liste der eigenen Umfragen (Name, 8-stelliger Zugangscode, Fragenanzahl) mit Links zum Öffnen, zu Ergebnissen oder zum Bearbeiten (Ersteller können Ergebnisse privater Umfragen im angemeldeten Zustand immer öffnen)
- **Umfrage-Editor** (`/editor` erstellen, `/editor/:formId` bearbeiten) — authentifizierte Create/Edit-UI für alle Fragetypen plus Schalter **Öffentliche Antworten**; Erstellen nutzt `POST /forms`, Bearbeiten `PUT /forms/{id}`; nach dem Speichern Weiterleitung nach `/surveys`
- Dashboard-Buttons **Open editor** / **View surveys** und Nav-Links **Survey Editor** / **Surveys** sind angebunden

### Internationalisierung (i18n)

- UI-Texte unterstützen **Englisch** und **Deutsch** (`rtgpoll-frontend/src/i18n/`)
- Die Sprache wird in der Navbar umgeschaltet und in `localStorage` als `rtg-locale` gespeichert
- Umfrageinhalte aus der API werden nicht automatisch übersetzt (nur Chrome / Labels)

### Seiten-Footer (geplant — noch nicht implementiert)

Ein gemeinsamer Seiten-Footer für rechtliche und informative Links ist geplant. Er ist **noch nicht in der UI**; dieser Abschnitt hält das vorgesehene Design fest, damit es später umgesetzt werden kann.

**Vorgesehene Inhalte**

- **About us / Über uns** — Projekt- bzw. Teamübersicht
- **Legal / Rechtliches** — Impressum bzw. rechtliche Hinweise
- **EULA** — Endbenutzer-Lizenzvertrag
- **Datenschutz** — Umgang mit Nutzer- und Umfragedaten
- Optional weitere Links (z. B. Kontakt, Nutzungsbedingungen) nach Bedarf

**Vorgesehene Umsetzung**

- Gemeinsame `Footer`-Komponente unter `rtgpoll-frontend/src/layout/`
- Einmaliges Einbinden in `App.js` innerhalb von `app-shell` (neben den routenspezifischen Navbars), damit der Footer auf den meisten Routen erscheint
- Öffentliche Seiten/Routen wie `/about`, `/legal`, `/eula` und `/privacy`, verlinkt aus dem Footer
- Styling mit bestehenden Theme-Tokens (`--app-navbar-bg`, `--app-text`, `--accent`), damit Hell-/Dunkelmodus und Akzentfarben konsistent bleiben
- Optional Ausblenden des Footers bei fokussierten Abläufen wie dem Ausfüllen einer Umfrage (`/access`)

### Noch Platzhalter / Stub

- **`/user-view`** — statische Beispieltabelle; ruft `GET /users` nicht auf (Admin-Verzeichnis ist `/admin`)
- Einige sekundäre UI-Aktionen (z. B. Account-**Settings**, **GitHub**-Login) zeigen noch „This feature is not yet available“

### Frontend-Seiten

| Pfad | Zugang | Rolle |
|------|--------|------|
| `/home` | Öffentlich | Startseite + Eingabe des Umfrage-Zugangscodes |
| `/access` | Öffentlich | Umfrage ausfüllen / Antworten ansehen (Ergebnisse nur bei öffentlichen Antworten oder wenn der Ersteller angemeldet ist) |
| `/auth/login` | Nur Gäste | Anmeldeformular (Passwort + Google-Anmeldung) |
| `/auth/signup` | Nur Gäste | Registrierungsformular (Passwort + Google-Anmeldung) |
| `/login-home` | Authentifiziert | Dashboard nach Login (Editor öffnen / Umfragen ansehen) |
| `/surveys` | Authentifiziert | Liste der eigenen Umfragen (`GET /forms`) |
| `/editor` | Authentifiziert | Neue Umfrage erstellen (`POST /forms`) |
| `/editor/:formId` | Authentifiziert | Bestehende Umfrage bearbeiten (`PUT /forms/{id}`) |
| `/admin` | Nur Admin | Benutzerverzeichnis und -verwaltung |
| `/user-view` | Authentifiziert | Platzhalter-Benutzertabelle |

Außerdem:

- Hell/Dunkel-Theme-Umschalter (gespeichert als `rtg-theme`) mit Wipe-Animation
- Akzentfarben-Zyklus (gespeichert als `rtg-accent`) mit derselben Wipe-Animation
- Sprachumschalter EN ↔ DE (gespeichert als `rtg-locale`) mit Scramble-Text bei Label-Wechseln
- Glass-Style-Bootstrap-UI, animierte Hintergrund-Blobs, Seiteneingangs-Übergänge und Button-Hover (`animejs`)
- Client-Routing-Guards: `ProtectedRoute`, `GuestRoute` und `AdminRoute` in `App.js`

### Architektur-Hinweise

- **Zustandslose JWT-Authentifizierung** — keine serverseitigen Sessions; Spring Security validiert Tokens bei jeder Anfrage; das Frontend prüft zusätzlich die Ablaufzeit und entfernt ungültige Tokens
- **Secrets über `.env`** — DB, JWT, Admin-Passwort und Google-Client-ID liegen in gitignored `.env`-Dateien (siehe `.env.example`); das Backend lädt sie beim Start über dotenv-java
- **Google OAuth** — Google Identity Services stellt im Browser ein ID-Token aus; `GoogleTokenVerifier` prüft Signatur und Audience; danach stellt die App weiterhin ihr eigenes JWT aus (keine Spring-Session für Google)
- **Rollenprüfung** — die UI sperrt Admin-Seiten mit `isAdmin()`; das Backend verlangt weiterhin `ROLE_ADMIN` für `/users/**`
- **Entwicklung**: Der React-Dev-Server (Port 3000) leitet API-Aufrufe an Spring Boot (Port 8080) über `"proxy"` in `package.json` weiter
- **Produktion**: Der React-Build kann vom `static`-Ordner des Backends auf Port 8080 ausgeliefert werden (`SpaController` liefert für SPA-Routen inkl. `/editor` und `/admin` `index.html`)
- **Startanleitung**: siehe [`runinstructions.md`](runinstructions.md) — enthält den **pflichtmäßigen** `logo.png`- und `.env`-Schritt für alle, die eine eigene Instanz hosten

---

## Plugins / verwendete Sprachen

### Sprachen

| Sprache | Einsatzbereich |
|---------|----------------|
| **Java 25** | Backend-Anwendungscode |
| **JavaScript** | Frontend (React) |
| **SQL** | MySQL-Datenbank (über JPA/Hibernate) |
| **HTML / CSS** | Frontend-Markup und Styling |

### Backend (Maven / Spring Boot)

| Abhängigkeit | Rolle |
|--------------|-------|
| **Spring Boot 4.1** | Anwendungsframework |
| **Spring Web MVC** | REST-Controller und HTTP-Verarbeitung |
| **Spring Data JPA** | Datenbank-Repositories und Entity-Mapping |
| **Spring Security** | Routenschutz und Passwort-Kodierung (BCrypt) |
| **Spring Validation** | Anfrage-Validierung |
| **MySQL Connector** | MySQL-Datenbanktreiber |
| **JJWT** (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) | JWT-Erstellung und -Prüfung |
| **Google API Client** (`google-api-client`) | Prüfung von Google-ID-Tokens (`GoogleTokenVerifier`) |
| **Lombok** | Reduzierung von Boilerplate-Code in DTOs und Modellen |
| **Maven** | Build- und Abhängigkeitsverwaltung |

### Frontend (npm)

| Paket | Rolle |
|-------|-------|
| **React 19** | UI-Komponenten und State |
| **React DOM** | Rendering |
| **React Router DOM** | Client-seitiges Routing und geschützte Routen |
| **Create React App** (`react-scripts`) | Dev-Server, Build-Tools und Test-Runner |
| **Bootstrap 5** | Layout und UI-Komponenten |
| **animejs** | Theme-Wipe, Seitenübergänge, Blobs und Hover-Animationen |
| **fetch API** | HTTP-Aufrufe ans Backend (`src/api/client.js`) |
| **Google Identity Services** | Google-Anmeldung im Browser (Script in `public/index.html`; Helfer in `src/auth/googleSignIn.js`) |
| **Eigenes i18n** (`src/i18n/`) | Englische / deutsche UI-Texte |
| **Testing Library** | Komponententests |

### Datenbank

- **MySQL** — speichert Benutzer (inkl. `role` und optionalem `google_id`), Formulare (inkl. `responses_public`), Fragen und Antworten (`USER`, `FORM`, `QUESTION`, `ANSWER`)
