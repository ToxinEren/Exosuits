#!/usr/bin/env python3
"""Genera in modo deterministico i dati per versione di Minecraft (solo stdlib): ricette al banco da lavoro, i loro
avanzamenti di sblocco (design par. 12.12), le tabelle di bottino del caricatore e della stazione e il tag del piccone
(design M4 par. 5.1). La stazione fa cadere l'oggetto solo dalla meta' principale: la voce della sua tabella ha una
condizione block_state_property, che validate() controlla (il blocco della tabella, properties non vuote e di testo).

Uso, dalla radice del repository:
    python3 -I tools/gen_data.py               scrive nei sorgenti (src/main/resources-mc1.20.1/data/... e
                                               src/main/resources-mc1.21.1/data/...)
    python3 -I tools/gen_data.py --out <dir>   scrive gli stessi file sotto <dir>, con i percorsi relativi a src/main:
                                               lo usa tools/check_data.sh per confrontarli con quelli committati
    python3 -I tools/gen_data.py --selftest    prove negative di validate(): in memoria, un difetto alla volta

I due alberi hanno le stesse ricette, gli stessi sblocchi, lo stesso bottino e lo stesso tag: cambiano i nomi delle
cartelle (al plurale su 1.20.1, al singolare su 1.21.1) e la chiave dell'oggetto nel risultato di una ricetta ("item"
su 1.20.1, "id" su 1.21.1). Una cartella con il nome dell'altra versione il gioco la ignora in silenzio: per questo
validate() gira prima di ogni scrittura, e se rifiuta qualcosa non si scrive nessun file.

Per aggiungere una ricetta: una riga in RECIPES (e in INGREDIENTS se serve un simbolo nuovo), poi
python3 -I tools/gen_data.py e l'elenco dei file in tools/check_jars.sh (f3).
"""
import argparse
import copy
import json
import os
import re
import sys

MAIN = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main")
MOD = "redstone_exoframes"

# Simbolo nelle righe di RECIPES -> id dell'oggetto. Id semplici, senza i tag convenzionali (diversi fra i due loader
# di 1.20.1). Il vuoto si scrive EMPTY nelle righe e uno spazio nel file.
EMPTY = "_"
INGREDIENTS = {
    "I": "minecraft:iron_ingot",
    "C": "minecraft:copper_ingot",
    "R": "minecraft:redstone_block",
    "r": "minecraft:redstone",
    "Q": "minecraft:quartz",
    "T": "minecraft:crafting_table",
}

# (oggetto, categoria del libro delle ricette, righe della forma). L'id della ricetta e' redstone_exoframes:<oggetto>,
# quello del suo sblocco redstone_exoframes:recipes/<categoria>/<oggetto>; il risultato e' sempre uno.
RECIPES = (
    ("exoframe", "equipment", ("ICI", "IRI", "I_I")),
    ("scrap_helmet", "equipment", ("ICI", "I_I")),
    ("scrap_torso", "equipment", ("I_I", "ICI", "III")),
    ("scrap_arm", "equipment", ("CI", "_I", "_I")),
    ("scrap_leg", "equipment", ("I_", "I_", "IC")),
    ("redstone_cell", "redstone", ("_C_", "rQr", "_C_")),
    ("charger", "redstone", ("ICI", "CRC", "III")),
    ("station", "misc", ("CCC", "ITI", "IrI")),
)

# I blocchi della mod: (nome, proprieta' dello stato da cui cade l'oggetto, o None se cade da ogni stato). Ciascuno ha
# la sua tabella di bottino, redstone_exoframes:blocks/<nome>, e sta nel tag minecraft:mineable/pickaxe. La stazione
# fa cadere l'oggetto solo dalla meta' principale, come il letto dalla testa (design M4 par. 5.1): una stazione rotta
# ne lascia al piu' una.
BLOCKS = (
    ("charger", None),
    ("station", {"part": "main"}),
)

# Le categorie che il libro delle ricette conosce per una ricetta del banco da lavoro.
CATEGORIES = ("building", "redstone", "equipment", "misc")

# I tipi di file e, per versione, la cartella di ciascuno sotto data/<spazio dei nomi>/. I tag degli oggetti oggi non
# li scrive nessuno, ma la loro cartella e' qui perche' validate() la riconosca nell'albero sbagliato.
KINDS = ("recipe", "advancement", "loot_table", "block_tag", "item_tag")
VERSIONS = {
    "1.20.1": {
        "root": "resources-mc1.20.1/data",
        "recipe": "recipes",
        "advancement": "advancements",
        "loot_table": "loot_tables",
        "block_tag": "tags/blocks",
        "item_tag": "tags/items",
        "result": "item",
    },
    "1.21.1": {
        "root": "resources-mc1.21.1/data",
        "recipe": "recipe",
        "advancement": "advancement",
        "loot_table": "loot_table",
        "block_tag": "tags/block",
        "item_tag": "tags/item",
        "result": "id",
    },
}

ID = re.compile(r"[a-z0-9_.-]+:[a-z0-9_./-]+")


def _path(version, kind, namespace, name):
    v = VERSIONS[version]
    return "%s/%s/%s/%s.json" % (v["root"], namespace, v[kind], name)


def _unlock_name(category, name):
    return "recipes/%s/%s" % (category, name)


def build(version):
    """I file di una versione: percorso relativo a src/main (con "/") -> contenuto, ancora da serializzare."""
    result_key = VERSIONS[version]["result"]
    files = {}
    for name, category, rows in RECIPES:
        recipe_id = "%s:%s" % (MOD, name)
        symbols = sorted(set("".join(rows)) - {EMPTY})
        missing = [s for s in symbols if s not in INGREDIENTS]
        if missing:
            raise ValueError("ricetta %s: simbolo %r senza ingrediente in INGREDIENTS" % (name, missing[0]))
        files[_path(version, "recipe", MOD, name)] = {
            "type": "minecraft:crafting_shaped",
            "category": category,
            "key": {s: {"item": INGREDIENTS[s]} for s in symbols},
            "pattern": [row.replace(EMPTY, " ") for row in rows],
            "result": {"count": 1, result_key: recipe_id},
        }
        # Lo sblocco: basta uno dei due criteri (avere un lingotto di rame, o avere gia' la ricetta), come fa il gioco.
        # La lista di id dentro "items" e' la forma che tutte e due le versioni leggono.
        files[_path(version, "advancement", MOD, _unlock_name(category, name))] = {
            "parent": "minecraft:recipes/root",
            "criteria": {
                "has_copper_ingot": {
                    "trigger": "minecraft:inventory_changed",
                    "conditions": {"items": [{"items": ["minecraft:copper_ingot"]}]},
                },
                "has_the_recipe": {
                    "trigger": "minecraft:recipe_unlocked",
                    "conditions": {"recipe": recipe_id},
                },
            },
            "requirements": [["has_copper_ingot", "has_the_recipe"]],
            "rewards": {"recipes": [recipe_id]},
            "sends_telemetry_event": False,
        }
    block_ids = []
    for name, properties in BLOCKS:
        block_id = "%s:%s" % (MOD, name)
        block_ids.append(block_id)
        # L'oggetto del blocco, uno; con la condizione della meta' solo dove serve. La pool vuole che il blocco
        # sopravviva all'esplosione, come le tabelle del gioco base.
        entry = {"type": "minecraft:item", "name": block_id}
        if properties is not None:
            entry["conditions"] = [{
                "condition": "minecraft:block_state_property",
                "block": block_id,
                "properties": dict(properties),
            }]
        files[_path(version, "loot_table", MOD, "blocks/" + name)] = {
            "type": "minecraft:block",
            "pools": [
                {
                    "rolls": 1,
                    "entries": [entry],
                    "conditions": [{"condition": "minecraft:survives_explosion"}],
                }
            ],
        }
    # Il tag minecraft:mineable/pickaxe sta nello spazio dei nomi del gioco: il file si aggiunge ai suoi valori.
    files[_path(version, "block_tag", "minecraft", "mineable/pickaxe")] = {"replace": False, "values": block_ids}
    return files


def serialize(obj):
    """Il testo di un file: chiavi in ordine, due spazi, solo ASCII, una riga vuota in fondo. Sempre gli stessi byte."""
    return json.dumps(obj, indent=2, sort_keys=True, ensure_ascii=True) + "\n"


def _fail(path, problem):
    raise ValueError("%s: %s" % (path, problem))


def _is_id(value):
    return isinstance(value, str) and ID.fullmatch(value) is not None


def _require_id(path, what, value):
    if not _is_id(value):
        _fail(path, "%s %r non ha la forma spazio:percorso" % (what, value))


def _classify(version, path):
    """Il tipo e l'id di un file dal suo percorso; rifiuta le cartelle dell'altra versione e quelle sconosciute."""
    v = VERSIONS[version]
    prefix = v["root"] + "/"
    if not path.startswith(prefix) or not path.endswith(".json"):
        _fail(path, "non e' un file .json sotto %s" % v["root"])
    namespace, _, rest = path[len(prefix):-len(".json")].partition("/")
    for kind in KINDS:
        folder = v[kind] + "/"
        if rest.startswith(folder) and len(rest) > len(folder):
            file_id = "%s:%s" % (namespace, rest[len(folder):])
            _require_id(path, "l'id", file_id)
            return kind, file_id
    for other, o in VERSIONS.items():
        for kind in KINDS:
            if other != version and rest.startswith(o[kind] + "/"):
                _fail(path, "cartella di dati di un'altra versione: %s/ e' di %s, su %s il gioco la ignora"
                      % (o[kind], other, version))
    _fail(path, "non sta in una cartella di dati conosciuta di %s" % version)


def _validate_recipe(version, path, recipe):
    result_key = VERSIONS[version]["result"]
    if recipe.get("type") != "minecraft:crafting_shaped":
        _fail(path, "tipo %r invece di minecraft:crafting_shaped" % (recipe.get("type"),))
    if recipe.get("category") not in CATEGORIES:
        _fail(path, "categoria %r sconosciuta (ammesse: %s)" % (recipe.get("category"), ", ".join(CATEGORIES)))
    pattern = recipe.get("pattern")
    if (not isinstance(pattern, list) or not 1 <= len(pattern) <= 3
            or not all(isinstance(row, str) and 1 <= len(row) <= 3 for row in pattern)
            or len({len(row) for row in pattern}) != 1):
        _fail(path, "pattern %r non rettangolare o fuori da 1x1 - 3x3" % (pattern,))
    key = recipe.get("key")
    if not isinstance(key, dict):
        _fail(path, "key assente")
    used = set("".join(pattern)) - {" "}
    for symbol in sorted(used - set(key)):
        _fail(path, "il simbolo %r di pattern non ha la sua chiave" % symbol)
    for symbol in sorted(set(key) - used):
        _fail(path, "la chiave %r non e' usata da pattern" % symbol)
    for symbol, ingredient in sorted(key.items()):
        if not isinstance(ingredient, dict) or set(ingredient) != {"item"}:
            _fail(path, "l'ingrediente %r non e' {\"item\": id}" % symbol)
        _require_id(path, "l'ingrediente %r" % symbol, ingredient["item"])
    result = recipe.get("result")
    if not isinstance(result, dict) or set(result) != {"count", result_key}:
        _fail(path, "result %r: su %s servono le chiavi count e %s" % (result, version, result_key))
    if type(result["count"]) is not int or result["count"] != 1:
        _fail(path, "count %r nel risultato invece di 1" % (result["count"],))
    _require_id(path, "il risultato", result[result_key])


def _validate_loot_table(path, file_id, table):
    # Il blocco della tabella: da redstone_exoframes:blocks/<nome> a redstone_exoframes:<nome>.
    block_id = file_id.replace(":blocks/", ":", 1)
    for pool in table.get("pools", []):
        for entry in pool.get("entries", []):
            _require_id(path, "l'oggetto", entry.get("name"))
            for condition in entry.get("conditions", []):
                if condition.get("condition") != "minecraft:block_state_property":
                    continue
                if condition.get("block") != block_id:
                    _fail(path, "la condizione block_state_property nomina %r invece del blocco della tabella %s"
                          % (condition.get("block"), block_id))
                properties = condition.get("properties")
                if (not isinstance(properties, dict) or not properties
                        or not all(isinstance(value, str) for value in properties.values())):
                    _fail(path, "properties vuote o non di testo nella condizione block_state_property: %r"
                          % (properties,))


def _validate_tag(path, tag):
    if "remove" in tag:
        _fail(path, "un file di tag non porta mai la chiave remove")
    if tag.get("replace") is not False:
        _fail(path, "replace deve essere false (true toglierebbe i valori degli altri)")
    for value in tag.get("values", []):
        _require_id(path, "il valore", value)


def _validate_version(version, files):
    """Controlla i file di una versione; restituisce tipo -> {id: percorso}."""
    ids = {kind: {} for kind in KINDS}
    recipes = []
    for path in sorted(files):
        obj = files[path]
        kind, file_id = _classify(version, path)
        if not isinstance(obj, dict):
            _fail(path, "il contenuto non e' un oggetto JSON")
        ids[kind][file_id] = path
        if kind == "recipe":
            _validate_recipe(version, path, obj)
            recipes.append((path, file_id, obj))
        elif kind == "loot_table":
            _validate_loot_table(path, file_id, obj)
        elif kind in ("block_tag", "item_tag"):
            _validate_tag(path, obj)
    for path, recipe_id, recipe in recipes:
        namespace, _, name = recipe_id.partition(":")
        unlock_id = "%s:%s" % (namespace, _unlock_name(recipe["category"], name))
        unlock_path = ids["advancement"].get(unlock_id)
        if unlock_path is None:
            _fail(path, "la ricetta %s non ha il suo sblocco %s" % (recipe_id, unlock_id))
        rewards = files[unlock_path].get("rewards")
        rewarded = rewards.get("recipes") if isinstance(rewards, dict) else None
        if rewarded != [recipe_id]:
            _fail(unlock_path, "i premi %r non sono la ricetta %s" % (rewarded, recipe_id))
    return ids


def validate(files_by_version):
    """Rifiuta con ValueError (il messaggio comincia con il percorso del file) cio' che il gioco non leggerebbe, o
    leggerebbe su una versione sola.

    files_by_version: versione -> {percorso relativo a src/main: contenuto}, come li da' build(). Impone: in ogni
    ricetta un risultato con count 1 e la chiave della versione ("item" o "id"), una categoria fra CATEGORIES, un
    pattern rettangolare da 1x1 a 3x3, una chiave per ogni simbolo di pattern diverso dallo spazio e nessuna chiave
    non usata; ogni id nella forma spazio:percorso; per ogni ricetta il suo sblocco, con lo stesso id nei premi; in
    ogni condizione minecraft:block_state_property di una voce di una tabella di bottino, block uguale al blocco della
    tabella (redstone_exoframes:<nome> per redstone_exoframes:blocks/<nome>) e properties un oggetto non vuoto di
    stringhe; nei file dei tag nessun remove e replace false; nessun percorso nelle cartelle dell'altra versione; gli
    stessi id di ricette, sblocchi, bottini e tag sulle versioni.
    """
    if set(files_by_version) != set(VERSIONS):
        raise ValueError("versioni %s invece di %s" % (sorted(files_by_version), sorted(VERSIONS)))
    ids = {version: _validate_version(version, files_by_version[version]) for version in VERSIONS}
    for version in VERSIONS:
        for other in VERSIONS:
            for kind in KINDS:
                for file_id in sorted(set(ids[version][kind]) - set(ids[other][kind])):
                    _fail(ids[version][kind][file_id], "%s c'e' su %s e manca su %s" % (file_id, version, other))


def build_all():
    """I file di tutte le versioni, gia' passati da validate()."""
    files_by_version = {version: build(version) for version in VERSIONS}
    validate(files_by_version)
    return files_by_version


# --- prove negative -------------------------------------------------------------------------------------------------

def _defect_count(files):
    files["1.20.1"][_path("1.20.1", "recipe", MOD, "exoframe")]["result"]["count"] = 2


def _defect_category(files):
    files["1.21.1"][_path("1.21.1", "recipe", MOD, "scrap_helmet")]["category"] = "tools"


def _defect_symbol_without_key(files):
    del files["1.20.1"][_path("1.20.1", "recipe", MOD, "charger")]["key"]["R"]


def _defect_unused_key(files):
    files["1.21.1"][_path("1.21.1", "recipe", MOD, "scrap_arm")]["key"]["Q"] = {"item": "minecraft:quartz"}


def _defect_remove_in_tag(files):
    files["1.21.1"][_path("1.21.1", "block_tag", "minecraft", "mineable/pickaxe")]["remove"] = ["minecraft:stone"]


def _defect_recipe_in_one_version(files):
    del files["1.21.1"][_path("1.21.1", "recipe", MOD, "redstone_cell")]


def _defect_other_version_folder(files):
    recipe = files["1.20.1"].pop(_path("1.20.1", "recipe", MOD, "scrap_leg"))
    files["1.20.1"]["resources-mc1.20.1/data/%s/recipe/scrap_leg.json" % MOD] = recipe


def _station_half_condition(files, version):
    """La condizione della meta' nella voce della tabella di bottino della stazione, nei file della versione."""
    table = files[version][_path(version, "loot_table", MOD, "blocks/station")]
    return table["pools"][0]["entries"][0]["conditions"][0]


def _defect_half_condition_other_block(files):
    _station_half_condition(files, "1.21.1")["block"] = "%s:charger" % MOD


def _defect_half_condition_empty_properties(files):
    _station_half_condition(files, "1.20.1")["properties"] = {}


# (descrizione, difetto, il file che il messaggio deve nominare, un pezzo del messaggio atteso): una segnalazione per
# un altro motivo non conta.
DEFECTS = (
    ("count 2 nel risultato", _defect_count,
     "resources-mc1.20.1/data/redstone_exoframes/recipes/exoframe.json", "count 2"),
    ("categoria tools", _defect_category,
     "resources-mc1.21.1/data/redstone_exoframes/recipe/scrap_helmet.json", "categoria 'tools'"),
    ("un simbolo senza chiave", _defect_symbol_without_key,
     "resources-mc1.20.1/data/redstone_exoframes/recipes/charger.json", "il simbolo 'R'"),
    ("una chiave non usata", _defect_unused_key,
     "resources-mc1.21.1/data/redstone_exoframes/recipe/scrap_arm.json", "la chiave 'Q'"),
    ("remove in un tag", _defect_remove_in_tag,
     "resources-mc1.21.1/data/minecraft/tags/block/mineable/pickaxe.json", "remove"),
    ("una ricetta tolta da una sola versione", _defect_recipe_in_one_version,
     "resources-mc1.20.1/data/redstone_exoframes/recipes/redstone_cell.json", "manca su 1.21.1"),
    ("un file di 1.20.1 sotto recipe/", _defect_other_version_folder,
     "resources-mc1.20.1/data/redstone_exoframes/recipe/scrap_leg.json", "un'altra versione"),
    ("la condizione della meta' che nomina un altro blocco", _defect_half_condition_other_block,
     "resources-mc1.21.1/data/redstone_exoframes/loot_table/blocks/station.json", "invece del blocco della tabella"),
    ("la condizione della meta' con properties vuote", _defect_half_condition_empty_properties,
     "resources-mc1.20.1/data/redstone_exoframes/loot_tables/blocks/station.json", "properties"),
)


def selftest():
    """Inietta un difetto alla volta in una copia dei file (in memoria) e pretende che validate() lo rifiuti."""
    clean = build_all()
    failed = 0
    for name, defect, path, fragment in DEFECTS:
        files = copy.deepcopy(clean)
        defect(files)
        try:
            validate(files)
        except ValueError as e:
            message = str(e)
            if message.startswith(path + ": ") and fragment in message:
                continue
            print("FAIL: %s: segnalato per un altro motivo: %s" % (name, message), file=sys.stderr)
        else:
            print("FAIL: %s: difetto non segnalato" % name, file=sys.stderr)
        failed += 1
    if failed:
        return 1
    print("OK selftest (%d difetti segnalati)" % len(DEFECTS))
    return 0


def main():
    ap = argparse.ArgumentParser(description="Genera i dati per versione (ricette, sblocchi, bottino, tag).")
    group = ap.add_mutually_exclusive_group()
    group.add_argument("--out", default=None, help="cartella al posto di src/main (stessi percorsi relativi)")
    group.add_argument("--selftest", action="store_true", help="prove negative di validate(), senza scrivere nulla")
    args = ap.parse_args()
    if args.selftest:
        return selftest()
    try:
        files_by_version = build_all()
    except ValueError as e:
        print("FAIL: %s" % e, file=sys.stderr)
        return 1
    base = MAIN if args.out is None else args.out
    count = 0
    for version in VERSIONS:
        for rel, obj in sorted(files_by_version[version].items()):
            path = os.path.join(base, *rel.split("/"))
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w", encoding="ascii", newline="\n") as f:
                f.write(serialize(obj))
            count += 1
    print("%d file di dati scritti sotto %s" % (count, os.path.normpath(base)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
