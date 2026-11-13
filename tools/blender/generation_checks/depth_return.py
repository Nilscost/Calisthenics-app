"""Reset-phase facing and planted-sole screens; not a biomechanical approval."""
import math
from generation_checks.depth_jump import _contact_flags, _phase_indices


def audit(data):
    fs=data.get('frames',[]);fps=data.get('fps',0);metrics={};failures=[]
    def record(name,value,lo=None,hi=None):
        valid=math.isfinite(value);ok=valid and (lo is None or value>=lo) and (hi is None or value<=hi)
        metrics[name]={'value':round(value,6) if valid else None,'min':lo,'max':hi,'ok':ok}
        if not ok:failures.append({'check':name,'value':metrics[name]['value']})
    floor,onbox=_contact_flags(data)
    if not fs or fps<=0 or floor is None:return {'metrics':metrics,'failures':[{'check':'return_data_valid','value':0}]}
    landing=_phase_indices(fs,floor,onbox)['second_touch']
    if landing is None:return {'metrics':metrics,'failures':[{'check':'return_landing_present','value':0}]}
    first=min(len(fs)-1,landing+round(.5*fps));align=[];slip=[];twist=[];facing=[]
    def forward(f):
        left,up=f['pelvis_left'],f['pelvis_up']
        v=[left[1]*up[2]-left[2]*up[1],left[2]*up[0]-left[0]*up[2]]
        n=math.hypot(*v);return [x/n for x in v]
    def footaxis(f,side):
        v=[f['toe_'+side][j]-f['ankle_'+side][j] for j in range(2)]
        n=math.hypot(*v);return [x/n for x in v]
    def planted(f,side):
        z=f['ankle_'+side][2]
        return abs(z-.08)<.0002 or (onbox(f,side) and abs(z-.38)<.0002)
    for a,b in zip(fs[first:],fs[first+1:]):
        fa,fb=forward(a),forward(b);facing.append(fb[0])
        direction=[b['pelvis'][j]-a['pelvis'][j] for j in range(2)];distance=math.hypot(*direction)
        # Turning in place has no travel direction. Traveling reset segments
        # should face their destination rather than crab or moonwalk there.
        if distance*fps>.04 and sum(x*y for x,y in zip(fa,fb))>math.cos(math.radians(3)):
            align.append(sum(x*y for x,y in zip(fb,direction))/distance)
        for side in ('l','r'):
            if planted(a,side) and planted(b,side) and abs(a['ankle_'+side][2]-b['ankle_'+side][2])<.0002:
                slip.append(math.dist(a['ankle_'+side],b['ankle_'+side]))
                cosine=max(-1.,min(1.,sum(x*y for x,y in zip(footaxis(a,side),footaxis(b,side)))))
                twist.append(math.degrees(math.acos(cosine)))
    record('return_travel_alignment_min',min(align,default=float('nan')),.8)
    record('return_planted_sole_slip_m',max(slip,default=float('nan')),hi=.003)
    record('return_planted_sole_twist_deg',max(twist,default=float('nan')),hi=2.)
    record('return_reverse_facing_min_x',min(facing,default=float('nan')),hi=-.8)
    return {'metrics':metrics,'failures':failures}
