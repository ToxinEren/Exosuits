"""Direzioni della stazione: un modulo per direzione, in ordine di `DIRECTIONS`. Ogni modulo espone LETTER, TITLE e
IDEA (per le anteprime, in italiano), build(bench) che scava le tasche e dipinge le tele di un bench.Bench, e icon()
che restituisce l'icona 16x16 RGBA dell'oggetto."""
import importlib

DIRECTIONS = ('officina', 'rame', 'pistoni')


def load(name):
    if name not in DIRECTIONS:
        raise ValueError(f'direzione sconosciuta: {name}')
    return importlib.import_module('station_designs.' + name)
