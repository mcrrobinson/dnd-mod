"""Generates the racial banner pattern textures (Building Blocks epic, "Racial banner patterns").

    python3 tools/banner_pattern_textures.py

For each pattern in PATTERNS it writes
- textures/entity/banner/<name>.png: 64x64, the emblem on the banner's front face (x 1-20, y 1-40, mirrored,
  since the game draws that face mirrored) and on its back face (x 22-41), as vanilla's banner patterns are laid out;
- textures/entity/shield/<name>.png: 64x64, the emblem on the shield's front face (x 1-12, y 1-22);
- textures/item/<name>_banner_pattern.png: 16x16, a parchment with a small brown emblem.

A pattern texture is a white mask: the game tints it with the dye and draws it where it's opaque. Each emblem
is a list of shapes in banner face coordinates (x 0-20, y 0-40), drawn by supersampling every pixel; the
shield and item versions sample the same shapes at their own size.
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
TEX = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures')

FULL, HALF = 255, 130


# ---- Shapes: each is a function (x, y) -> True when the point is inside. ----

def disk(cx, cy, r):
    return lambda x, y: (x - cx) ** 2 + (y - cy) ** 2 <= r * r


def ellipse(cx, cy, rx, ry, angle=0.0):
    c, s = math.cos(math.radians(angle)), math.sin(math.radians(angle))

    def inside(x, y):
        dx, dy = x - cx, y - cy
        u, v = dx * c + dy * s, -dx * s + dy * c
        return (u / rx) ** 2 + (v / ry) ** 2 <= 1.0
    return inside


def ring(cx, cy, r_out, r_in):
    return lambda x, y: r_in * r_in <= (x - cx) ** 2 + (y - cy) ** 2 <= r_out * r_out


def rect(x0, y0, x1, y1):
    return lambda x, y: x0 <= x <= x1 and y0 <= y <= y1


def _seg_dist(x, y, x1, y1, x2, y2):
    dx, dy = x2 - x1, y2 - y1
    t = 0.0 if dx == dy == 0 else max(0.0, min(1.0, ((x - x1) * dx + (y - y1) * dy) / (dx * dx + dy * dy)))
    return math.hypot(x - (x1 + t * dx), y - (y1 + t * dy)), t


def seg(x1, y1, x2, y2, w, w2=None):
    """A thick line from (x1, y1) to (x2, y2), w wide, tapering to w2 at the far end."""
    w2 = w if w2 is None else w2

    def inside(x, y):
        d, t = _seg_dist(x, y, x1, y1, x2, y2)
        return d <= (w + (w2 - w) * t) / 2
    return inside


def poly(*pts):
    def inside(x, y):
        hit = False
        for i in range(len(pts)):
            (ax, ay), (bx, by) = pts[i], pts[i - 1]
            if (ay > y) != (by > y) and x < (bx - ax) * (y - ay) / (by - ay) + ax:
                hit = not hit
        return hit
    return inside


def curve(p0, p1, p2, w0, w1, steps=24):
    """A quadratic Bezier stroke from p0 to p2 (control p1), w0 wide at p0 tapering to w1."""
    pts = [((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0],
            (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1])
           for t in (i / steps for i in range(steps + 1))]
    parts = [seg(pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1],
                 w0 + (w1 - w0) * i / steps, w0 + (w1 - w0) * (i + 1) / steps) for i in range(steps)]
    return lambda x, y: any(p(x, y) for p in parts)


def mirror(shape, cx=10.0):
    return lambda x, y: shape(2 * cx - x, y)


def regular(cx, cy, r, n, start=-90.0):
    return [(cx + r * math.cos(math.radians(start + 360.0 * i / n)), cy + r * math.sin(math.radians(start + 360.0 * i / n)))
            for i in range(n)]


# ---- The emblems: lists of (mode, shape); later entries draw over earlier ones. ----
# mode '+' paints, '-' cuts back to clear, 'h' paints at half strength (shading, veins, glints).

def bloodtusk():
    """Two crossed curved tusks under a handprint."""
    shapes = [
        # the hand: palm, four spread fingers, thumb
        ('+', ellipse(10.2, 12.6, 2.9, 2.5)),
        ('+', seg(8.0, 12.0, 6.4, 6.6, 1.3)),
        ('+', seg(9.4, 11.0, 9.0, 5.0, 1.3)),
        ('+', seg(11.0, 11.0, 11.6, 5.2, 1.3)),
        ('+', seg(12.2, 12.0, 14.2, 7.4, 1.3)),
        ('+', seg(12.0, 14.2, 15.8, 13.2, 1.3)),
    ]
    tusk = curve((3.6, 33.0), (6.4, 22.4), (16.8, 19.2), 3.8, 0.7)
    shapes += [('+', tusk), ('+', mirror(tusk))]
    return shapes


def clan_anvil():
    """A hammer over an anvil in a hexagon."""
    hexagon = regular(10, 20, 9.6, 6)
    inner = regular(10, 20, 8.2, 6)
    return [
        ('+', poly(*hexagon)),
        ('-', poly(*inner)),
        # hammer: head, then handle down towards the anvil
        ('+', rect(7.0, 11.0, 13.0, 14.0)),
        ('+', rect(9.2, 14.0, 10.8, 19.0)),
        # anvil: face with a horn to the left, waist, foot
        ('+', poly((3.6, 20.4), (15.4, 20.4), (15.4, 22.8), (7.6, 22.8))),
        ('+', rect(9.0, 22.8, 12.4, 25.0)),
        ('+', poly((6.6, 27.6), (8.6, 25.0), (12.8, 25.0), (14.8, 27.6))),
    ]


def sylvan_leaf():
    """A single elegant leaf rising from a crescent moon."""
    def leaf(x, y):
        # a lens, pointed at both ends, leaning a little to the right
        top, bottom = 5.2, 24.5
        if not top <= y <= bottom:
            return False
        t = (y - top) / (bottom - top)
        mid = 10.0 + 1.6 * (1 - t) ** 2 - 0.4
        half = 4.4 * math.sin(math.pi * t) ** 0.9
        return abs(x - mid) <= half

    def vein(x, y):
        if not 7.5 <= y <= 24.5:
            return False
        t = (y - 5.2) / (24.5 - 5.2)
        mid = 10.0 + 1.6 * (1 - t) ** 2 - 0.4
        return abs(x - mid) <= 0.55
    return [
        ('+', leaf),
        ('h', vein),
        ('+', seg(9.6, 24.0, 9.9, 27.6, 1.2)),
        # the crescent, horns up, cradling the leaf
        ('+', lambda x, y: disk(10, 23.5, 8.4)(x, y) and not disk(10, 20.6, 8.6)(x, y)),
    ]


def dragon_head(shapes, nx, ny, angle, size=1.0):
    """A horned dragon's head, its neck at (nx, ny), snout pointing along angle (degrees)."""
    ux, uy = math.cos(math.radians(angle)), math.sin(math.radians(angle))
    px, py = -uy, ux

    def at(f, s):
        return nx + (ux * f + px * s) * size, ny + (uy * f + py * s) * size
    skull = at(2.4, 0)
    snout = at(5.2, 0.4)
    shapes.append(('+', ellipse(skull[0], skull[1], 2.4 * size, 1.9 * size, angle)))
    shapes.append(('+', seg(*skull, *snout, 2.4 * size, 1.6 * size)))
    for side, back in ((-1, 2.6), (1, 2.0)):
        root = at(1.6, side * 1.0)
        tip = at(-1.6, side * back)
        shapes.append(('+', seg(*root, *tip, 1.2 * size, 0.5 * size)))
    eye = at(2.8, -0.7)
    shapes.append(('-', disk(eye[0], eye[1], 0.55 * size)))


def three_breaths():
    """Three dragon heads meeting in a triangle: one at each corner, seen from above, snouts meeting in the middle."""
    cx, cy = 10.0, 20.4
    shapes = []
    for k in range(3):
        a = -90 + 120 * k
        ux, uy = math.cos(math.radians(a)), math.sin(math.radians(a))
        px, py = -uy, ux
        back = 8.2

        def at(f, s, ux=ux, uy=uy, px=px, py=py, back=back):
            # f: from the back of the head in towards the middle; s: across it
            return cx + ux * (back - f) + px * s, cy + uy * (back - f) + py * s
        shapes.append(('+', poly(at(0, -2.9), at(0, 2.9), at(3.4, 2.1), at(6.2, 0.7), at(6.2, -0.7), at(3.4, -2.1))))
        for side in (-1, 1):
            shapes.append(('+', seg(*at(0.4, side * 2.0), *at(-2.6, side * 3.6), 1.4, 0.5)))
            shapes.append(('-', disk(*at(2.0, side * 1.3), 0.8)))
    return shapes


def infernal_pact():
    """A pentagram in a ring, with horns."""
    cx, cy = 10.0, 22.0
    star = regular(cx, cy, 6.8, 5)
    shapes = [('+', ring(cx, cy, 8.0, 6.8))]
    for i in range(5):
        a, b = star[i], star[(i + 2) % 5]
        shapes.append(('+', seg(a[0], a[1], b[0], b[1], 0.9)))
    horn = curve((5.0, 16.6), (1.6, 12.6), (3.6, 6.0), 3.0, 0.6)
    shapes += [('+', horn), ('+', mirror(horn))]
    return shapes


def gnome_cog():
    """A cog with a magnifying glass over it."""
    cx, cy = 8.6, 17.8
    shapes = [('+', disk(cx, cy, 5.0))]
    for i in range(8):
        a = math.radians(22.5 + 45 * i)
        shapes.append(('+', seg(cx + 4.0 * math.cos(a), cy + 4.0 * math.sin(a),
                                cx + 6.6 * math.cos(a), cy + 6.6 * math.sin(a), 2.2)))
    shapes.append(('-', disk(cx, cy, 2.0)))
    gx, gy = 12.8, 23.4
    shapes += [
        ('-', disk(gx, gy, 4.4)),
        ('-', seg(gx + 2.6, gy + 2.6, 17.8, 30.0, 3.2)),
        ('+', ring(gx, gy, 3.6, 2.4)),
        ('h', seg(gx - 1.0, gy - 0.6, gx - 0.2, gy - 1.4, 0.9)),
        ('+', seg(gx + 2.6, gy + 2.6, 17.4, 29.4, 1.9)),
    ]
    return shapes


def green_dragon():
    """A curled dragon: coiled round on itself, wing raised, head up and looking out to the right."""
    cx, cy = 9.4, 25.0
    shapes = []
    # the coil: from the neck (upper right) clockwise round the bottom and up the left, then the tail
    # spiralling in and tapering
    pts = []
    for i in range(61):
        t = i / 60
        a = math.radians(-50 + 400 * t)
        r = 5.4 - 3.0 * t
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a), 3.0 - 2.4 * t))
    for i in range(60):
        (x1, y1, w1), (x2, y2, w2) = pts[i], pts[i + 1]
        shapes.append(('+', seg(x1, y1, x2, y2, w1, w2)))
    # neck rising to the head
    shapes.append(('+', curve((pts[0][0], pts[0][1]), (14.6, 16.0), (11.6, 13.6), 2.8, 2.4)))
    dragon_head(shapes, 11.0, 13.2, -12, 1.25)
    # a bat wing raised over the coil's back
    shapes.append(('+', poly((9.6, 19.6), (3.4, 10.4), (2.0, 18.4), (3.8, 17.0), (4.4, 20.4), (6.4, 18.6), (7.6, 20.6))))
    return shapes


def goblin_skull():
    """A crude, grinning goblin skull with long ears."""
    shapes = [
        ('+', ellipse(10, 16.6, 5.8, 5.2)),
        ('+', rect(6.8, 18.0, 13.2, 25.4)),
        # long ears, sweeping up and out
        ('+', poly((5.0, 15.4), (0.4, 10.2), (5.8, 19.0))),
        ('+', poly((15.0, 15.4), (19.6, 10.2), (14.2, 19.0))),
        # big slanted eye sockets
        ('-', poly((5.8, 14.8), (9.4, 16.6), (9.0, 19.6), (6.4, 19.0))),
        ('-', poly((14.2, 14.8), (10.6, 16.6), (11.0, 19.6), (13.6, 19.0))),
        ('-', poly((10.0, 19.6), (8.8, 21.6), (11.2, 21.6))),
        # the grin: a gap between the jaws, with crooked teeth across it
        ('-', rect(6.8, 22.8, 13.2, 23.8)),
        ('+', rect(7.0, 22.6, 7.9, 23.0)),
        ('+', rect(9.0, 22.8, 9.9, 24.0)),
        ('+', rect(11.0, 22.6, 11.9, 23.2)),
        ('+', rect(12.0, 22.8, 12.9, 24.0)),
        # cheekbones: the jaw is narrower than the skull
        ('-', rect(6.8, 21.6, 7.4, 25.6)),
        ('-', rect(12.6, 21.6, 13.2, 25.6)),
        # a crack down the brow
        ('-', seg(11.8, 11.6, 12.8, 14.4, 0.7)),
    ]
    return shapes


PATTERNS = [
    ('bloodtusk', bloodtusk),
    ('clan_anvil', clan_anvil),
    ('sylvan_leaf', sylvan_leaf),
    ('three_breaths', three_breaths),
    ('infernal_pact', infernal_pact),
    ('gnome_cog', gnome_cog),
    ('green_dragon', green_dragon),
    ('goblin_skull', goblin_skull),
]

SS = 4  # supersamples per axis


def coverage(shapes, x, y):
    """Returns the mask strength at design point (x, y)."""
    value = 0
    for mode, shape in shapes:
        if shape(x, y):
            value = FULL if mode == '+' else HALF if mode == 'h' else 0
    return value


def mask(shapes, width, height, sx, sy, ox=0.0, oy=0.0):
    """width x height pixels, pixel (i, j) covering design x in ox + [i, i+1] * sx (y likewise)."""
    out = [[0] * width for _ in range(height)]
    for j in range(height):
        for i in range(width):
            full = half = 0
            for a in range(SS):
                for b in range(SS):
                    v = coverage(shapes, ox + (i + (a + 0.5) / SS) * sx, oy + (j + (b + 0.5) / SS) * sy)
                    full += v == FULL
                    half += v == HALF
            out[j][i] = FULL if full * 2 >= SS * SS else HALF if half and (full + half) * 2 >= SS * SS else 0
    return out


def pattern_png(face, x0, y0, back_x0=None, seed=0):
    rng = random.Random(seed)
    rows = [[(0, 0, 0, 0)] * 64 for _ in range(64)]
    for j, line in enumerate(face):
        for i, a in enumerate(line):
            if a:
                g = rng.randint(226, 255)
                if back_x0 is None:
                    rows[y0 + j][x0 + i] = (g, g, g, a)
                else:
                    # The banner's front face is drawn mirrored (seen from in front, x 20 is on the left), so
                    # the emblem goes on it mirrored, and as drawn on the back face.
                    rows[y0 + j][x0 + len(line) - 1 - i] = (g, g, g, a)
                    rows[y0 + j][back_x0 + i] = (g, g, g, a)
    return rows


PARCHMENT = (236, 226, 196)
PARCHMENT_DARK = (214, 199, 160)
ROLL = (178, 158, 112)
ROLL_DARK = (132, 112, 76)
INK = (92, 54, 32)


def item_png(shapes):
    """A parchment sheet, rolled at top and bottom, with the emblem inked small in the middle."""
    rows = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    for y in range(2, 14):
        for x in range(3, 13):
            edge = x in (3, 12)
            rows[y][x] = PARCHMENT_DARK + (255,) if edge else PARCHMENT + (255,)
    for y in (1, 14):
        for x in range(2, 14):
            rows[y][x] = (ROLL_DARK if x in (2, 13) or y == 14 else ROLL) + (255,)
    for y in (2, 13):
        rows[y][2] = ROLL_DARK + (255,)
        rows[y][13] = ROLL_DARK + (255,)
    # the emblem: design x 1-19, y 5-33 onto pixels 4-11, 3-12
    small = mask(shapes, 8, 10, 18.0 / 8, 28.0 / 10, ox=1.0, oy=5.5)
    for j in range(10):
        for i in range(8):
            if small[j][i] == FULL:
                rows[3 + j][4 + i] = INK + (255,)
            elif small[j][i] == HALF:
                rows[3 + j][4 + i] = tuple((p + q) // 2 for p, q in zip(INK, PARCHMENT)) + (255,)
    return rows


def main():
    for d in ('entity/banner', 'entity/shield', 'item'):
        os.makedirs(os.path.join(TEX, d), exist_ok=True)
    for index, (name, design) in enumerate(PATTERNS):
        shapes = design()
        banner = mask(shapes, 20, 40, 1.0, 1.0)
        shield = mask(shapes, 12, 22, 20.0 / 12, 40.0 / 22)
        pngio.write_png(os.path.join(TEX, 'entity/banner', name + '.png'), 64, 64,
                        pattern_png(banner, 1, 1, back_x0=22, seed=index))
        pngio.write_png(os.path.join(TEX, 'entity/shield', name + '.png'), 64, 64,
                        pattern_png(shield, 1, 1, seed=100 + index))
        pngio.write_png(os.path.join(TEX, 'item', name + '_banner_pattern.png'), 16, 16, item_png(shapes))
        print('wrote', name)


if __name__ == '__main__':
    main()
