# Load Test Infrastructure

Production-level load test environment for ttokttok using Docker Compose and k6.

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌──────────┐
│   k6 CLI    │────▶│   nginx:80   │────▶│  app:8080│
│  (orchestrator)│    │  (reverse    │     │ (Spring  │
└─────────────┘     │   proxy)     │     │  Boot)   │
                    └──────────────┘     └────┬─────┘
                                              │
                                      ┌───────▼───────┐
                                      │  db:5432       │
                                      │  postgres:16   │
                                      └───────────────┘
```

**Monitoring stack** (separate compose file):
- Prometheus scrapes `/actuator/prometheus` from app and `/nginx_status` from nginx
- Grafana dashboard `loadtest-overview` visualizes response times, error rates, throughput

## Quick Start

### Run a Scenario

```powershell
# Option A: Full script (start infra + run + cleanup)
.\scripts\run-loadtest.ps1 -Scenario issue-a -VUs 50 -Duration 30s

# Option B: Manual step-by-step
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d --build
# Wait for services to be healthy (check http://localhost:8080/actuator/health)
k6 run k6/scenarios/issue-a-view-count.js -e VUS=50 -e DURATION=30s
```

### Supported Scenarios

| Scenario | File | Purpose |
|----------|------|---------|
| **Issue A** | `k6/scenarios/issue-a-view-count.js` | View count concurrency — GET `/api/clubs/{id}/introduction` |
| **Issue B** | `k6/scenarios/issue-b-duplicate-apply.js` | Duplicate apply race — POST `/api/applicants` |
| **Issue C** | `k6/scenarios/issue-c-max-apply-count.js` | Max apply count constraint — POST `/api/applicants` |

### Configuration

All k6 scenarios accept environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `VUS` | `100` / `50` | Virtual users (issue-a: 100, b/c: 50) |
| `DURATION` | `30s` | Test duration |
| `BASE_URL` | `http://nginx:80` | Target base URL |
| `CLUB_ID` | `1` | Club ID for issue-a |
| `APPLYFORM_ID` | `1` | Apply form ID for issue-b/c |

### Cleanup

```powershell
.\scripts\cleanup.ps1              # Stop containers (preserve volumes)
.\scripts\cleanup.ps1 -RemoveVolumes  # Stop + delete all volumes
```

## File Structure

```
load-test/
├── docker-compose.yml              # Core services (nginx, app, db, redis)
├── docker-compose.monitoring.yml   # Prometheus, Grafana, Pushgateway
├── docker-compose.k6.yml           # k6 service (optional—run k6 locally instead)
├── Dockerfile                       # Spring Boot app image
├── k6.Dockerfile                    # k6 image with scenarios mounted
├── nginx.conf                       # Reverse proxy config
├── prometheus.yml                   # Prometheus scrape configuration
├── grafana/
│   ├── provisioning/
│   │   ├── datasources/prometheus.yml
│   │   └── dashboards/dashboards.yml
│   └── dashboards/
│       └── loadtest-overview.json   # 8-panel observability dashboard
├── k6/
│   ├── scenarios/
│   │   ├── issue-a-view-count.js
│   │   ├── issue-b-duplicate-apply.js
│   │   └── issue-c-max-apply-count.js
│   └── lib/
│       └── http-utils.js            # Minimal utilities (generateId, getName)
├── scripts/
│   ├── run-loadtest.ps1            # Full run script (start → wait → k6 → results)
│   └── cleanup.ps1                 # Stop containers, optional volume removal
└── README.md                        # This file
```

## Design Decisions

- **Docker Compose over Skaffold**: Single-command infrastructure management, deterministic networking
- **k6 run locally** (not in Docker): Avoids container network complexity; `run-loadtest.ps1` handles cross-container targeting
- **Environment variable configuration**: All scenario parameters are overridable via `-e` flag or PowerShell script params
- **`http-utils.js` minimal**: Only `generateId()` and `getName()` — all HTTP logic lives in scenario files
- **No `application-loadtest.yml`**: Configuration moved to Docker Compose environment variables
- **No V99 seed migration**: Seed data managed via separate scripts, not Flyway migrations

## Grafana Dashboard

Access at `http://localhost:3000` (admin/admin). Dashboard: **Load Test Overview** with 8 panels covering:
- RPS (requests per second)
- Error rate
- p50/p95/p99 latency
- Active connections
- Memory/CPU usage

## Tips

- Start with low VUs (10-20) to verify the environment works before scaling up
- Use `-RemoveVolumes` on cleanup to reset the database between test runs
- Check `k6/results/` for JSON output from each run
- Monitor Grafana during execution for real-time visibility
