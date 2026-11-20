"""Handed anatomical axes, independent of Blender for geometry checks."""
import math


def unit(v):
    length = math.sqrt(sum(x*x for x in v))
    if length < 1e-8:
        raise ValueError("degenerate hand axis")
    return [x/length for x in v]


def axes(wrist, tip, normal, side):
    if side not in ("l", "r"):
        raise ValueError("hand side must be l or r")
    u = unit([b-a for a, b in zip(wrist, tip)])
    dot = sum(a*b for a, b in zip(u, normal))
    projected = [normal[i]-u[i]*dot for i in range(3)]
    if sum(x*x for x in projected) < 1e-12:
        # Choose a perpendicular fallback, even when fingers point down.
        fallback = min(([1,0,0], [0,1,0], [0,0,1]), key=lambda v: abs(sum(a*b for a,b in zip(u,v))))
        dot = sum(a*b for a,b in zip(u, fallback))
        projected = [fallback[i]-u[i]*dot for i in range(3)]
    n = unit(projected)
    cross = [u[1]*n[2]-u[2]*n[1], u[2]*n[0]-u[0]*n[2], u[0]*n[1]-u[1]*n[0]]
    # Thumb side follows handedness, not the world/body midline. Supinating
    # the palm must rotate the thumb to the other side of the bar.
    thumb = [x * (-1 if side == "l" else 1) for x in cross]
    return u, n, thumb
