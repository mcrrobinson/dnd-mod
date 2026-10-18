"""Generates the Brimstone Sanctum building-block textures (Building Blocks epic).

    python3 tools/brimstone_block_textures.py

All 16x16 under textures/block/:
- brimstone, brimstone_top: dark basalt with bright yellow sulphur crystals crusted in its cracks, and a thicker
  crust on the top face. polished_brimstone: smooth dark stone with thin yellow veins. brimstone_bricks: dark
  bricks with yellow-crusted mortar. (Shapes share these, see tools/block_families.py.)
- hellbrass: one dark brass plate with an ember-red sheen and rivets at the corners; cut_hellbrass: the same
  plates in a 2x2 grid.
- pact_sigil_tile: polished blackstone engraved with a glowing red-orange summoning circle (a ring of infernal
  glyphs round a five-pointed star); 4 frames that pulse (pact_sigil_tile.png.mcmeta).
- infernal_script_panel: a framed blackstone panel carved with rows of burning red script (its front face).
- sulphur_vent_top: brimstone crust round a dark hole.
- contract_lectern_base/_front/_sides/_top: a blackstone lectern in vanilla's lectern layout; the top is a
  parchment with a red wax seal, the front has the scroll's tail hanging down it.
- soulfire_sconce: left half black iron, right half a soul flame (see models/block/soulfire_sconce.json).
- chain_hung_censer: brass censer in vanilla's lantern layout (body, lid, chain), red embers in its openwork.
- textures/item/soulfire_sconce.png and chain_hung_censer.png: their inventory sprites.
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

BASALT = (62, 60, 66)
BASALT_DARK = (40, 38, 44)
CRACK = (26, 24, 28)
SULPHUR = (232, 206, 64)
SULPHUR_LIGHT = (252, 238, 128)
SULPHUR_DARK = (178, 146, 34)
BLACKSTONE = (52, 46, 54)
BLACKSTONE_DARK = (34, 30, 36)
BLACKSTONE_LIGHT = (70, 64, 72)
BRASS = (104, 70, 38)
BRASS_LIGHT = (196, 146, 70)
BRASS_DARK = (40, 24, 16)
EMBER = (136, 40, 26)
GLOW = (255, 112, 36)
GLOW_HOT = (255, 196, 92)
GLOW_DIM = (196, 52, 26)


def px(c):
    return tuple(max(0, min(255, int(v))) for v in c[:3]) + (255,)


def shade(c, d):
    return px(tuple(v + d for v in c[:3]))


def mix(a, b, t):
    return px(tuple(a[i] + (b[i] - a[i]) * t for i in range(3)))


def blank(c=CLEAR):
    return [[c] * N for _ in range(N)]


def noise_fill(base, rng, spread=6):
    return [[shade(base, rng.randint(-spread, spread)) for _ in range(N)] for _ in range(N)]


# --- brimstone ------------------------------------------------------------------------------------------------

def crust(rows, x, y, rng, strong=False):
    """A sulphur crystal at x/y: bright on top, darker below."""
    if 0 <= x < N and 0 <= y < N:
        r = rng.random()
        rows[y][x] = px(SULPHUR_LIGHT if r < (0.4 if strong else 0.25) else SULPHUR if r < 0.8 else SULPHUR_DARK)


def brimstone_side():
    rng = random.Random(71)
    rows = blank()
    # Vertical strata like basalt's side, each column band its own tone.
    bands = [rng.choice((-6, -2, 0, 3, 6)) for _ in range(N)]
    for y in range(N):
        for x in range(N):
            rows[y][x] = shade(BASALT, bands[x] + rng.randint(-4, 4))
    # Cracks: short jagged lines, crusted with sulphur along one side.
    cracks = [[(3, 2), (4, 3), (4, 4), (5, 5), (5, 6)], [(11, 8), (10, 9), (10, 10), (11, 11), (12, 12)],
              [(1, 11), (2, 12), (3, 12), (4, 13)], [(13, 1), (13, 2), (14, 3)], [(7, 13), (8, 14), (8, 15)]]
    for line in cracks:
        for (x, y) in line:
            rows[y][x] = px(CRACK)
        for (x, y) in line:
            if rng.random() < 0.75:
                crust(rows, x + 1, y, rng)
            if rng.random() < 0.3:
                crust(rows, x - 1, y - 1, rng)
    # Light top edge of each stratum chunk, darker underside.
    for x in range(N):
        rows[0][x] = shade(rows[0][x], 6)
    return rows


def brimstone_top():
    rng = random.Random(72)
    rows = noise_fill(BASALT, rng, 5)
    # Basalt top rings, faintly.
    for y in range(N):
        for x in range(N):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if int(d) in (3, 6) and rng.random() < 0.6:
                rows[y][x] = shade(BASALT, -9 + rng.randint(-2, 2))
    # Thick crusts: a few blobs of crystals, bright centres and dark rims.
    for (cx, cy, r) in ((3, 4, 1.8), (11, 7, 1.4), (6, 12, 2.0), (14, 14, 1.0)):
        for y in range(N):
            for x in range(N):
                dx = min(abs(x - cx), N - abs(x - cx))
                dy = min(abs(y - cy), N - abs(y - cy))
                d = math.hypot(dx, dy) + rng.uniform(-0.4, 0.4)
                if d < r * 0.5:
                    rows[y][x] = px(SULPHUR_LIGHT if rng.random() < 0.3 else SULPHUR)
                elif d < r:
                    rows[y][x] = px(SULPHUR_DARK if rng.random() < 0.6 else SULPHUR)
                elif d < r + 0.7 and rng.random() < 0.5:
                    rows[y][x] = px(CRACK)
    return rows


def polished_brimstone():
    rng = random.Random(73)
    rows = noise_fill(BASALT_DARK, rng, 3)
    for i in range(N):
        rows[0][i] = shade(BASALT, 4)
        rows[i][0] = shade(BASALT, 2)
        rows[N - 1][i] = px(CRACK)
        rows[i][N - 1] = px(CRACK)
    # Thin veins wandering across, 1 px, bright where they cross the middle.
    for start in ((1, 4), (6, 14)):
        x, y = start
        for _ in range(22):
            if 1 <= x < N - 1 and 1 <= y < N - 1:
                rows[y][x] = px(SULPHUR if rng.random() < 0.7 else SULPHUR_DARK)
            x += 1
            y += rng.choice((-1, 0, 0, 1)) if start[1] < 8 else rng.choice((-1, -1, 0))
            y = max(1, min(N - 2, y))
    rows[5][9] = px(SULPHUR_LIGHT)
    return rows


def brimstone_bricks():
    rng = random.Random(74)
    rows = blank()
    for y in range(N):
        course = y // 4
        offset = 4 if course % 2 else 0
        for x in range(N):
            bx = (x + offset) % 8
            by = y % 4
            if by == 3 or bx == 7:
                # Mortar crusted with sulphur, broken by dark gaps.
                r = rng.random()
                rows[y][x] = px(SULPHUR if r < 0.1 else SULPHUR_DARK if r < 0.3 else CRACK)
                continue
            brick = ((x + offset) % N) // 8 + course * 2
            d = (-3, 4, 0, -5, 2, 6, -2, 1)[brick % 8] + rng.randint(-3, 3)
            if by == 0:
                d += 8
            elif by == 2:
                d -= 6
            rows[y][x] = shade(BASALT_DARK, d + 8)
    for (x, y) in ((2, 7), (13, 3), (9, 11), (5, 15)):
        rows[y][x] = px(SULPHUR_LIGHT)
    return rows


# --- hellbrass ------------------------------------------------------------------------------------------------

def plate(rows, x0, y0, size, rng):
    for y in range(size):
        for x in range(size):
            # A diagonal ember-red sheen across the plate.
            t = (x + y) / (2 * size)
            base = mix(BRASS, EMBER, 0.55 * max(0.0, 1 - abs(t - 0.55) * 3))
            d = rng.randint(-4, 4)
            if x == 0 or y == 0:
                d += 16
            elif x == size - 1 or y == size - 1:
                d -= 40
            rows[y0 + y][x0 + x] = shade(base, d)
    for (rx, ry) in ((1, 1), (size - 2, 1), (1, size - 2), (size - 2, size - 2)):
        rows[y0 + ry][x0 + rx] = px(BRASS_LIGHT)
        rows[y0 + ry + 1][x0 + rx] = px(BRASS_DARK)


def hellbrass():
    rng = random.Random(75)
    rows = blank()
    plate(rows, 0, 0, N, rng)
    return rows


def cut_hellbrass():
    rng = random.Random(76)
    rows = blank()
    for gy in (0, 8):
        for gx in (0, 8):
            plate(rows, gx, gy, 8, rng)
    return rows


# --- pact sigil -----------------------------------------------------------------------------------------------

def polished_blackstone(rng, frame=True):
    rows = noise_fill(BLACKSTONE, rng, 3)
    if frame:
        for i in range(N):
            rows[0][i] = shade(BLACKSTONE_LIGHT, 2)
            rows[i][0] = shade(BLACKSTONE_LIGHT, 0)
            rows[N - 1][i] = px(BLACKSTONE_DARK)
            rows[i][N - 1] = px(BLACKSTONE_DARK)
    return rows


def sigil_frame(brightness):
    rng = random.Random(77)
    rows = polished_blackstone(rng, frame=False)
    glow = mix(GLOW_DIM, GLOW, brightness)
    hot = mix(GLOW, GLOW_HOT, brightness)
    dim = mix(shade(GLOW_DIM, -60), GLOW_DIM, brightness)
    cx = cy = 7.5
    # Outer ring.
    for y in range(N):
        for x in range(N):
            d = math.hypot(x - cx, y - cy)
            if 6.6 <= d < 7.5:
                rows[y][x] = dim
    # Glyphs on a ring between the circle and the star: little marks at 8 points.
    glyphs = [(7, 1), (8, 1), (12, 3), (13, 4), (14, 7), (14, 8), (12, 12), (11, 13),
              (8, 14), (7, 14), (3, 12), (2, 11), (1, 8), (1, 7), (3, 3), (4, 2)]
    for (x, y) in glyphs:
        rows[y][x] = dim
    # Inner ring and a five-pointed star inside it.
    pts = []
    for k in range(5):
        a = -math.pi / 2 + k * 2 * math.pi / 5
        pts.append((cx + 5.2 * math.cos(a), cy + 0.4 + 5.2 * math.sin(a)))
    for k in range(5):
        (x1, y1), (x2, y2) = pts[k], pts[(k + 2) % 5]
        for s in range(40):
            t = s / 39
            x, y = int(round(x1 + (x2 - x1) * t)), int(round(y1 + (y2 - y1) * t))
            rows[y][x] = hot
    for (x, y) in pts:
        rows[int(round(y))][int(round(x))] = hot
    # Engraved edges: the stone next to each glowing line is darker.
    for y in range(N):
        for x in range(N):
            if rows[y][x] in (glow, hot, dim):
                continue
            if any(0 <= x + dx < N and 0 <= y + dy < N and rows[y + dy][x + dx] in (glow, hot, dim)
                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                rows[y][x] = shade(BLACKSTONE_DARK, -4)
    return rows


def sigil_tile():
    frames = [sigil_frame(b) for b in (1.0, 0.75, 0.5, 0.75)]
    return [row for f in frames for row in f]


# --- infernal script --------------------------------------------------------------------------------------------

def script_panel():
    rng = random.Random(78)
    rows = polished_blackstone(rng)
    for i in range(1, N - 1):
        rows[1][i] = px(BLACKSTONE_DARK)
        rows[N - 2][i] = shade(BLACKSTONE_LIGHT, -4)
        rows[i][1] = px(BLACKSTONE_DARK)
        rows[i][N - 2] = shade(BLACKSTONE_LIGHT, -4)
    # Four lines of joined-up script: a glowing baseline broken into words, with strokes rising from it.
    for y in (4, 7, 10, 13):
        x = 3
        while x < N - 3:
            word = rng.randint(2, 5)
            for i in range(min(word, N - 3 - x)):
                rows[y][x + i] = px(GLOW_DIM)
                r = rng.random()
                if r < 0.45:
                    rows[y - 1][x + i] = px(GLOW if rng.random() < 0.8 else GLOW_HOT)
            x += word + 1
    return rows


# --- sulphur vent --------------------------------------------------------------------------------------------

def vent_top():
    rng = random.Random(79)
    rows = noise_fill(BASALT, rng, 5)
    for y in range(N):
        for x in range(N):
            d = math.hypot(x - 7.5, y - 7.5) + rng.uniform(-0.5, 0.5)
            if d < 2.3:
                rows[y][x] = px((12, 10, 12)) if d < 1.6 else px(CRACK)
            elif d < 3.4:
                rows[y][x] = px(SULPHUR_LIGHT if rng.random() < 0.45 else SULPHUR)
            elif d < 5.0:
                r = rng.random()
                rows[y][x] = px(SULPHUR if r < 0.35 else SULPHUR_DARK if r < 0.6 else rows[y][x])
            elif d < 5.8 and rng.random() < 0.25:
                rows[y][x] = px(SULPHUR_DARK)
    return rows


# --- contract lectern ------------------------------------------------------------------------------------------

def lectern_stone(seed):
    rng = random.Random(seed)
    rows = noise_fill(BLACKSTONE, rng, 4)
    # Horizontal courses every 4 px, like polished blackstone bricks.
    for y in range(3, N, 4):
        for x in range(N):
            rows[y][x] = px(BLACKSTONE_DARK)
    return rows


PARCHMENT = (226, 208, 164)
PARCHMENT_DARK = (190, 168, 122)
INK = (70, 40, 30)
WAX = (178, 26, 30)
WAX_LIGHT = (222, 60, 56)


def lectern_top():
    # The slanted top's face uses uv 0,1-16,14 rotated 180: a parchment on a blackstone board.
    rng = random.Random(80)
    rows = lectern_stone(81)
    for y in range(2, 14):
        for x in range(2, 14):
            rows[y][x] = shade(PARCHMENT, rng.randint(-6, 4))
    for y in (2, 13):
        for x in range(2, 14):
            rows[y][x] = px(PARCHMENT_DARK)
    for y in (4, 6, 8):
        for x in range(4, 12):
            if rng.random() < 0.75:
                rows[y][x] = px(INK)
    # The seal, low on the sheet (the face is turned 180, so it shows nearest the reader).
    for (x, y) in ((7, 10), (8, 10), (6, 11), (7, 11), (8, 11), (9, 11), (7, 12), (8, 12)):
        rows[y][x] = px(WAX)
    rows[11][7] = px(WAX_LIGHT)
    return rows


def lectern_front():
    rows = lectern_stone(82)
    # The scroll's tail down the post's north face (uv 0-8 across), ending in a red ribbon.
    for y in range(0, 10):
        for x in (2, 3, 4, 5):
            rows[y][x] = px(PARCHMENT if x in (3, 4) else PARCHMENT_DARK)
    for x in (3, 4):
        rows[10][x] = px(WAX)
        rows[11][x] = px(WAX)
    rows[12][2] = px(WAX)
    rows[12][5] = px(WAX)
    for y in (2, 5, 7):
        rows[y][3] = px(INK)
    return rows


def lectern_base():
    rows = lectern_stone(83)
    for i in range(N):
        rows[0][i] = shade(BLACKSTONE_LIGHT, 4)
    return rows


def lectern_sides():
    return lectern_stone(84)


# --- lights ----------------------------------------------------------------------------------------------------

SOUL = (90, 222, 230)
SOUL_LIGHT = (196, 250, 250)
SOUL_DARK = (40, 140, 168)
IRON = (46, 44, 48)
IRON_LIGHT = (78, 76, 82)
IRON_DARK = (24, 22, 26)


def sconce():
    """Left half (x 0-7): black iron for the bracket's boxes. Right half (x 8-15): the soul flame, 8x12 at the
    top, transparent round it."""
    rng = random.Random(85)
    rows = blank()
    for y in range(N):
        for x in range(8):
            d = rng.randint(-4, 4)
            if y % 4 == 0:
                d += 14
            rows[y][x] = shade(IRON, d)
    flame = [
        '...##...',
        '...##...',
        '..#oo#..',
        '..#oo#..',
        '.#oWWo#.',
        '.#oWWo#.',
        '#oWWWWo#',
        '#oWWWWo#',
        '#ooWWoo#',
        '.#oooo#.',
        '..####..',
        '........',
    ]
    for y, line in enumerate(flame):
        for x, ch in enumerate(line):
            if ch == '#':
                rows[y][8 + x] = px(SOUL_DARK)
            elif ch == 'o':
                rows[y][8 + x] = px(SOUL)
            elif ch == 'W':
                rows[y][8 + x] = px(SOUL_LIGHT)
    return rows


def censer():
    """Vanilla lantern layout: body sides at uv 0,2-6,9, body top/bottom 0,9-6,15, lid sides 1,0-5,2, lid top
    1,10-5,14, chain 11,1-14,5 and 11,6-14,12 (the hanging chain's plane uses 11,6-14,12)."""
    rng = random.Random(86)
    rows = blank()

    def brass(d=0):
        return shade(BRASS, d + rng.randint(-5, 5))

    # Body sides: a brass band at top and bottom, openwork in the middle showing red embers.
    for y in range(2, 9):
        for x in range(0, 6):
            if y in (2, 8):
                rows[y][x] = brass(26 if y == 2 else -24)
            elif x in (0, 5):
                rows[y][x] = brass(10 if x == 0 else -14)
            elif (x + y) % 2 == 0 and 3 <= y <= 6:
                rows[y][x] = px(GLOW if rng.random() < 0.5 else EMBER)
            else:
                rows[y][x] = brass(-4)
    # Body top/bottom: a brass plate with a hot centre, and the lid on 1,10-5,14.
    for y in range(9, 15):
        for x in range(0, 6):
            rows[y][x] = brass(-10 if x in (0, 5) or y in (9, 14) else 0)
    for y in range(11, 13):
        for x in range(2, 4):
            rows[y][x] = px(GLOW_DIM)
    # Lid sides: a domed rim.
    for x in range(1, 5):
        rows[0][x] = brass(30)
        rows[1][x] = brass(4)
    # Chain: dark iron links like vanilla's.
    for (x0, y0, y1) in ((11, 1, 5), (11, 6, 12)):
        for y in range(y0, y1):
            if y % 2 == 0:
                rows[y][x0] = px(IRON_LIGHT)
                rows[y][x0 + 2] = px(IRON_LIGHT)
            else:
                rows[y][x0 + 1] = px(IRON_DARK)
                rows[y][x0] = px(IRON)
                rows[y][x0 + 2] = px(IRON)
    return rows


def sprite(art, palette):
    rows = blank()
    for y, line in enumerate(art):
        for x, ch in enumerate(line):
            if ch in palette:
                rows[y][x] = px(palette[ch])
    return rows


def sconce_item():
    return sprite([
        '................',
        '.........W......',
        '........WoW.....',
        '........oWo.....',
        '.......oWWWo....',
        '.......#oWo#....',
        '.......#ooo#....',
        '..II...IIIII....',
        '..Ii....III.....',
        '..IiiiiiiI......',
        '..Ii............',
        '..Ii............',
        '..Ii............',
        '..II............',
        '................',
        '................',
    ], {'W': SOUL_LIGHT, 'o': SOUL, '#': SOUL_DARK, 'I': IRON_LIGHT, 'i': IRON_DARK})


def censer_item():
    return sprite([
        '.......c........',
        '.......C........',
        '.......c........',
        '.......C........',
        '.......c........',
        '......lLl.......',
        '.....LLLLL......',
        '....dBBBBBd.....',
        '....BeBeBeB.....',
        '....dBgBgBd.....',
        '....BeBeBeB.....',
        '....dBBBBBd.....',
        '.....dBBBd......',
        '......ddd.......',
        '................',
        '................',
    ], {'c': IRON_LIGHT, 'C': IRON_DARK, 'L': BRASS_LIGHT, 'l': BRASS, 'B': BRASS, 'd': BRASS_DARK,
        'e': EMBER, 'g': GLOW})


# ----------------------------------------------------------------------------------------------------------------

ITEMS = {'soulfire_sconce': sconce_item, 'chain_hung_censer': censer_item}

TEXTURES = {
    'brimstone': brimstone_side,
    'brimstone_top': brimstone_top,
    'polished_brimstone': polished_brimstone,
    'brimstone_bricks': brimstone_bricks,
    'hellbrass': hellbrass,
    'cut_hellbrass': cut_hellbrass,
    'infernal_script_panel': script_panel,
    'sulphur_vent_top': vent_top,
    'contract_lectern_top': lectern_top,
    'contract_lectern_front': lectern_front,
    'contract_lectern_base': lectern_base,
    'contract_lectern_sides': lectern_sides,
    'soulfire_sconce': sconce,
    'chain_hung_censer': censer,
}


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, fn in TEXTURES.items():
        rows = fn()
        pngio.write_png(os.path.join(OUT, name + '.png'), N, len(rows), rows)
    item_dir = os.path.join(OUT, '..', 'item')
    for name, fn in ITEMS.items():
        pngio.write_png(os.path.join(item_dir, name + '.png'), N, N, fn())
    rows = sigil_tile()
    pngio.write_png(os.path.join(OUT, 'pact_sigil_tile.png'), N, len(rows), rows)
    with open(os.path.join(OUT, 'pact_sigil_tile.png.mcmeta'), 'w') as f:
        json.dump({'animation': {'frametime': 10, 'interpolate': True}}, f, indent=2)
        f.write('\n')
    print(f'wrote {len(TEXTURES) + 1} block and {len(ITEMS)} item textures')


if __name__ == '__main__':
    main()
