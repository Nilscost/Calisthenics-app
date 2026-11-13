"""Batch motion gate; compact reports and failure-only sheets save review tokens.

python tools/blender/motion_review.py --output work/motion-quality-pilot/audit.json
python tools/blender/motion_review.py --all --output work/motion-audit.json --failure-sheets work/motion-failures
Use --write to stage only passing scoped poses for Blender. Unscoped never means
approved. This command does not download references, render or install assets.
"""
import argparse
import json
from pathlib import Path
import time
import poses
from motion_quality import audit,CONTRACT,ROOT


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('ids',nargs='*')
    parser.add_argument('--all',action='store_true');parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--failure-sheets',type=Path);parser.add_argument('--write',action='store_true')
    args=parser.parse_args();poses.load_families();contract=json.loads(CONTRACT.read_text())
    if args.all and args.ids:parser.error('Use IDs or --all')
    ids=sorted(poses.EXERCISES) if args.all else args.ids or list(contract['exercises'])
    unknown=set(ids)-poses.EXERCISES.keys()
    if unknown:parser.error('Unknown IDs: '+str(sorted(unknown)))
    start=time.monotonic();reports=[];staged=[]
    for ex in ids:
        if ex not in contract['exercises']:
            reports.append({'exercise':ex,'status':'unscoped','failures':[]});continue
        data,reach=poses.build(ex);report=audit(data,contract);geometry=reach+poses.checks(data)
        if geometry:report['status']='fail';report['failures'].append({'check':'geometry','problems':geometry})
        if report['status']=='fail' and args.failure_sheets:
            import preview_sheet
            selected=sorted({v['frame'] for v in report['failures'] if v.get('frame') is not None})[:6]
            moments=[i/len(data['frames']) for i in selected] or (0,.25,.5,.75)
            args.failure_sheets.mkdir(parents=True,exist_ok=True)
            output=args.failure_sheets/(ex+'.png');preview_sheet.sheet(data,str(output),moments=moments)
            report['failure_sheet']=str(output)
        if args.write and report['status']=='pass':
            output=ROOT/'work/clips-bakeoff'/ex/'joints.json';output.parent.mkdir(parents=True,exist_ok=True)
            # Do not invalidate all cached frames if source data is identical.
            if not output.is_file() or json.loads(output.read_text())!=data:
                tmp=output.with_name('joints.staging.json');tmp.write_text(json.dumps(data));tmp.replace(output);staged.append(ex)
        reports.append(report)
    summary={status:sum(r['status']==status for r in reports) for status in ('pass','fail','unscoped')}
    result={'summary':summary,'seconds':round(time.monotonic()-start,3),'staged':staged,'reports':reports,
            'scope':'Motion screening only; unscoped exercises have no movement-identity contract'}
    args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({'summary':summary,'seconds':result['seconds'],'staged':staged,'report':str(args.output)}))
    for r in reports:
        if r['status']=='fail':print(r['exercise']+': '+', '.join(v['check'] for v in r['failures']))
    return int(summary['fail']>0)


if __name__=='__main__':raise SystemExit(main())
