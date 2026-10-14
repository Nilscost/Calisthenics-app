# Startup prompt to paste into a local model

This is a user-facing prompt, not an auto-loaded agent instruction file. No local model has been launched by this handoff. Use the installed local model's exact identifier; the earlier request named Qwen 3.8, but availability has not been verified. Do not silently substitute a cloud model.

```text
Work in /Users/nils/Documents/Apps/calisthenics.

Implement this project's approved Android calisthenics specification using the supplied workplan. Start by reading README.md and STATUS.md. Read docs/01-product-specification.md once, then select only the first available task from tasks.json. Load that task's relevant sections in docs/02-coding-specification.md and docs/04-verification.md rather than trying to implement the entire document in one response.

Begin with T00. Inspect the real folder and tools. Report missing access honestly. T01 is a required existing-codebase/license/build audit; T02 requires owner approval of proposed policies and the named content library. Do not skip these to build a generic fitness app. No production app implementation before G0, and no extensive feature work before G1 proves offline/locked-screen behavior on Galaxy S21.

For each behavior, write and run a failing test, implement it, rerun and record actual results. Work one task or named substep at a time. Keep tasks.json and STATUS.md current. Stop at human gates, failed prerequisites or licensing/content blockers. Never invent test logs, trained ability, media rights or user approval.

The product automatically progresses completed sessions even without feedback, labels those results assumed, and keeps a discomfort hold until explicit clearance. Stretch mode has no programmed passive-rest-only blocks. Keep these requirements even if a library's defaults differ.

Do not publish to GitHub or install onto my phone without my permission. Return changed files, test commands/exit codes, blockers and the next task. If context is running low, save a checkpoint in STATUS.md and stop rather than guessing.
```

## Resume prompt

```text
Resume this repository from STATUS.md and tasks.json. Reproduce the previous task's important checks, then execute the next unblocked task. Do not redo approved product discovery or silently revise policies. Read only the coding-spec sections needed for this task. Stop at its human gate.
```
