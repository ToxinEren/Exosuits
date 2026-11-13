#!/usr/bin/env python3
"""Controllo di privacy sui jar da pubblicare: nessun dato personale deve finire nei file distribuiti.

Uso (dalla radice del repository, lo chiama tools/check_jars.sh):

    python3 tools/check_jar_privacy.py build/libs/all/*.jar

Per ogni voce di ogni jar (anche dei jar annidati) segnala:

- percorsi locali di un computer (/home/..., /Users/..., X:\\Users\\..., /root/...);
- indirizzi e-mail;
- l'attributo Built-By nel manifest;
- metadati testuali nelle immagini PNG (tEXt, iTXt, zTXt, eXIf: gli editor ci scrivono autore e percorsi);
- commenti Vorbis nei file OGG diversi da ENCODER;
- il campo "author" nelle strutture NBT;
- le parole elencate nella variabile d'ambiente EXOFRAMES_PRIVATE_WORDS (separate da virgola, mai committate:
  per esempio nome e cognome o il nome utente del PC).

Esce con codice 1 se trova qualcosa.
"""
import gzip
import io
import os
import re
import struct
import sys
import zipfile
import zlib

# Un percorso locale comincia all'inizio di una stringa o dopo un separatore: "assets/x/models/root/a.json" e'
# una cartella delle risorse, non la home di root. Per questo i pattern rifiutano una lettera, una cifra, un punto,
# un trattino o un trattino basso subito prima.
NOT_INSIDE_A_NAME = rb'(?<![A-Za-z0-9_.-])'
# Domini di primo livello abbastanza comuni da coprire gli indirizzi personali senza scambiare per e-mail
# stringhe come "v1.0@build.ab".
TLDS = (rb'com|net|org|edu|gov|info|biz|me|io|dev|app|xyz|email|live|pro|cloud|online|eu|it|uk|de|fr|es|pt|nl|be|ch|at|'
        rb'pl|se|no|dk|fi|ie|gr|cz|ro|hu|ru|ua|us|ca|mx|br|ar|cl|au|nz|jp|cn|kr|in|za|tr|il')
PATTERNS = {
    'percorso locale /home/': re.compile(NOT_INSIDE_A_NAME + rb'/home/[A-Za-z0-9._-]+'),
    'percorso locale /Users/': re.compile(NOT_INSIDE_A_NAME + rb'/Users/[A-Za-z0-9._ -]+'),
    'percorso locale Windows': re.compile(rb'[A-Za-z]:[\\/]+Users[\\/]+[^\\/\x00-\x1f"<>|]+', re.I),
    'percorso locale /root/': re.compile(NOT_INSIDE_A_NAME + rb'/root/[A-Za-z0-9._-]+'),
    'indirizzo e-mail': re.compile(rb'[A-Za-z0-9._%+-]{2,}@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.(?:' + TLDS + rb')(?![A-Za-z0-9-])',
                                   re.I),
}
PNG_TEXT_CHUNKS = (b'tEXt', b'iTXt', b'zTXt', b'eXIf')
GZIP_MAGIC = b'\x1f\x8b'
NBT_AUTHOR = re.compile(rb'\x08\x00\x06author(..)', re.S)


def private_words():
    raw = os.environ.get('EXOFRAMES_PRIVATE_WORDS', '')
    return [w.strip().lower().encode('utf-8') for w in raw.split(',') if w.strip()]


def scan_bytes(where, data, findings, words):
    views = [data]
    if b'\\/' in data:  # JSON puo' scrivere "/" come "\/"
        views.append(data.replace(b'\\/', b'/'))
    for view in views:
        for label, rx in PATTERNS.items():
            for match in sorted(set(m.group(0) for m in rx.finditer(view))):
                findings.append((label, where, match[:80].decode('latin-1')))
    low = data.lower()
    for word in words:
        if word in low:
            findings.append(('parola privata (EXOFRAMES_PRIVATE_WORDS)', where, '<corrispondenza>'))


def scan_manifest(where, data, findings):
    for line in data.decode('utf-8', 'replace').splitlines():
        if line.lower().startswith('built-by'):
            findings.append(('attributo Built-By nel manifest', where, line.strip()[:80]))


def scan_png(where, data, findings):
    if data[:8] != b'\x89PNG\r\n\x1a\n':
        return
    i = 8
    while i + 8 <= len(data):
        length, kind = struct.unpack('>I4s', data[i:i + 8])
        body = data[i + 8:i + 8 + length]
        i += 12 + length
        if kind in PNG_TEXT_CHUNKS:
            text = body
            if kind == b'zTXt' and b'\x00' in body:
                key, rest = body.split(b'\x00', 1)
                try:
                    text = key + b'=' + zlib.decompress(rest[1:])
                except zlib.error:
                    pass
            findings.append(('metadato testuale nel PNG (' + kind.decode() + ')', where,
                             text[:60].replace(b'\x00', b' ').decode('latin-1')))
        elif kind == b'iCCP' and b'\x00' in body:
            name, rest = body.split(b'\x00', 1)
            try:
                profile = zlib.decompress(rest[1:])
            except zlib.error:
                profile = b''
            scan_bytes(where + ' (profilo colore)', name + b' ' + profile, findings, private_words())


def scan_ogg(where, data, findings):
    start = data.find(b'\x03vorbis')
    if start < 0:
        return
    try:
        k = start + 7
        vendor_len = struct.unpack('<I', data[k:k + 4])[0]
        k += 4 + vendor_len
        count = struct.unpack('<I', data[k:k + 4])[0]
        k += 4
        for _ in range(min(count, 64)):
            size = struct.unpack('<I', data[k:k + 4])[0]
            comment = data[k + 4:k + 4 + size]
            k += 4 + size
            if not comment.upper().startswith(b'ENCODER='):
                findings.append(('commento Vorbis nell\'OGG', where, comment[:60].decode('utf-8', 'replace')))
    except struct.error:
        pass


def gunzip(data):
    """Restituisce i dati decompressi, oppure None se non sono un gzip leggibile."""
    if data[:2] != GZIP_MAGIC:
        return None
    try:
        return gzip.decompress(data)
    except (OSError, EOFError, zlib.error):
        return None


def scan_nbt(where, data, findings, words):
    raw = gunzip(data)
    if raw is None:
        raw = data
    for match in NBT_AUTHOR.finditer(raw):
        size = struct.unpack('>H', match.group(1))[0]
        value = raw[match.end():match.end() + size]
        if value not in (b'', b'?'):
            findings.append(('campo author nella struttura NBT', where, value[:40].decode('utf-8', 'replace')))


def walk(archive, prefix, findings, words):
    for info in archive.infolist():
        if info.is_dir():
            continue
        where = prefix + info.filename
        lower = info.filename.lower()
        try:
            data = archive.read(info)
        except (zipfile.BadZipFile, zlib.error, OSError, EOFError, RuntimeError, NotImplementedError) as problem:
            # RuntimeError: voce cifrata; NotImplementedError: metodo di compressione non supportato.
            findings.append(('voce illeggibile', where, str(problem)[:60]))
            continue
        scan_bytes(where + ' (nome)', info.filename.encode('utf-8', 'replace'), findings, words)
        scan_bytes(where, data, findings, words)
        if data[:2] == GZIP_MAGIC:  # strutture .nbt, .dat e qualunque altro file compresso con gzip
            unpacked = gunzip(data)
            if unpacked is None:
                # Un gzip che non si apre non si puo' controllare: meglio fermarsi che lasciarlo passare.
                findings.append(('file gzip illeggibile', where, ''))
            else:
                scan_bytes(where + ' (decompresso)', unpacked, findings, words)
        if lower.endswith('.nbt'):
            scan_nbt(where, data, findings, words)
        if info.comment:
            scan_bytes(where + ' (commento zip)', info.comment, findings, words)
        if lower.endswith('manifest.mf'):
            scan_manifest(where, data, findings)
        elif lower.endswith('.png'):
            scan_png(where, data, findings)
        elif lower.endswith('.ogg'):
            scan_ogg(where, data, findings)
        elif lower.endswith('.jar'):
            try:
                walk(zipfile.ZipFile(io.BytesIO(data)), where + '!/', findings, words)
            except zipfile.BadZipFile:
                findings.append(('jar annidato illeggibile', where, ''))


def main(paths):
    if not paths:
        print('uso: check_jar_privacy.py <jar>...', file=sys.stderr)
        return 2
    words = private_words()
    failed = False
    for path in paths:
        findings = []
        try:
            with zipfile.ZipFile(path) as archive:
                if archive.comment:
                    scan_bytes('(commento del jar)', archive.comment, findings, words)
                walk(archive, '', findings, words)
        except (zipfile.BadZipFile, OSError) as problem:
            findings.append(('jar illeggibile', '(file)', str(problem)[:60]))
        for label, where, value in sorted(set(findings)):
            print('FAIL: %s: %s in %s: %s' % (os.path.basename(path), label, where, value), file=sys.stderr)
            failed = True
    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
