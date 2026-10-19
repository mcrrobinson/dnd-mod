"""Generates the Dwarven Fortress building-block textures (Building Blocks epic).

    python3 tools/dwarven_block_textures.py

16x16 textures/block/runic_deepslate_bricks.png: large, even deepslate-grey bricks (two courses of 8x8,
the lower one offset by half a brick) with thin gold-inlaid angular runes on two of them. Stairs, slab and
wall share it (tools/block_families.py).
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/block')

N = 16
MORTAR = (38, 38, 44)
STONE = (78, 78, 84)
GOLD = (222, 178, 62)
GOLD_DARK = (190, 144, 48)

# Runes as pixel offsets inside an 8x8 brick (1-px straight strokes, Dethek-like): keyed by the brick's
# top-left corner in the texture.
RUNES = {
    # a stave with two arms raised to the right
    (0, 0): [(2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (3, 2), (4, 1), (3, 4), (4, 3)],
    # a forked stave, like a Y
    (12, 8): [(1, 1), (2, 2), (5, 1), (4, 2), (3, 3), (3, 4), (3, 5)],
}


def shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)


def runic_bricks():
    rng = random.Random(41)
    rows = [[None] * N for _ in range(N)]
    for y in range(N):
        course = y // 8
        offset = 4 if course == 1 else 0
        for x in range(N):
            bx = (x + offset) % 8
            by = y % 8
            if bx == 7 or by == 7:
                rows[y][x] = MORTAR + (255,)
                continue
            # Each brick has its own tone; bevel light on the top/left, dark on the bottom/right.
            brick = ((x + offset) % N) // 8 + course * 2
            tone = (-4, 3, 6, -2)[brick % 4]
            d = tone + rng.randint(-5, 5)
            if bx == 0 or by == 0:
                d += 12
            elif bx == 6 or by == 6:
                d -= 12
            rows[y][x] = shade(STONE, d)
    for (ox, oy), strokes in RUNES.items():
        for (dx, dy) in strokes:
            x, y = (ox + dx) % N, oy + dy
            # The inlay is bright at the top of each stroke and duller lower down.
            rows[y][x] = (GOLD if dy <= 3 else GOLD_DARK) + (255,)
    return rows


# --- the rest of the Dwarven Fortresses card -------------------------------------------------------------------

def blank(c=(0, 0, 0, 0)):
    return [[c] * N for _ in range(N)]


def px(c, d=0):
    return shade(c, d)


def line(rows, x0, y0, x1, y1, c):
    """A 1-px Bresenham line."""
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        if 0 <= x0 < N and 0 <= y0 < N:
            rows[y0][x0] = c
        if x0 == x1 and y0 == y1:
            return
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def carved_panel(rng, base, recess=-14):
    """A stone face with a raised 2-px rim (light top/left, dark bottom/right) round a sunken middle."""
    rows = blank()
    for y in range(N):
        for x in range(N):
            d = rng.randint(-4, 4)
            if x == 0 or y == 0:
                d += 14
            elif x == N - 1 or y == N - 1:
                d -= 16
            elif x == 1 or y == 1:
                d += 4
            elif x == N - 2 or y == N - 2:
                d -= 6
            elif x == 2 or y == 2:
                d += recess - 8
            else:
                d += recess
            rows[y][x] = px(base, d)
    return rows


CHISELED_RUNE = [
    # a stave with arms reaching to all four corners: one big Dethek rune
    (7, 5, 7, 10), (8, 5, 8, 10),
    (6, 6, 5, 5), (9, 6, 10, 5),
    (6, 9, 5, 10), (9, 9, 10, 10),
]


def chiseled_runic():
    rng = random.Random(7)
    rows = carved_panel(rng, STONE)
    # gold frame just inside the rim
    for i in range(3, 13):
        for (x, y) in ((i, 3), (3, i)):
            rows[y][x] = GOLD + (255,)
        for (x, y) in ((i, 12), (12, i)):
            rows[y][x] = GOLD_DARK + (255,)
    for (x0, y0, x1, y1) in CHISELED_RUNE:
        line(rows, x0, y0, x1, y1, GOLD + (255,))
    # the strokes' lower/right edges catch less light
    for y in range(5, 11):
        rows[y][8] = GOLD_DARK + (255,)
    return rows


def clan_crest():
    rng = random.Random(11)
    rows = carved_panel(rng, (72, 72, 80), recess=-10)
    # hexagonal border, pointed top and bottom
    hexagon = [(7, 1), (8, 1), (13, 4), (13, 11), (8, 14), (7, 14), (2, 11), (2, 4)]
    for i in range(len(hexagon)):
        (x0, y0), (x1, y1) = hexagon[i], hexagon[(i + 1) % len(hexagon)]
        line(rows, x0, y0, x1, y1, (GOLD if (y0 + y1 < 15 or x0 + x1 < 15) else GOLD_DARK) + (255,))
    bright = (246, 208, 96, 255)
    # hammer: a broad head on a handle, hanging over the anvil
    for x in range(5, 11):
        rows[4][x] = bright
        rows[5][x] = GOLD + (255,)
    rows[5][10] = GOLD_DARK + (255,)
    for y in range(6, 8):
        rows[y][7] = GOLD + (255,)
        rows[y][8] = GOLD_DARK + (255,)
    # anvil: a face with a horn to the left, a narrow waist, a broad foot
    for x in range(4, 12):
        rows[9][x] = bright
    rows[9][4] = GOLD_DARK + (255,)
    rows[10][3] = GOLD_DARK + (255,)
    for x in range(6, 10):
        rows[10][x] = GOLD + (255,)
    for x in range(5, 11):
        rows[11][x] = GOLD_DARK + (255,)
    return rows


STATUE = (112, 112, 116)


def ancestor_statue():
    """A stern carved dwarf face: heavy brows, deep eyes, a broad nose and a braided beard."""
    rng = random.Random(23)
    rows = blank()
    for y in range(N):
        for x in range(N):
            rows[y][x] = px(STATUE, rng.randint(-5, 5) - 6)
    def put(x, y, d):
        rows[y][x] = px(STATUE, d + rng.randint(-3, 3))
    # helm band across the top, with rivets
    for x in range(N):
        put(x, 0, 10)
        put(x, 1, 4)
        put(x, 2, -22)
    for x in (2, 7, 8, 13):
        put(x, 1, 22)
    # brows: two heavy ridges, lit on top, shadow under
    for x in list(range(2, 7)) + list(range(9, 14)):
        put(x, 3, 20)
        put(x, 4, 10)
    put(7, 4, -10)
    put(8, 4, -10)
    # eyes deep in shadow under the brow
    for x in (4, 5, 10, 11):
        put(x, 5, -55)
    for x in (3, 6, 9, 12):
        put(x, 5, -24)
    put(4, 6, -18)
    put(11, 6, -18)
    # nose: a broad ridge, lit on the left, shadowed to the right and below
    for y in range(5, 9):
        put(7, y, 14)
        put(8, y, 2)
        put(9, y, -16)
    put(6, 8, 6)
    put(9, 8, -10)
    for x in range(6, 10):
        put(x, 9, -26)
    # cheeks
    for x in (3, 4, 11, 12):
        put(x, 7, 6)
    # moustache sweeping out and down
    for x in range(3, 13):
        put(x, 10, 14)
    put(2, 11, 10)
    put(13, 11, 10)
    for x in range(6, 10):
        put(x, 11, -30)  # the mouth under it
    # beard filling the bottom, with strands, and two braids
    for y in range(11, N):
        half = 7 - max(0, y - 13)
        for x in range(8 - half, 8 + half):
            if rows[y][x][0] < 70 and y == 11 and 6 <= x <= 9:
                continue
            strand = 16 if (x + y // 2) % 2 == 0 else -10
            put(x, y, strand)
    for bx in (5, 10):
        for y in range(11, N):
            put(bx, y, 26 if y % 2 == 0 else -26)
            put(bx + 1, y, -26 if y % 2 == 0 else 20)
    return rows


def ancestor_statue_side():
    rng = random.Random(29)
    rows = blank()
    for y in range(N):
        for x in range(N):
            d = rng.randint(-5, 5) - 6
            # chisel marks: short diagonal strokes
            if (x + 2 * y) % 7 == 0 and rng.random() < 0.6:
                d -= 10
            if y == 0:
                d += 8
            elif y == N - 1:
                d -= 10
            rows[y][x] = px(STATUE, d)
    return rows


FORGE_BRICK = (56, 40, 34)


def forge_bricks():
    """Heat-blackened brick courses with ember mortar, glowing brightest at the bottom."""
    rng = random.Random(53)
    rows = blank()
    for y in range(N):
        course = y // 4
        offset = 4 if course % 2 else 0
        heat = y / (N - 1)  # 0 at the top, 1 at the bottom
        for x in range(N):
            bx = (x + offset) % 8
            by = y % 4
            if by == 3 or bx == 7:
                r = int(150 + 105 * heat) + rng.randint(-10, 10)
                g = int(60 + 90 * heat) + rng.randint(-10, 10)
                b = int(20 + 30 * heat)
                if rng.random() < 0.2:
                    r, g = r - 40, g - 30  # a cooler fleck in the mortar
                rows[y][x] = (max(0, min(255, r)), max(0, min(255, g)), max(0, min(255, b)), 255)
                continue
            brick = ((x + offset) % N) // 8 + course * 2
            d = (-6, 4, 0, -10, 6, -2, 2, -8)[brick % 8] + rng.randint(-5, 5)
            if by == 0:
                d -= 12  # soot along each brick's top
            if by == 2:
                # the brick's lower edge picks up the glow below it
                c = px(FORGE_BRICK, d + 6)
                rows[y][x] = (min(255, c[0] + int(30 * heat)), min(255, c[1] + int(8 * heat)), c[2], 255)
                continue
            rows[y][x] = px(FORGE_BRICK, d)
    return rows


PILLAR = (58, 58, 66)


def gold_veined_pillar():
    """A fluted column of dark polished stone, with gold run into a crack from top to bottom."""
    rng = random.Random(61)
    rows = blank()
    for y in range(N):
        for x in range(N):
            f = x % 4
            d = (10, 4, -2, -14)[f] + rng.randint(-3, 3)
            if x == 0:
                d += 6
            if x == N - 1:
                d -= 6
            rows[y][x] = px(PILLAR, d)
    # The crack wanders down and comes back to its start so the column tiles; a short branch splits off.
    path = [5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 8, 8, 7, 6, 6, 5]
    for y, x in enumerate(path):
        rows[y][x] = (GOLD if y % 3 else (246, 208, 96)) + (255,)
        nxt = path[(y + 1) % N]
        if nxt != x:
            rows[y][nxt] = GOLD_DARK + (255,)
    for (x, y) in ((10, 9), (11, 10), (12, 10), (13, 11)):
        rows[y][x] = GOLD_DARK + (255,)
    rows[9][9] = GOLD + (255,)
    return rows


def gold_veined_pillar_top():
    rng = random.Random(67)
    rows = blank()
    for y in range(N):
        for x in range(N):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if r < 1.5:
                rows[y][x] = (246, 208, 96, 255)
                continue
            if 5.2 < r < 6.2:
                rows[y][x] = (GOLD if x + y < 15 else GOLD_DARK) + (255,)
                continue
            ring = int(r)
            d = (-14, -14, 10, -4, 6, -8, 0, 14, -16, -16, -16)[min(ring, 10)] + rng.randint(-3, 3)
            rows[y][x] = px(PILLAR, d)
    return rows


DARK_WOOD = (60, 40, 24)
OAK_END = (164, 122, 72)
HOOP = (86, 86, 92)
BRASS = (196, 152, 64)


def ale_keg_wall():
    """Three keg ends stacked, one on two, in dark wood framing: oak ends, iron hoops, brass spigots."""
    rng = random.Random(71)
    rows = blank()
    for y in range(N):
        for x in range(N):
            d = rng.randint(-4, 4) + (-8 if x % 4 == 0 else 0)
            rows[y][x] = px(DARK_WOOD, d)
    for (cx, cy) in ((7.5, 3.5), (3.5, 11.5), (11.5, 11.5)):
        for y in range(N):
            for x in range(N):
                r = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                if r <= 2.9:
                    # the end's boards, split by dark seams
                    d = (-26 if (x - int(cx)) % 3 == 2 else 0) + rng.randint(-4, 4)
                    if x < cx and y < cy:
                        d += 8
                    rows[y][x] = px(OAK_END, d)
                elif r <= 3.9:
                    d = 14 if (y < cy and x <= cx) else -8
                    rows[y][x] = px(HOOP, d)
        # the spigot, just below the heart
        sx, sy = int(cx), int(cy) + 1
        rows[sy][sx] = BRASS + (255,)
        rows[sy + 1][sx] = px(BRASS, -50)
    return rows


def ale_keg_wall_side():
    """Kegs seen from the side: long staves with iron hoops round them."""
    rng = random.Random(73)
    rows = blank()
    for y in range(N):
        for x in range(N):
            d = rng.randint(-4, 4) + (-14 if y % 5 == 4 else 0)
            if x in (2, 13):
                rows[y][x] = px(HOOP, 6 if y % 5 != 4 else -10)
                continue
            rows[y][x] = px((128, 92, 54) if y % 5 != 4 else DARK_WOOD, d)
    return rows


COIN = (232, 184, 56)
SILVER = (206, 208, 216)


def coin_pile_top():
    rng = random.Random(83)
    rows = blank()
    for y in range(N):
        for x in range(N):
            rows[y][x] = px((176, 124, 32), rng.randint(-10, 6))
    # coins: 2x2 discs, lit top-left, dark bottom-right; a few are silver
    spots = [(x, y) for y in range(0, N, 3) for x in range((y // 3) % 3, N, 3)]
    rng.shuffle(spots)
    for (x, y) in spots[:30]:
        x += rng.randint(0, 1)
        metal = SILVER if rng.random() < 0.18 else COIN
        for (dx, dy, d) in ((0, 0, 22), (1, 0, 4), (0, 1, 4), (1, 1, -18)):
            rows[(y + dy) % N][(x + dx) % N] = px(metal, d + rng.randint(-3, 3))
    for (x, y, gem) in ((4, 6, (200, 30, 44)), (12, 2, (40, 84, 220)), (9, 13, (200, 30, 44)), (1, 12, (40, 84, 220))):
        rows[y][x] = gem + (255,)
        rows[y][(x + 1) % N] = px(gem, -50)
        rows[(y + 1) % N][x] = px(gem, -40)
        rows[y - 1][x] = px(gem, 60)
    return rows


def coin_pile_side():
    """A cross-section of the heap: courses of coin edges, some silver, the odd gem."""
    rng = random.Random(89)
    rows = blank()
    for y in range(N):
        offset = rng.randint(0, 3)
        for x in range(N):
            edge = (x + offset) % 4 == 3
            base = (150, 102, 24) if y % 2 else (210, 160, 44)
            rows[y][x] = px(base, (-34 if edge else 0) + rng.randint(-6, 6))
        if rng.random() < 0.5:
            x0 = rng.randint(0, N - 3)
            for x in range(x0, x0 + 3):
                rows[y][x] = px(SILVER, (-20 if y % 2 else 4) + rng.randint(-4, 4))
    for (x, y, gem) in ((5, 6, (200, 30, 44)), (11, 12, (40, 84, 220))):
        rows[y][x] = gem + (255,)
    return rows


MITHRAL = (222, 230, 240)
SHEEN = (176, 204, 244)


def mithral_lattice():
    """Pale silver bars crossing in a diamond lattice, on transparent ground."""
    rows = blank()
    for y in range(N):
        for x in range(N):
            a = (x + y) % 8 == 3
            b = (x - y) % 8 == 3
            if a and b:
                rows[y][x] = (250, 252, 255, 255)  # the joins catch the light
            elif a:
                rows[y][x] = MITHRAL + (255,)
            elif b:
                rows[y][x] = SHEEN + (255,)
    return rows


def mithral_lattice_edge():
    """The pane's thin edges: only where a bar meets the edge, so stacked lattices show no seam."""
    rows = blank()
    for y in (3, 5, 11, 13):
        for x in range(N):
            rows[y][x] = MITHRAL + (255,)
    return rows


WAYSTONE = (116, 118, 122)
RUNE_GLOW = (255, 212, 92)


WAYSTONE_RUNES = [
    # four rune rows per half, each 3 wide and 3 tall, in a column at x 7..9 (the stone spans x 3..12)
    [(0, 0), (1, 0), (2, 0), (1, 1), (1, 2)],          # T
    [(0, 0), (0, 1), (0, 2), (1, 1), (2, 0), (2, 2)],  # K-ish
    [(1, 0), (0, 1), (2, 1), (1, 2)],                  # diamond
    [(0, 0), (0, 1), (0, 2), (1, 2), (2, 2), (2, 1)],  # hook
]


def waystone(upper):
    rng = random.Random(97 if upper else 101)
    rows = blank()
    for y in range(N):
        for x in range(N):
            d = rng.randint(-8, 8)
            if rng.random() < 0.08:
                d -= 18  # pits in the weathered stone
            rows[y][x] = px(WAYSTONE, d)
    # a sunken channel for the runes
    for y in range(N):
        for x in (6, 10):
            rows[y][x] = px(WAYSTONE, -26 + rng.randint(-3, 3))
    order = WAYSTONE_RUNES if upper else list(reversed(WAYSTONE_RUNES))
    for i, rune in enumerate(order):
        oy = 1 + i * 4 - (1 if upper else 0)
        if upper and i == 0:
            continue  # the top of the stone is plain
        for (dx, dy) in rune:
            y = oy + dy
            if 0 <= y < N:
                rows[y][7 + dx] = RUNE_GLOW + (255,)
    return rows


def waystone_side():
    rng = random.Random(103)
    rows = blank()
    for y in range(N):
        for x in range(N):
            d = rng.randint(-8, 8) - 6
            if rng.random() < 0.08:
                d -= 18
            rows[y][x] = px(WAYSTONE, d)
    return rows


# --- Dwarven Fortresses shapes ---------------------------------------------------------------------------------
COLUMN = (70, 70, 78)


def runic_column():
    """The column's shaft: fluted dark stone with one gold rune down the middle face (the faces use x 3-13)."""
    rng = random.Random(131)
    rows = blank()
    for y in range(N):
        for x in range(N):
            f = (x - 3) % 3
            d = (8, 0, -12)[f] + rng.randint(-3, 3)
            if x in (3, 4):
                d += 6
            if x in (11, 12):
                d -= 8
            rows[y][x] = px(COLUMN, d)
    # a sunken panel for the rune, and the rune in it
    for y in range(4, 12):
        for x in range(6, 10):
            rows[y][x] = px(COLUMN, -18 + rng.randint(-2, 2))
    for (x, y) in ((7, 5), (7, 6), (7, 7), (7, 8), (7, 9), (7, 10), (8, 6), (8, 8), (6, 7), (8, 10)):
        rows[y][x] = (GOLD if y < 8 else GOLD_DARK) + (255,)
    return rows


def runic_column_top():
    rng = random.Random(137)
    rows = blank()
    for y in range(N):
        for x in range(N):
            r = max(abs(x - 7.5), abs(y - 7.5)) * 0.7 + min(abs(x - 7.5), abs(y - 7.5)) * 0.4
            if r < 1.2:
                rows[y][x] = GOLD + (255,)
                continue
            d = (6 if int(r) % 2 else -6) + rng.randint(-3, 3)
            rows[y][x] = px(COLUMN, d)
    return rows


def runic_column_cap():
    """The base and capital mouldings: the capital's faces use rows 0-4, the base's rows 11-15."""
    rng = random.Random(139)
    bands = {0: 18, 1: 6, 2: -8, 3: None, 4: -16, 11: -10, 12: None, 13: 14, 14: 2, 15: -18}
    rows = blank()
    for y in range(N):
        for x in range(N):
            band = bands.get(y, 0)
            if band is None:
                rows[y][x] = (GOLD if x % 4 else GOLD_DARK) + (255,)
                continue
            rows[y][x] = px(STONE, band + rng.randint(-4, 4) + (4 if x == 0 else -4 if x == N - 1 else 0))
    return rows


def runic_corbel():
    """The bracket's faces: the top plate (rows 0-4) with a gold band, then two receding steps (rows 5-9, 10-15)."""
    rng = random.Random(149)
    rows = blank()
    for y in range(N):
        for x in range(N):
            d = rng.randint(-4, 4)
            if y in (0, 5, 10):
                d += 14
            elif y in (4, 9, 15):
                d -= 18
            elif y in (6, 11):
                d += 4
            rows[y][x] = px(STONE, d)
    for x in range(N):
        rows[2][x] = (GOLD if x % 3 else GOLD_DARK) + (255,)
    # a gold drop on the lowest step, like a rivet
    for (x, y) in ((7, 12), (8, 12), (7, 13), (8, 13)):
        rows[y][x] = (GOLD if y == 12 else GOLD_DARK) + (255,)
    return rows


TEXTURES = {
    'runic_deepslate_bricks': runic_bricks,
    'chiseled_runic_deepslate_bricks': chiseled_runic,
    'clan_crest': clan_crest,
    'ancestor_statue': ancestor_statue,
    'ancestor_statue_side': ancestor_statue_side,
    'forge_bricks': forge_bricks,
    'gold_veined_pillar': gold_veined_pillar,
    'gold_veined_pillar_top': gold_veined_pillar_top,
    'ale_keg_wall': ale_keg_wall,
    'ale_keg_wall_side': ale_keg_wall_side,
    'treasury_coin_pile': coin_pile_top,
    'treasury_coin_pile_side': coin_pile_side,
    'mithral_lattice': mithral_lattice,
    'mithral_lattice_edge': mithral_lattice_edge,
    'waystone_lower': lambda: waystone(False),
    'waystone_upper': lambda: waystone(True),
    'waystone_side': waystone_side,
    'runic_column': runic_column,
    'runic_column_top': runic_column_top,
    'runic_column_cap': runic_column_cap,
    'runic_corbel': runic_corbel,
}


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, draw in TEXTURES.items():
        pngio.write_png(os.path.join(OUT, name + '.png'), N, N, draw())
    print(f'wrote {len(TEXTURES)} textures')


if __name__ == '__main__':
    main()
