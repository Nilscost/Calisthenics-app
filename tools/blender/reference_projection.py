"""Blender: cheap camera projections for references, without rendering pixels.

blender -b --python-exit-code 1 -P tools/blender/reference_projection.py --
  --manifest tools/blender/motion_references.json

Camera angles are manually estimated view categories, not calibrated cameras.
The output remains screening evidence and must not certify correct form.
"""
import argparse
import json
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parent))
import bpy
from bpy_extras.object_utils import world_to_camera_view
from mathutils import Vector
from render import camera,scene_setup,RES
from pose_compare import features,digest
from motion_quality import audit

parser=argparse.ArgumentParser();parser.add_argument('--manifest',required=True)
args=parser.parse_args(sys.argv[sys.argv.index('--')+1:])
manifest=json.loads(Path(args.manifest).read_text())
for ref in manifest['references']:
    for ex,view in ref.get('comparison_camera',{}).items():
        source=Path('work/clips-bakeoff')/ex/'joints.json';data=json.loads(source.read_text())
        if audit(data)['status']!='pass':raise ValueError('Cannot project failed motion: '+ex)
        interval=ref.get('candidate_interval',{}).get(ex,[0,len(data['frames'])/data['fps']])
        if not 0<=interval[0]<interval[1]<=len(data['frames'])/data['fps']:raise ValueError('Invalid candidate cycle interval')
        selected=data['frames'][round(interval[0]*data['fps']):round(interval[1]*data['fps'])]
        scene=scene_setup('WORKBENCH')
        names=('shoulder_l','shoulder_r','elbow_l','elbow_r','wrist_l','wrist_r',
               'hip_l','hip_r','knee_l','knee_r','ankle_l','ankle_r','head')
        points=[fr[name] for fr in selected for name in names]
        cam=camera(view,points);bpy.context.view_layer.update()
        frames=[]
        for i,fr in enumerate(selected):
            projected={}
            for name in names:
                v=world_to_camera_view(scene,cam,Vector(fr[name]));projected[name]=[v.x*RES[0],(1-v.y)*RES[1]]
            frames.append({'time':i/data['fps'],'points':projected,'angles':features(projected),'hands':[]})
        result={'schema':1,'space':'image_2d','source_joints_sha256':digest(source),
                'candidate_interval':interval,'comparison_camera':{**view,'method':'manual approximate view match, uncalibrated'},'frames':frames}
        output=Path(ref['candidate_projection'][ex]);output.parent.mkdir(parents=True,exist_ok=True)
        output.write_text(json.dumps(result,indent=2)+'\n');print('PROJECTED',ex,ref['video_id'],str(output))
