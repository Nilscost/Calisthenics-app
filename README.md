# Calisthenics — implementation handoff

This repository contains an approved product specification and a precise workplan for a local coding model. **It does not yet contain a working Android application.** No Android build, Galaxy S21 test or repository audit has been passed.

## Start here
1. Read `docs/01-product-specification.md` (approved behavior).
2. Read `docs/02-coding-specification.md` (architecture, algorithms and data contracts).
3. Execute tasks in `docs/03-workplan.md`, using `tasks.json` as the machine-readable queue.
4. Follow `docs/04-verification.md`; stop at human gates.
5. Keep `STATUS.md` honest; use `docs/05-local-model-prompt.md` to start a local model.

A protected `AGENTS.md` write was not authorized, so no such file was created. The handoff is usable by explicitly loading the documents above; do not assume an agent will auto-load them.

## Initial product
Personal-first Android app, English, Galaxy S21. Familiar timed circuits, default 45 minutes; repetition targets inside timed blocks; optional skill goals and per-variation five-star progression. Automatically increase difficulty across completed training even without feedback, marking those results as assumed. Stretching on means no scheduled passive-rest-only blocks. Local data, offline demonstrations/audio, locked-screen guidance, recovery and portable backups.

## Planning decisions versus validated training guidance
The coding specification supplies deterministic **engineering defaults**. Progression pacing, exercise tiers, prerequisite rules, recovery windows and stretch compatibility must pass task T02 / gate G0 before use in real workouts. These are not medical or scientifically validated rules merely because this repository describes them. Existing survey claims are unverified leads.

## Repository checks available now
From this folder, using Python 3.10 or newer:

```sh
python3 tools/check_handoff.py
python3 -m unittest discover -s tests -p 'test_*.py' -v
```

These check the handoff/task/requirement structure, not an Android app. Android test commands are specified as a future contract in `docs/04-verification.md` and must only be claimed to work once their modules/tasks actually exist.

## Files
- `docs/01-product-specification.md`: agreed requirements and identifiers.
- `docs/02-coding-specification.md`: precise proposed implementation contracts.
- `docs/03-workplan.md`: small ordered tasks and stop conditions.
- `docs/04-verification.md`: automated and device acceptance scenarios.
- `docs/05-local-model-prompt.md`: copy-paste delegation prompt.
- `docs/06-github-and-install.md`: Mac placement, repository hygiene and later upload.
- `docs/07-decisions-and-blockers.md`: what remains unverified and who resolves it.
- `docs/research/landscape-survey.md`: prior survey, not a new source audit.
- `tasks.json`: dependencies, requirements and task status.
- `STATUS.md`: actual progress, not aspirational completion.

## Publication
No remote repository has been created or pushed. Do not assume a GitHub account, repo name or visibility. Future upload should initially be private unless the owner explicitly requests public. No application license is selected until reuse obligations are known. See `LICENSE-NOT-SELECTED.md` and `THIRD_PARTY_NOTICES.md`.
