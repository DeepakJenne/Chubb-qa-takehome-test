# Test Results

Fill in after running. Do not submit with this file empty.

| Suite | Command | Date | Passed | Failed | Notes |
|---|---|---|---|---|---|
| Existing backend unit tests (claims-service) | `mvn test` | 2026-11-18 | — | — | Not run: Java/Maven unavailable; `mvnw.cmd` also cannot start because `.mvn/wrapper/maven-wrapper.properties` is missing |
| Existing backend unit tests (bff-service) | `mvn test` | 2026-11-18 | — | — | Not run: Java/Maven unavailable; `mvnw.cmd` also cannot start because `.mvn/wrapper/maven-wrapper.properties` is missing |
| Existing UI unit tests | `npm test` | 2026-11-18 | 202 | 26 | 10 test files failed. Includes the 2 parity failures below; other failures include missing generated API imports, `localStorage` being undefined in store tests, and wizard-step expectations/timeouts |
| New API integration tests | `mvn test -Dgroups=integration -Dsurefire.excludedGroups=` | 2026-11-18 | — | — | Not run: Java/Maven unavailable, so suspected API defects are not confirmed |
| New UI parity tests | `npm test -- tests/parity` | 2026-11-18 | 25 | 2 | Padded description and incident-location inputs are accepted by the UI although the shared contract expects rejection; see BUG-008 |

## Defects confirmed by failing tests
- BUG-008 confirmed by `tests/parity/claim-validation.parity.test.ts`: padded description and incident-location values are accepted by UI validation despite the shared contract expecting rejection.
- The other UI-suite failures are recorded in the suite notes above; the backend suites could not run, so no backend hypothesis was confirmed.
