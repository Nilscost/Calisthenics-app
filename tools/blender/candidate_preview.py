"""Low-sample candidate frames for the side-by-side review (runs inside Blender, Workbench).

    Blender --background --python tools/blender/candidate_preview.py -- --out DIR [--count 8] [--az 40] ID...

Writes DIR/<id>/c00.png ... for `count` evenly spaced phases of the loop (same phase order as
reference_sheet.py: frame i of count at i/count of the repetition), plus DIR/<id>/joints.json.
Never writes app assets or the clips work directory; never uses Cycles.
"""
import argparse
import json
import math
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import bpy  # noqa: E402
import poses  # noqa: E402
import render as R  # noqa: E402


def main():
    argv = sys.argv[sys.argv.index('--') + 1:] if '--' in sys.argv else []
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', type=Path, required=True)
    ap.add_argument('--count', type=int, default=8)
    ap.add_argument('--az', type=float)
    ap.add_argument('--el', type=float)
    ap.add_argument('ids', nargs='+')
    args = ap.parse_args(argv)
    poses.load_families()
    for ex in args.ids:
        data, reach = poses.build(ex)
        if args.az is not None:
            data['cam_az'] = args.az
        if args.el is not None:
            data['cam_el'] = args.el
        out = args.out / ex
        out.mkdir(parents=True, exist_ok=True)
        (out / 'joints.json').write_text(json.dumps(data))
        sc = R.scene_setup('WORKBENCH')
        props = R.Props(data['props'], R.material('prop', R.PROP), data['frames'][0])
        fig = R.Figure(data['frames'][0], data.get('anatomy_detail', False), data.get('bar_grip'), None)
        pts = list(props.points)
        for f in data['frames']:
            for k, v in f.items():
                if not (k.endswith(('_up', '_left')) or k.startswith(('palmn_', 'grip_', 'finger_lift_', 'foot_arch_')) or k == 'head_detail'):
                    pts.append(tuple(v))
        pad = data.get('camera_padding', .12)
        pts = [(p[0] + dx, p[1] + dy, max(p[2] + dz, 0)) for p in set(pts)
               for dx, dy, dz in ((0, 0, 0), (pad, 0, 0), (-pad, 0, 0), (0, pad, 0), (0, -pad, 0), (0, 0, pad), (0, 0, -pad))]
        R.camera(data, pts)
        n = len(data['frames'])
        for i in range(args.count):
            fr = data['frames'][int(i * n / args.count) % n]
            fig.pose(fr)
            props.pose(fr)
            sc.render.filepath = str(out / ('c%02d.png' % i))
            bpy.ops.render.render(write_still=True)
        print('[preview] %s: %d frames -> %s' % (ex, args.count, out), flush=True)


main()
