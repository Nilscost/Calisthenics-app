"""Blender integration check for the actual pilot torso mesh and app framing."""
import json
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parent))
import bpy
from bpy_extras.object_utils import world_to_camera_view
from mathutils import Vector
import poses
from anatomy import Torso,vertices
from render import scene_setup,material,FIGURE,Props,PROP,camera

poses.load_families();checked=0
for ex in ('pushup-archer','pullup-full'):
    data,problems=poses.build(ex);assert not problems and not poses.checks(data)
    scene=scene_setup('WORKBENCH');props=Props(data['props'],material('prop',PROP),data['frames'][0])
    points=list(props.points)
    for fr in data['frames']:
        for name,v in fr.items():
            if not(name.endswith(('_up','_left')) or name.startswith(('palmn_','grip_','finger_lift_','foot_arch_')) or name=='head_detail'):points.append(tuple(v))
    points=[(p[0]+dx,p[1]+dy,max(p[2]+dz,0)) for p in set(points)
            for dx,dy,dz in ((0,0,0),(.12,0,0),(-.12,0,0),(0,.12,0),(0,-.12,0),(0,0,.12),(0,0,-.12))]
    cam=camera(data,points);bpy.context.view_layer.update();torso=Torso(data['frames'][0],material('figure',FIGURE))
    edge_use={}
    for poly in torso.obj.data.polygons:
        vs=list(poly.vertices)
        for a,b in zip(vs,vs[1:]+vs[:1]):
            edge=tuple(sorted((a,b)));edge_use[edge]=edge_use.get(edge,0)+1
    assert set(edge_use.values())=={2},'Torso mesh has an open/nonmanifold edge'
    for fr in data['frames']:
        torso.pose(fr)
        for vertex in vertices(fr):
            assert vertex.z>=-.002,(ex,'torso intersects floor',vertex.z)
            v=world_to_camera_view(scene,cam,vertex)
            x=v.x*480-(65 if data.get('grip_detail') else 0)
            assert 0<x<(283 if data.get('grip_detail') else 480),(ex,'body clipped or hidden by inset',x)
            assert 0<v.y<1,(ex,'body outside frame',v.y)
            checked+=1
print(f'PASS: {checked} torso vertices across all 240 pilot frames: closed mesh, no torso/floor penetration, app framing and inset clearance')
