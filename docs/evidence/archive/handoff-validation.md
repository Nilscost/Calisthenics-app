# Handoff validation — executed results

This report concerns planning files and support scripts only. No Android app build or device acceptance has been executed.

## Executed successfully in the Docker workspace
- `python3 tools/check_handoff.py`: exit 0; task dependency graph, requirement coverage, statuses and required files passed. It found 27 implementation tasks and 33 approved requirement identifiers.
- `python3 -m unittest discover -s tests -p 'test_*.py' -v`: exit 0; 19 tests passed. Coverage includes invalid task graphs/statuses/coverage and installer copy/idempotence, edit preservation, corruption, traversal, duplicate entries and symlink refusal.
- Initial validator rejection tests were run against an empty implementation and failed as expected before implementation; this was a handoff-support test, not Android TDD evidence.
- `git init -b main`: local repository created in the sandbox.
- `git diff --cached --check`: initial trailing whitespace in the inherited survey was corrected, then check passed.
- Tracked-file scan: no credential/private-key/database/APK filenames and no matches for the checked GitHub-token/private-key patterns. This is a bounded scan, not a security guarantee.
- `git remote`: no remote configured. Nothing uploaded.
- Host-side preview accepted and read back the mapped README identity at `/Users/nils/.hermes/sandboxes/docker/default/workspace/app/calisthenics/README.md`; this does not establish that the requested Apps destination contains these files.

## Packaging checks
The complete package was installed into a fresh temporary directory using `tools/install_local.py --destination <temporary>/calisthenics --check-github`: exit 0. All 20 manifest-listed source/destination files matched their hashes, a new local Git main repository was initialized/staged, handoff validation passed and all 19 tests passed again. An initial failure exposed an overbroad `evidence/` ignore pattern; it was anchored to `/evidence/` so sanitized `docs/evidence/` remains trackable, then the full installation passed.

The auth check correctly reported `GITHUB NOT VERIFIED: gh CLI is not installed/on PATH` in the sandbox. This says nothing about whether gh is installed or authenticated on the Mac. These tests do not establish write access to the requested Mac destination. Final archive/hash verification is performed separately when the delivery archive is assembled.

## Not verified
- Installation into `/Users/nils/Documents/Apps/calisthenics`.
- Mac GitHub authentication or permission to create/push a repository.
- Installed local Qwen model identity or delegation.
- Upstream repository buildability, current licenses, exercise-policy correctness or demo reuse rights.
- Android toolchain, APK build, offline audio, Galaxy S21 behavior, database migrations or app backup functionality.

All app tasks in tasks.json remain pending; all human gates remain unaccepted. The package is ready to begin T00, not to claim the app is implemented.
