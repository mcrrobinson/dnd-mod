"""Generates the drow item and effect icons.

    python3 tools/drow_icons.py

Writes textures/item/spidersilk.png (a pale silver skein, 16x16) and textures/mob_effect/drow_poison.png (a
green-black drop) and faerie_fire.png (a violet flame), both 18x18.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures')
CLEAR = (0, 0, 0, 0)


def grid(n):
    return [[CLEAR for _ in range(n)] for _ in range(n)]


def spidersilk():
    px = grid(16)
    # A loose skein: overlapping ellipses of silver thread.
    for t in range(0, 360, 3):
        for (cx, cy, rx, ry, rot) in ((8, 8, 6, 3, 0.5), (8, 8, 6, 3, -0.5), (8, 8, 5, 2, 1.4)):
            a = math.radians(t)
            x = rx * math.cos(a)
            y = ry * math.sin(a)
            xr = x * math.cos(rot) - y * math.sin(rot) + cx
            yr = x * math.sin(rot) + y * math.cos(rot) + cy
            ix, iy = int(round(xr)), int(round(yr))
            if 0 <= ix < 16 and 0 <= iy < 16:
                shade = 200 + (ix * 7 + iy * 3) % 40
                px[iy][ix] = (shade, shade, min(255, shade + 14), 255)
    for (x, y) in ((8, 8), (7, 8), (8, 7)):
        px[y][x] = (250, 250, 255, 255)
    return px


def drop(color, edge):
    px = grid(18)
    for y in range(18):
        for x in range(18):
            dx = x - 8.5
            dy = y - 10.5
            inside = dx * dx + dy * dy <= 25 or (y < 10 and abs(dx) <= (y - 2) * 0.6 and y >= 2)
            if inside:
                rim = dx * dx + dy * dy > 16 and y >= 8
                px[y][x] = (edge if rim else color) + (255,)
    px[9][6] = (180, 220, 150, 255)
    px[10][6] = (140, 190, 110, 255)
    return px


def flame():
    px = grid(18)
    for y in range(18):
        for x in range(18):
            dx = x - 8.5
            width = max(0.0, (y - 2) * 0.45) if y < 12 else max(0.0, 5 - (y - 12) * 0.9)
            if abs(dx) <= width and 2 <= y <= 16:
                core = abs(dx) <= width * 0.45 and y > 7
                px[y][x] = ((236, 210, 255) if core else (170, 100, 240) if abs(dx) <= width * 0.8 else (110, 50, 180)) + (255,)
    return px


def main():
    os.makedirs(os.path.join(ASSETS, 'item'), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, 'mob_effect'), exist_ok=True)
    pngio.write_png(os.path.join(ASSETS, 'item/spidersilk.png'), 16, 16, spidersilk())
    pngio.write_png(os.path.join(ASSETS, 'mob_effect/drow_poison.png'), 18, 18, drop((46, 70, 34), (20, 28, 16)))
    pngio.write_png(os.path.join(ASSETS, 'mob_effect/faerie_fire.png'), 18, 18, flame())
    print('wrote icons')


if __name__ == '__main__':
    main()
