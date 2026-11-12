"""Export the approved submission vector into Android and PICO icon layers.

SDF generation uses the installed PICO App Identity Configurator's offline
generator (tools/assets/GenerateIconSdf.java), never an app runtime dependency.
"""
from pathlib import Path
import io
import re

from PIL import Image
import resvg_py

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'app/src/main/res'
SOURCE = ROOT / 'output/submission-1.0/01-icon-1024.svg'
svg = SOURCE.read_text()
defs = re.search(r'<defs>.*?</defs>', svg, re.S).group()
body = svg.split('</defs>', 1)[1].rsplit('</svg>', 1)[0]
portal_end = body.index('</g>') + len('</g>')
background = body[:portal_end]
foreground = body[portal_end:]
clip = '<clipPath id="iconCircle"><circle cx="512" cy="512" r="512"/></clipPath>'


def render(body, viewbox='0 0 1024 1024'):
    text = f'<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="{viewbox}">{defs[:-7]}{clip}</defs><g clip-path="url(#iconCircle)">{body}</g></svg>'
    return resvg_py.svg_to_bytes(svg_string=text)


bg = render(background)
# Foreground contour is binary alpha as required for spatial layer edges;
# lighting and hover shadows are provided by the PICO launcher itself.
fg = Image.open(io.BytesIO(render(foreground))).convert('RGBA')
fg.putalpha(fg.getchannel('A').point(lambda a: 255 if a >= 200 else 0))
fg_path = RES / 'drawable/icon_3d_layer_1.png'
fg.save(fg_path)
(RES / 'drawable/icon_3d_layer_0.png').write_bytes(bg)
composite = Image.alpha_composite(Image.open(io.BytesIO(bg)).convert('RGBA'), fg)
composite.save(RES / 'mipmap-xxxhdpi/ic_spatial_launcher.png')
(RES / 'drawable/ic_launcher_foreground.png').write_bytes(render(background + foreground, '-256 -256 1536 1536'))

preview = ROOT / 'docs/delivery/launcher-identity'
preview.mkdir(parents=True, exist_ok=True)
composite.save(preview / 'apk-icon-preview.png')
print('Exported compatibility icon, adaptive foreground, background and foreground layers')
