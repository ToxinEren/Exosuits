"""Telaio nudo: lo scheletro della tuta, sempre visibile, anche sotto i pezzi, ed e' cio' che sta in piedi nel mondo
senza pezzi montati. Niente piastre di copertura: le piastre sono i pezzi (un design a parte); qui ci sono i punti
dove si montano (alette, borchie).

Il renderer scala la tuta di exo.SUIT_SCALE (1.4) attorno ai piedi e disegna il pilota a grandezza normale sollevato
di exo.PILOT_LIFT_PX (24(S-1) = 9.6 px), senza gambe: nelle coordinate del modello il pilota e' un giocatore scalato
di 1/S attorno a (0,24,0). A S = 1.4: testa y 24..29.71 (x, z +-2.86); busto y 15.43..24 (x +-2.86, z +-1.43);
braccia x +-2.86..5.71, y 15.43..24; le mani finiscono a y 15.43 sulle impugnature dentro la gabbia
dell'avambraccio. Le gambe del pilota (x +-2.86, y 6.86..15.43) sono nascoste dentro le cosce chiuse e il sedile del
bacino. Testa e sedile sono dimensionati per S = 1.4: con un'altra scala vanno ricontrollati.
Ingombro del telaio nudo: x +-9.5, y 0..30.7, z -7..9 (exo.FRAME_MIN/MAX). Fronte verso -z, lato destro a x
negativa; sym() scrive il lato destro e aggiunge il sinistro. Nessuna scatola ruotata.

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
    back(m)
    arms(m)
    legs(m)


def head(m):
    # testa scheletrica attorno alla testa del pilota (x, z +-3.21 con il margine): calotta, tempie, guance e
    # mascelle, nuca, cornice della visiera aperta davanti al viso
    m.box(HEAD, 'crown', (-4.5, 30.25, -4.25), (4.5, 30.5, 4.5), 'gun')
    m.sym(HEAD, 'temple', (-5, 27.25, -4), (-3.5, 30.25, 4), 'gun')
    m.sym(HEAD, 'jaw', (-5, 24.25, -4.25), (-3.5, 27.25, 1.75), 'steel')
    m.sym(HEAD, 'ear_hub', (-5.5, 25, -1.25), (-4.75, 27.5, 1.25), 'brass', hub=True)
    m.box(HEAD, 'nape', (-3.5, 24.25, 3.5), (3.5, 30.25, 4.5), 'gun')
    m.box(HEAD, 'brow', (-4.75, 28.75, -4.75), (4.75, 30, -3.5), 'steel')
    m.box(HEAD, 'chin', (-4.75, 22.75, -4.75), (4.75, 23.75, -3.5), 'gun')
    m.sym(HEAD, 'visor_post', (-3.5, 23.75, -4.5), (-3, 28.75, -3.5), 'gun')
    m.sym(HEAD, 'brow_lamp', (-3.25, 29.25, -5), (-2.25, 29.75, -4.75), 'lamp')
    m.box(HEAD, 'cheek_sensor', (-5.25, 25.5, -3.75), (-5, 26.5, -2.5), 'lamp')
    # alette per l'elmo
    m.sym(HEAD, 'helmet_lug', (-5.25, 28.25, 1.5), (-5, 29.5, 2.75), 'chrome')


def torso(m):
    # sedile del bacino: fondo, bordo davanti, schienale; chiude in alto le prese delle cosce
    m.box(BODY, 'seat', (-3, 12.5, -2.75), (3, 15, 2.75), 'gun')
    m.box(BODY, 'seat_lip', (-3, 15, -3.25), (3, 16.25, -2), 'steel')
    m.box(BODY, 'seat_back', (-3, 15, 2), (3, 18, 3.25), 'gun')
    m.sym(BODY, 'seat_bolt', (-2.5, 13.25, -3), (-1.5, 14.25, -2.75), 'chrome')
    # gabbia del petto: montanti, costole; il busto del pilota resta a vista
    m.sym(BODY, 'cage_strut', (-3.75, 16.25, -3.25), (-2.75, 23.5, -2.25), 'gun')
    for k, y in enumerate((18, 20.5)):
        m.box(BODY, f'cage_rib_{k}', (-2.75, y, -3), (2.75, y + 0.75, -2.25), 'gun')
    # giogo: davanti sopra il petto, sui fianchi, dietro la nuca
    m.sym(BODY, 'yoke_front', (-7, 23.5, -3.75), (-3.75, 25, -2.25), 'gun')
    m.sym(BODY, 'yoke_side', (-7, 24.25, -2.25), (-6.25, 25.75, 4.75), 'gun')
    m.box(BODY, 'yoke_back', (-7, 23, 4.75), (7, 25.75, 6), 'gun')
    # borchie per il torso
    m.sym(BODY, 'torso_boss', (-3.5, 21.75, -3.5), (-3, 22.5, -3.25), 'chrome')
    m.sym(BODY, 'yoke_lug', (-6, 25, -3.5), (-5, 25.75, -2.75), 'steel')


def back(m):
    # spina dorsale a vertebre con anima cromata, staffe che reggono la cella
    m.box(BODY, 'spine_core', (-1, 15.25, 2.5), (1, 23.25, 3.5), 'chrome')
    for k, y in enumerate((18.25, 19.75, 21.25)):
        m.box(BODY, f'vertebra_{k}', (-2, y, 2.25), (2, y + 1, 3.75), 'gun')
    m.box(BODY, 'spine_top', (-1, 22.5, 3.75), (1, 23.5, 4.75), 'gun')
    for k, y in enumerate((16, 21)):
        m.sym(BODY, f'pack_bracket_{k}', (-1.75, y, 3.5), (-1.25, y + 0.75, 6), 'steel')
    # alloggiamento della cella: due colonne, fondo, cornice, cella incassata, barre di protezione
    m.sym(BODY, 'pack_column', (-5.25, 14.5, 6), (-2.75, 25, 8.5), 'steel')
    m.box(BODY, 'pack_top', (-5.5, 25, 5.5), (5.5, 26.25, 8.75), 'gun')
    m.box(BODY, 'pack_bottom', (-5.5, 13.5, 6), (5.5, 14.5, 8.75), 'gun')
    m.box(BODY, 'pack_floor', (-2.75, 14.5, 6), (2.75, 25, 7), 'gun')
    m.box(BODY, 'cell', (-2.25, 15.5, 7), (2.25, 24, 8), 'cell')
    m.box(BODY, 'cell_frame_top', (-2.75, 24, 7), (2.75, 25, 8.25), 'gun')
    m.box(BODY, 'cell_frame_bottom', (-2.75, 14.5, 7), (2.75, 15.5, 8.25), 'gun')
    for k, y in enumerate((17, 19.75, 22.5)):
        m.box(BODY, f'cell_guard_{k}', (-3.25, y, 8.25), (3.25, y + 0.75, 9), 'gun')
    m.sym(BODY, 'pack_vent', (-5.5, 16, 6.5), (-5.25, 23, 8.25), 'vent')
    m.sym(BODY, 'pack_lamp', (-4.75, 25.25, 8.75), (-3.75, 26, 9), 'lamp')
    # due camini di scarico, fascia d'ottone, bocca calda (la cima resta sotto il box di collisione)
    m.sym(BODY, 'stack', (-4.75, 26.25, 6.25), (-3.25, 29.75, 7.75), 'gun', heat=True)
    m.sym(BODY, 'stack_band', (-5, 27.5, 6), (-3, 28.25, 8), 'brass')
    m.sym(BODY, 'stack_cap', (-5, 29.75, 6), (-3, 30.5, 8), 'vent')
    # cavi di rame: dalla colonna al giogo; dal fondo della cella al sedile; tubi di gomma al sedile
    m.sym(BODY, 'cable_a', (-6.5, 24, 6.5), (-5.25, 25, 7.5), 'cable')
    m.sym(BODY, 'cable_a_fwd', (-6.5, 24, 5.5), (-5.75, 25, 6.5), 'cable')
    m.sym(BODY, 'cable_b_down', (-4.75, 12.25, 6.5), (-4, 13.5, 7.25), 'cable')
    m.sym(BODY, 'cable_b_in', (-4.75, 12.25, 2.5), (-4, 13, 6.5), 'cable')
    m.sym(BODY, 'hose', (-3.5, 12.25, 6.75), (-2.5, 13.5, 7.75), 'rubber')
    m.sym(BODY, 'hose_fwd', (-3.5, 12.25, 2.5), (-2.5, 13.25, 6.75), 'rubber')


def arms(m):
    # spalla sopra e fuori dalla spalla del pilota: telaio, grande mozzo, alette per lo spallaccio
    m.sym(RARM, 'shoulder_housing', (-9, 23, -2.5), (-7.25, 27, 2.5), 'gun')
    m.sym(RARM, 'shoulder_hub', (-9.5, 23.5, -1.75), (-9, 26.5, 1.75), 'brass', hub=True)
    for k, z in enumerate((-1.75, 0.75)):
        m.sym(RARM, f'pauldron_lug_{k}', (-8.75, 27, z), (-7.5, 27.75, z + 1), 'steel')
    # braccio sul lato esterno: trave, pistoni davanti e dietro
    m.sym(RARM, 'upper_beam', (-8.75, 19.5, -1), (-6.5, 23, 1), 'gun')
    m.sym(RARM, 'bicep_sleeve', (-8.5, 20.5, -2.5), (-7, 23, -1.25), 'steel')
    m.sym(RARM, 'bicep_rod', (-8.25, 19.5, -2.25), (-7.25, 20.5, -1.5), 'chrome')
    m.sym(RARM, 'tricep_sleeve', (-8.5, 20.5, 1.25), (-7, 23, 2.5), 'steel')
    m.sym(RARM, 'tricep_rod', (-8.25, 19.5, 1.5), (-7.25, 20.5, 2.25), 'chrome')
    m.sym(RARM, 'arm_cable', (-9.25, 15.75, 1), (-8.75, 23, 1.75), 'cable')
    # gomito
    m.sym(RARM, 'elbow_block', (-9, 17.25, -2), (-6.25, 19.5, 2), 'gun')
    m.sym(RARM, 'elbow_hub', (-9.5, 17.5, -1.25), (-9, 19.25, 0.75), 'brass', hub=True)
    # avambraccio: trave esterna e gabbia con l'impugnatura sotto la mano del pilota
    m.sym(RARM, 'forearm_beam', (-9, 14.5, -1), (-6.5, 17.25, 1), 'gun')
    m.sym(RARM, 'forearm_lamp', (-9.25, 15.5, -0.5), (-9, 16.25, 0.5), 'lamp')
    m.sym(RARM, 'cage_front', (-6.5, 15.75, -2.75), (-3.75, 16.5, -2), 'steel')
    m.sym(RARM, 'cage_back', (-6.5, 15.75, 2), (-3.75, 16.5, 2.75), 'steel')
    m.sym(RARM, 'grip', (-6.5, 14.5, -2.75), (-3.25, 15, 2.75), 'chrome')
    m.sym(RARM, 'cage_post_front', (-3.75, 15, -2.75), (-3.25, 16.5, -2), 'gun')
    m.sym(RARM, 'cage_post_back', (-3.75, 15, 2), (-3.25, 16.5, 2.75), 'gun')
    # mano meccanica: palmo, tre dita in due falangi, pollice
    m.sym(RARM, 'palm', (-9, 12, -2), (-5.5, 14.5, 2), 'steel')
    for k, (x0, x1) in enumerate(((-9, -8), (-7.75, -6.75), (-6.5, -5.5))):
        m.sym(RARM, f'finger{k}', (x0, 10.5, -2), (x1, 12, -0.75), 'gun')
        m.sym(RARM, f'fingertip{k}', (x0, 9.75, -1.75), (x1, 10.5, -1), 'gun')
    m.sym(RARM, 'thumb', (-5.5, 11.5, -1.75), (-4.75, 13.5, -0.5), 'gun')
    m.sym(RARM, 'knuckle_bar', (-9.25, 12, -2.5), (-5.25, 12.75, -2), 'brass')


def legs(m):
    # presa della coscia: manicotto chiuso dove stanno le gambe del pilota, con due fasce e un pistone davanti
    m.sym(RLEG, 'thigh_socket', (-4, 6.75, -2.25), (0, 14, 2.25), 'gun')
    for k, y in enumerate((8, 11.5)):
        m.sym(RLEG, f'thigh_band_{k}', (-4.25, y, -2.5), (-0.25, y + 0.75, 2.5), 'steel')
    m.sym(RLEG, 'hip_hub', (-4.75, 11, -1.5), (-4.25, 13.5, 1.5), 'brass', hub=True)
    m.sym(RLEG, 'thigh_sleeve', (-3.25, 8.75, -3.25), (-1, 11.25, -2.5), 'steel')
    m.sym(RLEG, 'thigh_rod', (-2.75, 7.5, -3), (-1.5, 8.75, -2.5), 'chrome')
    # ginocchio: blocco, mozzo esterno, snodo davanti
    m.sym(RLEG, 'knee_block', (-4, 5.25, -1.75), (-0.5, 6.75, 1.75), 'gun')
    m.sym(RLEG, 'knee_hub', (-5, 5, -1.5), (-4, 7.5, 1.5), 'brass', hub=True)
    m.sym(RLEG, 'knee_cap', (-3.25, 5.5, -2.75), (-1.25, 7.5, -1.75), 'brass', hub=True)
    # stinco: due montanti, pistone davanti, pistone del polpaccio dietro
    m.sym(RLEG, 'shin_outer', (-4.5, 2.25, -1), (-3.25, 5.25, 1), 'gun')
    m.sym(RLEG, 'shin_inner', (-1.75, 2.25, -1), (-0.5, 5.25, 1), 'gun')
    m.sym(RLEG, 'shin_sleeve', (-3, 3, -2.25), (-1.75, 5.5, -1.25), 'steel')
    m.sym(RLEG, 'shin_rod', (-2.75, 2.25, -2), (-2, 3, -1.5), 'chrome')
    m.sym(RLEG, 'calf_sleeve', (-3.25, 3.75, 1.75), (-1.25, 7, 3), 'steel')
    m.sym(RLEG, 'calf_rod', (-2.75, 2, 2), (-1.75, 3.75, 2.75), 'chrome')
    # caviglia e piede: blocco, mozzo, suola, punta, calotta, tallone, sperone
    m.sym(RLEG, 'ankle_block', (-4.25, 1, -1.5), (-0.5, 2.25, 1.5), 'gun')
    m.sym(RLEG, 'ankle_hub', (-5, 1, -1.25), (-4.25, 2.75, 1.25), 'brass', hub=True)
    m.sym(RLEG, 'sole', (-5.25, 0, -5.5), (-0.25, 1, 3.75), 'gun')
    m.sym(RLEG, 'toe', (-5, 1, -5.25), (-0.5, 2, -1.75), 'steel')
    m.sym(RLEG, 'toe_cap', (-4.5, 2, -4.75), (-1, 2.5, -2), 'gun')
    m.sym(RLEG, 'heel', (-4.75, 1, 1.75), (-0.5, 2, 3.5), 'steel')
    m.sym(RLEG, 'heel_spur', (-4.25, 0.25, 3.75), (-1, 1.5, 4.5), 'gun')
    # borchie per i pezzi delle gambe (sulla faccia esterna del montante dello stinco e della presa della coscia)
    m.sym(RLEG, 'shin_boss', (-4.75, 3.25, -0.5), (-4.5, 4.25, 0.5), 'chrome')
    m.sym(RLEG, 'thigh_boss', (-4.25, 9.25, -0.5), (-4, 10.25, 0.5), 'chrome')
