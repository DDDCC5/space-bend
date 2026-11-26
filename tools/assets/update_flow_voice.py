"""Refresh only the four flow-dependent prompts using the existing macOS voices."""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess
import wave

ROOT = Path(__file__).resolve().parents[2]
KEYS = ('v_welcome', 'v_ready', 'v_select', 'v_launch')
SOURCES = ROOT / 'assets/style-a/audio'
PACKAGED = ROOT / 'app/src/main/assets/audio'
TEMP = ROOT / '.scratch/flow-voice-20261126'
TEMP.mkdir(parents=True, exist_ok=True)
manifest = json.loads((SOURCES / 'manifest.json').read_text())
records = []
for locale, voice, rate in [('zh-Hans', 'Binbin (Enhanced)', 175), ('en', 'Samantha', 155)]:
    texts = json.loads((ROOT / f'app/src/main/assets/locales/{locale}.json').read_text())
    for key in KEYS:
        aiff = TEMP / f'{locale}-{key}.aiff'
        wav = SOURCES / f'voice/{locale}/{key}.wav'
        subprocess.run(['say', '-v', voice, '-r', str(rate), '-o', str(aiff), texts[key]], check=True)
        subprocess.run(['afconvert', '-f', 'WAVE', '-d', 'LEI16@48000', '-c', '1', str(aiff), str(wav)], check=True)
        shutil.copy2(wav, PACKAGED / f'voice/{locale}/{key}.wav')
        with wave.open(str(wav), 'rb') as f:
            assert (f.getnchannels(), f.getframerate(), f.getsampwidth()) == (1, 48000, 2)
            duration = f.getnframes() / f.getframerate()
            assert 0 < duration < 15
        for entry in manifest['voice']:
            if entry['language'] == locale and entry['id'] == key:
                entry['text'] = texts[key]
                entry['voice'] = voice
        records.append({'locale': locale, 'id': key, 'text': texts[key], 'channels': 1,
                        'sampleRate': 48000, 'durationSeconds': duration,
                        'sha256': hashlib.sha256(wav.read_bytes()).hexdigest()})
manifest['voiceAliases'] = {'zh-Hans': 'zh-Hans', 'zh-Hant': 'zh-Hans', 'en': 'en'}
(SOURCES / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
report = ROOT / 'docs/delivery/flow-1.0.5/voice-validation.json'
report.parent.mkdir(parents=True, exist_ok=True)
report.write_text(json.dumps({'records': records, 'aliases': manifest['voiceAliases'],
                              'traditionalVoiceDuplicated': False}, ensure_ascii=False, indent=2) + '\n')
print(f'Validated {len(records)} updated mono prompts; Traditional Chinese shares zh-Hans audio.')
