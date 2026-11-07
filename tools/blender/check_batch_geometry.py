"""Native all-frame body-mesh floor and final app-framing gate for isolated batches.

Run in Blender: --python check_batch_geometry.py -- --directory ABS --output ABS.
This tests geometry, never anatomical form or human load-bearing mechanics.
"""
import sys,json,math,argparse
from pathlib import Path
root=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(root/'tools/blender'))
import bpy
from bpy_extras.object_utils import world_to_camera_view
from render import Figure,Props,scene_setup,camera,material,PROP
from camera_views import camera_stamp
from motion_quality import digest
from render_batch import _pipeline_source_stamp, _atomic_json
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--directory',type=Path,required=True);parser.add_argument('--output',type=Path,required=True)
args=parser.parse_args(sys.argv[sys.argv.index('--')+1:])
batch=args.directory
manifest=json.loads((batch/'manifest.json').read_text());results={}
for ex in manifest['order']:
 data=json.loads((batch/'staged'/ex/'joints.json').read_text());data_sha=digest(data);data.update(camera_stamp(ex) or {})
 scene=scene_setup('WORKBENCH');props=Props(data['props'],material('prop',PROP),data['frames'][0])
 rings=[p for p in data['props'] if p['kind']=='ring'] if data.get('ring_grip') else None
 fig=Figure(data['frames'][0],data.get('anatomy_detail',False),data.get('bar_grip'),rings)
 points=list(props.points)
 for f in data['frames']:
  for k,v in f.items():
   if not(k.endswith(('_up','_left')) or k.startswith(('palmn_','grip_','finger_lift_','foot_arch_')) or k=='head_detail'):points.append(tuple(v))
 if data.get('camera_focus'):points=[tuple(f[n]) for f in data['frames'] for n in data['camera_focus']]
 pad=data.get('camera_padding',.12)
 points=[(p[0]+dx,p[1]+dy,max(p[2]+dz,0)) for p in set(points) for dx,dy,dz in ((0,0,0),(pad,0,0),(-pad,0,0),(0,pad,0),(0,-pad,0),(0,0,pad),(0,0,-pad))]
 cam=camera(data,points);counts=0;outside=0;minimum=math.inf;first=None;min_foot=math.inf;floor_frame=None;floor_part=None;box_hits=0;first_box_hit=None;minimum_bar_gap=math.inf;best_bar_contact=math.inf;bar_contact_frames=0
 for i,f in enumerate(data['frames']):
  fig.pose(f);props.pose(f);bpy.context.view_layer.update()
  foot_gaps={'l':math.inf,'r':math.inf}
  objects=list(fig.parts.items())
  if fig.detailed:objects.append(('torso',fig.torso.obj))
  for name,ob in objects:
   for v in ob.data.vertices:
    world=ob.matrix_world@v.co
    if world.z<minimum:minimum=world.z;floor_frame=i;floor_part=name
    if name.startswith('foot_'):min_foot=min(min_foot,world.z)
    if ex in ('toes-to-bar','heel-hook') and name in ('foot_l','foot_r'):
     bar=next(p for p in data['props'] if p['kind']=='bar')
     target=(bar.get('x',0),max(-bar['half'],min(bar['half'],world.y)),bar['z'])
     gap=math.dist(world,target)-bar.get('r',.017)
     minimum_bar_gap=min(minimum_bar_gap,gap)
     side=name[-1];foot_gaps[side]=min(foot_gaps[side],gap)
    if ex in ('depth-jump','dragon-flag-negative','handstand-wall-walk-up'):
     for solid in (p for p in data['props'] if p['kind']=='box'):
      if all(abs(world[j]-solid['center'][j])<solid['size'][j]/2-.002 for j in range(3)):
       box_hits+=1
       if first_box_hit is None:first_box_hit={'frame':i,'part':name,'point':list(world)}
    p=world_to_camera_view(scene,cam,world);counts+=1
    # Include encoder shift and inset exclusion for grip-critical candidates.
    x=p.x*480-(65 if data.get('grip_detail') else 0)
    right=283 if data.get('grip_detail') else 480
    if not(0<x<right and 0<p.y<1):
     outside+=1
     if first is None:first={'frame':i,'part':name,'x_px':round(x,3),'y':round(p.y,5)}
  if ex in ('toes-to-bar','heel-hook'):
   eligible=(min(f['ankle_l'][2],f['ankle_r'][2])>2.05 if ex=='toes-to-bar' else
             f['ankle_l'][1]<f['ankle_r'][1] and min(f['ankle_l'][2],f['ankle_r'][2])>bar['z'])
   if eligible:
    best_bar_contact=min(best_bar_contact,max(foot_gaps.values()))
    if max(foot_gaps.values())<=.006:bar_contact_frames+=1
  if i%48==0:print(ex,i,flush=True)
 result={'id':ex,'data_sha256':data_sha,'minimum_floor_frame':floor_frame,'minimum_floor_part':floor_part,'frames':len(data['frames']),'vertices':counts,'outside_vertices':outside,'first_outside':first,'minimum_body_z_m':minimum,'minimum_foot_z_m':min_foot}
 result.update(box_interior_vertices=box_hits,first_box_hit=first_box_hit)
 bar_ok=True
 if ex in ('toes-to-bar','heel-hook'):
  result.update(minimum_foot_bar_skin_gap_m=minimum_bar_gap,best_bilateral_phase_skin_gap_m=best_bar_contact,bilateral_contact_frames=bar_contact_frames)
  bar_ok=minimum_bar_gap>=-.002 and best_bar_contact<=.006 and bar_contact_frames>=1
 result['status']='pass' if outside==0 and minimum>=-.002 and box_hits==0 and bar_ok else 'fail'
 results[ex]=result;print(json.dumps(result),flush=True)
report={'status':'pass' if all(r['status']=='pass' for r in results.values()) else 'fail',
        'pipeline_source_stamp':_pipeline_source_stamp(),'items':results,
        'meaning':'Actual all-frame body vertices, floor tolerance 2mm and final encoded framing. Not form approval.'}
_atomic_json(args.output,report)
print(report['status'].upper()+': native batch geometry; '+str(sum(r['vertices'] for r in results.values()))+' vertices',flush=True)
if report['status']!='pass':raise RuntimeError('Native batch geometry failed; inspect '+str(args.output))
