"""Actual Blender mesh/bar and framing checks; optional selected Cycles phases."""
import json,math,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent))
import bpy
from bpy_extras.object_utils import world_to_camera_view
import poses
from render import Figure,Props,scene_setup,material,PROP,camera
from bar_grip import grip_clearance
from motion_quality import audit
root=Path(__file__).resolve().parents[2]
poses.load_families();data,reach=poses.build('pullup-pullover')
assert not reach and not poses.checks(data)
assert audit(data)['status']=='pass',audit(data)['failures']
preview='--preview' in sys.argv
scene=scene_setup('CYCLES' if preview else 'WORKBENCH')
props=Props(data['props'],material('prop',PROP),data['frames'][0])
fig=Figure(data['frames'][0],True,data['bar_grip'])
points=list(props.points)
for f in data['frames']:
 for name,v in f.items():
  if name.endswith(('_up','_left')) or name.startswith(('palmn_','grip_','finger_lift_','foot_arch_')) or name=='head_detail':continue
  points.append(tuple(v))
points=[(p[0]+dx,p[1]+dy,max(p[2]+dz,0)) for p in set(points) for dx,dy,dz in ((0,0,0),(.12,0,0),(-.12,0,0),(0,.12,0),(0,-.12,0),(0,0,.12),(0,0,-.12))]
cam=camera(data,points);minimum=math.inf;checked=0;hand_min=math.inf
for index,f in enumerate(data['frames']):
 fig.pose(f);bpy.context.view_layer.update()
 for name,ob in list(fig.parts.items())+[('torso',fig.torso.obj)]:
  for vertex in ob.data.vertices:
   world=ob.matrix_world@vertex.co
   gap=math.hypot(world.x,world.z-2.3)-.017
   minimum=min(minimum,gap)
   if name.startswith(('fing_','palm_','hand_back_','wrist_')):hand_min=min(hand_min,gap)
   assert gap>.0004,(index,name,'actual mesh/bar intersection',gap)
   v=world_to_camera_view(scene,cam,world)
   assert 0<v.x<1 and 0<v.y<1,(index,name,'clipped mesh',tuple(v))
   checked+=1
 if preview and index in (0,32,48,64,80,96,116,138,164,190,224,263):
  out=root/'work/pullover-refinement/phases';out.mkdir(exist_ok=True)
  scene.render.filepath=str(out/(str(index)+'.png'));bpy.ops.render.render(write_still=True)
print(f'PASS: {checked} actual body/hand mesh vertices over {len(data["frames"])} frames, minimum bar clearance {minimum*1000:.3f} mm, hand clearance {hand_min*1000:.3f} mm, complete app framing')
print(f'PASS: conservative whole-finger capsule bar clearance {grip_clearance(data)*1000:.3f} mm')
