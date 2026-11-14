#!/usr/bin/env bash
# Controlli sui jar finali (dalla radice del repository): bash tools/check_jars.sh
set -eu

cd "$(dirname "$0")/.."

fail=0
err() { echo "FAIL: $1: $2" >&2; fail=1; }

command -v python3 >/dev/null 2>&1 || { echo "FAIL: python3 non trovato: serve per leggere i metadati e per il controllo di privacy" >&2; exit 1; }
# tomllib (per leggere neoforge.mods.toml) c'e' da Python 3.11: con uno piu' vecchio il controllo (e2) del jar NeoForge
# fallirebbe con un messaggio che non dice la causa.
if ! python3 -c 'import sys; sys.exit(0 if sys.version_info >= (3, 11) else 1)'; then
    pyver=$(python3 -c 'import platform; print(platform.python_version())')
    echo "FAIL: python3 $pyver troppo vecchio: serve Python 3.11 o piu' recente (tomllib)" >&2
    exit 1
fi

shopt -s nullglob
dir=${1:-build/libs/all}
jars=("$dir"/*.jar)

# L'insieme dei jar deve essere esattamente quello atteso.
expected=(
    redstone-exoframes-fabric-0.1.0+mc1.20.1.jar
    redstone-exoframes-fabric-0.1.0+mc1.21.1.jar
    redstone-exoframes-forge-0.1.0+mc1.20.1.jar
    redstone-exoframes-neoforge-0.1.0+mc1.21.1.jar
)
for e in "${expected[@]}"; do
    [ -f "$dir/$e" ] || err "$e" "jar atteso mancante in $dir"
done
for jar in "${jars[@]}"; do
    n=$(basename "$jar")
    found=0
    for e in "${expected[@]}"; do [ "$n" = "$e" ] && found=1; done
    [ "$found" -eq 1 ] || err "$n" "jar inatteso in $dir"
done
[ "$fail" -eq 0 ] || exit 1

for jar in "${jars[@]}"; do
    name=$(basename "$jar")
    case "$name" in
        *-fabric-*) loader=fabric ;;
        *-neoforge-*) loader=neoforge ;;
        *-forge-*) loader=forge ;;
        *) err "$name" "loader non riconoscibile dal nome"; continue ;;
    esac
    case "$name" in
        *+mc1.20.1.jar) mc=1.20.1; pack=15 ;;
        *+mc1.21.1.jar) mc=1.21.1; pack=34 ;;
        *) err "$name" "versione Minecraft non riconoscibile dal nome"; continue ;;
    esac

    listing=$(unzip -Z1 "$jar")

    # (a) un solo file di metadati, quello del loader del nome.
    case "$loader" in
        fabric) meta=fabric.mod.json ;;
        forge) meta=META-INF/mods.toml ;;
        neoforge) meta=META-INF/neoforge.mods.toml ;;
    esac
    for f in fabric.mod.json META-INF/mods.toml META-INF/neoforge.mods.toml; do
        if [ "$f" = "$meta" ]; then
            echo "$listing" | grep -qx "$f" || err "$name" "manca $f"
        else
            ! echo "$listing" | grep -qx "$f" || err "$name" "file di metadati estraneo $f"
        fi
    done

    # (b) pack_format.
    if ! unzip -p "$jar" pack.mcmeta 2>/dev/null | tr -d '[:space:]' | grep -q "\"pack_format\":$pack[,}]"; then
        err "$name" "pack_format diverso da $pack"
    fi

    # (c) intervallo Minecraft.
    case "$loader-$mc" in
        fabric-1.20.1) want='"minecraft": ">=1.20.1 <1.21"' ;;
        fabric-1.21.1) want='"minecraft": ">=1.21 <1.22"' ;;
        forge-1.20.1) want='versionRange="[1.20.1,1.21)"' ;;
        neoforge-1.21.1) want='versionRange="[1.21.1,1.22)"' ;;
        *) err "$name" "combinazione loader/mc inattesa $loader-$mc"; continue ;;
    esac
    if ! unzip -p "$jar" "$meta" 2>/dev/null | grep -qF "$want"; then
        err "$name" "intervallo Minecraft atteso $want assente in $meta"
    fi

    # (d) classe principale.
    echo "$listing" | grep -qx 'com/toxineren/exoframes/RedstoneExoframes.class' \
        || err "$name" "manca com/toxineren/exoframes/RedstoneExoframes.class"

    # (d2) le classi d'ingresso dichiarate nei metadati sono nel jar (anche l'entrypoint client di Fabric,
    #      che un avvio del server dedicato non carica mai).
    case "$loader" in
        fabric)
            entries=$(unzip -p "$jar" fabric.mod.json | python3 -c 'import json, sys
for value in json.load(sys.stdin).get("entrypoints", {}).values():
    for item in (value if isinstance(value, list) else [value]):
        print(item if isinstance(item, str) else item.get("value", ""))' 2>/dev/null) \
                || { err "$name" "fabric.mod.json assente o non leggibile"; entries=""; } ;;
        forge) entries=com.toxineren.exoframes.platform.RedstoneExoframesForge ;;
        neoforge) entries=com.toxineren.exoframes.platform.RedstoneExoframesNeoForge ;;
    esac
    [ -n "$entries" ] || err "$name" "nessuna classe d'ingresso dichiarata"
    for cls in $entries; do
        echo "$listing" | grep -qx "$(echo "$cls" | tr . /).class" \
            || err "$name" "classe d'ingresso $cls dichiarata nei metadati ma assente dal jar"
    done

    # (e) nessun refmap: con questa Loom il processore di annotazioni dei mixin e' spento e remapJar riscrive i nomi
    #     direttamente nelle annotazioni dei mixin (vedi (e2)), quindi nessun loader ne richiede uno.
    refmaps=$(echo "$listing" | grep -E '\.refmap\.json$' || true)
    [ -z "$refmaps" ] || err "$name" "refmap presente: $refmaps"

    # (e2) mixin: la configurazione e' alla radice, e' quella della versione (compatibilityLevel), richiesta, con
    #      defaultRequire 1, senza refmap; la sezione mixins (comune ai due lati) elenca esattamente la classe della
    #      grandezza reale della versione del jar (PlayerSize120Mixin o PlayerSize121Mixin), e la classe dell'altra
    #      versione non e' nel jar (la esclude build.gradle.kts); la sezione server e' vuota, la sezione client elenca
    #      esattamente le tre classi ammesse (CLIENT qui sotto, in qualunque ordine); ogni classe elencata e' nel jar
    #      con i nomi nello spazio del loader (intermediary su Fabric, SRG su Forge, Mojang su NeoForge), riconosciuti
    #      dai nomi Mojang dei bersagli; il mixin della grandezza e quello del culling portano esattamente i nomi
    #      attesi dei loro bersagli per loader e versione; i metadati del loader la dichiarano (fabric.mod.json
    #      "mixins", MixinConfigs nel manifest su Forge, [[mixins]] in neoforge.mods.toml).
    mixins=redstone_exoframes.mixins.json
    case "$mc" in
        1.20.1) level=JAVA_17; size_mixin=PlayerSize120Mixin; other_size_mixin=PlayerSize121Mixin ;;
        *) level=JAVA_21; size_mixin=PlayerSize121Mixin; other_size_mixin=PlayerSize120Mixin ;;
    esac
    case "$loader" in
        fabric) names=intermediary ;;
        forge) names=srg ;;
        neoforge) names=mojang ;;
    esac
    if ! echo "$listing" | grep -qx "$mixins"; then
        err "$name" "manca $mixins alla radice"
    else
        problems=$(python3 - "$jar" "$mixins" "$level" "$names" "$size_mixin" "$other_size_mixin" "$loader" "$mc" <<'PY'
import json, re, sys, zipfile
jar, cfg_name, level, names, size_mixin, other_size_mixin, loader, mc = sys.argv[1:]
# Nomi esatti dei bersagli della grandezza reale e del culling nel jar, per loader e versione (dopo remapJar).
CULLING = "client.LivingEntityCullingMixin"
# Le sole classi ammesse nella sezione client (lato client soltanto: un server dedicato non le carica).
CLIENT = sorted(["client.ItemInHandLayerMixin", CULLING, "client.PlayerRendererMixin"])
TARGETS = {
    ("neoforge", "1.21.1"): {"PlayerSize121Mixin": [b"getDefaultDimensions("],
                             CULLING: [b"getBoundingBoxForCulling("]},
    ("forge", "1.20.1"): {"PlayerSize120Mixin": [b"m_6972_(", b"m_6431_("], CULLING: [b"m_6921_("]},
    ("fabric", "1.20.1"): {"PlayerSize120Mixin": [b"method_18377(", b"method_18394("], CULLING: [b"method_5830("]},
    ("fabric", "1.21.1"): {"PlayerSize121Mixin": [b"method_55694("], CULLING: [b"method_5830("]},
}
# Nomi Mojang dei bersagli dei mixin: nel jar restano solo su NeoForge, altrove li sostituisce remapJar.
MOJANG = (b"setModelProperties", b"renderArmWithItem", b"getDimensions(", b"getStandingEyeHeight(",
          b"getDefaultDimensions(", b"getBoundingBoxForCulling(")
with zipfile.ZipFile(jar) as z:
    files = set(z.namelist())
    try:
        cfg = json.loads(z.read(cfg_name))
    except ValueError as e:
        print(f"{cfg_name} non e' JSON valido: {e}")
        sys.exit()
    if cfg.get("required") is not True:
        print(f"{cfg_name}: required deve essere true")
    if cfg.get("injectors", {}).get("defaultRequire") != 1:
        print(f"{cfg_name}: injectors.defaultRequire deve essere 1")
    if cfg.get("compatibilityLevel") != level:
        print(f"{cfg_name}: compatibilityLevel {cfg.get('compatibilityLevel')} invece di {level}")
    common = cfg.get("mixins") or []
    if common != [size_mixin]:
        print(f"{cfg_name}: sezione mixins {common} invece di ['{size_mixin}'] (solo la grandezza reale della versione)")
    if cfg.get("server"):
        print(f"{cfg_name}: sezione server non vuota")
    if "refmap" in cfg:
        print(f"{cfg_name}: dichiara il refmap {cfg['refmap']}, che nessun loader richiede (vedi (e))")
    client = cfg.get("client") or []
    if sorted(client) != CLIENT:
        print(f"{cfg_name}: sezione client {sorted(client)} invece di {CLIENT}")
    package = cfg.get("package", "")
    other = (package + "." + other_size_mixin).replace(".", "/") + ".class"
    if other in files:
        print(f"{other}: mixin della grandezza dell'altra versione nel jar (va escluso in build.gradle.kts)")
    targets = TARGETS.get((loader, mc))
    if targets is None:
        print(f"nessun nome atteso dei bersagli per {loader} {mc}")
        targets = {}
    for mixin, expected_names in targets.items():
        path = (package + "." + mixin).replace(".", "/") + ".class"
        if mixin not in common + client:
            print(f"{mixin} non elencato in {cfg_name}")
            continue
        if path not in files:
            continue  # segnalato dal controllo delle classi elencate, qui sotto
        data = z.read(path)
        for expected_name in expected_names:
            if expected_name not in data:
                print(f"{path}: manca il nome del bersaglio {expected_name.decode()} atteso su {loader} {mc}")
    for mixin in common + client:
        path = (package + "." + mixin).replace(".", "/") + ".class"
        if path not in files:
            print(f"classe {path} elencata in {cfg_name} ma assente dal jar")
            continue
        data = z.read(path)
        mojang = any(marker in data for marker in MOJANG)
        if names == "mojang" and not mojang:
            print(f"{path}: nomi Mojang attesi nelle annotazioni (NeoForge 1.21.1 gira con i nomi Mojang)")
        if names != "mojang" and mojang:
            print(f"{path}: nomi Mojang nelle annotazioni, non rimappati in {names}")
        if names == "intermediary" and not re.search(rb"method_\d+", data):
            print(f"{path}: nessun nome intermediary (method_N) nelle annotazioni")
        if names == "srg" and not re.search(rb"m_\d+_", data):
            print(f"{path}: nessun nome SRG (m_N_) nelle annotazioni")
PY
) || problems="controllo della configurazione dei mixin non eseguito"
        [ -z "$problems" ] || while IFS= read -r p; do err "$name" "$p"; done <<< "$problems"
    fi
    case "$loader" in
        fabric)
            unzip -p "$jar" fabric.mod.json 2>/dev/null | python3 -c 'import json, sys
sys.exit(0 if sys.argv[1] in json.load(sys.stdin).get("mixins", []) else 1)' "$mixins" \
                || err "$name" "fabric.mod.json non dichiara $mixins in \"mixins\"" ;;
        forge)
            unzip -p "$jar" META-INF/MANIFEST.MF 2>/dev/null | tr -d '\r' | grep -E '^MixinConfigs: ' | grep -qw "$mixins" \
                || err "$name" "il manifest non dichiara $mixins in MixinConfigs" ;;
        neoforge)
            unzip -p "$jar" META-INF/neoforge.mods.toml 2>/dev/null | python3 -c 'import sys, tomllib
sys.exit(0 if any(m.get("config") == sys.argv[1] for m in tomllib.load(sys.stdin.buffer).get("mixins", [])) else 1)' "$mixins" \
                || err "$name" "neoforge.mods.toml non dichiara $mixins in [[mixins]]" ;;
    esac

    # (f) icona del mod alla radice del jar (referenziata da icon/logoFile).
    echo "$listing" | grep -qx 'icon.png' || err "$name" "manca icon.png alla radice"

    # (f2) texture trasparenti del layer armatura vanilla per il materiale "redstone_exoframes_frame": servono solo
    #      su 1.20.1, dove vanilla le disegna sempre; su 1.21.1 il materiale non ha layer e non devono esserci. Il
    #      vecchio nome generico "frame" non deve tornare.
    for t in assets/minecraft/textures/models/armor/redstone_exoframes_frame_layer_1.png \
            assets/minecraft/textures/models/armor/redstone_exoframes_frame_layer_2.png; do
        if [ "$mc" = 1.20.1 ]; then
            echo "$listing" | grep -qx "$t" || err "$name" "manca $t (texture trasparente del materiale frame su 1.20.1)"
        else
            ! echo "$listing" | grep -qx "$t" || err "$name" "$t presente in un jar $mc (serve solo su 1.20.1)"
        fi
    done
    ! echo "$listing" | grep -qE '^assets/minecraft/textures/models/armor/frame_layer_[12]\.png$' \
        || err "$name" "texture con il vecchio nome generico frame_layer_*.png (il materiale e' redstone_exoframes_frame)"
done

# (g) nessun dato personale nei jar da pubblicare: percorsi locali, e-mail, metadati di immagini e suoni.
python3 tools/check_jar_privacy.py "${jars[@]}" || fail=1

[ "$fail" -eq 0 ] || exit 1
echo "OK jars"
