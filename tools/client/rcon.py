#!/usr/bin/env python3
"""Client RCON minimo per il server di prova locale di tools/client/client_shot.sh.

Uso: python3 tools/client/rcon.py [--no-check] <porta> <password> <comando> [comando ...]

Manda i comandi in ordine (senza la barra iniziale) e stampa ogni risposta. Un comando la cui risposta ha la forma di
un errore di Minecraft (sintassi, bersaglio inesistente, oggetto sconosciuto, colore non valido del mod...) ferma
l'esecuzione: la riga RIFIUTATO va su stderr e l'uscita e' 1. RCON non dice se un comando e' riuscito, quindi il
controllo legge il testo della risposta (il server risponde sempre in inglese): l'elenco e' in FAILURE_PATTERNS.
Con --no-check le risposte non si controllano (serve per "stop" alla chiusura).
Uscite: 0 tutto accettato, 1 comando rifiutato, 2 connessione o autenticazione fallita, 64 uso sbagliato.
Solo libreria standard."""
import re
import socket
import struct
import sys

# Testi delle risposte di errore di Minecraft 1.20.1 e 1.21.1 e del comando /exoframe del mod.
FAILURE_PATTERNS = [
    r'<--\[HERE\]',
    r'Unknown or incomplete command',
    r'Incorrect argument for command',
    r'An unexpected error occurred',
    r'No entity was found',
    r'No player was found',
    r'Unknown (item|block|entity|dimension|function|gamerule)',
    r'\bInvalid\b',
    r'Could not set the block',
    r'No blocks were filled',
    r'That position is not loaded',
    r'does not have (a )?slot',
    r'\b(Cannot|cannot|Can\'t|can\'t|Unable to)\b',
]
FAILURE_RE = re.compile('|'.join(FAILURE_PATTERNS))

TYPE_AUTH = 3
TYPE_COMMAND = 2


def packet(rid, typ, body):
    data = struct.pack('<ii', rid, typ) + body.encode('utf-8') + b'\x00\x00'
    return struct.pack('<i', len(data)) + data


def read_exact(sock, n):
    buf = b''
    while len(buf) < n:
        chunk = sock.recv(n - len(buf))
        if not chunk:
            raise ConnectionError('connessione chiusa dal server')
        buf += chunk
    return buf


def receive(sock):
    size = struct.unpack('<i', read_exact(sock, 4))[0]
    buf = read_exact(sock, size)
    rid, _ = struct.unpack('<ii', buf[:8])
    return rid, buf[8:-2].decode('utf-8', 'replace')


def main(argv):
    check = True
    if argv and argv[0] == '--no-check':
        check = False
        argv = argv[1:]
    if len(argv) < 3:
        print(__doc__, file=sys.stderr)
        return 64
    port, password, commands = int(argv[0]), argv[1], argv[2:]
    try:
        sock = socket.create_connection(('127.0.0.1', port), timeout=15)
        sock.sendall(packet(1, TYPE_AUTH, password))
        rid, _ = receive(sock)
    except (OSError, ConnectionError) as e:
        print(f'rcon: impossibile collegarsi al server sulla porta {port}: {e}', file=sys.stderr)
        return 2
    if rid == -1:
        print('rcon: password RCON rifiutata dal server', file=sys.stderr)
        return 2
    try:
        for i, command in enumerate(commands, start=2):
            sock.sendall(packet(i, TYPE_COMMAND, command))
            try:
                _, body = receive(sock)
            except (ConnectionError, socket.timeout):
                body = '(nessuna risposta)'
            print(f'> {command}\n{body}', flush=True)
            if check and FAILURE_RE.search(body):
                print(f'RIFIUTATO dal server: "{command}": {body.strip()}', file=sys.stderr)
                return 1
    except OSError as e:
        print(f'rcon: connessione persa: {e}', file=sys.stderr)
        return 2
    finally:
        sock.close()
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
