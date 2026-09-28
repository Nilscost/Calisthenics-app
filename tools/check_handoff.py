"""Validate handoff structure, not application or training correctness."""
import json
import re
import sys
from collections import Counter
from pathlib import Path, PurePosixPath

STATES = {'pending', 'in_progress', 'blocked', 'implemented', 'verified', 'accepted'}


def validate(requirement_ids, payload):
    errors = []
    if payload.get('schema_version') != 1:
        errors.append('unsupported schema version')
    tasks = payload.get('tasks', [])
    if not isinstance(tasks, list) or not tasks:
        return errors + ['tasks must be a nonempty list']
    ids = [t.get('id', '') for t in tasks]
    duplicates = [k for k, v in Counter(ids).items() if v > 1]
    if duplicates:
        errors.append('duplicate task IDs: ' + ', '.join(duplicates))
    by_id = {t.get('id'): t for t in tasks}
    covered = set()
    for t in tasks:
        tid = t.get('id', '')
        if not re.fullmatch(r'T\d{2}', tid):
            errors.append(f'{tid}: invalid task ID')
        for key in ('title', 'deliverables', 'steps', 'checks', 'manual_check'):
            if not t.get(key):
                errors.append(f'{tid}: empty {key}')
        state = t.get('status')
        if state not in STATES:
            errors.append(f'{tid}: invalid status {state}')
        if state in {'verified', 'accepted'} and not t.get('evidence'):
            errors.append(f'{tid}: verified status needs evidence')
        if state == 'accepted' and t.get('human_gate') and not t.get('owner_approval'):
            errors.append(f'{tid}: accepted gate needs owner approval')
        for f in t.get('deliverables', []):
            p = PurePosixPath(f)
            if p.is_absolute() or '..' in p.parts or '\\' in f:
                errors.append(f'{tid}: deliverable must be safe relative path: {f}')
        for req in t.get('requirements', []):
            covered.add(req)
            if req not in requirement_ids:
                errors.append(f'{tid}: unknown requirement {req}')
        for dep in t.get('depends_on', []):
            if dep not in by_id:
                errors.append(f'{tid}: unknown dependency {dep}')
            elif state in {'in_progress', 'implemented', 'verified', 'accepted'}:
                target = by_id[dep]
                allowed = {'accepted'} if target.get('human_gate') else {'verified', 'accepted'}
                if target.get('status') not in allowed:
                    errors.append(f'{tid}: dependency not complete: {dep}')
    for req in sorted(requirement_ids - covered):
        errors.append('uncovered requirement: ' + req)
    if sum(t.get('status') == 'in_progress' for t in tasks) > 1:
        errors.append('multiple tasks in_progress')
    visiting, visited = set(), set()
    def walk(tid):
        if tid in visiting:
            errors.append('dependency cycle at ' + tid)
            return
        if tid in visited or tid not in by_id:
            return
        visiting.add(tid)
        for dep in by_id[tid].get('depends_on', []):
            walk(dep)
        visiting.remove(tid)
        visited.add(tid)
    for tid in by_id:
        walk(tid)
    return errors


def main():
    root = Path(__file__).resolve().parents[1]
    try:
        spec = (root / 'docs/01-product-specification.md').read_text()
        reqs = set(re.findall(r'\*\*([A-Z]+-\d+)\*\*', spec))
        payload = json.loads((root / 'tasks.json').read_text())
        errors = validate(reqs, payload)
        required = ['README.md', 'STATUS.md', 'docs/01-product-specification.md',
                    'docs/02-coding-specification.md', 'docs/03-workplan.md',
                    'docs/04-verification.md', 'docs/05-local-model-prompt.md',
                    'docs/06-github-and-install.md', 'docs/07-decisions-and-blockers.md',
                    'docs/08-traceability.md', '.gitignore', 'LICENSE-NOT-SELECTED.md',
                    'THIRD_PARTY_NOTICES.md']
        errors += [f'missing handoff file: {p}' for p in required if not (root / p).is_file()]
        matrix = root / 'docs/08-traceability.md'
        if matrix.exists():
            text = matrix.read_text()
            for req in reqs:
                if f'| {req} |' not in text:
                    errors.append('missing traceability row: ' + req)
        if not reqs:
            errors.append('no approved requirements found')
        print(f'Tasks: {len(payload["tasks"])}; approved requirements: {len(reqs)}')
        for e in errors:
            print('FAIL:', e)
        if not errors:
            print('PASS: dependency graph, coverage, states and required handoff files')
        return 1 if errors else 0
    except (OSError, ValueError, TypeError, KeyError) as exc:
        print('FAIL:', exc)
        return 1


if __name__ == '__main__':
    sys.exit(main())
