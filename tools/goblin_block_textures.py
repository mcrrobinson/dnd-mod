"""Generates the Goblin Camp and Goblin Warren building-block textures (Building Blocks epic).

    python3 tools/goblin_block_textures.py

All 16x16 under textures/block/, drawn as a messier, cheaper copy of the orcs' materials:
- patchwork_hide: brown, dirty green and grey hide and cloth scraps sewn together with big uneven stitches,
  with one patch of ripped sackcloth (also the Patchwork Hide Carpet)
- scrap_lashed_planks: boards of different woods nailed together at angles with rusty nails, a diagonal
  brace and a rope lashing (stairs and slab share it, tools/block_families.py)
- goblin_junk_pile, goblin_junk_pile_top: a heap of broken blades, a cracked pot, bones and a rusty helmet
- crude_glyph_daub_<glyph>: cobblestone smeared with a goblin war mark, and glyph_daubed_spruce_log_<glyph>
  the same marks on spruce bark; <glyph> is skull (red), fist (black, yellow on bark), arrow (yellow) or
  warlord (red)
- rope_and_bone_fence, rope_and_bone_fence_cap: a spruce post with rope rails and lashings, topped by a
  small skull
- war_order_board: a crude plank board with scraps of paper pinned by daggers
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

LEATHER = (118, 76, 44)
CLOTH_GREEN = (84, 98, 42)
CLOTH_GREY = (88, 90, 86)
SACK = (166, 140, 96)
THREAD = (196, 178, 136)
SPRUCE_BARK = (59, 39, 19)
SPRUCE = (122, 90, 52)
OAK = (172, 138, 84)
BIRCH = (196, 180, 128)
GREY_WOOD = (112, 104, 92)
DARK_OAK = (74, 52, 30)
ROPE = (176, 150, 98)
ROPE_DARK = (128, 104, 64)
RUST = (138, 72, 38)
RUST_LIGHT = (176, 98, 52)
IRON = (150, 150, 150)
BONE = (226, 220, 196)
BONE_DARK = (180, 172, 146)
STONE = (120, 120, 120)
MORTAR = (72, 72, 72)
PAPER = (214, 202, 168)
INK = (64, 52, 40)
PAINTS = {'red': (164, 34, 26), 'black': (32, 28, 28), 'yellow': (214, 172, 40)}


def shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)


def blank(c=CLEAR):
    return [[c] * N for _ in range(N)]


# --- Patchwork Hide -----------------------------------------------------------------------------------------

# Patches as (x0, y0, x1, y1, kind); they tile across the texture's edges.
PATCHES = [
    (0, 0, 7, 6, 'leather'),
    (7, 0, 16, 5, 'green'),
    (11, 5, 16, 12, 'leather_dark'),
    (0, 6, 5, 16, 'grey'),
    (5, 6, 11, 12, 'sack'),
    (5, 12, 16, 16, 'green_dark'),
]


def patch_px(kind, x, y, rng):
    if kind == 'leather':
        return shade(LEATHER, rng.randint(-8, 6) + (6 if (x * 3 + y * 5) % 7 == 0 else 0))
    if kind == 'leather_dark':
        return shade(LEATHER, -22 + rng.randint(-6, 6))
    if kind in ('green', 'green_dark'):
        weave = 5 if (x + y) % 2 else -5
        return shade(CLOTH_GREEN, weave + rng.randint(-5, 5) + (-14 if kind == 'green_dark' else 0))
    if kind == 'grey':
        weave = 4 if (x + 2 * y) % 3 else -6
        return shade(CLOTH_GREY, weave + rng.randint(-4, 4))
    # Sackcloth: a coarse grid weave.
    weave = -16 if x % 2 == 0 and y % 2 == 0 else (6 if (x + y) % 2 else -2)
    return shade(SACK, weave + rng.randint(-5, 5))


def patchwork_hide():
    rng = random.Random(7)
    rows = blank()
    owner = [[None] * N for _ in range(N)]
    for (x0, y0, x1, y1, kind) in PATCHES:
        for y in range(y0, y1):
            for x in range(x0, x1):
                owner[y][x] = kind
                rows[y][x] = patch_px(kind, x, y, rng)
    # Seams: darken a pixel wherever the patch below or to the right differs (1-px shading, no outline).
    for y in range(N):
        for x in range(N):
            k = owner[y][x]
            if owner[(y + 1) % N][x] != k or owner[y][(x + 1) % N] != k:
                rows[y][x] = shade(rows[y][x], -26)
    # Big uneven stitches across the seams: 2-3 px thread dashes, irregularly spaced.
    stitches = [(7, 1, 'v'), (7, 4, 'v'), (2, 6, 'h'), (11, 7, 'v'), (11, 10, 'v'), (9, 12, 'h'), (13, 12, 'h'),
                (14, 5, 'h'), (5, 8, 'v'), (5, 13, 'v'), (3, 0, 'h'), (11, 3, 'v'), (1, 11, 'h')]
    for (x, y, d) in stitches:
        for i in range(-1, 2):
            px, py = (x + i, y) if d == 'v' else (x, y + i)
            rows[py % N][px % N] = THREAD + (255,) if i != 1 else shade(THREAD, -30)
    # A rip in the sackcloth: a jagged dark tear with the frayed edge lighter.
    for (x, y) in [(7, 8), (8, 8), (8, 9), (9, 9), (9, 10)]:
        rows[y][x] = (40, 30, 22, 255)
    for (x, y) in [(7, 7), (9, 8), (10, 10)]:
        rows[y][x] = shade(SACK, 24)
    return rows


# --- Scrap-lashed Planks ------------------------------------------------------------------------------------

def scrap_lashed_planks():
    rng = random.Random(13)
    rows = blank()
    # Boards of mismatched woods and widths; each board's seams sit at different places.
    boards = [(0, 4, SPRUCE, 5), (4, 7, OAK, 11), (7, 11, GREY_WOOD, 3), (11, 16, DARK_OAK, 9)]
    for (y0, y1, wood, seam) in boards:
        for y in range(y0, y1):
            for x in range(N):
                d = rng.randint(-6, 6) + (8 if (x * 7 + y * 3) % 11 == 0 else 0)
                if y == y0:
                    d += 10
                elif y == y1 - 1:
                    d -= 24
                if x == seam:
                    d -= 30
                rows[y][x] = shade(wood, d)
    # A diagonal brace of birch nailed across, at a slant from bottom-left to top-right.
    for x in range(N):
        yc = 13 - (x * 10) // 15
        for t in range(3):
            y = yc + t - 1
            if 0 <= y < N:
                d = (10 if t == 0 else -18 if t == 2 else 0) + rng.randint(-5, 5)
                rows[y][x] = shade(BIRCH, d)
        if 0 <= yc + 2 < N:
            rows[yc + 2][x] = shade(rows[yc + 2][x], -30)
    # Rope lashing round the brace and the boards at x = 12-13.
    for y in range(1, 9):
        for x in (12, 13):
            rows[y][x] = (ROPE if (x + y) % 2 else ROPE_DARK) + (255,)
    # Rusty nails (with a light pixel on top) at board ends and through the brace.
    for (x, y) in [(1, 1), (9, 2), (3, 8), (14, 13), (6, 12), (2, 5), (8, 5), (6, 9)]:
        rows[y][x] = RUST + (255,)
        rows[y - 1][x] = shade(RUST_LIGHT, 0) if y > 0 else rows[y - 1][x]
    return rows


# --- Goblin Junk Pile ---------------------------------------------------------------------------------------

def junk_ground(rng):
    rows = blank()
    for y in range(N):
        for x in range(N):
            c = (70, 58, 46) if (x * 5 + y * 3) % 7 else (54, 46, 38)
            rows[y][x] = shade(c, rng.randint(-8, 8))
    return rows


def blade(rows, pts):
    """A broken blade: iron with a light edge along the given (x, y) path, then a rusty break."""
    for i, (x, y) in enumerate(pts):
        rows[y][x] = shade(IRON, 30 if i % 2 else 0)
        if y + 1 < N:
            rows[y + 1][x] = shade(IRON, -40)
    x, y = pts[-1]
    rows[y][x] = RUST + (255,)


def bone(rows, x0, y0, length, vertical=False):
    for i in range(length):
        x, y = (x0, y0 + i) if vertical else (x0 + i, y0)
        rows[y][x] = BONE + (255,)
        ox, oy = (x + 1, y) if vertical else (x, y + 1)
        rows[oy % N][ox % N] = BONE_DARK + (255,)
    # Knobbly ends.
    for end in (0, length - 1):
        x, y = (x0, y0 + end) if vertical else (x0 + end, y0)
        if vertical:
            rows[y][(x - 1) % N] = BONE + (255,)
        else:
            rows[(y - 1) % N][x] = BONE + (255,)


def helmet(rows, cx, cy):
    """A rusty pot helmet: a dome with a rim and a dent."""
    for dy in range(-3, 1):
        half = {-3: 1, -2: 2, -1: 3, 0: 3}[dy]
        for dx in range(-half, half + 1):
            d = 16 if dx < 0 else -6
            rows[cy + dy][cx + dx] = shade(RUST, d - abs(dx) * 3 + (24 if (dx, dy) == (-1, -2) else 0))
    for dx in range(-4, 5):
        rows[cy + 1][cx + dx] = shade(RUST, -24)
    rows[cy - 1][cx + 1] = shade(RUST, -40)


def pot(rows, cx, cy):
    """A cracked clay pot lying on its side: terracotta with a dark crack and a dark mouth."""
    clay = (164, 90, 56)
    for dy in range(-2, 3):
        for dx in range(-2, 3):
            if abs(dx) + abs(dy) <= 3:
                rows[cy + dy][cx + dx] = shade(clay, 12 - 6 * (dy + 2))
    rows[cy][cx + 2] = (36, 24, 18, 255)
    rows[cy + 1][cx + 2] = (36, 24, 18, 255)
    for (dx, dy) in [(-1, -2), (0, -1), (0, 0), (1, 1)]:
        rows[cy + dy][cx + dx] = shade(clay, -50)


def junk_pile_side():
    rng = random.Random(21)
    rows = junk_ground(rng)
    blade(rows, [(1, 9), (2, 8), (3, 7), (4, 6), (5, 5)])
    rows[10][0] = (88, 56, 30, 255)
    rows[9][0] = shade(IRON, -20)
    bone(rows, 8, 12, 6)
    bone(rows, 14, 2, 5, vertical=True)
    helmet(rows, 9, 7)
    pot(rows, 3, 13)
    rows[2][4] = BONE + (255,)
    rows[3][10] = shade(IRON, 10)
    return rows


def junk_pile_top():
    rng = random.Random(22)
    rows = junk_ground(rng)
    blade(rows, [(9, 2), (10, 3), (11, 4), (12, 5), (13, 6), (14, 7)])
    bone(rows, 1, 3, 5)
    bone(rows, 4, 9, 6, vertical=True)
    helmet(rows, 10, 13)
    rows[7][1] = shade(IRON, 20)
    rows[14][1] = BONE + (255,)
    rows[1][14] = RUST + (255,)
    return rows


# --- Crude Glyph Daub ---------------------------------------------------------------------------------------

def cobblestone(seed):
    """Rough stones in dark mortar: the nearest of a few seed points (tiling) picks each pixel's stone."""
    rng = random.Random(seed)
    seeds = [(2, 2), (9, 1), (14, 5), (5, 7), (11, 9), (1, 12), (7, 13), (14, 14), (3, 4)]
    tones = [rng.randint(-14, 30) for _ in seeds]

    def nearest(x, y):
        best = []
        for i, (sx, sy) in enumerate(seeds):
            dx = min(abs(x - sx), N - abs(x - sx))
            dy = min(abs(y - sy), N - abs(y - sy))
            best.append((dx * dx + dy * dy * 1.3, i))
        best.sort()
        return best[0][1], best[1][0] - best[0][0]

    rows = blank()
    for y in range(N):
        for x in range(N):
            i, gap = nearest(x, y)
            if gap < 2.2:
                rows[y][x] = shade(MORTAR, rng.randint(-6, 4))
                continue
            d = tones[i] + rng.randint(-6, 6)
            # Light on each stone's upper-left, dark on its lower-right.
            j, _ = nearest(x + 1, y + 1)
            k, _ = nearest(x - 1, y - 1)
            if j != i:
                d -= 22
            elif k != i:
                d += 16
            rows[y][x] = shade(STONE, d)
    return rows


def spruce_bark(seed):
    rng = random.Random(seed)
    rows = blank()
    ridges = [rng.randint(-10, 10) for _ in range(N)]
    for y in range(N):
        for x in range(N):
            d = ridges[x] + rng.randint(-4, 4)
            if x % 4 == 0 and (y + x) % 5 != 0:
                d -= 14
            elif x % 4 == 1:
                d += 10
            rows[y][x] = shade(SPRUCE_BARK, d)
    return rows


GLYPHS = {
    # A grinning skull: round crown, two eye holes, a nose and teeth.
    'skull': ('red', [
        '...#####...',
        '..#######..',
        '.#########.',
        '.##..#..##.',
        '.##..#..##.',
        '.#########.',
        '..###.###..',
        '...#####...',
        '...#.#.#...',
    ]),
    # A clenched fist, raised.
    'fist': ('black', [
        '.##.##.##.',
        '##########',
        '#.#..#..##',
        '##########',
        '#######..#',
        '.########.',
        '..######..',
        '...####...',
        '...####...',
        '...####...',
    ]),
    # A war arrow pointing up and right: go this way.
    'arrow': ('yellow', [
        '.....######',
        '.......####',
        '......#####',
        '.....###.##',
        '....###...#',
        '...###.....',
        '..###......',
        '.###.......',
        '###........',
        '.#.........',
    ]),
    # The Warlord as a stick figure: a spiky crown, arms up, one hand holding a cleaver.
    'warlord': ('red', [
        '.#.#.#..###',
        '.#####..###',
        '..###....#.',
        '..###...#..',
        '#..#...#...',
        '.#####.....',
        '...#.......',
        '...#.......',
        '..#.#......',
        '.#...#.....',
        '#.....#....',
    ]),
}


def daub(rows, glyph, seed, dark=False):
    """Smears the glyph's paint over a background: blotchy paint, a darker stroke edge and a few drips. On a
    dark background (bark) black paint wouldn't show, so the goblins use yellow there."""
    rng = random.Random(seed)
    paint_name, art = GLYPHS[glyph]
    if dark and paint_name == 'black':
        paint_name = 'yellow'
    paint = PAINTS[paint_name]
    h, w = len(art), len(art[0])
    ox, oy = (N - w) // 2, (N - h) // 2
    for y, line in enumerate(art):
        for x, ch in enumerate(line):
            if ch != '#':
                continue
            px, py = ox + x, oy + y
            d = rng.randint(-14, 10)
            below = y + 1 >= h or art[y + 1][x] != '#'
            if below:
                d -= 18
            rows[py][px] = shade(paint, d)
    # Drips running down from the lowest painted pixels of a few columns.
    for x in range(w):
        col = [y for y in range(h) if art[y][x] == '#']
        if col and rng.random() < 0.3:
            y = oy + col[-1] + 1
            for i in range(rng.randint(1, 2)):
                if y + i < N:
                    rows[y + i][ox + x] = shade(paint, -24 - 6 * i)
    return rows


# --- Rope-and-Bone Fence ------------------------------------------------------------------------------------

def rope_and_bone_fence():
    """Post columns 6-9 (the vanilla fence post uv) are spruce with rope wraps where the rails (rows 1-2 and
    7-8) meet it; elsewhere the rails' rows are twisted rope, and the rest is plain spruce for the post top."""
    rng = random.Random(31)
    rows = blank()
    for y in range(N):
        for x in range(N):
            d = rng.randint(-6, 6) + (-14 if x % 3 == 0 else 0)
            rows[y][x] = shade(SPRUCE, d - 30 if 6 <= x <= 9 else d)
    for y in (1, 2, 7, 8):
        for x in range(N):
            twist = (x + y) % 3
            rows[y][x] = shade(ROPE, 16 if twist == 0 else -10 if twist == 1 else 0)
            if y in (2, 8):
                rows[y][x] = shade(rows[y][x], -16)
    # A bone tied into the post between the rope wraps.
    for y in range(3, 7):
        rows[y][7] = BONE + (255,)
        rows[y][8] = BONE_DARK + (255,)
    # Post top (uv 6-10, 6-10): end grain.
    for y in range(6, 10):
        for x in range(6, 10):
            ring = max(abs(2 * x - 15), abs(2 * y - 15)) // 2
            rows[y][x] = shade(SPRUCE, -6 if ring % 2 else 8)
    return rows


def rope_and_bone_fence_cap():
    """The small skull on top of the post: uv 0-6 x 0-4 is a side (eye sockets and teeth), 0-6 x 6-12 the top."""
    rows = blank(shade(BONE, 0))
    side = [
        'BBBBBB',
        'B..B.B',
        'BBBBBB',
        'b.b.bb',
    ]
    for y, line in enumerate(side):
        for x, ch in enumerate(line):
            rows[y][x] = {'B': shade(BONE, 0), 'b': shade(BONE_DARK, 0), '.': (40, 34, 28, 255)}[ch]
    rows[1][3] = shade(BONE_DARK, -10)
    for y in range(6, 12):
        for x in range(6):
            edge = x in (0, 5) or y in (6, 11)
            rows[y][x] = shade(BONE, -24 if edge else 6 if (x + y) % 4 == 0 else 0)
    rows[8][2] = shade(BONE_DARK, -20)
    rows[9][3] = shade(BONE_DARK, -20)
    return rows


# --- War Order Board ----------------------------------------------------------------------------------------

def war_order_board():
    rng = random.Random(41)
    rows = blank()
    # Vertical boards of uneven widths, with dark gaps between them.
    edges = [0, 4, 7, 12, 16]
    tones = [0, -14, 8, -6]
    for y in range(N):
        for x in range(N):
            b = max(i for i in range(4) if edges[i] <= x)
            d = tones[b] + rng.randint(-6, 6)
            if x == edges[b]:
                d -= 34
            elif x == edges[b + 1] - 1:
                d -= 10
            rows[y][x] = shade(GREY_WOOD if b % 2 else SPRUCE, d)
    # Scraps of paper (scrawled orders), each pinned by a dagger: (x0, y0, x1, y1, dagger x, dagger y).
    scraps = [(1, 2, 7, 7, 4, 2), (8, 1, 14, 6, 10, 1), (3, 9, 9, 14, 6, 9), (10, 8, 15, 15, 12, 8)]
    for (x0, y0, x1, y1, dx, dy) in scraps:
        for y in range(y0, y1):
            for x in range(x0, x1):
                torn = (x == x0 or x == x1 - 1) and (y * 3 + x) % 4 == 0
                if torn:
                    continue
                d = rng.randint(-8, 6) - (16 if y == y1 - 1 else 0)
                rows[y][x] = shade(PAPER, d)
        # Scrawled lines of goblin script.
        for y in range(y0 + 2, y1 - 1, 2):
            for x in range(x0 + 1, x1 - 1):
                if (x * 5 + y) % 3:
                    rows[y][x] = INK + (255,)
        # The dagger: a dark hilt with a guard, its point driven in through the top of the scrap.
        rows[dy][dx] = (60, 40, 28, 255)
        rows[dy + 1][dx - 1] = shade(IRON, 10)
        rows[dy + 1][dx] = shade(IRON, 10)
        rows[dy + 1][dx + 1] = shade(IRON, -10)
        if dy > 0:
            rows[dy - 1][dx] = (88, 60, 36, 255)
    # A red smear: the Warlord's mark.
    for (x, y) in [(13, 11), (12, 12), (13, 12), (12, 13)]:
        rows[y][x] = shade(PAINTS['red'], 0)
    return rows


def main():
    os.makedirs(OUT, exist_ok=True)
    textures = {
        'patchwork_hide': patchwork_hide(),
        'scrap_lashed_planks': scrap_lashed_planks(),
        'goblin_junk_pile': junk_pile_side(),
        'goblin_junk_pile_top': junk_pile_top(),
        'rope_and_bone_fence': rope_and_bone_fence(),
        'rope_and_bone_fence_cap': rope_and_bone_fence_cap(),
        'war_order_board': war_order_board(),
    }
    for i, glyph in enumerate(GLYPHS):
        textures['crude_glyph_daub_' + glyph] = daub(cobblestone(50 + i), glyph, 60 + i)
        textures['glyph_daubed_spruce_log_' + glyph] = daub(spruce_bark(70 + i), glyph, 80 + i, dark=True)
    for name, rows in textures.items():
        pngio.write_png(os.path.join(OUT, name + '.png'), N, N, rows)
    print(f'wrote {len(textures)} textures')


if __name__ == '__main__':
    main()
