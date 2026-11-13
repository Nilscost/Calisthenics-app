"""Three appearance-only stills of the same approved pose; never installs assets."""
import argparse
import json
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parent))
import bpy
from mathutils import Vector
from render import Figure,Props,scene_setup,material,PROP,camera,ellipsoid,place_basis

root=Path(__file__).resolve().parents[2]
p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True)
args=p.parse_args(sys.argv[sys.argv.index('--')+1:]);args.output.mkdir(parents=True,exist_ok=True)
data=json.loads((root/'work/clips-bakeoff/pullup-full/joints.json').read_text());frame=data['frames'][48]
for style in ('current','studio','anatomy-cues'):
 sc=scene_setup('CYCLES');sc.render.resolution_x=640;sc.render.resolution_y=640
 figure=Figure(frame,True,data.get('bar_grip'));figure.pose(frame)
 props=Props(data['props'],material('prop',PROP),frame);props.pose(frame)
 if style!='current':
  # A continuous shaded torso with restrained material contrast. Same motion.
  for mat in bpy.data.materials:
   if mat.name.startswith(('figure','joint')):
    rgb=(.27,.35,.40)
    mat.diffuse_color=(*rgb,1)
    bsdf=mat.node_tree.nodes.get('Principled BSDF')
    bsdf.inputs['Base Color'].default_value=(*rgb,1);bsdf.inputs['Roughness'].default_value=.43
  sub=figure.torso.obj.modifiers.new('Softer torso contours','SUBSURF');sub.levels=2;sub.render_levels=2
  for ob in bpy.data.objects:
   if ob.type=='LIGHT' and ob.data.type=='AREA':ob.data.energy*=1.25
 if style=='anatomy-cues':
  # Still-only concept: fuse body surfaces to remove primitive/joint seams.
  # Animation would require a properly skinned continuous mesh, not this bake.
  names=('upper_','fore_','thigh_','shin_','knee_','elbow_','ankle_','wrist_')
  body=[ob for name,ob in figure.parts.items() if name.startswith(names) or name in ('neck','shoulders')]
  body.append(figure.torso.obj)
  bpy.ops.object.select_all(action='DESELECT')
  for ob in body:ob.select_set(True)
  bpy.context.view_layer.objects.active=figure.torso.obj
  # Bake the torso's preview subdivision before joining the other surfaces.
  bpy.ops.object.modifier_apply(modifier='Softer torso contours')
  bpy.ops.object.join();merged=bpy.context.object
  bpy.ops.object.transform_apply(location=False,rotation=True,scale=True)
  remesh=merged.modifiers.new('Continuous surface concept','REMESH');remesh.mode='VOXEL';remesh.voxel_size=.009
  bpy.ops.object.modifier_apply(modifier=remesh.name)
  smooth=merged.modifiers.new('Relax surface','SMOOTH');smooth.factor=.65;smooth.iterations=5
  bpy.ops.object.modifier_apply(modifier=smooth.name)
  sub=merged.modifiers.new('Refined body surface','SUBSURF');sub.levels=1;sub.render_levels=1
  for polygon in merged.data.polygons:polygon.use_smooth=True
 # Same close full-figure view in all variants; exclude distant bar uprights.
 points=[]
 for key in ('head','chest','pelvis','ankle_l','ankle_r','wrist_l','wrist_r'):
  pt=frame[key]
  for dx,dy,dz in ((-.13,0,0),(.13,0,0),(0,-.13,0),(0,.13,0),(0,0,-.13),(0,0,.13)):
   points.append((pt[0]+dx,pt[1]+dy,pt[2]+dz))
 camera(dict(cam_az=25,cam_el=8),points)
 sc.render.filepath=str(args.output/(style+'.png'));bpy.ops.render.render(write_still=True)
print('PASS: three same-pose appearance previews; no pose, renderer or installed asset changed')
