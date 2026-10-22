# Running LandLedger (Stage 1) on your own laptop

This is the front-end prototype of the Land Ledger app. It runs completely on
your own machine — no server, no database, no internet account needed. The
sample land records live inside your browser.

## 1. Install Node.js (one time)

1. Go to https://nodejs.org and download the **LTS** installer for Windows.
2. Run it and accept all the defaults (it adds `node` and `npm` for you).
3. Open **PowerShell** (press the Windows key, type `powershell`) and check it worked:

```powershell
node -v
npm -v
```

Both should print a version number. If it says "not recognized", close PowerShell
and open a new one, or restart the laptop.

## 2. Unzip and open the folder

1. Right-click `LandLedger-Stage1.zip` → **Extract All…** → extract somewhere easy,
   for example `C:\Users\YourName\landledger`.
2. In PowerShell, go into the folder (note the `\` and the folder you just made):

```powershell
cd C:\Users\YourName\landledger
```

You should be inside the folder that contains `package.json`. Check with:

```powershell
dir package.json
```

## 3. Install the libraries (one time, about 1–2 minutes)

```powershell
npm install
```

If that finishes with an error, run this instead and try again:

```powershell
npm install --legacy-peer-deps
```

You will see a new `node_modules` folder appear — that is normal, it is the
libraries the app needs.

## 4. Configure PostgreSQL

PostgreSQL 17 should be running locally on port `5432`. In pgAdmin, open the
Query Tool while connected as your PostgreSQL administrator. Execute each
statement separately, replacing the placeholder password with a strong
password of your own:

```sql
CREATE ROLE landledger LOGIN PASSWORD 'REPLACE_WITH_A_STRONG_PASSWORD';
CREATE DATABASE landledger OWNER landledger;
```

If the `landledger` role already exists, set its password with:

```sql
ALTER ROLE landledger WITH LOGIN PASSWORD 'REPLACE_WITH_A_STRONG_PASSWORD';
```

In `C:\Users\ADMIN\Downloads\LandLedger-Stage1\landledger-backend`, copy
`.env.example` to `.env` and fill in the database password and a random JWT
secret. The first Admin's name, email, and password must be set together; leave
all three blank if you do not want to bootstrap an Admin account yet. Make the
Admin password at least 12 characters. `.env` is excluded from source control.

Generate the JWT secret in PowerShell:

```powershell
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
$rng.Dispose()
```

Paste the generated Base64 value into `LANDLEDGER_JWT_SECRET` in `.env`. The
backend applies its versioned Flyway migration to the empty database at
startup; Hibernate validates the schema and does not recreate or erase data.

Existing browser demonstration accounts and records are not imported or used.
Public registration creates new Citizen accounts in PostgreSQL. Promote a
registered Citizen to `OFFICER` from the Admin screen when needed. Officers can
review or reject registration applications; only an Admin can create a parcel
directly or approve an application to register a parcel.

When directly registering a parcel, an Admin can choose **Draw parcel boundary**
and click at least three points on the map. The displayed area is estimated from
the outline and should be checked against official records. Citizens can select
registered parcel boundaries on the map to view their details and ownership
history. Existing parcels that have only a location coordinate remain displayed
as point markers.

## 5. Optional: enable local blockchain proofs

The app can anchor approved parcel registrations and ownership transfers to a
local Ethereum-compatible development chain. PostgreSQL remains the registry
database. The chain stores only a salted SHA-256 proof, not parcel documents or
personal details. This is for local development/demo use, not a legally
authoritative land registry.

No wallet login, external account, API key, or real cryptocurrency is needed.
The local chain provides a development account and test funds. Keep its RPC
server on this computer and do not expose port 8545 to the internet. Hardhat
prints public test-account keys in its startup log; never use or fund those
accounts on a live network.

In a separate PowerShell window, install and start the local chain:

```powershell
cd C:\Users\ADMIN\Downloads\LandLedger-Stage1\landledger-chain
npm install
npm run chain
```

Keep that window open. Open another PowerShell window and deploy the proof
contract:

```powershell
cd C:\Users\ADMIN\Downloads\LandLedger-Stage1\landledger-chain
npm run deploy
```

Copy the four `LANDLEDGER_BLOCKCHAIN_...` settings printed by deployment into
`landledger-backend\.env`, then restart the backend. Hardhat's local chain keeps
its state in memory, so keep its PowerShell window open. Stopping it resets the
chain; redeploy the contract after restarting. Transaction references from the
previous local run will no longer exist, so this chain is for temporary demos,
not long-term records.

New approved registrations (including parcels directly created by an Admin)
and approved ownership transfers will show `PENDING`, then `CONFIRMED` with a
transaction reference when processed. Existing history entries are not
backfilled. If the chain is stopped, the PostgreSQL approval still completes
and the proof is retried when the chain returns. To run without blockchain,
leave `LANDLEDGER_BLOCKCHAIN_ENABLED=false` (the default).

## 6. Install Java and Maven, then start the Spring Boot backend

The backend requires Java 21 or newer and Apache Maven. Install them if needed,
then check:

```powershell
java -version
mvn -version
```

Open a PowerShell window in `C:\Users\ADMIN\Downloads\LandLedger-Stage1\landledger-backend`
and run:

```powershell
mvn spring-boot:run
```

The health API listens on `http://localhost:8081/api/health` so it does not
conflict with the frontend's documented port. Check it in a browser or from
PowerShell:

```powershell
Invoke-RestMethod http://localhost:8081/api/health
```

Expected response:

```json
{"status":"UP","service":"LandLedger Backend"}
```

## 7. Start the app

```powershell
npm run dev
```

Wait until you see a line like `Local: http://localhost:8080/`, then open that
address in your browser. Keep the PowerShell window open — closing it stops the app.
To stop it on purpose, press `Ctrl` + `C` in that window.

If port 8080 is already used by something else, pick another one:

```powershell
npm run dev -- --port 3000
```

then open `http://localhost:3000/`.

If you use a different frontend port, set `LANDLEDGER_FRONTEND_ORIGIN` for
Spring Boot to that exact origin (for example `http://localhost:3000`) and
restart the backend. Set it in the backend PowerShell window before starting
Spring Boot:

```powershell
$env:LANDLEDGER_FRONTEND_ORIGIN = "http://localhost:3000"
```

To change the backend URL used by the frontend, copy
`.env.example` to `.env.local`, update `VITE_API_BASE_URL`, and restart Vite.

The sign-in page shows whether it can reach the backend. Sign-in and registry
actions use the backend; errors are shown rather than replaced with fake data.

## 8. Sign in

Register through the app to create a Citizen account. Use the Admin email and
password you configured in `.env` for the initial administrator. No demo
credentials are installed. The backend stores account password hashes and
issues signed session tokens.

## Other useful commands

| Command           | What it does                                                        |
| ----------------- | ------------------------------------------------------------------- |
| `npm run dev`     | Runs the app for you to use and edit (this is the one you want).    |
| `npm run build`   | Checks that the whole app compiles and puts the result in `dist/`.  |
| `npm test`        | Runs the small set of checks that come with the project.            |
| `npm run lint`    | Checks code style.                                                  |

## Notes

- Registration data and accounts are stored in PostgreSQL.
- Map markers use applicant-provided coordinates only; no parcel boundary or
  government verification is fabricated.
- The project is built with TanStack Start (React + TypeScript + Tailwind CSS).
- If `npm install` is very slow or fails, you may be behind a college proxy/network
  that blocks package downloads — try a phone hotspot.
