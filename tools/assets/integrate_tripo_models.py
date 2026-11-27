"""Install reviewed, normalized Tripo GLBs and export sanitized provenance + previews."""
from pathlib import Path
import hashlib,json,shutil,struct
from PIL import Image, ImageDraw, ImageFont
ROOT=Path(__file__).resolve().parents[2]
PLAN=ROOT/'docs/assets/tripo-20261127/generation-plan.json'
plan=json.loads(PLAN.read_text());reviews=json.loads((PLAN.parent/'visual-review.json').read_text())
objects=[];bounds={};credits=0
for item in plan['assets']:
    name=item['id'];record=json.loads((ROOT/f'assets/tripo/normalized/{name}.json').read_text())
    receipt=json.loads((ROOT/f'assets/tripo/runs/{name}.stdout.json').read_text())
    assert reviews[name]['geometry']=='accepted' and reviews[name]['materials']=='accepted'
    assert receipt['status']=='success' and receipt['credits_consumed']<=item['validatedPlan']['generation_credits_estimate']
    source=ROOT/record['preparedFile'];raw=source.read_bytes()
    assert hashlib.sha256(raw).hexdigest()==record['sha256']
    n=struct.unpack_from('<I',raw,12)[0];gltf=json.loads(raw[20:20+n])
    assert len(gltf.get('textures',[]))>=3
    assert all('TANGENT' in p['attributes'] for m in gltf['meshes'] for p in m['primitives'])
    assert record['normalizedRadius']<record['collisionRadius']
    for folder in ('assets/style-a/models','app/src/main/assets/models'):
        shutil.copy2(source,ROOT/f'{folder}/{name}.glb')
    bounds[name]=record['bounds'];credits+=receipt['credits_consumed']
    objects.append({**record,'name':item['displayName'],'file':f'models/{name}.glb','visualReview':reviews[name]})
assert len(objects)==6 and credits==360
(ROOT/'app/src/main/assets/model-bounds.json').write_text(json.dumps(bounds,indent=2)+'\n')
catalog={'status':'integrated_pending_user_visual_feedback','source':'Official Tripo CLI 0.11.4 / H3.1 v3.1-20260211 / detailed geometry + PBR','units':'meters','upAxis':'Y','forward':'-Z for directional assets','creditsConsumed':credits,'objects':objects}
(ROOT/'assets/style-a/models/catalog.json').write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n')
(PLAN.parent/'delivery-manifest.json').write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n')
rows=[{'id':o['id'],'url':'/assets/style-a/models/'+o['id']+'.glb'} for o in objects]
(ROOT/'assets/tripo/review/models.json').write_text(json.dumps(rows,indent=2)+'\n')
(ROOT/'assets/style-a/models-preview.html').write_text('<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta http-equiv="refresh" content="0;url=../tripo/review/index.html"><title>Space Bend Tripo 模型</title><a href="../tripo/review/index.html">查看六种 Tripo PBR 模型</a></html>\n')
font_path=next(p for p in [Path('/System/Library/Fonts/PingFang.ttc'),Path('/System/Library/Fonts/Supplemental/Arial Unicode.ttf')] if p.exists())
font=lambda size:ImageFont.truetype(str(font_path),size)
sheet=Image.new('RGB',(1560,1270),'#0b121a');draw=ImageDraw.Draw(sheet)
draw.text((48,30),'SPACE BEND / GRAVITY LAB',font=font(24),fill='#d5ac61')
draw.text((48,66),'空间折跃 · Tripo 物件重制',font=font(40),fill='#edf5f8')
draw.text((48,124),'六种实际生成模型 · PBR 材质 · Tripo 3.1 · 360 积分',font=font(22),fill='#9bb2c0')
for i,item in enumerate(plan['assets']):
    o=objects[i];x=36+(i%3)*508;y=180+(i//3)*526
    draw.rectangle((x,y,x+488,y+504),fill='#111e29',outline='#38505f',width=2)
    receipt=json.loads((ROOT/f'assets/tripo/runs/{item["id"]}.stdout.json').read_text())
    im=Image.open(ROOT/receipt['preview']).convert('RGBA');im.thumbnail((450,410),Image.Resampling.LANCZOS)
    sheet.paste(im,(x+(488-im.width)//2,y+8+(410-im.height)//2),im)
    draw.text((x+24,y+427),item['displayName'],font=font(28),fill='#ecf2f5')
    draw.text((x+24,y+471),f'{o["triangles"]:,} 三角面 / PBR',font=font(19),fill='#98adba')
sheet.save(ROOT/'assets/tripo/review/contact-sheet.png')
shutil.copy2(ROOT/'assets/tripo/review/contact-sheet.png',ROOT/'assets/style-a/previews/models-board.png')
# Keep a portable candidate bundle without credentials or provider download URLs.
import zipfile
with zipfile.ZipFile(ROOT/'output/SpaceBend-Tripo-models.zip','w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
    for o in objects:z.write(ROOT/'assets/style-a'/o['file'],o['id']+'.glb')
    z.write(PLAN.parent/'delivery-manifest.json','delivery-manifest.json')
    z.write(ROOT/'assets/tripo/review/contact-sheet.png','contact-sheet.png')
print(json.dumps({'models':len(objects),'creditsConsumed':credits,'normalizedTotalBytes':sum(x['normalizedBytes'] for x in objects)},ensure_ascii=False))
