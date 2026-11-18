# Test Strategy and Plan: Claims Management App

Status: Steps 0–2 drafted from code review only (not yet run). Items marked **(hypothesis)** must be proven by a test.

## 1. Objectives
- Give meaningful confidence in the highest-risk behaviour: status transitions, authorization, event delivery, validation consistency.
- Expose real defects through failing tests that carry evidence.
- Keep the suite fast, deterministic and runnable locally against the Docker Compose stack.

## 2. Scope
**In scope**
- Claims Service: status transition rules, error mapping (HTTP codes), authorization (users API, claim ownership, admin endpoints), claim validation boundaries.
- BFF to Claims Service contract (request/response and error codes).
- Kafka to BFF to WebSocket pipeline: routing, malformed input, both event topics.
- UI: claim wizard, admin status select, realtime handler (component level).
- 3 E2E journeys (Playwright).
- Optional: one K6 script on claims list and dashboard.

**Out of scope (deliberate)**
- Re-testing behaviour the existing ~125 unit tests already cover.
- Full Debezium end to end (CDC consumer is covered by injecting messages into Kafka).
- Visual regression, theming, performance tuning, Keycloak internals, a11y audit.

## 3. Risk priorities
| Priority | Area |
|---|---|
| High | Status transitions and error mapping; authorization/RBAC and WebSocket routing; Kafka to WebSocket to UI delivery; validation consistency across UI, BFF, domain |
| Medium | Dashboard cache staleness and Redis-down fallback; token expiry; concurrent status updates; UI realtime handler edge cases |
| Low | Theming, N+1 queries, metric tag cardinality |

## 4. Test pyramid (target split by effort)
| Layer | Share | Tooling | Focus |
|---|---|---|---|
| Unit | ~25% | JUnit 5, Mockito, AssertJ; Vitest | Only gaps: error mapping, authz rules, BFF status mapping, zod vs backend boundary parity |
| Component (UI) | ~20% | Vitest + React Testing Library | Wizard validation, status select options, realtime toasts and state updates, from the user's perspective |
| Integration | ~35% | Spring Boot Test + Testcontainers (Postgres, Kafka, Redis); Pact | API with real DB and security, Kafka to WebSocket pipeline, BFF to claims contract |
| E2E | ~15% | Playwright | 3 journeys (below) |
| Load (bonus) | ~5% | K6 | Claims list and dashboard under modest concurrency |

E2E journeys:
1. Claimant submits a claim, admin sees it live.
2. Admin changes status, claimant sees it live.
3. Invalid transition is rejected with a clear message.

## 5. Test data and environment
- **Environment:** `./start-infra.sh` then `./start-app.sh` (Docker Compose). Seed users: admin@demo.com, claimant@demo.com. Default event mode: direct Kafka (`APP_EVENTS_CDC_ENABLED=false`).
- **Integration tests:** Testcontainers for isolation; no dependence on the running stack.
- **Data:** builders and factories (reuse existing `ClaimTestBuilder`, `ClaimFactory`, `UserFactory`); each test creates its own claims; no shared mutable state; unique IDs per test.
- **Waiting:** poll/await with timeouts; no fixed sleeps.
- **Real defects:** keep the failing test, document it in the bug log, mark it with a clear convention (e.g. `@Disabled("BUG-n")`) so the suite is green but the evidence stays.

## 6. Execution order and time budget (about 3 hours)
| Time | Activity |
|---|---|
| 0:00–0:20 | Run existing suites; list failures; fix the planted broken test(s) in their own commit |
| 0:20–1:20 | High-risk integration and API tests (transitions, authz, error mapping); log bugs as found |
| 1:20–2:00 | Kafka to WebSocket pipeline integration tests |
| 2:00–2:30 | UI component tests (wizard, status select, realtime handler) |
| 2:30–3:00 | 2–3 Playwright journeys; strategy notes, AI journal, bug log, walkthrough notes |
| Only if time | K6 script; Pact contract |

## 7. Entry and exit criteria
**Entry:** stack starts cleanly; existing unit suites run (failures known).
**Exit:**
- All new tests pass or are explicitly marked as known-defect evidence.
- Every High-risk area has at least one meaningful test.
- Suite runs from a documented command in the README.
- Bug log, AI journal and strategy notes committed.

## 8. Risks and assumptions
- Testcontainers needs Docker available on the test machine.
- Keycloak tokens: E2E and API tests need a way to obtain admin and claimant tokens (realm import in repo; assume password grant is enabled **(verify)**).
- Suspected defects are hypotheses until reproduced; some may be intentional design.
- Time-boxed: depth over breadth, shortcuts will be listed in the walkthrough notes.

## 9. Suspected issues to prove (from code review)
1. Invalid status transition probably returns 500, not 400 as documented **(hypothesis)**.
2. `POST /api/users` lacks an authorization check and accepts `role` from the body **(hypothesis)**.
3. Events can be silently lost (fire-and-forget send, schema validation off) **(hypothesis)**.
4. Mock CDC payload field mismatch (`id` vs `claim_id`) **(hypothesis)**.
5. Dashboard stats are not invalidated on claim changes **(hypothesis)**.
6. UNDER_REVIEW to SUBMITTED allowed; intent unclear.
7. JWT accepted as WebSocket query parameter; consumers catch only JSON parse errors.

## 10. How to run (to be completed with the final test commands)
```bash
./start-infra.sh && ./start-app.sh
cd code/claims-service && mvn verify
cd code/bff-service && mvn verify
cd code/demo-app-ui && npm test
cd code/demo-app-ui && npx playwright test
```
