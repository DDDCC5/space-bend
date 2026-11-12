"""Check the compiled APK identity, resolved icon references and decoded pixels."""
from pathlib import Path
import hashlib
import io
import json
import re
import subprocess
import sys
import zipfile
import xml.etree.ElementTree as ET
from PIL import Image

root = Path(__file__).resolve().parents[2]
apk = Path(sys.argv[1])
aapt2 = sys.argv[2]
out = root / 'docs/delivery/launcher-identity'
out.mkdir(parents=True, exist_ok=True)
badging = subprocess.check_output([aapt2, 'dump', 'badging', str(apk)], text=True)
manifest = subprocess.check_output([aapt2, 'dump', 'xmltree', str(apk), '--file', 'AndroidManifest.xml'], text=True)
resources = subprocess.check_output([aapt2, 'dump', 'resources', str(apk)], text=True)
assert "name='com.example.spacebend' versionCode='2' versionName='1.0.1'" in badging
icon_id = re.search(r'resource (0x\w+) mipmap/ic_spatial_launcher', resources).group(1)
for attribute in ['icon', 'roundIcon']:
    assert re.search(rf'android:{attribute}\(.*?\)=@{icon_id}', manifest)
for key, resource in [('icon.3d.list', 'icon_3d_list'), ('icon.sdf.list', 'icon_sdf_list')]:
    array_id = re.search(rf'resource (0x\w+) array/{resource}', resources).group(1)
    assert re.search(rf'name.*?="{re.escape(key)}".*?resource.*?=@{array_id}', manifest, re.S)
assert '[@drawable/icon_3d_layer_1, @drawable/icon_3d_layer_0]' in resources
assert '[@drawable/icon_3d_sdf_1, @drawable/icon_3d_sdf_0]' in resources

names = ['ic_launcher_foreground.png', 'icon_3d_layer_0.png', 'icon_3d_layer_1.png',
         'icon_3d_sdf_0.png', 'icon_3d_sdf_1.png', 'ic_spatial_launcher.png']
records = []
with zipfile.ZipFile(apk) as z:
    assert not any(n.startswith('org/byted/ide') for n in z.namelist())
    for name in names:
        paths = [n for n in z.namelist() if n.startswith('res/') and n.endswith('/' + name)]
        assert len(paths) == 1, (name, paths)
        data = z.read(paths[0])
        source = next((root / 'app/src/main/res').rglob(name))
        packaged = Image.open(io.BytesIO(data)).convert('RGBA')
        expected = Image.open(source).convert('RGBA')
        assert packaged.size == expected.size == (1024, 1024)
        assert packaged.tobytes() == expected.tobytes(), name
        records.append({'apk_entry': paths[0], 'size': list(packaged.size),
                        'source_sha256': hashlib.sha256(source.read_bytes()).hexdigest(),
                        'decoded_pixels_match_source': True})
        if name == 'ic_spatial_launcher.png':
            packaged.save(out / 'icon-extracted-from-apk.png')

results = [ET.parse(p).getroot() for p in (root / 'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml')]
tests = {k: sum(int(r.get(k, 0)) for r in results) for k in ['tests', 'failures', 'errors', 'skipped']}
assert tests['tests'] == 34 and tests['failures'] == tests['errors'] == 0
report = {'package': 'com.example.spacebend', 'versionName': '1.0.1', 'versionCode': 2,
          'apk_sha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
          'android_icon_and_round_icon': 'mipmap/ic_spatial_launcher',
          'pico_layer_order_front_to_back': ['icon_3d_layer_1', 'icon_3d_layer_0'],
          'pico_sdf_order_front_to_back': ['icon_3d_sdf_1', 'icon_3d_sdf_0'],
          'compiled_manifest_and_arrays_verified': True, 'resources': records, 'jvm_tests': tests,
          'runtime_launcher_visual_verification': 'not performed; packaged icon extracted and visually inspected'}
(out / 'apk-icon-verification.json').write_text(json.dumps(report, indent=2) + '\n')
(out / 'apk-manifest.txt').write_text(manifest)
(out / 'apk-badging.txt').write_text(badging)
print(json.dumps(report, indent=2))
