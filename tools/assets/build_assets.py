# -*- coding: utf-8 -*-
from pathlib import Path
import json,math,wave,array,random,html,subprocess
ROOT=Path(__file__).resolve().parents[2]; OUT=ROOT/'assets/style-a'
for d in ['ui','locales','audio/sfx','audio/voice','previews','vfx']: (OUT/d).mkdir(parents=True,exist_ok=True)
# Parallel key sets are the source of all localized artwork and narration.
rows=[
('loop','闭环判定','閉環判定','Loop check'),
('app','空间折跃','空間折躍','Space Bend'),
('lab','引力实验室','引力實驗室','Gravity Laboratory'),
('challenge','关卡挑战','關卡挑戰','Mission challenges'),
('sandbox','自由创作','自由創作','Free creation'),
('tutorial','新手训练','新手訓練','Flight training'),
('settings','设置','設定','Settings'),
('start','开始实验','開始實驗','Begin experiment'),
('back','返回','返回','Back'),
('continue','继续','繼續','Continue'),
('reset','重置关卡','重設關卡','Reset mission'),
('close','关闭','關閉','Close'),
('confirm','确认','確認','Confirm'),
('cancel','取消','取消','Cancel'),
('select','选择物体','選擇物體','Select payload'),
('place','确认放置','確認放置','Place payload'),
('scan','空间扫描','空間掃描','Room scan'),
('scanning','正在识别真实平面','正在辨識真實平面','Detecting real surfaces'),
('scan_ready','空间扫描完成','空間掃描完成','Room scan complete'),
('scan_retry','重新扫描','重新掃描','Scan again'),
('plane_lost','平面跟踪丢失，请重新扫描','平面追蹤遺失，請重新掃描','Surface tracking lost. Scan again.'),
('draw','绘制虫洞','繪製蟲洞','Draw portal'),
('draw_hint','按住扳机或抓捏，在平面上绘制闭合轮廓','按住扳機或捏合，在平面上繪製閉合輪廓','Hold trigger or pinch and trace a closed outline on a surface.'),
('move_hint','按住握持键，在当前平面内移动','按住握持鍵，在目前平面內移動','Hold grip to move within this surface.'),
('delete_hint','长按 B / Y 两秒删除','長按 B / Y 兩秒刪除','Hold B / Y for two seconds to delete.'),
('object_hint','按下摇杆呼出，左右切换，扳机确认','按下搖桿叫出，左右切換，扳機確認','Click thumbstick to open. Move left or right; trigger to confirm.'),
('mass','质量','質量','Mass'),
('velocity','初始速度','初始速度','Initial speed'),
('robot','探测机器人','探測機器人','Exploration robot'),
('core','能量核心','能量核心','Energy core'),
('craft','小型航天器','小型太空船','Small spacecraft'),
('probe','微型探测器','微型探測器','Micro probe'),
('capsule','重型舱体','重型艙體','Heavy capsule'),
('beacon','信标装置','信標裝置','Beacon'),
('missions','任务选择','任務選擇','Mission selection'),
('tier1','基础锚定','基礎錨定','Surface basics'),
('tier2','轨迹规划','軌跡規劃','Trajectory planning'),
('tier3','多点折跃','多點折躍','Multi-point transit'),
('tier4','稳定闭环','穩定閉環','Stable orbit'),
('locked','尚未解锁','尚未解鎖','Locked'),
('best','最快记录','最快紀錄','Best time'),
('portal_count','虫洞数量','蟲洞數量','Portal count'),
('time','实验用时','實驗用時','Elapsed time'),
('success','稳定闭环已建立','穩定閉環已建立','Stable loop established'),
('failure','实验中止：检测到碰撞','實驗中止：偵測到碰撞','Experiment stopped: collision'),
('collision','碰撞次数','碰撞次數','Collisions'),
('stars','任务评级','任務評級','Mission rating'),
('one_star','形成完整闭环','形成完整閉環','Complete the loop'),
('two_star','推荐虫洞数与限定时长内完成','建議蟲洞數與限定時間內完成','Meet recommended count and time'),
('three_star','最优虫洞数、零碰撞、最优时长','最佳蟲洞數、零碰撞、最佳時間','Optimal count and time; no collisions'),
('language','语言','語言','Language'),
('music','环境音乐','環境音樂','Ambient music'),
('sfx','效果音','效果音','Sound effects'),
('voice','语音提示','語音提示','Voice guidance'),
('haptics','手柄震动','控制器震動','Controller haptics'),
('on','开启','開啟','On'),
('off','关闭','關閉','Off'),
('saved','本地记录已保存','本機紀錄已儲存','Records saved locally'),
('no_record','暂无记录','尚無紀錄','No record'),
('next','下一关','下一關','Next mission'),
('tracking','平面跟踪正常','平面追蹤正常','Surface tracking active'),
('sandbox_hint','自由绘制与发射，无胜负判定','自由繪製與發射，不判定勝負','Draw and launch freely. No win or loss.'),
('v_welcome','欢迎进入引力实验室。请先扫描周围空间。','歡迎進入引力實驗室。請先掃描周圍空間。','Welcome to the Gravity Laboratory. Scan your surroundings to begin.'),
('v_scan','缓慢环顾四周，识别地面、桌面和墙面。','緩慢環顧四周，辨識地面、桌面和牆面。','Look around slowly to identify the floor, tables, and walls.'),
('v_ready','扫描完成。请在已识别的平面上绘制虫洞。','掃描完成。請在已辨識的平面上繪製蟲洞。','Scan complete. Draw a portal on a detected surface.'),
('v_draw','按住扳机或抓捏，绘制一个闭合轮廓。','按住扳機或捏合，繪製一個閉合輪廓。','Hold the trigger or pinch, and trace a closed outline.'),
('v_closed','虫洞已闭合，引力场稳定。','蟲洞已閉合，引力場穩定。','Portal closed. Gravitational field stable.'),
('v_select','按下摇杆，选择传送物体。','按下搖桿，選擇傳送物體。','Click the thumbstick to choose a payload.'),
('v_launch','按下扳机，将物体放置到首个虫洞入口。','按下扳機，將物體放置到第一個蟲洞入口。','Press the trigger to place the payload at the first portal.'),
('v_loop','闭环已建立。物体正在稳定循环。','閉環已建立。物體正在穩定循環。','Loop established. The payload is circulating steadily.'),
('v_fail','检测到碰撞。实验已中止，请手动重置关卡。','偵測到碰撞。實驗已中止，請手動重設關卡。','Collision detected. Experiment stopped. Reset the mission when ready.'),
('v_move','按住握持键，可在当前平面内移动虫洞。','按住握持鍵，可在目前平面內移動蟲洞。','Hold the grip button to move the portal within its current surface.'),
('v_delete','对准虫洞，长按 B 或 Y 键两秒删除。','對準蟲洞，長按 B 或 Y 鍵兩秒刪除。','Point at a portal and hold B or Y for two seconds to delete it.'),
('v_lost','平面跟踪丢失。请重新扫描空间。','平面追蹤遺失。請重新掃描空間。','Surface tracking lost. Scan your surroundings again.')]
L={code:{r[0]:r[i+1] for r in rows} for i,code in enumerate(['zh-Hans','zh-Hant','en'])}
for code,d in L.items(): (OUT/f'locales/{code}.json').write_text(json.dumps(d,ensure_ascii=False,indent=2))
tokens={'titanium':'#AEBCC7','graphite':'#101820','panel':'#18242E','ice':'#9CE5F4','text':'#EAF2F7','muted':'#93A9B8','amber':'#E9B66E','danger':'#F19A91'}
(OUT/'ui/tokens.json').write_text(json.dumps({'palette':tokens,'font':'PingFang SC, PingFang TC, Helvetica Neue, sans-serif','geometry':'Custom chamfered contours, engineering ticks, no native rounded controls','states':['idle','hover','pressed','disabled','focus'],'approval':'pending'},indent=2))
objs=[('robot','R-01',2.4,2.8),
('core','E-02',1.2,3.6),
('craft','S-03',1.8,4.0),
('probe','P-04',0.3,4.8),
('capsule','C-05',8.0,1.8),
('beacon','B-06',0.8,3.2)]
(OUT/'object-visual-catalog.json').write_text(json.dumps({'status':'proposed_for_asset_review','note':'Mass and launch speed are proposed tuning values, not yet approved game balance.','objects':[{'id':a,'marking':b,'massKg':m,'initialSpeedMps':v} for a,b,m,v in objs]},indent=2))
def text(x,y,t,size=16,color='#EAF2F7',extra=''):
 return f'<text x="{x}" y="{y}" fill="{color}" font-size="{size}" {extra}>{html.escape(str(t))}</text>'
def pathbox(x,y,w,h,fill='#18242E',stroke='#3C5667',cut=12):
 return f'<path d="M{x+cut} {y}H{x+w}V{y+h-cut}L{x+w-cut} {y+h}H{x}V{y+cut}Z" fill="{fill}" stroke="{stroke}"/>'
def btn(x,y,w,label,active=False):
 return pathbox(x,y,w,48,'#263E4A' if active else '#14212A','#9CE5F4' if active else '#425F70')+text(x+18,y+30,label,16,'#EAF2F7')
def star(cx,cy,r=10,fill='#E9B66E'):
 pts=[]
 for k in range(10):
  a=-math.pi/2+k*math.pi/5;rr=r if k%2==0 else r*.44;pts.append(f'{cx+math.cos(a)*rr:.2f},{cy+math.sin(a)*rr:.2f}')
 return f'<polygon points="{" ".join(pts)}" fill="{fill}"/>'
def icon(i,x,y,s=1):
 # Schematic badges are UI artwork, explicitly not 3D model previews.
 parts=[f'<g transform="translate({x} {y}) scale({s})" stroke="#AEBCC7" stroke-width="2" fill="#263945">']
 if i==0: parts+=['<path d="M-32 8H32V-14H-32Z"/><path d="M-38 9H38V23H-38Z"/>',''.join(f'<circle cx="{v}" cy="16" r="4"/>' for v in [-28,-14,0,14,28]),'<path d="M0-14V-30H22V-19H0Z"/><circle cx="15" cy="-25" r="3" fill="#9CE5F4"/>']
 elif i==1:parts+=['<circle r="24"/><ellipse rx="13" ry="34"/><ellipse rx="34" ry="13"/><circle r="9" fill="#9CE5F4"/>']
 elif i==2:parts+=['<path d="M0-33L12-2L39 17L10 13L7 28H-7L-10 13L-39 17L-12-2Z"/><path d="M0-16V20" stroke="#9CE5F4"/>']
 elif i==3:parts+=['<path d="M-8-22H8V22H-8Z"/><path d="M-8-10L-31-24M8-10L31-24M-8 10L-28 30M8 10L28 30"/><circle cy="-25" r="7" fill="#9CE5F4"/>']
 elif i==4:parts+=['<path d="M-30-24H30V24H-30Z"/><path d="M-22-29H-15V29H-22ZM15-29H22V29H15Z"/><path d="M-10 0H10" stroke="#E9B66E" stroke-width="5"/>']
 else:parts+=['<path d="M-28 26H28L18 13H-18ZM-6 13V-20H6V13Z"/><path d="M-25-20Q0-40 25-20M-17-13Q0-27 17-13" fill="none" stroke="#9CE5F4"/>']
 return ''.join(parts)+'</g>'
def header(d,title):
 return '<path d="M32 76H928" stroke="#425F70"/>'+text(32,40,'SB / GRAVITY LABORATORY',15,'#9CE5F4', 'letter-spacing="3"')+text(32,115,title,29)+text(32,146,'EXPERIMENT CONTROL   /   '+d['tracking'],12,'#93A9B8')+text(790,40,'MR / EXPERIMENT CONTROL',9,'#93A9B8')
def wrap(body,w=960,h=640):
 return f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}" font-family="PingFang SC,PingFang TC,Helvetica Neue,sans-serif"><defs><pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse"><path d="M40 0H0V40" fill="none" stroke="#9CE5F4" stroke-opacity=".035"/></pattern></defs>'+pathbox(1,1,w-2,h-2,'#101820','#526B78',24)+f'<path d="M0 0H{w}V{h}H0Z" fill="url(#grid)"/>'+body+'</svg>'
for lang,d in L.items():
 body=header(d,d['app'])+text(38,225,d['lab'],43)+text(40,267,'FIELD / 9.8 m·s⁻²',15,'#93A9B8')+text(40,302,'TRACE. TRANSIT. RETURN.',17,'#9CE5F4','letter-spacing="3"')
 for j,k in enumerate(['challenge','sandbox','tutorial']):body+=btn(40,345+j*65,340,d[k],j==0)
 body+=btn(40,553,160,d['settings'])+text(580,545,'GRAVITATIONAL FIELD / STABLE',12,'#93A9B8')
 body+='<g transform="translate(704 337) rotate(-18)"><ellipse rx="138" ry="172" fill="#091016" stroke="#243D4B" stroke-width="30"/><ellipse rx="138" ry="172" fill="none" stroke="#9CE5F4" stroke-width="2"/><ellipse rx="148" ry="184" fill="none" stroke="#9CE5F4" stroke-opacity=".25" stroke-dasharray="2 18"/>'
 for a in range(0,360,30):body+=f'<path d="M0-185V-194" transform="rotate({a})" stroke="#E9B66E"/>'
 body+='</g>'+text(676,343,'01',44,'#EAF2F7')
 (OUT/f'ui/main-menu.{lang}.svg').write_text(wrap(body))
 body=header(d,d['missions'])
 for row,k in enumerate(['tier1','tier2','tier3','tier4']):
  y=183+row*101;body+=text(34,y+22,f'0{row+1}',20,'#9CE5F4')+text(80,y+22,d[k],16)
  for j in range(3):
   n=row*3+j+1;x=300+j*208;locked=n>3
   body+=pathbox(x,y-3,190,80,'#14212A','#314754' if locked else '#698B9B')+text(x+16,y+27,f'{n:02}',24,'#667B89' if locked else '#EAF2F7')
   body+=text(x+16,y+55,d['locked'] if locked else ('00:42.80' if n==1 else d['no_record']),12,'#93A9B8')
   if not locked:
    for t in range(3):body+=star(x+114+t*24,y+24,8,'#E9B66E' if n==1 else '#354B59')
 body+=btn(34,579,140,d['back'])
 (OUT/f'ui/level-select.{lang}.svg').write_text(wrap(body))
 body=header(d,d['select'])
 for j,(k,tag,m,v) in enumerate(objs):
  x=32+j*151;body+=pathbox(x,206,140,176,'#1B303D' if j==0 else '#14212A','#9CE5F4' if j==0 else '#36505F')+text(x+12,229,tag,12,'#9CE5F4')+icon(j,x+70,291,.9)+text(x+12,353,d[k],13)
 body+=text(36,433,d['robot'],27)+text(36,474,f'{d["mass"]}  2.4 kg     /     {d["velocity"]}  2.8 m/s',18,'#93A9B8')+text(36,521,d['object_hint'],14,'#93A9B8')+btn(36,559,180,d['back'])+btn(704,559,220,d['place'],True)
 (OUT/f'ui/object-select.{lang}.svg').write_text(wrap(body))
 for failed in [False,True]:
  body=header(d,d['failure'] if failed else d['success'])
  if not failed:
   for j in range(3):body+=star(415+j*65,224,25)
  else:body+='<path d="M480 180L520 250H440Z" fill="none" stroke="#F19A91" stroke-width="3"/>'+text(475,237,'!',30,'#F19A91')
  for j,(key,val) in enumerate([('time','00:42.80'),
('portal_count','03'),
('collision','01' if failed else '00')]):
   x=70+j*298;body+=text(x,333,d[key],16,'#93A9B8')+text(x,380,val,34)
  body+=text(70,446,d['failure'] if failed else d['three_star'],17,'#93A9B8')+text(70,483,d['saved'],13,'#93A9B8')+btn(70,549,180,d['back'])+btn(655,549,235,d['reset'] if failed else d['next'],True)
  (OUT/f'ui/result-{"failure" if failed else "success"}.{lang}.svg').write_text(wrap(body))
 body=header(d,d['settings'])+text(36,205,d['language'],18)
 for j,label in enumerate(['简体中文','繁體中文','English']):body+=btn(280+j*210,174,194,label,['zh-Hans','zh-Hant','en'][j]==lang)
 for j,(key,val) in enumerate([('music',65),
('sfx',85),
('voice',80)]):
  y=286+j*77;body+=text(36,y,d[key],18)+f'<path d="M300 {y-5}H850" stroke="#354B59" stroke-width="5"/><path d="M300 {y-5}H{300+5.5*val}" stroke="#9CE5F4" stroke-width="5"/>'+f'<path d="M{295+5.5*val} {y-16}h10v22h-10Z" fill="#EAF2F7"/>'+text(880,y,val,15,'#93A9B8')
 body+=text(36,517,d['haptics'],18)+btn(300,487,180,d['on'],True)+btn(36,568,160,d['back'])
 (OUT/f'ui/settings.{lang}.svg').write_text(wrap(body))
# Reusable vector symbol sources and state examples.
icons={'scan':'M3 9V3H9M15 3H21V9M21 15V21H15M9 21H3V15M7 12H17','close':'M5 5L19 19M19 5L5 19','back':'M14 5L7 12L14 19M7 12H21','play':'M8 4L20 12L8 20Z','reset':'M4 10A8 8 0 1 1 4 16M4 10V3M4 10H11','grip':'M7 20V9Q7 6 10 9V3Q12 0 13 3V11Q18 7 19 12L17 21Z','portal':'M12 3C22 2 24 18 15 21C3 25 0 7 12 3Z','lock':'M6 11H18V21H6ZM8 11V7A4 4 0 0 1 16 7V11','sound':'M3 9H7L12 4V20L7 15H3ZM16 8Q22 12 16 16','check':'M4 12L10 18L21 5'}
for n,p in icons.items(): (OUT/f'ui/icon-{n}.svg').write_text(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="square" stroke-linejoin="miter"><path d="{p}"/></svg>')
for n,c in [('earned','#E9B66E'),
('empty','#354B59')]: (OUT/f'ui/star-{n}.svg').write_text('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">'+star(12,12,11,c)+'</svg>')
body=text(32,55,'CONTROL / STATE LIBRARY',26)
for j,(name,fill,stroke) in enumerate([('IDLE','#14212A','#425F70'),
('HOVER','#263E4A','#9CE5F4'),
('PRESSED','#365968','#EAF2F7'),
('DISABLED','#14212A','#263945'),
('FOCUS','#263E4A','#E9B66E')]):body+=pathbox(32+j*184,92,168,50,fill,stroke)+text(47+j*184,123,name,14,'#667B89' if j==3 else '#EAF2F7')
for j in range(5):
 body+=text(32,207+j*59,f'PROGRESS / {j*25}%',14,'#93A9B8')+f'<path d="M245 {200+j*59}H890" stroke="#354B59" stroke-width="7"/>'
 if j: body+=f'<path d="M245 {200+j*59}H{245+645*j/4}" stroke="#9CE5F4" stroke-width="7" stroke-dasharray="10 3"/>'
for j in range(3):body+=star(53+j*40,536,14)
body+=text(190,542,'EARNED / EMPTY / WARNING / SURFACE RETICLE',15,'#93A9B8')+'<path d="M800 498V516M800 550V568M765 533H783M817 533H835" stroke="#9CE5F4"/>'
(OUT/'ui/control-library.svg').write_text(wrap(body))
# Vector animated effect preview. Visual asset only; spatial runtime implementation waits for approval.
portal='''<svg xmlns="http://www.w3.org/2000/svg" width="960" height="640" viewBox="0 0 960 640"><defs><radialGradient id="lens"><stop stop-color="#050a0f"/><stop offset=".8" stop-color="#101e29"/><stop offset="1" stop-color="#75c9df" stop-opacity=".4"/></radialGradient><filter id="halo"><feGaussianBlur stdDeviation="6"/></filter></defs><path d="M0 0H960V640H0Z" fill="#101820"/><g stroke="#344651" opacity=".6">'''
for y in range(100,640,55):portal+=f'<path d="M0 {y}H960"/>'
for x in range(0,961,80):portal+=f'<path d="M480 50L{x} 640"/>'
portal+='</g><g transform="translate(480 330)"><g><animateTransform attributeName="transform" type="scale" values="0;1;1;1;.03;0" keyTimes="0;.15;.65;.8;.96;1" dur="8s" repeatCount="indefinite"/><path id="rim" d="M-155-80C-128-185 58-192 139-98C224 7 142 170 25 180C-112 199-229 57-155-80Z" fill="url(#lens)" stroke="#9CE5F4" stroke-width="3"/><use href="#rim" fill="none" stroke="#9CE5F4" stroke-width="9" filter="url(#halo)" opacity=".5"/>'
for j in range(50):
 a=j*math.tau/50;x=math.cos(a)*270;y=math.sin(a)*235
 portal+=f'<circle r="{1+j%3*.4}" fill="#B5E8F7"><animate attributeName="cx" from="{x}" to="0" dur="{1.4+j%5*.2}s" begin="-{j*.13}s" repeatCount="indefinite"/><animate attributeName="cy" from="{y}" to="0" dur="{1.4+j%5*.2}s" begin="-{j*.13}s" repeatCount="indefinite"/><animate attributeName="opacity" values="0;.9;0" dur="{1.4+j%5*.2}s" repeatCount="indefinite"/></circle>'
portal+='</g></g><g font-family="Helvetica Neue,PingFang SC" fill="#EAF2F7"><text x="36" y="46" font-size="22">GRAVITY FIELD / 虫洞动态素材预览</text><text x="36" y="600" font-size="15" fill="#93A9B8">描边闭合 → 引力吸入 → 向心收缩消失 · 8 秒循环 · 非设备运行截图</text></g></svg>'
(OUT/'vfx/wormhole-cycle.svg').write_text(portal)
(OUT/'vfx/wormhole-timing.json').write_text(json.dumps({'status':'asset_preview_not_runtime','cycleSeconds':8,'stages':[{'id':'close','from':0,'to':1.2},{'id':'suction','from':1.2,'to':6.4},{'id':'collapse','from':6.4,'to':8}],'runtimeRequirements':['Arbitrary closed-contour rim matching detected real plane','Inside-only suction particles','Background lensing depends on supported renderer capability','Bidirectional surface-normal entry and exit','Spatial Audio mono emitter with distance attenuation'],'palette':tokens},indent=2))
print('Vector and localization assets written:',OUT)
