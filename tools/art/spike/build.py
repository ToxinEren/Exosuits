"""Build models, check them, paint atlases, export JSON / PNG / geo.json. Usage: python3 build.py [A B C]"""
import sys

from designs import DESIGNS
from paint import paint_model

which = sys.argv[1:] or list(DESIGNS)
for k in which:
    m = DESIGNS[k]()
    m.save(f'build/{k}', paint_model)
    lo, hi = m.bbox()
    size = hi - lo
    issues = m.check()
    print(f'{k}: {len(m.boxes)} boxes, atlas {m.atlas_w}x{m.atlas_h} (used h {m.used_h}), '
          f'bbox {lo.tolist()} -> {hi.tolist()} size {size.tolist()}')
    for a, b, f, p in issues:
        print(f'   coplanar {a} / {b} on {f} @ {p}')
