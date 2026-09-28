# T00 — Workspace and tool access evidence

**Date:** 2026-09-25 11:25 UTC
**Executor:** Hermes Agent, running in a Docker Linux sandbox (aarch64, Debian-based, kernel 6.12.76-linuxkit)
**Task:** T00 — Verify workspace and tool access (requirements PLAT-01, SCOPE-02)

## 1. Working folder

- Working copy in use: `/workspace/app/calisthenics` (Docker container path, root-owned, device 42)
- Git: `main` branch, no commits yet; all 20 handoff files staged (`git status --short` shows 20 `A` entries); no remotes; `git diff --cached --check` = clean
- Package integrity: all 20 files in `PACKAGE-MANIFEST.json` re-hashed in place — **20/20 sha256 and byte-size matches**
- Baseline handoff checks (run in this folder, Python 3.11.15):
  - `python3 tools/check_handoff.py` → `Tasks: 27; approved requirements: 33` → `PASS: dependency graph, coverage, states and required handoff files` → **exit 0**
  - `python3 -m unittest discover -s tests -p 'test_*.py' -v` → `Ran 19 tests ... OK` → **exit 0**
  - `git diff --check` → **exit 0** (clean)

### Folder-mapping caveat (honest limitation)
The prompt and handoff name the real project folder as `/Users/nils/Documents/Apps/calisthenics` on the owner's Mac.
From inside this Docker sandbox the Mac filesystem is NOT mounted: `/Users/nils` in-container contains only `reuse-audit/` (empty), and `/root/Documents` does not exist.
The container's home/workspace overlays are the only persistent file access available to this agent.
**Therefore: the working copy here is the handoff package (verified 20/20 against PACKAGE-MANIFEST.json), but the owner must confirm that the Mac folder contains this same package before T01 reuse work or any commit is trusted.** This is T00's owner-visible check and remains open.

## 2. Tool inventory (actual command results)

| Tool | Command | Result |
|---|---|---|
| git | `git --version` | git version 2.47.3 — **present** |
| Python | `python3 --version` | Python 3.11.15 — **present** (handoff requires 3.10+; OK) |
| JDK | `java -version` | `command not found` — **MISSING** |
| Gradle | `gradle --version` | `command not found` — **MISSING** (no `gradlew` in repo yet; T03 creates wrapper) |
| Android SDK | `ANDROID_HOME` / `sdkmanager` | unset / not found — **MISSING** |
| adb | `adb version` | `command not found` — **MISSING** |
| Galaxy S21 | `adb devices` | device not reachable — **NOT CONNECTED / NOT TESTED** (Android/One UI versions unknown) |
| GitHub CLI | `command -v gh` | not found — **MISSING** (T00 step 3: gh auth check not run; no tokens touched) |
| git identity | `git config user.name/email` | both **UNSET** (no commit has been made; nothing to sign) |
| Node (incidental) | `node --version` | v20.20.2, npm 10.8.2 (not part of the Android contract) |
| Package manager | `apt-get update` | HTTP 403 via egress proxy; repo fetches blocked — **no package installation possible** |

## 3. Network access

- All outbound HTTPS is routed through the Hermes egress proxy (`host.docker.internal:9090/9091`); every probed host returned `CONNECT tunnel failed, response 403`:
  github.com, api.github.com, raw.githubusercontent.com, pypi.org, dl.google.com, services.gradle.org, repo.maven.apache.org, google.com
- Hermes web tools (search/extract) also fail (DNS resolution blocked upstream)
- **Consequence: no git clone, no Gradle/dependency download, no APK signing-material fetch, no repo audit from this sandbox. T01 (reuse audit) cannot fetch Ironvellum / Calisthenics Memory / Calistenia / Ballast checkouts until network or an alternative source path is provided.**

## 4. Local model inventory (T00 step 2)

- No local model runner exists in this sandbox: no `ollama`, no `llama.cpp`/`llama-cli`, nothing listening on a local inference port.
- The model currently serving this agent is a **remote custom provider** (`custom:qwen3.8-27b-8080`, model id `unsloth/Qwen3.8-27B-GGUF:UD-Q4_K_XL`) — it is not a locally installed runner and its availability was not verified from this sandbox. Per the handoff rule, this is recorded, not substituted for anything: no cloud model is silently used for the project, and no local Qwen claim is made.

## 5. Blockers recorded (per T00 step 3 / manual check)

| ID | Blocker | Unblocks | Resolution owner |
|---|---|---|---|
| B1 | Owner confirmation that the real Mac folder == this verified working copy | trusting the folder; commits | Owner (T00 manual check) |
| B2 | No JDK / Gradle / Android SDK / adb in sandbox; no package manager network access | T03 scaffold build, all Android test commands | Install toolchain in a network-enabled environment (Mac or fixed sandbox) |
| B3 | Egress proxy blocks all outbound traffic (403 on every host) | T01 repo inspection, all downloads | Restore egress or provide local checkout sources |
| B4 | Galaxy S21 not connected / not reachable from sandbox | T04 and all device scenarios A01–A13 | Connect device where adb exists |
| B5 | gh CLI absent; GitHub auth unverified | T26 (only, if authorized later) | `gh auth login` in owner's terminal; no token ever enters chat |
| B6 | No local model runner inventory to record an exact local model ID | none (informational) | N/A — recorded honestly |

## 6. T00 conclusion

T00's deliverable is this file. All T00 check commands were executed with real outputs above; every missing tool is recorded as a blocker rather than faked.
Status: **implemented** — pending owner confirmation of the target folder (B1) before it may be marked verified.
Next task in queue: **T01 (audit reuse candidates)** — currently **blocked** on B3 (network) unless the owner provides local checkout sources; B2/B4 block any build/device part of it.
