"""Aiuti per la parte bassa delle gambe, misurata sull'immagine di riferimento di Luigi: le misure sono date come
distanza |x| dal piano di mezzo; ax() le porta alla x della gamba destra, leg() aggiunge la scatola destra e la
sinistra speculare. Il nome comincia con '_': designs.load_all non lo carica come design."""


def ax(lo, hi):
    """Intervallo di x della gamba destra da un intervallo di distanze |x| dal piano di mezzo."""
    return -hi, -lo


def leg(m, bone, name, a0, a1, y0, y1, z0, z1, mat, **opts):
    """Scatola della gamba destra da |x| a0..a1, y y0..y1, z z0..z1, e la sua copia speculare sulla gamba sinistra."""
    x0, x1 = ax(a0, a1)
    m.sym(bone, name, (x0, y0, z0), (x1, y1, z1), mat, **opts)
