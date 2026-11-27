"""Generate Space Bend's offline neural narration; no TTS dependency at runtime.

Run with edge-tts installed in a local venv. MP3 sources stay under .scratch;
the reviewed mono PCM WAVs and provenance manifest are the shipped assets.
"""
import asyncio
import array
import hashlib
import json
import math
import shutil
import subprocess
import wave
from pathlib import Path

import edge_tts

ROOT = Path(__file__).resolve().parents[2]
TEMP = ROOT / '.scratch/voice-1.0.6'
SOURCE = ROOT / 'assets/style-a/audio'
PACKAGED = ROOT / 'app/src/main/assets/audio'
VOICES = {'zh-Hans': ('zh-CN-XiaoxiaoNeural', '-6%', '-2Hz'),
          'en': ('en-US-AriaNeural', '-5%', '-2Hz')}
TEXTS = {
 'zh-Hans': {
  'v_welcome': '欢迎来到引力实验室。请选择自由体验，或关卡挑战。',
  'v_scan': '请缓慢环顾四周。看看地面、桌面，还有墙面。确认扫描范围后，点击继续。',
  'v_ready': '扫描完成，空间已锁定。现在，可以在平面上绘制虫洞了。',
  'v_draw': '按住扳机，或保持抓捏。画出一个闭合的轮廓。',
  'v_closed': '虫洞已就绪。引力场稳定。',
  'v_select': '选一个物体吧。确认洞口足够大，再点击发射。',
  'v_launch': '发射成功。看看它会怎样穿过虫洞。',
  'v_loop': '闭环建立成功。物体正在稳定循环。',
  'v_fail': '检测到碰撞，实验已停止。准备好后，再试一次。',
  'v_move': '按住握持键，就能在当前平面内移动虫洞。',
  'v_delete': '对准虫洞。按住手柄的 B 键，或 Y 键，两秒后删除。',
  'v_lost': '当前平面不可用。请重新扫描周围的空间。',
 },
 'zh-Hant': {
  'v_welcome': '歡迎來到引力實驗室。請選擇自由體驗，或關卡挑戰。',
  'v_scan': '請緩慢環顧四周。看看地面、桌面，還有牆面。確認掃描範圍後，點擊繼續。',
  'v_ready': '掃描完成，空間已鎖定。現在，可以在平面上繪製蟲洞了。',
  'v_draw': '按住扳機，或保持抓捏。畫出一個閉合的輪廓。',
  'v_closed': '蟲洞已就緒。引力場穩定。',
  'v_select': '選一個物體吧。確認洞口足夠大，再點擊發射。',
  'v_launch': '發射成功。看看它會怎樣穿過蟲洞。',
  'v_loop': '閉環建立成功。物體正在穩定循環。',
  'v_fail': '偵測到碰撞，實驗已停止。準備好後，再試一次。',
  'v_move': '按住握持鍵，就能在目前平面內移動蟲洞。',
  'v_delete': '對準蟲洞。按住手把的 B 鍵，或 Y 鍵，兩秒後刪除。',
  'v_lost': '目前平面無法使用。請重新掃描周圍的空間。',
 },
 'en': {
  'v_welcome': 'Welcome to the Gravity Lab. Choose free play, or a mission challenge.',
  'v_scan': 'Take a slow look around. Scan the floor, tables, and walls. When you are ready, select continue.',
  'v_ready': 'Scan complete. Your surfaces are now locked. You can start drawing portals.',
  'v_draw': 'Hold the trigger, or keep pinching. Draw a closed outline.',
  'v_closed': 'Portal ready. The gravitational field is stable.',
  'v_select': 'Choose an object. Make sure it fits the portals, then select launch.',
  'v_launch': 'Launch successful. Watch it travel through your portals.',
  'v_loop': 'You have made a stable loop. The object is circulating smoothly.',
  'v_fail': 'There was a collision. The experiment has stopped. Try again when you are ready.',
  'v_move': 'Hold the grip button to move a portal along its current surface.',
  'v_delete': 'Point at a portal. Hold the B button, or the Y button, for two seconds to delete it.',
  'v_lost': 'This surface is no longer available. Please scan your surroundings again.',
 },
}

def master(path):
    with wave.open(str(path), 'rb') as f:
        assert (f.getnchannels(), f.getframerate(), f.getsampwidth()) == (1, 48000, 2)
        samples = array.array('h', f.readframes(f.getnframes()))
    # Keep natural pauses inside phrases; trim only leading/trailing digital silence.
    active = [i for i, v in enumerate(samples) if abs(v) > 100]
    assert active, 'Silent synthesis'
    samples = samples[max(0, active[0] - 3840):min(len(samples), active[-1] + 5760)]
    rms = math.sqrt(sum(v*v for v in samples) / len(samples))
    gain = min(32767 * 10**(-20/20) / rms, 27570 / max(abs(v) for v in samples), 3.0)
    samples = array.array('h', (round(v * gain) for v in samples))
    for i in range(min(384, len(samples)//2)):
        samples[i] = round(samples[i] * i/384)
        samples[-i-1] = round(samples[-i-1] * i/384)
    with wave.open(str(path), 'wb') as f:
        f.setparams((1, 2, 48000, 0, 'NONE', 'not compressed'))
        f.writeframes(samples.tobytes())
    return {'durationSeconds': len(samples)/48000,
            'peakDbfs': 20*math.log10(max(abs(v) for v in samples)/32768),
            'rmsDbfs': 20*math.log10(math.sqrt(sum(v*v for v in samples)/len(samples))/32768)}

async def main():
    TEMP.mkdir(parents=True, exist_ok=True)
    manifest = json.loads((SOURCE/'manifest.json').read_text())
    records = []
    for locale, (voice, rate, pitch) in VOICES.items():
        for key, text in TEXTS[locale].items():
            mp3 = TEMP/f'{locale}-{key}.mp3'
            wav = TEMP/f'{locale}-{key}.wav'
            if not mp3.exists():
                for attempt in range(3):
                    try:
                        await edge_tts.Communicate(text, voice, rate=rate, pitch=pitch).save(str(mp3))
                        break
                    except Exception:
                        mp3.unlink(missing_ok=True)
                        if attempt == 2: raise
                        await asyncio.sleep(2)
            subprocess.run(['afconvert','-f','WAVE','-d','LEI16@48000','-c','1',str(mp3),str(wav)],check=True)
            metrics = master(wav)
            assert .3 < metrics['durationSeconds'] < 20
            assert metrics['peakDbfs'] < -1.4
            for base in (SOURCE, PACKAGED):
                target = base/f'voice/{locale}/{key}.wav'
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(wav, target)
            for entry in manifest['voice']:
                if entry['language'] == locale and entry['id'] == key:
                    entry.update(text=text, voice=voice, rate=rate, pitch=pitch, engine='Microsoft Edge neural TTS via edge-tts')
            records.append({'locale':locale,'id':key,'voice':voice,'text':text,
                            'channels':1,'sampleRate':48000,'bits':16,**metrics,
                            'sha256':hashlib.sha256(wav.read_bytes()).hexdigest()})
            print(f'{locale} {key}: {metrics["durationSeconds"]:.2f}s',flush=True)
    # Write the new copy only once every required audio file has been generated.
    for locale, values in TEXTS.items():
        for base in (ROOT/'assets/style-a/locales', ROOT/'app/src/main/assets/locales'):
            path = base/f'{locale}.json'
            if path.exists():
                strings = json.loads(path.read_text());strings.update(values)
                path.write_text(json.dumps(strings,ensure_ascii=False,indent=2)+'\n')
    manifest['notes']=[x for x in manifest.get('notes',[]) if 'macOS' not in x]
    manifest['voiceAliases']={'zh-Hans':'zh-Hans','zh-Hant':'zh-Hans','en':'en'}
    manifest['voiceDesign']={'version':'1.0.6','direction':'Calm, natural laboratory guide; female neural voices, restrained warmth, short phrases; no vocoder or metallic echo.',
                            'delivery':'Offline mono PCM; no network or speech SDK at runtime.'}
    (SOURCE/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    report=ROOT/'docs/delivery/room-1.0.6/voice-validation.json'
    report.parent.mkdir(parents=True,exist_ok=True)
    report.write_text(json.dumps({'records':records,'aliases':manifest['voiceAliases']},ensure_ascii=False,indent=2)+'\n')

if __name__ == '__main__': asyncio.run(main())
