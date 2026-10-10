"""Generates the Arcane Seal block textures.

    python3 tools/obstacle_textures.py

16x16 translucent glyph panes: a violet field with a bright rune circle (lesser), the same with a
gold rim (greater), and the faint broken frame an opened seal leaves behind.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/block')

N = 16
FIELD = (118, 58, 196, 96)
FIELD_DARK = (92, 40, 160, 120)
EDGE = (170, 110, 255, 200)
RUNE = (226, 190, 255, 235)
GOLD = (242, 196, 72, 245)
GOLD_DARK = (176, 128, 40, 245)


def rune(x, y):
    """Whether (x, y) is on the glyph: a ring with a three-pointed star inside."""
    cx = cy = 7.5
    r = math.hypot(x - cx, y - cy)
    if 4.6 <= r <= 5.6:
        return True
    for k in range(3):
        a = math.radians(-90 + 120 * k)
        # distance from the spoke through the centre at angle a, only within the ring
        dx, dy = math.cos(a), math.sin(a)
        t = (x - cx) * dx + (y - cy) * dy
        d = abs((x - cx) * dy - (y - cy) * dx)
        if 0 <= t <= 4.6 and d <= 0.55:
            return True
    return r <= 1.0


def seal(greater):
    rows = []
    for y in range(N):
        row = []
        for x in range(N):
            edge = min(x, y, N - 1 - x, N - 1 - y)
            if greater and edge == 0:
                px = GOLD if (x + y) % 3 else GOLD_DARK
            elif greater and edge == 1:
                px = EDGE
            elif not greater and edge == 0:
                px = EDGE
            elif rune(x, y):
                px = RUNE
            else:
                px = FIELD if (x * 7 + y * 3) % 5 else FIELD_DARK
            row.append(px)
        rows.append(row)
    return rows


def broken():
    """Just the corners of the frame, faint: what's left once a seal shatters."""
    rows = []
    for y in range(N):
        row = []
        for x in range(N):
            edge = min(x, y, N - 1 - x, N - 1 - y)
            corner = min(x, N - 1 - x) <= 3 and min(y, N - 1 - y) <= 3
            if edge == 0 and corner:
                row.append((170, 110, 255, 110))
            elif (x * 5 + y * 11) % 23 == 0:
                row.append((200, 160, 255, 50))
            else:
                row.append((0, 0, 0, 0))
        rows.append(row)
    return rows


def main():
    os.makedirs(OUT, exist_ok=True)
    pngio.write_png(os.path.join(OUT, 'lesser_arcane_seal.png'), N, N, seal(False))
    pngio.write_png(os.path.join(OUT, 'greater_arcane_seal.png'), N, N, seal(True))
    pngio.write_png(os.path.join(OUT, 'arcane_seal_open.png'), N, N, broken())


if __name__ == '__main__':
    main()
