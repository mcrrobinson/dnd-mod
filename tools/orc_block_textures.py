"""Generates the Orc Stronghold building-block textures (Building Blocks epic).

    python3 tools/orc_block_textures.py

16x16 textures under textures/block/:
- tanned_hide, pale_tanned_hide, grey_tanned_hide: stitched leather panels with dark seams, stitch marks and a
  couple of pale scars, slightly uneven tone (brown, pale and grey).
- painted_hide: brown hide with the Bloodtusk mark in red ochre, a dripping handprint over two crossed tusks.
- bloodtusk_mud_bricks: mud bricks, some daubed with red ochre handprints and streaks.
- smithy_slag: glassy black-green lumps with rust-orange pockets.
- war_totem_front: dark oak bark with a carved snarling tusked face in red and white war paint (the sides and
  top use vanilla's dark oak log).
- sharpened_stake: dark oak stakes with pale cut points (see-through round the tips), bound with rope.
- tusk_rack: a wooden plaque with a horned skull and two curved tusks, see-through background.
- war_drum_top / war_drum_side / war_drum_bottom: a pale hide head with a red ring; barrel staves laced with
  rope; a plank bottom.
- hide_rug: brown fur with darker wavy stripes.
"""
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
OCHRE = (158, 44, 28)
OCHRE_DARK = (120, 32, 22)
IVORY = (232, 224, 198)
IVORY_SHADE = (196, 184, 152)
BARK = (60, 46, 28)
BARK_DARK = (42, 32, 19)
BARK_LIGHT = (78, 60, 36)
PALE_WOOD = (186, 158, 112)
PALE_WOOD_SHADE = (150, 122, 84)
ROPE = (184, 158, 108)
ROPE_DARK = (132, 108, 70)


def shade(c, d):
    return tuple(max(0, min(255, int(v + d))) for v in c[:3]) + (255,)


def mix(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3)) + (255,)


def blank(fill=CLEAR):
    return [[fill] * N for _ in range(N)]


# --- hides --------------------------------------------------------------------------------------------------

def seam_rows(x):
    """The two horizontal seams' rows at column x: a wobbly one mid-tile and one along the bottom."""
    return (7 + (1 if 4 <= x < 10 else 0), 15)


# Vertical seams: (top row, bottom row, column at the top, column at the bottom), each between two horizontals.
V_SEAMS = [(0, 6, 4, 5), (9, 14, 12, 11)]


def hide(base, seed, seams=True):
    rng = random.Random(seed)
    rows = blank()
    for y in range(N):
        for x in range(N):
            d = rng.randint(-4, 4)
            # Uneven tone: soft blotches, one panel a shade darker than its neighbours.
            d += 5 * math.sin((x + 2 * y) * 0.6 + seed) * math.cos((y - x) * 0.45 + seed)
            if (8 <= y < 15 and x < 12) or (y < 7 and x >= 5):
                d -= 4
            rows[y][x] = shade(base, d)
    if not seams:
        return rows
    seam = shade(base, -34)
    stitch = shade(base, -22)
    for x in range(N):
        for y in seam_rows(x):
            rows[y][x] = seam
            rows[(y + 1) % N][x] = shade(rows[(y + 1) % N][x], 9)
            # Cross stitches over the seam, every other pixel, alternating sides.
            if x % 2 == 0:
                rows[y - 1 if x % 4 == 0 else (y + 1) % N][x] = stitch
    for (y0, y1, x0, x1) in V_SEAMS:
        for y in range(y0, y1 + 1):
            x = x0 + round((x1 - x0) * (y - y0) / max(1, y1 - y0))
            rows[y][x] = seam
            rows[y][x + 1] = shade(rows[y][x + 1], 8)
            if y % 2 == 0:
                rows[y][x - 1 if y % 4 == 0 else x + 1] = stitch
    return rows


def scars(rows, base, marks):
    scar = shade(base, 42)
    for points in marks:
        for (x, y) in points:
            rows[y][x] = scar
        x, y = points[-1]
        rows[y][x] = shade(base, 30)


SCARS = [[(1, 2), (2, 3), (3, 3)], [(7, 11), (8, 11), (9, 12)], [(13, 2), (14, 3)]]


def tanned_hide(base, seed):
    rows = hide(base, seed)
    scars(rows, base, SCARS)
    return rows


HIDE_BROWN = (122, 84, 52)
HIDE_PALE = (192, 166, 128)
HIDE_GREY = (136, 132, 122)


def curve(rows, p0, p1, p2, color, tip_color=None, thick_until=0.45, steps=40):
    """A quadratic Bezier stroke from p0 (2 px thick for the first part) to p2 (1 px)."""
    for i in range(steps + 1):
        t = i / steps
        x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0]
        y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]
        c = tip_color if tip_color and t > 0.8 else color
        points = [(round(x), round(y))]
        if t < thick_until:
            points.append((round(x), round(y) - 1))
        for (px, py) in points:
            if 0 <= px < N and 0 <= py < N:
                rows[py][px] = c + (255,)


def painted_hide():
    rows = hide(HIDE_BROWN, 7, seams=False)
    seam = shade(HIDE_BROWN, -34)
    for i in range(N):
        rows[15][i] = seam
        rows[i][15] = seam
        if i % 2 == 0:
            rows[14][i] = shade(HIDE_BROWN, -20)
            rows[i][14] = shade(HIDE_BROWN, -20)
    # Two tusks crossing under the hand, thick at the root, curving up to their tips.
    curve(rows, (2, 15), (8, 13), (13, 8), OCHRE, OCHRE_DARK, thick_until=0.3)
    curve(rows, (13, 15), (7, 13), (2, 8), OCHRE, OCHRE_DARK, thick_until=0.3)
    # The handprint: a palm, four spread fingers and a thumb, two drips running down from it.
    hand = [(x, y) for x in range(5, 11) for y in range(4, 8)]
    hand += [(5, 1), (5, 2), (5, 3), (7, 0), (7, 1), (7, 2), (7, 3), (9, 1), (9, 2), (9, 3), (11, 2), (11, 3),
             (10, 3), (4, 5), (3, 4), (6, 8), (7, 8), (8, 8), (9, 8)]
    drips = [(6, 9), (6, 10), (9, 9), (9, 10), (9, 11)]
    for (x, y) in hand + drips:
        rows[y][x] = OCHRE + (255,)
    for (x, y) in [(8, 5), (6, 6), (9, 7), (6, 10), (9, 11)]:
        rows[y][x] = OCHRE_DARK + (255,)
    return rows


# --- mud bricks ---------------------------------------------------------------------------------------------

MUD = (140, 106, 80)
MUD_MORTAR = (94, 72, 56)


def bloodtusk_mud_bricks():
    rng = random.Random(11)
    rows = blank()
    tones = {}
    for y in range(N):
        course = y // 4
        offset = 4 if course % 2 else 0
        for x in range(N):
            bx = (x + offset) % 8
            by = y % 4
            if by == 3 or bx == 7:
                rows[y][x] = shade(MUD_MORTAR, rng.randint(-4, 3))
                continue
            brick = (course, ((x + offset) % N) // 8)
            tone = tones.setdefault(brick, rng.randint(-9, 9))
            d = tone + rng.randint(-5, 5)
            if by == 0:
                d += 9
            elif by == 2:
                d -= 7
            rows[y][x] = shade(MUD, d)
    # A red handprint on one brick, streaks smeared over two others.
    print_ = [(9, 5), (10, 5), (11, 5), (12, 5), (9, 6), (10, 6), (11, 6), (12, 6), (13, 6), (9, 4), (11, 4),
              (8, 6)]
    streaks = [(1, 9), (2, 9), (3, 10), (4, 10), (5, 9), (6, 9), (2, 13), (3, 13), (4, 13), (5, 14), (6, 14)]
    for (x, y) in print_:
        rows[y][x] = mix(rows[y][x], OCHRE, 0.85)
    for (x, y) in streaks:
        rows[y][x] = mix(rows[y][x], OCHRE, 0.6)
    rows[2][4] = mix(rows[2][4], OCHRE, 0.5)
    return rows


# --- slag ---------------------------------------------------------------------------------------------------

SLAG = (30, 38, 33)
RUST = (172, 86, 34)
RUST_DARK = (116, 56, 24)


def smithy_slag():
    rng = random.Random(5)
    seeds = [(rng.uniform(0, N), rng.uniform(0, N)) for _ in range(7)]

    def nearest(x, y):
        best = []
        for i, (sx, sy) in enumerate(seeds):
            for ox in (-N, 0, N):
                for oy in (-N, 0, N):
                    dx, dy = x + 0.5 - sx - ox, y + 0.5 - sy - oy
                    best.append((dx * dx + dy * dy, i, dx, dy))
        best.sort()
        return best[0], best[1]

    rows = blank()
    for y in range(N):
        for x in range(N):
            (d1, i, dx, dy), (d2, _, _, _) = nearest(x, y)
            edge = math.sqrt(d2) - math.sqrt(d1)
            if edge < 0.7:
                rows[y][x] = shade(SLAG, -16)
                continue
            # Each lump is lit from the top left: lighter there, dark on its lower right.
            light = -(dx + dy) * 4.0
            rows[y][x] = shade(mix(SLAG, (44, 64, 52), 0.3 if i % 2 else 0.0), light + rng.randint(-3, 3))
            if dx < -1.2 and dy < -1.2 and edge > 1.3 and rng.random() < 0.35:
                rows[y][x] = (96, 120, 104, 255)
    for (x, y) in [(3, 11), (4, 11), (3, 12), (12, 3), (13, 3), (13, 4), (9, 13)]:
        rows[y][x] = RUST + (255,)
    for (x, y) in [(4, 12), (12, 4), (8, 13), (2, 11)]:
        rows[y][x] = RUST_DARK + (255,)
    return rows


# --- wood: bark, totem face, stakes -------------------------------------------------------------------------

def bark(seed):
    rng = random.Random(seed)
    rows = blank()
    for y in range(N):
        for x in range(N):
            # Vertical furrows of dark oak bark.
            c = BARK_DARK if x % 5 == 2 or (x % 5 == 4 and rng.random() < 0.5) else BARK
            if x % 5 == 0:
                c = BARK_LIGHT
            rows[y][x] = shade(c, rng.randint(-4, 4))
    return rows


def war_totem_front():
    rows = bark(3)
    carved = (104, 80, 52)
    carved_dark = (78, 58, 36)
    # The carved face: a recessed panel of paler wood.
    for y in range(2, 15):
        for x in range(2, 14):
            if (y in (2, 14)) and x in (2, 13):
                continue
            rows[y][x] = shade(carved, (-6 if x > 10 or y > 12 else 0) + (5 if y == 2 or x == 2 else 0))
    for y in range(3, 15):
        rows[y][13] = shade(carved_dark, 0)
    for x in range(3, 13):
        rows[14][x] = shade(carved_dark, 0)
    white = (226, 222, 210, 255)
    red = (168, 32, 24, 255)
    dark = (26, 18, 12, 255)
    # Brow ridge in white paint, angry slanted eyes, red paint round them.
    for x in range(3, 13):
        rows[3][x] = white
    for (x, y) in [(3, 5), (4, 5), (5, 6), (6, 6), (12, 5), (11, 5), (10, 6), (9, 6)]:
        rows[y][x] = dark
    for (x, y) in [(3, 4), (4, 4), (5, 5), (6, 5), (12, 4), (11, 4), (10, 5), (9, 5), (3, 6), (4, 6), (11, 6),
                   (12, 6), (3, 7), (12, 7)]:
        rows[y][x] = red
    rows[5][5] = (230, 60, 30, 255)
    rows[5][10] = (230, 60, 30, 255)
    # Nose, red cheek stripes.
    for (x, y) in [(7, 6), (8, 6), (7, 7), (8, 7), (6, 8), (9, 8)]:
        rows[y][x] = shade(carved_dark, -6)
    for (x, y) in [(3, 9), (4, 9), (11, 9), (12, 9), (3, 10), (12, 10)]:
        rows[y][x] = red
    # The snarling mouth with teeth, and tusks rising from its corners.
    for x in range(5, 11):
        for y in (11, 12):
            rows[y][x] = dark
    for x in (6, 8, 9):
        rows[11][x] = white
    rows[12][7] = white
    for (x, y) in [(4, 12), (4, 11), (4, 10), (5, 9), (11, 12), (11, 11), (11, 10), (10, 9)]:
        rows[y][x] = IVORY + (255,)
    rows[12][4] = IVORY_SHADE + (255,)
    rows[12][11] = IVORY_SHADE + (255,)
    # White chin stripes.
    for x in (6, 9):
        rows[13][x] = white
    return rows


def sharpened_stake():
    rng = random.Random(9)
    rows = blank()
    for y in range(N):
        for x in range(N):
            s = x % 4
            c = BARK_LIGHT if s == 0 else BARK_DARK if s == 3 else BARK
            rows[y][x] = shade(c, rng.randint(-4, 4))
    for stake in range(4):
        x0 = stake * 4
        # The cut point: see-through outside the tip, pale wood on the cut, darker on its shaded side.
        rows[0][x0] = rows[0][x0 + 3] = CLEAR
        rows[0][x0 + 1] = PALE_WOOD + (255,)
        rows[0][x0 + 2] = PALE_WOOD_SHADE + (255,)
        rows[1][x0] = PALE_WOOD + (255,)
        rows[1][x0 + 1] = shade(PALE_WOOD, 10)
        rows[1][x0 + 2] = PALE_WOOD_SHADE + (255,)
        rows[1][x0 + 3] = shade(PALE_WOOD_SHADE, -16)
        rows[2][x0] = shade(PALE_WOOD, -6)
        rows[2][x0 + 1] = PALE_WOOD + (255,)
        rows[2][x0 + 2] = shade(PALE_WOOD_SHADE, -6)
        rows[3][x0 + 1] = shade(PALE_WOOD_SHADE, -20)
    # Rope binding: two twisted turns round the stakes.
    for y in (9, 10):
        for x in range(N):
            rows[y][x] = (ROPE if (x + y) % 3 else ROPE_DARK) + (255,)
    for x in range(N):
        rows[11][x] = shade(rows[11][x], -14)
    return rows


# --- tusk rack ----------------------------------------------------------------------------------------------

def tusk_rack():
    rows = blank()
    plank = (88, 60, 32)
    for y in range(2, 14):
        for x in range(4, 12):
            if y == 13 and x in (4, 11):
                continue
            d = 8 if y == 2 or x == 4 else -10 if y == 13 or x == 11 else 0
            d += -8 if y in (5, 9) else 0  # plank joins
            rows[y][x] = shade(plank, d + ((x * 7 + y * 3) % 5) - 2)
    bone = (224, 216, 192)
    bone_shade = (178, 168, 142)
    horn = (112, 94, 76)
    horn_dark = (84, 70, 56)
    # Horns curving out and up from the top of the skull.
    for (x, y) in [(5, 4), (4, 3), (3, 2), (2, 2), (1, 1), (10, 4), (11, 3), (12, 2), (13, 2), (14, 1)]:
        rows[y][x] = horn + (255,)
    for (x, y) in [(4, 4), (3, 3), (11, 4), (12, 3), (1, 0), (14, 0)]:
        rows[y][x] = horn_dark + (255,)
    # The skull.
    for y in range(4, 11):
        for x in range(5, 11):
            if y >= 9 and x in (5, 10):
                continue
            rows[y][x] = (bone if x < 9 and y < 8 else bone_shade) + (255,)
    dark = (40, 30, 24, 255)
    rows[6][6] = rows[6][9] = dark
    rows[7][6] = rows[7][9] = dark
    rows[8][7] = rows[8][8] = dark
    for x in (6, 8):
        rows[10][x] = dark
    # Two big curved tusks under the skull, sweeping out and up.
    curve(rows, (5, 14), (0, 14), (2, 4), IVORY, IVORY_SHADE, thick_until=0.55)
    curve(rows, (10, 14), (15, 14), (13, 4), IVORY, IVORY_SHADE, thick_until=0.55)
    return rows


# --- war drum -----------------------------------------------------------------------------------------------

DRUM_WOOD = (112, 78, 44)


def war_drum_top():
    rng = random.Random(4)
    rows = blank()
    for y in range(N):
        for x in range(N):
            r = math.hypot(x - 7.5, y - 7.5)
            if x in (0, 15) or y in (0, 15):
                rows[y][x] = shade(DRUM_WOOD, -10 + rng.randint(-4, 4))
            elif 4.6 <= r <= 5.8:
                rows[y][x] = shade(OCHRE, rng.randint(-6, 6))
            else:
                # The head: worn darker in the middle where it's struck, lighter at the edge.
                rows[y][x] = shade(HIDE_PALE, -int(10 - r * 1.5) + rng.randint(-4, 4))
    return rows


def war_drum_side():
    rng = random.Random(6)
    rows = blank()
    for y in range(N):
        for x in range(N):
            stave = x // 4
            d = (-4, 6, 0, -8)[stave] + rng.randint(-4, 4)
            if x % 4 == 3:
                d -= 18
            rows[y][x] = shade(DRUM_WOOD, d)
    for x in range(N):
        # The head's hide folded over the top, a dark hoop at the bottom.
        rows[0][x] = shade(HIDE_PALE, 6)
        rows[1][x] = shade(HIDE_PALE, -16)
        rows[14][x] = shade(BARK_DARK, 6)
        rows[15][x] = shade(BARK_DARK, -4)
    # Rope lacing in V's from the head down to the hoop.
    for x in range(N):
        t = x % 8
        y_down = 2 + int(t * 11 / 7)
        y_up = 13 - int(t * 11 / 7)
        for y in (y_down, y_up):
            rows[y][x] = (ROPE if y < 8 else ROPE_DARK) + (255,)
    return rows


def war_drum_bottom():
    rng = random.Random(8)
    rows = blank()
    for y in range(N):
        for x in range(N):
            d = (-4, 4, -2, 6)[y // 4] + rng.randint(-4, 4)
            if y % 4 == 3:
                d -= 18
            rows[y][x] = shade(DRUM_WOOD, d - 8)
    return rows


# --- rug ----------------------------------------------------------------------------------------------------

def hide_rug():
    rng = random.Random(2)
    fur = (114, 78, 46)
    stripe = (66, 44, 26)
    rows = blank()
    for y in range(N):
        for x in range(N):
            wave = 2.0 * math.sin(x * 2 * math.pi / N)
            band = (y + wave) % 8
            c = stripe if 5.2 <= band < 7.4 else fur
            # Short fur strands: alternate light and dark pixels along the stripes.
            d = rng.randint(-8, 8) + (6 if (x + y) % 3 == 0 else 0)
            rows[y][x] = shade(c, d)
    return rows


TEXTURES = {
    'tanned_hide': lambda: tanned_hide(HIDE_BROWN, 1),
    'pale_tanned_hide': lambda: tanned_hide(HIDE_PALE, 2),
    'grey_tanned_hide': lambda: tanned_hide(HIDE_GREY, 3),
    'painted_hide': painted_hide,
    'bloodtusk_mud_bricks': bloodtusk_mud_bricks,
    'smithy_slag': smithy_slag,
    'war_totem_front': war_totem_front,
    'sharpened_stake': sharpened_stake,
    'tusk_rack': tusk_rack,
    'war_drum_top': war_drum_top,
    'war_drum_side': war_drum_side,
    'war_drum_bottom': war_drum_bottom,
    'hide_rug': hide_rug,
}


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, draw in TEXTURES.items():
        pngio.write_png(os.path.join(OUT, name + '.png'), N, N, draw())
    print(f'wrote {len(TEXTURES)} textures')


if __name__ == '__main__':
    main()
