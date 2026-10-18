"""Generates the Elven Enclave building blocks' textures and the one-off blocks' resources (Building Blocks epic).

    python3 tools/elven_block_textures.py

Textures (16x16, textures/block/ unless noted):
- Heartwood: heartwood_log (silver-white bark, soft grey-green ridges, gold flecks) and heartwood_log_top
  (swirled rings), heartwood_wood(_top) (bark all round), stripped_heartwood_log(_top), heartwood_planks
  (pale cream, gold-green grain flowing in curves), heartwood_door_top/_bottom (+ textures/item/heartwood_door),
  heartwood_trapdoor.
- Moonstone: moonstone (pale white with a blue-lilac sheen and a few bright pixels), polished_moonstone,
  moonstone_bricks (rounded, smooth bricks).
- living_wood: pale bark with leaves, tendrils and tiny flowers growing out of it.
- silverleaf: silver-green leaves with lighter edges and a few glints (not biome-tinted).
- woven_rope_plank (top: slats across, rope along both edges, gaps between slats), woven_rope_plank_side.
- sylvan_glyph_stone_bottom/_top (mossy grey standing stone, a pale green leaf glyph over both halves),
  sylvan_glyph_stone_cap.
- glimmercap (cross: pale blue glowing mushrooms), moonpetal (closed white bud), moonpetal_open (silver petals).
- starglass (faint blue, white star specks, thin silver frame), starglass_pane_top.

The families' blockstates/models/loot/recipes come from tools/block_families.py. This script writes the rest:
the one-off blocks' blockstates, models, loot tables, recipes, lang names and tags, the extra Heartwood and
Moonstone recipes (planks from wood, stonecutting moonstone into its polished and brick forms) and the
Heartwood tags (logs, planks). Safe to rerun.
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402
import block_families as bf  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
TEX = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/block')
ITEM_TEX = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/item')
N = 16
CLEAR = (0, 0, 0, 0)


def c(rgb, d=0, a=255):
    return tuple(max(0, min(255, v + d)) for v in rgb[:3]) + (a,)


def grid(fill=CLEAR):
    return [[fill] * N for _ in range(N)]


def save(name, rows, folder=TEX):
    os.makedirs(folder, exist_ok=True)
    pngio.write_png(os.path.join(folder, name + '.png'), N, N, rows)


# --- Heartwood ---------------------------------------------------------------------------------------------

BARK = (214, 216, 206)
RIDGE = (150, 164, 144)
GOLD = (214, 182, 92)
CREAM = (232, 222, 190)
GRAIN = (188, 182, 112)


def heartwood_bark():
    rng = random.Random(7)
    rows = grid()
    # Ridges: soft vertical grey-green furrows that wander a pixel left and right.
    ridges = [1, 5, 9, 13]
    for y in range(N):
        for x in range(N):
            rows[y][x] = c(BARK, rng.randint(-6, 6))
        for i, rx in enumerate(ridges):
            wob = int(round(math.sin((y + i * 5) * 0.55) * 0.9))
            x = (rx + wob) % N
            # Ridge body, a lighter edge to its left (lit) and a shade to its right.
            rows[y][x] = c(RIDGE, rng.randint(-5, 5))
            rows[y][(x + 1) % N] = c(RIDGE, 22 + rng.randint(-4, 4))
            rows[y][(x - 1) % N] = c(BARK, 14)
    # Faint gold flecks.
    for (x, y) in [(3, 2), (11, 6), (7, 11), (15, 14), (2, 9)]:
        rows[y][x] = c(GOLD, rng.randint(-8, 8))
    return rows


def rings(border, light, dark, seed, border_shade=0):
    """A log end: swirled rings inside a 1-px border of bark (or stripped wood)."""
    rng = random.Random(seed)
    rows = grid()
    for y in range(N):
        for x in range(N):
            if x in (0, 15) or y in (0, 15):
                rows[y][x] = c(border, rng.randint(-6, 6) + border_shade)
                continue
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            theta = math.atan2(dy, dx)
            # The swirl: the ring index twists with the angle, so the rings spiral rather than sit round.
            k = (r + 1.1 * math.sin(theta + r * 0.45)) / 1.7
            band = int(math.floor(k)) % 2
            base = light if band == 0 else dark
            rows[y][x] = c(base, rng.randint(-4, 4) + (8 if r < 1.2 else 0))
    return rows


def stripped_side():
    rng = random.Random(11)
    rows = grid()
    for y in range(N):
        for x in range(N):
            rows[y][x] = c(CREAM, rng.randint(-5, 5))
    # Grain flowing in long curves down the log.
    for i, gx in enumerate((2, 7, 12)):
        for y in range(N):
            x = int(round(gx + 1.6 * math.sin(y * 0.5 + i * 2.1)))
            rows[y][x % N] = c(GRAIN, rng.randint(-6, 6))
    return rows


def planks():
    rng = random.Random(23)
    rows = grid()
    for y in range(N):
        board = y // 4
        for x in range(N):
            rows[y][x] = c(CREAM, rng.randint(-4, 4) + (2, -3, 4, -1)[board])
        if y % 4 == 3:
            for x in range(N):
                rows[y][x] = c(CREAM, -34 + rng.randint(-3, 3))
    # Board ends (one joint per board, staggered like vanilla).
    for board, jx in enumerate((11, 4, 14, 7)):
        for y in range(board * 4, board * 4 + 3):
            rows[y][jx] = c(CREAM, -30)
    # A gold-green grain that flows in a gentle curve along each board.
    for board in range(4):
        phase = board * 1.7
        for x in range(N):
            y = board * 4 + 1 + int(round(0.9 * math.sin(x * 0.55 + phase)))
            if rows[y][x][0] > 200:
                rows[y][x] = c(GRAIN, rng.randint(-5, 5))
    return rows


def door(top):
    rows = planks()
    frame = c(CREAM, -40)
    for y in range(N):
        for x in range(N):
            if x in (0, 15) or (top and y == 0) or (not top and y == 15):
                rows[y][x] = c(frame, (-6 if x == 15 or y == 15 else 0))
    if top:
        # A leaf-shaped window, its midrib a thin bar of wood.
        for y in range(1, 15):
            t = (y - 1) / 13.0
            half = 5.2 * math.sin(math.pi * t) ** 0.8
            for x in range(N):
                if abs(x - 7.5) < half - 0.4:
                    rows[y][x] = CLEAR
        for y in range(3, 14):
            rows[y][7] = c(frame, 12)
            rows[y][8] = c(frame, -4)
    else:
        # Raised panel with a curling gold inlay.
        for y in range(3, 13):
            rows[y][3] = c(CREAM, 14)
            rows[y][12] = c(CREAM, -26)
        for x in range(3, 13):
            rows[3][x] = c(CREAM, 14)
            rows[12][x] = c(CREAM, -26)
        for i in range(16):
            a = i / 16.0 * 2 * math.pi
            x = int(round(7.5 + 2.6 * math.cos(a)))
            y = int(round(7.5 + 2.6 * math.sin(a) + 0.6 * math.cos(2 * a)))
            rows[y][x] = c(GOLD, -10)
    return rows


def door_item(top, bottom):
    rows = grid()
    halves = top + bottom
    for y in range(N):
        for x in range(8):
            px = halves[y * 2][x * 2]
            rows[y][4 + x] = px
    return rows


def trapdoor():
    rows = planks()
    frame = c(CREAM, -40)
    for y in range(N):
        for x in range(N):
            if x in (0, 15) or y in (0, 15):
                rows[y][x] = frame
    # Four curved openings round a crossed middle.
    for y in range(2, 14):
        for x in range(2, 14):
            if x in (7, 8) or y in (7, 8):
                rows[y][x] = c(frame, 8)
                continue
            qx = 4.0 if x < 7 else 11.0
            qy = 4.0 if y < 7 else 11.0
            if math.hypot(x - qx, y - qy) < 2.4:
                rows[y][x] = CLEAR
    return rows


# --- Moonstone ---------------------------------------------------------------------------------------------

MOON = (226, 228, 236)
SHEEN_BLUE = (198, 210, 238)
SHEEN_LILAC = (214, 202, 236)
GLOW = (246, 250, 255)


def sheen(x, y, seed):
    """0-1: soft blotches of sheen."""
    return 0.5 + 0.5 * math.sin(x * 0.7 + seed) * math.cos(y * 0.6 - seed * 0.5)


def moonstone():
    rng = random.Random(31)
    rows = grid()
    for y in range(N):
        for x in range(N):
            s = sheen(x, y, 1.3)
            base = SHEEN_BLUE if s > 0.78 else SHEEN_LILAC if s < 0.18 else MOON
            rows[y][x] = c(base, rng.randint(-6, 5))
    for (x, y) in [(4, 3), (12, 9), (7, 13), (1, 10)]:
        rows[y][x] = c(GLOW)
    return rows


def polished_moonstone():
    rng = random.Random(37)
    rows = grid()
    for y in range(N):
        for x in range(N):
            s = sheen(x, y, 0.4)
            base = SHEEN_BLUE if s > 0.85 else MOON
            d = rng.randint(-2, 2)
            if x == 0 or y == 0:
                d += 10
            elif x == 15 or y == 15:
                d -= 22
            rows[y][x] = c(base, d)
    rows[4][11] = c(GLOW)
    rows[11][4] = c(GLOW)
    return rows


def moonstone_bricks():
    rng = random.Random(43)
    mortar = (172, 170, 192)
    rows = grid()
    for y in range(N):
        course = y // 8
        offset = 4 if course == 1 else 0
        for x in range(N):
            bx = (x + offset) % 8
            by = y % 8
            if bx == 7 or by == 7:
                rows[y][x] = c(mortar, rng.randint(-3, 3))
                continue
            # Rounded corners: the mortar takes the corner pixel of each brick.
            if (bx in (0, 6)) and (by in (0, 6)):
                rows[y][x] = c(mortar, 12)
                continue
            s = sheen(x + course * 3, y, 2.0)
            base = SHEEN_BLUE if s > 0.82 else SHEEN_LILAC if s < 0.12 else MOON
            d = rng.randint(-3, 3)
            if bx == 0 or by == 0:
                d += 8
            elif bx == 6 or by == 6:
                d -= 18
            rows[y][x] = c(base, d)
    rows[2][3] = c(GLOW)
    rows[11][13] = c(GLOW)
    return rows


# --- Living wood -------------------------------------------------------------------------------------------

LW_BARK = (196, 192, 172)
LEAF = (92, 146, 64)
LEAF_LIGHT = (128, 180, 86)
TENDRIL = (104, 150, 72)


def living_wood():
    rng = random.Random(53)
    rows = grid()
    for y in range(N):
        for x in range(N):
            rows[y][x] = c(LW_BARK, rng.randint(-7, 7))
        # Irregular furrows.
        for fx in (3, 10):
            x = int(round(fx + 1.2 * math.sin(y * 0.7 + fx)))
            rows[y][x % N] = c(LW_BARK, -34 + rng.randint(-4, 4))
    # Tendrils curling across the bark.
    for (sx, sy, length, phase) in [(0, 12, 9, 0.0), (8, 1, 8, 1.7)]:
        for i in range(length):
            x = (sx + i) % N
            y = int(round(sy + 1.4 * math.sin(i * 0.8 + phase))) % N
            rows[y][x] = c(TENDRIL, rng.randint(-6, 6))
    # Small leaves (2-3 px) sprouting from the tendrils and bark.
    for (x, y) in [(2, 10), (5, 13), (9, 3), (13, 0), (12, 7), (6, 6)]:
        rows[y][x] = c(LEAF)
        rows[y][(x + 1) % N] = c(LEAF_LIGHT)
        rows[(y + 1) % N][x] = c(LEAF, -14)
    # Tiny flowers.
    for (x, y, col) in [(4, 2, (244, 244, 236)), (14, 11, (236, 188, 224)), (8, 9, (244, 244, 236))]:
        rows[y][x] = c(col)
    rows[2][4] = c((240, 214, 110))
    return rows


# --- Silverleaf --------------------------------------------------------------------------------------------

SILVER_LEAF = (138, 168, 150)


def silverleaf():
    rng = random.Random(61)
    # The see-through pixels keep a dark silver-green, which Fast graphics shows (it draws leaves opaque).
    rows = grid((46, 62, 54, 0))
    centres = [(2, 2), (8, 1), (13, 3), (5, 6), (11, 7), (1, 9), (7, 11), (14, 11), (3, 14), (10, 14)]
    for (cx, cy) in centres:
        for y in range(cy - 2, cy + 3):
            for x in range(cx - 2, cx + 2):
                dx, dy = x - cx + 0.5, y - cy
                if dx * dx / 3.2 + dy * dy / 5.0 <= 1.0:
                    edge = dx * dx / 3.2 + dy * dy / 5.0 > 0.45
                    rows[y % N][x % N] = c(SILVER_LEAF, (34 if edge else 0) + rng.randint(-8, 8))
    # Leave some gaps, as leaves have (about a fifth see-through).
    for (x, y) in [(5, 0), (15, 7), (0, 5), (10, 10), (6, 15), (12, 1)]:
        rows[y][x] = (46, 62, 54, 0)
    for (x, y) in [(8, 1), (5, 6), (14, 11), (3, 14)]:
        rows[y][x] = c((236, 246, 240))
    return rows


# --- Woven rope plank --------------------------------------------------------------------------------------

BIRCH_SLAT = (214, 200, 150)
ROPE = (176, 150, 100)


def rope(y, x):
    """Twisted rope: alternate light and dark diagonals."""
    return c(ROPE, 16 if (x + y) % 3 == 0 else -18 if (x + y) % 3 == 2 else 0)


def woven_top():
    rng = random.Random(71)
    rows = grid()
    for y in range(N):
        slat = y % 4
        for x in range(N):
            if slat == 3:
                rows[y][x] = CLEAR
                continue
            d = rng.randint(-5, 5) + (8 if slat == 0 else -10 if slat == 2 else 0)
            rows[y][x] = c(BIRCH_SLAT, d)
        # Birch-like dark knots now and then.
        if slat == 1:
            rows[y][(y * 5 + 3) % 12 + 2] = c(BIRCH_SLAT, -60)
    for y in range(N):
        for x in (1, 2, 13, 14):
            rows[y][x] = rope(y, x)
    return rows


def woven_side():
    rng = random.Random(73)
    rows = grid()
    for y in range(N):
        for x in range(N):
            # Slat ends with a gap every 4 pixels, ropes wrapped round at both ends.
            if x % 4 == 3:
                rows[y][x] = c(BIRCH_SLAT, -46)
            else:
                rows[y][x] = c(BIRCH_SLAT, rng.randint(-5, 5))
        for x in (1, 2, 13, 14):
            rows[y][x] = rope(y, x)
    return rows


# --- Sylvan glyph stone ------------------------------------------------------------------------------------

STONE = (124, 126, 120)
MOSS = (88, 112, 58)
GLYPH = (176, 244, 170)
GLYPH_DIM = (128, 210, 130)


def glyph_stone(top):
    rng = random.Random(81 if top else 83)
    rows = grid()
    for y in range(N):
        for x in range(N):
            gy = y + (0 if top else 16)
            d = rng.randint(-8, 8) + int(6 * math.sin(x * 0.9 + gy * 0.4))
            rows[y][x] = c(STONE, d)
            # Moss creeping up from the foot and down from the top.
            moss = (gy > 26 - 2 * math.sin(x * 0.8)) or (gy < 3 + math.sin(x * 1.3))
            if moss and rng.random() < 0.75:
                rows[y][x] = c(MOSS, rng.randint(-10, 10))
    # Crevices.
    for (x, y) in ([(3, 4), (3, 5), (4, 6), (12, 9), (12, 10)] if top else [(11, 3), (11, 4), (10, 5), (4, 8)]):
        rows[y][x] = c(STONE, -40)
    # The leaf glyph: a leaf in the top half, its stem running down into the bottom half.
    if top:
        for y in range(3, 15):
            t = (y - 3) / 11.0
            half = 3.2 * math.sin(math.pi * t)
            for x in range(N):
                if abs(abs(x - 7.5) - half) < 0.55 and half > 0.6:
                    rows[y][x] = c(GLYPH)
            if y >= 6:
                rows[y][7] = c(GLYPH_DIM)
                rows[y][8] = c(GLYPH_DIM)
        rows[15][7] = c(GLYPH)
        rows[15][8] = c(GLYPH)
    else:
        for y in range(0, 9):
            rows[y][7] = c(GLYPH, -10 * (y // 3))
            rows[y][8] = c(GLYPH, -10 * (y // 3))
        # Root-like curls at the foot of the stem.
        for (x, y) in [(6, 8), (5, 9), (9, 8), (10, 9)]:
            rows[y][x] = c(GLYPH_DIM)
    return rows


def glyph_cap():
    rng = random.Random(87)
    rows = grid()
    for y in range(N):
        for x in range(N):
            rows[y][x] = c(MOSS if rng.random() < 0.55 else STONE, rng.randint(-10, 10))
    return rows


# --- Glimmercap and moonpetal ------------------------------------------------------------------------------

CAP = (128, 196, 238)
CAP_BRIGHT = (214, 246, 255)
STALK = (222, 226, 232)


def glimmercap():
    rows = grid()
    # (stem x, cap top y, stem height, cap half-width)
    for (sx, cy, h, w) in [(4, 7, 6, 2), (10, 3, 9, 3), (13, 9, 4, 1)]:
        for y in range(cy + 2, min(16, cy + 2 + h)):
            rows[y][sx] = c(STALK, -12 if y > 13 else 0)
        for dx in range(-w, w + 1):
            rows[cy + 1][sx + dx] = c(CAP, -24)
            if abs(dx) < w:
                rows[cy][sx + dx] = c(CAP)
        rows[cy][sx] = c(CAP_BRIGHT)
        if w > 1:
            rows[cy + 1][sx - w + 1] = c(CAP_BRIGHT)
    return rows


STEM = (70, 130, 60)


def moonpetal(open_):
    rows = grid()
    for y in range(7, 16):
        rows[y][8] = c(STEM, -10 if y > 12 else 0)
    # Two leaves on the stem.
    for (x, y) in [(6, 12), (7, 11), (9, 13), (10, 12), (11, 12)]:
        rows[y][x] = c(STEM, 18)
    if open_:
        petal = (234, 238, 248)
        shade = (188, 198, 218)
        for y in range(1, 9):
            for x in range(3, 14):
                dx, dy = x - 8, y - 4.5
                r = math.hypot(dx, dy * 1.2)
                if r <= 4.6:
                    a = math.atan2(dy, dx)
                    # Five petals: dips between them.
                    lobe = math.cos(5 * a)
                    if r > 3.4 and lobe < -0.3:
                        continue
                    rows[y][x] = c(shade if r > 3.4 and lobe < 0.4 else petal)
        for (x, y) in [(8, 4), (7, 4), (8, 5), (7, 5)]:
            rows[y][x] = c((240, 228, 164))
    else:
        bud = (238, 238, 228)
        for y in range(2, 8):
            half = [0, 1, 1, 1, 1, 0][y - 2]
            for x in range(8 - half, 9 + half):
                rows[y][x] = c(bud, -16 if x > 8 else 0)
        # Green sepals cupping the bud.
        for (x, y) in [(6, 7), (7, 8), (9, 8), (10, 7), (8, 8)]:
            rows[y][x] = c(STEM, 8)
    return rows


# --- Starglass ---------------------------------------------------------------------------------------------

FRAME = (200, 206, 218)


def starglass():
    rows = grid()
    for y in range(N):
        for x in range(N):
            if x in (0, 15) or y in (0, 15):
                rows[y][x] = c(FRAME, -24 if x == 15 or y == 15 else 0)
            else:
                rows[y][x] = (182, 212, 255, 46)
    # Small stars: a bright middle with dimmer arms.
    for (x, y, big) in [(4, 4, True), (11, 3, False), (10, 10, True), (3, 11, False), (7, 7, False), (13, 13, False)]:
        rows[y][x] = (255, 255, 255, 230)
        if big:
            for (dx, dy) in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
                rows[y + dy][x + dx] = (236, 244, 255, 140)
    return rows


def pane_top():
    rows = grid()
    for y in range(N):
        for x in range(N):
            rows[y][x] = c(FRAME, -10 if x in (0, 15) else 0)
    return rows


def textures():
    bark = heartwood_bark()
    save('heartwood_log', bark)
    save('heartwood_wood', bark)
    save('heartwood_wood_top', bark)
    save('heartwood_log_top', rings(BARK, (232, 222, 190), (202, 194, 132), 13))
    save('stripped_heartwood_log', stripped_side())
    save('stripped_heartwood_log_top', rings(CREAM, (238, 230, 200), (206, 198, 138), 17, -14))
    save('heartwood_planks', planks())
    top, bottom = door(True), door(False)
    save('heartwood_door_top', top)
    save('heartwood_door_bottom', bottom)
    save('heartwood_door', door_item(top, bottom), ITEM_TEX)
    save('heartwood_trapdoor', trapdoor())
    save('moonstone', moonstone())
    save('polished_moonstone', polished_moonstone())
    save('moonstone_bricks', moonstone_bricks())
    save('living_wood', living_wood())
    save('silverleaf', silverleaf())
    save('woven_rope_plank', woven_top())
    save('woven_rope_plank_side', woven_side())
    save('sylvan_glyph_stone_top', glyph_stone(True))
    save('sylvan_glyph_stone_bottom', glyph_stone(False))
    save('sylvan_glyph_stone_cap', glyph_cap())
    save('glimmercap', glimmercap())
    save('moonpetal', moonpetal(False))
    save('moonpetal_open', moonpetal(True))
    save('starglass', starglass())
    save('starglass_pane_top', pane_top())


# --- One-off blocks' resources -----------------------------------------------------------------------------

def ref(path):
    return bf.ref(path)


def model(name, obj):
    bf.write(os.path.join(bf.ASSETS, 'models/block', name + '.json'), obj)


def item(name, obj):
    bf.write(os.path.join(bf.ASSETS, 'models/item', name + '.json'), obj)


def generated_item(name, texture):
    item(name, {'parent': 'minecraft:item/generated', 'textures': {'layer0': ref('block/' + texture)}})


def tool_conditions():
    """Loot conditions: mined with silk touch, mined with shears."""
    match = {'condition': 'minecraft:match_tool', 'predicate': {
        'enchantments': [{'enchantment': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}
    shears = {'condition': 'minecraft:match_tool', 'predicate': {'items': ['minecraft:shears']}}
    return match, shears


def resources():
    # Silverleaf: leaves (the leaves parent carries a tint index, but nothing tints this block).
    model('silverleaf', {'parent': 'minecraft:block/leaves', 'textures': {'all': ref('block/silverleaf')}})
    bf.blockstate('silverleaf', {'variants': {'': bf.variant('silverleaf')}})
    item('silverleaf', {'parent': ref('block/silverleaf')})

    # Woven rope plank: a 3-px deck in the bottom (y 5-8) or top (13-16) half; the unrotated model runs north-south.
    for half, (y1, y2) in (('bottom', (5, 8)), ('top', (13, 16))):
        faces = {d: {'texture': '#side'} for d in ('north', 'south', 'east', 'west')}
        faces['up'] = {'texture': '#top', **({'cullface': 'up'} if half == 'top' else {})}
        faces['down'] = {'texture': '#top'}
        model('woven_rope_plank_' + half, {
            'parent': 'minecraft:block/block',
            'textures': {'particle': ref('block/woven_rope_plank'), 'top': ref('block/woven_rope_plank'),
                         'side': ref('block/woven_rope_plank_side')},
            'elements': [{'from': [0, y1, 0], 'to': [16, y2, 16], 'faces': faces}]})
    variants = {}
    for half in ('bottom', 'top'):
        for axis, y in (('z', 0), ('x', 90)):
            variants[f'axis={axis},half={half}'] = bf.variant('woven_rope_plank_' + half, y=y)
    bf.blockstate('woven_rope_plank', {'variants': variants})
    item('woven_rope_plank', {'parent': ref('block/woven_rope_plank_bottom')})

    # Sylvan glyph stone: a 12x12 standing stone, two blocks high.
    for half in ('lower', 'upper'):
        side = ref('block/sylvan_glyph_stone_' + ('top' if half == 'upper' else 'bottom'))
        cap = ref('block/sylvan_glyph_stone_cap')
        faces = {d: {'uv': [2, 0, 14, 16], 'texture': '#side'} for d in ('north', 'south', 'east', 'west')}
        faces['up'] = {'uv': [2, 2, 14, 14], 'texture': '#cap'}
        faces['down'] = {'uv': [2, 2, 14, 14], 'texture': '#cap', **({'cullface': 'down'} if half == 'lower' else {})}
        model('sylvan_glyph_stone_' + half, {
            'parent': 'minecraft:block/block', 'textures': {'particle': side, 'side': side, 'cap': cap},
            'elements': [{'from': [2, 0, 2], 'to': [14, 16, 14], 'faces': faces}]})
    bf.blockstate('sylvan_glyph_stone', {'variants': {
        'half=lower': bf.variant('sylvan_glyph_stone_lower'), 'half=upper': bf.variant('sylvan_glyph_stone_upper')}})
    item('sylvan_glyph_stone', {'parent': 'minecraft:item/generated', 'textures': {
        'layer0': ref('item/sylvan_glyph_stone')}})

    # Glimmercap and moonpetal: cross models.
    model('glimmercap', {'parent': 'minecraft:block/cross', 'textures': {'cross': ref('block/glimmercap')}})
    bf.blockstate('glimmercap', {'variants': {'': bf.variant('glimmercap')}})
    generated_item('glimmercap', 'glimmercap')
    for name in ('moonpetal', 'moonpetal_open'):
        model(name, {'parent': 'minecraft:block/cross', 'textures': {'cross': ref('block/' + name)}})
    bf.blockstate('moonpetal', {'variants': {'open=false': bf.variant('moonpetal'),
                                             'open=true': bf.variant('moonpetal_open')}})
    generated_item('moonpetal', 'moonpetal_open')

    # Starglass and its pane (vanilla's glass pane models).
    model('starglass', {'parent': 'minecraft:block/cube_all', 'textures': {'all': ref('block/starglass')}})
    bf.blockstate('starglass', {'variants': {'': bf.variant('starglass')}})
    item('starglass', {'parent': ref('block/starglass')})
    pane = ref('block/starglass')
    edge = ref('block/starglass_pane_top')
    for part in ('post', 'side', 'side_alt'):
        model('starglass_pane_' + part, {'parent': 'minecraft:block/template_glass_pane_' + part,
                                         'textures': {'edge': edge, 'pane': pane}})
    for part in ('noside', 'noside_alt'):
        model('starglass_pane_' + part, {'parent': 'minecraft:block/template_glass_pane_' + part,
                                         'textures': {'pane': pane}})
    parts = [{'apply': bf.variant('starglass_pane_post')}]
    for side, mdl, y in (('north', 'side', 0), ('east', 'side', 90), ('south', 'side_alt', 0), ('west', 'side_alt', 90)):
        parts.append({'apply': bf.variant('starglass_pane_' + mdl, y=y), 'when': {side: 'true'}})
    for side, mdl, y in (('north', 'noside', 0), ('east', 'noside_alt', 0), ('south', 'noside_alt', 90),
                         ('west', 'noside', 270)):
        parts.append({'apply': bf.variant('starglass_pane_' + mdl, y=y), 'when': {side: 'false'}})
    bf.blockstate('starglass_pane', {'multipart': parts})
    generated_item('starglass_pane', 'starglass')

    # Loot: most drop themselves; the glyph stone from its lower half; glass needs silk touch, leaves silk
    # touch or shears.
    bf.loot('woven_rope_plank')
    bf.loot('glimmercap')
    bf.loot('moonpetal')
    bf.loot('sylvan_glyph_stone', {'conditions': [{'block': ref('sylvan_glyph_stone'),
                                                   'condition': 'minecraft:block_state_property',
                                                   'properties': {'half': 'lower'}}]})
    silk, shears = tool_conditions()
    for name in ('starglass', 'starglass_pane'):
        bf.loot(name, {'conditions': [silk]}, pool_conditions=False)
    bf.loot('silverleaf', {'conditions': [{'condition': 'minecraft:alternative', 'terms': [shears, silk]}]},
            pool_conditions=False)

    # Recipes.
    bf.shaped('woven_rope_plank', ['S S', '###'], {'S': 'minecraft:string', '#': 'minecraft:birch_slab'},
              ref('woven_rope_plank'), 6)
    bf.shaped('sylvan_glyph_stone', ['L', 'M', 'M'], {'L': 'minecraft:glow_lichen',
                                                      'M': 'minecraft:mossy_cobblestone'},
              ref('sylvan_glyph_stone'), 1, 'decorations')
    bf.shaped('starglass', ['###', '#G#', '###'], {'#': 'minecraft:glass', 'G': 'minecraft:glowstone_dust'},
              ref('starglass'), 8)
    bf.shaped('starglass_pane', ['###', '###'], {'#': ref('starglass')}, ref('starglass_pane'), 16, 'decorations')
    for source in ('heartwood_wood', 'stripped_heartwood_log'):
        bf.shaped(f'heartwood_planks_from_{source}', ['#'], {'#': ref(source)}, ref('heartwood_planks'), 4,
                  group='planks')
    for result, count in (('polished_moonstone', 1), ('moonstone_bricks', 1), ('moonstone_brick_stairs', 1),
                          ('moonstone_brick_slab', 2), ('moonstone_brick_wall', 1)):
        bf.stonecut(f'{result}_from_moonstone_stonecutting', ref('moonstone'), ref(result), count)
    bf.stonecut('moonstone_bricks_from_polished_moonstone_stonecutting', ref('polished_moonstone'),
                ref('moonstone_bricks'), 1)

    # Tags.
    logs = [ref(n) for n in ('heartwood_log', 'heartwood_wood', 'stripped_heartwood_log', 'living_wood')]
    for kind in ('blocks', 'items'):
        bf.add_to_tag(kind, 'logs', logs)
        bf.add_to_tag(kind, 'logs_that_burn', logs)
        bf.add_to_tag(kind, 'planks', [ref('heartwood_planks')])
        bf.add_to_tag(kind, 'leaves', [ref('silverleaf')])
        bf.add_to_tag(kind, 'flowers', [ref('moonpetal')])
        bf.add_to_tag(kind, 'small_flowers', [ref('moonpetal')])
    bf.add_to_tag('blocks', 'mineable/axe', [ref('woven_rope_plank')])
    bf.add_to_tag('blocks', 'mineable/hoe', [ref('silverleaf')])
    bf.add_to_tag('blocks', 'mineable/pickaxe', [ref('sylvan_glyph_stone')])
    bf.add_to_tag('blocks', 'impermeable', [ref('starglass')])

    lang({
        'block.dndclasses.silverleaf': 'Silverleaf',
        'block.dndclasses.woven_rope_plank': 'Woven Rope Plank',
        'block.dndclasses.sylvan_glyph_stone': 'Sylvan Glyph Stone',
        'block.dndclasses.glimmercap': 'Glimmercap',
        'block.dndclasses.moonpetal': 'Moonpetal',
        'block.dndclasses.starglass': 'Starglass',
        'block.dndclasses.starglass_pane': 'Starglass Pane',
    })


def lang(entries):
    """Appends missing keys to en_us.json before its closing brace (as block_families.gen_lang does)."""
    path = os.path.join(bf.ASSETS, 'lang/en_us.json')
    with open(path) as fh:
        text = fh.read()
    current = bf.json.loads(text)
    lines = text.rstrip().split('\n')
    for key, value in entries.items():
        if key in current:
            continue
        if not lines[-2].rstrip().endswith(','):
            lines[-2] = lines[-2].rstrip() + ','
        lines.insert(len(lines) - 1, f'    {bf.json.dumps(key)}: {bf.json.dumps(value)}')
    with open(path, 'w') as fh:
        fh.write('\n'.join(lines) + '\n')
    bf.json.loads(open(path).read())


def glyph_item():
    """The glyph stone's item icon: both halves of its side, squeezed to half height, centred."""
    top, bottom = glyph_stone(True), glyph_stone(False)
    rows = grid()
    both = top + bottom
    for y in range(N):
        for x in range(2, 14):
            if 4 <= x <= 11:
                rows[y][x] = both[y * 2][2 + (x - 4) * 12 // 8]
    return rows


def main():
    textures()
    save('sylvan_glyph_stone', glyph_item(), ITEM_TEX)
    resources()
    print('wrote the Elven Enclave block textures and resources')


if __name__ == '__main__':
    main()
