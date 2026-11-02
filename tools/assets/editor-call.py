import json,sys,urllib.request
name=sys.argv[1]
args=json.load(open(sys.argv[2])) if len(sys.argv)>2 else {}
data=json.dumps({'name':name,'arguments':args}).encode()
req=urllib.request.Request('http://127.0.0.1:8766/call',data,{'Content-Type':'application/json'})
raw=json.load(urllib.request.build_opener(urllib.request.ProxyHandler({})).open(req,timeout=310))
json.dump(raw,open('docs/assets/editor-last-raw.json','w'),ensure_ascii=False,indent=2)
r=raw.get('structuredContent')
if not r:
 for c in raw.get('content',[]):
  if c.get('type')=='text':
   try:r=json.loads(c['text']);break
   except ValueError:pass
r=r or raw
json.dump(r,open('docs/assets/editor-state.json','w'),ensure_ascii=False,indent=2)
print(json.dumps(r,ensure_ascii=False,indent=2))
