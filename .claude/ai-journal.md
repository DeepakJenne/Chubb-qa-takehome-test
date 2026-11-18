# AI Journal

Running record of how Claude was used, what was accepted, and what had to be corrected. Facts only; add your own decisions in the "Your notes" lines before submitting.

Limits that applied throughout: Claude could read the code but could not run Maven, Docker or npm, so nothing below was validated by execution.

## 1. Prompt creation
- **AI proposed:** a master prompt from the assessment brief (`prompts/01`).
- **Problem found later:** the prompt and the brief both assumed Angular and no existing tests. The repository is Next.js/React with unit tests, Playwright/Pact scaffolding and a Debezium CDC path.
- **Outcome:** prompt rewritten against the real stack (`prompts/02`).
- Your notes:

## 2. Step 0: discovery
- **AI proposed:** architecture summary, domain rules, and eight suspected issues from static code review (unmapped exceptions, unprotected user endpoints, fire-and-forget Kafka publishing, mock CDC field mismatch, stale cache, backward transition, query-parameter token, minor items).
- **Verified by the AI:** no `@Version` on `ClaimEntity`; no controller-level test for invalid transitions; no tests for the status select or realtime handler; Playwright setup file missing.
- **Not verified:** that any suspected issue actually fails at runtime.
- **Decision:** proceed to risk assessment.
- Your notes:

## 3. Step 1: risk assessment
- **AI proposed:** High/Medium/Low priorities and a scope-in/out list.
- **Decision:** priorities approved as proposed.
- Your notes (what you would change):

## 4. Step 2: strategy and plan
- **AI proposed:** pyramid split (unit 25 / component 20 / integration 35 / E2E 15 / load 5), tooling, 3-hour time plan, entry/exit criteria.
- **Decision:** approved as proposed.
- Your notes:

## 5. Step 3: test cases
- **AI proposed:** 67 cases in 8 areas, 12 written as tests that may expose defects.
- **Correction made by the AI:** summary counts were wrong (said 61) and were fixed to 67.
- **Decision:** approved; move on to helpers and first tests.
- Your notes:

## 6. Fixtures and first tests
- **AI proposed:** shared JSON contracts, support classes (auth, Kafka event probe, seeding base class) and the first tests for transitions, validation and authorization, plus UI parity tests.
- **Problems the AI caught itself:** a malformed assertion line (removed); integration test class names changed to `*IntegrationTest` so Surefire picks them up; unmapped-exception behaviour in MockMvc handled explicitly.
- **Known risks:** Boot 4 package names, security-test setup and seeding via `Claim.reconstitute` unconfirmed (see `docs/QA_KIT_NOTES.md`).
- Your notes:

## 7. Packaging
- **AI proposed:** this repository layout, following the take-home structure video (README, tests, resources, compose, NOTES, prompts, journal).
- Your notes:

## What I would tell the panel about AI use
- AI accelerated discovery, test design and boilerplate.
- AI assumptions were wrong at least once (stack) and were caught by reading the repository.
- Everything marked "not yet executed" must be run and corrected by me before I rely on it.
