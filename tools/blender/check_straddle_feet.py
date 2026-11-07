"""Actual rendered foot frames, floor clearance and camera fit for the press."""
import math,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent))
import bpy
from bpy_extras.object_utils import world_to_camera_view
from mathutils import Vector
import poses
from render import Figure,scene_setup,camera,foot_points
from motion_quality import audit
root=Path(__file__).resolve().parents[2];poses.load_families()
data,reach=poses.build('handstand-press-straddle')
assert not reach and not poses.checks(data)
assert audit(data)['status']=='pass',audit(data)['failures']
preview='--preview' in sys.argv
scene=scene_setup('CYCLES' if preview else 'WORKBENCH');fig=Figure(data['frames'][0])
points=[]
for f in data['frames']:
 for name,v in f.items():
  if name.endswith(('_up','_left')) or name.startswith(('palmn_','grip_','finger_lift_','foot_arch_')) or name=='head_detail':continue
  points.append(tuple(v))
points=[(p[0]+dx,p[1]+dy,max(p[2]+dz,0)) for p in set(points) for dx,dy,dz in ((0,0,0),(.12,0,0),(-.12,0,0),(0,.12,0),(0,-.12,0),(0,0,.12),(0,0,-.12))]
cam=camera(data,points);last={};minimum=math.inf;normal_dot=1;alignment=1;vertices=0;foot_vertices=0
for index,f in enumerate(data['frames']):
 fig.pose(f);bpy.context.view_layer.update()
 for side in ('l','r'):
  heel,tip=foot_points(f,side);ank=Vector(f['ankle_'+side])
  d=(Vector(f['toe_'+side])-ank).normalized()
  n=(heel-ank+d*.045).normalized()
  if side in last:normal_dot=min(normal_dot,n.dot(last[side]))
  last[side]=n;alignment=min(alignment,(tip-heel).normalized().dot(d))
 for name,ob in fig.parts.items():
  for vertex in ob.data.vertices:
   world=ob.matrix_world@vertex.co
   if name.startswith('foot_'):
    minimum=min(minimum,world.z);foot_vertices+=1
    assert world.z>=-.002,(index,name,'foot/floor intersection',world.z)
   v=world_to_camera_view(scene,cam,world)
   assert 0<v.x<1 and 0<v.y<1,(index,name,'body outside app frame',tuple(v))
   vertices+=1
 if preview and index in (0,32,48,72,96,120,144,191):
  out=root/'work/straddle-foot-refinement/phases';out.mkdir(exist_ok=True)
  scene.render.filepath=str(out/(str(index)+'.png'));bpy.ops.render.render(write_still=True)
assert normal_dot>.99,('heel frame flip',normal_dot)
assert alignment>.999,('rendered toe axis differs from source',alignment)
print(f'PASS: {vertices} actual body vertices across 192 frames fit the app camera')
print(f'PASS: {foot_vertices} actual foot vertices, minimum floor clearance {minimum*1000:.3f} mm, source/rendered toe alignment >= {alignment:.6f}, consecutive heel normal dot >= {normal_dot:.6f}')
