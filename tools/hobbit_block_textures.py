"""Generates the Hobbit Villages building-block textures (Building Blocks epic).

    python3 tools/hobbit_block_textures.py [--preview DIR]

16x16 textures under textures/block/ for the families in ModBlockFamilies (thatch, mossy thatch, wattle and
daub, timbered wattle and daub, smial turf wall, round door frame stone and keystone, hearth stone, pipe-weed
bundle) and the one-off blocks in HobbitBlocks (larder shelf, ale cask, hanging herbs, onion string).
Vanilla-like palettes, 1-px shading, no outlines. --preview also writes each texture upscaled 16x to DIR.
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/block')
N = 16
CLEAR = (0, 0, 0, 0)


def rgba(c, d=0):
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)


def blank(color=CLEAR):
    return [[color] * N for _ in range(N)]


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


# --- straw ---------------------------------------------------------------------------------------------------

STRAW = [(222, 186, 98), (204, 166, 80), (186, 148, 66), (166, 128, 54)]
STRAW_GAP = (104, 74, 34)
MOSS = [(104, 122, 70), (86, 104, 58), (120, 134, 84), (72, 88, 50)]


def thatch(mossy=False, seed=7):
    """Straw strands running down in uneven bundles, dark gaps between them, frayed at the bottom."""
    rng = random.Random(seed)
    rows = blank()
    # Bundles 3-5 strands wide; between them a darker strand of shadow, deepest low down where the bundles part.
    edges = set()
    x = rng.randint(0, 2)
    while x < N:
        edges.add(x)
        x += rng.choice((3, 4, 4, 5))
    for x in range(N):
        tone = rng.randint(0, 3)
        end = N - 1 - rng.choice((0, 0, 0, 1, 1, 2))  # frayed: some strands stop short of the bottom
        for y in range(N):
            if y > end:
                rows[y][x] = rgba(STRAW_GAP, -20)
                continue
            # Streaks: a strand changes shade every few pixels.
            if rng.random() < 0.18:
                tone = max(0, min(3, tone + rng.choice((-1, 1))))
            d = rng.randint(-4, 4)
            c = STRAW[tone]
            if x in edges and y > 2:
                c = mix(STRAW[3], STRAW_GAP, 0.35 + 0.5 * y / N)
            if y == 0:
                d -= 22  # the course above shadows the top
            elif y == 1:
                d -= 10
            if y >= end - 1:
                d += 10  # sunlit frayed tips
            rows[y][x] = rgba(c, d)
    if mossy:
        # Green-grey moss creeping down from the top edge in ragged patches, thinning out by the top third.
        for x in range(N):
            depth = rng.choice((0, 1, 2, 2, 3, 4, 5))
            for y in range(depth):
                if rng.random() < 0.85:
                    rows[y][x] = rgba(MOSS[rng.randint(0, 3)], rng.randint(-4, 4) - (6 if y == depth - 1 else 0))
        for _ in range(5):
            x, y = rng.randrange(N), rng.randint(3, 6)
            rows[y][x] = rgba(MOSS[rng.choice((1, 3))])
    return rows


# --- wattle and daub ---------------------------------------------------------------------------------------

PLASTER = (226, 214, 188)
WATTLE = (196, 180, 148)
DARK_OAK = [(74, 50, 26), (62, 41, 20), (52, 34, 16), (84, 58, 32)]


def wattle_and_daub(seed=11):
    """Cream plaster with faint woven withies showing through."""
    rng = random.Random(seed)
    rows = blank()
    for y in range(N):
        for x in range(N):
            rows[y][x] = rgba(PLASTER, rng.randint(-6, 5))
    # Horizontal withies weaving over and under stakes every 4 px: where a withy passes over, it shows through.
    for i, wy in enumerate((2, 7, 12)):
        for x in range(N):
            over = ((x // 4) + i) % 2 == 0
            if over and rng.random() < 0.85:
                rows[wy][x] = rgba(WATTLE, rng.randint(-4, 4))
                if rng.random() < 0.5:
                    rows[wy + 1][x] = rgba(mix(WATTLE, PLASTER, 0.5), rng.randint(-3, 3))
    # Faint stakes.
    for sx in (1, 9):
        for y in range(N):
            if rng.random() < 0.35:
                rows[y][sx] = rgba(mix(WATTLE, PLASTER, 0.4), rng.randint(-3, 3))
    # A few cracks and darker patches.
    for (x, y) in ((5, 10), (6, 11), (13, 4), (12, 5)):
        rows[y][x] = rgba(PLASTER, -26)
    return rows


def timbered(seed=13):
    """Wattle and daub with a dark oak beam across the top and down the left side."""
    rng = random.Random(seed)
    rows = wattle_and_daub(seed)
    for y in range(N):
        for x in range(N):
            beam_top = y < 3
            beam_side = x < 3
            if not (beam_top or beam_side):
                continue
            # Grain runs along the beam: horizontally in the top beam, vertically down the side.
            along = x if beam_top else y
            across = y if beam_top else x
            c = DARK_OAK[(across * 3 + along // 5) % 3]
            d = rng.randint(-4, 4)
            if across == 0:
                d += 12
            elif across == 2:
                d -= 12
            rows[y][x] = rgba(c, d)
    # Shadow of the beams on the plaster.
    for x in range(3, N):
        rows[3][x] = rgba(rows[3][x], -18)
    for y in range(3, N):
        rows[y][3] = rgba(rows[y][3], -18)
    # A peg where the beams meet.
    rows[1][1] = rgba((40, 26, 12))
    return rows


# --- turf -------------------------------------------------------------------------------------------------

GRASS = [(124, 176, 74), (108, 160, 62), (140, 190, 88), (94, 142, 54)]
DIRT = [(134, 96, 67), (121, 85, 58), (150, 108, 74), (106, 74, 50)]
ROOT = (204, 184, 146)
PEBBLE = [(132, 132, 128), (112, 112, 110), (156, 154, 150)]


def dirt(seed):
    rng = random.Random(seed)
    rows = blank()
    for y in range(N):
        for x in range(N):
            rows[y][x] = rgba(DIRT[rng.choice((0, 0, 1, 1, 2, 3))], rng.randint(-3, 3))
    return rows


def turf_side(seed=17):
    """A grass band at the top over packed earth with pale roots and a pebble or two."""
    rng = random.Random(seed)
    rows = dirt(seed)
    # Pale roots hanging down from the grass, wandering a pixel side to side.
    for rx in (2, 7, 12):
        x = rx
        for y in range(4, 4 + rng.randint(5, 9)):
            rows[y][x % N] = rgba(ROOT, rng.randint(-10, 6))
            if rng.random() < 0.3:
                x += rng.choice((-1, 1))
    # Pebbles: 2x2 or 2x1 grey stones with a light top.
    for (px, py, w) in ((4, 11, 2), (10, 8, 2), (14, 13, 1)):
        for dx in range(w):
            rows[py][px + dx] = rgba(PEBBLE[2])
            rows[py + 1][px + dx] = rgba(PEBBLE[1])
    # Grass band: 3 rows, with a jagged lower edge of blades hanging over the earth.
    for x in range(N):
        depth = 3 + rng.choice((0, 0, 1, 1, 2))
        for y in range(depth):
            d = rng.randint(-6, 6)
            if y == depth - 1:
                d -= 14
            rows[y][x] = rgba(GRASS[rng.randint(0, 3)], d)
    # Shadow under the grass.
    for x in range(N):
        for y in range(3, 7):
            if rows[y][x][1] < 120 and rows[y - 1][x][1] >= 120:
                rows[y][x] = rgba(rows[y][x], -16)
                break
    return rows


def turf_top(seed=19):
    rng = random.Random(seed)
    rows = blank()
    for y in range(N):
        for x in range(N):
            rows[y][x] = rgba(GRASS[rng.choice((0, 0, 1, 1, 2, 3))], rng.randint(-6, 6))
    return rows


# --- stone ------------------------------------------------------------------------------------------------

FRAME = (178, 178, 172)


def frame_stone(seed=23):
    """Smooth pale grey stone with a lighter rim along the top and left (the arch's inner edge)."""
    rng = random.Random(seed)
    rows = blank()
    for y in range(N):
        for x in range(N):
            d = rng.randint(-4, 4)
            if y == 0 or x == 0:
                d += 18
            elif y == 1 or x == 1:
                d += 8
            elif y == N - 1 or x == N - 1:
                d -= 22
            rows[y][x] = rgba(FRAME, d)
    # A few faint flecks.
    for _ in range(8):
        x, y = rng.randint(2, 14), rng.randint(2, 14)
        rows[y][x] = rgba(FRAME, rng.choice((-12, 10)))
    return rows


def keystone(seed=29):
    """Frame stone with a wedge-shaped keystone in the middle, wide at the top."""
    rng = random.Random(seed)
    rows = frame_stone(seed)
    for y in range(1, N - 1):
        half = 6 - (y - 1) * 3 // 13  # 6 wide each side at the top, 3 at the bottom
        lo, hi = 8 - half, 7 + half
        for x in range(lo, hi + 1):
            d = 10 + rng.randint(-3, 3)
            rows[y][x] = rgba(FRAME, d)
        rows[y][lo - 1] = rgba(FRAME, -30)
        rows[y][hi + 1] = rgba(FRAME, -18)
        rows[y][lo] = rgba(FRAME, 24)
    for x in range(1, N - 1):
        if rows[1][x][0] > FRAME[0] + 4:
            rows[1][x] = rgba(FRAME, 26)
    return rows


BRICK = [(152, 76, 58), (134, 64, 48), (168, 90, 70), (120, 56, 42)]
MORTAR = (150, 140, 132)
GLOW = (222, 118, 42)


def hearth_stone(seed=31):
    """Red-brown bricks, soot-blackened towards the middle, an orange glow in the mortar near the bottom."""
    rng = random.Random(seed)
    rows = blank()
    for y in range(N):
        course = y // 4
        offset = 4 if course % 2 else 0
        for x in range(N):
            bx = (x + offset) % 8
            by = y % 4
            mortar = by == 3 or bx == 7
            if mortar:
                c = rgba(MORTAR, rng.randint(-6, 6))
            else:
                brick = ((x + offset) // 8 + course) % 4
                d = rng.randint(-6, 6)
                if by == 0:
                    d += 8
                elif by == 2:
                    d -= 8
                c = rgba(BRICK[brick], d)
            # Soot: darken towards the centre.
            dist = (((x - 7.5) / 9) ** 2 + ((y - 6.5) / 8) ** 2) ** 0.5
            soot = min(0.85, max(0.0, 1.15 - dist) ** 0.9)
            c = mix(c, (30, 24, 22), soot + rng.uniform(-0.05, 0.05) if soot > 0.1 else soot)
            if mortar and y >= 11:
                # The fire's glow in the lower joints.
                c = mix(c, GLOW, 0.35 + (y - 11) * 0.12 + rng.uniform(-0.08, 0.08))
            rows[y][x] = c
    return rows


# --- pipe-weed ------------------------------------------------------------------------------------------

LEAF = [(118, 108, 52), (98, 92, 42), (134, 122, 64), (82, 76, 36), (110, 112, 58)]
TWINE = (198, 176, 124)


def pipeweed_side(seed=37):
    """Like a hay bale: dried brown-green leaves running down, tied with two bands of twine."""
    rng = random.Random(seed)
    rows = blank()
    for x in range(N):
        tone = LEAF[rng.randint(0, 4)]
        for y in range(N):
            d = rng.randint(-6, 6)
            if x % 3 == 2:
                d -= 10
            rows[y][x] = rgba(tone, d)
    # Leaf veins: lighter streaks.
    for _ in range(10):
        x, y = rng.randrange(N), rng.randrange(N - 3)
        for k in range(3):
            rows[y + k][x] = rgba(LEAF[2], 12)
    for band in (3, 11):
        for x in range(N):
            rows[band][x] = rgba(TWINE, rng.randint(-6, 6) + 6)
            rows[band + 1][x] = rgba(TWINE, rng.randint(-6, 6) - 22)
            rows[band - 1][x] = rgba(rows[band - 1][x], -14)
    return rows


def pipeweed_top(seed=41):
    """The leafy cut end: overlapping curled leaves, the twine round the rim."""
    rng = random.Random(seed)
    rows = blank()
    for y in range(N):
        for x in range(N):
            rows[y][x] = rgba(LEAF[3], rng.randint(-8, 2))
    for _ in range(26):
        cx, cy = rng.randrange(N), rng.randrange(N)
        tone = LEAF[rng.choice((0, 2, 4, 1))]
        r = rng.choice((1, 1, 2))
        for y in range(cy - r, cy + r + 1):
            for x in range(cx - r - 1, cx + r + 1):
                if 0 <= x < N and 0 <= y < N and (x - cx) ** 2 + (y - cy) ** 2 <= r * r + 1:
                    d = 10 if y < cy else (-8 if y > cy else 0)
                    rows[y][x] = rgba(tone, d + rng.randint(-3, 3))
    for i in range(N):
        for (x, y) in ((i, 0), (i, N - 1), (0, i), (N - 1, i)):
            rows[y][x] = rgba(TWINE, -20 + rng.randint(-6, 6))
    return rows


# --- larder shelf ----------------------------------------------------------------------------------------

OAK = [(162, 130, 78), (184, 148, 92), (140, 110, 64), (122, 96, 54)]
BACK = (40, 27, 14)
CHEESE = (232, 192, 82)
RIND = (196, 146, 52)
HAM = (170, 92, 74)
HAM_DARK = (132, 66, 52)
BONE = (232, 222, 200)
JAR_LID = (222, 204, 160)


def oak_planks(seed):
    """Oak planks in four horizontal boards, vanilla style."""
    rng = random.Random(seed)
    rows = blank()
    for y in range(N):
        board = y // 4
        for x in range(N):
            seam = (x == (board * 5 + 3) % N)
            d = rng.randint(-5, 5)
            if y % 4 == 3:
                c = rgba(OAK[3], d)
            elif seam:
                c = rgba(OAK[3], d + 4)
            else:
                c = rgba(OAK[(x // 3 + board) % 3 if rng.random() < 0.8 else 0], d)
            rows[y][x] = c
    return rows


def larder_front(seed=43):
    """Dark oak shelves: a hanging ham and two jars of preserves above, a wheel of cheese below."""
    rng = random.Random(seed)
    rows = blank()
    for y in range(N):
        for x in range(N):
            rows[y][x] = rgba(BACK, rng.randint(-3, 3))
    # Frame and shelves.
    for y in range(N):
        for x in range(N):
            frame = x == 0 or x == N - 1 or y == 0
            shelf = y in (7, 8, 14, 15)
            if frame or shelf:
                c = DARK_OAK[0 if (shelf and y in (7, 14)) else 1]
                d = rng.randint(-4, 4)
                if shelf and y in (8, 15):
                    d -= 12
                if x == 0 or y == 0:
                    d += 8
                rows[y][x] = rgba(c, d)
    # Hanging ham on the left: a string from the top, the bone knuckle up, the joint widening below.
    rows[1][4] = rgba(TWINE, -20)
    rows[2][4] = rgba(BONE)
    for y, (lo, hi) in zip(range(3, 7), ((4, 4), (3, 5), (2, 5), (2, 6))):
        for x in range(lo, hi + 1):
            d = 12 if x == lo else (-14 if x == hi else 0)
            rows[y][x] = rgba(HAM_DARK if y == 6 or x == hi else HAM, d + rng.randint(-3, 3))
    rows[3][4] = rgba(BONE, -20)
    rows[5][3] = rgba(HAM, 30)  # fat shine
    # Two jars of preserves on the right: red and purple, cloth lids.
    for jx, fill in ((8, (176, 40, 52)), (12, (112, 44, 98))):
        for x in range(jx, jx + 3):
            rows[3][x] = rgba(JAR_LID, rng.randint(-4, 4))
            for y in range(4, 7):
                d = 14 if x == jx else (-12 if x == jx + 2 else 0)
                rows[y][x] = rgba(fill, d)
        rows[4][jx] = rgba((236, 230, 220))  # glint on the glass
        rows[2][jx + 1] = rgba(JAR_LID, -14)
    # A wheel of cheese below, face on, with a wedge cut out of it, and a small crock beside it.
    wheel = ['.####.',
             '#oooo#',
             '#oo##.',
             '#oo#..',
             '.####.']
    for dy, line in enumerate(wheel):
        for dx, ch in enumerate(line):
            x, y = 2 + dx, 9 + dy
            if ch == '#':
                rows[y][x] = rgba(RIND, (10 if dy == 0 else -6) + rng.randint(-3, 3))
            elif ch == 'o':
                rows[y][x] = rgba(CHEESE, (10 if dy == 1 else 0) + rng.randint(-3, 3))
    rows[10][3] = rgba(CHEESE, 26)
    rows[12][4] = rgba(CHEESE, -22)  # a hole
    # The wedge, standing beside the wheel.
    for y, (lo, hi) in zip(range(11, 14), ((8, 8), (8, 9), (8, 10))):
        for x in range(lo, hi + 1):
            rows[y][x] = rgba(CHEESE, 14 if y == 11 else (-8 if x == hi else 2))
    for y in range(11, 14):
        for x in range(12, 15):
            rows[y][x] = rgba((150, 120, 96), (10 if x == 12 else -8) + rng.randint(-3, 3))
    rows[10][13] = rgba((150, 120, 96), -20)
    return rows


# --- hanging bundles ------------------------------------------------------------------------------------

HERB = [(86, 132, 52), (66, 106, 40), (112, 152, 72), (140, 146, 74)]
ONION = [(228, 192, 112), (206, 164, 84), (244, 218, 156), (166, 124, 62)]


def hanging_herbs(seed=47):
    """Three bunches of herbs tied on a twine, hanging upside down: stems at the top fanning out to leaves."""
    rng = random.Random(seed)
    rows = blank()
    for y in range(0, 4):
        rows[y][8] = rgba(TWINE, -10 if y % 2 else 4)
    for x in range(4, 12):
        rows[4][x] = rgba(TWINE, -16)
    for bx in (5, 8, 11):
        rows[5][bx] = rgba(TWINE, -24)  # the tie
        for y in range(6, 15):
            spread = (y - 5) // 3 + 1
            for x in range(bx - spread, bx + spread + 1):
                if not 0 <= x < N or rng.random() < 0.3 + (0.25 if abs(x - bx) == spread else 0):
                    continue
                tone = HERB[3] if y >= 13 and rng.random() < 0.5 else HERB[rng.randint(0, 2)]
                rows[y][x] = rgba(tone, rng.randint(-6, 6))
        for y in range(6, 9):
            rows[y][bx] = rgba(HERB[1], -10)  # stems
    return rows


def onion_string(seed=53):
    """A braided twine with pale gold onions on either side, their papery tops tucked into the braid."""
    rng = random.Random(seed)
    rows = blank()
    for y in range(N):
        rows[y][7 + (y // 2) % 2] = rgba(TWINE, -8 if y % 2 else 6)
    shape = ['.##.',
             '####',
             '####',
             '.##.']
    for i, oy in enumerate((1, 4, 8, 11)):
        left = i % 2 == 0
        ox = 3 if left else 9
        for dy, line in enumerate(shape):
            for dx, ch in enumerate(line):
                if ch != '#':
                    continue
                d = rng.randint(-4, 4)
                if dy == 0 or dx == 0:
                    tone = ONION[2]
                elif dy == 3 or dx == 3:
                    tone = ONION[1]
                    d -= 8
                else:
                    tone = ONION[0]
                rows[oy + dy][ox + dx] = rgba(tone, d)
        # The papery top runs up to the braid; a root tuft underneath.
        tx = ox + (3 if left else 0)
        rows[oy][tx] = rgba(ONION[3])
        rows[oy - 1 if oy > 0 else 0][tx + (1 if left else -1)] = rgba(ONION[3], -10)
        if oy + 4 < N:
            rows[oy + 4][ox + 1] = rgba(ONION[3], -20)
    return rows


# --- ale cask -------------------------------------------------------------------------------------------

STAVE = [(150, 112, 62), (134, 98, 52), (166, 126, 72)]
HOOP = (82, 82, 88)
HOOP_LIGHT = (128, 128, 134)
BRASS = (214, 170, 64)
DRAGON = (58, 136, 64)


def cask_side(seed=59):
    """Staves running horizontally (along the cask), iron hoops near both ends and round the middle."""
    rng = random.Random(seed)
    rows = blank()
    for y in range(N):
        stave = y // 3
        for x in range(N):
            d = rng.randint(-5, 5)
            if y % 3 == 0:
                d += 8
            elif y % 3 == 2:
                d -= 14
            # The belly of the cask bulges: ends a touch darker.
            if x < 2 or x > 13:
                d -= 10
            rows[y][x] = rgba(STAVE[stave % 3], d)
    for hx in (2, 12):
        for y in range(N):
            rows[y][hx] = rgba(HOOP_LIGHT, rng.randint(-4, 4))
            rows[y][hx + 1] = rgba(HOOP, rng.randint(-4, 4))
        rows[rng.randrange(N)][hx] = rgba((150, 98, 64))  # a fleck of rust
    return rows


def cask_top(seed=59):
    side = cask_side(seed)
    return [[side[x][y] for x in range(N)] for y in range(N)]


def cask_head(tap, seed=61):
    """The round head: oak boards inside an iron hoop; the front has a painted dragon and a brass tap."""
    rng = random.Random(seed)
    rows = blank()
    c = 7.5
    for y in range(N):
        for x in range(N):
            r = ((x - c) ** 2 + (y - c) ** 2) ** 0.5
            if r > 7.6:
                # Corners: the dark ends of the cradle behind the round head.
                rows[y][x] = rgba((56, 38, 20), rng.randint(-4, 4))
            elif r > 6.4:
                rows[y][x] = rgba(HOOP_LIGHT if y + x < 15 else HOOP, rng.randint(-4, 4))
            else:
                board = (x + 1) // 4
                d = rng.randint(-4, 4) + (8 if (x + 1) % 4 == 0 else 0) - (8 if (x + 1) % 4 == 3 else 0)
                rows[y][x] = rgba(STAVE[(board + 1) % 3], d)
    if tap:
        # A little green dragon, rearing, wings up.
        dragon = ['..#.....',
                  '.###..#.',
                  '..##.##.',
                  '...####.',
                  '..#####.',
                  '.##.#.#.',
                  '#.......']
        for dy, line in enumerate(dragon):
            for dx, ch in enumerate(line):
                if ch == '#':
                    rows[3 + dy][4 + dx] = rgba(DRAGON, 14 if dy < 2 else (-10 if dy > 4 else 0))
        rows[4][5] = rgba((236, 200, 70))  # eye
        # Brass tap at the bottom.
        for y in (11, 12):
            rows[y][7] = rgba(BRASS, 16)
            rows[y][8] = rgba(BRASS, -14)
        rows[13][7] = rgba(BRASS, -30)
        rows[13][8] = rgba(BRASS, -40)
        rows[10][6] = rgba(BRASS, 10)
        rows[10][9] = rgba(BRASS, -10)
        rows[10][7] = rgba(BRASS, 30)
        rows[10][8] = rgba(BRASS, 0)
    return rows


TEXTURES = {
    'thatch': lambda: thatch(False),
    'mossy_thatch': lambda: thatch(True),
    'wattle_and_daub': wattle_and_daub,
    'timbered_wattle_and_daub': timbered,
    'smial_turf_wall': turf_side,
    'smial_turf_wall_top': turf_top,
    'smial_turf_wall_bottom': lambda: dirt(71),
    'round_door_frame_stone': frame_stone,
    'round_door_keystone': keystone,
    'hearth_stone': hearth_stone,
    'pipeweed_bundle': pipeweed_side,
    'pipeweed_bundle_top': pipeweed_top,
    'larder_shelf_front': larder_front,
    'larder_shelf_side': lambda: oak_planks(67),
    'larder_shelf_top': lambda: oak_planks(73),
    'hanging_herbs': hanging_herbs,
    'onion_string': onion_string,
    'ale_cask_front': lambda: cask_head(True),
    'ale_cask_back': lambda: cask_head(False, 62),
    'ale_cask_side': cask_side,
    'ale_cask_top': cask_top,
}


def upscale(rows, k=16, bg=(255, 0, 255, 255)):
    out = []
    for row in rows:
        line = []
        for px in row:
            line.extend([px if px[3] else bg] * k)
        out.extend([line] * k)
    return out


def main():
    os.makedirs(OUT, exist_ok=True)
    preview = None
    if '--preview' in sys.argv:
        preview = sys.argv[sys.argv.index('--preview') + 1]
        os.makedirs(preview, exist_ok=True)
    for name, make in TEXTURES.items():
        rows = make()
        pngio.write_png(os.path.join(OUT, name + '.png'), N, N, rows)
        if preview:
            pngio.write_png(os.path.join(preview, name + '.png'), N * 16, N * 16, upscale(rows))
    print(f'wrote {len(TEXTURES)} textures')


if __name__ == '__main__':
    main()
