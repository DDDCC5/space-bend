import json,urllib.request,time
from pathlib import Path
opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
out=Path('docs/development/codegen');out.mkdir(parents=True,exist_ok=True)
def call(name,args):
 req=urllib.request.Request('http://127.0.0.1:8766/call',json.dumps({'name':name,'arguments':args}).encode(),{'Content-Type':'application/json'})
 raw=json.load(opener.open(req,timeout=310));r=raw.get('structuredContent')
 if not r:
  for c in raw.get('content',[]):
   if c.get('type')=='text':
    try:r=json.loads(c['text']);break
    except ValueError:pass
 r=r or raw
 Path('.scratch/codegen-current.json').write_text(json.dumps(r,ensure_ascii=False,indent=2))
 (out/f'{time.time_ns()}.json').write_text(json.dumps(r,ensure_ascii=False,indent=2))
 print(name,r.get('runId'),r.get('current'),r.get('error'),flush=True)
 return r
r=call('start_3d_generation_workflow',json.load(open('.scratch/codegen-start.json')))
assert 'sceneId' in r['next']['submission']['inputSchema']['properties']
spec=json.load(open('.scratch/codegen-submit.json'))['input']
r=call(r['next']['tool'],{**r['next']['arguments'],'input':spec})
assert 'sceneEntry' in r['next']['submission']['inputSchema']['properties']
changes=json.load(open('.scratch/codegen-finish.json'))['input'];changes['changedFiles']=[str(p) for p in Path('app/src/main/java').rglob('*.kt')]
r=call(r['next']['tool'],{**r['next']['arguments'],'input':changes})
if r.get('next',{}).get('action')=='wait':r=call(r['next']['tool'],r['next']['arguments'])
print('RESULT',json.dumps(r.get('result',{}),ensure_ascii=False))
