"""Check actual rendered Burpee foot skin throughout the planted push-up.

Run with Blender --background --python tools/check_burpee_toe_contact.py --
--input /absolute/joints.json --output /absolute/contact-report.json.
This separate review gate does not certify form or human load bearing.
"""
import sys,json,math
from pathlib import Path
r=Path(__file__).resolve().parents[1];sys.path.insert(0,str(r/'tools/blender'))
import bpy
from render import Figure,scene_setup
from generation_checks.burpee_articulation import _plank_mask
import argparse
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--input',type=Path,required=True);parser.add_argument('--output',type=Path,required=True)
args=parser.parse_args(sys.argv[sys.argv.index('--')+1:]);data=json.loads(args.input.read_text())
if data.get('id')!='burpee-pushup':raise ValueError('Only the authored burpee variant is scoped')
scene_setup('WORKBENCH');fig=Figure(data['frames'][0],data.get('anatomy_detail',False),data.get('bar_grip'))
rows=[]
for i,planted in enumerate(_plank_mask(data['frames'])):
 if not planted:continue
 fig.pose(data['frames'][i]);bpy.context.view_layer.update()
 for side in ['l','r']:
  ob=fig.parts['foot_'+side];zs=[(ob.matrix_world@v.co).z for v in ob.data.vertices];rows.append({'frame':i,'side':side,'minimum_skin_z_m':min(zs)})
report={'status':'pass' if rows and all(-.002<=x['minimum_skin_z_m']<=.006 for x in rows) else 'fail','samples':len(rows),'min_skin_z_m':min(x['minimum_skin_z_m'] for x in rows),'max_skin_z_m':max(x['minimum_skin_z_m'] for x in rows),'tolerance_m':[-.002,.006],'meaning':'Actual rendered foot mesh during planted press; engineering contact check, not load-bearing or form approval.'}
args.output.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if report['status']!='pass':raise RuntimeError('toe contact failed')
