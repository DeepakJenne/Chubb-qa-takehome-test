# Revised prompt: Chubb QA take-home (matches the actual repo)

Paste into Claude Code / Cursor / Copilot, opened at the repo root (`demo-app-qa-main`).

```
ROLE
You are a senior QA/SDET engineer. I'm doing a QA Engineer take-home (2–3 hours target, 5 hard cap) on an insurance Claims Management app. Prioritise ruthlessly; I must be able to defend every test and every omission in a walkthrough.

ACTUAL STACK (differs from the brief, which says Angular / no tests)
- UI: Next.js 16 + React 19, Zustand, zod, Vitest + Testing Library (port 3001)
- BFF: Spring Boot 4 / Java 21 (8090): cookie access_token -> Bearer, REST proxy to claims-service, WebSocket /api/ws/claims, Kafka consumer
- Claims Service: Spring Boot 4 (8080), hexagonal, Postgres + Liquibase, Redis dashboard cache, Kafka producer (topic claim-events)
- Keycloak (8180), Kafka, optional Debezium CDC (topic cdc.demo-app.public.claims)
- Existing: ~100 backend unit tests, ~25 UI unit tests, ArchUnit. Scaffolded but empty: Playwright (config points to missing tests/e2e/global-setup.ts), Pact, Testcontainers.
- Run: ./start-infra.sh, ./start-app.sh; mvn test in each service; npm test in code/demo-app-ui. Users: admin@demo.com / Admin123!, claimant@demo.com / Claimant123!

DONE SO FAR (Step 0). Verify, don't trust:
Domain: SUBMITTED->UNDER_REVIEW|REJECTED; UNDER_REVIEW->APPROVED|REJECTED|SUBMITTED; APPROVED|REJECTED->CLOSED; CLOSED terminal. Validation: date not future, description 10–1000, location 5–200, amount 0.01–1,000,000.
Suspected issues to PROVE with failing tests (each is a hypothesis):
1. InvalidStatusTransitionException and user-domain UnauthorizedException have no @ExceptionHandler -> likely HTTP 500, OpenAPI promises 400; BFF only maps 400/403/404 and throws otherwise.
2. POST /api/users has no @PreAuthorize, takes role from body, re-keys existing users by email; GET /api/users/{id} has no ownership check.
3. Events published after commit via fire-and-forget kafkaTemplate.send (no callback), schema validation disabled -> silent loss; cdc-enabled defaults true in application.yml but false in compose.
4. TestEventController mock CDC payload uses "id" but consumer reads "claim_id".
5. Dashboard cache (300s TTL) never evicted on create/status change.
6. UNDER_REVIEW->SUBMITTED allowed (confirm whether intended).
7. WebSocket accepts JWT via ?token= query param; Kafka consumers catch only JsonProcessingException.
8. Admin claim list N+1 user lookups; "unknown@example.com" fallback; userId used as metric tag.

RISK PRIORITIES (agreed)
HIGH: status-transition flow and error mapping; authorization/RBAC (users API, claim ownership, admin endpoints, WS routing so claimants never see others' events); Kafka -> BFF -> WebSocket -> UI delivery (lost, duplicate, malformed, wrong recipient); claim creation validation consistency across UI / BFF / claims-service.
MEDIUM: dashboard cache staleness and Redis-down fallback; auth cookie/token expiry; concurrent status updates (check for optimistic locking); UI realtime handler (bad status, reconnect, state sync).
LOW / OUT: theming, Storybook, perf tuning, Keycloak internals, re-testing what existing unit tests already cover, full Debezium E2E (cover CDC consumer by injecting messages into Kafka instead).

STEPS (stop for my approval after Steps 2 and 3)
STEP 1 (risk matrix): written up in TEST_STRATEGY.md from the priorities above.
STEP 2 – STRATEGY AND PLAN: concise. Objectives, scope in/out, pyramid split, tools (Vitest + React Testing Library; JUnit 5 + Spring Boot Test + Testcontainers incl. Kafka; Pact for BFF<->claims contract; Playwright for 2–3 E2E journeys; optional K6), test data/environment, entry/exit criteria, how to run locally. First action: run all existing suites, list failures, fix the planted broken test(s) separately.
STEP 3 – TEST CASES: table per area: ID | Layer | Title (reads as a spec) | Preconditions | Steps | Data | Expected | Priority | Automated?. Cover positive, negative, boundary, state transitions, error paths, messaging failures, real-time updates. Layers: component (React Testing Library, user perspective), unit, integration (API+Kafka+WebSocket, UI->backend). Must-have E2E: claimant submits claim -> admin sees it live; admin changes status -> claimant sees it live; invalid transition rejected.
STEP 4 – IMPLEMENT in priority order. DRY (builders, fixtures, page objects), SOLID, no magic values, no sleeps (await/poll), no tests that pass regardless of behaviour. Small meaningful commits. A failing test that proves a real defect is kept, tagged/documented (e.g. @Disabled with bug ID or an xfail convention), not "fixed" by weakening it.
STEP 5 – BUG LOG: for each defect: title, severity, repro steps, expected vs actual, evidence (test/code ref), suggested fix.
STEP 6 – DELIVERABLES: TEST_STRATEGY.md; test code + README run instructions; AI_JOURNAL.md (what AI proposed / I accepted / challenged / overrode, and why); walkthrough notes (15–20 min outline, likely "why not X?" answers, "with more time" list in priority order).

RULES
- Ask before assuming anything ambiguous. Keep outputs concise.
- Flag every shortcut so I can defend it.
- Mark anything you couldn't verify by running it.
```
