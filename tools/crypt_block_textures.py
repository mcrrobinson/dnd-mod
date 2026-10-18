"""Generates the Crypt building-block textures (Building Blocks: Crypt dungeons).

    python3 tools/crypt_block_textures.py

16x16 textures under textures/block/ (models from tools/crypt_blocks.py and tools/block_families.py):
- ossuary_bricks: grey stone bricks with a long bone and a skull set into the mortar
- funerary_relief: a carved stone panel with a shrouded figure, hands folded
- burial_niche_wall, _inside, _shroud: the niche's carved frame, its shadowy recess, the wrapped body's linen
- sarcophagus_side, _end, _head_top, _foot_top: a dark stone coffin, its lid carved with a knight's effigy
  (head and shoulders on the head half, the legs and the sword's point on the foot half)
- grave_candle_cluster (a ragged puddle of white wax, cut out) and grave_candle_cluster_stub (the stubs)
- lich_sigil_floor (animated: a slow pulse of the green runes), lich_sigil_floor_side
- grave_moss: pale, sickly green moss with grey threads (also the carpet)
- phylactery_pedestal_side, _top: black stone with silver bands and a green rune on top
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/block')

N = 16
CLEAR = (0, 0, 0, 0)

STONE = (124, 124, 124)
MORTAR = (86, 85, 86)
BONE = (212, 206, 184)
BONE_SHADE = (178, 171, 148)
BONE_DARK = (140, 134, 114)
SOCKET = (40, 36, 34)


def c(rgb, d=0, a=255):
    return tuple(max(0, min(255, int(v + d))) for v in rgb[:3]) + (a,)


def grid(fill=CLEAR):
    return [[fill] * N for _ in range(N)]


def stone_bricks(rng, base=STONE, mortar=MORTAR):
    """Vanilla-like stone bricks: four courses 3 high over a mortar row, 8-wide bricks, every other course
    offset by 4, each brick with its own tone and a 1-px bevel (light top/left, dark bottom/right)."""
    rows = grid()
    for y in range(N):
        course, by = divmod(y, 4)
        offset = 4 if course % 2 else 0
        for x in range(N):
            bx = (x + offset) % 8
            if by == 3 or bx == 7:
                rows[y][x] = c(mortar, rng.randint(-4, 4))
                continue
            brick = ((x + offset) % N) // 8 + course * 2
            d = (-5, 4, 8, -3, 2, -7, 5, 0)[brick % 8] + rng.randint(-6, 6)
            if by == 0 or bx == 0:
                d += 14
            elif by == 2 or bx == 6:
                d -= 10
            rows[y][x] = c(base, d)
    return rows


def put(rows, x, y, col):
    if 0 <= x < N and 0 <= y < N:
        rows[y][x] = col


# --- ossuary bricks ------------------------------------------------------------------------------------------

SKULL = [  # 5x5: B bone, s shade, o socket
    '.BBB.',
    'BBBBs',
    'BoBos',
    'sBoBs',
    '.sss.',
]


def skull(rows, ox, oy):
    for dy, line in enumerate(SKULL):
        for dx, ch in enumerate(line):
            if ch == 'B':
                put(rows, ox + dx, oy + dy, c(BONE, 6 if dy == 0 else 0))
            elif ch == 's':
                put(rows, ox + dx, oy + dy, c(BONE_SHADE))
            elif ch == 'o':
                put(rows, ox + dx, oy + dy, c(SOCKET))
    # the skull sits in a socket of dark mortar: shadow under and right of it
    for dx in range(1, 6):
        put(rows, ox + dx, oy + 5, c(MORTAR, -26))
    for dy in range(1, 5):
        put(rows, ox + 5, oy + dy, c(MORTAR, -20))


def long_bone(rows, x0, x1, y):
    """A bone lying along row y..y+1 from x0 to x1, with knuckled ends."""
    for x in range(x0, x1 + 1):
        put(rows, x, y, c(BONE, 8))
        put(rows, x, y + 1, c(BONE_SHADE))
        put(rows, x, y + 2, c(MORTAR, -24))  # shadow in the mortar under it
    for x in (x0, x1):
        put(rows, x, y - 1, c(BONE, 4))
        put(rows, x, y + 2, c(BONE_DARK))
    put(rows, x0 - 1, y, c(BONE_SHADE))
    put(rows, x0 - 1, y + 1, c(BONE_DARK))
    put(rows, x1 + 1, y, c(BONE_SHADE))
    put(rows, x1 + 1, y + 1, c(BONE_DARK))


def ossuary_bricks():
    rng = random.Random(7)
    rows = stone_bricks(rng)
    # a long bone along the mortar between the first and second course, a shorter one lower down
    long_bone(rows, 2, 12, 3)
    long_bone(rows, 10, 14, 11)
    # a skull set into the bricks of the third course, and a tiny one in the bottom corner
    skull(rows, 2, 8)
    return rows


# --- funerary relief -----------------------------------------------------------------------------------------

PALE = (150, 148, 144)


def funerary_relief():
    rng = random.Random(11)
    rows = grid()
    for y in range(N):
        for x in range(N):
            ring = min(x, y, N - 1 - x, N - 1 - y)
            if ring == 0:
                rows[y][x] = c(MORTAR, rng.randint(-3, 3))
            elif ring == 1:
                # raised frame: lit from the top left
                d = 18 if (x == 1 or y == 1) else -14
                rows[y][x] = c(PALE, d + rng.randint(-3, 3))
            else:
                # the sunken field, a little darker than the frame
                rows[y][x] = c(PALE, -48 + rng.randint(-4, 4))
    figure = [  # x from 4, y from 2; H hood, F face shadow, R robe, h hands, f fold, S shoulder light
        '...HHHH...',
        '..HHHHHH..',
        '..HHFFHH..',
        '..HFFFFH..',
        '...HFFH...',
        '.SRRRRRRR.',
        '.SRRhhRRR.',
        '.SRhhhhRR.',
        '.SRRhhRfR.',
        '..SRRRfR..',
        '..SRfRRR..',
        '..SRfRfR..',
    ]
    for dy, line in enumerate(figure):
        for dx, ch in enumerate(line):
            x, y = 3 + dx, 2 + dy
            if ch == '.':
                continue
            col = {'H': c(PALE, 6), 'F': c(PALE, -58), 'R': c(PALE, 0), 'h': c(PALE, 26), 'f': c(PALE, -20),
                   'S': c(PALE, 20)}[ch]
            rows[y][x] = col
            # carved depth: a shadow cast to the right and below onto the field
            if x + 1 < N - 2 and line[dx + 1:dx + 2] in ('.', ''):
                rows[y][x + 1] = c(PALE, -52)
    return rows


# --- burial niche ----------------------------------------------------------------------------------------------

def burial_niche_wall():
    """The front of the niche: only the frame shows (rows 0-3 lintel, 13-15 sill, columns 0-1 and 14-15 jambs)."""
    rng = random.Random(13)
    rows = stone_bricks(rng, base=(130, 128, 126))
    for y in range(N):
        for x in range(N):
            # the lintel: one long carved stone with an inscription line
            if y <= 3:
                d = 16 if y == 0 else -12 if y == 3 else 0
                rows[y][x] = c(PALE, d + rng.randint(-4, 4))
            # the sill
            if y >= 13:
                d = 14 if y == 13 else -14 if y == 15 else -4
                rows[y][x] = c(PALE, d + rng.randint(-4, 4))
    for x in range(3, 13):
        if x % 3 != 2:
            rows[1][x] = c(PALE, -46)
    # the jambs: an upright stone each side, bevelled towards the opening
    for y in range(4, 13):
        for x in (0, 1, 14, 15):
            d = 12 if x == 0 else -16 if x == 15 else 0
            rows[y][x] = c(PALE, d - 4 + rng.randint(-4, 4))
        rows[y][1] = c(PALE, -20)
        rows[y][14] = c(PALE, 6)
    return rows


def burial_niche_wall_inside():
    rng = random.Random(17)
    rows = grid()
    for y in range(N):
        for x in range(N):
            rows[y][x] = c((70, 68, 68), rng.randint(-7, 7) + (-6 if (x + y * 3) % 7 == 0 else 0))
    return rows


def burial_niche_wall_shroud():
    """Old linen bound with darker strips every few pixels (vertical, so they run across the body)."""
    rng = random.Random(19)
    linen = (196, 186, 160)
    rows = grid()
    for y in range(N):
        for x in range(N):
            d = rng.randint(-6, 6)
            if x % 4 == 0:
                d -= 34  # binding
            elif x % 4 == 1:
                d += 8
            if y % 5 == 4:
                d -= 8  # a fold
            rows[y][x] = c(linen, d)
    return rows


# --- sarcophagus ---------------------------------------------------------------------------------------------

LID = (92, 92, 98)
CARVE = (128, 128, 132)


def coffin_side(rng, motif):
    """Rows as the models' auto UV lays them: 2-4 the lid's edge, 5-13 the body, 14-15 the plinth."""
    rows = grid()
    for y in range(N):
        for x in range(N):
            n = rng.randint(-4, 4)
            if y <= 4:
                d = 14 if y <= 2 else 0 if y == 3 else -18
                rows[y][x] = c(LID, d + n)
            elif y <= 13:
                d = -10
                inner = 2 <= x <= 13 and 6 <= y <= 12
                if inner:
                    d = -22
                    if y == 6 or x == 2:
                        d = -32  # the panel's sunken top/left edge in shadow
                elif y == 5:
                    d = -28  # under the lid's overhang
                rows[y][x] = c(LID, d + n)
            else:
                d = 6 if y == 14 else -16
                rows[y][x] = c(LID, d - 8 + n)
    motif(rows)
    return rows


def side_motif(rows):
    # a row of three small carved crosses in the panel
    for cx in (4, 8, 11):
        for y in range(7, 12):
            rows[y][cx] = c(CARVE, -8 if y == 11 else 0)
        rows[8][cx - 1] = c(CARVE, 0)
        rows[8][cx + 1] = c(CARVE, -10)


def end_motif(rows):
    # a carved shield with a cross on it
    shield = ['.SSSSSS.', '.SSXSSS.', '.SXXXSS.', '.SSXSSS.', '..SXSS..', '...SS...']
    for dy, line in enumerate(shield):
        for dx, ch in enumerate(line):
            if ch == 'S':
                rows[7 + dy][4 + dx] = c(CARVE, -6)
            elif ch == 'X':
                rows[7 + dy][4 + dx] = c(CARVE, 22)


EFFIGY = {'H': (150, 150, 156), 'h': (118, 118, 124), 'v': (52, 52, 58), 'A': (140, 140, 146),
          'a': (108, 108, 114), 'T': (132, 132, 138), 'G': (165, 160, 140), 'g': (130, 124, 106),
          'B': (176, 176, 184), 'b': (140, 140, 150)}


def lid_top(rng, figure, border_rows):
    rows = grid()
    for y in range(N):
        for x in range(N):
            d = rng.randint(-4, 4)
            if x == 0 or y in border_rows[0]:
                d += 14  # bevelled edge, lit
            elif x == N - 1 or y in border_rows[1]:
                d -= 16
            rows[y][x] = c(LID, d)
    for dy, line in enumerate(figure):
        for dx, ch in enumerate(line):
            if ch == '.':
                continue
            x, y = 2 + dx, dy
            rows[y][x] = c(EFFIGY[ch], rng.randint(-3, 3))
            # carved depth: shadow to the right where the figure ends
            if dx + 1 >= len(line) or line[dx + 1] == '.':
                if x + 1 < N - 1:
                    rows[y][x + 1] = c(LID, -26)
    return rows


# The knight's effigy, 12 wide from x=2. Head half: helm with its visor slit, pauldrons, breastplate, the
# hands folded on the sword's hilt. Foot half: the blade down between the legs, greaves and sabatons.
HEAD_FIGURE = [
    '............',
    '............',
    '....HHHH....',
    '...HHHHHh...',
    '...HvvvvH...',
    '...HHHHHh...',
    '....hHHh....',
    '.AAAATTAAAA.',
    '.AaTTTTTTaA.',
    '..aTTGGTTa..',
    '..aTgGGgTa..',
    '..aTGGGGTa..',
    '..aTTbbTTa..',
    '...TTBBTT...',
    '...TTBBTT...',
    '...TTBBTT...',
]
FOOT_FIGURE = [
    '...TTBBTT...',
    '...TTBBTT...',
    '...TTBBTT...',
    '...AABBAA...',
    '...AaBBaA...',
    '...AaBBaA...',
    '...AabbaA...',
    '...Aa..aA...',
    '...Aa..aA...',
    '...Aa..aA...',
    '..AAa..aAA..',
    '..AAA..AAA..',
    '............',
    '............',
    '............',
    '............',
]


# --- grave candle cluster ------------------------------------------------------------------------------------

WAX = (234, 230, 214)


def grave_candle_cluster():
    """The puddle of wax under the stubs: a ragged blob, cut out."""
    rng = random.Random(23)
    rows = grid()
    lobes = [(8, 8, 5.6), (4.5, 6, 2.6), (11.5, 5, 2.4), (11, 11.5, 2.8), (5, 11.5, 2.6), (8, 3, 2.0)]
    for y in range(N):
        for x in range(N):
            inside = max((r - math.hypot(x + 0.5 - cx, y + 0.5 - cy)) for cx, cy, r in lobes)
            if inside > 0:
                d = rng.randint(-5, 5)
                if inside < 1.0:
                    d -= 22  # the thin rim, where the wax runs out over the stone
                elif rng.random() < 0.15:
                    d -= 12
                rows[y][x] = c(WAX, d)
    return rows


def grave_candle_cluster_stub():
    """Columns 0-13: the stubs' sides (each stub shows a 2-px strip, its top in rows 16-h), with runs of
    melted wax down them. (14-15, 0-1): a stub's top. (14, 3): the wick."""
    rng = random.Random(29)
    rows = grid()
    for y in range(N):
        for x in range(N):
            d = rng.randint(-4, 4)
            if x % 2 == 1:
                d -= 10  # the shaded side of each 2-px stub
            rows[y][x] = c(WAX, d)
    # drips: short darker runs from the tops of the shorter and taller stubs
    for x, y0, length in [(0, 9, 3), (3, 11, 2), (5, 8, 4), (8, 10, 3), (11, 9, 2), (12, 11, 3), (1, 12, 2)]:
        for y in range(y0, y0 + length):
            rows[y][x] = c(WAX, -18)
        rows[y0 + length][x] = c(WAX, -26)
    rows[0][14], rows[0][15], rows[1][14], rows[1][15] = c(WAX, -8), c(WAX, -2), c(WAX, -2), c(WAX, -14)
    rows[3][14] = (34, 30, 28, 255)
    return rows


# --- lich sigil floor ----------------------------------------------------------------------------------------

DARK = (44, 44, 50)
GLOW = (96, 236, 116)
HALO = (46, 112, 62)

# runes just inside the ring, at the four quarters (offsets from the middle, 7.5, 7.5)
SIGIL_RUNES = [(-1, -4), (0, -4), (0, -3), (4, -1), (3, 0), (4, 0), (-1, 4), (0, 4), (-1, 3), (-4, 0), (-4, -1),
               (-3, -1)]


def sigil_pixels():
    """(x, y) -> 1.0 for the runes' bright pixels, 0.4 for their glow on the stone round them."""
    lit = {}
    for y in range(N):
        for x in range(N):
            r = math.hypot(x - 7.5, y - 7.5)
            if abs(r - 6.3) <= 0.5:
                lit[(x, y)] = 1.0
    # a gap every so often in the ring, as if worn
    for gap in [(13, 6), (2, 9), (9, 13)]:
        lit.pop(gap, None)
    for dx, dy in SIGIL_RUNES:
        lit[(int(7.5 + dx + 0.5) if dx >= 0 else int(7.5 + dx), int(7.5 + dy + 0.5) if dy >= 0 else int(7.5 + dy))] = 1.0
    # a small eye of the Lich in the middle
    for p in [(7, 7), (8, 7), (7, 8), (8, 8), (6, 7), (9, 8)]:
        lit[p] = 1.0
    glow = {}
    for (x, y) in lit:
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < N and 0 <= ny < N and (nx, ny) not in lit:
                glow[(nx, ny)] = 0.4
    glow.update(lit)
    return glow


def dark_stone(rng, base=DARK):
    rows = grid()
    for y in range(N):
        for x in range(N):
            d = rng.randint(-5, 5)
            if x == 0 or y == 0:
                d += 8
            elif x == N - 1 or y == N - 1:
                d -= 8
            if (x * 7 + y * 3) % 11 == 0:
                d -= 6
            rows[y][x] = c(base, d)
    return rows


def lich_sigil_floor_frames():
    """Four frames: the runes pulse from full glow down to dim and back (interpolated in game)."""
    pix = sigil_pixels()
    frames = []
    for strength in (1.0, 0.8, 0.6, 0.8):
        rows = dark_stone(random.Random(31))
        for (x, y), k in pix.items():
            base = rows[y][x]
            target = GLOW if k == 1.0 else HALO
            t = strength * (1.0 if k == 1.0 else 0.45)
            rows[y][x] = tuple(int(base[i] + (target[i] - base[i]) * t) for i in range(3)) + (255,)
        frames.append(rows)
    return frames


def lich_sigil_floor_side():
    rows = dark_stone(random.Random(37))
    # a faint green seam where the runes' light bleeds over the edge
    for x in range(N):
        if x % 5 != 3:
            rows[0][x] = c(HALO, -10)
    return rows


# --- grave moss ----------------------------------------------------------------------------------------------

def grave_moss():
    rng = random.Random(41)
    tones = [(138, 150, 104), (124, 138, 92), (150, 162, 116), (110, 122, 82), (160, 170, 128)]
    rows = grid()
    # clumps: each pixel takes the tone of the nearest of a few seeded points (tiles: distances wrap)
    seeds = [(rng.uniform(0, N), rng.uniform(0, N), rng.randrange(len(tones))) for _ in range(14)]
    for y in range(N):
        for x in range(N):
            def dist(s):
                dx = min(abs(x - s[0]), N - abs(x - s[0]))
                dy = min(abs(y - s[1]), N - abs(y - s[1]))
                return dx * dx + dy * dy
            near = min(seeds, key=dist)
            rows[y][x] = c(tones[near[2]], rng.randint(-5, 5))
    # grey threads winding through it (wrapping, so the texture tiles)
    for start, length, step in [((1, 2), 12, (1, 0)), ((9, 6), 10, (0, 1)), ((3, 11), 11, (1, 0)),
                                ((13, 12), 6, (0, 1))]:
        x, y = start
        for i in range(length):
            rows[y % N][x % N] = c((156, 156, 150), rng.randint(-8, 8))
            x += step[0]
            y += step[1]
            if rng.random() < 0.4:
                if step[0]:
                    y += rng.choice((-1, 1))
                else:
                    x += rng.choice((-1, 1))
    return rows


# --- phylactery pedestal -------------------------------------------------------------------------------------

BLACK = (46, 42, 52)
SILVER = (190, 194, 204)


def silver_band(rows, y):
    for x in range(N):
        rows[y][x] = c(SILVER, 24 if x % 6 == 1 else -30 if x % 6 == 4 else 0)


def phylactery_pedestal_side():
    """Auto UV rows: 0-2 the cap, 3-12 the shaft, 13-15 the base."""
    rng = random.Random(43)
    rows = grid()
    for y in range(N):
        for x in range(N):
            d = rng.randint(-4, 4)
            if y in (0, 13):
                d += 14
            elif y in (2, 15):
                d -= 14
            elif y == 3:
                d -= 18  # under the cap
            rows[y][x] = c(BLACK, d)
    silver_band(rows, 1)
    silver_band(rows, 4)
    silver_band(rows, 11)
    silver_band(rows, 14)
    # a dim green rune on the shaft's face
    for x, y in [(7, 6), (8, 6), (7, 7), (7, 8), (8, 8), (8, 9), (6, 7), (9, 9)]:
        rows[y][x] = c(HALO, 20)
    return rows


def phylactery_pedestal_top():
    """The cap's top is u/v 2-14 of this: black stone ringed in silver, a glowing green rune in the middle."""
    rng = random.Random(47)
    rows = grid()
    for y in range(N):
        for x in range(N):
            rows[y][x] = c(BLACK, rng.randint(-4, 4))
            ring = min(x - 2, y - 2, 13 - x, 13 - y)
            if ring == 0:
                rows[y][x] = c(SILVER, 16 if (x == 2 or y == 2) else -24)
    rune = [(7, 5), (8, 5), (6, 6), (9, 6), (6, 7), (7, 7), (8, 7), (9, 7), (7, 8), (8, 8), (7, 9), (8, 9), (6, 10),
            (9, 10)]
    for x, y in rune:
        rows[y][x] = c(GLOW)
    for x, y in rune:
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if (nx, ny) not in rune and 3 <= nx <= 12 and 3 <= ny <= 12:
                rows[ny][nx] = c(HALO)
    return rows


def main():
    os.makedirs(OUT, exist_ok=True)
    rng = random.Random(53)
    textures = {
        'ossuary_bricks': ossuary_bricks(),
        'funerary_relief': funerary_relief(),
        'burial_niche_wall': burial_niche_wall(),
        'burial_niche_wall_inside': burial_niche_wall_inside(),
        'burial_niche_wall_shroud': burial_niche_wall_shroud(),
        'sarcophagus_side': coffin_side(random.Random(59), side_motif),
        'sarcophagus_end': coffin_side(random.Random(61), end_motif),
        'sarcophagus_head_top': lid_top(rng, HEAD_FIGURE, ((0,), ())),
        'sarcophagus_foot_top': lid_top(rng, FOOT_FIGURE, ((), (N - 1,))),
        'grave_candle_cluster': grave_candle_cluster(),
        'grave_candle_cluster_stub': grave_candle_cluster_stub(),
        'lich_sigil_floor_side': lich_sigil_floor_side(),
        'grave_moss': grave_moss(),
        'phylactery_pedestal_side': phylactery_pedestal_side(),
        'phylactery_pedestal_top': phylactery_pedestal_top(),
    }
    for name, rows in textures.items():
        pngio.write_png(os.path.join(OUT, name + '.png'), N, N, rows)
    frames = lich_sigil_floor_frames()
    pngio.write_png(os.path.join(OUT, 'lich_sigil_floor.png'), N, N * len(frames), [r for f in frames for r in f])
    with open(os.path.join(OUT, 'lich_sigil_floor.png.mcmeta'), 'w') as fh:
        json.dump({'animation': {'frametime': 12, 'interpolate': True}}, fh, indent=2)
        fh.write('\n')
    print(f'wrote {len(textures) + 1} textures')


if __name__ == '__main__':
    main()
