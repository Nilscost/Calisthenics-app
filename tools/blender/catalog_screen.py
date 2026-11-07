"""Read-only catalog screening. Technical evidence and review flags, never form approval.

python tools/blender/catalog_screen.py --output work/catalog-screening/screen.json
Every current source trajectory is built; every installed MP4 is fully decoded.
No source poses, shipped media, catalog review states or render caches are changed.
"""
import argparse
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
import math
from pathlib import Path
import subprocess
import time

import poses
from motion_quality import audit
ROOT=Path(__file__).resolve().parents[2]
JOINTS=('pelvis','spine','chest','neck','head')+tuple(n+'_'+s for n in ('hip','knee','ankle','toe','shoulder','elbow','wrist','hand') for s in ('l','r'))


def trajectory(data, reach):
    frames=data['frames'];fps=data['fps'];flags=[]
    if not frames or fps<=0 or any(not math.isfinite(v) for f in frames for key in JOINTS for v in f[key]):
        return {'flags':[{'check':'invalid_geometry','severity':'error'}]}
    speeds=[(math.dist(frames[i][j],frames[i-1][j])*fps,i,j) for i in range(1,len(frames)) for j in JOINTS]
    maximum,frame,joint=max(speeds,default=(0,0,''))
    seam,joint_seam=max((math.dist(frames[-1][j],frames[0][j]),j) for j in JOINTS)
    # Heuristics flag candidates; jumps/ballistic movements can legitimately be fast.
    if maximum>3:flags.append({'check':'high_joint_speed','severity':'review','frame':frame,'joint':joint,'value_m_s':round(maximum,4)})
    if seam>.04:flags.append({'check':'large_loop_step','severity':'review','joint':joint_seam,'value_m':round(seam,4)})
    if reach:flags.append({'check':'unreachable_authored_targets','severity':'review','messages':reach})
    floor=poses.checks(data)
    if floor:flags.append({'check':'possible_floor_intersection','severity':'review','messages':floor})
    return {'frames':len(frames),'fps':fps,'max_joint_speed_m_s':round(maximum,4),'max_speed_frame':frame,
            'loop_step_m':round(seam,4),'flags':flags,
            'limitation':'Source-joint checks do not establish contacts, full mesh collisions, exercise identity or installed animation equivalence.'}


def media(path):
    import cv2
    if not path.is_file():return {'flags':[{'check':'missing_video','severity':'error'}]}
    info=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-show_format','-of','json',str(path)],text=True))
    streams=info['streams'];video=[s for s in streams if s['codec_type']=='video'];flags=[]
    if len(video)!=1:return {'flags':[{'check':'invalid_video_stream_count','severity':'error'}]}
    v=video[0]
    if (v.get('codec_name'),v.get('profile'),v.get('pix_fmt'),v.get('width'),v.get('height'),v.get('r_frame_rate'))!=('h264','Main','yuv420p',480,360,'24/1'):
        flags.append({'check':'unexpected_video_format','severity':'error'})
    if any(s['codec_type']=='audio' for s in streams):flags.append({'check':'unexpected_audio','severity':'error'})
    if path.stat().st_size>120*1024:flags.append({'check':'byte_cap_exceeded','severity':'error'})
    cap=cv2.VideoCapture(str(path));decoded=0;previous=None;first=None;max_step=0.;step_frame=0
    while True:
        ok,fr=cap.read()
        if not ok:break
        gray=cv2.cvtColor(cv2.resize(fr,(120,90)),cv2.COLOR_BGR2GRAY)
        if first is None:first=gray
        if previous is not None:
            step=float(cv2.absdiff(gray,previous).mean())
            if step>max_step:max_step,step_frame=step,decoded
        previous=gray;decoded+=1
    cap.release()
    expected=int(v.get('nb_frames',0))
    if not decoded or expected and expected!=decoded:flags.append({'check':'incomplete_decode','severity':'error','expected':expected,'decoded':decoded})
    return {'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'bytes':path.stat().st_size,'decoded_frames':decoded,
            'duration_s':round(float(info['format']['duration']),4),'max_pixel_step':round(max_step,4),'max_step_frame':step_frame,
            'loop_pixel_step':round(float(cv2.absdiff(first,previous).mean()),4) if decoded else None,'flags':flags}


def screen(entry):
    ex=entry['id'];record={'id':ex,'name':entry['name'],'source_status':'not_built','form_status':'not_approved'}
    try:
        data,reach=poses.build(ex);record['source_status']='built';record['trajectory']=trajectory(data,reach)
        record['motion_contract']=audit(data)['status']
        record['source_module']=str(Path(poses.EXERCISES[ex].__code__.co_filename).resolve().relative_to(ROOT))
        stage=ROOT/'work/clips-bakeoff'/ex/'joints.json'
        record['current_source_matches_staged_joints']=json.loads(stage.read_text())==data if stage.is_file() else None
        record['media']=media(ROOT/'app/src/main/assets/demos'/(ex+'.mp4'))
        flags=record['trajectory']['flags']+record['media']['flags']
        if record['current_source_matches_staged_joints'] is False:flags.append({'check':'source_differs_from_staged_joints','severity':'review'})
        if record['motion_contract']=='fail':flags.append({'check':'motion_contract_failed','severity':'review'})
        record['flags']=flags;record['technical_status']='error' if any(f['severity']=='error' for f in flags) else 'review' if flags else 'no_flags'
    except Exception as e:
        record.update(technical_status='error',flags=[{'check':'screen_exception','severity':'error','detail':str(e)}])
    return record


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);p.add_argument('--workers',type=int,default=4);args=p.parse_args()
    poses.load_families();entries=json.loads((ROOT/'app/src/main/assets/catalog.json').read_text())['variations'];start=time.monotonic()
    with ThreadPoolExecutor(max_workers=args.workers) as pool:records=list(pool.map(screen,sorted(entries,key=lambda v:v['id'])))
    counts=Counter(r['technical_status'] for r in records);checks=Counter(f['check'] for r in records for f in r['flags'])
    result={'schema':1,'scope':'Technical screening only. No clip is approved for exercise form by this report.',
            'seconds':round(time.monotonic()-start,2),'total':len(records),'technical_status_counts':dict(counts),'flag_counts':dict(checks),
            'motion_contract_counts':dict(Counter(r.get('motion_contract','not_run') for r in records)),
            'not_checked':['reference agreement for remaining movements','full mesh collisions','intended contact anchors per exercise','expert form approval','phone playback'],
            'records':records}
    args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({k:result[k] for k in ('total','seconds','technical_status_counts','flag_counts','motion_contract_counts')}))
    return int(counts['error']>0)

if __name__=='__main__':raise SystemExit(main())
