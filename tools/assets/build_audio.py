# -*- coding: utf-8 -*-
from pathlib import Path
import math,random,array,wave,json,subprocess
O=Path(__file__).resolve().parents[2]/'assets/style-a/audio'; SR=48000
random.seed(173)
def save(name,samples,channels=1):
 a=array.array('h',(int(max(-1,min(1,v)) * 26000) for v in samples))
 with wave.open(str(O/name),'wb') as w:w.setnchannels(channels);w.setsampwidth(2);w.setframerate(SR);w.writeframes(a.tobytes())
def tone(t,f):return math.sin(2*math.pi*f*t)
for name,dur in [('draw-loop',2),('portal-close',.8),('suction-loop',4),('object-flight',1.6),('plane-impact',.6),('success',1.4),('failure',1.4),('portal-collapse',1.3)]:
 a=[];low=0
 for j in range(int(dur*SR)):
  t=j/SR;u=t/dur;noise=random.uniform(-1,1);low=.97*low+.03*noise
  if name=='draw-loop':v=.12*tone(t,175)+.05*noise+.05*tone(t,350+20*tone(t,.5));env=.8+.2*tone(t,.5)
  elif name=='suction-loop':v=.20*tone(t,48)+.18*low+.04*tone(t,144);env=.8+.2*tone(t,.25)
  elif name=='portal-close':v=.4*tone(t,80+210*(1-u))+.2*low+.12*tone(t,830);env=math.exp(-6*u)*min(1,t/.008)
  elif name=='object-flight':v=.18*noise+.2*tone(t,180-70*u);env=math.sin(math.pi*u)**2
  elif name=='plane-impact':v=.5*low+.3*tone(t,62)+.1*noise;env=math.exp(-10*u)*min(1,t/.002)
  elif name=='portal-collapse':v=.25*low+.25*tone(t,180*(1-u)+35);env=math.sin(math.pi*u)**.6
  else:
   notes=[392,494,587] if name=='success' else [220,196,147];v=0
   for k,f in enumerate(notes):
    tt=t-k*.19
    if tt>=0:v+=.19*tone(tt,f)*math.exp(-4*tt)*min(1,tt/.015)
   env=min(1,(dur-t)/.06)
  if name.endswith('loop'):env*=min(1,t/.015,(dur-t)/.015)
  a.append(v*env)
 if name.endswith('loop'):
  # Crossfade endpoints, preserve exact zero endpoint and avoid looping clicks.
  a[0]=a[-1]=0
 save('sfx/'+name+'.wav',a)
 print(name,flush=True)
# Exactly periodic 32-second stereo ambient bed; integer cycles guarantee loop continuity.
D=32;ns=int(D*SR);buf=[]
for j in range(ns):
 t=j/SR
 # All frequencies are integer multiples of 1/32 Hz.
 l=.14*tone(t,48)+.08*tone(t,72)+.042*tone(t,144)+.025*tone(t,216)+.015*tone(t,385)
 r=.14*tone(t,48)+.08*tone(t,72)+.042*tone(t,144+.03125)+.025*tone(t,216)+.015*tone(t,385-.03125)
 gain=.68+.15*math.sin(2*math.pi*t/32)
 buf.extend((l*gain,r*gain))
save('ambient-lab-loop.wav',buf,2)
manifest={'sampleRate':SR,'format':'PCM 16-bit WAV','music':{'file':'ambient-lab-loop.wav','channels':2,'durationSeconds':D,'loop':True},'sfx':[],'voice':[],'notes':['SFX are mono emitters for later PICO Spatial Audio placement.','Distance attenuation and HRTF require runtime integration; stereo preview is not proof of spatial playback.','Locally synthesized original audio. Voice uses installed macOS TTS, pending voice-quality approval.']}
for p in sorted((O/'sfx').glob('*.wav')):
 with wave.open(str(p),'rb') as w:manifest['sfx'].append({'file':str(p.relative_to(O)),'channels':w.getnchannels(),'durationSeconds':w.getnframes()/SR,'loop':'loop' in p.stem})
for lang,voice,rate in [('zh-Hans','Binbin (Enhanced)',175),('en','Samantha',155)]:
 d=json.loads((O.parent/'locales'/f'{lang}.json').read_text());folder=O/'voice'/lang;folder.mkdir(parents=True,exist_ok=True)
 for key,txt in d.items():
  if not key.startswith('v_'):continue
  aiff=folder/f'{key}.aiff';wav=folder/f'{key}.wav'
  subprocess.run(['say','-v',voice,'-r',str(rate),'-o',str(aiff),txt],check=True,stdout=subprocess.DEVNULL)
  subprocess.run(['afconvert','-f','WAVE','-d','LEI16@48000','-c','1',str(aiff),str(wav)],check=True)
  aiff.unlink()
  manifest['voice'].append({'language':lang,'id':key,'text':txt,'voice':voice,'file':str(wav.relative_to(O))})
 print('voice',lang,'complete',flush=True)
manifest['voiceAliases']={'zh-Hans':'zh-Hans','zh-Hant':'zh-Hans','en':'en'}
(O/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2))
