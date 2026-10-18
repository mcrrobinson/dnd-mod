"""Generates the Dragon Temple building-block textures (Building Blocks epic).

    python3 tools/temple_block_textures.py

All 16x16 under textures/block/ (animated ones are vertical strips with a .mcmeta):
- <ember|frost|storm>_dragonscale_tiles: overlapping rounded scales in offset rows, each with a light rim along
  its rounded edge and a dark shadow under the scales above it. One drawing, three palettes.
- temple_lacquer_planks: glossy red-lacquered boards with a subtle grain and black edge lines.
- <ember|frost|storm>_scorched_stone: basalt grey, with red glaze and glowing cracks (Ember), frost rime and pale
  blue cracks (Frost) or branching yellow-white scorch lines on darker stone (Storm).
- ancestor_relief_front / ancestor_relief_side: carved grey-black stone, the front with a dragon's head in
  profile and a gold eye.
- prayer_wheel_drum (still) / prayer_wheel_drum_spinning (8 frames, the script bands sliding round),
  prayer_wheel_cap: a red drum with gold script bands between brass caps.
- incense_brazier_bronze, <kind>_incense_brazier_embers (4 frames, flickering): a dark bronze bowl, embers
  glowing in the ancestry's colour.
- paper_lantern: an atlas for the paper lantern model (cream paper body, red caps, cord).
- sparring_post_side / _front / _top: straw wrapped round a post with rope bands, a padded cloth front.
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


def rgba(c):
    return tuple(max(0, min(255, int(v))) for v in c[:3]) + (255,)


def shade(c, d):
    return rgba(tuple(v + d for v in c[:3]))


def mix(a, b, t):
    return rgba(tuple(a[i] + (b[i] - a[i]) * t for i in range(3)))


def blank(c=(0, 0, 0, 0)):
    return [[c] * N for _ in range(N)]


def save(name, rows, height=N, frametime=None, interpolate=False):
    pngio.write_png(os.path.join(OUT, name + '.png'), N, height, rows)
    meta = os.path.join(OUT, name + '.png.mcmeta')
    if frametime:
        anim = {'frametime': frametime}
        if interpolate:
            anim['interpolate'] = True
        with open(meta, 'w') as fh:
            json.dump({'animation': anim}, fh, indent=2)
            fh.write('\n')
    elif os.path.exists(meta):
        os.remove(meta)
    print('wrote', name + '.png')


# --- Dragonscale Tiles ----------------------------------------------------------------------------------------

# rim (the scale's light rounded edge), body top, body bottom, shadow (under the scales above), highlight
SCALE_PALETTES = {
    'ember': ((250, 150, 60), (200, 62, 34), (150, 34, 26), (78, 16, 16), None),
    'frost': ((246, 252, 255), (182, 220, 242), (140, 188, 222), (82, 120, 168), None),
    'storm': ((84, 204, 196), (34, 140, 150), (26, 92, 124), (14, 38, 76), (242, 220, 84)),
}
SCALE_W = 8  # scales are 8 wide and hang 8 down; rows every 4, odd rows offset by 4


def scale_at(px, py):
    """(cx, top, e) of the scale on top at a pixel: rows higher up overlap the ones below (like shingles), so
    it's the one with the highest top whose hanging half-ellipse covers the point; e is 0 at its middle, 1 at
    its rounded edge."""
    for top in range(-16, 24, 4):
        if py < top:
            continue
        offset = 4 if (top // 4) % 2 else 0
        for cx in range(offset - 16, 40, SCALE_W):
            e = math.hypot((px - cx) / (SCALE_W / 2), (py - top) / 8.0)
            if e <= 1.0:
                return cx, top, e
    return None


def dragonscale(kind):
    rim, top_c, bottom_c, shadow, spark = SCALE_PALETTES[kind]
    rng = random.Random(7)
    rows = blank()
    for y in range(N):
        for x in range(N):
            px, py = x + 0.5, y + 0.5
            cx, top, e = scale_at(px, py)
            # Lighter where it comes out from under the scales above, darker down to its rim.
            t = max(0.0, min(1.0, (py - top - 3.0) / 5.0))
            c = mix(bottom_c, top_c, t)
            # The rounded rim catches the light...
            nxt = scale_at(px, py + 1.0)
            if nxt is None or nxt[:2] != (cx, top):
                c = rgba(rim)
            # ...and casts a shadow on the scale below it.
            above = scale_at(px, py - 1.0)
            if above is not None and above[:2] != (cx, top) and above[1] < top:
                c = rgba(shadow)
            rows[y][x] = shade(c, rng.randint(-4, 4))
    if spark:
        # A yellow glint on some scales' rims.
        for (x, y) in ((3, 6), (11, 2), (7, 10), (15, 14)):
            rows[y][x] = rgba(spark)
    return rows


# --- Temple Lacquer Planks ------------------------------------------------------------------------------------

def lacquer_planks():
    rng = random.Random(23)
    red = (168, 34, 30)
    rows = blank()
    for y in range(N):
        board = y // 4
        by = y % 4
        for x in range(N):
            # Boards are 4 tall; joints offset per board.
            joint = (x + board * 5) % 16 == 0
            if by == 3:
                rows[y][x] = rgba((28, 10, 10))
                continue
            if joint:
                rows[y][x] = rgba((70, 14, 14))
                continue
            d = (-6, 4, -2, 6)[board] + rng.randint(-3, 3)
            # Grain: long darker streaks running along the board.
            if by == 1 and (x + board * 3) % 8 < 5 or by == 2 and (x + board * 5) % 7 < 3:
                d -= 12
            if by == 0:
                d += 22  # the glossy lit top edge of each board
            rows[y][x] = shade(red, d)
    # Glints of gloss.
    for (x, y) in ((3, 0), (4, 0), (11, 4), (12, 4), (6, 8), (14, 12), (15, 12)):
        rows[y][x] = rgba((236, 120, 104))
    return rows


# --- Breath-Scorched Stone ------------------------------------------------------------------------------------

def basalt(seed, base=(92, 92, 98)):
    rng = random.Random(seed)
    rows = blank()
    for y in range(N):
        for x in range(N):
            d = rng.randint(-8, 8)
            if (x + 2 * y) % 7 == 0:
                d -= 6
            rows[y][x] = shade(base, d)
    return rows


def crack_path(rng, start, steps, branch=0.0):
    """A wandering 1-px crack: list of pixels; with `branch` chance it forks."""
    pts = []
    x, y = start
    for _ in range(steps):
        pts.append((x % N, y % N))
        r = rng.random()
        if r < 0.4:
            x += 1
        elif r < 0.8:
            y += 1
        else:
            x += 1
            y += 1
        if branch and rng.random() < branch:
            pts.extend(crack_path(rng, (x, y), 4))
    return pts


STORM_BOLT = [(5, 0), (7, 3), (5, 6), (9, 9), (8, 12), (11, 16)]
STORM_FORK_1 = [(7, 3), (11, 4), (13, 2)]
STORM_FORK_2 = [(5, 6), (2, 8), (1, 11)]
STORM_FORK_3 = [(9, 9), (13, 10), (15, 13)]


def scorched(kind):
    if kind == 'ember':
        rows = basalt(31, (86, 80, 82))
        rng = random.Random(5)
        # Red glaze pooled in patches.
        for (cx, cy, r) in ((4, 4, 3.2), (12, 11, 3.6), (13, 2, 2.0), (3, 13, 2.2)):
            for y in range(N):
                for x in range(N):
                    if math.hypot(x - cx, y - cy) < r:
                        rows[y][x] = shade((150, 44, 34), rng.randint(-10, 10))
        for p in crack_path(rng, (0, 6), 14) + crack_path(rng, (7, 0), 12) + crack_path(rng, (9, 9), 8):
            rows[p[1]][p[0]] = rgba((255, 140, 50))
        for p in crack_path(random.Random(9), (1, 10), 6):
            rows[p[1]][p[0]] = rgba((214, 70, 30))
    elif kind == 'frost':
        rows = basalt(32, (100, 104, 112))
        rng = random.Random(6)
        # Rime: pale speckles thickest round the cracks.
        cracks = crack_path(rng, (0, 3), 15) + crack_path(rng, (6, 8), 12) + crack_path(rng, (11, 0), 7)
        for (x, y) in cracks:
            for (dx, dy) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if rng.random() < 0.55:
                    rows[(y + dy) % N][(x + dx) % N] = rgba((214, 232, 244))
        for (x, y) in cracks:
            rows[y][x] = rgba((140, 200, 246))
        for _ in range(18):
            rows[rng.randrange(N)][rng.randrange(N)] = rgba((236, 246, 255))
    else:
        rows = basalt(33, (58, 58, 66))
        # Branching scorch lines (a bolt and its forks, wrapping round the edges), darkened round them.
        lines = []
        for path in (STORM_BOLT, STORM_FORK_1, STORM_FORK_2, STORM_FORK_3):
            for (x0, y0), (x1, y1) in zip(path, path[1:]):
                steps = max(abs(x1 - x0), abs(y1 - y0))
                for i in range(steps + 1):
                    lines.append(((x0 + (x1 - x0) * i // steps) % N, (y0 + (y1 - y0) * i // steps) % N))
        for (x, y) in lines:
            for (dx, dy) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                rows[(y + dy) % N][(x + dx) % N] = shade(rows[(y + dy) % N][(x + dx) % N], -18)
        for i, (x, y) in enumerate(lines):
            rows[y][x] = rgba((250, 240, 170) if i % 3 else (255, 255, 230))
    return rows


# --- Ancestor Relief ------------------------------------------------------------------------------------------

RELIEF_FRONT = [
    'BBBBBBBBBBBBBBBb',
    'Bsssssssssssshsb',
    'BssssssssssshHdb',
    'BsssssssssshHdsb',
    'BsssssssHHhhhdsb',
    'BsssssHHhhhhhdsb',
    'BsssHHhhhheEhdsb',
    'BsHHhhhhhhEEhdsb',
    'BHhnhhhhhhhhhhdb',
    'Bddhhhhhhhhhhhdb',
    'BstststdhhhhhHdb',
    'BssddddhhhhhHdsb',
    'BsssssdhhhhHdssb',
    'BssssssdhhhHdssb',
    'BssssssshhhHdssb',
    'bbbbbbbbbbbbbbbb',
]
RELIEF_COLOURS = {
    'B': (96, 94, 102),    # the frame's lit edge
    'b': (34, 33, 38),     # the frame's shadowed edge
    's': (46, 45, 52),     # the sunken ground
    'h': (78, 76, 84),     # the raised head
    'H': (108, 106, 114),  # the head's lit ridges
    'd': (30, 29, 34),     # the head's shadow on the ground
    'e': (240, 196, 70),   # gold eye
    'E': (186, 136, 40),
    'n': (24, 22, 26),     # nostril
    't': (196, 192, 184),  # teeth
}


def relief_front():
    rng = random.Random(3)
    rows = blank()
    for y, line in enumerate(RELIEF_FRONT):
        assert len(line) == N, (y, line)
        for x, ch in enumerate(line):
            c = RELIEF_COLOURS[ch]
            rows[y][x] = rgba(c) if ch in 'eEt' else shade(c, rng.randint(-3, 3))
    return rows


def relief_side():
    rng = random.Random(4)
    rows = blank()
    for y in range(N):
        for x in range(N):
            if y == 0 or x == 0:
                c = RELIEF_COLOURS['B']
            elif y == N - 1 or x == N - 1:
                c = RELIEF_COLOURS['b']
            elif y in (1, N - 2) or x in (1, N - 2):
                c = (60, 58, 66)
            else:
                c = (52, 51, 58)
                # A carved key-pattern band through the middle.
                if y in (6, 9) and 2 <= x <= 13 or x in (4, 8, 12) and 6 <= y <= 9:
                    c = (36, 35, 41)
            rows[y][x] = shade(c, rng.randint(-3, 3))
    return rows


# --- Prayer Wheel ---------------------------------------------------------------------------------------------

BRASS = (198, 152, 64)
BRASS_DARK = (138, 98, 40)
DRUM_RED = (164, 30, 28)
GOLD = (246, 204, 84)
# Glyphs of the prayer in the band, as columns of (row, ...) pixels; repeated round the drum.
GLYPHS = [(6, 7, 8, 9), (6, 8), (7, 9), (6, 7, 9), (), (7, 8), (6, 9), (6, 7, 8), (8, 9), ()]


def drum(shift):
    rows = blank()
    for y in range(N):
        for x in range(N):
            sx = (x + shift) % N
            if y in (0, 1, 14, 15):
                c = BRASS if y in (0, 15) else BRASS_DARK
            elif y in (3, 12):
                c = GOLD
            elif y in (4, 11):
                c = BRASS_DARK
            else:
                c = DRUM_RED
                glyph = GLYPHS[sx % len(GLYPHS)] if sx < 15 else ()
                if 5 <= y <= 10 and y in glyph:
                    c = GOLD
                # Cylinder shading: darker at the ends of each face.
            edge = min(x, N - 1 - x)
            d = -18 if edge <= 2 else (-8 if edge <= 4 else 6)
            rows[y][x] = shade(c, d)
    return rows


def prayer_wheel():
    save('prayer_wheel_drum', drum(0))
    frames = []
    for f in range(8):
        frames.extend(drum(f * 2))
    save('prayer_wheel_drum_spinning', frames, height=N * 8, frametime=2)
    # The caps: a brass disc with a darker ring and the axle's gold boss in the middle.
    rows = blank()
    for y in range(N):
        for x in range(N):
            d = math.hypot(x - 7.5, y - 7.5)
            c = BRASS
            if d < 1.6:
                c = GOLD
            elif d < 2.6:
                c = BRASS_DARK
            elif 4.6 < d < 5.6:
                c = BRASS_DARK
            elif y in (0, 1, 14, 15) or x in (0, 1, 14, 15):
                c = shade(BRASS, 16) if y in (0, 1) or x in (0, 1) else BRASS_DARK
            rows[y][x] = shade(c, 0)
    save('prayer_wheel_cap', rows)


# --- Incense Brazier ------------------------------------------------------------------------------------------

BRONZE = (66, 46, 34)


def bronze():
    """Dark cast bronze. Rows 0-1 are the rim's top (the rim elements), rows 7-10 the bowl's side with a lit
    lip at the top, rows 11-15 the legs; a narrow sheen and a few green-tinged pits."""
    rng = random.Random(8)
    rows = blank()
    for y in range(N):
        for x in range(N):
            sheen = max(0.0, 1.0 - abs(x - 5.5) / 2.5)
            c = mix(BRONZE, (150, 108, 64), sheen * 0.5)
            if y in (0, 7):
                c = rgba((196, 146, 74))  # the lit lip
            elif y in (1, 8):
                c = shade(c, 14)
            elif y == 10:
                c = shade(c, -20)
            c = shade(c, rng.randint(-3, 3))
            if rng.random() < 0.03:
                c = rgba((64, 80, 58))
            rows[y][x] = c
    return rows


# glow core, glow, ember dark, ash
EMBER_PALETTES = {
    'ember': ((255, 236, 140), (250, 120, 40), (150, 36, 20), (52, 34, 30)),
    'frost': ((250, 254, 255), (150, 210, 255), (60, 110, 180), (40, 46, 60)),
    'storm': ((240, 255, 220), (70, 220, 210), (20, 100, 130), (26, 40, 48)),
}


def embers(kind, frame):
    core, glow, dark, ash = EMBER_PALETTES[kind]
    rng = random.Random(11)
    flick = random.Random(100 + frame)
    rows = blank()
    for y in range(N):
        for x in range(N):
            # Coal lumps: a value field from a few overlapping blobs.
            v = 0.0
            for (cx, cy, r) in ((5, 5, 3.5), (10, 6, 3.0), (7, 10, 3.6), (11, 11, 2.6), (3, 11, 2.2)):
                v = max(v, 1.0 - math.hypot(x - cx, y - cy) / r)
            v += rng.uniform(-0.15, 0.15) + flick.uniform(-0.12, 0.12)
            if v > 0.7:
                c = core
            elif v > 0.4:
                c = glow
            elif v > 0.15:
                c = dark
            else:
                c = ash
            rows[y][x] = rgba(c)
    return rows


def brazier():
    save('incense_brazier_bronze', bronze())
    for kind in EMBER_PALETTES:
        frames = []
        for f in range(4):
            frames.extend(embers(kind, f))
        save(f'{kind}_incense_brazier_embers', frames, height=N * 4, frametime=6, interpolate=True)


# --- Paper Lantern --------------------------------------------------------------------------------------------

def paper_lantern():
    """Atlas: body side x0-8 y0-9; body end x8-16 y8-16; cap top x8-14 y0-6; cap side x8-14 y6-7; cord x14-15."""
    rows = blank((0, 0, 0, 0))
    paper = (246, 226, 176)
    for y in range(9):
        for x in range(8):
            # A warm glow from the middle, ribs every other row, tinted top and bottom.
            glow = 1.0 - min(1.0, math.hypot(x - 3.5, y - 4.0) / 5.0)
            c = mix((222, 176, 110), paper, glow)
            if y in (0, 8):
                c = shade(c, -26)
            elif y % 2 == 0:
                c = shade(c, -14)
            if glow > 0.75:
                c = mix(c, (255, 244, 200), 0.6)
            rows[y][x] = c
    for y in range(8, 16):
        for x in range(8, 16):
            rows[y][x] = rgba((236, 206, 150))
    for y in range(6):
        for x in range(8, 14):
            edge = x in (8, 13) or y in (0, 5)
            rows[y][x] = rgba((222, 176, 72) if edge else (186, 32, 30))
    for x in range(8, 14):
        rows[6][x] = rgba((150, 24, 24))
    for y in range(N):
        rows[y][14] = rgba((60, 40, 30))
        rows[y][15] = rgba((40, 26, 20))
    for y in range(9, 16):
        for x in range(8):
            rows[y][x] = rgba((186, 32, 30))
    return rows


# --- Sparring Post --------------------------------------------------------------------------------------------

STRAW = (206, 172, 82)
ROPE = (128, 96, 58)


def sparring_side():
    rng = random.Random(12)
    rows = blank()
    for y in range(N):
        for x in range(N):
            if y in (2, 3, 12, 13):
                # Rope bands, twisted.
                c = shade(ROPE, 14 if (x + y) % 3 == 0 else -6)
            else:
                # Straw wrapped round the post: diagonal bundles of strands.
                d = rng.randint(-10, 10)
                if (x + y // 2) % 4 == 0:
                    d -= 22
                elif (x + y // 2) % 4 == 1:
                    d += 14
                c = shade(STRAW, d)
            rows[y][x] = c
    return rows


def sparring_front():
    rng = random.Random(13)
    cloth = (176, 156, 120)
    rows = blank()
    for y in range(N):
        for x in range(N):
            # Quilted padding: a grid of stitched squares, each puffed up in the middle.
            qx, qy = x % 5, y % 5
            if qx == 4 or qy == 4:
                c = shade(cloth, -30)
            else:
                d = 12 - 6 * (abs(qx - 1.5) + abs(qy - 1.5))
                c = shade(cloth, d + rng.randint(-4, 4))
            rows[y][x] = c
    # A red target mark sewn on.
    for y in range(N):
        for x in range(N):
            d = math.hypot(x - 7.5, y - 7.5)
            if 2.2 < d < 3.4 or d < 1.0:
                rows[y][x] = rgba((170, 36, 30))
    return rows


def sparring_top():
    rows = blank()
    for y in range(N):
        for x in range(N):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 6.2:
                c = shade(STRAW, -18)
            else:
                ring = int(d) % 2
                c = (124, 88, 52) if ring else (148, 108, 64)
            rows[y][x] = rgba(c)
    return rows


def main():
    os.makedirs(OUT, exist_ok=True)
    for kind in SCALE_PALETTES:
        save(f'{kind}_dragonscale_tiles', dragonscale(kind))
        save(f'{kind}_scorched_stone', scorched(kind))
    save('temple_lacquer_planks', lacquer_planks())
    save('ancestor_relief_front', relief_front())
    save('ancestor_relief_side', relief_side())
    prayer_wheel()
    brazier()
    save('paper_lantern', paper_lantern())
    save('sparring_post_side', sparring_side())
    save('sparring_post_front', sparring_front())
    save('sparring_post_top', sparring_top())


if __name__ == '__main__':
    main()
