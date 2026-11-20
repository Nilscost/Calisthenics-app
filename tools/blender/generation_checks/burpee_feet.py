"""Burpee-specific toe support and feet-in continuity; not form certification."""
import math
from generation_checks.burpee_articulation import _plank_mask


def audit(data):
    from generation_checks.conditioning import _foot_capsule_min_z
    frames, fps = data['frames'], data['fps']
    planted = [i for i, yes in enumerate(_plank_mask(frames)) if yes]
    metrics, failures = {}, []
    def record(name, value, lo=None, hi=None):
        ok = value is not None and math.isfinite(value) and (lo is None or value >= lo) and (hi is None or value <= hi)
        metrics[name] = dict(value=round(value,6) if value is not None and math.isfinite(value) else None,min=lo,max=hi,ok=bool(ok))
        if not ok: failures.append(dict(check=name,value=metrics[name]['value']))
    # The foot's 0.20 m toe axis must descend from a raised ankle through the
    # planted press. This rejects a flat, floating sole, without scaling it.
    drop = min((frames[i]['ankle_'+s][2]-frames[i]['toe_'+s][2]
                for i in planted for s in ('l','r')),default=None)
    record('burpee_plank_toe_drop_min_m',drop,lo=.04)
    skin = [_foot_capsule_min_z(frames[i],s) for i in planted for s in ('l','r')]
    record('burpee_plank_skin_floor_max_m',max(skin,default=None),hi=.006)
    record('burpee_plank_skin_floor_min_m',min(skin,default=None),lo=-.002)
    end = max(planted,default=len(frames)-1)
    landing = next((i for i in range(end+1,len(frames)) if all(
        frames[i]['ankle_'+s][0]>-.02 and frames[i]['ankle_'+s][2]<.095 for s in ('l','r'))),None)
    record('burpee_feet_in_landing_found',int(landing is not None),lo=1,hi=1)
    speeds,accels = [],[]
    if landing is not None:
        for side in ('l','r'):
            key='ankle_'+side
            speeds += [math.dist(frames[i][key],frames[i-1][key])*fps for i in range(end+1,landing+1)]
            accels += [math.sqrt(sum((frames[i+1][key][j]-2*frames[i][key][j]+frames[i-1][key][j])**2 for j in range(3)))*fps*fps for i in range(end+1,landing)]
    record('burpee_feet_in_peak_speed_m_s',max(speeds,default=None),hi=1.8)
    record('burpee_feet_in_peak_accel_m_s2',max(accels,default=None),hi=15)
    return dict(metrics=metrics,failures=failures)
