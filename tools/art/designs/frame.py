"""Telaio nudo: lo scheletro della tuta, sempre visibile, anche sotto i pezzi, ed e' cio' che sta in piedi nel mondo
senza pezzi montati. E' il telaio della quarta passata della prova d'arte, approvata da Luigi il 13 novembre 2026:
scheletro massiccio, piu' grande del pilota alle braccia, al petto e alle gambe, con petto a botte, testa incassata,
spalle enormi, vita stretta, gambe e stivali grossi. Solo parti di macchina (travi, alloggiamenti degli attuatori,
pistoni, mozzi); le piastre sono i pezzi (designs/scrap.py), qui ci sono i punti dove si montano (alette, borchie).
Piedi (dita divise, cerniera, puntoni della caviglia, appoggio del tallone) e parte bassa e laterale della schiena
(trave lombare con feritoie, puntoni verso il giogo) sono della quinta passata; la cella e il suo alloggiamento no.

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
ORDER = 0

HEAD, BODY, RARM, RLEG = 'frameHead', 'frameBody', 'frameRightArm', 'frameRightLeg'


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
    # parte bassa della schiena: trave lombare sotto la vita, fascia di feritoie orizzontali sotto l'alloggiamento della
    # cella, blocco della spina in fondo
    m.box(BODY, 'lumbar_beam', (-5.5, 11, 3.25), (5.5, 12.5, 5), 'gun')
    m.box(BODY, 'lumbar_vent', (-5.25, 11.75, 5), (5.25, 13.5, 5.5), 'vent')
    m.box(BODY, 'spine_block', (-1.25, 10, 3.5), (1.25, 11, 5), 'gun')
    # puntoni a gradini dalla trave lombare, ai lati dell'alloggiamento, fino al giogo dietro le spalle
    for k, (x0, y0, y1) in enumerate(((-7.25, 12, 15.25), (-8.5, 15.25, 18.75), (-9.75, 18.75, 22.25), (-10.5, 22.25, 24.75))):
        m.sym(BODY, f'back_strut_{k}', (x0, y0, 4.25), (x0 + 1.5, y1, 5.5), 'steel' if k % 2 else 'gun')


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
    # ginocchio grande
    m.sym(RLEG, 'knee_block', (-5.25, 4.5, -2.5), (-0.5, 7, 2.5), 'gun')
    m.sym(RLEG, 'knee_hub', (-6.5, 4, -2.25), (-5.25, 7.5, 2.25), 'brass', hub=True)
    m.sym(RLEG, 'knee_cap', (-4.25, 4.75, -3.75), (-1.25, 7.25, -2.5), 'brass', hub=True)
    # stinco spesso: due montanti, pistone davanti, pistone del polpaccio
    m.sym(RLEG, 'shin_outer', (-6, 2, -1.5), (-4, 4.5, 1.5), 'gun')
    m.sym(RLEG, 'shin_inner', (-2, 2, -1.75), (-0.5, 4.5, 1.75), 'gun')
    m.sym(RLEG, 'shin_ram', (-3.5, 2.25, -3), (-2, 4.75, -1.5), 'steel')
    m.sym(RLEG, 'calf_ram', (-4, 3, 2), (-1.5, 6.5, 3.5), 'steel')
    m.sym(RLEG, 'calf_rod', (-3.25, 1.75, 2.5), (-2.25, 3, 3.25), 'chrome')
    # caviglia: blocco sopra il piede, mozzo esterno e puntoni a gradini sui due lati, dal tallone allo stinco
    m.sym(RLEG, 'ankle_block', (-5.75, 1.75, -2), (-1, 2.75, 2), 'gun')
    m.sym(RLEG, 'ankle_hub', (-7.75, 1, -1.25), (-7, 3, 1.25), 'brass', hub=True)
    m.sym(RLEG, 'ankle_strut_out_0', (-8, 1, 2), (-6.75, 2.25, 3.5), 'steel')
    m.sym(RLEG, 'ankle_strut_out_1', (-7.75, 2.25, 1.25), (-7, 3.5, 2.75), 'steel')
    m.sym(RLEG, 'ankle_strut_out_2', (-7.5, 3.5, 0.5), (-6, 4.75, 2), 'steel')
    m.sym(RLEG, 'ankle_strut_in_0', (-0.75, 1, 2), (-0.25, 2.25, 3.5), 'steel')
    m.sym(RLEG, 'ankle_strut_in_1', (-0.75, 2.25, 1.25), (-0.25, 3.5, 2.75), 'steel')
    m.sym(RLEG, 'ankle_strut_in_2', (-0.75, 3.5, 0.5), (-0.25, 4.75, 2), 'steel')
    # piede grande: base, collo del piede, tallone; davanti due dita squadrate con un'apertura in mezzo, su una
    # cerniera d'ottone che si vede dai due lati; dietro il tallone un appoggio a due gradini
    m.sym(RLEG, 'foot_base', (-7, 0, -3), (-0.5, 1, 4.5), 'gun')
    m.sym(RLEG, 'instep', (-6.75, 1, -3), (-0.75, 2, 1.25), 'steel')
    m.sym(RLEG, 'heel', (-6.75, 1, 1.25), (-0.75, 2.25, 4.25), 'gun')
    for side, (x0, x1) in (('out', (-7.5, -4.25)), ('in', (-3.75, -0.5))):
        m.sym(RLEG, f'toe_{side}', (x0, 0, -8), (x1, 1.25, -3), 'gun')
        m.sym(RLEG, f'toe_{side}_cap', (x0 + 0.25, 1.25, -7.75), (x1 - 0.25, 1.75, -3.25), 'steel')
    m.sym(RLEG, 'toe_hinge', (-7.75, 0.25, -3.75), (-0.25, 1.5, -2.5), 'brass', hub=True)
    m.sym(RLEG, 'heel_strut', (-5.75, 0, 4.5), (-1.75, 1.25, 6.5), 'gun')
    m.sym(RLEG, 'heel_spur', (-5.25, 0, 6.5), (-2.25, 0.75, 7.25), 'steel')
    # borchie per i pezzi delle gambe
    m.sym(RLEG, 'shin_boss', (-6.25, 2.75, -0.5), (-6, 3.75, 0.5), 'chrome')
