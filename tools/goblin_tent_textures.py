"""Generates the goblin camp tents' textures (Building Blocks epic, "Goblin camps: realistic tents").

    python3 tools/goblin_tent_textures.py

All 16x16 under textures/block/:
- stretched_hide: one big rawhide stretched taut, mottled brown with darker scars and hair specks and a faint
  laced seam, quiet enough to cover a whole tent (its stairs and slab share it, tools/block_families.py)
- stretched_hide_trapdoor: the Stretched Hide Flap, a hide flap with a laced edge, a thong tie and ragged corners
- tent_canvas: grubby, sun-bleached canvas: a fine weave, soft dirt stains, a sewn panel seam
- tent_canvas_trapdoor: the Tent Canvas Flap, canvas with a hemmed edge, a rope tie and a frayed corner
- tent_stake: the Tent Stake, a split spruce stake and the guy rope tied to it (one texture, UV-mapped)
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


def clamp(v):
    return max(0, min(255, int(round(v))))


def rgba(c):
    return tuple(clamp(v) for v in c[:3]) + (255,)


def mix(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def ramp(stops, t):
    """Colour at t (0..1) along a list of (t, colour) stops."""
    t = max(0.0, min(1.0, t))
    for (t0, c0), (t1, c1) in zip(stops, stops[1:]):
        if t <= t1:
            return mix(c0, c1, (t - t0) / (t1 - t0) if t1 > t0 else 0)
    return stops[-1][1]


def noise(seed, cell):
    """Smooth value noise that tiles every 16 px, on a grid of cells (cell or (cx, cy) px; divides 16)."""
    rng = random.Random(seed)
    cx, cy = cell if isinstance(cell, tuple) else (cell, cell)
    gx, gy = N // cx, N // cy
    grid = [[rng.random() for _ in range(gx)] for _ in range(gy)]

    def at(x, y):
        fx, fy = x / cx, y / cy
        x0, y0 = int(fx) % gx, int(fy) % gy
        x1, y1 = (x0 + 1) % gx, (y0 + 1) % gy
        tx, ty = fx - int(fx), fy - int(fy)
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        top = grid[y0][x0] * (1 - tx) + grid[y0][x1] * tx
        bot = grid[y1][x0] * (1 - tx) + grid[y1][x1] * tx
        return top * (1 - ty) + bot * ty

    return [[at(x, y) for x in range(N)] for y in range(N)]


def field(seed, octaves):
    """Sum of tiling noise octaves [(cell, weight)], normalised to 0..1."""
    layers = [(noise(seed + i, cell), w) for i, (cell, w) in enumerate(octaves)]
    total = sum(w for _, w in octaves)
    vals = [[sum(layer[y][x] * w for layer, w in layers) / total for x in range(N)] for y in range(N)]
    lo = min(min(r) for r in vals)
    hi = max(max(r) for r in vals)
    return [[(v - lo) / (hi - lo) for v in r] for r in vals]


# --- Stretched Hide -----------------------------------------------------------------------------------------

HIDE = [(0.0, (92, 62, 38)), (0.35, (120, 82, 50)), (0.7, (140, 98, 60)), (1.0, (158, 116, 74))]
LACE = (176, 150, 104)


def hide_base(seed):
    """Rawhide: broad soft mottling, a fine grain, hair specks and two healed scars."""
    rng = random.Random(seed)
    f = field(seed, [(8, 3), (4, 1.4), (2, 0.35)])
    rows = [[rgba(ramp(HIDE, 0.28 + 0.46 * f[y][x])) for x in range(N)] for y in range(N)]
    for y in range(N):
        for x in range(N):
            d = rng.randint(-3, 3)
            if rng.random() < 0.05:
                d -= 9  # hair follicle / grime speck
            elif rng.random() < 0.03:
                d += 7
            rows[y][x] = rgba(tuple(v + d for v in rows[y][x][:3]))
    # Healed scars: short pale ridges with a dark side.
    for (x, y, dx, dy, n) in [(4, 5, 1, 0, 3), (11, 11, 1, 0, 2)]:
        for i in range(n):
            px, py = (x + dx * i) % N, (y + dy * i) % N
            rows[py][px] = rgba(tuple(v + 10 for v in rows[py][px][:3]))
            qy = (py + 1) % N
            rows[qy][px] = rgba(tuple(v - 10 for v in rows[qy][px][:3]))
    return rows


def stretched_hide():
    rows = hide_base(31)
    # A faint laced seam down the left edge, where two hides are sewn: a slightly darker overlap with a lace
    # crossing it every 4 px, so a wall of it reads as a few big hides rather than a grid of tiles.
    for y in range(N):
        rows[y][0] = rgba(tuple(v - 6 for v in rows[y][0][:3]))
    for y in (3, 11):
        rows[y][0] = rgba(mix(LACE, rows[y][0][:3], 0.35))
    return rows


def stretched_hide_flap():
    """A door flap: hide with a laced top hem, a thong tie across the middle and ragged bottom corners."""
    rows = hide_base(47)
    for x in range(N):
        rows[0][x] = rgba(tuple(v - 18 for v in rows[0][x][:3]))
        rows[1][x] = rgba(tuple(v - 8 for v in rows[1][x][:3]))
    for x in range(1, N, 3):
        rows[1][x] = rgba(LACE)
    # Thong tie: a pale strap across the middle with a knot.
    for x in range(N):
        if x not in (7, 8):
            rows[8][x] = rgba(mix(LACE, (120, 96, 62), (x % 3) / 4))
            rows[9][x] = rgba(tuple(v - 14 for v in rows[9][x][:3]))
    for (x, y) in [(7, 7), (8, 7), (7, 8), (8, 8), (7, 9), (8, 9)]:
        rows[y][x] = rgba(mix(LACE, (100, 80, 50), 0.3 if (x + y) % 2 else 0.0))
    rows[10][6] = rgba(LACE)
    rows[11][6] = rgba(mix(LACE, (100, 80, 50), 0.4))
    rows[10][9] = rgba(LACE)
    # Ragged edges: bites out of the bottom corners and the odd hole.
    for (x, y) in [(0, 15), (1, 15), (0, 14), (14, 15), (15, 15), (15, 14), (15, 13), (6, 15), (10, 15),
                   (0, 6), (15, 4)]:
        rows[y][x] = CLEAR
    for (x, y) in [(2, 15), (1, 14), (0, 13), (13, 15), (14, 14), (14, 13), (5, 15), (7, 15), (9, 15), (11, 15)]:
        if rows[y][x] != CLEAR:
            rows[y][x] = rgba(tuple(v - 22 for v in rows[y][x][:3]))
    return rows


# --- Tent Canvas --------------------------------------------------------------------------------------------

CANVAS = [(0.0, (140, 128, 98)), (0.5, (168, 155, 120)), (1.0, (190, 178, 144))]
THREAD = (120, 104, 76)


def canvas_base(seed):
    """Grubby canvas: a fine plain weave over soft dirt and water stains."""
    rng = random.Random(seed)
    f = field(seed, [(8, 3), ((8, 4), 1.2), ((4, 2), 0.3)])
    rows = []
    for y in range(N):
        row = []
        for x in range(N):
            c = ramp(CANVAS, 0.3 + 0.45 * f[y][x])
            weave = 3 if (x + y) % 2 == 0 else -2
            if x % 5 == 0 and y % 2 == 0:
                weave -= 3  # the odd thicker thread
            row.append(rgba(tuple(v + weave + rng.randint(-2, 2) for v in c)))
        rows.append(row)
    return rows


def tent_canvas():
    rows = canvas_base(53)
    # A sewn seam where two widths of canvas overlap: a darker fold with a stitch line beside it.
    for y in range(N):
        rows[y][12] = rgba(tuple(v - 16 for v in rows[y][12][:3]))
        rows[y][13] = rgba(tuple(v + 6 for v in rows[y][13][:3]))
        if y % 3 == 1:
            rows[y][14] = rgba(mix(rows[y][14][:3], THREAD, 0.6))
    return rows


def tent_canvas_flap():
    """A door flap: canvas with a stitched top hem, a rope tie across the middle and a frayed corner."""
    rows = canvas_base(61)
    for x in range(N):
        rows[0][x] = rgba(tuple(v - 22 for v in rows[0][x][:3]))
        rows[1][x] = rgba(tuple(v - 6 for v in rows[1][x][:3]))
        if x % 2 == 0:
            rows[2][x] = rgba(mix(rows[2][x][:3], THREAD, 0.55))
    rope = (150, 122, 76)
    for x in range(N):
        rows[8][x] = rgba(tuple(v + (12 if x % 2 == 0 else -6) for v in rope))
        rows[9][x] = rgba(tuple(v - 18 for v in rows[9][x][:3]))
    for (x, y) in [(10, 7), (11, 7), (10, 8), (11, 8), (10, 9), (11, 9)]:
        rows[y][x] = rgba(tuple(v + (10 if (x + y) % 2 else -14) for v in rope))
    rows[10][9] = rgba(rope)
    rows[11][9] = rgba(tuple(v - 16 for v in rope))
    rows[10][12] = rgba(rope)
    # Frayed hem at the bottom: loose threads and a torn corner.
    for (x, y) in [(15, 15), (14, 15), (15, 14), (4, 15), (9, 15)]:
        rows[y][x] = CLEAR
    for x in range(N):
        if rows[15][x] != CLEAR and x % 3 == 1:
            rows[15][x] = rgba(tuple(v - 18 for v in rows[15][x][:3]))
    return rows


# --- Tent Stake ---------------------------------------------------------------------------------------------

def tent_stake():
    """
    UV layout (see models/block/tent_stake_*.json): x 0-3 is the stake's side (split spruce, a pale notch near
    the top), x 4-5 its top, y 0-1 x 6-15 the rope (twisted hemp) for the guy lines and the lashing.
    """
    rows = [[CLEAR] * N for _ in range(N)]
    rng = random.Random(71)
    wood = (118, 86, 50)
    for y in range(N):
        for x in range(4):
            d = rng.randint(-5, 5) + (-18 if x == 0 else 8 if x == 2 else 0)
            rows[y][x] = rgba(tuple(v + d for v in wood))
    for y in range(N):
        for x in range(4, 6):
            rows[y][x] = rgba(tuple(v + 20 + rng.randint(-4, 4) for v in wood))
    rope = (176, 148, 98)
    for y in range(N):
        for x in range(6, N):
            twist = 12 if (x + y) % 3 == 0 else -10 if (x + y) % 3 == 2 else 0
            rows[y][x] = rgba(tuple(v + twist for v in rope))
    return rows


def main():
    os.makedirs(OUT, exist_ok=True)
    textures = {
        'stretched_hide': stretched_hide(),
        'stretched_hide_trapdoor': stretched_hide_flap(),
        'tent_canvas': tent_canvas(),
        'tent_canvas_trapdoor': tent_canvas_flap(),
        'tent_stake': tent_stake(),
    }
    for name, rows in textures.items():
        pngio.write_png(os.path.join(OUT, name + '.png'), N, N, rows)
    print(f'wrote {len(textures)} textures')


if __name__ == '__main__':
    main()
