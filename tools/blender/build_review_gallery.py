"""Build a self-contained eight-video review fragment from freshly screened bytes.

python tools/blender/build_review_gallery.py --screen work/catalog-screening/screen.json --output /absolute/task-owned/complex-movement-review.html
No video re-encoding, generation APIs, source pose changes or form approval.
"""
import argparse
import base64
import hashlib
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]


def build(screen_path,output):
    manifest=json.loads(Path(__file__).with_name('complex_review_set.json').read_text())
    catalog={r['id']:r for r in json.loads((ROOT/'app/src/main/assets/catalog.json').read_text())['variations']}
    records={r['id']:r for r in json.loads(screen_path.read_text())['records']};clips=[]
    for entry in manifest['clips']:
        ex=entry['id'];record=records[ex];path=ROOT/'app/src/main/assets/demos'/(ex+'.mp4');raw=path.read_bytes()
        if hashlib.sha256(raw).hexdigest()!=record['media']['sha256']:
            raise ValueError(ex+': installed video changed after screening; rerun catalog_screen.py')
        kinds={f['check'] for f in record['flags']}
        prompt=('Fast movement and a large loop transition need review.' if {'high_joint_speed','large_loop_step'}<=kinds
                else 'A large loop transition needs review.' if 'large_loop_step' in kinds
                else 'Fast joint movement needs review; it may be intentional.' if 'high_joint_speed' in kinds
                else 'No generic timing flag; movement form still needs review.')
        clips.append({**entry,'name':catalog[ex]['name'],'duration_s':record['media']['duration_s'],
                      'screening_prompt':prompt,'form_status':'not_approved',
                      'video':'data:video/mp4;base64,'+base64.b64encode(raw).decode()})
    template=Path(__file__).with_name('review_gallery.html').read_text()
    assert template.count('__CLIPS__')==1
    html=template.replace('__CLIPS__',json.dumps(clips,separators=(',',':')).replace('</','<\\/'))
    if len(html.encode())>=1_000_000:raise ValueError('Review fragment exceeds 1 MB; reduce selection')
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(html)
    return {'clips':len(clips),'bytes':len(html.encode()),'output':str(output)}


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--screen',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
    print(json.dumps(build(a.screen,a.output)))

if __name__=='__main__':main()
