"""Deterministic submission exports. Requires resvg-py and Pillow.

Raster poster art is generated separately with imagegen. Screenshots retain
their original pixels: only a documented viewport crop, uniform scaling and a
small disclosure strip are applied. No app content is generated or retouched.
"""
from pathlib import Path
import base64
import hashlib
import io
import json
import zipfile

import resvg_py
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "output/submission-1.0"
OUT.mkdir(parents=True, exist_ok=True)


def embedded(path):
    return "data:image/png;base64," + base64.b64encode(path.read_bytes()).decode()


def svg_png(svg, target):
    target.write_bytes(resvg_py.svg_to_bytes(svg_string=svg))


icon = '''<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024">
<defs>
 <radialGradient id="bg"><stop stop-color="#1c3541"/><stop offset="1" stop-color="#091119"/></radialGradient>
 <radialGradient id="void"><stop stop-color="#02060b"/><stop offset=".82" stop-color="#07101c"/><stop offset="1" stop-color="#224c5c"/></radialGradient>
 <linearGradient id="ring" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#edfcff"/><stop offset=".35" stop-color="#89e8ff"/><stop offset=".65" stop-color="#327b98"/><stop offset="1" stop-color="#c5f4ff"/></linearGradient>
 <linearGradient id="steel" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#e1e9ec"/><stop offset=".45" stop-color="#7c949f"/><stop offset="1" stop-color="#344852"/></linearGradient>
 <filter id="glow" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="14"/></filter>
</defs>
<rect width="1024" height="1024" fill="url(#bg)"/>
<path d="M80 220V110L110 80H220M804 80H914L944 110V220M944 804V914L914 944H804M220 944H110L80 914V804" fill="none" stroke="#557580" stroke-width="4"/>
<g transform="rotate(24 512 512)">
 <ellipse cx="512" cy="512" rx="240" ry="338" fill="url(#void)"/>
 <ellipse cx="512" cy="512" rx="249" ry="347" fill="none" stroke="#68d6fd" stroke-width="30" opacity=".36" filter="url(#glow)"/>
 <ellipse cx="512" cy="512" rx="244" ry="342" fill="none" stroke="url(#ring)" stroke-width="16"/>
 <ellipse cx="512" cy="512" rx="267" ry="365" fill="none" stroke="#4f96ae" stroke-width="3" stroke-dasharray="46 12 5 12"/>
 <ellipse cx="512" cy="512" rx="227" ry="323" fill="none" stroke="#90e8ff" stroke-opacity=".6" stroke-width="3"/>
 <path d="M455 230C690 390 310 610 570 788M407 261C640 450 340 560 620 755M365 306C588 431 415 658 665 714" fill="none" stroke="#6dc6e5" stroke-width="3" opacity=".26"/>
</g>
<path d="M159 640C255 687 324 685 414 625" fill="none" stroke="#f2bd73" stroke-width="7" stroke-dasharray="17 13"/>
<path d="M398 535L515 485L625 552L509 608Z" fill="#b9cbd3" stroke="#e0edf2" stroke-width="4"/>
<path d="M398 535L509 608V743L398 672Z" fill="#415a69" stroke="#91a9b5" stroke-width="4"/>
<path d="M509 608L625 552V685L509 743Z" fill="url(#steel)" stroke="#c0d4de" stroke-width="4"/>
<path d="M418 562L485 605V628L418 586ZM531 620L602 585V608L531 644Z" fill="#efb569"/>
<path d="M491 552L517 540L543 555L517 568Z" fill="#0b1c26"/>
<path d="M464 100H560M512 52V148M464 924H560M512 876V972" fill="none" stroke="#628a99" stroke-width="3"/>
</svg>'''
(OUT / "01-icon-1024.svg").write_text(icon)
svg_png(icon, OUT / "01-icon-1024.png")

poster = OUT / "sources/poster-imagegen.png"
assert poster.exists(), "Copy the generated poster master to sources/poster-imagegen.png first"
image = Image.open(poster).convert("RGB")
# Exact platform export, not a semantic edit of the generated artwork.
image = image.resize((2560, 1440), Image.Resampling.LANCZOS)
image.save(OUT / "02-poster-2560x1440.jpg", quality=95, subsampling=0, optimize=True)

screenshots = [
    ("03-screenshot-room.png", "space-bend-emulator-scan-ready-20261105.png", "2026-11-05"),
    ("04-screenshot-loop.png", "space-bend-stable-loop.png", "2026-11-02"),
]
exports = []
for output, source, date in screenshots:
    path = ROOT / "output/screenshots" / source
    source_image = Image.open(path)
    assert source_image.size == (2160, 2158)
    # The complete primary panel fits inside source y=[700,1915]; the unused
    # ceiling and nearest floor are outside this viewport. No horizontal crop.
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="2560" height="1440" viewBox="0 0 2560 1440">
    <svg width="2560" height="1440" viewBox="0 700 2160 1215" preserveAspectRatio="xMidYMid meet">
    <image width="2160" height="2158" href="{embedded(path)}"/></svg>
    <rect y="1378" width="2560" height="62" fill="#08121b" fill-opacity=".9"/>
    <text x="48" y="1419" fill="#d2e5ec" font-family="PingFang SC,Arial,sans-serif" font-size="27">PICO 6.1 模拟器运行画面 · Space Bend 1.0</text>
    <text x="2512" y="1419" text-anchor="end" fill="#a5bdc7" font-family="Arial" font-size="24">{date}</text>
    </svg>'''
    svg_png(svg, OUT / output)
    exports.append({"file": output, "source": str(path.relative_to(ROOT)),
                    "source_sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
                    "source_viewport": [0, 700, 2160, 1215], "disclosure_strip": [0, 1378, 2560, 62]})

records = []
for name, expected, limit in [
    ("01-icon-1024.png", (1024, 1024), 4_000_000),
    ("02-poster-2560x1440.jpg", (2560, 1440), 5_000_000),
    ("03-screenshot-room.png", (2560, 1440), 5_000_000),
    ("04-screenshot-loop.png", (2560, 1440), 5_000_000),
]:
    path = OUT / name
    with Image.open(path) as check:
        check.load()
        assert check.size == expected, (name, check.size)
    assert path.stat().st_size <= limit, (name, path.stat().st_size)
    records.append({"file": name, "width": expected[0], "height": expected[1],
                    "bytes": path.stat().st_size, "limit_bytes": limit,
                    "sha256": hashlib.sha256(path.read_bytes()).hexdigest()})

(OUT / "image-specs.json").write_text(json.dumps({"files": records, "screenshot_provenance": exports}, ensure_ascii=False, indent=2) + "\n")
(OUT / "SHA256SUMS.txt").write_text("".join(f"{r['sha256']}  {r['file']}\n" for r in records))
print(json.dumps(records, ensure_ascii=False, indent=2))
