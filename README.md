# Java Multithreaded Telemetry Dashboard

A system-design demo that demonstrates concurrency, rate limiting, and request queuing using `ThreadPoolExecutor` and a browser-based telemetry dashboard.

## Features
- Resource-constrained execution (3 concurrent worker slots)
- Real-time backend telemetry (snapshot-on-entry)
- Browser-side parallel testing with client-side queue coordination

## Quickstart (Docker)

1. Copy `.env.example` to `.env` and edit if necessary:

```sh
cp .env.example .env
# set PORT if needed
```

2. Build and run with Docker Compose:

```sh
docker-compose up --build
```

3. Open the URL printed by the container (default: http://localhost:8080). If the container falls back to another port it will print it in logs.

## Force-hit 4 tabs for testing

- PowerShell (Windows):

```powershell
# open four tabs/windows
Start-Process "http://localhost:8080"
Start-Process "http://localhost:8080"
Start-Process "http://localhost:8080"
Start-Process "http://localhost:8080"
```

- Bash (macOS / Linux):

```bash
for i in {1..4}; do open "http://localhost:8080"; done
```

## Local run (no Docker)

```powershell
# compile
javac -d out "src\MulthiThread\MServer.java"
# run
$env:PORT=8080; java -cp out multithread.MServer
```

## Files of interest
- `src/MulthiThread/MServer.java` — server with ThreadPoolExecutor and snapshot-on-entry telemetry
- `web/index.html` — client UI and cross-tab queue coordination