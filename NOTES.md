# Decisions and Assumptions

## Decisions
1. **Risk first.** Priorities (High: transitions and error mapping, authorization, event delivery, validation consistency) were fixed before any test code, then every test maps to a risk ID in `docs/TEST_CASES.md`.
2. **Shared contracts in `qa/shared`.** Status transitions and validation boundaries are data, read by Java and TypeScript tests. Chosen over copying tables into each layer to prevent drift.
3. **Reuse the repo's `TestcontainersConfiguration`.** The base class imports it instead of defining new containers, so tests follow the existing `integration` tag convention (`-Dgroups=integration -Dsurefire.excludedGroups=`).
4. **MockMvc, not a live HTTP server.** Faster and self-contained. Trade-off: unmapped exceptions surface as `ServletException`; the transition test reports that as HTTP 500. A check against the running stack would confirm it.
5. **Synthetic JWTs for API tests.** Role and subject match what the security config reads; signature validation is bypassed.
6. **Direct Kafka mode in tests** (`app.events.cdc-enabled=false`). Debezium end to end is out of scope; the CDC consumer is covered by message injection (planned, D-08).
7. **Strict hypothesis tests.** Tests for suspected defects assert the documented behaviour. A failure is kept and logged, not softened.
8. **Provided root README moved to `docs/APP_README.md`** so the root README describes this submission. No application code was changed.

## Assumptions
- The brief says Angular and "no tests"; the repository is Next.js/React and ships unit tests. The repository was treated as the source of truth.
- OpenAPI is the intended contract (400 for invalid transitions, 403 for forbidden).
- UNDER_REVIEW → SUBMITTED may be intentional; flagged as a question (A-10), not a defect.
- Demo credentials in `docker/compose/.env`, the Keycloak realm file and the README come with the provided app. None were added by this submission.

## Out of scope (deliberate)
Visual regression, performance tuning, Keycloak internals, full Debezium end to end, re-testing behaviour already covered by existing unit tests.
