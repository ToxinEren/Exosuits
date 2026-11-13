#!/usr/bin/env python3
"""Confronta due PNG generati da tools/art senza dipendere dal compressore.

Uso: python3 tools/art/compare_png.py <atteso.png> <committato.png>

Il deflate prodotto da zlib non e' un contratto (zlib-ng e versioni diverse danno byte diversi per gli stessi
pixel), quindi si confrontano: l'elenco dei blocchi (solo IHDR, IDAT, IEND, nessun metadato), i campi di IHDR e i
dati di IDAT decompressi. Il confronto resta esatto sui pixel. Esce con 0 se i due file sono equivalenti, con 1
stampando le differenze altrimenti, con 2 (e l'uso) se non riceve esattamente due argomenti.
"""
import struct
import sys
import zlib

SIGNATURE = b'\x89PNG\r\n\x1a\n'


def read(path):
    """Restituisce (elenco dei tipi di blocco, IHDR, dati IDAT decompressi) oppure solleva ValueError."""
    with open(path, 'rb') as fh:
        data = fh.read()
    if data[:8] != SIGNATURE:
        raise ValueError('firma PNG assente')
    kinds, ihdr, idat = [], None, b''
    i = 8
    while i < len(data):
        if i + 8 > len(data):
            raise ValueError('blocco troncato')
        length, kind = struct.unpack('>I4s', data[i:i + 8])
        body = data[i + 8:i + 8 + length]
        crc = data[i + 8 + length:i + 12 + length]
        if len(body) != length or len(crc) != 4 or struct.unpack('>I', crc)[0] != zlib.crc32(kind + body) & 0xFFFFFFFF:
            raise ValueError(f'blocco {kind!r} troncato o con CRC errato')
        kinds.append(kind.decode('latin-1'))
        if kind == b'IHDR':
            ihdr = body
        elif kind == b'IDAT':
            idat += body
        i += 12 + length
    try:
        pixels = zlib.decompress(idat)
    except zlib.error as e:
        raise ValueError(f'IDAT non decompressibile: {e}')
    return kinds, ihdr, pixels


def main():
    if len(sys.argv) != 3:
        print('uso: python3 tools/art/compare_png.py <png attesa> <png da controllare>', file=sys.stderr)
        return 2
    expected_path, actual_path = sys.argv[1:3]
    problems = []
    try:
        expected = read(expected_path)
        actual = read(actual_path)
    except (OSError, ValueError) as e:
        print(f'FAIL: {actual_path}: {e}', file=sys.stderr)
        return 1
    # i blocchi ammessi sono solo questi: niente metadati nel file committato
    kinds = actual[0]
    if len(kinds) < 3 or kinds[0] != 'IHDR' or kinds[-1] != 'IEND' or set(kinds[1:-1]) != {'IDAT'}:
        problems.append(f'blocchi {actual[0]} (attesi solo IHDR, IDAT, IEND)')
    if actual[1] != expected[1]:
        problems.append('IHDR diverso (dimensioni o formato dei pixel)')
    if actual[2] != expected[2]:
        diff = sum(1 for a, b in zip(actual[2], expected[2]) if a != b) + abs(len(actual[2]) - len(expected[2]))
        problems.append(f'pixel diversi ({diff} byte dei dati non compressi)')
    for p in problems:
        print(f'FAIL: {actual_path}: {p}', file=sys.stderr)
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main())
