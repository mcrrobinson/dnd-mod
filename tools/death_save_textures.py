"""Generates the death save pips for the downed HUD.

    python3 tools/death_save_textures.py

One 36x9 strip of four 9x9 icons: a green pass gem, an empty pass socket, a red fail skull and an
empty skull outline. DownedHud draws them under "DOWNED": three passes, then three fails.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/gui/death_saves.png')

N = 9
CLEAR = (0, 0, 0, 0)
OUTLINE = (20, 12, 12, 255)

# A 9x9 skull: '#' bone, 'o' eye socket / nose, '.' clear
SKULL = [
    "..#####..",
    ".#######.",
    "#########",
    "#oo###oo#",
    "#oo###oo#",
    "####o####",
    ".#######.",
    "..#.#.#..",
    "..#####..",
]


def gem(x, y, full):
    """A round pass pip: a shaded green gem, or a dark empty socket."""
    cx = cy = 4.0
    r = math.hypot(x - cx, y - cy)
    if r > 4.3:
        return CLEAR
    if r > 3.3:
        return OUTLINE
    if not full:
        return (60, 60, 60, 200)
    light = max(0.0, 1.0 - math.hypot(x - 3.0, y - 3.0) / 4.0)
    return (int(40 + 120 * light), int(170 + 80 * light), int(50 + 100 * light), 255)


def skull(x, y, full):
    """A fail pip: a red skull, or a dark outline of one."""
    c = SKULL[y][x]
    if c == '.':
        return CLEAR
    if c == 'o':
        return OUTLINE if full else CLEAR
    edge = any(not (0 <= x + dx < N and 0 <= y + dy < N) or SKULL[y + dy][x + dx] != '#'
               for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
    if not full:
        return (70, 70, 70, 210) if edge else (40, 40, 40, 120)
    shade = 1.0 - y / 14.0
    if edge:
        return (int(120 * shade), 16, 16, 255)
    return (int(235 * shade), int(50 * shade), int(45 * shade), 255)


def main():
    icons = [lambda x, y: gem(x, y, True), lambda x, y: gem(x, y, False),
             lambda x, y: skull(x, y, True), lambda x, y: skull(x, y, False)]
    rows = []
    for y in range(N):
        row = []
        for icon in icons:
            for x in range(N):
                row.append(icon(x, y))
        rows.append(row)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    pngio.write_png(OUT, N * len(icons), N, rows)
    print('wrote', os.path.relpath(OUT, ROOT))


if __name__ == '__main__':
    main()
