"""Generates the Cogsdeep building-block textures (Building Blocks epic, card "Building blocks: Cogsdeep").

    python3 tools/cogsdeep_block_textures.py

Writes textures/block/ (16x16, animated ones as vertical strips with a .mcmeta):
- riveted_brass.png: four warm brass plates, rivets along their edges, a scuffed diagonal sheen.
- cut_riveted_brass.png: the same brass cut into eight small plates, a rivet at each plate's ends.
- gnomish_bricks.png: small, rounded pale-grey bricks, 4 by 4 (16 to a face, twice vanilla's).
- cogwork_panel.png (8 frames): a brass panel with a cut-out where two meshing cogs turn.
- gearwall.png (8 frames): one big four-spoked gear filling the face, turning slowly.
- pipework.png / pipework_joint.png: the copper pipe and its riveted brass joint.
- glowcrystal_{red,blue,green,amber}.png: a faceted gem in a brass setting.
- clockface_0..11.png: a cream clock face in a brass rim, the hour hand at that hour, the minute hand at twelve.
- blueprint_table_{top,side,front}.png: a wooden table, a blue blueprint pinned on top, brass drafting tools.
- telescope_lens{,_front,_back}.png: a brass tube, its glass lens and its back plate.
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
BRASS = (204, 164, 74)
BRASS_LIGHT = (236, 202, 112)
BRASS_DARK = (156, 116, 46)
BRASS_DEEP = (112, 80, 32)
COPPER = (190, 104, 72)
COPPER_LIGHT = (224, 140, 100)
COPPER_DARK = (146, 74, 52)
HOLLOW = (40, 32, 26)


def shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)


def rgba(c):
    return tuple(c[:3]) + (255,)


def blank(c=(0, 0, 0)):
    return [[rgba(c) for _ in range(N)] for _ in range(N)]


def write(name, rows, frametime=None):
    height = len(rows)
    pngio.write_png(os.path.join(OUT, name + '.png'), N, height, rows)
    meta = os.path.join(OUT, name + '.png.mcmeta')
    if frametime is not None:
        with open(meta, 'w') as f:
            f.write('{\n  "animation": {\n    "frametime": %d\n  }\n}\n' % frametime)
    print('wrote', name + '.png', '(animated)' if frametime else '')


def rivet(rows, x, y):
    """A 1-px rivet head: bright, with a dark shadow below and right."""
    rows[y][x] = rgba((250, 228, 150))
    if y + 1 < N:
        rows[y + 1][x] = shade(rows[y + 1][x], -48)
    if x + 1 < N:
        rows[y][x + 1] = shade(rows[y][x + 1], -36)
    if x + 1 < N and y + 1 < N:
        rows[y + 1][x + 1] = shade(rows[y + 1][x + 1], -20)


def plates(rng, cells, sheen=True):
    """Brass plates over the cells (x0, y0, w, h): bevel light top/left, dark bottom/right, scuffs, sheen."""
    rows = blank()
    for (x0, y0, w, h) in cells:
        tone = rng.randint(-6, 6)
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                d = tone + rng.randint(-4, 4)
                if x == x0 or y == y0:
                    d += 22
                elif x == x0 + w - 1 or y == y0 + h - 1:
                    d -= 44
                if sheen and 9 <= (x + y) % 16 <= 11:
                    d += 10
                rows[y][x] = shade(BRASS, d)
        # A scuff or two: short darker strokes.
        for _ in range(2):
            sx, sy = x0 + rng.randint(1, w - 3), y0 + rng.randint(1, h - 2)
            for i in range(rng.randint(1, 2)):
                if sx + i < x0 + w - 1:
                    rows[sy][sx + i] = shade(rows[sy][sx + i], -14)
    return rows


def riveted_brass():
    rng = random.Random(7)
    rows = plates(rng, [(0, 0, 8, 8), (8, 0, 8, 8), (0, 8, 8, 8), (8, 8, 8, 8)])
    for (x0, y0) in [(0, 0), (8, 0), (0, 8), (8, 8)]:
        for (dx, dy) in [(1, 1), (5, 1), (1, 5), (5, 5)]:
            rivet(rows, x0 + dx, y0 + dy)
    return rows


def cut_riveted_brass():
    rng = random.Random(8)
    rows = plates(rng, [(x, y, 8, 4) for y in range(0, N, 4) for x in (0, 8)])
    for y0 in range(0, N, 4):
        for x0 in (0, 8):
            rivet(rows, x0 + 1, y0 + 1)
            rivet(rows, x0 + 5, y0 + 1)
    return rows


def gnomish_bricks():
    rng = random.Random(12)
    stone = (178, 176, 170)
    mortar = (118, 116, 112)
    rows = blank()
    for y in range(N):
        course = y // 4
        off = 2 if course % 2 else 0
        for x in range(N):
            bx, by = (x + off) % 4, y % 4
            if bx == 3 or by == 3:
                rows[y][x] = rgba(mortar)
                continue
            brick = ((x + off) % N) // 4 + course * 4
            tone = (brick * 37) % 11 - 5
            d = tone + rng.randint(-3, 3)
            corner = bx in (0, 2) and by in (0, 2)
            if corner:
                # Rounded corners: halfway to the mortar.
                rows[y][x] = shade(stone, -26 if (bx, by) != (0, 0) else -14)
                continue
            if by == 0 or bx == 0:
                d += 10
            elif by == 2 or bx == 2:
                d -= 10
            rows[y][x] = shade(stone, d)
    return rows


def coverage(fn, x, y, ss=4):
    hits = 0
    for i in range(ss):
        for j in range(ss):
            if fn(x + (i + 0.5) / ss, y + (j + 0.5) / ss):
                hits += 1
    return hits / (ss * ss)


def gear_fn(cx, cy, outer, root, teeth, angle, holes=0, hole_r=0.0, hole_at=0.0):
    """Inside test for a gear centred on (cx, cy) (pixel units), turned by angle (radians): a disk out to root,
    teeth out to outer, and {@code holes} round lightening holes of radius hole_r at radius hole_at."""
    pitch = 2 * math.pi / teeth

    def inside(px, py):
        dx, dy = px - cx, py - cy
        r = math.hypot(dx, dy)
        a = math.atan2(dy, dx) - angle
        for i in range(holes):
            ha = angle + i * 2 * math.pi / holes
            if math.hypot(dx - math.cos(ha) * hole_at, dy - math.sin(ha) * hole_at) < hole_r:
                return False
        if r <= root:
            return True
        if r > outer:
            return False
        off = (a % pitch) / pitch
        return 0.22 <= off <= 0.72

    return inside


def draw_gear(rows, inside, cx, cy, root, base, light, dark, clip=(0, 0, N, N), axle=1.0):
    x0, y0, x1, y1 = clip
    for y in range(y0, y1):
        for x in range(x0, x1):
            if coverage(inside, x, y) < 0.5:
                continue
            r = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r < axle:
                c = HOLLOW
            elif r < axle + 1.0:
                c = light
            elif r > root - 0.9:
                # The rim and teeth: lit on the top-left, shadowed bottom-right.
                c = light if (x + 0.5 - cx) + (y + 0.5 - cy) < 0 else dark
            else:
                c = base
            rows[y][x] = rgba(c)


def cogwork_panel():
    frames = []
    for f in range(8):
        rows = blank()
        # The panel: a one-pixel brass frame with a dark cut-out.
        for y in range(N):
            for x in range(N):
                edge = min(x, y, N - 1 - x, N - 1 - y)
                if edge >= 1:
                    rows[y][x] = rgba(HOLLOW) if (x * 3 + y) % 7 else shade(HOLLOW, 10)
                else:
                    d = 18 if (x == 0 or y == 0) and x != N - 1 and y != N - 1 else -30
                    rows[y][x] = shade(BRASS, d)
        for i in range(1, N - 1):
            rows[1][i] = shade(rows[1][i], -12)
            rows[i][1] = shade(rows[i][1], -12)
        # Two meshing cogs: the big one (8 teeth) turns clockwise, the small one (6 teeth) the other way, each one
        # tooth over the 8 frames, so the strip loops.
        t = f / 8
        clip = (1, 1, N - 1, N - 1)
        small = gear_fn(10.7, 10.7, 3.9, 2.8, 6, -t * 2 * math.pi / 6 + 0.35)
        draw_gear(rows, small, 10.7, 10.7, 2.8, COPPER, COPPER_LIGHT, COPPER_DARK, clip, axle=0.8)
        big = gear_fn(5.4, 5.4, 4.7, 3.5, 8, t * 2 * math.pi / 8)
        draw_gear(rows, big, 5.4, 5.4, 3.5, BRASS, BRASS_LIGHT, BRASS_DARK, clip, axle=0.9)
        frames.extend(rows)
    return frames


def gearwall():
    frames = []
    for f in range(8):
        rows = blank()
        # The housing behind the gear: dark iron with brass corner bolts.
        for y in range(N):
            for x in range(N):
                rows[y][x] = shade((58, 52, 46), ((x * 5 + y * 11) % 7) - 3)
        for (x, y) in [(0, 0), (14, 0), (0, 14), (14, 14)]:
            rivet(rows, x, y)
        # A big gear, 8 teeth and 4 lightening holes, a quarter turn over the 8 frames (the holes repeat each
        # quarter, the teeth each eighth).
        a = f / 8 * math.pi / 2
        gear = gear_fn(8.0, 8.0, 7.5, 5.7, 8, a, holes=4, hole_r=1.3, hole_at=3.2)
        draw_gear(rows, gear, 8.0, 8.0, 5.7, BRASS, BRASS_LIGHT, BRASS_DARK, axle=1.2)
        frames.extend(rows)
    return frames


def pipework():
    rng = random.Random(3)
    rows = blank()
    for y in range(N):
        for x in range(N):
            rows[y][x] = shade(COPPER, rng.randint(-6, 6))
    # A few patina specks.
    for _ in range(5):
        x, y = rng.randrange(N), rng.randrange(N)
        rows[y][x] = rgba((96, 150, 126))
    return rows


def pipework_joint():
    rng = random.Random(4)
    rows = blank()
    for y in range(N):
        for x in range(N):
            # The joint cube uses the middle 6x6 (5..10): bevel that, the rest is plain brass.
            d = rng.randint(-4, 4)
            if x == 5 or y == 5:
                d += 16
            elif x == 10 or y == 10:
                d -= 28
            rows[y][x] = shade(BRASS, d)
    rivet(rows, 6, 6)
    rivet(rows, 8, 8)
    return rows


GEMS = {
    'red': (196, 34, 44),
    'blue': (40, 92, 214),
    'green': (36, 172, 74),
    'amber': (236, 142, 24),
}


def glowcrystal(colour):
    rows = blank()
    # The brass setting.
    for y in range(N):
        for x in range(N):
            edge = min(x, y, N - 1 - x, N - 1 - y)
            d = 14 if edge == 0 and (x == 0 or y == 0) else -26 if edge == 0 else -8 if edge == 2 else 0
            rows[y][x] = shade(BRASS, d + ((x * 3 + y * 7) % 5) - 2)
    for (x, y) in [(1, 1), (13, 1), (1, 13), (13, 13)]:
        rivet(rows, x, y)
    # The gem: an octagon cut with a flat table in the middle and four facets round it.
    cx = cy = 7.5
    for y in range(N):
        for x in range(N):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            if abs(dx) + abs(dy) > 7.0 or abs(dx) > 5.5 or abs(dy) > 5.5:
                continue
            if abs(dx) <= 2.5 and abs(dy) <= 2.5:
                d = 40  # the table
            elif dy < 0 and abs(dy) >= abs(dx):
                d = 60  # top facet, catching the light
            elif dx < 0 and abs(dx) > abs(dy):
                d = 20
            elif dx > 0 and abs(dx) > abs(dy):
                d = -30
            else:
                d = -60  # bottom facet
            rows[y][x] = shade(colour, d)
    # Facet edges and a glint.
    for i in range(3, 6):
        rows[i][i] = shade(colour, 90)
    rows[5][6] = rgba((255, 255, 255))
    rows[6][5] = rgba((255, 250, 240))
    return rows


def clockface(hour):
    cream = (236, 226, 196)
    rows = blank()
    cx = cy = 8.0
    for y in range(N):
        for x in range(N):
            r = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r > 7.2:
                # Corners: brass plate.
                rows[y][x] = shade(BRASS_DARK, ((x + y) % 3) * 4)
            elif r > 6.0:
                d = 18 if (x + y) < 15 else -18
                rows[y][x] = shade(BRASS, d)
            else:
                rows[y][x] = shade(cream, -6 if r > 5.2 else 0)
    # Hour marks.
    for h in range(12):
        a = h * math.pi / 6
        mx = int(math.floor(cx + math.sin(a) * 5.3))
        my = int(math.floor(cy - math.cos(a) * 5.3))
        rows[my][mx] = rgba((70, 60, 50)) if h % 3 == 0 else rgba((150, 136, 112))

    def hand(angle, length, colour):
        steps = int(length * 4)
        for i in range(1, steps + 1):
            t = i / 4
            x = int(math.floor(cx + math.sin(angle) * t))
            y = int(math.floor(cy - math.cos(angle) * t))
            rows[y][x] = rgba(colour)

    hand(0.0, 5.0, (60, 52, 46))                      # the minute hand at twelve
    hand(hour * math.pi / 6, 3.4, (26, 22, 20))       # the hour hand
    for (x, y) in [(7, 7), (8, 7), (7, 8), (8, 8)]:
        rows[y][x] = rgba(BRASS_DEEP)
    rows[7][7] = rgba(BRASS_LIGHT)
    return rows


WOOD = (150, 108, 62)
WOOD_DARK = (102, 72, 40)


def planks(rows, rng, y0, y1):
    for y in range(y0, y1):
        for x in range(N):
            d = rng.randint(-5, 5) + (8 if y == y0 else -14 if y == y1 - 1 else 0)
            if x % 8 == 7 and (y // 4) % 2 == 0:
                d -= 18
            rows[y][x] = shade(WOOD, d)


def blueprint_table_top():
    rng = random.Random(21)
    rows = blank()
    planks(rows, rng, 0, N)
    for y in range(N):
        for x in range(N):
            rows[y][x] = shade(WOOD, rng.randint(-5, 5) + (-12 if y % 4 == 3 else 0))
    # The blueprint: blue paper with a pale grid and the outline of a gear.
    paper = (44, 86, 160)
    for y in range(2, 13):
        for x in range(2, 14):
            line = (x - 2) % 4 == 0 or (y - 2) % 4 == 0
            rows[y][x] = shade(paper, 18 if line else rng.randint(-3, 3))
    # The drawing: a gear (a ring with four teeth and an axle) and a dimension line under it.
    ink = (222, 234, 250)
    for y in range(2, 13):
        for x in range(2, 14):
            r = math.hypot(x + 0.5 - 7.0, y + 0.5 - 7.0)
            if 2.3 <= r <= 3.3:
                rows[y][x] = rgba(ink)
    for (x, y) in [(6, 2), (6, 11), (2, 6), (10, 6), (6, 6)]:
        rows[y][x] = rgba(ink)
    for x in range(3, 11):
        rows[11][x] = rgba(ink) if x in (3, 10) or x % 2 else rows[11][x]
    for y in range(4, 10):
        rows[y][12] = rgba(ink)
    # A curled corner and brass pins.
    rows[12][13] = shade(paper, -30)
    for (x, y) in [(2, 2), (13, 2), (2, 12)]:
        rows[y][x] = rgba(BRASS_LIGHT)
    # Drafting tools: a brass rule along the bottom edge and a pair of dividers.
    for x in range(3, 13):
        rows[14][x] = rgba(BRASS_LIGHT if x % 3 else BRASS_DARK)
    for i in range(4):
        rows[3 + i][11 + i // 2] = rgba(BRASS_DARK)
        rows[3 + i][12 + i // 2 - (i % 2)] = rgba(BRASS)
    return rows


def blueprint_table_side(front=False):
    rng = random.Random(22 if front else 23)
    rows = blank(HOLLOW)
    planks(rows, rng, 0, 4)
    # Legs at both ends, a stretcher low down, shadow between.
    for y in range(4, N):
        for x in range(N):
            if x <= 2 or x >= 13:
                rows[y][x] = shade(WOOD, (8 if x in (0, 13) else -10) + rng.randint(-4, 4))
            elif 11 <= y <= 12:
                rows[y][x] = shade(WOOD_DARK, rng.randint(-4, 4) + (8 if y == 11 else -6))
            else:
                rows[y][x] = shade(HOLLOW, rng.randint(-3, 3))
    if front:
        # A drawer under the top with a brass pull, and a set square hanging in the shadow.
        for y in range(4, 8):
            for x in range(3, 13):
                rows[y][x] = shade(WOOD, (8 if y == 4 else -16 if y == 7 else 0) + rng.randint(-3, 3))
        rows[5][7] = rgba(BRASS_LIGHT)
        rows[5][8] = rgba(BRASS_DARK)
        for i in range(3):
            rows[9][5 + i] = rgba(BRASS)
            rows[9 - i][5] = rgba(BRASS_LIGHT)
    return rows


def telescope_tube():
    rows = blank()
    # The tube's side (the model maps it along the tube): brass with two raised bands.
    for y in range(N):
        for x in range(N):
            d = ((x * 3 + y * 5) % 5) - 2
            if x in (1, 2, 12, 13):
                d += 18 if x in (1, 12) else -20
            rows[y][x] = shade(BRASS, d)
    return rows


def telescope_front():
    rows = blank()
    cx = cy = 8.0
    for y in range(N):
        for x in range(N):
            r = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r > 3.6:
                d = 16 if x + y < 15 else -24
                if r < 4.6:
                    d += 20 if x + y < 15 else -10
                rows[y][x] = shade(BRASS, d)
            else:
                # Glass: deep blue-green, bright at the top-left.
                d = int((cx - x) * 6 + (cy - y) * 6)
                rows[y][x] = shade((96, 150, 170), d)
    rows[6][7] = rgba((240, 250, 255))
    rows[7][6] = rgba((210, 236, 245))
    return rows


def telescope_back():
    rows = blank()
    for y in range(N):
        for x in range(N):
            r = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            rows[y][x] = shade(BRASS_DARK if r < 4.4 else BRASS, -20 if r < 2.0 else 0)
    return rows


def main():
    os.makedirs(OUT, exist_ok=True)
    write('riveted_brass', riveted_brass())
    write('cut_riveted_brass', cut_riveted_brass())
    write('gnomish_bricks', gnomish_bricks())
    write('cogwork_panel', cogwork_panel(), frametime=2)
    write('gearwall', gearwall(), frametime=5)
    write('pipework', pipework())
    write('pipework_joint', pipework_joint())
    for name, colour in GEMS.items():
        write('glowcrystal_' + name, glowcrystal(colour))
    for hour in range(12):
        write('clockface_%d' % hour, clockface(hour))
    write('blueprint_table_top', blueprint_table_top())
    write('blueprint_table_side', blueprint_table_side())
    write('blueprint_table_front', blueprint_table_side(front=True))
    write('telescope_lens', telescope_tube())
    write('telescope_lens_front', telescope_front())
    write('telescope_lens_back', telescope_back())


if __name__ == '__main__':
    main()
