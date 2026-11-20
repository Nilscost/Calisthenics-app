"""Reference contact sheet: 6 to 8 evenly spaced frames of one repetition, labelled, one PNG.

    python tools/blender/reference_sheet.py --exercise plank-long-lever --out sheet.png [--frames 8]

Frames are the primary evidence. Below them a small joint-angle table (elbow, shoulder, hip,
knee, trunk) is printed only when MediaPipe sees the body; otherwise every cell reads "n/a"
(MediaPipe often fails on floor and inverted poses). Local analysis only; nothing here ships.
"""
import argparse
import math
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw

import reference_fetch

ANGLE_ROWS = ('elbow', 'shoulder', 'hip', 'knee', 'trunk')
CELL_W, CELL_H = 240, 180


def frame_times(duration, count):
    """`count` evenly spaced offsets in [0, duration): the repetition's end equals its start."""
    if not 6 <= count <= 8:
        raise ValueError('frames per repetition must be 6 to 8, got %d' % count)
    return [duration * i / count for i in range(count)]


def ffmpeg_binary():
    found = shutil.which('ffmpeg')
    if found:
        return found
    import imageio_ffmpeg
    return imageio_ffmpeg.get_ffmpeg_exe()


def video_duration(path, ffmpeg=None):
    ffmpeg = ffmpeg or ffmpeg_binary()
    out = subprocess.run([ffmpeg, '-i', str(path)], capture_output=True, text=True).stderr
    import re
    m = re.search(r'Duration: (\d+):(\d+):([\d.]+)', out)
    if not m:
        raise RuntimeError('cannot read duration of %s' % path)
    return int(m.group(1)) * 3600 + int(m.group(2)) * 60 + float(m.group(3))


def extract_frames(video, offsets, out_dir, ffmpeg=None, run=subprocess.run):
    """Decode one PNG per offset (seconds from the start of the cached interval)."""
    ffmpeg = ffmpeg or ffmpeg_binary()
    paths = []
    for i, t in enumerate(offsets):
        out = Path(out_dir) / ('f%02d.png' % i)
        run([ffmpeg, '-y', '-loglevel', 'error', '-ss', '%.3f' % t, '-i', str(video), '-frames:v', '1', str(out)], check=True)
        paths.append(out)
    return paths


def angle3(a, b, c):
    u, v = [x - y for x, y in zip(a, b)], [x - y for x, y in zip(c, b)]
    n = math.hypot(*u) * math.hypot(*v)
    if n < 1e-10:
        return None
    return math.degrees(math.acos(max(-1, min(1, sum(x * y for x, y in zip(u, v)) / n))))


def angle_table(points, visibility=None, min_visibility=.5):
    """Joint angles of one frame from 2D/3D landmark dict (MediaPipe names), else n/a.

    points: shoulder_l/r, elbow_l/r, wrist_l/r, hip_l/r, knee_l/r, ankle_l/r -> coordinates.
    Returns {row: 'deg' | 'n/a'}; a row is n/a unless the joints it needs are all visible.
    Elbow/shoulder/hip/knee show the better-seen (higher mean visibility) side.
    """
    visibility = visibility or {}
    def ok(names):
        return all(n in points and visibility.get(n, 1) >= min_visibility for n in names)
    def best(fmt):
        results = []
        for s in ('l', 'r'):
            names = [n.format(s=s) for n in fmt]
            if ok(names):
                a = angle3(*(points[n] for n in names))
                if a is not None:
                    results.append((sum(visibility.get(n, 1) for n in names), a))
        return '%.0f' % max(results)[1] if results else 'n/a'
    row = {'elbow': best(('shoulder_{s}', 'elbow_{s}', 'wrist_{s}')),
           'shoulder': best(('hip_{s}', 'shoulder_{s}', 'elbow_{s}')),
           'hip': best(('shoulder_{s}', 'hip_{s}', 'knee_{s}')),
           'knee': best(('hip_{s}', 'knee_{s}', 'ankle_{s}'))}
    trunk_names = ['shoulder_l', 'shoulder_r', 'hip_l', 'hip_r']
    if ok(trunk_names):
        sh = [(points['shoulder_l'][i] + points['shoulder_r'][i]) / 2 for i in range(2)]
        hp = [(points['hip_l'][i] + points['hip_r'][i]) / 2 for i in range(2)]
        # trunk angle from vertical in the image plane (image y grows downward)
        row['trunk'] = '%.0f' % abs(math.degrees(math.atan2(sh[0] - hp[0], hp[1] - sh[1])))
    else:
        row['trunk'] = 'n/a'
    return row


def mediapipe_angles(frame_paths):
    """Per-frame angle tables when mediapipe and its pose model are available, else None."""
    try:
        import mediapipe  # noqa: F401
    except Exception:
        return None
    return None  # model bundle wiring lives in pose_compare.py extract-references; frames stay primary


def compose(frames, labels, title, out, angles=None, cell=(CELL_W, CELL_H)):
    """One PNG: title, row of labelled frames, then the angle table (n/a where unseen)."""
    n = len(frames)
    cw, ch = cell
    cols = min(n, 4)
    rows = math.ceil(n / cols)
    table_h = 20 * (len(ANGLE_ROWS) + 1) + 12
    img = Image.new('RGB', (cols * cw, 28 + rows * (ch + 18) + table_h), 'white')
    d = ImageDraw.Draw(img)
    d.text((6, 8), title, fill=(0, 0, 0))
    for i, (path, label) in enumerate(zip(frames, labels)):
        im = Image.open(path).convert('RGB')
        im.thumbnail((cw - 4, ch))
        x, y = (i % cols) * cw, 28 + (i // cols) * (ch + 18)
        img.paste(im, (x + 2, y + 16))
        d.text((x + 4, y + 2), label, fill=(0, 0, 0))
    y0 = 28 + rows * (ch + 18) + 4
    d.text((6, y0), 'angles (deg)', fill=(60, 60, 60))
    for r, name in enumerate(ANGLE_ROWS, 1):
        d.text((6, y0 + 20 * r), name, fill=(60, 60, 60))
        for i in range(n):
            val = (angles[i].get(name, 'n/a') if angles else 'n/a')
            d.text((80 + i * (cw * cols - 80) // n, y0 + 20 * r), val, fill=(0, 0, 0) if val != 'n/a' else (160, 160, 160))
    img.save(out)
    return out


def build_sheet(entry, video, out, count=8, work_dir=None, ffmpeg=None):
    duration = video_duration(video, ffmpeg)
    offsets = frame_times(duration, count)
    tmp = Path(work_dir) if work_dir else Path(tempfile.mkdtemp(prefix='refsheet-'))
    tmp.mkdir(parents=True, exist_ok=True)
    frames = extract_frames(video, offsets, tmp, ffmpeg)
    labels = ['%d/%d  t=%.1fs' % (i + 1, count, entry['interval'][0] + o) for i, o in enumerate(offsets)]
    title = '%s | %s | %s | %s' % (entry.get('creator', '?'), entry.get('video_id', ''), entry.get('variant', ''), entry.get('view', ''))
    return compose(frames, labels, title, out, mediapipe_angles(frames)), frames


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--exercise', required=True)
    p.add_argument('--out-dir', type=Path, required=True)
    p.add_argument('--frames', type=int, default=8)
    p.add_argument('--cache-dir', type=Path, default=reference_fetch.DEFAULT_CACHE)
    p.add_argument('--refs', type=Path, default=reference_fetch.REFS)
    args = p.parse_args(argv)
    data = reference_fetch.load(args.refs)
    args.out_dir.mkdir(parents=True, exist_ok=True)
    for i, entry in enumerate(reference_fetch.entries_for(data, args.exercise)):
        video = args.cache_dir / reference_fetch.cache_key(entry['url'], entry['interval'])
        if not video.exists():
            print('missing %s; run reference_fetch.py first' % video)
            continue
        out = args.out_dir / ('%s-ref%d.png' % (args.exercise, i + 1))
        build_sheet(entry, video, out, args.frames, work_dir=args.out_dir / ('%s-ref%d-frames' % (args.exercise, i + 1)))
        print(out)
    return 0


if __name__ == '__main__':
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    sys.exit(main())
