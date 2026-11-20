"""Run in Blender: check the actual rendered hand meshes against the bar."""
from pathlib import Path
import math
import sys
sys.path.insert(0,str(Path(__file__).resolve().parent))
import bpy
import poses
from render import Figure,hand_segments,scene_setup
from bar_grip import grip_clearance,segment_clearance

poses.load_families();data,problems=poses.build('pullup-full');assert not problems
baseline=[]
for side in ('l','r'):
    segments=hand_segments(data['frames'][0],side)[-1]
    baseline.extend(segment_clearance(a,b,data['bar_grip'],.0105 if i==len(segments)-1 else .0088)
                    for i,(a,b) in enumerate(segments))
assert min(baseline)<0,'Previous generic grip must reproduce the reported intersection'
print(f'REPRODUCED: previous generic fingers penetrated bar by {-min(baseline)*1000:.3f} mm')
scene_setup('WORKBENCH');figure=Figure(data['frames'][0],True,data['bar_grip'])
minimum=math.inf;checked=0
for frame in data['frames']:
    figure.pose(frame);bpy.context.view_layer.update()
    for side in ('l','r'):
        _,_,_,_,segments=hand_segments(frame,side,data['bar_grip'])
        for i,(a,b) in enumerate(segments):
            obj=figure.parts[f'fing_{side}_{i}']
            # No hidden discrepancy between analytic segments and rendered capsules.
            assert (obj.location-a).length<1e-7
            axis=obj.rotation_quaternion @ __import__('mathutils').Vector((0,0,1))
            assert axis.dot((b-a).normalized())>.99999
        names=[n for n in figure.parts if n.startswith('fing_'+side+'_')]
        names += [prefix+side for prefix in ('palm_','palm_pad_','hand_back_','wrist_') if prefix+side in figure.parts]
        names += ['wrist_'+side+'_ball']
        for name in names:
            obj=figure.parts[name]
            for vertex in obj.data.vertices:
                p=obj.matrix_world @ vertex.co
                clearance=math.hypot(p.x,p.z-data['bar_grip']['z'])-data['bar_grip']['r']
                minimum=min(minimum,clearance);checked+=1
                assert clearance>=.0004,(name,'bar intersection',clearance)
assert grip_clearance(data)>.0005
print(f'PASS: {checked} actual hand-mesh vertices across 96 frames; minimum vertex clearance {minimum*1000:.3f} mm; conservative capsule clearance {grip_clearance(data)*1000:.3f} mm')
