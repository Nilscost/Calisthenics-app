"""Check explicit R8 poses; optionally write only clean, unaccepted inputs.

Unlike poses.py's historical CLI, missing/unknown IDs and --help never build
anything. Checks are read-only by default. Identical JSON is never rewritten.
"""
import argparse
import json
from pathlib import Path
import poses

ROOT = Path(__file__).resolve().parents[2]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('ids', nargs='*')
    parser.add_argument('--group', help='Explicit group in the R8 contract')
    parser.add_argument('--describe', action='store_true')
    parser.add_argument('--write', action='store_true', help='Write clean, unaccepted inputs only')
    args = parser.parse_args()
    contract = json.loads((ROOT / 'docs/23-new-exercises.json').read_text())
    entries = {e['id']: e for e in contract['exercises']}
    ids = list(dict.fromkeys(args.ids))
    if args.group:
        if args.ids:
            parser.error('Choose explicit IDs or one group, not both')
        ids = [e['id'] for e in entries.values() if e['poseGroup'] == args.group]
        if not ids:
            parser.error('Unknown or empty group: ' + args.group)
    if not ids:
        parser.error('Provide explicit new exercise IDs or --group')
    poses.load_families()
    unknown = [i for i in ids if i not in entries or i not in poses.EXERCISES]
    if unknown:
        parser.error('Unknown/unimplemented R8 IDs: ' + ', '.join(unknown))
    if args.write:
        accepted = set()
        for n in (1, 2, 3):
            manifest = ROOT / f'work/clips-bakeoff/render-batch-{n}.txt'
            if manifest.exists():
                accepted.update(manifest.read_text().splitlines())
        frozen = sorted(accepted.intersection(ids))
        if frozen:
            parser.error('Accepted render inputs are frozen: ' + ', '.join(frozen))
    built = []
    failed = False
    for exercise_id in ids:
        data, report = poses.build(exercise_id)
        problems = report + poses.checks(data)
        print(f'{exercise_id}: {len(data["frames"])} frames ' + ('PROBLEM' if problems else 'OK'))
        for problem in problems:
            print('  ' + problem)
        if args.describe:
            print(poses.describe(data))
        failed |= bool(problems)
        built.append((exercise_id, data))
    if failed:
        return 1
    if args.write:
        for exercise_id, data in built:
            target = ROOT / 'work/clips-bakeoff' / exercise_id / 'joints.json'
            payload = json.dumps(data)
            if target.exists() and json.loads(target.read_text()) == data:
                continue
            target.parent.mkdir(parents=True, exist_ok=True)
            temporary = target.with_name('.joints.new.json')
            temporary.write_text(payload)
            temporary.replace(target)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
