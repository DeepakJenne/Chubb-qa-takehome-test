# Master prompt v1 (initial; assumed Angular and no existing tests)

Superseded by `02_master_prompt_v2_actual_stack.md` after the repository was read and the stack turned out to be Next.js/React with existing unit tests.

```
ROLE
You are a senior QA/SDET engineer. I'm doing a take-home assessment for a QA Engineer role on a Claims Management Application: Angular frontend, Spring Boot backend, Kafka messaging, WebSocket real-time updates, run via Docker Compose. The app has NO existing tests. Time budget: 2–3 hours (hard cap 5), so prioritise ruthlessly.

STEP 0 – DISCOVER (no test code yet)
Read the full source. Summarise: architecture, modules, REST endpoints, Kafka topics/producers/consumers, WebSocket flows, data model, claim lifecycle/state transitions, validation rules, error handling, auth (if any), config. List anything unclear as questions.

STEP 1 – RISK ASSESSMENT
Produce a risk matrix (area | what could fail | likelihood | impact | priority H/M/L) covering: business logic; integration points; messaging risks (duplicates, ordering, lost messages, poison messages, retries); data and concurrency; security basics; error handling and resilience. Then state what we will test, what we deliberately won't, and why.

STEP 2 – TEST STRATEGY AND PLAN
Objectives, scope in/out, test pyramid split by layer, tools, test data and environment strategy, entry/exit criteria, risks and assumptions, how to run locally against Docker Compose.

STEP 3 – TEST CASES
Table: ID | Layer | Title | Preconditions | Steps | Test data | Expected result | Priority | Automated? Cover positive, negative, boundary, edge, state transitions, error paths, messaging failures, real-time updates. Layers: component, unit, integration. Bonus: visual regression, K6.

STEP 4 – IMPLEMENT in priority order. DRY, SOLID, clean code. No tests that pass regardless of app behaviour. No flaky sleeps. Small commits.

STEP 5 – BUG HUNT: title, severity, repro steps, expected vs actual, evidence, suggested fix.

STEP 6 – DELIVERABLES: TEST_STRATEGY.md; test code + README; AI_JOURNAL.md; walkthrough notes.

RULES: work phase by phase and pause for approval after Steps 0, 1 and 3; ask before assuming; keep outputs concise; flag any shortcut.
```
