"""The three Rottame (Scrap) directions. Coordinates are (x, y, z) min -> max in px.
Front of the suit faces -z. Right-side parts are written at -x and mirrored with sym()."""
from exo import Model

H, B, RA, LA, RL, LL = ('armorHead', 'armorBody', 'armorRightArm', 'armorLeftArm',
                        'armorRightLeg', 'armorLeftLeg')


def frame(m, variant):
    F = 'frame'
    # --- head: a light halo brace, always visible
    m.sym(H, F, (-4.5, 25, -1), (-3.5, 30, 1), 'frame', name='head_brace')
    m.box(H, F, (-3.5, 31.5, -1), (3.5, 32.5, 1), 'frame', name='head_strap')
    m.box(H, F, (-4.5, 29, -1.5), (-3.5, 30, -0.5), 'light', name='head_lamp')
    # --- body: spine, yoke, belt, chest struts, power housing + cell
    m.box(B, F, (-1, 14, 2), (1, 26, 4), 'frame', name='spine')
    m.box(B, F, (-7, 22, 2.5), (7, 23, 3.5), 'frame', name='yoke')
    m.box(B, F, (-4.5, 12, -2.5), (4.5, 14, 2.5), 'frame', name='belt')
    m.sym(B, F, (-3, 14, -3), (-2, 23, -2), 'frame', name='chest_strut')
    m.box(B, F, (-4, 20, -3.5), (4, 21, -2.5), 'frame', name='chest_bar')
    m.sym(B, F, (-6.5, 21, 2.5), (-1.5, 22, 3.5), 'wire', name='yoke_wire')
    if variant == 'C':
        # low saddle mount for the boiler; the cell sits in a squat cradle
        m.box(B, F, (-4, 14, 3), (4, 20, 6), 'iron3', name='housing', opts={'rust': 0.3})
        m.box(B, F, (-3, 15, 5), (3, 19, 7), 'cell', name='cell')
        m.sym(B, F, (-4, 20, 2.5), (-3, 24, 3.5), 'frame', name='mount_post')
    elif variant == 'B':
        # open cage: the cell is visible, wrapped in copper coils
        m.box(B, F, (-3, 22, 2.5), (3, 23, 6.5), 'frame', name='cage_top')
        m.box(B, F, (-3, 14, 2.5), (3, 15, 6.5), 'frame', name='cage_bottom')
        m.sym(B, F, (-3, 15, 5.5), (-2, 22, 6.5), 'frame', name='cage_bar')
        m.box(B, F, (-2, 15, 3), (2, 22, 6), 'cell', name='cell')
        m.box(B, F, (-2.5, 17, 2.5), (2.5, 18, 6.5), 'pipe', name='coil1')
        m.box(B, F, (-2.5, 19.5, 2.5), (2.5, 20.5, 6.5), 'pipe', name='coil2')
        m.box(B, F, (-1, 23, 4), (1, 24, 5), 'light', name='status')
    else:
        m.box(B, F, (-3, 13, 3), (3, 24, 6), 'iron3', name='housing', opts={'rust': 0.3})
        m.box(B, F, (-2, 15, 5), (2, 22, 7), 'cell', name='cell')
        m.box(B, F, (1, 24, 4), (2, 25, 5), 'light', name='status')
    # --- arms
    m.sym(RA, F, (-9.5, 19.5, -1.5), (-7.5, 22.5, 1.5), 'joint', name='shoulder_joint')
    m.sym(RA, F, (-9, 16, -0.5), (-8, 20, 0.5), 'frame', name='upper_strut')
    m.sym(RA, F, (-9.5, 14, -1.5), (-7.5, 17, 1.5), 'joint', name='elbow_joint')
    m.sym(RA, F, (-8.5, 12, -2.5), (-3.5, 14, 2.5), 'frame', name='wrist_band')
    m.sym(RA, F, (-8.5, 10, -2.5), (-3.5, 12, 2.5), 'iron3', name='hand_plate', opts={'rivets': False})
    m.sym(RA, F, (-9, 22.5, -2), (-5, 24.5, 2), 'frame', name='shoulder_bracket')
    # --- legs
    m.sym(RL, F, (-3.5, 8, -3), (-2.5, 12, -2), 'frame', name='thigh_strut')
    m.sym(RL, F, (-4, 5, -4), (-1, 8, -2), 'joint', name='knee_joint')
    m.sym(RL, F, (-3.5, 1, -3), (-2.5, 5, -2), 'frame', name='shin_strut')
    m.sym(RL, F, (-5, 2, -0.5), (-4, 8, 0.5), 'frame', name='calf_strut')
    m.sym(RL, F, (-5, 0, -6), (0, 1, 3), 'frame', name='sole')
    m.sym(RL, F, (-4.5, 1, -2.5), (-0.5, 2, 2.5), 'frame', name='ankle_band')
    m.sym(RL, F, (-3, 6, 2.5), (-1, 11, 4.5), 'brass', name='calf_cyl')
    m.sym(RL, F, (-2.5, 2, 3), (-1.5, 6, 4), 'chrome', name='calf_rod')
    if variant == 'B':
        # extra exposed hydraulics: the frame carries the look of this direction
        m.sym(RA, F, (-7.5, 17, -4), (-5.5, 21, -2), 'brass', name='arm_cyl')
        m.sym(RA, F, (-7, 13, -3.5), (-6, 17, -2.5), 'chrome', name='arm_rod')
        m.sym(RL, F, (-4.5, 8, 2.5), (-3.5, 12, 3.5), 'wire', name='leg_wire')


def build_A():
    """Bulkhead: slab plates, tall squared shoulders, visor helmet sunk between them."""
    m = Model('A')
    frame(m, 'A')
    P = 'piece'
    # helmet
    m.box(H, P, (-4.5, 24, -4.5), (4.5, 33, 4.5), 'iron', name='helm_shell', opts={'seam': False})
    m.box(H, P, (-3.5, 27, -5.5), (3.5, 29, -4.5), 'visor', name='helm_visor')
    m.box(H, P, (-4.5, 29, -5.5), (4.5, 30, -4.5), 'iron2', name='helm_brow')
    m.box(H, P, (-2.5, 24, -5.5), (2.5, 27, -4.5), 'grill', name='helm_jaw')
    m.sym(H, P, (-4.5, 24, -5.5), (-2.5, 27, -4.5), 'paint2', name='helm_cheek', opts={'rivets': False})
    m.sym(H, P, (-5.5, 26, -2), (-4.5, 29, 1), 'copper', name='helm_ear')
    m.box(H, P, (-2.5, 33, -3), (2.5, 34, 2), 'iron2', name='helm_hatch')
    # torso
    m.box(B, P, (-4.5, 15, -4), (4.5, 24, 4), 'iron', name='torso_core')
    m.box(B, P, (-4.5, 16, -5), (4.5, 24, -4), 'paint1', name='chest_plate')
    m.box(B, P, (-3.5, 16.5, -6), (3.5, 23.5, -5), {'default': 'iron3', 'north': 'decal'}, name='chest_decal')
    m.box(B, P, (-4, 12, -4), (4, 15, 3), 'grill', name='abdomen')
    m.sym(B, P, (-4.5, 16, 4), (-3.5, 24, 5), 'grill', name='back_vent', opts={'hot': True})
    # arms
    for side in (RA, LA):
        s = -1 if side == RA else 1
        def X(a, b):
            return (min(s * a, s * b), max(s * a, s * b))
        def bx(x, y, z, x2, y2, z2, mat, name, **kw):
            xa, xb = X(x, x2)
            return m.box(side, P, (xa, y, z), (xb, y2, z2), mat, name=name + ('_r' if s < 0 else '_l'), **kw)
        bx(10, 21, -4, 4, 30, 4, 'paint1', 'pauldron')
        bx(11, 21, -4.5, 10, 30, 4.5, 'iron2', 'pauldron_plate')
        bx(8.5, 16, -2.5, 3.5, 21, 2.5, 'iron3', 'sleeve', opts={'rivets': False})
        bx(10, 12, -3.5, 4, 17, 3.5, 'paint2', 'gauntlet')
        bx(7, 17, 2.5, 5, 21, 4.5, 'brass', 'arm_cyl')
        bx(6.5, 13, 3, 5.5, 17, 4, 'chrome', 'arm_rod')
    m.box(LA, P, (11, 22, -3.5), (12, 29, 3.5), {'default': 'iron3', 'east': 'decal'}, name='shoulder_decal')
    m.box(RA, P, (-12, 24, -2), (-11, 27, 1), 'copper', name='shoulder_patch')
    # legs
    m.sym(RL, P, (-5, 7, -3.5), (0, 12, 2.5), 'paint1', name='thigh')
    m.sym(RL, P, (-4.5, 5, -4.5), (-0.5, 8, -3.5), 'copper', name='knee_cop')
    m.sym(RL, P, (-4.5, 5, -3.5), (-0.5, 7, 2.5), 'iron3', name='shin')
    m.sym(RL, P, (-5, 1, -5), (0, 5, 3), 'iron', name='boot')
    return m


def build_B():
    """Scavenger: open frame, pistons on show, small mismatched asymmetric plates."""
    m = Model('B')
    frame(m, 'B')
    P = 'piece'
    # helmet: half-mask + cage
    m.box(H, P, (-4.5, 30, -4.5), (4.5, 33, 4.5), 'iron2', name='skullcap')
    m.box(H, P, (-4.5, 24, 3.5), (4.5, 30, 4.5), 'iron3', name='helm_back')
    m.box(H, P, (0, 24, -5.5), (5, 29, -4.5), 'iron', name='half_mask')
    m.box(H, P, (1, 25, -6.5), (4, 28, -5.5), 'cell', name='lens')
    for x in (-3.5, -1.5):
        m.box(H, P, (x, 24, -5), (x + 1, 30, -4), 'frame', name=f'cage_bar{x}')
    m.box(H, P, (-5.5, 25, -2.5), (-4.5, 30, 2.5), 'frame', name='cage_side_r')
    m.box(H, P, (4.5, 24, -4.5), (5.5, 30, 3.5), 'copper', name='cheek_l')
    m.box(H, P, (-4, 33, 2), (-3, 34, 3), 'chrome', name='antenna')
    m.box(H, P, (-4, 34, 2), (-3, 35, 3), 'light', name='antenna_tip')
    # torso: salvaged chest plate on the right, open ribs on the left
    m.box(B, P, (-4.5, 16, -4), (2.5, 24, -3), 'paint1', name='chest_plate')
    m.box(B, P, (-4, 17, -5), (2, 23, -4), {'default': 'iron3', 'north': 'decal'}, name='chest_decal')
    for y in (16, 18.5, 21):
        m.box(B, P, (2.5, y, -3.5), (4.5, y + 1, -2.5), 'iron3', name=f'rib{y}', opts={'rivets': False})
    m.box(B, P, (-4, 13, -3.5), (1, 15, -2.5), 'copper', name='belly_plate')
    m.box(B, P, (1.5, 12.5, -4), (3.5, 15.5, -2), 'brass', name='belly_cyl', pivot=(2.5, 14, -3), rot=(0, 0, -18))
    m.box(B, P, (-4.5, 18, 2), (-1.5, 24, 3), "iron2", name="shoulder_blade_r")
    # right arm: stepped rusty pauldron + copper cuff
    m.box(RA, P, (-10, 21, -3.5), (-4, 25, 3.5), 'iron2', name='pauldron_r', opts={'rust': 0.45})
    m.box(RA, P, (-9, 25, -2.5), (-5, 27, 2.5), 'iron2', name='pauldron_step_r', opts={'rust': 0.45})
    m.box(RA, P, (-10, 12, -3), (-4, 15, 3), 'copper', name='cuff_r')
    m.box(RA, P, (-10.5, 15, -1), (-9.5, 21, 1), 'rubber', name='hose_r')
    # left arm: tall plate pauldron carrying the decal, strapped forearm plate
    m.box(LA, P, (4, 20, -4), (9, 28, 4), 'paint2', name='pauldron_l')
    m.box(LA, P, (9, 20.5, -3.5), (10, 27.5, 3.5), {'default': 'iron3', 'east': 'decal'}, name='shoulder_decal')
    m.box(LA, P, (9, 12, -2.5), (10, 18, 2.5), 'iron', name='forearm_plate_l')
    # legs: mismatched
    m.box(RL, P, (-4.5, 8, -4), (-0.5, 12, -3), 'iron3', name='thigh_plate_r')
    m.box(LL, P, (4, 6, -2.5), (5, 11, 2.5), 'copper', name='thigh_plate_l')
    m.box(RL, P, (-5, 1, -5), (0, 4, 3), 'iron', name='boot_r')
    m.box(LL, P, (0, 1, -5), (5, 3, 3), 'iron2', name='boot_l')
    m.box(LL, P, (0.5, 3, -4), (4.5, 5, -2), 'copper', name='shin_l')
    return m


def build_C():
    """Boiler Back: hunched over a riveted copper boiler, huge forearms, small domed head."""
    m = Model('C')
    frame(m, 'C')
    P = 'piece'
    # helmet: small dome with a porthole and a rebreather
    m.box(H, P, (-4.5, 24, -4.5), (4.5, 31, 4.5), 'iron', name='helm')
    m.box(H, P, (-3.5, 31, -3.5), (3.5, 33, 3.5), 'iron2', name='helm_dome')
    m.box(H, P, (-2.5, 26, -5.5), (2.5, 30, -4.5), {'default': 'copper', 'north': 'porthole'}, name='porthole')
    m.box(H, P, (-1.5, 24, -5.5), (1.5, 26, -4.5), 'grill', name='rebreather')
    # torso
    m.box(B, P, (-4.5, 13, -3), (4.5, 24, 3), 'iron3', name='core')
    m.box(B, P, (-4.5, 16, -4), (4.5, 24, -3), 'paint1', name='chest_plate')
    m.box(B, P, (-3.5, 16.5, -5), (3.5, 23.5, -4), {'default': 'iron3', 'north': 'decal'}, name='chest_decal')
    m.box(B, P, (-4, 12, -4), (4, 15, -3), 'grill', name='belly')
    # the boiler
    m.box(B, P, (-6, 17, 3), (6, 33, 8), 'copper', name='boiler', opts={'patina': 0.22})
    for y in (19, 24, 30):
        m.box(B, P, (-6.5, y, 2.5), (6.5, y + 1, 8.5), 'iron3', name=f'band{y}', opts={'rivets': False, 'rust': 0.4})
    m.box(B, P, (-5, 33, 4), (5, 34, 7), 'iron2', name='boiler_cap')
    m.box(B, P, (-4.5, 34, 5), (-2.5, 35, 7), 'frame', name='stack')
    m.box(B, P, (1, 34, 5), (3, 35, 7), 'brass', name='valve')
    m.box(B, P, (-3, 18, 8), (3, 22, 9), 'grill', name='firebox', opts={'hot': True})
    m.box(B, P, (6, 25, 4), (7, 28, 7), {'default': 'brass', 'east': 'gauge'}, name='gauge')
    m.sym(B, P, (-7, 26, 4), (-6, 28, 6), 'pipe', name='boiler_outlet')
    # arms: sloped pauldrons, oversized forearms, pipes from the boiler
    m.sym(RA, P, (-10, 22, -4), (-4, 25, 4), 'paint1', name='pauldron', pivot=(-4, 25, 0), rot=(0, 0, 15))
    m.box(RA, P, (-11, 18, -3.5), (-10, 25, 3.5), 'iron2', name='shoulder_flap_r')
    m.box(LA, P, (10, 18, -3.5), (11, 25, 3.5), {'default': 'iron3', 'east': 'decal'}, name='shoulder_decal')
    m.sym(RA, P, (-10, 26, 4), (-6, 28, 6), 'pipe', name='arm_pipe_h')
    m.sym(RA, P, (-10, 16, 4), (-8, 26, 6), 'pipe', name='arm_pipe_v')
    m.sym(RA, P, (-8.5, 18, -2.5), (-3.5, 20, 2.5), 'rubber', name='bicep_hose')
    m.sym(RA, P, (-11, 11, -4), (-4, 18, 4), 'paint2', name='forearm', opts={'seam': True})
    m.sym(RA, P, (-10, 11, -5), (-5, 14, -4), 'copper', name='knuckles')
    m.sym(RA, P, (-12, 13, -2), (-11, 17, 2), 'grill', name='forearm_vent', opts={'hot': True})
    # legs: short and heavy
    m.sym(RL, P, (-4.5, 6, -3), (-0.5, 11, 3), 'iron', name='thigh')
    m.sym(RL, P, (-4.5, 5, -4.5), (-0.5, 8, -3.5), 'copper', name='knee')
    m.sym(RL, P, (-5, 1, -5), (0, 5, 3), 'iron2', name='boot')
    m.sym(RL, P, (-5, 5, -3.5), (0, 6, 2.5), 'rubber', name='boot_ring')
    return m


DESIGNS = {'A': build_A, 'B': build_B, 'C': build_C}
