"""Run inside Blender: anatomical hand geometry checks using the real renderer.

blender -b -P tools/blender/check_hand_geometry.py
"""
import math
from pathlib import Path
import sys

sys.path.insert(0,str(Path(__file__).resolve().parent))
import poses
from render import hand_frame,hand_segments

poses.load_families()
checked=0
for exercise in sorted(poses.EXERCISES):
    data,_=poses.build(exercise)
    for frame in data['frames']:
        for side in ('l','r'):
            wrist,u,n,thumb_axis=hand_frame(frame,side)
            pc,_,_,_,segments=hand_segments(frame,side)
            assert abs(u.dot(n))<1e-6,exercise
            assert abs(u.dot(thumb_axis))<1e-6,exercise
            assert all(math.isfinite(v) for segment in segments for p in segment for v in p),exercise
            # Index MCP must be closer to the thumb base than little MCP.
            index_base=segments[0][0];little_base=segments[6][0];thumb_base=segments[-1][0]
            assert (index_base-thumb_base).length < (little_base-thumb_base).length,exercise
            checked+=1
print(f'PASS: {checked} sampled anatomical hands across {len(poses.EXERCISES)} exercise poses; no form certification')

# Exercise the actual renderer resume path separately from geometry, with a
# temporary cache. This checks cache handling; it is not device evidence.
import json
import os
import tempfile
import render
from render_contract import render_stamp

with tempfile.TemporaryDirectory() as tmp:
    previous=render.WORK
    try:
        render.WORK=tmp
        root=Path(tmp)/'test';frames=root/'blender'/'frames';frames.mkdir(parents=True)
        detail=root/'blender'/'grip-frames';detail.mkdir()
        joints=root/'joints.json';joints.write_text(json.dumps({'frames':[{},{}],'grip_detail':'test'}))
        stamp=root/'blender'/'render-detail.json';stamp.write_text(json.dumps(render_stamp()))
        fresh=joints.stat().st_mtime_ns+2_000_000_000
        for directory in (frames,detail):
            for name in ('0000.png','0001.png'):
                path=directory/name;path.write_bytes(b'cache fixture');os.utime(path,ns=(fresh,fresh))
        assert render.done_already('test')
        (detail/'0001.png').unlink()
        assert not render.done_already('test')
        path=detail/'0001.png';path.write_bytes(b'cache fixture');os.utime(path,ns=(fresh,fresh))
        (frames/'0002.png').write_bytes(b'extra fixture')
        assert not render.done_already('test')
        (frames/'0002.png').unlink();stamp.write_text('{}')
        assert not render.done_already('test')
    finally:
        render.WORK=previous
print('PASS: real renderer resume path accepts complete current cache and rejects missing, extra or stale cache')
