"""Submit explicitly named, validated Space Bend jobs. Never resubmit a started job."""
import argparse
import concurrent.futures
import datetime
import json
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
PLAN = ROOT / 'docs/assets/tripo-20261127/generation-plan.json'
RUNS = ROOT / 'assets/tripo/runs'

def run(asset):
    name = asset['id']
    marker = RUNS / (name + '.started.json')
    with marker.open('x') as f:
        json.dump({'id':name, 'time':datetime.datetime.now(datetime.timezone.utc).isoformat(),
                   'estimatedCredits':asset['validatedPlan']['generation_credits_estimate'],
                   'scope':'User requested six Space Bend objects regenerated with Tripo; configured Shatter account reused.'}, f, indent=2)
    with (RUNS/(name+'.stdout.json')).open('w') as out, (RUNS/(name+'.stderr.log')).open('w') as err:
        result = subprocess.run(asset['command']+['--yes'], cwd=ROOT,
                                env={**os.environ,'TRIPO_CLIENT_APP':'codex'}, stdout=out, stderr=err)
    payload = json.loads((RUNS/(name+'.stdout.json')).read_text())
    receipt = {'id':name,'exitCode':result.returncode,'result':payload}
    (RUNS/(name+'.receipt.json')).write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({'id':name,'exitCode':result.returncode,
                      **{k:payload.get(k) for k in ('task_id','credits_consumed','model_file','preview')}},ensure_ascii=False),flush=True)
    if result.returncode:
        raise RuntimeError(name+': CLI failed; recover the existing task, do not generate again')
    if payload.get('credits_consumed',0) > asset['validatedPlan']['generation_credits_estimate']:
        raise RuntimeError(name+': fee exceeded estimate; stop further submission')

if __name__ == '__main__':
    parser=argparse.ArgumentParser();parser.add_argument('ids',nargs='+');parser.add_argument('--workers',type=int,default=2)
    args=parser.parse_args();plan=json.loads(PLAN.read_text());RUNS.mkdir(parents=True,exist_ok=True)
    assets=[next(x for x in plan['assets'] if x['id']==name) for name in args.ids]
    assert len(set(args.ids))==len(args.ids),'Duplicate object ID'
    for asset in assets:
        assert not (RUNS/(asset['id']+'.started.json')).exists(),asset['id']+': already started; recover existing task'
        assert asset['validatedPlan']['valid'] and not asset['validatedPlan']['warnings']
    with concurrent.futures.ThreadPoolExecutor(max_workers=max(1,min(args.workers,2))) as pool:
        pending=[pool.submit(run,asset) for asset in assets]
        try:
            for job in concurrent.futures.as_completed(pending):job.result()
        except Exception:
            for job in pending:job.cancel()
            raise
