# Prompts

| File | Purpose | Outcome |
|---|---|---|
| `01_master_prompt_v1_initial.md` | First master prompt, built from the assessment brief | Replaced: it assumed Angular and no tests |
| `02_master_prompt_v2_actual_stack.md` | Corrected prompt after reading the repository (stack, existing tests, suspected issues, agreed priorities) | Drove Steps 1–3 and the first tests |

Conversation-level instructions that shaped the work (not separate files):
- "Analyze the attached document and prepare the prompt to prepare a detailed plan, test and writing cases" (produced v1)
- Repository supplied as a zip; "continue" and "yes" approvals after Step 0 (discovery), Step 1 (risk priorities), Step 2 (strategy), Step 3 (test cases)
- "Create shared helpers and fixtures" (produced the `qa/shared` contracts, support classes and first tests)
- Request to package everything as a git-ready QA take-home repository following the repository-structure video
