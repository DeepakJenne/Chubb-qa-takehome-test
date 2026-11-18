# QA kit: shared fixtures and first High-priority tests

Mirrors the repo layout. Copy the folders into `demo-app-qa-main/` (merge, no overwrites needed).

| Path | Purpose | Test cases |
|---|---|---|
| `qa/shared/status-transitions.json` | One status contract for backend and UI | A-01, A-02, A-09 |
| `qa/shared/validation-cases.json` | One boundary table for backend and UI | C-01 to C-05, C-08 |
| `claims-service/.../support/AbstractApiIntegrationTest.java` | Base class: real app + Testcontainers + MockMvc; seeds users and claims | all API-IT |
| `claims-service/.../support/TestAuth.java` | Authenticates requests as admin / claimant / no roles | B-xx |
| `claims-service/.../support/ClaimEventsProbe.java` | Records `claim-events` from Kafka; "exactly N" or "none" | A-03, A-04 |
| `claims-service/.../support/SharedCases.java` | Loads the shared JSON for JUnit | |
| `claims-service/.../api/ClaimStatusTransitionIntegrationTest.java` | A-01 to A-06 | |
| `claims-service/.../api/ClaimCreationValidationIntegrationTest.java` | C-01 to C-07 | |
| `claims-service/.../api/AuthorizationIntegrationTest.java` | B-01 to B-08 | |
| `demo-app-ui/tests/parity/*` | UI vs shared contracts | A-09, C-08 |

## Run
```bash
# Backend (needs Docker). The existing ITs are tagged "integration" and excluded by default.
cd code/claims-service
mvn test -Dgroups=integration -Dsurefire.excludedGroups=

# UI
cd code/demo-app-ui
npm test -- tests/parity
```

## What to expect (hypotheses: a failure is evidence, do not weaken the test)
- `A-02` (18 disallowed transitions): likely HTTP 500 instead of 400.
- `B-06`, `B-07`, `B-08`: likely pass the request that should be forbidden.
- UI parity, "padded with leading spaces" (description and location): the UI probably accepts what the API rejects.
- Everything else should pass. If it does not, suspect the test first (see below).

When a failure is confirmed as a real defect, log it in the bug log and mark the test `@Disabled("BUG-n: ...")` (UI: `it.fails` / `it.skip` with the bug id) so the suite stays green and the evidence stays in the repo.

## Not compiled or run
Written against the real class and package names in the repo, but I had no Maven, Docker or npm packages available. Syntax-checked only. Expect small fixes. Things I could not confirm:
- Spring Boot 4 package names for the test annotations (copied from the existing `HealthCheckIntegrationTest`).
- `spring-security-test` is on the test classpath and its `jwt()` post-processor applies `.authorities(...)` as intended.
- `Claim.reconstitute(...)` followed by `claimRepository.save(...)` inserts cleanly (no extra DB constraints).
- Kafka `AdminClient`/`KafkaConsumer` classes are available on the test classpath (they come with spring-kafka).
- Unmapped domain exceptions surface in MockMvc as `ServletException` (handled in `patchAndGetStatus` by reporting 500). A real HTTP check against the running stack would be a stronger confirmation for A-02.
- `TestAuth` skips signature validation, so token-expiry and tampering tests (B-04 variants) still need real Keycloak tokens.
