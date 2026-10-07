"""Generates the d20 icon the skill-check HUD draws its roll on.

    python3 tools/d20_texture.py

A red twenty-sided die seen face-on: a hexagon outline with the upward-pointing front face in the
middle (where the HUD writes the number) and six darker side facets round it. 64x64, drawn at 32x32.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/gui/d20.png')

SIZE = 64
R = 30.5          # hexagon radius
INNER = 0.62      # front-face radius as a fraction of R
EDGE = 1.3        # outline half-width in pixels
SS = 4            # supersampling per axis

C = SIZE / 2
HEX = [(C + R * math.cos(math.radians(-90 + 60 * k)), C + R * math.sin(math.radians(-90 + 60 * k)))
       for k in range(6)]
TRI = [(C + R * INNER * math.cos(math.radians(-90 + 120 * k)), C + R * INNER * math.sin(math.radians(-90 + 120 * k)))
       for k in range(3)]
# Facet edges: the front face, plus spokes from its corners to the hexagon corners
SEGMENTS = [(TRI[k], TRI[(k + 1) % 3]) for k in range(3)]
SEGMENTS += [(TRI[k], HEX[2 * k]) for k in range(3)]
SEGMENTS += [(TRI[k], HEX[(2 * k + 1) % 6]) for k in range(3)]
SEGMENTS += [(TRI[k], HEX[(2 * k - 1) % 6]) for k in range(3)]
SEGMENTS += [(HEX[k], HEX[(k + 1) % 6]) for k in range(6)]

FACE = (196, 38, 48)
SIDES = [(168, 30, 40), (140, 24, 33), (118, 18, 27), (132, 22, 30), (150, 27, 36), (176, 34, 44)]
OUTLINE = (58, 8, 14)
SHINE = (236, 96, 100)


def inside(poly, x, y):
    sign = None
    for (ax, ay), (bx, by) in zip(poly, poly[1:] + poly[:1]):
        cross = (bx - ax) * (y - ay) - (by - ay) * (x - ax)
        if abs(cross) < 1e-9:
            continue
        s = cross > 0
        if sign is None:
            sign = s
        elif s != sign:
            return False
    return True


def seg_dist(px, py, a, b):
    (ax, ay), (bx, by) = a, b
    dx, dy = bx - ax, by - ay
    t = max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)))
    return math.hypot(px - ax - t * dx, py - ay - t * dy)


def sample(x, y):
    if not inside(HEX, x, y):
        return None
    if min(seg_dist(x, y, a, b) for a, b in SEGMENTS) < EDGE:
        return OUTLINE
    if inside(TRI, x, y):
        # A soft highlight towards the upper left of the front face
        d = math.hypot(x - (C - 6), y - (C - 4))
        t = max(0.0, 1 - d / 22) * 0.35
        return tuple(round(f + (s - f) * t) for f, s in zip(FACE, SHINE))
    angle = (math.degrees(math.atan2(y - C, x - C)) + 90) % 360
    return SIDES[int(angle // 60) % 6]


def main():
    rows = []
    for py in range(SIZE):
        row = []
        for px in range(SIZE):
            acc = [0, 0, 0]
            hits = 0
            for sy in range(SS):
                for sx in range(SS):
                    c = sample(px + (sx + 0.5) / SS, py + (sy + 0.5) / SS)
                    if c is not None:
                        hits += 1
                        for i in range(3):
                            acc[i] += c[i]
            if hits:
                row.append(tuple(round(v / hits) for v in acc) + (round(255 * hits / (SS * SS)),))
            else:
                row.append((0, 0, 0, 0))
        rows.append(row)
    pngio.write_png(OUT, SIZE, SIZE, rows)
    print(OUT)


if __name__ == '__main__':
    main()
