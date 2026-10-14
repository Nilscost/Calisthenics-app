# Mac folder placement and GitHub readiness

## Actual boundary
The active Apps project was found at `/Users/nils/Documents/Apps`. A Calisthenics desktop project was registered at `/Users/nils/Documents/Apps/calisthenics`; this proves project metadata only, not that handoff files have been installed there.

The live Docker mount table maps `/workspace` to host storage under `/Users/nils/.hermes/sandboxes/docker/default/workspace`. The handoff was created at container `/workspace/app/calisthenics`, so the corresponding host storage path is `/Users/nils/.hermes/sandboxes/docker/default/workspace/app/calisthenics`. This mapping is supported by mount metadata; direct Mac target-folder access remains unavailable in this session.

## Install into the requested Apps folder
Run this in your Mac terminal (not the Docker terminal):

```sh
python3 "$HOME/.hermes/sandboxes/docker/default/workspace/app/calisthenics/tools/install_local.py" --destination "$HOME/Documents/Apps/calisthenics" --check-github
```

Alternatively, unzip the handoff download and run `python3 tools/install_local.py --destination "$HOME/Documents/Apps/calisthenics" --check-github` from the extracted package.

The installer verifies SHA-256 checksums, refuses to overwrite modified files or follow target symlinks, copies the handoff, verifies destination hashes, initializes a local main-branch Git repository if needed, stages only packaged files, runs handoff checks/tests, and checks GitHub authentication if `gh` is available. It does **not** create a commit, remote repository, push, publish anything or change global configuration. It does not install Android tools or the app.

If the source path is not present on your host, use the downloadable archive; do not create an empty folder and assume the documents are there. If the destination has conflicting files, preserve them and resolve deliberately; the installer will refuse to overwrite them.

## Check upload readiness
After installation:

```sh
cd "$HOME/Documents/Apps/calisthenics"
python3 tools/check_handoff.py
python3 -m unittest discover -s tests -p 'test_*.py' -v
git status --short
git diff --cached --check
git config user.name
git config user.email
gh auth status
```

Missing Git identity is not a reason to invent an identity; configure your chosen identity locally before committing. Missing `gh` or authentication blocks account verification, not creation of these portable documents. Authenticate using the official CLI/browser flow on your machine; never paste a token into chat or commit credentials.

Checks in this package establish ordinary Git structure, coverage, safe paths and absence of obvious secret-bearing filenames/large binaries. They cannot prove rights to upload content acquired later, a future GitHub account's repository-creation permission or future source-code quality.

## Later upload — only after explicit approval
No GitHub repository has been created for this project. Confirm the account, repository name and private/public visibility before running any creation/push command. A private repository is the recommended first step while media/code licensing is unresolved.

Once the owner authorizes `calisthenics` under the authenticated account:

```sh
git commit -m "Add calisthenics implementation handoff"
gh repo create calisthenics --private --source . --remote origin --push
git rev-parse HEAD
git ls-remote origin refs/heads/main
gh repo view --json nameWithOwner,url,visibility
```

These are documented future commands, NOT commands executed by this handoff. If an origin or repository already exists, stop and inspect it instead of overwriting it or force-pushing. Verify the remote main SHA equals local HEAD and visibility is as authorized before claiming successful upload.

## Repo hygiene
- No secrets, signing keys, personal workout databases/backups, APKs or private execution logs in Git.
- Preserve source/data/media licenses independently. No license has been selected for the eventual app yet.
- `docs/research/landscape-survey.md` is a historical research input; its external source claims require fresh verification before reuse.
- Large demo assets need an explicit distribution strategy; do not dump unlicensed or oversized media into Git.
- `.gitignore` covers common Android artifacts, credentials, local databases and private evidence.
