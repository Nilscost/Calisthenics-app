"""Fetch only the reviewed repetition of a coaching video, at low resolution, for local analysis.

    python tools/blender/reference_fetch.py --exercise plank-long-lever [--dry-run]
    python tools/blender/reference_fetch.py --all

`motion_references.json` key `batch_references` maps an exercise id to two or three
demonstrations: {video_id, url, creator, title, interval: [start_s, end_s], variant, view,
assistance, notes, sha256?}. The interval is downloaded with yt-dlp `--download-sections`
(at most 480p, no audio), cached in work/references/ under a name derived from URL+interval,
and the sha256 of the cached file is written back into the JSON. Videos are never committed,
shipped or re-encoded into the app; the repository keeps only URL, creator, interval and hash.
"""
import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
REFS = Path(__file__).with_name('motion_references.json')
DEFAULT_CACHE = ROOT / 'work' / 'references'
MAX_HEIGHT = 480
URL_RE = re.compile(r'^https://(www\.)?youtube\.com/watch\?v=[A-Za-z0-9_-]{11}$')


def validate_entry(entry):
    if not URL_RE.match(entry.get('url', '')):
        raise ValueError('url must be a plain https://www.youtube.com/watch?v=ID link: %r' % entry.get('url'))
    start, end = entry['interval']
    if not (0 <= start < end) or end - start > 20:
        raise ValueError('interval must be [start, end] seconds, 0 <= start < end, at most 20 s (one repetition): %r' % (entry['interval'],))
    if not entry.get('creator'):
        raise ValueError('creator is required')


def cache_key(url, interval):
    """Stable file name from URL + interval: same inputs, same cache file."""
    digest = hashlib.sha1(('%s|%.2f|%.2f' % (url, interval[0], interval[1])).encode()).hexdigest()[:10]
    vid = url.rsplit('=', 1)[-1]
    return '%s_%s-%s_%s.mp4' % (vid, ('%g' % interval[0]).replace('.', 'p'), ('%g' % interval[1]).replace('.', 'p'), digest)


def build_command(entry, out, ytdlp='yt-dlp', ffmpeg=None):
    validate_entry(entry)
    start, end = entry['interval']
    cmd = [ytdlp, '--no-playlist', '--quiet', '--no-warnings',
           '-f', 'bv*[height<=%d][ext=mp4]/bv*[height<=%d]/b[height<=%d]' % (MAX_HEIGHT, MAX_HEIGHT, MAX_HEIGHT),
           '--download-sections', '*%g-%g' % (start, end), '--force-keyframes-at-cuts',
           '--remux-video', 'mp4', '-o', str(out)]
    if ffmpeg:
        cmd += ['--ffmpeg-location', str(ffmpeg)]
    return cmd + [entry['url']]


def sha256(path):
    h = hashlib.sha256()
    with open(path, 'rb') as fh:
        for block in iter(lambda: fh.read(1 << 20), b''):
            h.update(block)
    return h.hexdigest()


def fetch(entry, cache_dir, dry_run=False, run=subprocess.run, ytdlp='yt-dlp', ffmpeg=None):
    """Return {'path', 'sha256', 'cached', 'command'} for one entry; never downloads twice."""
    cache_dir = Path(cache_dir)
    out = cache_dir / cache_key(entry['url'], entry['interval'])
    cmd = build_command(entry, out, ytdlp, ffmpeg)
    if out.exists() and out.stat().st_size > 0:
        return dict(path=str(out), sha256=sha256(out), cached=True, command=cmd)
    if dry_run:
        return dict(path=str(out), sha256=None, cached=False, command=cmd)
    cache_dir.mkdir(parents=True, exist_ok=True)
    result = run(cmd, capture_output=True, text=True)
    if result.returncode != 0 or not out.exists():
        # yt-dlp may add an extension when remuxing; accept a single sibling with our stem.
        siblings = list(cache_dir.glob(out.stem + '.*'))
        if result.returncode == 0 and len(siblings) == 1:
            siblings[0].rename(out)
        else:
            raise RuntimeError('yt-dlp failed for %s: %s' % (entry['url'], (getattr(result, 'stderr', '') or '')[-400:]))
    return dict(path=str(out), sha256=sha256(out), cached=False, command=cmd)


def load(path=REFS):
    return json.loads(Path(path).read_text())


def entries_for(data, exercise):
    return data.get('batch_references', {}).get(exercise, [])


def record_hash(data, exercise, index, digest):
    data['batch_references'][exercise][index]['sha256'] = digest


def save(data, path=REFS):
    Path(path).write_text(json.dumps(data, indent=1, ensure_ascii=False) + '\n')


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--exercise', action='append', default=[])
    p.add_argument('--all', action='store_true')
    p.add_argument('--dry-run', action='store_true', help='print commands and cache paths, download nothing')
    p.add_argument('--cache-dir', type=Path, default=Path(os.environ.get('CALISTHENICS_REFERENCES', DEFAULT_CACHE)))
    p.add_argument('--refs', type=Path, default=REFS)
    args = p.parse_args(argv)
    data = load(args.refs)
    ids = sorted(data.get('batch_references', {})) if args.all else args.exercise
    if not ids:
        p.error('give --exercise ID or --all')
    ytdlp = shutil.which('yt-dlp') or 'yt-dlp'
    ffmpeg = shutil.which('ffmpeg')
    changed = False
    for ex in ids:
        entries = entries_for(data, ex)
        if not entries:
            print('%s: no batch_references' % ex)
            continue
        for i, entry in enumerate(entries):
            r = fetch(entry, args.cache_dir, dry_run=args.dry_run, ytdlp=ytdlp, ffmpeg=ffmpeg)
            state = 'cached' if r['cached'] else ('would fetch' if args.dry_run else 'fetched')
            print('%s[%d] %s %s %s' % (ex, i, state, r['path'], r['sha256'] or ''))
            if args.dry_run:
                print('   ' + ' '.join(r['command']))
            elif r['sha256'] and entry.get('sha256') != r['sha256']:
                record_hash(data, ex, i, r['sha256'])
                changed = True
    if changed:
        save(data, args.refs)
    return 0


if __name__ == '__main__':
    sys.exit(main())
