# Bug Log

Status values: **Suspected** (from code review, not reproduced), **Confirmed** (a test fails for this reason), **Not a bug** (behaviour intended or test wrong). Update after running the suites. Severity is provisional until confirmed.

| ID | Title | Severity (provisional) | Status | Evidence / test | Expected | Suspected cause |
|---|---|---|---|---|---|---|
| BUG-001 | Invalid status transition returns 500, not 400 | High | Suspected | A-02 `ClaimStatusTransitionIntegrationTest` | 400 ProblemDetail (per OpenAPI) | No `@ExceptionHandler` for `InvalidStatusTransitionException` in claims-service; BFF maps only 400/403/404 |
| BUG-002 | Any authenticated user can create a user with role ADMIN | High | Suspected | B-06 `AuthorizationIntegrationTest` | 403 | `POST /api/users` has no `@PreAuthorize`; role taken from body |
| BUG-003 | Existing user can be re-keyed by email | High | Suspected | B-07 | Rejected; original id unchanged | Create-user flow updates the id of an existing email |
| BUG-004 | Any authenticated user can read another user's profile | Medium | Suspected | B-08 | 403 | No ownership check on `GET /api/users/{id}` |
| BUG-005 | Events can be lost silently | High | Suspected | D-03 (not yet implemented) | Failure detectable; no silent loss | After-commit listener with fire-and-forget `kafkaTemplate.send`, schema validation off |
| BUG-006 | Mock CDC event produces empty claim id | Low | Suspected | D-09 (not yet implemented) | claimId preserved | Mock payload uses `id`, consumer reads `claim_id` |
| BUG-007 | Dashboard stats stale after create/status change | Medium | Suspected | E-03 (not yet implemented) | Reflects new data | Cache TTL 300s, no eviction on writes |
| BUG-008 | UI accepts space-padded text rejected by the shared contract | Low | Confirmed | `tests/parity/claim-validation.parity.test.ts`: UI accepts padded description and incident-location values that the shared contract marks invalid; the API endpoint itself was not run | Same outcome in both layers | UI validation does not trim before length check |
| BUG-009 | Concurrent status updates both succeed | Medium | Suspected | H-01 (not yet implemented) | One wins, other 4xx | No `@Version` on `ClaimEntity` |
| Q-001 | UNDER_REVIEW → SUBMITTED allowed | n/a | Question | A-10 | Confirm intent with product | Domain and UI both allow it |

## Entry template
```
### BUG-nnn: <title>
Severity: | Status:
Steps to reproduce:
Expected:
Actual:
Evidence: <test id / log / response>
Suggested fix:
```
