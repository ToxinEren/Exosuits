"""Design del modello. Ogni modulo di questa cartella espone ORDER (intero) e build(model): build.py li chiama tutti
in ordine di ORDER (a parita', di nome), quindi aggiungere un design vuol dire aggiungere un file. `only` (elenco di
nomi di modulo) limita il caricamento a quei design, per esempio per le anteprime del telaio nudo."""
import importlib
import os


def load_all(only=None):
    here = os.path.dirname(os.path.abspath(__file__))
    names = sorted(fn[:-3] for fn in os.listdir(here) if fn.endswith('.py') and not fn.startswith('_'))
    if only is not None:
        unknown = sorted(set(only) - set(names))
        if unknown:
            raise ValueError(f'design sconosciuti: {", ".join(unknown)} (disponibili: {", ".join(names)})')
        names = [n for n in names if n in only]
    mods = []
    for name in names:
        mod = importlib.import_module('designs.' + name)
        mods.append((getattr(mod, 'ORDER', 100), name + '.py', mod))
    return [m for _, _, m in sorted(mods, key=lambda t: (t[0], t[1]))]
