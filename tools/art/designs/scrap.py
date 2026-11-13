"""Livello Rottame: i sei pezzi, armatura pesante chiusa sul telaio (designs/frame.py). Quarta passata della prova
d'arte, approvata da Luigi il 13 novembre 2026. Petto a botte a tre lastre con chiglia, vita a segmenti, falde basse
sui fianchi, spallacci enormi a tre strati con feritoie a lamelle, testa piccola incassata nel collare, avambracci e
stivali pesanti. Lamiere di recupero: toppe, bulloni, ammaccature dipinte. Piedi (schinieri, piastre delle dita) e
schiena (scapole, feritoie, piastre lombari e dei fianchi) sono della quinta passata, dal riferimento di Luigi.

Ogni scatola si dichiara una volta nell'osso modello exo.piece(slot, regione) e il modello la ripete nei quattro
stati visivi (ossa piece<Slot>_s0..s3_<regione>, stesse scatole, zone diverse dell'atlante); la pittura degli stati
la fa paint.wear. Regioni: p1 (primaria, nel gioco bronzo 0x8A6F52 se il pezzo non e' verniciato), p2 (secondaria,
canna di fucile 0x4A4F55), n (metallo nudo, vetro, spie, griglie: colori finali).
Gli spallacci stanno nel pezzo Torso perche' gli attuatori delle spalle sono del busto. Braccia e gambe sono
speculari (un solo item per braccio e uno per gamba): si scrive il lato destro con sym(). Elmo e torso possono essere
asimmetrici (fessure luminose sul petto a sinistra, toppa sullo spallaccio destro).
Decalcomanie: due ossa piatte con texel trasparenti, nascoste dal renderer fino a M5 (materiale 'decal').
Ingombro con i pezzi: x +-13.5, y 0..32, z -8.75..10.5 (exo.SUIT_MIN/MAX).
"""
from exo import piece

ORDER = 10

SLOTS = (('Helmet', 'armorHead'), ('Torso', 'armorBody'), ('RightArm', 'armorRightArm'),
         ('LeftArm', 'armorLeftArm'), ('RightLeg', 'armorRightLeg'), ('LeftLeg', 'armorLeftLeg'))
# solo la faccia esterna di una decalcomania va nell'atlante
DECAL_ONLY = {'north': ('east', 'south', 'west', 'up', 'down'), 'west': ('north', 'east', 'south', 'up', 'down')}


def build(m):
    for slot, parent in SLOTS:
        m.piece_bones(slot, parent)
    helmet(m)
    torso(m)
    shoulders(m)
    arms(m)
    legs(m)
    decals(m)


def helmet(m):
    # elmo stretto e basso: calotta, tetto a gradino, piastra del viso a due gradini con una banda di visione
    # dietro tre sbarre verticali e tre feritoie per l'aria sul mento, tettuccio sopra la visiera
    p1, p2, n = piece('Helmet', 'p1'), piece('Helmet', 'p2'), piece('Helmet', 'n')
    m.sym(p1, 'helm_side', (-5.25, 24.75, -4.75), (-4.75, 31.25, 4.5), 'paint')
    m.box(p1, 'helm_back', (-4.75, 25.75, 4.25), (4.75, 31.25, 4.75), 'paint')
    m.box(p1, 'helm_top', (-5.25, 31.25, -5.25), (5.25, 31.75, 4.75), 'paint')
    m.box(p2, 'helm_top2', (-3.5, 31.75, -4), (3.5, 32, 3.5), 'paint')
    m.box(p1, 'helm_face', (-4.75, 23.25, -5.25), (4.75, 31.25, -4.75), 'paint')
    m.box(p1, 'helm_jaw', (-3.5, 23.25, -5.75), (3.5, 25.75, -5.25), 'paint')
    m.box(n, 'helm_breather', (-2, 23.75, -6), (2, 25.25, -5.75), 'vent')
    m.box(n, 'visor_glass', (-3, 26.25, -5.5), (3, 27.5, -5.25), 'glass')
    for k, x in enumerate((-2, -0.25, 1.5)):
        m.box(n, f'visor_bar_{k}', (x, 26, -5.75), (x + 0.5, 27.75, -5.25), 'gun')
    m.box(p2, 'helm_hood', (-5, 28, -6), (5, 28.75, -5.25), 'paint')


def torso(m):
    p1, p2, n = piece('Torso', 'p1'), piece('Torso', 'p2'), piece('Torso', 'n')
    # petto a botte: tre lastre annidate, il centro sporge di piu'; gradino verso il collare
    # ogni lastra ha gli angoli smussati a gradino (fascia centrale piu' alta, bordi laterali piu' bassi)
    m.box(p1, 'chest_1', (-8.5, 16.25, -6.5), (8.5, 24.75, -6), 'paint')
    m.sym(p1, 'chest_1_edge', (-9.5, 17.25, -6.25), (-8.5, 23.75, -5.75), 'paint')
    m.box(p1, 'chest_2', (-7, 17, -7.25), (7, 24, -6.5), 'paint')
    m.sym(p1, 'chest_2_edge', (-8, 17.75, -7), (-7, 23.25, -6.5), 'paint')
    m.box(p1, 'chest_3', (-4.5, 17.75, -8), (4.5, 23.25, -7.25), 'paint')
    m.sym(p1, 'chest_3_edge', (-5.5, 18.5, -7.75), (-4.5, 22.5, -7.25), 'paint')
    m.box(p2, 'chest_keel', (-1, 18.25, -8.5), (1, 22.75, -8), 'paint')
    m.box(p1, 'chest_upper', (-7.5, 24.75, -6.25), (7.5, 25.5, -5.5), 'paint')
    # punto focale: tre fessure luminose strette, basse sul lato sinistro (niente dischi)
    for k, y in enumerate((17.75, 18.5, 19.25)):
        m.box(n, f'chest_slit_{k}', (5.75, y, -7.5), (7.25, y + 0.25, -7.25), 'lamp')
    m.box(p2, 'chest_patch', (-6.75, 20, -7.5), (-5.25, 22, -7.25), 'paint')
    # collare che sale attorno alla testa ai lati e dietro
    m.sym(p1, 'collar_top', (-7.5, 27.75, -4.25), (-5.25, 28.5, 4), 'paint')
    m.sym(p1, 'collar_outer', (-7.5, 24.5, -4.25), (-7, 27.75, 4), 'paint')
    m.box(p1, 'collar_back', (-7.25, 27.75, 4.5), (7.25, 29, 6.25), 'paint')
    # vita a segmenti, piu' stretta del petto
    m.box(p2, 'belly_1', (-6, 12.75, -5.5), (6, 14.25, -5), 'paint')
    m.box(p2, 'belly_2', (-5.5, 14.5, -6), (5.5, 15.25, -5.25), 'paint')
    m.box(p2, 'belly_3', (-5, 15.5, -6), (5, 16.25, -5.25), 'paint')
    # falde basse sui fianchi, davanti e di lato
    m.sym(p1, 'tasset_front', (-6, 8.75, -5.5), (-1, 12.5, -4.75), 'paint')
    # parte da y 10.25, sopra il palmo e il pollice della mano della tuta a riposo
    m.sym(p1, 'tasset_side', (-7.5, 10.25, -3.25), (-6.75, 13, 3.25), 'paint')
    # schiena, attorno all'alloggiamento della cella (che resta scoperto): piastra di fondo, scapole a tre strati sotto
    # i puntoni del telaio, fasce di feritoie sotto le scapole, piastra lombare a due gradini sotto quella del telaio,
    # piastre dei fianchi che girano dietro (fuori dalla larghezza delle cosce, quindi mai toccate camminando)
    m.box(p1, 'back_plate', (-9.5, 15.5, 4.5), (9.5, 23.5, 5.25), 'paint')
    m.sym(p1, 'scapula_1', (-10.75, 17.75, 5.25), (-6.75, 25, 6.5), 'paint')
    m.sym(p1, 'scapula_2', (-10.25, 18.75, 6.5), (-7.25, 24.25, 7.25), 'paint')
    m.sym(p2, 'scapula_3', (-9.75, 19.75, 7.25), (-7.75, 23.25, 7.75), 'paint')
    m.sym(n, 'blade_louver', (-10.25, 14.5, 5.25), (-6.75, 17.75, 7.25), 'vent')
    m.box(p1, 'lumbar_1', (-5.75, 10.25, 7.75), (5.75, 11.25, 9), 'paint')
    m.box(p2, 'lumbar_2', (-4, 10.5, 9), (4, 11, 9.5), 'paint')
    m.sym(p1, 'hip_back', (-7.5, 10.25, 3.25), (-6.5, 13, 5.25), 'paint')
    m.sym(p2, 'hip_back_2', (-7.25, 10.75, 5.25), (-6.75, 12.5, 5.75), 'paint')


def shoulders(m):
    # spallacci enormi a tre strati sulle alette, gonna esterna, davanti e dietro; feritoie a lamelle davanti
    p1, p2, n = piece('Torso', 'p1'), piece('Torso', 'p2'), piece('Torso', 'n')
    m.sym(p1, 'pauldron_1', (-13.5, 29.5, -5), (-7, 30.25, 4.75), 'paint')
    m.sym(p1, 'pauldron_2', (-12.75, 30.25, -4.25), (-7.75, 30.75, 4), 'paint')
    m.sym(p2, 'pauldron_3', (-11.75, 30.75, -3), (-8.75, 31, 2.75), 'paint')
    m.sym(p1, 'pauldron_skirt_out', (-13.5, 25.5, -4.5), (-12.75, 29.5, 4.25), 'paint')
    m.sym(p2, 'pauldron_skirt_front', (-12.75, 26.5, -5.5), (-7.25, 29.5, -4.75), 'paint')
    m.sym(p2, 'pauldron_skirt_back', (-12.75, 26.5, 3.75), (-7.25, 29.5, 4.5), 'paint')
    m.sym(n, 'pauldron_louver', (-12, 27, -5.75), (-8, 28.75, -5.5), 'vent')
    m.box(p2, 'pauldron_patch', (-11, 29.75, -5.25), (-9, 30.25, -5), 'paint')


def arms(m):
    p1, p2 = piece('RightArm', 'p1'), piece('RightArm', 'p2')
    m.sym(p2, 'rerebrace', (-12, 19, -2), (-11.5, 23, 2), 'paint')
    m.sym(p1, 'elbow_cop', (-13.25, 15, -2.75), (-12.75, 19, 2.75), 'paint')
    # avambraccio corazzato piu' grosso del braccio: esterno, davanti, dietro, rinforzo
    m.sym(p1, 'vambrace_out', (-13.5, 10.5, -3.75), (-12.75, 15, 3.75), 'paint')
    m.sym(p1, 'vambrace_front', (-12.75, 11.25, -5), (-7.5, 15, -4.25), 'paint')
    m.sym(p1, 'vambrace_back', (-12.75, 11.25, 4.25), (-7.5, 15, 5), 'paint')
    m.sym(p2, 'vambrace_front_2', (-11.5, 12, -5.5), (-8.75, 14.25, -5), 'paint')
    m.sym(p2, 'knuckle_guard', (-12.5, 9.25, -4), (-7, 10, -3.5), 'paint')
    # toppa dietro il bracciale (nella prova era solo sul sinistro: qui su entrambi, un solo item per i due lati)
    m.sym(p2, 'vambrace_patch', (-11.5, 11.75, 5), (-9.5, 13.25, 5.25), 'paint')


def legs(m):
    p1, p2, n = piece('RightLeg', 'p1'), piece('RightLeg', 'p2'), piece('RightLeg', 'n')
    m.sym(p1, 'cuisse', (-5.25, 9, -4.75), (-0.5, 11.75, -4), 'paint')
    m.sym(p1, 'knee_cop', (-5, 4.5, -4.5), (-0.5, 7.5, -3.75), 'paint')
    m.sym(p2, 'knee_ridge', (-4, 5, -5), (-1.5, 7, -4.5), 'paint')
    # schiniere a gradini che scende sul davanti della caviglia, con feritoie; piastra esterna della caviglia (il mozzo
    # e i puntoni del telaio restano fuori); piastra del polpaccio, paratacco
    m.sym(p1, 'greave', (-5.75, 3.25, -3.75), (-0.5, 4.75, -3), 'paint')
    m.sym(p1, 'greave_low', (-6.25, 2, -4.25), (-0.75, 3.25, -3.25), 'paint')
    m.sym(p2, 'greave_ridge', (-4.75, 2.25, -4.5), (-2.25, 3, -4.25), 'paint')
    m.sym(n, 'greave_louver', (-5, 3.5, -4), (-1.25, 4.5, -3.75), 'vent')
    m.sym(p1, 'ankle_guard', (-7.25, 0.75, -2.75), (-6.75, 3.25, 1), 'paint')
    m.sym(p1, 'calf_plate', (-5.5, 2.75, 3.5), (-0.75, 6.5, 4.25), 'paint')
    m.sym(p2, 'calf_plate_2', (-4.75, 3.5, 4.25), (-1.5, 5.75, 4.75), 'paint')
    m.sym(p2, 'heel_guard', (-6.25, 1, 4.25), (-1.25, 2.5, 4.75), 'paint')
    # stivale: piastra e naso su ciascuna delle due dita
    for side, (x0, x1) in (('out', (-7.5, -4.25)), ('in', (-3.75, -0.5))):
        m.sym(p1, f'toe_plate_{side}', (x0, 1.75, -7.75), (x1, 2.25, -3.5), 'paint')
        m.sym(p2, f'toe_nose_{side}', (x0 + 0.25, 0.5, -8.5), (x1 - 0.25, 1.75, -8), 'paint')


def decals(m):
    # rettangoli piatti di 3.5 x 3.5 px (7 x 7 texel), 0.25 px sopra la piastra: sulla lastra centrale del petto a
    # destra della chiglia, e sulla piastra esterna del braccio sinistro sotto lo spallaccio (l'osso e' del braccio e
    # ne segue il movimento)
    m.bone('pieceTorso_decal', 'armorBody', 'decal')
    m.box('pieceTorso_decal', 'decal_torso', (-4.5, 18.75, -8.25), (-1, 22.25, -8), 'decal',
          hide=DECAL_ONLY['north'])
    m.bone('pieceLeftArm_decal', 'armorLeftArm', 'decal')
    m.box('pieceLeftArm_decal', 'decal_left_arm', (12, 19.25, -1.75), (12.25, 22.75, 1.75), 'decal',
          hide=DECAL_ONLY['west'])
