# Install Instructions

One-time setup for tools and project config. After this, use `[runinstructions.md](runinstructions.md)` to start the servers.


| You need                                           | Why                                                         |
| -------------------------------------------------- | ----------------------------------------------------------- |
| **Git**                                            | Clone / pull the repo                                       |
| **Java 25**                                        | Run the Spring Boot backend                                 |
| **Node.js (LTS)**                                  | Run / build the React frontend (`npm`)                      |
| **MySQL Server**                                   | Store users, surveys, and answers                           |
| **MySQL Workbench** (optional)                     | GUI to create the database and inspect tables               |
| **Google Cloud OAuth client** (optional for local) | Google Sign-In                                              |
| **SMTP mailbox** (optional for local)              | Email verification codes for account email/password changes |


Maven is **not** required separately — the backend ships with the Maven Wrapper (`mvnw` / `mvnw.cmd`).

---



## 1. Git



### Windows

```powershell
winget install --id Git.Git -e
```

Or download: [https://git-scm.com/download/win](https://git-scm.com/download/win)

Close and reopen the terminal, then check:

```powershell
git --version
```



### macOS

```bash
xcode-select --install
# or: brew install git
git --version
```



### Linux (Debian/Ubuntu)

```bash
sudo apt update
sudo apt install git
git --version
```

Clone the project (adjust the URL if you use SSH or a fork):

```powershell
git clone <repository-url>
cd OpenSurveys
```

---



## 2. Java 25

The backend (`opensurveys-backend/pom.xml`) targets **Java 25**.

### Windows

```powershell
winget install --id Oracle.JDK.25 -e
```

If that package id is unavailable, install a JDK 25 build from:

- [https://www.oracle.com/java/technologies/downloads/](https://www.oracle.com/java/technologies/downloads/)
- or [https://adoptium.net/](https://adoptium.net/) (Temurin 25, if listed)

Close and reopen the terminal, then check:

```powershell
java -version
```

You should see a version line that includes `25`.

### macOS / Linux

Install JDK 25 via your package manager or Adoptium/Oracle, then:

```bash
java -version
```

Set `JAVA_HOME` to the JDK 25 install if `java -version` still points at an older JDK.

---



## 3. Node.js (includes npm)

Use an **LTS** release (recommended for Create React App).

### Windows

```powershell
winget install --id OpenJS.NodeJS.LTS -e
```

Or download: [https://nodejs.org/](https://nodejs.org/)

Close and reopen the terminal, then check:

```powershell
node -v
npm -v
```



### macOS

```bash
brew install node
# or install the LTS pkg from https://nodejs.org/
node -v
npm -v
```



### Linux (Debian/Ubuntu)

Prefer NodeSource or `nvm` for a current LTS; avoid very old distro Node packages if possible.

```bash
node -v
npm -v
```

Install frontend dependencies once (after Node is installed):

```powershell
cd opensurveys-frontend
npm install
```

---



## 4. MySQL Server

OpenSurveys uses **MySQL** (JDBC driver `com.mysql.cj.jdbc.Driver`). Microsoft SQL Server is not supported.

### Windows (official installer — recommended)

1. Download **MySQL Installer for Windows**: [https://dev.mysql.com/downloads/installer/](https://dev.mysql.com/downloads/installer/)
2. Run the installer and install **MySQL Server**.
3. Complete configuration:
  - Port: `3306` (default)
  - Set a **root password** and remember it
4. Confirm the Windows service is running (often named `MySQL80`):

```powershell
Get-Service *mysql*
```

Status should be **Running**. If it is Stopped:

```powershell
Start-Service MySQL80
```

(Use the exact service name from `Get-Service` if it differs.)

Alternative quick install:

```powershell
winget install Oracle.MySQL
```

If the MySQL Configurator later says `my.ini` / config file not found, the install is broken — uninstall MySQL completely, delete leftover folders under `C:\ProgramData\MySQL` and `C:\Program Files\MySQL` if present, reboot, and reinstall with the official installer.

### macOS

```bash
brew install mysql
brew services start mysql
```



### Linux (Debian/Ubuntu)

```bash
sudo apt update
sudo apt install mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
```



### Create the application database

With the server running, create an empty database named `opensurveys`.

**Option A — MySQL Workbench (optional GUI)**

1. Connect to `localhost` / `127.0.0.1`, port `3306`, user `root`, your password.
2. Right-click in **SCHEMAS** → **Create Schema...**
3. Name: `opensurveys` → **Apply**.

**Option B — command line**

```powershell
mysql -u root -p -e "CREATE DATABASE opensurveys;"
```

If `mysql` is not on your PATH (common on Windows), use the full path to `mysql.exe` from your MySQL install (for example under `C:\Program Files\MySQL\MySQL Server 8.0\bin\`).

---



## 5. MySQL Workbench (optional)

Workbench is only a client. You still need MySQL Server (step 4).

### Windows

```powershell
winget install --id Oracle.MySQLWorkbench -e
```

Or install it from the same MySQL Installer bundle / [https://dev.mysql.com/downloads/workbench/](https://dev.mysql.com/downloads/workbench/)

### Connect

1. Open Workbench.
2. Use an existing **Local instance** connection, or create one:
  - Hostname: `127.0.0.1`
  - Port: `3306`
  - Username: `root`
  - Password: your MySQL root password
3. **Test Connection**, then connect and create schema `opensurveys` if you have not already.

---



## 6. Project files that are not in Git



### Navbar logo

1. Name the file exactly: `logo.png`
2. Place it at: `opensurveys-frontend/public/logo.png`
3. Use a PNG that looks clear at about **36px** height (navbar size)



### Backend `.env`

```powershell
cd opensurveys-backend
Copy-Item .env.example .env
```

macOS / Linux: `cp .env.example .env`

Edit `opensurveys-backend/.env` and set at least:


| Variable                                    | What to put                                                                     |
| ------------------------------------------- | ------------------------------------------------------------------------------- |
| `DB_URL`                                    | `jdbc:mysql://localhost:3306/opensurveys?useSSL=false&allowPublicKeyRetrieval=true` |
| `DB_USERNAME`                               | MySQL user (often `root`)                                                       |
| `DB_PASSWORD`                               | That user’s MySQL password                                                      |
| `JWT_SECRET`                                | A long random secret string                                                     |
| `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD` | Local admin login (username defaults to `admin`)                                |
| `GOOGLE_CLIENT_ID`                          | Google Cloud **Web** OAuth client ID (needed for Google Sign-In)                |
| `MAIL_*` / `MAIL_FROM`                      | SMTP settings (needed for email verification flows)                             |


Comments for every variable are in `.env.example`.

**Important:** Do **not** set `DB_URL`, `DB_USERNAME`, or `DB_PASSWORD` as permanent Windows/macOS/Linux environment variables unless you intend them to override `.env`. The backend skips `.env` entries when the same name already exists in the process environment. If you previously pointed `DB_URL` at a remote host, remove those OS variables so local `.env` is used.

### Frontend `.env`

```powershell
cd opensurveys-frontend
Copy-Item .env.example .env
```

macOS / Linux: `cp .env.example .env`


| Variable                     | What to put                                          |
| ---------------------------- | ---------------------------------------------------- |
| `REACT_APP_GOOGLE_CLIENT_ID` | **Same** Web client ID as backend `GOOGLE_CLIENT_ID` |


Leave `REACT_APP_API_URL` unset for normal local development (the CRA proxy targets `http://localhost:8080`).

---



## 7. Google Sign-In (optional for basic local use)

Required only if you want Google login buttons to work.

1. Open [Google Cloud Console](https://console.cloud.google.com/) → APIs & Services → Credentials.
2. Create an **OAuth 2.0 Client ID** of type **Web application**.
3. Under **Authorized JavaScript origins**, add:
  - `http://localhost:3000` (dev frontend)
  - `http://localhost:8080` (if you use the production-style build served by Spring)
4. Copy the client ID into:
  - `opensurveys-backend/.env` → `GOOGLE_CLIENT_ID`
  - `opensurveys-frontend/.env` → `REACT_APP_GOOGLE_CLIENT_ID`

---



## 8. SMTP for verification emails (optional for basic local use)

Required only for account flows that email a verification code (change email / password).

Typical Gmail-style values (use an app password if 2FA is on):


| Variable        | Example                      |
| --------------- | ---------------------------- |
| `MAIL_HOST`     | `smtp.gmail.com`             |
| `MAIL_PORT`     | `587`                        |
| `MAIL_USERNAME` | your mailbox address         |
| `MAIL_PASSWORD` | app password / SMTP password |
| `MAIL_FROM`     | From address shown to users  |


Without SMTP, the rest of the app can still run; those email actions return an error when send fails.

---



## 9. Quick verification checklist

Before following `[runinstructions.md](runinstructions.md)`:

- [ ] `git --version` works
- [ ] `java -version` shows **25**
- [ ] `node -v` and `npm -v` work
- [ ] MySQL service is **Running**
- [ ] Database `opensurveys` exists
- [ ] `opensurveys-frontend/public/logo.png` exists
- [ ] `opensurveys-backend/.env` and `opensurveys-frontend/.env` exist and are filled in
- [ ] No leftover OS env vars (`DB_URL`, etc.) pointing at a remote database
- [ ] `npm install` completed in `opensurveys-frontend/`

Then start the backend and frontend using **runinstructions.md**.

When the backend starts successfully, the log line **Database JDBC URL** should show `localhost` (or your intended host), not an unexpected remote server.