"""Telaio nudo: sempre visibile, anche sotto i pezzi, ed e' cio' che sta in piedi nel mondo senza pezzi montati.

Coordinate in pixel del file (minimo -> massimo), fronte verso -z, lato destro a x negativa. Le parti simmetriche si
scrivono sul lato destro e sym() aggiunge la copia sinistra. Nessuna scatola ruotata. Il giocatore di riferimento
(exo.PLAYER) resta libero: almeno 0.25 px dal corpo e dagli arti, 0.5 px dalla testa.
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
    body(m)
    arms(m)
    legs(m)


def head(m):
    # gabbia leggera: due montanti alle tempie, un arco sopra la testa e un cerchio alla fronte; il viso resta libero
    m.sym(HEAD, 'temple_post', (-6.5, 25.5, -1), (-4.5, 32.5, 1), 'frame')
    m.sym(HEAD, 'temple_hub', (-7.5, 27, -1.5), (-6.5, 30, 1.5), 'joint')
    m.box(HEAD, 'crown_arc', (-6.5, 32.5, -0.5), (6.5, 33.5, 0.5), 'frame')
    m.box(HEAD, 'brow_bar', (-4.5, 30, -5.5), (4.5, 31, -4.5), 'frame')
    m.sym(HEAD, 'brow_side', (-5.5, 30, -5.5), (-4.5, 31, -1), 'frame')
    m.box(HEAD, 'nape_bar', (-4.5, 30, 4.5), (4.5, 31, 5.5), 'frame')
    m.sym(HEAD, 'nape_side', (-5.5, 30, 1), (-4.5, 31, 5.5), 'frame')
    m.box(HEAD, 'brow_lamp', (2, 30.25, -6.5), (4, 31.25, -5.5), 'lamp')


def body(m):
    # collare: barre sulle spalle unite dietro al collo (il fronte resta aperto)
    m.sym(BODY, 'yoke_top', (-6.5, 24.25, -3), (-4.5, 25.25, 5.5), 'frame')
    m.box(BODY, 'yoke_back', (-4.5, 24.25, 4.5), (4.5, 25.25, 5.5), 'frame')
    m.sym(BODY, 'yoke_post', (-2.5, 23.25, 4.5), (-1.5, 24.25, 5.5), 'frame')
    # alloggiamento della cella sulla schiena, con la cella a vista dietro due barre
    m.box(BODY, 'housing', (-3, 14.25, 2.25), (3, 23.25, 5.25), 'steel', rust=0.25)
    m.box(BODY, 'cell', (-2, 15.25, 5.25), (2, 22.25, 6.25), 'cell')
    m.box(BODY, 'cell_guard_top', (-2.5, 20, 6.25), (2.5, 21, 7.25), 'frame')
    m.box(BODY, 'cell_guard_bottom', (-2.5, 16.5, 6.25), (2.5, 17.5, 7.25), 'frame')
    m.box(BODY, 'status_lamp', (3, 21, 3.5), (4, 22, 4.5), 'lamp')
    # spina dorsale sotto l'alloggiamento fino alla cintura
    m.box(BODY, 'spine', (-1, 13.75, 2.25), (1, 14.25, 4.25), 'frame')
    # cavi di redstone dalla cella verso le spalle
    m.sym(BODY, 'wire', (-6, 22.25, 2.25), (-3, 23.25, 3.25), 'wire')
    # cavi di rame dall'alloggiamento alla cintura
    m.sym(BODY, 'back_cable', (-4, 13.75, 3.25), (-3, 20, 4.25), 'cable')
    # imbracatura sul petto
    m.sym(BODY, 'chest_strap', (-3.5, 14, -3.25), (-2.5, 23, -2.25), 'frame')
    m.box(BODY, 'chest_bar', (-2.5, 19, -3.25), (2.5, 20, -2.25), 'frame')
    m.box(BODY, 'chest_plate', (-1.5, 17, -4.25), (1.5, 21, -3.25), 'steel')
    # cintura davanti e dietro
    m.box(BODY, 'belt_front', (-4, 12, -3.25), (4, 14, -2.25), 'steel', rivets=False)
    m.box(BODY, 'belt_back', (-4, 12, 2.25), (4, 13.75, 4.25), 'steel', rivets=False)


def arms(m):
    m.sym(RARM, 'shoulder_hub', (-10.25, 20.5, -1.5), (-8.25, 23.5, 1.5), 'joint')
    m.sym(RARM, 'shoulder_cap', (-9.25, 24.25, -2), (-6.5, 25.25, 2), 'frame')
    m.sym(RARM, 'shoulder_drop', (-9.25, 23.5, -1), (-8.25, 24.25, 1), 'frame')
    m.sym(RARM, 'upper_strut', (-9.25, 17.5, -0.5), (-8.25, 20.5, 0.5), 'frame')
    m.sym(RARM, 'elbow_hub', (-10.25, 15, -1.5), (-8.25, 17.5, 1.5), 'joint')
    m.sym(RARM, 'fore_strut', (-9.25, 14.5, -0.5), (-8.25, 15, 0.5), 'frame')
    # polsiera a U (il lato interno del braccio tocca il corpo)
    m.sym(RARM, 'wrist_outer', (-9.25, 12, -2.25), (-8.25, 14.5, 2.25), 'steel', rivets=False)
    m.sym(RARM, 'wrist_front', (-9.25, 12, -3.25), (-4.25, 14.5, -2.25), 'steel', rivets=False)
    m.sym(RARM, 'wrist_back', (-9.25, 12, 2.25), (-4.25, 14.5, 3.25), 'steel', rivets=False)
    # pistone dietro il braccio, dal gomito alla polsiera
    m.sym(RARM, 'arm_cylinder', (-7.5, 17, 2.25), (-5.5, 21, 4.25), 'brass')
    m.sym(RARM, 'arm_rod', (-7, 14.5, 2.75), (-6, 17, 3.75), 'chrome')
    # piastra della mano davanti al pugno
    m.sym(RARM, 'knuckle_plate', (-8.25, 10.5, -3.25), (-4.25, 12, -2.25), 'steel', rivets=False)


def legs(m):
    m.sym(RLEG, 'hip_hub', (-6.25, 8.5, -1.5), (-4.25, 11.5, 1.5), 'joint')
    m.sym(RLEG, 'outer_strut', (-5.25, 2, -0.5), (-4.25, 8.5, 0.5), 'frame')
    m.sym(RLEG, 'thigh_strut', (-2.5, 7.5, -3.25), (-1.5, 11.5, -2.25), 'frame')
    m.sym(RLEG, 'knee_hub', (-3.5, 4.5, -4.25), (-0.5, 7.5, -2.25), 'joint')
    m.sym(RLEG, 'shin_strut', (-2.5, 2, -3.25), (-1.5, 4.5, -2.25), 'frame')
    m.sym(RLEG, 'calf_cylinder', (-3, 5, 2.25), (-1, 10, 4.25), 'brass')
    m.sym(RLEG, 'calf_rod', (-2.5, 2, 2.75), (-1.5, 5, 3.75), 'chrome')
    # caviglia a U e piastra del piede sotto la suola
    m.sym(RLEG, 'ankle_front', (-4.25, 1, -3.25), (-0.25, 2, -2.25), 'steel', rivets=False)
    m.sym(RLEG, 'ankle_outer', (-5.25, 1, -2.25), (-4.25, 2, 2.25), 'steel', rivets=False)
    m.sym(RLEG, 'ankle_back', (-4.25, 1, 2.25), (-0.25, 2, 3.25), 'steel', rivets=False)
    m.sym(RLEG, 'toe_cap', (-4.25, -0.25, -4.25), (-0.25, 1, -2.25), 'steel', rivets=False)
    m.sym(RLEG, 'sole', (-4.75, -1, -4.25), (-0.25, -0.25, 3.25), 'frame')
