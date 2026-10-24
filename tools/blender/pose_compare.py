"""Local, reference-based movement screening. Never certifies correct form.

extract: decode a reviewed interval with MediaPipe pose + hand models.
compare: phase-align angle trajectories against 3–5 independent references.
All reference video stays in ignored work/, never in application assets.
"""
import argparse
import hashlib
import json
import math
from pathlib import Path
import statistics

ANGLE_JOINTS = {
    'elbow_l': ('shoulder_l', 'elbow_l', 'wrist_l'),
    'elbow_r': ('shoulder_r', 'elbow_r', 'wrist_r'),
    'shoulder_l': ('hip_l', 'shoulder_l', 'elbow_l'),
    'shoulder_r': ('hip_r', 'shoulder_r', 'elbow_r'),
    'knee_l': ('hip_l', 'knee_l', 'ankle_l'),
    'knee_r': ('hip_r', 'knee_r', 'ankle_r'),
}
POSE_IDS = dict(shoulder_l=11, shoulder_r=12, elbow_l=13, elbow_r=14,
                wrist_l=15, wrist_r=16, hip_l=23, hip_r=24,
                knee_l=25, knee_r=26, ankle_l=27, ankle_r=28)


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def angle(a, b, c):
    u = [x-y for x,y in zip(a,b)]
    v = [x-y for x,y in zip(c,b)]
    lengths = math.sqrt(sum(x*x for x in u)*sum(x*x for x in v))
    if lengths < 1e-10:
        return None
    return math.degrees(math.acos(max(-1, min(1, sum(x*y for x,y in zip(u,v))/lengths))))


def features(points):
    return {name: angle(*(points[j] for j in joints))
            for name,joints in ANGLE_JOINTS.items() if all(j in points for j in joints)}


def distance(a, b):
    shared = [k for k in ANGLE_JOINTS if a.get(k) is not None and b.get(k) is not None]
    if len(shared) < 2:
        return None
    return statistics.mean(abs(a[k]-b[k]) for k in shared)


def align(candidate, reference):
    """Constrained DTW; retain missing frames as gaps instead of hiding them."""
    n,m = len(candidate),len(reference)
    if min(n,m) < 3:
        return None
    costs={(0,0):0}; prev={}
    for i in range(1,n+1):
        for j in range(1,m+1):
            if abs(i/n-j/m) > .25:
                continue
            d=distance(candidate[i-1],reference[j-1])
            # Missing measurements are expensive, never a perfect match.
            penalty=90 if d is None else d
            best=min(((costs.get(k,math.inf),k) for k in ((i-1,j-1),(i-1,j),(i,j-1))))
            if math.isfinite(best[0]):
                costs[i,j]=best[0]+penalty;prev[i,j]=best[1]
    if (n,m) not in costs:
        return None
    pairs=[];p=(n,m)
    while p != (0,0):
        pairs.append((p[0]-1,p[1]-1));p=prev[p]
    errors={}
    for k in ANGLE_JOINTS:
        values=[abs(candidate[i][k]-reference[j][k]) for i,j in pairs
                if candidate[i].get(k) is not None and reference[j].get(k) is not None]
        if values:
            errors[k]={'mean_deg':round(statistics.mean(values),2),
                       'p95_deg':round(sorted(values)[min(len(values)-1,int(.95*len(values)))],2),
                       'coverage':round(len(values)/len(pairs),3)}
    measured=sum(distance(candidate[i],reference[j]) is not None for i,j in pairs)
    return {'mean_deg':round(costs[n,m]/len(pairs),2), 'path_coverage':round(measured/len(pairs),3),
            'joints':errors, 'aligned_pairs':list(reversed(pairs))}


def extract(args):
    import cv2
    import mediapipe as mp
    from mediapipe.tasks import python
    from mediapipe.tasks.python import vision
    source=Path(args.video)
    import inspect
    key={'video_sha256':digest(source),'interval':[args.start,args.end],
         'pose_model_sha256':digest(args.pose_model),'hand_model_sha256':digest(args.hand_model),
         'hz':args.hz,'confidence':args.confidence,'mediapipe':mp.__version__,'opencv':cv2.__version__,
         'extractor_sha256':hashlib.sha256((inspect.getsource(extract)+inspect.getsource(features)+inspect.getsource(angle)).encode()).hexdigest()}
    output=Path(args.output)
    if output.is_file():
        try:
            cached=json.loads(output.read_text())
            if cached.get('extraction_key')==key and cached.get('frames'):
                print(json.dumps({'output':args.output,'cached':True,'frames':len(cached['frames'])}))
                return
        except (ValueError,OSError):
            pass
    cap=cv2.VideoCapture(str(source))
    if not cap.isOpened():
        raise ValueError('Cannot open video')
    fps=cap.get(cv2.CAP_PROP_FPS);duration=cap.get(cv2.CAP_PROP_FRAME_COUNT)/fps
    if not (0 <= args.start < args.end <= duration+.1):
        raise ValueError('Select a valid demonstration interval')
    base=lambda p:python.BaseOptions(model_asset_path=str(p),delegate=python.BaseOptions.Delegate.CPU)
    po=vision.PoseLandmarkerOptions(base_options=base(args.pose_model),running_mode=vision.RunningMode.VIDEO,num_poses=1)
    ho=vision.HandLandmarkerOptions(base_options=base(args.hand_model),running_mode=vision.RunningMode.VIDEO,num_hands=2)
    rows=[]
    with vision.PoseLandmarker.create_from_options(po) as pose, vision.HandLandmarker.create_from_options(ho) as hands:
        count=math.ceil((args.end-args.start)*args.hz)
        for i in range(count):
            t=args.start+i/args.hz
            cap.set(cv2.CAP_PROP_POS_MSEC,t*1000);ok,frame=cap.read()
            if not ok:break
            height,width=frame.shape[:2]
            image=mp.Image(image_format=mp.ImageFormat.SRGB,data=cv2.cvtColor(frame,cv2.COLOR_BGR2RGB))
            stamp=round(i*1000/args.hz)
            p=pose.detect_for_video(image,stamp);h=hands.detect_for_video(image,stamp)
            points={}
            if p.pose_landmarks:
                for name,idx in POSE_IDS.items():
                    lm=p.pose_landmarks[0][idx]
                    if min(lm.visibility,lm.presence) >= args.confidence:
                        points[name]=[lm.x*width,lm.y*height]
            # Retain all 21 hand landmarks for a separate grip review. Body
            # landmarks or a detector's handedness label do NOT prove grip.
            hand_data=[]
            for landmarks,categories in zip(h.hand_landmarks,h.handedness):
                hand_data.append({'label':categories[0].category_name,'score':categories[0].score,
                                  'points':[[v.x,v.y,v.z] for v in landmarks]})
            rows.append({'time':round(t,4),'points':points,'angles':features(points),'hands':hand_data})
    cap.release()
    if not rows:raise ValueError('No source frames decoded; no analysis cached')
    result={'extraction_key':key,'schema':1,'video_sha256':digest(source),'source':str(source),'interval':[args.start,args.end],
            'hz':args.hz,'confidence':args.confidence,'space':'image_2d',
            'pose_model_sha256':digest(args.pose_model),'hand_model_sha256':digest(args.hand_model),
            'frames':rows,'grip_status':'requires_visual_review',
            'hand_frame_coverage':round(sum(bool(r['hands']) for r in rows)/max(1,len(rows)),3)}
    Path(args.output).write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({'output':args.output,'frames':len(rows),'body_coverage':sum(len(r['angles'])>=2 for r in rows)/max(1,len(rows)),
                      'hand_coverage':result['hand_frame_coverage']}))


def project(args):
    # Export from Blender's actual camera projection, not a guessed 3D angle.
    data=json.loads(Path(args.joints).read_text())
    if 'projected_frames' not in data:
        raise ValueError('Requires camera projection exported by render.py')
    rows=[{'time':i/data['fps'],'points':p,'angles':features(p),'hands':[]} for i,p in enumerate(data['projected_frames'])]
    Path(args.output).write_text(json.dumps({'schema':1,'space':'image_2d','frames':rows,
        'source_sha256':digest(args.joints),'grip_status':'requires_visual_review'},indent=2)+'\n')


def compare(args):
    manifest=json.loads(Path(args.manifest).read_text())
    references=[r for r in manifest['references'] if args.exercise in r['exercises']]
    if not 3 <= len({r['video_id'] for r in references}) <= 5:
        raise ValueError('Need 3–5 distinct reference videos per exercise')
    candidate=json.loads(Path(args.candidate).read_text())
    if candidate.get('space') != 'image_2d':
        raise ValueError('Candidate must use camera-projected image coordinates')
    expected_joints=digest(args.joints) if getattr(args,'joints',None) else candidate.get('source_joints_sha256')
    results=[]
    for ref in references:
        record={'video_id':ref['video_id'],'url':ref['url'],'status':'missing_analysis'}
        file=Path(ref.get('analysis',{}).get(args.exercise,''))
        if file.is_file():
            data=json.loads(file.read_text())
            reviewed=ref.get('review',{}).get(args.exercise,{})
            if data.get('space') != 'image_2d' or data.get('interval') != reviewed.get('interval'):
                record['status']='stale_or_incompatible_analysis'
                results.append(record)
                continue
            joints=ref.get('comparison_joints',list(ANGLE_JOINTS))
            if not joints or any(j not in ANGLE_JOINTS for j in joints):
                raise ValueError('Unknown or empty comparison joint selection')
            selected=lambda rows:[{k:v for k,v in r['angles'].items() if k in joints} for r in rows]
            current_candidate=candidate
            alternate=ref.get('candidate_projection',{}).get(args.exercise)
            if alternate:
                if not Path(alternate).is_file():
                    record['status']='missing_candidate_projection';results.append(record);continue
                current_candidate=json.loads(Path(alternate).read_text())
                if current_candidate.get('space')!='image_2d':raise ValueError('Invalid reference-view projection')
                if expected_joints and current_candidate.get('source_joints_sha256')!=expected_joints:
                    record['status']='stale_candidate_projection';results.append(record);continue
                record['comparison_camera']=current_candidate.get('comparison_camera')
                record['candidate_projection_sha256']=digest(alternate)
            match=align(selected(current_candidate['frames']),selected(data['frames']))
            record.update(comparison=match,hand_frame_coverage=data.get('hand_frame_coverage'),
                          comparison_joints=joints,
                          reference_video_sha256=data.get('video_sha256'),analysis_sha256=digest(file),
                          status='screening_only' if reviewed.get('view_comparable') and match and match['path_coverage']>=.8 else 'inconclusive',
                          visual_grip_review=reviewed.get('grip','pending'))
        results.append(record)
    output={'exercise':args.exercise,'status':'requires_visual_review','candidate_sha256':digest(args.candidate),'references':results,
            'limitations':['2D camera and occlusion affect angles; no biomechanics certification.',
                          'Hand detection coverage is not grip correctness.',
                          'Review whole reps, floor/bar contacts, thumbs and encoded phone-size output.']}
    Path(args.output).write_text(json.dumps(output,indent=2)+'\n')
    print(json.dumps({'output':args.output,'references':len(results),'statuses':[r['status'] for r in results]}))


def extract_references(args):
    manifest=json.loads(Path(args.manifest).read_text())
    for ref in manifest['references']:
        for exercise in ref['exercises']:
            if getattr(args,'exercise',None) and exercise!=args.exercise:continue
            start,end=ref['review'][exercise]['interval']
            extract(argparse.Namespace(video=ref['local_video'], start=start, end=end,
                    hz=args.hz, confidence=args.confidence, pose_model=args.pose_model,
                    hand_model=args.hand_model, output=ref['analysis'][exercise]))


def main():
    parser=argparse.ArgumentParser(description=__doc__);sub=parser.add_subparsers(dest='command',required=True)
    e=sub.add_parser('extract');e.add_argument('video');e.add_argument('--start',type=float,required=True);e.add_argument('--end',type=float,required=True)
    e.add_argument('--pose-model',required=True);e.add_argument('--hand-model',required=True);e.add_argument('--hz',type=float,default=8)
    e.add_argument('--confidence',type=float,default=.7);e.add_argument('--output',required=True);e.set_defaults(run=extract)
    p=sub.add_parser('project');p.add_argument('joints');p.add_argument('--output',required=True);p.set_defaults(run=project)
    c=sub.add_parser('compare');c.add_argument('--manifest',required=True);c.add_argument('--candidate',required=True)
    c.add_argument('--joints',help='Current joints.json, to reject stale camera projections');c.add_argument('--exercise',required=True);c.add_argument('--output',required=True);c.set_defaults(run=compare)
    r=sub.add_parser('extract-references');r.add_argument('--manifest',required=True)
    r.add_argument('--pose-model',required=True);r.add_argument('--hand-model',required=True)
    r.add_argument('--exercise');r.add_argument('--hz',type=float,default=8);r.add_argument('--confidence',type=float,default=.5)
    r.set_defaults(run=extract_references)
    args=parser.parse_args()
    if hasattr(args,'hz') and (args.hz<=0 or args.hz>60 or not 0<=args.confidence<=1):parser.error('Invalid sampling or confidence')
    args.run(args)


if __name__=='__main__':
    main()
