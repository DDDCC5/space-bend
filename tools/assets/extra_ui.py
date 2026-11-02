# -*- coding: utf-8 -*-
import runpy
from pathlib import Path
m=runpy.run_path(str(Path(__file__).with_name('build_assets.py')))
O=m['OUT'];text=m['text'];box=m['pathbox'];wrap=m['wrap'];btn=m['btn']
for state,fill,stroke in [('idle','#14212A','#425F70'),('hover','#263E4A','#9CE5F4'),('pressed','#365968','#EAF2F7'),('disabled','#14212A','#263945'),('focus','#263E4A','#E9B66E')]:
 (O/f'ui/button-{state}.svg').write_text('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 240 56">'+box(1,1,238,54,fill,stroke)+'</svg>')
for n in [0,25,50,75,100]:
 a=f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 24"><path d="M2 12H398" stroke="#354B59" stroke-width="6"/>'
 if n:a+=f'<path d="M2 12H{2+396*n/100}" stroke="#9CE5F4" stroke-width="6" stroke-dasharray="10 3"/>'
 (O/f'ui/progress-{n}.svg').write_text(a+'</svg>')
for lang,d in m['L'].items():
 a=m['header'](d,d['scan'])+'<g fill="none" stroke="#9CE5F4" stroke-width="2"><path d="M310 230H650V440H310Z" stroke-dasharray="6 12"/><path d="M290 260V210H340M620 210H670V260M670 410V460H620M340 460H290V410"/><path d="M310 337H650" stroke-opacity=".5"/></g>'+text(34,519,d['scanning'],20)+text(34,552,d['v_scan'],14,'#93A9B8')+btn(34,578,180,d['scan_retry'])+btn(700,578,220,d['continue'],True)
 (O/f'ui/room-scan.{lang}.svg').write_text(wrap(a))
 keys=['scan','draw','select','place','loop']
 a=m['header'](d,d['tutorial'])
 for j,k in enumerate(keys):
  x=60+j*180;a+=box(x,208,150,95,'#1B303D' if j==1 else '#14212A','#9CE5F4' if j==1 else '#425F70')+text(x+16,238,f'0{j+1}',14,'#9CE5F4')+text(x+16,276,d[k],14)
 a+=text(60,384,d['draw'],28)+text(60,430,d['draw_hint'],16,'#93A9B8')+text(60,473,d['delete_hint'],14,'#93A9B8')+btn(60,555,180,d['back'])+btn(690,555,210,d['continue'],True)
 (O/f'ui/tutorial.{lang}.svg').write_text(wrap(a))
