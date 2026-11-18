# Claims Management App: QA Take-Home Submission

QA engineering submission for the Chubb APAC take-home. The application under test is the provided three-tier claims app (Next.js UI, Spring Boot BFF, Spring Boot Claims Service, Kafka, Keycloak, Postgres, Redis). This repository adds a risk-based test strategy, shared test contracts, integration and UI parity tests, a bug log, and a record of how AI was used.

> **Status: draft, tests written but not yet executed.** Run the steps under [Testing](#testing) and fill in `docs/TEST_RESULTS.md` before submitting. Every defect in `docs/BUG_LOG.md` is a hypothesis until a test reproduces it.

## Overview
- Strategy first: risks ranked, then tests chosen to cover the High risks within the 3-hour budget ([docs/TEST_STRATEGY.md](docs/TEST_STRATEGY.md)).
- 67 designed test cases ([docs/TEST_CASES.md](docs/TEST_CASES.md)); the first High-priority set is implemented.
- One contract, many layers: status transitions and validation boundaries live in `qa/shared/*.json` and drive both the Java API tests and the UI tests, so layers cannot silently drift apart.
- Suspected defects are written as strict tests. A failing test is the evidence, so none were weakened.

## Architecture
Provided with the app: [ARCHITECTURE.md](ARCHITECTURE.md) and `ss-demo-app.png`. Summary: UI :3001 → BFF :8090 → Claims Service :8080 → Postgres/Redis; events go Claims Service → Kafka (`claim-events`) → BFF → WebSocket → UI, or via Debezium CDC (optional). The original app README is kept at [docs/APP_README.md](docs/APP_README.md).

## Setup
Prerequisites: Docker 24+, JDK 21, Maven 3.9+, Node 20+.
```bash
./start-infra.sh      # Postgres, Kafka, Redis, Keycloak
./start-app.sh        # builds and starts Claims Service, BFF, UI
```
Test users: `admin@demo.com` / `Admin123!`, `claimant@demo.com` / `Claimant123!` (demo credentials shipped with the provided app).

## Testing
| Layer | What | Where | Run |
|---|---|---|---|
| API integration (Testcontainers) | status transitions, validation boundaries, authorization, Kafka events | `code/claims-service/src/test/java/com/example/demo/api`, helpers in `.../support` | `cd code/claims-service && mvn test -Dgroups=integration -Dsurefire.excludedGroups=` |
| UI parity | UI rules vs the shared contracts | `code/demo-app-ui/tests/parity` | `cd code/demo-app-ui && npm test -- tests/parity` |
| Existing unit tests | provided with the app | `code/*` | `mvn test` in each service, `npm test` in the UI |

Integration tests start their own containers; the running stack is not needed for them. Docker must be running.

Not yet implemented (designed in `docs/TEST_CASES.md`): BFF and Kafka-to-WebSocket tests (D-xx), dashboard cache (E-xx), UI component tests for the status select and realtime handler (F-xx), Playwright journeys (G-xx), concurrency and load (H-xx).

## Findings
See [docs/BUG_LOG.md](docs/BUG_LOG.md). Headline hypotheses: invalid status transition likely returns 500 not 400; `POST /api/users` has no role check; events can be lost silently; dashboard stats go stale; UI accepts space-padded text the API rejects.

## How this maps to the expected repository shape
| Expectation | Where |
|---|---|
| README with overview, setup, testing | this file |
| Application source | `code/` (provided) |
| Real, passing tests | `code/**/src/test`, `code/demo-app-ui/tests` (run and record results first) |
| Resources and API contracts | `code/*/src/main/resources` (OpenAPI, AsyncAPI), `qa/shared` (test contracts) |
| One-command setup | `docker-compose.*.yml`, `start-*.sh` |
| Decisions and assumptions | [NOTES.md](NOTES.md) |
| Prompts used | [prompts/](prompts/) |
| AI journal | [.claude/ai-journal.md](.claude/ai-journal.md) |
| Walkthrough for the panel | [docs/WALKTHROUGH.md](docs/WALKTHROUGH.md) |

## Limitations
- Tests were authored with AI assistance in an environment without Maven, Docker or npm packages, so they were syntax-checked only. Expect small fixes on first run (see `docs/QA_KIT_NOTES.md`).
- Integration tests authenticate with synthetic JWTs (no signature check). Token expiry and tampering need real Keycloak tokens.
