# Run Instructions

How to **start** the servers. For installing Java, Node, MySQL, Workbench, logo, `.env` files, Google OAuth, and SMTP, use [`installinstructions.md`](installinstructions.md) first.

This project has two servers:

- `opensurveys-backend/` for the Spring Boot API on port `8080`
- `opensurveys-frontend/` for the React/npm app on port `3000`

## Before you run

Confirm the install checklist in [`installinstructions.md`](installinstructions.md) is done:

- Java 25, Node.js, and MySQL Server are installed
- MySQL is **running** and database `opensurveys` exists
- `opensurveys-frontend/public/logo.png` is in place
- `opensurveys-backend/.env` and `opensurveys-frontend/.env` exist and are filled in
- `npm install` has been run once in `opensurveys-frontend/`
- No OS environment variables (`DB_URL`, etc.) override `.env` with a remote database

If the navbar logo is missing or auth / database / email fails, fix setup in the install guide — not by changing these run steps.

---

## Test Build

Use this for local development: React on port `3000`, Spring Boot on port `8080`. The CRA proxy forwards API calls to the backend.

### Start the Spring Boot server

1. Open a terminal.
2. Go to the backend folder:

```powershell
cd opensurveys-backend
```

3. Start the Spring Boot app with the Maven Wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

4. Wait until the backend finishes starting and is running on `http://localhost:8080`.
5. In the startup log, confirm **Database JDBC URL** shows your intended host (for local MySQL: `localhost`). If you see an unexpected remote host, stop the server and fix `DB_URL` / OS env vars per [`installinstructions.md`](installinstructions.md).

### Stop the Spring Boot server

1. Go to the terminal where the backend is running.
2. Press `Ctrl + C`.

### Start the npm frontend server

1. Open a second terminal.
2. Go to the frontend folder:

```powershell
cd opensurveys-frontend
```

3. If this is your first time running the frontend (or you have not done it yet during install), install dependencies:

```powershell
npm install
```

4. Start the frontend:

```powershell
npm start
```

5. Open `http://localhost:3000` in your browser.

### Stop the npm frontend server

1. Go to the terminal where the frontend is running.
2. Press `Ctrl + C`.

### Recommended order

1. Start the Spring Boot backend first.
2. Start the npm frontend second.

The frontend is configured to send API requests to `http://localhost:8080`.

### Share a short demo with Cloudflare Tunnel

By default the app only runs on `localhost`, so people on the internet cannot open it. For a short demo, use a Cloudflare quick tunnel so visitors get a public HTTPS URL that forwards to your local frontend.

Only tunnel port `3000`. The React dev server proxies API calls to your local backend on `8080`, so visitors do not need a separate tunnel for the backend.

#### 1. Start OpenSurveys locally

1. Start MySQL (see [`installinstructions.md`](installinstructions.md) if it is not installed yet).
2. Start the Spring Boot backend on port `8080`.
3. Start the npm frontend on port `3000`.
4. Confirm `http://localhost:3000` works on your PC before opening the tunnel.

#### 2. Install cloudflared

In PowerShell:

```powershell
winget install --id Cloudflare.cloudflared
```

Or download it from: https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/downloads/

After install, **close that terminal completely and open a new one**. Old terminals keep a stale PATH and will report `cloudflared` as not recognized even though it is installed.

Then check:

```powershell
cloudflared --version
```

If it is still not recognized, call it by full path instead:

```powershell
& "C:\Program Files (x86)\cloudflared\cloudflared.exe" --version
```

#### 3. Start a quick tunnel

Open a **new** third terminal and run:

```powershell
cloudflared tunnel --url http://localhost:3000
```

Or with the full path:

```powershell
& "C:\Program Files (x86)\cloudflared\cloudflared.exe" tunnel --url http://localhost:3000
```

cloudflared prints a public URL like `https://something-random.trycloudflare.com`. Share that link for the demo.

No Cloudflare account is required for this quick-tunnel mode. The free URL changes every time you restart `cloudflared`.

#### 4. Stop the tunnel

1. Leave backend, frontend, and `cloudflared` running while the demo is live.
2. When finished, press `Ctrl + C` in the cloudflared terminal.
3. Stop the frontend and backend as usual.

If the page loads but API calls fail, make sure the Spring Boot backend is still running on `8080`.

---

## Production Build

Prerequisite installs and `.env` setup are the same as local — see [`installinstructions.md`](installinstructions.md).

In production, Spring Boot serves the built React app from `src/main/resources/static/` on port `8080` (API + SPA together). You do **not** run `npm start`.

Leave `REACT_APP_API_URL` unset in `opensurveys-frontend/.env` when the SPA is served by this backend (same origin). Set it only if the API is on a different host.

### 1. Build the frontend

```powershell
cd opensurveys-frontend
npm install
npm run build
```

This writes the production bundle to `opensurveys-frontend/build/`.

### 2. Copy the build into the backend

Replace the contents of Spring’s static folder with the new build:

```powershell
Remove-Item -Recurse -Force ..\opensurveys-backend\src\main\resources\static\*
Copy-Item -Recurse -Force .\build\* ..\opensurveys-backend\src\main\resources\static\
```

macOS / Linux:

```bash
rm -rf ../opensurveys-backend/src/main/resources/static/*
cp -R build/* ../opensurveys-backend/src/main/resources/static/
```

### 3. Start the backend

```powershell
cd ..\opensurveys-backend
.\mvnw.cmd spring-boot:run
```

Or package a JAR and run it:

```powershell
.\mvnw.cmd package
java -jar target\<jar-name>.jar
```

(Use the actual JAR filename from `target/`.)

Open `http://localhost:8080` in your browser. If you use Google Sign-In, add `http://localhost:8080` (or your real host) as an authorized JavaScript origin in Google Cloud Console.
