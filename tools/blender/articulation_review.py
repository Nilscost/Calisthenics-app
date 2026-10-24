"""Compact articulation measurements for human review, never form approval.

python tools/blender/articulation_review.py ring-muscle-up --output report.json
The hand axis is a simplified wrist-to-fingertip direction, not a measured palm
bone. Flexion, deviation and load cannot be certified from one scalar angle.
"""
import argparse
import json
import math
from pathlib import Path


def bend(a,b,c):
    u=[y-x for x,y in zip(a,b)]
    v=[y-x for x,y in zip(b,c)]
    length=math.sqrt(sum(x*x for x in u)*sum(x*x for x in v))
    if length<1e-10:return None
    return math.degrees(math.acos(max(-1,min(1,sum(x*y for x,y in zip(u,v))/length))))


def review(data):
    report={'exercise':data['id'],'status':'needs_human_review','form_approval':False,
            'meaning':'Geometry-only measurements; no physiological range-of-motion or loaded-contact approval',
            'questions':['Does the wrist bend in a plausible direction relative to the forearm and palm?',
                         'Does the grip permit this wrist position under load through every phase?',
                         'Are hand, wrist, forearm and implement contacts continuous and legible?'],
            'wrists':{}}
    for side in ('l','r'):
        values=[bend(*(f[n+'_'+side] for n in ('elbow','wrist','hand'))) for f in data['frames']]
        if any(v is None for v in values):
            report['wrists'][side]={'error':'degenerate articulation axis'};continue
        worst=max(range(len(values)),key=values.__getitem__)
        report['wrists'][side]={'min_bend_deg':round(min(values),3),'max_bend_deg':round(values[worst],3),
            'max_bend_frame':worst,'max_bend_time_s':round(worst/data['fps'],3),
            'max_frame_angle_change_deg':round(max(abs(v-values[i-1]) for i,v in enumerate(values)),3)}
    return report


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('ids',nargs='+');parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args();import poses;poses.load_families()
    reports=[review(poses.build(ex)[0]) for ex in args.ids]
    args.output.write_text(json.dumps(reports,indent=2)+'\n')
    print(json.dumps(reports))


if __name__=='__main__':main()
