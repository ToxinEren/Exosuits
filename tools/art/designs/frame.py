"""Telaio nudo: lo scheletro della tuta, sempre visibile, anche sotto i pezzi, ed e' cio' che sta in piedi nel mondo
senza pezzi montati. E' il telaio della quarta passata della prova d'arte, approvata da Luigi il 13 novembre 2026:
scheletro massiccio, piu' grande del pilota alle braccia, al petto e alle gambe, con petto a botte, testa incassata,
spalle enormi, vita stretta, gambe e stivali grossi. Solo parti di macchina (travi, alloggiamenti degli attuatori,
pistoni, mozzi); le piastre sono i pezzi (designs/scrap.py), qui ci sono i punti dove si montano (alette, borchie).
La parte bassa e laterale della schiena (trave lombare con feritoie, puntoni verso il giogo) e' della quinta passata;
la cella e il suo alloggiamento no. Dal ginocchio in giu' la gamba riproduce l'immagine di riferimento di Luigi (sesta
passata, sua decisione esplicita: vale solo per quella parte).

Il renderer scala la tuta di exo.SUIT_SCALE (1.65) attorno ai piedi e disegna il pilota a grandezza normale sollevato
di exo.PILOT_LIFT_PX (24(S-1) = 15.6 px), senza gambe: nelle coordinate del modello il pilota e' un giocatore scalato
di 1/S attorno a (0,24,0). A S = 1.65: testa y 24..28.85 (x, z +-2.42); busto y 16.73..24 (x +-2.42, z +-1.21);
braccia x +-2.42..4.85, y 16.73..24, mani in fondo a y 16.73, sulle impugnature dentro le gabbie del braccio;
fuori, sul lato esterno, il braccio meccanico con la mano della tuta (fondo a y 6.75). Le gambe del pilota (x +-2.42,
y 9.45..16.73) sono nascoste nelle prese delle cosce e nel sedile, che sale fino a y 16.5. Testa, sedile e
impugnature sono dimensionati per S = 1.65 (con S = 1.4 la tuta era alta 2.5 blocchi; Luigi ne vuole almeno 3): con
un'altra scala vanno ricontrollati. La testa arriva a y 31.25: il telaio nudo e' alto 3.02 blocchi.
Gli attuatori delle spalle stanno nel busto (frameBody), non nelle braccia: per questo gli spallacci sono nel pezzo
Torso. Ingombro del telaio nudo: x +-12.75, y 0..31.25, z -8..10 (exo.FRAME_MIN/MAX). Fronte verso -z, lato destro a
x negativa; sym() scrive il lato destro e aggiunge il sinistro. Nessuna scatola ruotata.

Materiali: gun (acciaio brunito), steel (parti lavorate chiare), brass (mozzi), chrome (steli, impugnature,
borchie), cable (rame), rubber (tubi), vent (griglie calde), cell (cella di redstone), lamp (spie).
"""
from designs._legs import leg

ORDER = 0

HEAD, BODY, RARM, RLEG = 'frameHead', 'frameBody', 'frameRightArm', 'frameRightLeg'


# parte bassa della gamba destra (il sinistro e' speculare), misurata sull'immagine di riferimento di Luigi riportata
# ai pixel del modello: (|x| minimo, |x| massimo, y minimo, y massimo). Piede del telaio (anima d'acciaio, il guscio di
# bronzo e' nei pezzi): strato basso diviso attorno alla porta davanti
FOOT_CORE = (
    ('foot_0_out', 4.25, 4.5, 0, 1), ('foot_0_in', 2.25, 2.75, 0, 1),
    ('foot_1', 2.25, 4.5, 1, 1.5), ('foot_2', 2.25, 4.25, 1.5, 1.75),
)
ANKLE_BAND = ((1.75, 2), (2.25, 2.5), (2.75, 3.75), (4, 4.25), (4.5, 4.75))
# gradini dei puntoni della schiena, lato destro: (x minimo, y minimo, y massimo), larghi 1.5 px
BACK_STRUT_STEPS = ((-7.5, 12, 15.5), (-8.5, 15.5, 18.5), (-9.5, 18.5, 21.5), (-10.5, 21.5, 24.5))


def build(m):
    m.bone('frameHead', 'armorHead', 'frame')
    m.bone('frameBody', 'armorBody', 'frame')
    m.bone('frameRightArm', 'armorRightArm', 'frame')
    m.bone('frameLeftArm', 'armorLeftArm', 'frame')
    m.bone('frameRightLeg', 'armorRightLeg', 'frame')
    m.bone('frameLeftLeg', 'armorLeftLeg', 'frame')
    head(m)
    torso(m)
    shoulders(m)
    back(m)
    arms(m)
    legs(m)


def head(m):
    # testa scheletrica stretta: pareti di 1 px (x +-3.25..4.25) attorno alla testa del pilota, che con il margine del
    # cappello arriva a x e z +-2.73
    m.box(HEAD, 'crown', (-4, 31, -3.75), (4, 31.25, 4), 'gun')
    m.sym(HEAD, 'temple', (-4.25, 27.25, -3.75), (-3.25, 31, 3.75), 'gun')
    m.sym(HEAD, 'jaw', (-4.25, 24.25, -4), (-3.25, 27.25, 1.75), 'steel')
    m.sym(HEAD, 'ear_hub', (-4.75, 25, -1.25), (-4.25, 27.5, 1.25), 'brass', hub=True)
    m.box(HEAD, 'nape', (-3.25, 24.25, 3.25), (3.25, 31, 4.25), 'gun')
    m.box(HEAD, 'brow', (-4, 29.5, -4.5), (4, 31, -3.25), 'steel')
    m.box(HEAD, 'chin', (-4.25, 22.75, -4.5), (4.25, 23.75, -3.25), 'gun')
    m.sym(HEAD, 'visor_post', (-3.25, 23.75, -4.25), (-2.75, 29.5, -3.25), 'gun')
    m.box(HEAD, 'brow_lamp', (-0.5, 30, -4.75), (0.5, 30.5, -4.5), 'lamp')
    m.sym(HEAD, 'helmet_lug', (-4.5, 28.25, 1.5), (-4.25, 29.5, 2.75), 'chrome')


def torso(m):
    # sedile e anello della vita stretto (la vita e' molto piu' stretta del petto)
    m.box(BODY, 'seat', (-2.75, 12.5, -2.75), (2.75, 16.5, 2.75), 'gun')
    # il bordo sale fino a 16.75 e chiude davanti la fessura tra il sedile (16.5, piu' su toccherebbe il pilota) e il
    # busto del pilota (16.73)
    m.box(BODY, 'seat_lip', (-3, 15, -3.25), (3, 16.75, -2), 'steel')
    m.box(BODY, 'seat_back', (-3, 15, 2), (3, 18, 3.25), 'gun')
    m.box(BODY, 'waist_front', (-5.5, 12.5, -5), (5.5, 14.25, -3.25), 'gun')
    m.sym(BODY, 'waist_side', (-5.75, 12.5, -3.25), (-4.25, 14.25, 3.5), 'gun')
    m.box(BODY, 'waist_back', (-5.5, 12.5, 3.5), (5.5, 14.25, 5), 'gun')
    m.sym(BODY, 'hip_bracket', (-6.75, 11.75, -1.5), (-5.5, 14, 1.5), 'steel')
    # petto a botte: telaio profondo, montanti larghi che si allargano verso le spalle, costole sottili
    m.sym(BODY, 'chest_strut', (-6, 14.25, -5.75), (-4.25, 21.5, -3.5), 'gun')
    m.sym(BODY, 'chest_flank', (-9.25, 18.5, -5.75), (-6, 24.5, -4.25), 'gun')
    for k, y in enumerate((17, 19.75)):
        m.box(BODY, f'chest_rib_{k}', (-4.25, y, -4.75), (4.25, y + 0.75, -3.75), 'gun')
    m.box(BODY, 'chest_yoke', (-9, 21.5, -6), (9, 22.75, -4.5), 'steel')
    m.sym(BODY, 'chest_boss', (-8, 20, -6), (-7, 21, -5.75), 'chrome')
    m.sym(BODY, 'collar_post', (-6.5, 22.75, -5.5), (-5.25, 25.5, -4), 'gun')
    # anello del collare attorno alla testa: sale ai lati e dietro, basso davanti
    m.sym(BODY, 'collar_side', (-7, 24.5, -4), (-5.25, 27.75, 3.75), 'gun')
    m.box(BODY, 'collar_front', (-5.25, 24.5, -5.5), (5.25, 25.25, -4.5), 'gun')
    m.sym(BODY, 'collar_lamp', (-6.75, 23.25, -6), (-5.75, 23.75, -5.75), 'lamp')


def shoulders(m):
    # giogo dietro la testa, alto; attuatori delle spalle grandi sotto gli spallacci
    m.box(BODY, 'yoke_back', (-10, 24, 3.75), (10, 27.75, 6), 'gun')
    m.sym(BODY, 'actuator', (-12.25, 24, -4), (-7, 28, 3.75), 'gun')
    m.sym(BODY, 'actuator_bearing', (-12.75, 24.5, -2), (-12.25, 27.5, 2), 'brass', hub=True)
    m.sym(BODY, 'actuator_front', (-11.5, 24.5, -4.75), (-7.75, 27.25, -4), 'steel')
    m.sym(BODY, 'actuator_cap', (-11.25, 28, -2.5), (-8, 28.75, 2.5), 'steel')
    for k, z in enumerate((-1.75, 1)):
        m.sym(BODY, f'pauldron_lug_{k}', (-10.5, 28.75, z), (-9.25, 29.5, z + 0.75), 'chrome')
    # pistoni dalla parte alta della schiena alla cima delle spalle
    m.sym(BODY, 'shoulder_ram_sleeve', (-7.5, 27.75, 4), (-5, 28.75, 5.25), 'steel')
    m.sym(BODY, 'shoulder_ram_rod', (-9.5, 28, 4.25), (-7.5, 28.5, 5), 'chrome')
    # pistoni dal bacino alle spalle, dietro le braccia del pilota
    m.sym(BODY, 'torso_ram_sleeve', (-7.25, 18, 3.25), (-5.75, 24, 4.5), 'steel')
    m.sym(BODY, 'torso_ram_rod', (-6.75, 14.25, 3.5), (-6.25, 18, 4.25), 'chrome')


def back(m):
    # spina a vertebre, staffe, alloggiamento della cella piu' grande, camini, cavi
    m.box(BODY, 'spine_core', (-1, 15.25, 2.5), (1, 23.25, 3.5), 'chrome')
    for k, y in enumerate((18.25, 19.75, 21.25)):
        m.box(BODY, f'vertebra_{k}', (-2, y, 2.25), (2, y + 1, 3.75), 'gun')
    m.box(BODY, 'spine_top', (-1.5, 22.5, 3.75), (1.5, 24, 4.75), 'gun')
    for k, y in enumerate((15.75, 20.5)):
        m.sym(BODY, f'pack_bracket_{k}', (-1.75, y, 3.5), (-1.25, y + 0.75, 6.25), 'steel')
    m.sym(BODY, 'pack_column', (-6.25, 14.5, 6.25), (-3.25, 26, 9.25), 'steel')
    m.box(BODY, 'pack_top', (-6.5, 26, 6), (6.5, 27.25, 9.5), 'gun')
    m.box(BODY, 'pack_bottom', (-6.5, 13.5, 6), (6.5, 14.5, 9.5), 'gun')
    m.box(BODY, 'pack_floor', (-3.25, 14.5, 6.25), (3.25, 26, 7.5), 'gun')
    m.box(BODY, 'cell', (-2.75, 15.5, 7.5), (2.75, 25, 8.75), 'cell')
    m.box(BODY, 'cell_frame_top', (-3.25, 25, 7.5), (3.25, 26, 9), 'gun')
    m.box(BODY, 'cell_frame_bottom', (-3.25, 14.5, 7.5), (3.25, 15.5, 9), 'gun')
    for k, y in enumerate((17, 19.75, 22.5)):
        m.box(BODY, f'cell_guard_{k}', (-3.75, y, 9), (3.75, y + 0.75, 10), 'gun')
    m.sym(BODY, 'pack_vent', (-6.5, 16, 6.75), (-6.25, 24.5, 9), 'vent')
    m.sym(BODY, 'pack_lamp', (-5.75, 26.25, 9.5), (-4.75, 27, 9.75), 'lamp')
    m.sym(BODY, 'stack', (-5.25, 27.25, 6.75), (-3.75, 29.75, 8.25), 'gun', heat=True)
    m.sym(BODY, 'stack_band', (-5.5, 28, 6.5), (-3.5, 28.75, 8.5), 'brass')
    m.sym(BODY, 'stack_cap', (-5.5, 29.75, 6.5), (-3.5, 30.5, 8.5), 'vent')
    m.sym(BODY, 'cable_a', (-8, 24.75, 7), (-6.25, 25.75, 8), 'cable')
    m.sym(BODY, 'cable_a_fwd', (-8, 24.75, 6), (-7.25, 25.75, 7), 'cable')
    # parte bassa della schiena. Tutto cio' che sta sotto il perno delle gambe (y 12) sta dietro z 6.25, fuori dall'arco
    # che coscia e ginocchio spazzano camminando (vedi tools/art/swing.py): trave lombare appesa al fondo
    # dell'alloggiamento della cella, fascia di feritoie orizzontali sul suo dorso, blocco della spina sotto
    m.box(BODY, 'lumbar_beam', (-5.5, 10.75, 7.25), (5.5, 13.5, 8.5), 'gun')
    m.box(BODY, 'lumbar_vent', (-5.25, 11.25, 8.5), (5.25, 13.25, 9), 'vent')
    m.box(BODY, 'spine_block', (-1, 10, 7.5), (1, 10.75, 8.25), 'gun')
    # puntoni a gradini verso le spalle, in rilievo sopra le scapole del pezzo Torso: dal fondo dell'alloggiamento
    # salgono di lato fino a un attacco sul giogo dietro le spalle
    for k, (x0, y0, y1) in enumerate(BACK_STRUT_STEPS):
        m.sym(BODY, f'back_strut_{k}', (x0, y0, 7.75), (x0 + 1.5, y1, 9), 'chrome')
    m.sym(BODY, 'back_strut_mount', (-11, 23.75, 5.5), (-8.75, 25.5, 8.25), 'gun')


def arms(m):
    # gabbia del braccio del pilota: parete esterna, archi davanti e dietro, impugnatura sotto la mano
    m.sym(RARM, 'arm_cage_wall', (-8.5, 16.5, -2.75), (-6.25, 23.25, 2.75), 'gun')
    m.sym(RARM, 'arm_cage_front', (-6.25, 20.5, -3), (-3.75, 21.5, -2), 'steel')
    m.sym(RARM, 'arm_cage_back', (-6.25, 20.5, 2), (-3.75, 21.5, 3), 'steel')
    m.sym(RARM, 'grip', (-6.25, 16, -2.75), (-3.25, 16.5, 2.75), 'chrome')
    m.sym(RARM, 'grip_post_front', (-3.75, 16.5, -2.75), (-3.25, 17.75, -2), 'gun')
    m.sym(RARM, 'grip_post_back', (-3.75, 16.5, 2), (-3.25, 17.75, 2.75), 'gun')
    m.sym(RARM, 'grip_bar_front', (-6.25, 17, -2.75), (-3.75, 17.75, -2), 'gun')
    m.sym(RARM, 'grip_bar_back', (-6.25, 17, 2), (-3.75, 17.75, 2.75), 'gun')
    # braccio meccanico: trave, due pistoni, gomito grande
    m.sym(RARM, 'upper_beam', (-11.75, 18.75, -1.5), (-8.5, 23.5, 1.5), 'gun')
    m.sym(RARM, 'upper_ram_front', (-11.25, 19.25, -3.25), (-9.25, 23.25, -1.5), 'steel')
    m.sym(RARM, 'upper_ram_back', (-11.25, 19.25, 1.5), (-9.25, 23.25, 3.25), 'steel')
    m.sym(RARM, 'elbow_block', (-12.25, 15.25, -3), (-8.5, 18.75, 3), 'gun')
    m.sym(RARM, 'elbow_hub', (-12.75, 15.5, -2), (-12.25, 18.5, 2), 'brass', hub=True)
    m.sym(RARM, 'elbow_hub_in', (-8.5, 15.75, -1.5), (-8, 18.25, 1.5), 'brass', hub=True)
    # avambraccio pesante (5 x 6 px) con pistoni davanti e dietro e griglia esterna
    m.sym(RARM, 'forearm', (-12.5, 10.75, -3.5), (-7.75, 15.25, 3.5), 'gun')
    m.sym(RARM, 'forearm_ram_front', (-11.25, 11.25, -4.25), (-9, 15, -3.5), 'steel')
    m.sym(RARM, 'forearm_rod_front', (-10.5, 10.25, -4), (-9.75, 11.25, -3.75), 'chrome')
    m.sym(RARM, 'forearm_ram_back', (-11.25, 11.25, 3.5), (-9, 15, 4.25), 'steel')
    m.sym(RARM, 'forearm_rod_back', (-10.5, 10.25, 3.75), (-9.75, 11.25, 4), 'chrome')
    m.sym(RARM, 'forearm_vent', (-12.75, 11.5, -2.25), (-12.5, 14.5, 2.25), 'vent')
    m.sym(RARM, 'forearm_lamp', (-8.75, 13.75, -3.75), (-7.75, 14.5, -3.5), 'lamp')
    m.sym(RARM, 'arm_cable', (-12.5, 15.5, 2.5), (-11.75, 23.5, 3.25), 'cable')
    # polso e mano grande: palmo, tre dita, pollice, nocche d'ottone
    m.sym(RARM, 'wrist', (-11.5, 10.25, -2.25), (-8, 11, 2.25), 'brass')
    m.sym(RARM, 'palm', (-12.25, 8.5, -3), (-7.25, 10.25, 3), 'steel')
    for k, (x0, x1) in enumerate(((-12, -10.5), (-10.25, -8.75), (-8.5, -7.25))):
        m.sym(RARM, f'finger{k}', (x0, 7.25, -2.75), (x1, 8.5, -1), 'gun')
        m.sym(RARM, f'fingertip{k}', (x0 + 0.25, 6.75, -2.5), (x1 - 0.25, 7.25, -1.25), 'gun')
    m.sym(RARM, 'thumb', (-7.25, 7.75, -2.25), (-6.5, 10, -0.75), 'gun')
    m.sym(RARM, 'knuckles', (-12.5, 8.5, -3.5), (-7, 9.25, -3), 'brass')


def legs(m):
    # presa della coscia (manicotto chiuso), fasce, pistone davanti, mozzo dell'anca
    m.sym(RLEG, 'thigh_socket', (-5.5, 6.75, -3), (0, 14, 3), 'gun')
    for k, y in enumerate((8, 11.5)):
        m.sym(RLEG, f'thigh_band_{k}', (-5.75, y, -3.25), (-0.25, y + 0.75, 3.25), 'steel')
    m.sym(RLEG, 'hip_hub', (-6.25, 9, -1.75), (-5.75, 11.25, 1.75), 'brass', hub=True)
    m.sym(RLEG, 'thigh_ram', (-4.25, 8.75, -4), (-1, 11.25, -3.25), 'steel')
    m.sym(RLEG, 'thigh_rod', (-3, 7.25, -3.75), (-2, 8.75, -3.25), 'chrome')
    # ginocchio (dietro lo scudo dello stinco dei pezzi)
    m.sym(RLEG, 'knee_block', (-5.25, 4.5, -2.5), (-0.5, 7, 2.5), 'gun')
    m.sym(RLEG, 'knee_cap', (-4.25, 4.75, -3.75), (-1.25, 7.25, -2.5), 'brass', hub=True)
    # Dal ginocchio in giu' la gamba riproduce quella dell'immagine di riferimento di Luigi (sua decisione, Task 14),
    # misurata sul riferimento (|x| = distanza dal piano di mezzo). Qui l'anima d'acciaio; quello che nel riferimento ha
    # il colore dell'armatura (scudo, puntone, polpaccio, piede, tallone) e' nei pezzi delle gambe.
    leg(m, RLEG, 'shin_outer', 3.5, 4.75, 2.5, 4.5, -1.5, 1.5, 'gun')
    leg(m, RLEG, 'shin_inner', 0.5, 1.75, 2.5, 4.5, -1.75, 1.75, 'gun')
    leg(m, RLEG, 'shin_ram', 2, 3.25, 2.5, 4.75, -3, -1.5, 'steel')
    leg(m, RLEG, 'calf_ram', 1.25, 3.75, 3, 6, 2, 3.25, 'steel')
    # fascia della caviglia in cinque segmenti (due corti, uno lungo, due corti)
    for k, (a0, a1) in enumerate(ANKLE_BAND):
        leg(m, RLEG, f'ankle_band_{k}', a0, a1, 2, 2.25, -1.75, 1.75, 'steel')
    # asta del puntone esterno, anima del cuscinetto, staffa alla caviglia
    leg(m, RLEG, 'strut_rod', 5.5, 5.75, 3, 6.5, -0.25, 0.25, 'steel')
    leg(m, RLEG, 'strut_rod_low', 5.25, 5.5, 1.75, 3, -0.25, 0.25, 'steel')
    leg(m, RLEG, 'strut_arm', 5, 5.5, 5, 6, -0.25, 0.25, 'steel')
    leg(m, RLEG, 'strut_pad_core', 5.25, 5.75, 6.5, 7.25, -1, 1, 'steel')
    leg(m, RLEG, 'strut_bracket_core', 5.5, 5.75, 1.25, 1.75, -0.25, 0.25, 'gun')
    # solco scuro tra il puntone e lo scudo/la piastra del polpaccio (|x| 5..5.25), davanti e dietro al ginocchio e
    # sotto di esso; davanti, il riempimento scuro tra la piastra alta, il cuscinetto e la linguetta
    leg(m, RLEG, 'strut_gap_low', 5, 5.25, 1.75, 4.5, -0.5, 0.5, 'rubber')
    leg(m, RLEG, 'strut_gap_front', 5, 5.25, 4.5, 6.25, -2.75, -2.5, 'rubber')
    leg(m, RLEG, 'strut_gap_back', 4.75, 5.25, 3.5, 6.25, 2.5, 2.75, 'rubber')
    # davanti, accanto al bordo interno del piede: uno schermo scuro davanti al cuneo interno del tallone
    leg(m, RLEG, 'heel_in_screen', 1, 2, 0, 1, -1.25, -1, 'rubber')
    # montanti scuri ai lati del piede
    leg(m, RLEG, 'foot_post_in', 1.25, 2, 1.25, 2, -1, 1, 'gun')
    leg(m, RLEG, 'foot_post_in_flare', 1, 2, 1, 1.25, -1, 1, 'gun')
    leg(m, RLEG, 'foot_post_out', 4.75, 5, 1, 2, -0.75, 0.75, 'gun')
    # piede: anima a tre strati; davanti, dietro la porta del guscio, un incavo con il fondo nero
    for name, a0, a1, y0, y1 in FOOT_CORE:
        leg(m, RLEG, name, a0, a1, y0, y1, -6.25, 3.5, 'steel')
    leg(m, RLEG, 'foot_0_mid', 2.75, 4.25, 0, 1, -5.75, 3.5, 'steel')
    leg(m, RLEG, 'foot_notch_back', 3, 4, 0, 0.75, -6, -5.75, 'rubber')
    # tallone: anime dei tre blocchi e i due solchi scuri tra il centrale e i laterali
    leg(m, RLEG, 'heel_core_center', 2.5, 2.75, 0, 1.75, 3.5, 4.75, 'steel')
    leg(m, RLEG, 'heel_core_out', 3.75, 4.25, 0, 1.25, 3.5, 4.5, 'steel')
    leg(m, RLEG, 'heel_core_in', 1.5, 1.75, 0, 1, 3.5, 4.5, 'steel')
    leg(m, RLEG, 'heel_gap_out', 3.25, 3.5, 0, 1.75, 3.75, 4.75, 'gun')
    leg(m, RLEG, 'heel_gap_in', 2, 2.25, 0, 1.75, 3.75, 4.75, 'gun')
    # incavo del polpaccio dietro l'arco della piastra: fondo nero, barra, blocchetto con un quadretto, due montanti
    leg(m, RLEG, 'calf_recess', 1.75, 4, 2, 4, 3.25, 3.75, 'rubber')
    leg(m, RLEG, 'calf_recess_top', 2, 3.5, 4, 4.25, 3.25, 3.5, 'rubber')
    leg(m, RLEG, 'calf_bar', 2.25, 3.25, 3.5, 3.75, 3.75, 4, 'steel', tone=0.85, bolts=False)
    leg(m, RLEG, 'calf_block', 2.5, 3.25, 2.5, 3.5, 3.75, 4, 'steel')
    leg(m, RLEG, 'calf_block_square', 2.75, 3, 2.75, 3, 4, 4.25, 'chrome')
    leg(m, RLEG, 'calf_post_out', 3.5, 3.75, 2, 3, 3.75, 4, 'steel')
    leg(m, RLEG, 'calf_post_in', 2, 2.25, 2, 3, 3.75, 4, 'steel')
    # buca del ginocchio sopra la piastra del polpaccio: fondo nero e tre aste chiare, la centrale con un cappuccio
    leg(m, RLEG, 'knee_pit', 1.5, 3.5, 6.25, 7.5, 3, 3.25, 'rubber')
    leg(m, RLEG, 'knee_pit_rod_in', 1.75, 2, 6.25, 7.25, 3.25, 3.5, 'steel')
    leg(m, RLEG, 'knee_pit_rod_mid', 2.25, 2.75, 6.25, 7, 3.25, 3.5, 'steel')
    leg(m, RLEG, 'knee_pit_rod_cap', 2.25, 2.75, 7, 7.25, 3.25, 3.5, 'chrome')
    leg(m, RLEG, 'knee_pit_rod_out', 3, 3.25, 6.25, 7.25, 3.25, 3.5, 'steel')
