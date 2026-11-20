"""Scoped engineering screens for a bilateral Pilates Saw review candidate.

Bounds describe this authored loop, not clinical/expert form approval.
"""
import math


def audit(data):
    metrics, failures = {}, []
    def record(name, value, lo=None, hi=None):
        good = isinstance(value, (int, float)) and math.isfinite(value)
        ok = good and (lo is None or value >= lo) and (hi is None or value <= hi)
        metrics[name] = dict(value=round(value, 6) if good else None, min=lo, max=hi, ok=ok)
        if not ok: failures.append(dict(check=name, value=metrics[name]['value']))
    frames = data.get('frames', [])
    names = ('pelvis','spine','chest','neck','head') + tuple(
        f'{joint}_{side}' for joint in ('hip','knee','ankle','toe','shoulder','elbow','wrist','hand') for side in ('l','r'))
    valid = len(frames) >= 4 and data.get('id') == 'pilates-saw' and data.get('fps', 0) > 0
    valid = valid and all(all(isinstance(f.get(k), (list, tuple)) and len(f[k]) == 3 and all(isinstance(v,(int,float)) and math.isfinite(v) for v in f[k]) for k in names+('chest_left',)) for f in frames)
    record('saw_valid_geometry', int(valid), 1, 1)
    if not valid: return dict(metrics=metrics, failures=failures)
    pairs = list(zip(frames,frames[1:]+frames[:1]))
    record('saw_joint_speed_m_s', max(math.dist(a[k],b[k])*data['fps'] for a,b in pairs for k in names), hi=2.5)
    record('saw_seam_m', max(math.dist(frames[0][k],frames[-1][k]) for k in names), hi=.012)
    record('saw_pelvis_drift_m', max(math.dist(f['pelvis'],frames[0]['pelvis']) for f in frames), hi=.04)
    record('saw_fold_range_m', max(f['chest'][2] for f in frames)-min(f['chest'][2] for f in frames), .30, .40)
    record('saw_left_rotation', max(f['chest_left'][0] for f in frames), .5, .75)
    record('saw_right_rotation', -min(f['chest_left'][0] for f in frames), .5, .75)
    for side in ('l','r'):
        record('saw_'+side+'_heel_drift_m', max(math.dist(f['ankle_'+side],frames[0]['ankle_'+side]) for f in frames), hi=.003)
        angles=[]
        for f in frames:
            a=[x-y for x,y in zip(f['hip_'+side],f['knee_'+side])];b=[x-y for x,y in zip(f['ankle_'+side],f['knee_'+side])]
            denom=math.sqrt(sum(x*x for x in a)*sum(x*x for x in b))
            angles.append(math.degrees(math.acos(max(-1,min(1,sum(x*y for x,y in zip(a,b))/denom)))))
        record('saw_'+side+'_knee_min_deg',min(angles),lo=165)
    return dict(metrics=metrics,failures=failures)
