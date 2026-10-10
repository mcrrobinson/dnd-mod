"""Generates the physical obstacle block textures (Barbarian, Fighter and Monk obstacles).

    python3 tools/physical_obstacle_textures.py

16x16: rubble for a Collapsed Passage, a granite Heavy Boulder, iron Portcullis bars (cut out),
a winch drum (side and end) shared by the portcullis and bridge winches, and cracked Crumbling
Ledge bricks. A chasm's lowered bridge uses vanilla spruce planks.
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


def shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)


def rubble():
    """Fist-sized stones in a dark matrix, with gravel specks: obviously not plain cobblestone."""
    rng = random.Random(11)
    stones = [(rng.uniform(0, N), rng.uniform(0, N), rng.uniform(2.0, 3.6)) for _ in range(14)]
    greys = [(128, 124, 118), (104, 100, 96), (142, 136, 126), (116, 108, 98)]
    rows = []
    for y in range(N):
        row = []
        for x in range(N):
            px = (58, 52, 48, 255)
            best = None
            for i, (sx, sy, r) in enumerate(stones):
                # wrap so the texture tiles
                dx = min(abs(x - sx), N - abs(x - sx))
                dy = min(abs(y - sy), N - abs(y - sy))
                d = (dx * dx + dy * dy) ** 0.5 / r
                if d < 1.0 and (best is None or d < best[0]):
                    best = (d, i, dx, dy)
            if best is not None:
                d, i, dx, dy = best
                base = greys[i % len(greys)] + (255,)
                px = shade(base, 18 if d < 0.45 else (-22 if d > 0.85 else 0))
            elif rng.random() < 0.35:
                px = (96, 82, 70, 255)
            row.append(px)
        rows.append(row)
    return rows


def boulder():
    """Mottled granite with a couple of cracks and a mossy speckle."""
    rng = random.Random(23)
    rows = []
    for y in range(N):
        row = []
        for x in range(N):
            n = rng.randint(-14, 14) + (8 if (x * 3 + y * 5) % 7 == 0 else 0)
            px = shade((122, 118, 112), n)
            if (x == y // 2 + 3 and 2 < y < 12) or (y == 12 and 6 < x < 13):
                px = (70, 66, 62, 255)
            elif rng.random() < 0.06:
                px = (84, 108, 58, 255)
            row.append(px)
        rows.append(row)
    return rows


def portcullis():
    """Three dark iron bars down, two across, rivets where they meet; see-through between."""
    rows = []
    for y in range(N):
        row = []
        for x in range(N):
            vertical = x % 6 in (1, 2)
            horizontal = y in (3, 4, 11, 12)
            if vertical and horizontal:
                px = (150, 150, 156, 255) if (x % 6 == 1 and y in (3, 11)) else (92, 92, 98, 255)
            elif vertical:
                px = (78, 78, 84, 255) if x % 6 == 1 else (58, 58, 64, 255)
            elif horizontal:
                px = (70, 70, 76, 255) if y in (3, 11) else (50, 50, 56, 255)
            else:
                px = CLEAR
            # spikes on the bottom row of each bar
            if y == 15 and vertical:
                px = (40, 40, 46, 255)
            row.append(px)
        rows.append(row)
    return rows


def winch_side():
    """A wooden drum wound with rope, iron bands at both ends."""
    rows = []
    for y in range(N):
        row = []
        for x in range(N):
            if y in (0, 1, 14, 15):
                px = (74, 74, 80, 255) if y in (0, 15) else (104, 104, 110, 255)
            elif y % 3 == 2:
                px = (122, 98, 60, 255)
            else:
                px = (176, 148, 98, 255) if (x + y) % 4 else (156, 128, 82, 255)
            row.append(px)
        rows.append(row)
    return rows


def winch_end():
    """An iron handle wheel with four spokes on a wooden end."""
    rows = []
    for y in range(N):
        row = []
        for x in range(N):
            cx = cy = 7.5
            r = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if 5.5 <= r <= 7.0:
                px = (86, 86, 92, 255)
            elif r <= 1.6:
                px = (140, 140, 148, 255)
            elif abs(x - cx) <= 0.6 or abs(y - cy) <= 0.6:
                px = (100, 100, 108, 255)
            else:
                px = (112, 82, 48, 255) if (x * 2 + y) % 5 else (96, 70, 40, 255)
            row.append(px)
        rows.append(row)
    return rows


def crumbling_ledge():
    """Stone bricks gone to pieces: wide cracks and pale dust, so it reads as unsafe."""
    rng = random.Random(5)
    cracks = {(x, y) for x, y in [(2, 1), (3, 2), (3, 3), (4, 4), (5, 4), (6, 5), (10, 2), (11, 3), (11, 4),
                                   (12, 5), (5, 9), (6, 10), (6, 11), (7, 12), (8, 12), (13, 10), (12, 11),
                                   (12, 12), (11, 13), (1, 12), (2, 13), (9, 6), (9, 7), (8, 8)]}
    rows = []
    for y in range(N):
        row = []
        for x in range(N):
            mortar = y in (0, 7, 8, 15) or (y < 8 and x in (0, 8)) or (y >= 8 and x in (4, 12))
            if (x, y) in cracks:
                px = (40, 38, 36, 255)
            elif mortar:
                px = (88, 86, 84, 255)
            else:
                px = shade((132, 130, 126), rng.randint(-10, 10))
                if rng.random() < 0.08:
                    px = (176, 170, 156, 255)
            row.append(px)
        rows.append(row)
    return rows


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, rows in [('collapsed_passage', rubble()), ('heavy_boulder', boulder()),
                       ('portcullis', portcullis()), ('winch_side', winch_side()),
                       ('winch_end', winch_end()), ('crumbling_ledge', crumbling_ledge())]:
        pngio.write_png(os.path.join(OUT, name + '.png'), N, N, rows)


if __name__ == '__main__':
    main()
