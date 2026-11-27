"""Build the current PBR Tripo preview; original flat-color generator is retired."""
from pathlib import Path
import subprocess
root=Path(__file__).resolve().parents[2]
subprocess.run(["node",str(root/"tools/assets/build_tripo_review.mjs")],cwd=root,check=True)
