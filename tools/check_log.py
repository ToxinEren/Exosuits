#!/usr/bin/env python3
"""Avvisi ed errori del mod in un log di Minecraft, confrontati con l'elenco di quelli voluti.

    python3 -I tools/check_log.py <ambito> <log>...

<ambito> e' "gametest" (il log di un'esecuzione di tools/run_gametests.sh) o "client" (i log di server e client di
tools/client/client_shot.sh). Per ogni log si guardano:

- le righe WARN, ERROR e FATAL del gioco (forma "[ora] [thread/LIVELLO] ...", la stessa su Forge, NeoForge e Fabric)
  insieme alle righe di traccia che le seguono (eccezione, "at ...", "Caused by", "... N more"), quando la riga o la
  sua traccia nomina il mod: "exoframe" in qualunque forma (il logger "Redstone Exoframes", l'id redstone_exoframes,
  le classi com.toxineren.exoframes, il file del registro, l'entita' ExoframeEntity che vanilla nomina per esempio in
  "UUID of added entity already exists");
- in qualunque riga, a ogni livello: un errore di applicazione dei mixin (InvalidInjectionException,
  MixinApplyError, "Mixin apply ... failed") o una traccia che passa dal codice del mod (com.toxineren) fuori da una
  riga ammessa.

Una riga trovata passa solo se il suo messaggio (quello che segue il nome del logger) corrisponde per intero a una
riga dell'elenco tools/log_allowlist.txt per lo stesso ambito, e solo fino al numero di volte che quella riga ammette
in un log. Un log in cui non si riconosce nemmeno una riga con livello nella forma attesa (vuoto, o il gioco ha
cambiato formato) e' un problema: non si e' potuto controllare nulla. Tutto il resto e' stampato su stdout, una riga
per problema, e l'uscita e' 1; senza problemi non si stampa niente e l'uscita e' 0. Uscita 2 per un uso sbagliato o un
elenco scritto male.

Solo libreria standard di Python 3.
"""
import os
import re
import sys

ALLOWLIST = os.path.join(os.path.dirname(os.path.abspath(__file__)), "log_allowlist.txt")
SCOPES = ("gametest", "client")

# Riga di log del gioco con livello: "[ora] [thread/LIVELLO] [logger/marker]: messaggio" (Forge, NeoForge) oppure
# "[ora] [thread/LIVELLO] (logger) messaggio" (Fabric).
HEAD = re.compile(r"^\[[0-9:.]+\] \[[^\]]*/(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\] "
                  r"(?:\[(?P<l1>[^\]]*)\]: |\((?P<l2>[^)]*)\) )?(?P<msg>.*)$")
# Righe di una traccia che seguono la riga che l'ha stampata.
TRACE = re.compile(r"^(\s+\S|Caused by: |Suppressed: |[\w.$]+(Exception|Error|Throwable)\b)")
MOD = re.compile(r"exoframe", re.IGNORECASE)
ALWAYS = re.compile(r"com\.toxineren|InvalidInjectionException|MixinApplyError|Mixin apply .*failed")


def fail_usage(message):
    print(f"ERRORE: {message}", file=sys.stderr)
    sys.exit(2)


def read_allowlist(path, scope):
    """Le righe dell'elenco per l'ambito: [(massimo, espressione compilata, testo della riga)]."""
    macros = {}
    rules = []
    with open(path, encoding="utf-8") as f:
        for number, raw in enumerate(f, 1):
            line = raw.rstrip("\n")
            if not line.strip() or line.lstrip().startswith("#"):
                continue
            where = f"{path}:{number}"
            if line.startswith("="):
                name, _, value = line[1:].strip().partition(" ")
                if not re.fullmatch(r"[A-Z_]+", name) or not value.strip():
                    fail_usage(f"{where}: definizione scritta male (= NOME espressione)")
                macros[name] = value.strip()
                continue
            parts = line.split(" | ")
            if len(parts) < 4:
                fail_usage(f"{where}: servono quattro campi separati da ' | ' (ambito | massimo | messaggio | chi)")
            rule_scope, most, producer = parts[0].strip(), parts[1].strip(), parts[-1].strip()
            pattern = " | ".join(parts[2:-1]).strip()
            if rule_scope not in SCOPES:
                fail_usage(f"{where}: ambito '{rule_scope}' sconosciuto (gametest o client)")
            if not most.isdigit() or int(most) < 1:
                fail_usage(f"{where}: il massimo deve essere un numero intero da 1 in su")
            if not producer:
                fail_usage(f"{where}: manca chi produce la riga (il test o l'avvio che la scrive)")
            expanded = re.sub(r"\{([A-Z_]+)\}", lambda m: macros.get(m.group(1), m.group(0)), pattern)
            if re.search(r"\{[A-Z_]+\}", expanded):
                fail_usage(f"{where}: nome non definito in {pattern}")
            if re.fullmatch(r"[.^$*+?\\sSdDwW]*", expanded) or expanded.startswith(".*") or expanded.endswith(".*"):
                fail_usage(f"{where}: espressione troppo larga: deve descrivere il messaggio intero")
            try:
                compiled = re.compile(expanded)
            except re.error as e:
                fail_usage(f"{where}: espressione non valida: {e}")
            if rule_scope == scope:
                rules.append([int(most), compiled, line])
    return rules


def records(lines):
    """Le righe con livello del log, ciascuna con la traccia che la segue: (numero, livello, messaggio, testo)."""
    current = None
    for number, line in enumerate(lines, 1):
        head = HEAD.match(line)
        if head:
            if current:
                yield current
            current = [number, head.group(1), head.group("msg"), line]
        elif current and TRACE.match(line):
            current[3] += "\n" + line
        else:
            if current:
                yield current
                current = None
            yield [number, None, line, line]
    if current:
        yield current


def check(scope, path, rules):
    problems = []
    counts = [0] * len(rules)
    recognised = 0
    with open(path, encoding="utf-8", errors="replace") as f:
        lines = f.read().splitlines()
    for number, level, message, text in records(lines):
        if level is not None:
            recognised += 1
        named = level in ("WARN", "ERROR", "FATAL") and MOD.search(text)
        if not named and not ALWAYS.search(text):
            continue
        allowed = False
        if level in ("WARN", "ERROR", "FATAL"):
            for i, (most, compiled, _) in enumerate(rules):
                if compiled.fullmatch(message):
                    counts[i] += 1
                    allowed = True
                    break
        if not allowed:
            problems.append(f"{path}:{number}: riga non ammessa: {text.splitlines()[0][:400]}")
    for (most, _, line), count in zip(rules, counts):
        if count > most:
            problems.append(f"{path}: {count} volte la riga ammessa al massimo {most} volte: {line}")
    if recognised == 0:
        problems.append(f"{path}: nessuna riga con livello nella forma attesa ([ora] [thread/LIVELLO] ...) su "
                        f"{len(lines)} righe: il log e' vuoto o il gioco ha cambiato formato, e nulla si e' potuto "
                        f"controllare")
    return problems


def main(argv):
    if len(argv) < 3 or argv[1] not in SCOPES:
        fail_usage("uso: python3 -I tools/check_log.py gametest|client <log>...")
    scope = argv[1]
    rules = read_allowlist(ALLOWLIST, scope)
    problems = []
    for path in argv[2:]:
        if not os.path.isfile(path):
            fail_usage(f"log non trovato: {path}")
        problems += check(scope, path, rules)
    for p in problems:
        print(p)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
