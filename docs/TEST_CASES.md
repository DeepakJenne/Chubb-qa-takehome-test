# Test Cases: Claims Management App

Draft for approval (Step 3). Based on code review; nothing run yet.
Columns: **P** = priority (H/M/L). **Auto** = automate (Y/N). **(hyp)** = expected result is what the spec says, but the code suggests it may fail. A failure there is a candidate bug.
Before writing each test, check it is not already covered by an existing unit test.

Layers: **API-IT** = Spring Boot integration test with real Postgres/Kafka/Redis (Testcontainers) and JWTs; **BFF-IT** = BFF with stubbed or real claims-service; **CT** = component test (React Testing Library); **UT** = unit; **E2E** = Playwright.

Verified from code: no `@Version` on `ClaimEntity` (no optimistic locking); no controller-level test for invalid transitions; `ClaimStatusSelect` and the realtime handler hook have no tests; Playwright `global-setup.ts` missing.

---
## A. Status transitions and error mapping (High)

| ID | Layer | Title | Preconditions / Steps / Data | Expected | P | Auto |
|---|---|---|---|---|---|---|
| A-01 | API-IT | Each valid transition succeeds | Claim in source status; admin PATCH `/api/admin/claims/{id}/status`. Parameterised: SUBMITTED→UNDER_REVIEW, SUBMITTED→REJECTED, UNDER_REVIEW→APPROVED, UNDER_REVIEW→REJECTED, UNDER_REVIEW→SUBMITTED, APPROVED→CLOSED, REJECTED→CLOSED | 200; body status = target; `updatedAt` advances | H | Y |
| A-02 | API-IT | Every invalid transition is rejected with 400 (hyp) | Parameterised over all 18 non-allowed pairs incl. self-transitions and anything out of CLOSED | 400 ProblemDetail `validation-error`, message names both statuses; DB status unchanged | H | Y |
| A-03 | API-IT | Rejected transition emits no event | Invalid PATCH, then read `claim-events` | No new message; claim row unchanged | H | Y |
| A-04 | API-IT | Valid transition emits exactly one status-changed event | PATCH SUBMITTED→UNDER_REVIEW (direct mode) | One message, key = claimId, `oldStatus`/`newStatus` correct | H | Y |
| A-05 | API-IT | Unknown claim returns 404 | PATCH random UUID | 404 ProblemDetail `not-found` | H | Y |
| A-06 | API-IT | Invalid status value returns 400 | Body `{"newStatus":"FOO"}`; empty body; missing field | 400, not 500 | M | Y |
| A-07 | BFF-IT | BFF passes claims-service 400/403/404 through | Stub claims-service responses | BFF 400/403/404 respectively | H | Y |
| A-08 | BFF-IT | BFF behaviour on claims-service 500 | Stub 500 | Defined, documented status (currently an unhandled exception); check body is a ProblemDetail, no stack trace | M | Y |
| A-09 | UT | Transition table parity UI vs backend | Compare `VALID_TRANSITIONS` (UI) with `ClaimStatus.canTransitionTo` via shared fixture | Identical sets | M | Y |
| A-10 | Manual | Is UNDER_REVIEW→SUBMITTED intended? | Raise with panel / product | Documented decision | L | N |

## B. Authorization and RBAC (High)

| ID | Layer | Title | Preconditions / Steps / Data | Expected | P | Auto |
|---|---|---|---|---|---|---|
| B-01 | API-IT | Claimant cannot read another user's claim | Claimant A GET claim owned by B | 403 | H | Y |
| B-02 | API-IT | Claimant list contains only own claims | Two users, 2 claims each | Each sees exactly own 2, newest first | H | Y |
| B-03 | API-IT | Claimant blocked from admin endpoints | Claimant calls `/api/admin/claims`, `/status`, dashboard stats, flush-cache | 403 for all | H | Y |
| B-04 | API-IT | No token / expired token / bad signature | Call `/api/claims` | 401 each | H | Y |
| B-05 | API-IT | Token without realm roles gets no admin access | JWT with no `realm_access` | 403 on admin endpoints | M | Y |
| B-06 | API-IT | Claimant cannot create a user with ADMIN role (hyp) | Claimant POST `/api/users` with `role: ADMIN` | 403 | H | Y |
| B-07 | API-IT | Existing user cannot be re-keyed by another caller (hyp) | POST `/api/users` with existing email and new userId | Rejected (403/409); original userId unchanged | H | Y |
| B-08 | API-IT | Claimant cannot read another user's profile (hyp) | GET `/api/users/{otherId}` | 403 | M | Y |
| B-09 | BFF-IT | Cookie auth: `access_token` cookie accepted; Authorization header wins when both present | Requests with cookie only, header only, both with different users | Identity matches header when both present | M | Y |
| B-10 | API-IT | Admin filters: by status, by userId, both | Seed mixed claims | Only matching claims returned | M | Y |
| B-11 | API-IT | DB role check in status use case | Valid admin JWT but user row has role CLAIMANT | Defined result (not 500) (hyp) | M | Y |

## C. Claim creation validation (High)

Shared boundary table (`validation-cases.json`) drives API, BFF and UI tests so they cannot drift.

| ID | Layer | Title | Preconditions / Steps / Data | Expected | P | Auto |
|---|---|---|---|---|---|---|
| C-01 | API-IT | Amount boundaries | 0.00, 0.01, 1,000,000, 1,000,000.01, negative | Reject, accept, accept, reject, reject | H | Y |
| C-02 | API-IT | Sub-cent amount (hyp) | 0.001 | Rejected, or stored amount equals what was accepted (DB is DECIMAL(12,2)) | M | Y |
| C-03 | API-IT | Description length | 9, 10, 1000, 1001 chars; 10 spaces + 5 chars | Reject, accept, accept, reject, reject (trimmed) | H | Y |
| C-04 | API-IT | Location length | 4, 5, 200, 201 chars | Reject, accept, accept, reject | H | Y |
| C-05 | API-IT | Incident date | today, yesterday, tomorrow | Accept, accept, reject | H | Y |
| C-06 | API-IT | Missing / wrong-type fields | Omit each field; amount as string | 400 with field detail | M | Y |
| C-07 | API-IT | New claim starts SUBMITTED, owned by JWT subject | Valid POST | 201, Location header, status SUBMITTED, userId = token subject (body userId ignored) | H | Y |
| C-08 | CT | Wizard step 1 boundaries match API | Same shared table via UI inputs | Same accept/reject outcome as C-01/C-04/C-05 | H | Y |
| C-09 | UT | "Today" across time zones (hyp) | Date = today in UTC+13 and UTC-11 | UI and API agree | L | Y |
| C-10 | CT | Wizard preserves data on submit failure | Step 3 submit, API returns 500 | Error shown, entered data retained, can retry | M | Y |

## D. Event pipeline: Kafka → BFF → WebSocket (High)

| ID | Layer | Title | Preconditions / Steps / Data | Expected | P | Auto |
|---|---|---|---|---|---|---|
| D-01 | API-IT | Claim creation publishes one submitted event | Direct mode; POST claim | One `claim-submitted` message, key = claimId, envelope matches AsyncAPI schema | H | Y |
| D-02 | API-IT | No event on rollback | Force save failure after domain event | No message published | M | Y |
| D-03 | API-IT | Kafka down during create (hyp) | Stop broker, create claim | Defined behaviour; loss is detectable (log/metric), claim not corrupted | H | Y |
| D-04 | BFF-IT | Submitted event reaches admin sockets only | Connect admin + claimant sockets; send `claim-submitted` | Admin gets `CLAIM_SUBMITTED`; claimant gets nothing | H | Y |
| D-05 | BFF-IT | Status event reaches owner + admins, not other claimants | Sockets: admin, owner, other claimant | Admin and owner receive; other claimant does not | H | Y |
| D-06 | BFF-IT | Malformed JSON does not stop the consumer | Send garbage, then valid message | Garbage logged; valid message still delivered | H | Y |
| D-07 | BFF-IT | Unknown eventType ignored; missing payload fields handled | Send each variant | No exception loop; no broadcast with empty claimId (hyp) | M | Y |
| D-08 | BFF-IT | CDC update/create mapped correctly | Debezium-shaped message with `claim_id`, `user_id`, before/after | Correct WS message; create → admins only | H | Y |
| D-09 | BFF-IT | `/api/test/trigger-event` payload consumable (hyp) | Trigger mock CDC event | WS message has the claimId sent (payload uses `id`, consumer reads `claim_id`) | M | Y |
| D-10 | BFF-IT | Duplicate message delivered twice | Same event twice | Documented behaviour; UI shows one toast (see F-06) | M | Y |
| D-11 | BFF-IT | WebSocket handshake auth | No token, invalid token, cookie token, `?token=` | Rejected, rejected, accepted, accepted (flag `?token=` as security concern) | H | Y |
| D-12 | BFF-IT | Dead session does not block others | One closed session among several | Others still receive; dead one removed | M | Y |
| D-13 | Manual | Both event modes enabled | `cdc-enabled=true` plus direct publish | Duplicate notifications as README warns; document | L | N |

## E. Dashboard and cache (Medium)

| ID | Layer | Title | Preconditions / Steps / Data | Expected | P | Auto |
|---|---|---|---|---|---|---|
| E-01 | API-IT | Cache hit flag | Stats twice | First `cacheHit=false`, second `true` | M | Y |
| E-02 | API-IT | `bypassCache=true` returns fresh data | Create claim between calls | Count increased | M | Y |
| E-03 | API-IT | Stats stale after create / status change (hyp) | Prime cache, create claim, read stats | Reflects new claim (currently stale up to 300s) | M | Y |
| E-04 | API-IT | Flush clears cache | Prime, flush, read | `cacheHit=false` | M | Y |
| E-05 | API-IT | Redis down falls back to DB | Stop Redis container | 200 with correct counts | M | Y |
| E-06 | API-IT | Status counts sum to total | Mixed claims | Sum(claimsByStatus) = totalClaims | M | Y |

## F. UI component tests (React Testing Library)

| ID | Layer | Title | Preconditions / Steps / Data | Expected | P | Auto |
|---|---|---|---|---|---|---|
| F-01 | CT | Status select offers only valid next statuses | Render for each of 5 statuses | Options match transition table; CLOSED shows "--" | H | Y |
| F-02 | CT | Changing status requires confirmation | Pick option | Dialog appears; Cancel makes no API call | H | Y |
| F-03 | CT | Confirm updates status and shows success | Confirm | Store called with claimId + status; success toast | H | Y |
| F-04 | CT | API failure shows error, UI stays consistent | Store rejects | Error toast; status unchanged; control re-enabled | H | Y |
| F-05 | CT | Realtime: invalid status ignored | Message `newStatus: "BOGUS"` | No store update, no toast | M | Y |
| F-06 | CT | Realtime: claimant sees update once | `CLAIM_STATUS_CHANGED` twice with same claimId | One toast (deduped by id), store updated | M | Y |
| F-07 | CT | Realtime: admin refetches claims; submitted also refetches stats | Admin user, each message type | `fetchClaims` (and `fetchStats` for submitted) called; fetch failure caught | M | Y |
| F-08 | CT | Realtime: claimant ignores `CLAIM_SUBMITTED` | Claimant user | No refetch, no toast | L | Y |
| F-09 | CT | Disconnect banner and recovery | Simulate socket close then reopen | Warning banner shown, then cleared | M | Y |

## G. End-to-end journeys (Playwright)

| ID | Layer | Title | Steps | Expected | P | Auto |
|---|---|---|---|---|---|---|
| G-01 | E2E | Claimant submits, admin sees it live | Admin logged in on page 1; claimant submits wizard on page 2 | Admin list updates without reload; toast shown | H | Y |
| G-02 | E2E | Admin changes status, claimant sees it live | Admin selects UNDER_REVIEW and confirms | Claimant sees new status badge and toast | H | Y |
| G-03 | E2E | Invalid transition not possible/handled | Admin attempts via API after UI shows stale state | Clear error message; no crash | H | Y |
| G-04 | E2E | Claimant cannot reach admin pages | Claimant visits `/admin/claims` | Redirected/denied | M | Y |

## H. Concurrency and load

| ID | Layer | Title | Preconditions / Steps / Data | Expected | P | Auto |
|---|---|---|---|---|---|---|
| H-01 | API-IT | Concurrent conflicting updates (hyp) | Two admins PATCH same UNDER_REVIEW claim (APPROVED vs REJECTED) in parallel | Exactly one wins; other gets 4xx; exactly one event (no `@Version` → both may succeed) | M | Y |
| H-02 | API-IT | Concurrent creates | 20 parallel POSTs | 20 distinct claims, 20 events | L | Y |
| H-03 | Load | Claims list under load (bonus) | K6: 20 VUs for 1 min on GET `/api/claims` | p95 under agreed threshold, error rate < 1% | L | Y (if time) |
| H-04 | Load | Dashboard cached vs bypass (bonus) | K6 comparing both | Cached noticeably faster | L | Y (if time) |

---
## Summary
- Total: 67 cases (32 High, rest Medium/Low); 65 are automatable, 2 are manual (A-10, D-13).
- Realistic 3-hour target: all 32 High cases first (many are parameterised, so far fewer test methods), then Medium as time allows.
- Hypothesis tests (potential bugs): A-02, B-06, B-07, B-08, B-11, C-02, C-09, D-03, D-07, D-09, E-03, H-01.
- Not automated: A-10, D-13 (product decisions / documentation only).
- If time is short, drop in this order: H-03/H-04, C-09, D-12, G-04, B-09.
