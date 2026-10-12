"""Generates the Fire Curtain's connected soul-fire sheet textures.

    python3 tools/fire_curtain_textures.py [--preview DIR]

A curtain block draws a flat sheet of flame (see FireCurtainModel). Which texture it uses depends on which of
its four in-plane neighbours are curtain too, so the blocks of one curtain join into a single sheet:

    sheet_<t><b><l><r>_<c>.png   t = top edge (nothing above), b = bottom edge (base), l / r = left / right
                                 edge (no curtain that way); 'x' in a slot means a neighbour continues the
                                 sheet there. c = 0..2 is the block's column along the sheet (world position
                                 mod 3), so the tongues only repeat every three blocks.

All 48 are cut from one flame field that wraps every 48 pixels across and 16 up and scrolls upward, so the
edges between blocks line up in every animation frame. Only the sheet's outer edges are cut: licking tongues
along the top, ragged flames down the sides, a white-hot base along the bottom. 32 frames of 16x16, like
vanilla soul fire, and its palette (dark teal tips to a white core). --preview also writes enlarged mock-ups
of a few curtain shapes (frame 0) into DIR.
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/block/fire_curtain')

N = 16
FRAMES = 32
# Blocks along the sheet cycle through COLUMNS textures (by world position), so the field repeats only every
# WIDTH pixels and the tongues don't line up block after block
COLUMNS = 3
WIDTH = N * COLUMNS
TAU = 2 * math.pi

# Vanilla soul fire's ramp, from the dark teal at the tips to the white at the base
RAMP = [
    (0.00, (0, 92, 98)),
    (0.22, (1, 132, 137)),
    (0.42, (1, 170, 176)),
    (0.60, (16, 211, 217)),
    (0.76, (84, 243, 248)),
    (0.90, (185, 253, 255)),
    (1.00, (255, 255, 255)),
]


def colour(b):
    b = max(0.0, min(1.0, b))
    for (t0, c0), (t1, c1) in zip(RAMP, RAMP[1:]):
        if b <= t1:
            k = (b - t0) / (t1 - t0)
            return tuple(round(c0[i] + (c1[i] - c0[i]) * k) for i in range(3)) + (255,)
    return RAMP[-1][1] + (255,)


class PeriodicNoise:
    """Smooth value noise on a cx x cy lattice that wraps every WIDTH pixels across and 16 up."""

    def __init__(self, seed, cx, cy):
        rng = random.Random(seed)
        self.cx, self.cy = cx, cy
        self.v = [[rng.random() for _ in range(cx)] for _ in range(cy)]

    def __call__(self, x, y):
        gx = (x / WIDTH * self.cx) % self.cx
        gy = (y / N * self.cy) % self.cy
        x0, y0 = int(gx), int(gy)
        fx, fy = gx - x0, gy - y0
        fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        x1, y1 = (x0 + 1) % self.cx, (y0 + 1) % self.cy
        a = self.v[y0][x0] + (self.v[y0][x1] - self.v[y0][x0]) * fx
        b = self.v[y1][x0] + (self.v[y1][x1] - self.v[y1][x0]) * fx
        return a + (b - a) * fy


# Tall, narrow cells: the body reads as rising streaks of flame
BODY = PeriodicNoise(7, 20, 2)
FLICKER = PeriodicNoise(13, 40, 4)


def scroll(f):
    """How far (pixels) the flame has risen by frame f: one whole tile over the loop."""
    return N * f / FRAMES


def body(x, y, f):
    """Brightness 0..1 of the flame body at x, y (pixels, y up from the block's bottom)."""
    s = y - scroll(f)
    n = 0.65 * BODY(x, s) + 0.35 * FLICKER(x, s)
    # Contrast: dark seams between brighter rising tongues
    n = n * n * (3 - 2 * n)
    b = 0.30 + 0.62 * n
    # Faint upward-slanting streaks
    b += 0.06 * math.sin(TAU * (5 * x / WIDTH + s / N))
    return b


def tongues(x, f):
    """Height (pixels above the block's bottom) the flame reaches in a top-edge block: pointed, licking tongues."""
    t = f / FRAMES
    h = 8.0
    for k, m, amp, ph in ((2, 1, 2.6, 0.10), (5, -2, 3.0, 0.55), (7, 3, 2.4, 0.30), (11, -4, 1.8, 0.80),
                          (16, 5, 1.2, 0.45)):
        h += amp * (0.5 + 0.5 * math.cos(TAU * (k * x / WIDTH + m * t + ph))) ** 2
    return min(h, 15.6)


def ragged(y, f, phase):
    """How far (pixels) a side edge eats into the block at height y: flames flickering up the side."""
    s = y - scroll(f)
    e = 0.6 + 1.6 * (0.5 + 0.5 * math.sin(TAU * (s / N + phase))) ** 2
    e += 1.2 * (0.5 + 0.5 * math.sin(TAU * (3 * s / N + 2 * phase + 0.2)))
    return e


def sheet(top, base, left, right, column, f):
    """One 16x16 frame of the given column (0..COLUMNS-1) of the field: rows top to bottom of RGBA."""
    rows = []
    for r in range(N):
        row = []
        for c in range(N):
            lx, y = c + 0.5, N - r - 0.5
            x = column * N + lx  # position in the whole field, for the flame; lx within the block, for the edges
            dist = 9.0  # pixels to the nearest cut edge of the sheet
            limit = N + 1.0
            if top:
                limit = tongues(x, f)
                # Round the shoulders where the top meets a side edge
                if left:
                    limit -= max(0.0, 7.0 - 1.4 * lx)
                if right:
                    limit -= max(0.0, 7.0 - 1.4 * (N - lx))
                dist = min(dist, limit - y)
            if left:
                dist = min(dist, lx - ragged(y, f, 0.0))
            if right:
                dist = min(dist, (N - lx) - ragged(y, f, 0.5))
            if dist <= 0:
                row.append((0, 0, 0, 0))
                continue
            b = body(x, y, f)
            # Darker teal round the cut edges, like the tips of vanilla fire
            b *= 0.55 + 0.45 * min(1.0, dist / 3.0)
            if base:
                b += 0.45 * max(0.0, 1.0 - y / 5.0)
            row.append(colour(b))
        rows.append(row)
    return rows


def variants():
    for top in (True, False):
        for base in (True, False):
            for left in (True, False):
                for right in (True, False):
                    for column in range(COLUMNS):
                        name = 'sheet_' + ''.join(ch if on else 'x' for ch, on in
                                                  (('t', top), ('b', base), ('l', left), ('r', right)))
                        yield '%s_%d' % (name, column), (top, base, left, right, column)


def preview(out_dir):
    """Enlarged frame-0 mock-ups of a few curtain shapes, to judge the joins without starting the game."""
    os.makedirs(out_dir, exist_ok=True)
    scale = 8
    for w, h in ((3, 1), (1, 3), (3, 3), (6, 2)):
        px = [[(40, 40, 40, 255)] * (w * N * scale) for _ in range(h * N * scale)]
        for bx in range(w):
            for by in range(h):  # by = 0 is the top row
                tile = sheet(by == 0, by == h - 1, bx == 0, bx == w - 1, bx % COLUMNS, 0)
                for r in range(N):
                    for c in range(N):
                        if tile[r][c][3]:
                            for dy in range(scale):
                                for dx in range(scale):
                                    px[(by * N + r) * scale + dy][(bx * N + c) * scale + dx] = tile[r][c]
        path = os.path.join(out_dir, 'curtain_%dx%d.png' % (w, h))
        pngio.write_png(path, w * N * scale, h * N * scale, px)
        print('wrote', path)


def main():
    if len(sys.argv) == 3 and sys.argv[1] == '--preview':
        preview(sys.argv[2])
        return
    os.makedirs(OUT, exist_ok=True)
    for name, flags in variants():
        rows = []
        for f in range(FRAMES):
            rows.extend(sheet(*flags, f))
        path = os.path.join(OUT, name + '.png')
        pngio.write_png(path, N, N * FRAMES, rows)
        with open(path + '.mcmeta', 'w') as fh:
            json.dump({'animation': {'frametime': 1}}, fh, indent=2)
            fh.write('\n')
        print('wrote', os.path.relpath(path, ROOT))


if __name__ == '__main__':
    main()
