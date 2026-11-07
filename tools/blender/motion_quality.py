"""Token-free whole-repetition movement screening, with compact failure evidence.

python tools/blender/motion_quality.py [ids] [--output report.json]
No rendering, downloading, paid API calls or model inference. Unknown exercises
are explicitly unscoped. Bounds describe these authored variants, not all valid
ways humans can perform an exercise.
"""
import argparse
import hashlib
import json
import math
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
CONTRACT=Path(__file__).with_name('motion_contracts.json')


def digest(data):
    return hashlib.sha256(json.dumps(data,sort_keys=True,separators=(',',':'),allow_nan=False).encode()).hexdigest()


def sub(a,b):return [x-y for x,y in zip(a,b)]
def norm(v):return math.sqrt(sum(x*x for x in v))
def dot(a,b):return sum(x*y for x,y in zip(a,b))
def centre(f,name):return [(a+b)/2 for a,b in zip(f[name+'_l'],f[name+'_r'])]


def angle(a,b,c):
    u,v=sub(a,b),sub(c,b);length=norm(u)*norm(v)
    if length<1e-10:return float('nan')
    return math.degrees(math.acos(max(-1,min(1,dot(u,v)/length))))


def elbow(f,side):return angle(*(f[n+'_'+side] for n in ('shoulder','elbow','wrist')))


def audit(data, contract=None):
    contract=contract or json.loads(CONTRACT.read_text())
    ex=data['id'];definition=contract['exercises'].get(ex)
    if not definition:return {'exercise':ex,'status':'unscoped','failures':[]}
    fs=data.get('frames',[]);b=definition['bounds'];metrics={};failures=[]
    def record(name,value,lo=None,hi=None,frame=None):
        good=math.isfinite(value) and (lo is None or value>=lo) and (hi is None or value<=hi)
        metrics[name]={'value':round(value,5) if math.isfinite(value) else None,'min':lo,'max':hi,'ok':good}
        if not good:failures.append({'check':name,'value':metrics[name]['value'],'frame':frame,'time_s':round(frame/data['fps'],3) if frame is not None else None})
    if len(fs)<4 or data.get('fps',0)<=0:
        return {'exercise':ex,'status':'fail','failures':[{'check':'valid_timeline'}]}
    required=('pelvis','chest','head','wrist_l','wrist_r','ankle_l','ankle_r')
    if not all(isinstance(f,dict) and all(n in f and isinstance(f[n],(list,tuple)) and len(f[n])==3 for n in required)
               and all(isinstance(v,(list,tuple)) and all(isinstance(x,(int,float)) and math.isfinite(x) for x in v) for v in f.values()) for f in fs):
        return {'exercise':ex,'status':'fail','failures':[{'check':'finite_geometry'}]}
    for side in ('l','r'):
        name='wrist_'+side
        values=[norm(sub(f[name],fs[0][name])) for f in fs]
        record(name+'_drift_m',max(values),hi=b['hand_drift_max_m'],frame=values.index(max(values)))
    if definition.get('validator'):
        import importlib
        scoped=importlib.import_module(definition['validator']).audit(data)
        declared=definition.get('metric_bounds',{})
        frame_by_check={f['check']:f.get('frame') for f in scoped['failures']}
        for name,metric in scoped['metrics'].items():
            limits=declared.get(name,metric)
            lo,hi=limits.get('min'),limits.get('max')
            lo=None if lo is None or not math.isfinite(lo) else lo
            hi=None if hi is None or not math.isfinite(hi) else hi
            value=metric.get('value')
            record(name,value if isinstance(value,(int,float)) else float('nan'),lo,hi,frame_by_check.get(name))
    elif ex=='ring-muscle-up':
        rings=[p for p in data.get('props',[]) if p.get('kind')=='ring']
        record('visible_rings_count',len(rings),lo=2,hi=2)
        record('ring_grip_geometry',int(bool(data.get('ring_grip'))),lo=1)
        record('ring_grip_closeup',int(bool(data.get('grip_detail'))),lo=1)
        if rings:record('ring_radius_m',min(p['radius'] for p in rings),lo=b['ring_radius_min_m'])
        from ring_geometry import capsule_ring_clearance
        arm_clearances=[]
        for f in fs:
            for spec in rings:
                side=spec['joint'][-1]
                # Include the slightly larger shoulder/elbow/wrist joint envelopes.
                for start,end,r1,r2 in (('shoulder','elbow',.055,.044),('elbow','wrist',.044,.032)):
                    arm_clearances.append(capsule_ring_clearance(f[start+'_'+side],f[end+'_'+side],r1,r2,f,spec))
        record('arm_ring_volume_clearance_m',min(arm_clearances,default=0),lo=.0005)
        if any(spec.get('frame_center') for spec in rings):
            from ring_geometry import palm_surface_clearance
            palm_clearances=[palm_surface_clearance(f,spec) for f in fs for spec in rings]
            record('palm_ring_surface_clearance_m',min(palm_clearances,default=0),lo=b['palm_surface_clearance_min_m'])
            from ring_geometry import finger_volume_clearance
            finger_clearances=[finger_volume_clearance(f,spec) for f in fs for spec in rings]
            record('finger_ring_volume_clearance_m',min(finger_clearances,default=0),lo=b['finger_volume_clearance_min_m'])

        from articulation_review import bend
        from ring_geometry import ring_frame,grip_offset
        for side in ('l','r'):
            angles=[bend(f['elbow_'+side],f['wrist_'+side],f['hand_'+side]) for f in fs]
            record('wrist_'+side+'_bend_deg',max(v for v in angles if v is not None),hi=b.get('wrist_bend_max_deg',180))
            record('wrist_'+side+'_frame_change_deg',max(abs(v-angles[i-1]) for i,v in enumerate(angles)),hi=b.get('wrist_frame_change_max_deg',180))
            spec=next((r for r in rings if r['joint']=='wrist_'+side),None)
            if spec and spec.get('articulated_contact'):
                errors=[]
                for f in fs:
                    u=sub(f['hand_'+side],f['wrist_'+side]);u=[x/norm(u) for x in u]
                    n=f['palmn_'+side];target=ring_frame(f,spec)[3]
                    offset=grip_offset(f,side)
                    ring_normal=ring_frame(f,spec)[1]
                    lateral=offset[2] if len(offset)>2 else 0.0
                    actual=[w+offset[0]*x+offset[1]*y+lateral*z for w,x,y,z in zip(f['wrist_'+side],u,n,ring_normal)]
                    errors.append(norm(sub(actual,target)))
                record('wrist_'+side+'_contact_error_m',max(errors),hi=.00002)
        chest_x=[f['chest'][0] for f in fs];pelvis_x=[f['pelvis'][0] for f in fs]
        record('chest_fore_aft_m',max(chest_x)-min(chest_x),hi=b['chest_fore_aft_max_m'])
        record('pelvis_fore_aft_m',max(pelvis_x)-min(pelvis_x),hi=b['pelvis_fore_aft_max_m'])
        record('chest_forward_speed_m_s',max(abs(a-b)*data['fps'] for a,b in zip(chest_x,chest_x[1:])),hi=b['chest_forward_speed_max_m_s'])
        record('chest_speed_m_s',max(norm(sub(a['chest'],b_['chest']))*data['fps'] for a,b_ in zip(fs,fs[1:])),hi=b['chest_speed_max_m_s'])
        top=max(range(len(fs)),key=lambda i:fs[i]['chest'][2]);bottom=min(range(len(fs)),key=lambda i:fs[i]['chest'][2])
        record('rise_m',fs[top]['chest'][2]-fs[bottom]['chest'][2],lo=b['rise_min_m'])
        for side in ('l','r'):
            tuck=[f['ankle_'+side][2]-f['hip_'+side][2] for f in fs]
            record('leg_tuck_change_'+side+'_m',max(tuck)-min(tuck),hi=b['leg_tuck_change_max_m'])
            record('hang_elbow_'+side+'_deg',elbow(fs[bottom],side),lo=b['endpoint_elbow_min_deg'])
            record('support_elbow_'+side+'_deg',elbow(fs[top],side),lo=b['endpoint_elbow_min_deg'])
        # Apparatus and wrist articulation supplement the existing full-body
        # identity and volume gates; passing one lane cannot replace another.
        from generation_checks.ring_articulation import audit as ring_articulation
        scoped=ring_articulation(data)
        declared=definition.get('metric_bounds',{})
        frame_by_check={f['check']:f.get('frame') for f in scoped['failures']}
        for name,metric in scoped['metrics'].items():
            limits=declared.get(name,metric)
            value=metric.get('value')
            record(name,value if isinstance(value,(int,float)) else float('nan'),
                   limits.get('min'),limits.get('max'),frame_by_check.get(name))
        if any(r.get('hand_model') == 'rigid-phalanx-v1' for r in rings):
            from generation_checks.ring_hand_rig import audit as hand_rig_audit
            rigid=hand_rig_audit(data)
            for name,metric in rigid['metrics'].items():
                value=metric.get('value')
                limits=declared.get(name,metric)
                record(name,value if isinstance(value,(int,float)) else float('nan'),
                       limits.get('min'),limits.get('max'))
    elif ex=='pullup-pullover':
        from bar_grip import segment_clearance, grip_clearance
        record('duration_s',len(fs)/data['fps'],lo=b['duration_min_s'])
        # Recover an unwrapped full revolution from the solved torso vectors.
        raw=[math.degrees(math.atan2(f['chest_up'][0],f['chest_up'][2])) for f in fs]
        rotation=[raw[0]]
        for a_,b_ in zip(raw,raw[1:]):rotation.append(rotation[-1]+(b_-a_+180)%360-180)
        record('rotation_range_deg',max(rotation)-min(rotation),lo=350,hi=370)
        angular=[(rotation[(i+1)%len(fs)]-rotation[i])*data['fps'] for i in range(len(fs))]
        record('angular_speed_deg_s',max(abs(v) for v in angular),hi=b['angular_speed_max_deg_s'])
        record('angular_acceleration_deg_s2',max(abs(v-angular[i-1])*data['fps'] for i,v in enumerate(angular)),hi=b['angular_acceleration_max_deg_s2'])
        first_support=min(range(len(fs)),key=lambda i:rotation[i])
        for label,sign in [('turnover',-1),('return',1)]:
            speeds=[angular[i]*sign for i,a_ in enumerate(rotation)
                    if -315<a_<-45 and ((i<=first_support) if sign<0 else (i>first_support))]
            record(label+'_interior_speed_deg_s',min(speeds,default=0),lo=b['interior_rotation_speed_min_deg_s'])
        # Use matched central rotation windows; exclude the deliberate holds.
        durations=[sum(1 for i,a_ in enumerate(rotation) if -315<a_<-45 and angular[i]*sign>0)/data['fps'] for sign in (-1,1)]
        record('descent_ascent_time_ratio',durations[1]/durations[0] if durations[0] else 0,lo=b['descent_ascent_ratio_min'])
        tracked=('pelvis','chest','head','elbow_l','elbow_r','knee_l','knee_r','ankle_l','ankle_r')
        acceleration=[norm([fs[(i+1)%len(fs)][n][axis]-2*f[n][axis]+fs[i-1][n][axis] for axis in range(3)])*data['fps']**2 for i,f in enumerate(fs) for n in tracked]
        record('joint_acceleration_m_s2',max(acceleration),hi=b['joint_acceleration_max_m_s2'])
        bar=data.get('bar_grip',{'z':2.3,'r':.017})
        record('rolling_grip',int(bool(bar.get('rotate_with_hand'))),lo=1)
        clearances=[]
        for f in fs:
            for side in ('l','r'):
                # The wrist may rotate, but must stay on the same contact circle.
                clearances.extend(segment_clearance(f[a_+'_'+side],f[b_+'_'+side],bar,radius_)
                    for a_,b_,radius_ in [('shoulder','elbow',.055),('elbow','wrist',.044),('hip','knee',.078),('knee','ankle',.056)])
            clearances.extend(segment_clearance(f[a_],f[b_],bar,radius_)
                for a_,b_,radius_ in [('pelvis','spine',.105),('spine','chest',.105),('chest','neck',.06),('neck','head',.105)])
        for side in ('l','r'):
            errors=[abs(math.hypot(f['wrist_'+side][0],f['wrist_'+side][2]-bar['z'])-math.hypot(.045,.065)) for f in fs]
            record('wrist_'+side+'_contact_error_m',max(errors),hi=.0001)
            record('wrist_'+side+'_axial_drift_m',max(abs(f['wrist_'+side][1]-fs[0]['wrist_'+side][1]) for f in fs),hi=.003)
        record('body_bar_volume_clearance_m',min(clearances),lo=b['bar_volume_clearance_min_m'])
        if data.get('bar_grip'):record('finger_bar_clearance_m',grip_clearance(data),lo=.0005)
        support=min(range(len(fs)),key=lambda i:rotation[i])
        record('support_chest_above_bar_m',fs[support]['chest'][2]-bar['z'],lo=.5)
        for side in ('l','r'):
            record('hang_elbow_'+side+'_deg',elbow(fs[0],side),lo=160)
            record('support_elbow_'+side+'_deg',elbow(fs[support],side),lo=150)
    elif ex=='handstand-press-straddle':
        record('duration_s',len(fs)/data['fps'],lo=b['duration_min_s'])
        top=max(range(len(fs)),key=lambda i:fs[i]['pelvis'][2])
        for side in ('l','r'):
            directions=[sub(f['toe_'+side],f['ankle_'+side]) for f in fs]
            if any(norm(d)<1e-8 for d in directions):
                record(side+'_foot_axis_valid',0,lo=1)
                continue
            directions=[[x/norm(d) for x in d] for d in directions]
            raw=[math.degrees(math.atan2(d[2],d[0])) for d in directions]
            turns=[raw[0]]
            for a_,b_ in zip(raw,raw[1:]):turns.append(turns[-1]+(b_-a_+180)%360-180)
            record(side+'_foot_turn_deg',max(turns)-min(turns),lo=b['foot_turn_min_deg'],hi=b['foot_turn_max_deg'])
            record(side+'_foot_angular_speed_deg_s',max(abs(turns[(i+1)%len(fs)]-v)*data['fps'] for i,v in enumerate(turns)),hi=b['foot_angular_speed_max_deg_s'])
            normals=[];lengths=[];shin_directions=[]
            for f,d in zip(fs,directions):
                shin=sub(f['ankle_'+side],f['knee_'+side]);shin=[x/norm(shin) for x in shin]
                n=[x-y*dot(shin,d) for x,y in zip(shin,d)];lengths.append(norm(n))
                normals.append([x/norm(n) for x in n] if norm(n)>1e-8 else [0,0,0])
                shin_directions.append(shin)
            record(side+'_heel_frame_norm',min(lengths),lo=b['foot_frame_norm_min'])
            record(side+'_heel_frame_step_cos',min(dot(n,normals[i-1]) for i,n in enumerate(normals)),lo=b['foot_frame_step_cos_min'])
            record(side+'_initial_toe_forward',directions[0][0],lo=.99)
            record(side+'_top_toe_shin_dot',dot(directions[top],shin_directions[top]),lo=b['top_toe_shin_dot_min'])
            angles=[angle(f['knee_'+side],f['ankle_'+side],f['toe_'+side]) for f in fs]
            record(side+'_ankle_joint_min_deg',min(angles),lo=b['ankle_joint_min_deg'])
            record(side+'_ankle_joint_max_deg',max(angles),hi=b['ankle_joint_max_deg'])
    elif ex=='pushup-archer':
        chest_y=[f['chest'][1] for f in fs];chest_z=[f['chest'][2] for f in fs]
        record('lateral_range_m',max(chest_y)-min(chest_y),lo=b['lateral_range_min_m'])
        record('chest_range_m',max(chest_z)-min(chest_z),lo=b['chest_range_min_m'])
        for label,sign,working,support in [('right',-1,'r','l'),('left',1,'l','r')]:
            chosen=[(i,f) for i,f in enumerate(fs) if sign*f['chest'][1]>=b['side_shift_min_m']]
            record(label+'_low_frames',len(chosen),lo=4)
            if chosen:
                bottom=min(chosen,key=lambda p:p[1]['chest'][2])
                low=[(i,f) for i,f in chosen if f['chest'][2]<=bottom[1]['chest'][2]+.04]
                straight=[(elbow(f,support),i) for i,f in low];bent=[(elbow(f,working),i) for i,f in low]
                value,i=min(straight);record(label+'_support_elbow_deg',value,lo=b['support_elbow_min_deg'],frame=i)
                value,i=max(bent);record(label+'_working_elbow_deg',value,hi=b['working_elbow_max_deg'],frame=i)
                record(label+'_working_min_deg',min(v for v,i in bent),lo=b['working_elbow_min_deg'])
        for side in ('l','r'):
            name='toe_'+side;values=[norm(sub(f[name],fs[0][name])) for f in fs]
            record(name+'_drift_m',max(values),hi=b['toe_drift_max_m'],frame=values.index(max(values)))
        values=[angle(f['chest'],f['pelvis'],centre(f,'ankle')) for f in fs]
        record('body_line_deg',min(values),lo=b['body_line_min_deg'],frame=values.index(min(values)))
    else:
        top=max(range(len(fs)),key=lambda i:fs[i]['chest'][2]);bottom=min(range(len(fs)),key=lambda i:fs[i]['chest'][2])
        tilt=lambda f:math.degrees(math.atan2(f['chest'][0]-f['pelvis'][0],f['chest'][2]-f['pelvis'][2]))
        tilts=[tilt(f) for f in fs]
        record('top_back_tilt_deg',-tilts[top],lo=b['top_tilt_min_deg'],hi=b['top_tilt_max_deg'],frame=top)
        record('tilt_change_deg',max(tilts)-min(tilts),lo=b['tilt_change_min_deg'])
        hips=[f['pelvis'][0] for f in fs];record('hip_travel_m',max(hips)-min(hips),lo=b['hip_travel_min_m'],hi=b['hip_travel_max_m'])
        offsets=[dot(sub(f['shoulder_l'],f['chest']),f['chest_up']) for f in fs]
        record('shoulder_action_m',max(offsets)-min(offsets),lo=b['shoulder_action_min_m'],hi=b['shoulder_action_max_m'])
        record('rise_m',fs[top]['chest'][2]-fs[bottom]['chest'][2],lo=b['rise_min_m'])
        # Ignore deliberate endpoint holds; reject stops in the middle of travel.
        heights=[f['pelvis'][2] for f in fs];low,high=min(heights),max(heights)
        travel=high-low
        velocities=[abs((heights[(i+1)%len(fs)]-heights[i])*data['fps']) for i in range(len(fs))]
        interior=[i for i,h in enumerate(heights) if travel and .15<(h-low)/travel<.85]
        record('interior_speed_min_m_s',min((velocities[i] for i in interior),default=0),lo=.12)
        # A nonzero speed alone allowed the previous mid-movement surge.
        # Check each direction separately, excluding only endpoint easing.
        for label,sign in (('rise',1),('descent',-1)):
            chosen=[i for i in interior if 0<i<len(fs)-1 and
                    sign*(heights[i+1]-heights[i-1])>0]
            vertical=[abs(heights[i+1]-heights[i-1])*data['fps']/2 for i in chosen]
            torso=[norm(sub(fs[i+1]['chest'],fs[i-1]['chest']))*data['fps']/2 for i in chosen]
            ratio=lambda values:max(values)/min(values) if values and min(values)>1e-8 else float('inf')
            record(label+'_vertical_speed_ratio',ratio(vertical),hi=1.03)
            record(label+'_torso_speed_ratio',ratio(torso),hi=1.12)
        if data.get('bar_grip'):
            from bar_grip import grip_clearance
            record('finger_bar_clearance_m',grip_clearance(data),lo=.0005)
        for side in ('l','r'):
            record('hang_elbow_'+side+'_deg',elbow(fs[bottom],side),lo=b['hang_elbow_min_deg'],frame=bottom)
            record('top_elbow_'+side+'_deg',elbow(fs[top],side),lo=b['top_elbow_min_deg'],hi=b['top_elbow_max_deg'],frame=top)
            record('palm_'+side+'_forward',min(f['palmn_'+side][0] for f in fs),lo=b['palm_forward_min'])
    # Common anti-teleport/loop checks, deliberately generous engineering limits.
    names=('pelvis','chest','head','wrist_l','wrist_r','ankle_l','ankle_r')
    speeds=[(norm(sub(fs[i][name],fs[i-1][name]))*data['fps'],i) for i in range(len(fs)) for name in names]
    value,i=max(speeds);record('common_max_joint_speed_m_s' if definition.get('validator') else 'max_joint_speed_m_s',value,hi=b.get('max_joint_speed_max_m_s',2.5),frame=i)
    record('loop_gap_m',max(norm(sub(fs[-1][name],fs[0][name])) for name in names),hi=.012)
    return {'exercise':ex,'style':definition['style'],'status':'pass' if not failures else 'fail',
            'metrics':metrics,'failures':failures,'data_sha256':digest(data),'contract_sha256':digest(contract),
            'meaning':'Authored-motion screening only; reference and encoded visual review still required'}


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('ids',nargs='*');p.add_argument('--output');p.add_argument('--joints',type=Path)
    args=p.parse_args();import poses;poses.load_families()
    contract=json.loads(CONTRACT.read_text());reports=[]
    if args.joints:
        if args.ids:p.error('Use IDs or --joints')
        reports=[audit(json.loads(args.joints.read_text()),contract)]
    else:
        for ex in args.ids or contract['exercises']:
            if ex not in poses.EXERCISES:p.error('Unknown exercise: '+ex)
            data,reach=poses.build(ex);r=audit(data,contract)
            geometry=reach+poses.checks(data)
            if geometry:r['status']='fail';r['failures'].append({'check':'geometry','problems':geometry})
            reports.append(r)
    if args.output:Path(args.output).write_text(json.dumps(reports,indent=2)+'\n')
    for r in reports:
        print(r['exercise']+': '+r['status'].upper()+''.join('\n  '+str(f) for f in r['failures']))
    return int(any(r['status']=='fail' for r in reports))


if __name__=='__main__':raise SystemExit(main())
