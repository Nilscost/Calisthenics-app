"""Opt-in overhand cylinder grip. Metres; bar axis is world Y.

Capsule centre lines follow the outside of the cylinder. Clearance includes
finger thickness and the chords between knuckles, not just joint positions.
"""
import math

FINGER_RADIUS = .0088
THUMB_RADIUS = .0105


def _fixed_segments(frame, side, bar):
    from hand_axes import axes
    wrist = frame['wrist_'+side]
    _, _, across = axes(wrist, frame['hand_'+side], frame['palmn_'+side], side)
    z, radius = bar['z'], bar['r'] + FINGER_RADIUS + .0082
    segments = []
    for offset, end in ((.030,-20),(.010,-30),(-.010,-25),(-.029,0)):
        y = wrist[1] + across[1]*offset
        points = [(wrist[0],y,wrist[2]+.085)]
        for angle in (110,45,end):
            a=math.radians(angle)
            points.append((radius*math.cos(a),y,z+radius*math.sin(a)))
        segments.extend(zip(points,points[1:]))
    y=wrist[1]+across[1]*.032
    # Opposed thumb passes below the bar, with its own larger capsule radius.
    points=[(wrist[0],y,z-.020),(-.009,y,z-.037),(.027,y,z-.028)]
    segments.extend(zip(points,points[1:]))
    return segments


def wrapped_segments(frame, side, bar):
    if not bar.get('rotate_with_hand'):
        return _fixed_segments(frame, side, bar)
    # Roll the existing wrapped grip as one assembly around the cylinder.
    # The wrist follows the same circle in the authored pose. Default grips
    # retain their previous exact coordinates.
    wrist = frame['wrist_'+side]
    hand = frame['hand_'+side]
    angle = math.atan2(hand[0]-wrist[0], hand[2]-wrist[2])
    c, s = math.cos(angle), math.sin(angle)
    fixed = dict(frame)
    fixed['wrist_'+side] = [-.045, wrist[1], bar['z']-.065]
    fixed['hand_'+side] = [-.045, wrist[1], bar['z']+.105]
    fixed['palmn_'+side] = [1, 0, 0]
    def turn(point):
        x, y, z = point[0], point[1], point[2]-bar['z']
        return [c*x+s*z, y, bar['z']-s*x+c*z]
    return [(turn(a), turn(b)) for a, b in _fixed_segments(fixed, side, bar)]


def segment_clearance(a,b,bar,radius):
    """Signed capsule clearance from an infinite cylinder (conservative at ends)."""
    ax,az=a[0],a[2]-bar['z'];dx,dz=b[0]-a[0],b[2]-a[2]
    denom=dx*dx+dz*dz
    t=max(0.,min(1.,-(ax*dx+az*dz)/denom)) if denom else 0.
    return math.hypot(ax+t*dx,az+t*dz)-bar['r']-radius


def grip_clearance(data):
    return min(segment_clearance(a,b,data['bar_grip'],
               THUMB_RADIUS if i>=12 else FINGER_RADIUS)
               for fr in data['frames'] for side in ('l','r')
               for i,(a,b) in enumerate(wrapped_segments(fr,side,data['bar_grip'])))
