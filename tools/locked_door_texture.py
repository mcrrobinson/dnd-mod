"""Generates the Locked Door obstacle texture: a riveted iron plate with a keyhole.

    python3 tools/locked_door_texture.py
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/block')

N = 16
IRON = (196, 196, 196)
DARK = (120, 120, 124)
RIVET = (232, 232, 236)
HOLE = (24, 22, 26)
BRASS = (196, 160, 72)


def shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)


def locked_door():
    rng = random.Random(11)
    keyhole = {(7, 6), (8, 6), (7, 7), (8, 7), (7, 8), (8, 8), (7, 9), (8, 9), (7, 10), (8, 10)}
    keyhole_round = {(6, 6), (9, 6), (6, 7), (9, 7)}
    plate = {(x, y) for x in range(5, 11) for y in range(4, 13)}

    def px(x, y):
        if x in (0, 15) or y in (0, 15):
            return shade(DARK, -10)
        if (x, y) in keyhole:
            return HOLE + (255,)
        if (x, y) in keyhole_round and y == 6:
            return HOLE + (255,)
        if (x, y) in plate:
            return shade(BRASS, rng.randint(-14, 10))
        if (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13), (2, 7), (13, 7)):
            return RIVET + (255,)
        if y in (5, 11) and x not in range(5, 11):
            return shade(DARK, rng.randint(-6, 6))
        return shade(IRON, rng.randint(-12, 8))

    return [[px(x, y) for x in range(N)] for y in range(N)]


def main():
    pngio.write_png(os.path.join(OUT, 'locked_door.png'), N, N, locked_door())


if __name__ == '__main__':
    main()
