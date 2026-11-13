"""Real Blender checks for ring contact/visibility; optional selected phase renders."""
import json,math,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent))
import bpy
from mathutils import Vector
from bpy_extras.object_utils import world_to_camera_view
import poses
from render import Figure,Props,scene_setup,material,PROP,camera,LIMBS
from ring_geometry import ring_frame,ring_outline,hand_segments,torus_clearance,capsule_ring_clearance
root=Path(__file__).resolve().parents[2];poses.load_families();data,reach=poses.build('ring-muscle-up')
assert not reach and not poses.checks(data)
specs=data['props'];preview='--preview' in sys.argv
scene=scene_setup('CYCLES' if preview else 'WORKBENCH')
props=Props(specs,material('prop',PROP),data['frames'][0]);fig=Figure(data['frames'][0],True,None,specs)
points=list(props.points)
for f in data['frames']:
 for name,v in f.items():
  if name.endswith(('_up','_left')) or name.startswith(('palmn_','grip_','finger_lift_','foot_arch_')) or name=='head_detail':continue
  points.append(tuple(v))
points=[(p[0]+dx,p[1]+dy,max(p[2]+dz,0)) for p in set(points) for dx,dy,dz in ((0,0,0),(.12,0,0),(-.12,0,0),(0,.12,0),(0,-.12,0),(0,0,.12),(0,0,-.12))]
cam=camera(data,points);minimum=math.inf;capsule_min=math.inf;vertices=0;widths=[];body_min=math.inf;arm_min=math.inf
rigged=all(p.get('hand_model')=='rigid-phalanx-v1' for p in specs)
mesh_lengths={};worst_finger_contact=0.;worst_heel_contact=0.
for index,f in enumerate(data['frames']):
 fig.pose(f);props.pose(f);bpy.context.view_layer.update()
 for spec in specs:
  side=spec['joint'][-1]
  ring=next(obs[0] for kind,obs,pr in props.dynamic if kind=='ring' and pr['joint']==spec['joint'])
  projected=[world_to_camera_view(scene,cam,ring.matrix_world@v.co) for v in ring.data.vertices]
  assert all(0<p.x<1 and 0<p.y<1 for p in projected)
  width=(max(p.x for p in projected)-min(p.x for p in projected))*480;widths.append(width)
  height=(max(p.y for p in projected)-min(p.y for p in projected))*360
  assert width>=1.5 and height>=18,(index,'ring too small',width,height)
  for name,(start,end,r1,r2) in LIMBS.items():
   if name.startswith(('upper_','fore_')):
    bound=capsule_ring_clearance(f[start],f[end],r1,r2,f,spec)
    arm_min=min(arm_min,bound)
    assert bound>.0005,(index,name,'arm volume intersects ring',bound)
  # Every finger capsule, including thickness, checked along its centre line.
  for i,(a,b) in enumerate(hand_segments(f,side,spec)):
   radius=.0105 if i>=12 else .0088
   sampled=[]
   for t in range(65):
    point=[x+(y-x)*t/64 for x,y in zip(a,b)]
    sampled.append(torus_clearance(point,f,spec)-radius)
   # Distance to a surface is 1-Lipschitz: cover the unsampled half-step too.
   bound=min(sampled)-math.dist(a,b)/128
   capsule_min=min(capsule_min,bound)
   assert bound>.0005,(index,'finger/ring overlap')
  finger_contacts={j:math.inf for j in range(5)};heel_contact=math.inf
  for name,ob in fig.parts.items():
   if name.startswith('fing_'+side+'_') or name in ('palm_'+side,'palm_pad_'+side,'hand_back_'+side,'wrist_'+side+'_ball'):
    if rigged and name.startswith('fing_'+side+'_'):
     segment=int(name.rsplit('_',1)[1]);radius=.0105 if segment>=12 else .0088
     length=max(v.co.z for v in ob.data.vertices)-radius*.92
     mesh_lengths.setdefault(name,[]).append(length)
    for v in ob.data.vertices:
     distance=torus_clearance(ob.matrix_world@v.co,f,spec)
     minimum=min(minimum,distance);vertices+=1
     if name.startswith('fing_'+side+'_'):
      segment=int(name.rsplit('_',1)[1]);finger=min(segment//3,4)
      finger_contacts[finger]=min(finger_contacts[finger],distance)
     elif name in ('palm_'+side,'palm_pad_'+side,'wrist_'+side+'_ball'):
      heel_contact=min(heel_contact,distance)
     assert distance>=.0004,(index,name,'hand mesh/ring overlap',distance)
  if rigged:
   worst_finger_contact=max(worst_finger_contact,max(finger_contacts.values()))
   worst_heel_contact=max(worst_heel_contact,heel_contact)
   assert max(finger_contacts.values())<=.004,(index,side,'floating finger grip',finger_contacts)
   assert heel_contact<=.005,(index,side,'floating palm/wrist heel',heel_contact)
 for spec in specs:
  objects=list(fig.parts.values())+[fig.torso.obj]
  for ob in objects:
   if ob.name.startswith(('fing_','palm_','hand_back_','wrist_')):continue
   for vertex in ob.data.vertices:
    clearance=torus_clearance(ob.matrix_world@vertex.co,f,spec)
    body_min=min(body_min,clearance)
    assert clearance>=.0004,(index,ob.name,'body/ring intersection',clearance)
 if preview and index in {0,round(.14*len(data['frames'])),round(.25*len(data['frames'])),round(.31*len(data['frames'])),round(.40*len(data['frames'])),round(.50*len(data['frames'])),round(.70*len(data['frames'])),len(data['frames'])-1}:
  out=Path(sys.argv[sys.argv.index('--preview-output')+1]) if '--preview-output' in sys.argv else root/'work/ring-muscle-up-review/phases'
  out.mkdir(parents=True,exist_ok=True)
  scene.render.filepath=str(out/(str(index)+'.png'));bpy.ops.render.render(write_still=True)
print(f'PASS: conservative all-frame arm volume clearance {arm_min*1000:.3f} mm')
print(f'PASS: conservative whole-capsule clearance {capsule_min*1000:.3f} mm; all-frame body vertex clearance {body_min*1000:.3f} mm')
print(f'PASS: {vertices} actual hand vertices, {len(data["frames"])} frames, minimum mesh/ring clearance {minimum*1000:.3f} mm, projected ring width >= {min(widths):.1f} px at 480x360')

if rigged:
 variation=max(max(v)-min(v) for v in mesh_lengths.values())
 assert variation<=.0001,('actual finger mesh bone lengths vary',variation)
 print(f'PASS: native rigid finger length variation {variation*1000:.3f} mm; worst finger skin proximity {worst_finger_contact*1000:.3f} mm; worst palm/heel proximity {worst_heel_contact*1000:.3f} mm')
