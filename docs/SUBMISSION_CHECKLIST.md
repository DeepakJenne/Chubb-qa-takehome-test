# Submission Checklist

## Before committing
- [ ] Docker running; `./start-infra.sh` and `./start-app.sh` healthy
- [ ] Run existing suites; record results and the fixed broken test in `docs/TEST_RESULTS.md`
- [ ] Run new integration and parity suites; fix test-side problems (compile errors, package names, seeding)
- [ ] Update `docs/BUG_LOG.md` statuses (Suspected → Confirmed / Not a bug)
- [ ] Mark confirmed-defect tests with `@Disabled("BUG-nnn: ...")` (UI: `it.skip`) so the build is green and evidence stays
- [ ] Fill every "Your notes" line in `.claude/ai-journal.md`
- [ ] Remove the draft banner from `README.md`
- [ ] No new secrets added (the provided demo credentials are unchanged)

## Git
```bash
git init -b main
git config user.name  "<your name>"
git config user.email "<your email>"

# 1. baseline: the provided app
git add -A -- . ':(exclude)README.md' ':(exclude)NOTES.md' ':(exclude)qa' ':(exclude)prompts' \
  ':(exclude).claude' ':(exclude)docs' \
  ':(exclude)code/claims-service/src/test/java/com/example/demo/support' \
  ':(exclude)code/claims-service/src/test/java/com/example/demo/api' \
  ':(exclude)code/demo-app-ui/tests/parity'
git commit -m "chore: import provided claims demo app"

# 2. strategy and cases
git add docs/APP_README.md docs/TEST_STRATEGY.md docs/TEST_CASES.md
git commit -m "docs: add risk-based test strategy and 67 designed test cases"

# 3. shared contracts and helpers
git add qa code/claims-service/src/test/java/com/example/demo/support
git commit -m "test: add shared status and validation contracts and API test helpers"

# 4. API integration tests
git add code/claims-service/src/test/java/com/example/demo/api
git commit -m "test: add transition, validation and authorization integration tests"

# 5. UI parity tests
git add code/demo-app-ui/tests/parity
git commit -m "test: add UI parity tests against shared contracts"

# 6. documentation and AI record
git add README.md NOTES.md docs prompts .claude
git commit -m "docs: add README, notes, bug log, walkthrough, prompts and AI journal"

git remote add origin <your-repo-url>
git push -u origin main
```
