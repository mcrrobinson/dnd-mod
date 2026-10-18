"""Generates the Dwarven Ruin and Beholder lair building-block textures (Building Blocks epic).

    python3 tools/ruin_block_textures.py

16x16 textures/block/:
- cracked_runic_deepslate_bricks: the Runic Deepslate Bricks (tools/dwarven_block_textures.py) with cracks
  through them and the gold inlay dulled, half of it chipped out.
- mossy_runic_deepslate_bricks: the same bricks with moss grown along the mortar and the inlay gone dull.
- tarnished_gilding (+ _top): dark fluted stone with gold leaf flaking off, leaving dull brown patches; the top
  is a ring of gold leaf round a dark boss.
- ray_glazed_stone: smooth glassy grey with a diagonal sheen and faint concentric swirls, melted smooth.
- collapsed_rubble: broken stone chunks in grey dust.
- petrified_remains: grey stone for the kneeling statue's model, its frightened face in the top-left 6x4.
- eye_stalk_growth (+ .mcmeta): pink-purple stalks ending in closed eyes (cross model); now and then one
  eye opens for a moment.
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402
from dwarven_block_textures import runic_bricks, GOLD, GOLD_DARK, MORTAR  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/block')

N = 16
CLEAR = (0, 0, 0, 0)


def rgba(c):
    return tuple(c[:3]) + (255,)


def shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)


def blank(color=CLEAR):
    return [[color] * N for _ in range(N)]


# --- Fallen Runic Bricks ---------------------------------------------------------------------------------

DULL_GOLD = (150, 122, 64)
DULL_GOLD_DARK = (118, 94, 52)
CHIPPED = (50, 49, 55)   # the groove where the inlay was
CRACK = (28, 28, 32)
CRACK_EDGE = (98, 98, 104)


def is_gold(c):
    return c[:3] in (GOLD, GOLD_DARK)


def dull_inlay(rows, rng, keep):
    """Dulls the gold inlay; each stroke pixel survives with chance {@code keep}, else it's an empty groove."""
    for y in range(N):
        for x in range(N):
            c = rows[y][x]
            if is_gold(c):
                if rng.random() < keep:
                    rows[y][x] = rgba(DULL_GOLD if c[:3] == GOLD else DULL_GOLD_DARK)
                else:
                    rows[y][x] = rgba(CHIPPED)


# Cracks as jagged pixel paths (x, y) that wander across bricks and mortar; each gets a lighter broken edge
# above it. Mortar pixels on a path stay mortar.
CRACKS = [
    [(0, 3), (1, 3), (2, 4), (3, 4), (3, 5), (4, 5), (5, 5), (5, 6), (6, 7), (7, 8), (7, 9), (8, 10), (9, 10),
     (10, 10), (10, 11)],
    [(10, 0), (10, 1), (11, 2), (11, 3), (12, 3), (13, 4)],
    [(13, 4), (13, 5), (14, 6)],
    [(1, 12), (2, 12), (3, 13), (4, 13), (5, 13)],
    [(13, 11), (13, 12), (14, 13), (14, 14)],
]
# Corners knocked off bricks: a shadowed hollow.
CHIPS = [(6, 0), (5, 0), (6, 1), (14, 8), (15, 9), (15, 8), (8, 14), (9, 14), (8, 13)]


def cracked_bricks():
    rng = random.Random(7)
    rows = runic_bricks()
    dull_inlay(rows, rng, 0.5)
    on_path = {p for path in CRACKS for p in path}
    for path in CRACKS:
        for (x, y) in path:
            if rows[y][x][:3] == MORTAR:
                continue
            rows[y][x] = rgba(CRACK)
            if y > 0 and (x, y - 1) not in on_path and rows[y - 1][x][:3] not in (CRACK, MORTAR):
                rows[y - 1][x] = shade(rows[y - 1][x], 9)
    for (x, y) in CHIPS:
        if rows[y][x][:3] != MORTAR:
            rows[y][x] = rgba((40, 40, 46))
    return rows


MOSS = [(66, 88, 36), (82, 106, 42), (96, 120, 48), (56, 74, 30)]


def mossy_bricks():
    rng = random.Random(11)
    rows = runic_bricks()
    dull_inlay(rows, rng, 0.85)
    # Moss along the mortar in clumps, creeping a pixel onto the bricks round it.
    seeds = [(3, 7), (12, 7), (7, 15), (0, 15), (15, 3), (11, 12), (3, 10)]
    for y in range(N):
        for x in range(N):
            near = min(abs(x - sx) + abs(y - sy) for sx, sy in seeds)
            mortar = rows[y][x][:3] == MORTAR
            if mortar and near <= 4 and rng.random() < 0.9:
                rows[y][x] = rgba(MOSS[rng.randrange(3)])
            elif not mortar and near <= 2 and rng.random() < 0.55 and not is_gold(rows[y][x]) \
                    and rows[y][x][:3] not in (DULL_GOLD, DULL_GOLD_DARK):
                # creep only next to mossed mortar
                for dx, dy in ((0, 1), (0, -1), (1, 0), (-1, 0)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < N and 0 <= ny < N and rows[ny][nx][:3] in MOSS:
                        rows[y][x] = rgba(MOSS[rng.choice((0, 3))])
                        break
    return rows


# --- Tarnished Gilding -------------------------------------------------------------------------------------

DARK_STONE = (44, 40, 46)
LEAF = (164, 130, 56)
LEAF_LIGHT = (196, 160, 72)
FLAKED = [(104, 78, 52), (88, 66, 46), (120, 92, 58)]


def gilding_side():
    rng = random.Random(23)
    rows = blank()
    for y in range(N):
        for x in range(N):
            # Shallow fluting: a lighter ridge every 4 columns, darker groove beside it.
            d = (6, 2, -2, -7)[x % 4] + rng.randint(-3, 3)
            rows[y][x] = shade(DARK_STONE, d)
    # Gold leaf in patches, most of it flaked off to brown.
    patches = [(2, 2, 5, 4), (9, 1, 5, 3), (1, 9, 4, 5), (8, 8, 6, 5), (12, 13, 4, 3), (5, 14, 3, 2)]
    for i, (px, py, w, h) in enumerate(patches):
        for y in range(py, min(N, py + h)):
            for x in range(px, min(N, px + w)):
                # ragged edges
                if (x in (px, px + w - 1) or y in (py, py + h - 1)) and rng.random() < 0.45:
                    continue
                r = rng.random()
                if r < (0.16 if i % 3 == 1 else 0.07):
                    c = LEAF_LIGHT if r < 0.025 else LEAF
                else:
                    c = rng.choice(FLAKED)
                rows[y][x] = rgba(c)
    return rows


def gilding_top():
    rng = random.Random(29)
    rows = blank()
    for y in range(N):
        for x in range(N):
            ring = max(abs(x - 7.5), abs(y - 7.5))
            if ring > 7:
                c = shade(DARK_STONE, -8)
            elif ring > 5:
                c = rgba(LEAF if rng.random() < 0.2 else rng.choice(FLAKED))
                if ring > 6.5 and rng.random() < 0.5:
                    c = shade(DARK_STONE, 4)
            elif ring > 4:
                c = shade(DARK_STONE, -10)
            else:
                c = shade(DARK_STONE, 6 - int(ring * 2) + rng.randint(-3, 3))
            rows[y][x] = c
    return rows


# --- Ray-glazed Stone --------------------------------------------------------------------------------------

GLAZE = (112, 114, 122)


def ray_glazed():
    rng = random.Random(31)
    rows = blank()
    cx, cy = 10.5, 5.5
    for y in range(N):
        for x in range(N):
            # Wrap the swirl's distance so it tiles: nearest of the centre's 9 copies.
            r = min(math.hypot(x - cx - ox, y - cy - oy) for ox in (-N, 0, N) for oy in (-N, 0, N))
            swirl = math.sin(r * 1.35) * 5
            # A soft diagonal sheen band from top left to bottom right.
            band = (x + y) % 16
            sheen = 12 if band in (3, 4) else 6 if band in (2, 5) else 0
            d = int(round(swirl + sheen)) + rng.choice((0, 0, 0, 1, -1))
            rows[y][x] = shade(GLAZE, d)
    for (x, y) in ((4, 0), (3, 1), (13, 8), (12, 9)):
        rows[y][x] = shade(GLAZE, 26)
    return rows


# --- Collapsed Rubble --------------------------------------------------------------------------------------

DUST = (104, 100, 96)
CHUNKS = [
    # (x, y, pixels as rows of a mask, colour)
    (1, 1, ['.##.', '####', '###.'], (74, 74, 80)),
    (8, 0, ['###', '####', '.##'], (86, 86, 92)),
    (12, 5, ['.##', '###', '##.'], (66, 66, 72)),
    (3, 6, ['####.', '#####', '.###.'], (80, 80, 86)),
    (10, 10, ['.###', '####', '####', '.##.'], (72, 72, 78)),
    (1, 11, ['##.', '###', '.##'], (90, 88, 86)),
    (6, 12, ['##', '##'], (64, 64, 70)),
]


def solid(mask, x, y):
    return 0 <= y < len(mask) and 0 <= x < len(mask[y]) and mask[y][x] == '#'


def rubble():
    rng = random.Random(37)
    rows = blank()
    for y in range(N):
        for x in range(N):
            rows[y][x] = shade(DUST, rng.randint(-9, 7))
    for (ox, oy, mask, colour) in CHUNKS:
        for dy, line in enumerate(mask):
            for dx, ch in enumerate(line):
                if ch != '#':
                    continue
                x, y = (ox + dx) % N, (oy + dy) % N
                up = not solid(mask, dx, dy - 1)
                left = not solid(mask, dx - 1, dy)
                down = not solid(mask, dx, dy + 1)
                right = not solid(mask, dx + 1, dy)
                d = 14 if up or left else -12 if down or right else rng.randint(-3, 3)
                rows[y][x] = shade(colour, d)
    # shadows: dust pixels just below or right of a chunk's lower edge
    chunk_px = set()
    for (ox, oy, mask, _) in CHUNKS:
        for dy, line in enumerate(mask):
            for dx, ch in enumerate(line):
                if ch == '#':
                    chunk_px.add(((ox + dx) % N, (oy + dy) % N))
    for (x, y) in list(chunk_px):
        for (sx, sy) in ((x, (y + 1) % N), ((x + 1) % N, y)):
            if (sx, sy) not in chunk_px:
                rows[sy][sx] = shade(DUST, -24)
    return rows


# --- Petrified Remains -------------------------------------------------------------------------------------

PETRI = (138, 138, 134)


def petrified():
    rng = random.Random(43)
    rows = blank()
    for y in range(N):
        for x in range(N):
            rows[y][x] = shade(PETRI, rng.randint(-8, 6))
    # A few hairline cracks and spots of lichen-dark grime.
    for (x, y) in ((9, 7), (10, 8), (10, 9), (11, 10), (3, 12), (4, 13), (13, 14), (14, 13)):
        rows[y][x] = shade(PETRI, -38)
    for (x, y) in ((1, 9), (14, 6), (7, 15)):
        rows[y][x] = shade(PETRI, -20)
    # The face, top-left 6x4 (the head's north face): raised brows, wide hollow eyes, an open mouth.
    face = [
        'b....b',   # brows lifted in fear
        'ee..ee',   # wide eye sockets
        '..nn..',
        '..oo..',   # mouth open in a scream
    ]
    for y, line in enumerate(face):
        for x, ch in enumerate(line):
            base = shade(PETRI, 4)
            rows[y][x] = {'b': shade(PETRI, 22), 'e': shade(PETRI, -46), 'n': shade(PETRI, 14),
                          'o': shade(PETRI, -56)}.get(ch, base)
    rows[1][1] = shade(PETRI, -30)
    rows[1][4] = shade(PETRI, -30)
    return rows


# --- Eye-Stalk Growth --------------------------------------------------------------------------------------

STALK = (168, 92, 128)
STALK_DARK = (126, 64, 100)
STALK_LIGHT = (200, 128, 158)
LID = (96, 42, 76)
SCLERA = (232, 222, 206)
IRIS = (196, 120, 40)
PUPIL = (30, 18, 30)

# Each stalk: path of (x, y) from root to just under the bulb, and the bulb's top-left (4 wide, 4 high).
STALKS = [
    ([(3, 15), (3, 14), (3, 13), (2, 12), (2, 11), (2, 10)], (0, 6)),
    ([(9, 15), (9, 14), (9, 13), (9, 12), (10, 11), (10, 10), (10, 9), (10, 8), (10, 7), (10, 6), (10, 5)], (8, 1)),
    ([(13, 15), (13, 14), (14, 13), (14, 12), (14, 11)], (12, 7)),
    ([(6, 15), (6, 14), (6, 13)], (5, 9)),
]
BULB = [
    '.LS.',
    'LSSS',
    'DKKD',
    '.DD.',
]


def eye_stalks(open_eye=None):
    rows = blank()
    for i, (path, (bx, by)) in enumerate(STALKS):
        for j, (x, y) in enumerate(path):
            rows[y][x] = rgba(STALK if j % 3 else STALK_DARK)
            # a thicker, lit root
            if y >= 14 and x - 1 >= 0 and rows[y][x - 1][3] == 0:
                rows[y][x - 1] = rgba(STALK_LIGHT if y == 15 else STALK)
        # Bulb: lit top-left, a closed lid (dark line) across it.
        for dy, line in enumerate(BULB):
            for dx, ch in enumerate(line):
                c = {'L': STALK_LIGHT, 'S': STALK, 'D': STALK_DARK, 'K': LID}.get(ch)
                if c:
                    rows[by + dy][bx + dx] = rgba(c)
        if open_eye == i:
            rows[by + 1][bx + 1] = rgba(SCLERA)
            rows[by + 1][bx + 2] = rgba(IRIS)
            rows[by + 2][bx] = rgba(STALK_DARK)
            rows[by + 2][bx + 1] = rgba(SCLERA)
            rows[by + 2][bx + 2] = rgba(PUPIL)
            rows[by + 2][bx + 3] = rgba(STALK_DARK)
    return rows


def eye_stalk_frames():
    """Frame 0: all closed. Frames 1-2: the tall stalk's or the right one's eye open."""
    frames = [eye_stalks(), eye_stalks(1), eye_stalks(2)]
    return [row for frame in frames for row in frame]


EYE_MCMETA = {'animation': {'interpolate': False, 'frames': [
    {'index': 0, 'time': 140}, {'index': 1, 'time': 18}, {'index': 0, 'time': 90}, {'index': 2, 'time': 12}]}}


def main():
    os.makedirs(OUT, exist_ok=True)
    out = {
        'cracked_runic_deepslate_bricks': cracked_bricks(),
        'mossy_runic_deepslate_bricks': mossy_bricks(),
        'tarnished_gilding': gilding_side(),
        'tarnished_gilding_top': gilding_top(),
        'ray_glazed_stone': ray_glazed(),
        'collapsed_rubble': rubble(),
        'petrified_remains': petrified(),
    }
    for name, rows in out.items():
        pngio.write_png(os.path.join(OUT, name + '.png'), N, N, rows)
        print(f'wrote {name}.png')
    frames = eye_stalk_frames()
    pngio.write_png(os.path.join(OUT, 'eye_stalk_growth.png'), N, len(frames), frames)
    with open(os.path.join(OUT, 'eye_stalk_growth.png.mcmeta'), 'w') as fh:
        json.dump(EYE_MCMETA, fh, indent=2)
        fh.write('\n')
    print('wrote eye_stalk_growth.png (+ .mcmeta)')


if __name__ == '__main__':
    main()
