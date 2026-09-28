"""Copy this verified handoff to the Mac Apps folder without overwriting edits.

No download, remote creation, commit, push or global configuration change.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import shutil
import subprocess
import sys
import tempfile


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def safe_path(root, rel):
    p = PurePosixPath(rel)
    if p.is_absolute() or '..' in p.parts or not p.parts or '\\' in rel:
        raise ValueError('Unsafe package path: ' + rel)
    out = root.joinpath(*p.parts)
    for candidate in [out, *out.parents]:
        if candidate == root:
            break
        if candidate.is_symlink():
            raise ValueError('Refusing symlink: ' + str(candidate))
    return out


def install(source, destination, initialize_git=True):
    source = Path(source).resolve()
    requested = Path(destination).expanduser().absolute()
    if any(p.is_symlink() for p in [requested, *requested.parents]):
        raise ValueError('Destination path contains a symlink')
    destination = requested.resolve()
    manifest_path = source / 'PACKAGE-MANIFEST.json'
    manifest = json.loads(manifest_path.read_text())
    if manifest.get('version') != 1 or not manifest.get('files'):
        raise ValueError('Missing/unsupported package manifest')
    entries = manifest['files']
    seen = set()
    # Validate every source and every possible destination conflict BEFORE writes.
    for entry in entries:
        rel = entry['path']
        if rel in seen:
            raise ValueError('Duplicate manifest path: ' + rel)
        seen.add(rel)
        src = safe_path(source, rel)
        dst = safe_path(destination, rel)
        if not src.is_file() or digest(src) != entry['sha256']:
            raise ValueError('Source hash mismatch: ' + rel)
        if dst.exists() and (not dst.is_file() or digest(dst) != entry['sha256']):
            raise ValueError('Existing edited file; refusing overwrite: ' + str(dst))
    manifest_dst = safe_path(destination, 'PACKAGE-MANIFEST.json')
    if manifest_dst.exists() and digest(manifest_dst) != digest(manifest_path):
        raise ValueError('Existing different package manifest; refusing overwrite')
    destination.mkdir(parents=True, exist_ok=True)
    if source != destination:
        for rel in [e['path'] for e in entries] + ['PACKAGE-MANIFEST.json']:
            src, dst = safe_path(source, rel), safe_path(destination, rel)
            if dst.exists():
                continue
            dst.parent.mkdir(parents=True, exist_ok=True)
            fd, tempname = tempfile.mkstemp(prefix='.handoff-', dir=dst.parent)
            try:
                with os.fdopen(fd, 'wb') as out:
                    out.write(src.read_bytes())
                os.replace(tempname, dst)
            finally:
                if os.path.exists(tempname):
                    os.unlink(tempname)
    for entry in entries:
        if digest(safe_path(destination, entry['path'])) != entry['sha256']:
            raise ValueError('Destination verification failed: ' + entry['path'])
    if initialize_git:
        git = shutil.which('git')
        if not git:
            raise RuntimeError('Files copied and verified, but Git is not installed')
        if not (destination / '.git').exists():
            subprocess.run([git, 'init', '-b', 'main'], cwd=destination, check=True)
        subprocess.run([git, 'add', '--', *[e['path'] for e in entries], 'PACKAGE-MANIFEST.json'],
                       cwd=destination, check=True)
        subprocess.run([git, 'diff', '--cached', '--check'], cwd=destination, check=True)
    return len(entries)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--destination', type=Path, default=Path.home() / 'Documents/Apps/calisthenics')
    parser.add_argument('--check-github', action='store_true')
    args = parser.parse_args()
    source = Path(__file__).resolve().parents[1]
    try:
        count = install(source, args.destination)
        print(f'Verified {count} packaged files in {args.destination}')
        for cmd in [[sys.executable, 'tools/check_handoff.py'],
                    [sys.executable, '-m', 'unittest', 'discover', '-s', 'tests', '-p', 'test_*.py', '-v']]:
            subprocess.run(cmd, cwd=args.destination, check=True)
        print('Git initialized/staged; no commit, remote creation or push performed.')
        if args.check_github:
            gh = shutil.which('gh')
            if not gh:
                print('GITHUB NOT VERIFIED: gh CLI is not installed/on PATH.')
            else:
                result = subprocess.run([gh, 'auth', 'status'], capture_output=True, text=True, timeout=30)
                for line in (result.stdout + result.stderr).splitlines():
                    if 'token:' not in line.lower():
                        print(line)
                if result.returncode == 0:
                    who = subprocess.run([gh, 'api', 'user', '--jq', '.login'], capture_output=True, text=True, timeout=30)
                    if who.returncode == 0:
                        print('Authenticated GitHub account:', who.stdout.strip())
                        print('Authentication verified; repository creation/push permissions still require a real authorized operation.')
                    else:
                        print('GITHUB NOT VERIFIED: authenticated account lookup failed.')
                else:
                    print('GITHUB NOT VERIFIED: authentication check failed. Use gh auth login in your own terminal; never paste a token into chat.')
        return 0
    except (OSError, ValueError, RuntimeError, subprocess.SubprocessError) as exc:
        print('INSTALL BLOCKED:', exc, file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
