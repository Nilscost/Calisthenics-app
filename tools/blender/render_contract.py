"""Provenance shared by renderer and installer for grip-critical media."""
import hashlib
from pathlib import Path


def render_stamp():
    root = Path(__file__).resolve().parent
    names=['render.py', 'ring_geometry.py', 'ring_hand_rig.py', 'bar_grip.py', 'hand_axes.py', 'anatomy.py', 'motion_quality.py',
           'motion_contracts.json', 'motion_constraints.py']
    names += [str(p.relative_to(root)) for p in sorted((root/'generation_checks').glob('*.py'))]
    return {name: hashlib.sha256((root / name).read_bytes()).hexdigest()
            for name in names}
