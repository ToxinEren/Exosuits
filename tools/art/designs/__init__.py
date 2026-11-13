"""Design del modello. Ogni modulo di questa cartella espone ORDER (intero) e build(model): build.py li chiama tutti
in ordine di ORDER (a parita', di nome), quindi aggiungere un design vuol dire aggiungere un file."""
import importlib
import os


def load_all():
    here = os.path.dirname(os.path.abspath(__file__))
    mods = []
    for fn in sorted(os.listdir(here)):
        if fn.endswith('.py') and not fn.startswith('_'):
            mod = importlib.import_module('designs.' + fn[:-3])
            mods.append((getattr(mod, 'ORDER', 100), fn, mod))
    return [m for _, _, m in sorted(mods, key=lambda t: (t[0], t[1]))]
