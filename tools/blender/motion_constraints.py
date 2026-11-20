"""Small deterministic constraints for authored motion; no image/model API calls."""
import math


def bisect_target(fn, lo, hi, target, steps=40):
    """Solve a monotonic scalar constraint, rejecting unattainable targets."""
    a,b=fn(lo)-target,fn(hi)-target
    if a*b>0:
        raise ValueError(f'Constraint not bracketed: residuals {a:.5f}, {b:.5f}')
    for _ in range(steps):
        mid=(lo+hi)/2;c=fn(mid)-target
        if a*c<=0:hi=mid
        else:lo=mid;a=c
    return (lo+hi)/2


def planted_ankle(hip, toe, preferred, leg_length=.866, side=1):
    """Intersect leg/foot length circles at ankle height, keeping a fixed toe.

    The ankle can pivot around the planted toe; neither chain is stretched.
    Keep the same anatomical branch throughout the movement.
    """
    z=preferred[2]
    r=math.sqrt(leg_length**2-(hip[2]-z)**2)
    q=math.sqrt(.20**2-(toe[2]-z)**2)
    dx,dy=toe[0]-hip[0],toe[1]-hip[1];d=math.hypot(dx,dy)
    if d>r+q or d<abs(r-q) or d<1e-8:
        raise ValueError(f'Planted foot cannot reach hip: separation={d:.5f}, leg circle={r:.5f}, foot circle={q:.5f}, hip={hip}, toe={toe}')
    along=(r*r-q*q+d*d)/(2*d);height=math.sqrt(max(0,r*r-along*along))
    centre=(hip[0]+along*dx/d,hip[1]+along*dy/d)
    # Keep the same anatomical branch throughout the rep; nearest-point
    # selection can flip sides near the neutral pose and teleport the heel.
    s=-side
    return [centre[0]+s*height*(-dy/d),centre[1]+s*height*(dx/d),z]
