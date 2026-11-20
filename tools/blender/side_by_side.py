"""Side-by-side contact sheet: candidate phases beside reference phases, same order, labelled.

    python tools/blender/side_by_side.py --exercise ID --candidate DIR/ID --references REFDIR --out ID.png

Rows: the candidate (low-sample Blender preview frames c00..), then one row per reference
(frames f00.. extracted by reference_sheet.py). Columns are the same fraction of one
repetition, so a reviewer reads each column top to bottom and compares the posture.
Optionally a stick-figure row from preview_sheet.py (side view) is added under the candidate.
The coordinator accepts the clip, or sends it back with one concrete observation, from this sheet.
"""
import argparse
import json
import sys
from pathlib import Path

from PIL import Image, ImageDraw

CELL = (240, 180)


def _fit(path, cell):
    im = Image.open(path).convert('RGB')
    im.thumbnail(cell)
    canvas = Image.new('RGB', cell, (245, 245, 245))
    canvas.paste(im, ((cell[0] - im.width) // 2, (cell[1] - im.height) // 2))
    return canvas


def compose(rows, columns, out, title='', cell=CELL, label_w=150):
    """rows: list of (label, [frame paths or None]); every row has `columns` cells."""
    for label, frames in rows:
        if len(frames) != columns:
            raise ValueError('row %r has %d frames, expected %d' % (label, len(frames), columns))
    head = 34
    img = Image.new('RGB', (label_w + columns * cell[0], head + len(rows) * (cell[1] + 4)), 'white')
    d = ImageDraw.Draw(img)
    d.text((6, 8), title, fill=(0, 0, 0))
    for c in range(columns):
        d.text((label_w + c * cell[0] + 4, 20), 'phase %d/%d' % (c + 1, columns), fill=(90, 90, 90))
    for r, (label, frames) in enumerate(rows):
        y = head + r * (cell[1] + 4)
        d.text((4, y + 4), label[:22], fill=(0, 0, 0))
        d.text((4, y + 18), label[22:44], fill=(0, 0, 0))
        for c, f in enumerate(frames):
            if f:
                img.paste(_fit(f, cell), (label_w + c * cell[0], y))
    img.save(out)
    return out


def build(exercise, candidate_dir, reference_dirs, out, columns=8, stick=None):
    candidate_dir = Path(candidate_dir)
    cand = [candidate_dir / ('c%02d.png' % i) for i in range(columns)]
    if not all(p.exists() for p in cand):
        raise FileNotFoundError('missing candidate frames in %s' % candidate_dir)
    rows = [('CANDIDATE (new)', cand)]
    if stick:
        rows.append(('stick figure', stick))
    for ref in reference_dirs:
        ref = Path(ref)
        frames = [ref / ('f%02d.png' % i) for i in range(columns)]
        if all(p.exists() for p in frames):
            rows.append(('ref ' + ref.name.replace('-frames', '')[:30], frames))
    return compose(rows, columns, out, 'Batch A side-by-side: ' + exercise)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--exercise', required=True)
    ap.add_argument('--candidate', type=Path, required=True)
    ap.add_argument('--references', type=Path, required=True, help='directory holding <exercise>-refN-frames')
    ap.add_argument('--out', type=Path, required=True)
    ap.add_argument('--columns', type=int, default=8)
    args = ap.parse_args(argv)
    refs = sorted(args.references.glob(args.exercise + '-ref*-frames'))
    build(args.exercise, args.candidate, refs, args.out, args.columns)
    print(args.out)
    return 0


if __name__ == '__main__':
    sys.exit(main())
