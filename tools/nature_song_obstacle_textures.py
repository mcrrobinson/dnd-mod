"""Generates the nature and song obstacle block textures (Druid, Bard).

    python3 tools/nature_song_obstacle_textures.py

16x16: a Guardian Beast tether post (dark wood with an iron ring and taut chain, and a slack, unhooked chain
once the beasts are calmed), a dense Thornwall of dark vines with pale thorns, and a Singing Door: a stone face
with an open, singing mouth.
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/block')

N = 16


def shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)


def grid(fn):
    return [[fn(x, y) for x in range(N)] for y in range(N)]


def guardian_post(slack):
    """Dark spruce post with grain, an iron ring, and a chain hanging taut (or slack and unhooked)."""
    rng = random.Random(31)
    ring = {(x, y) for x in range(5, 11) for y in range(3, 6)} - {(6, 4), (7, 4), (8, 4), (9, 4)}
    if slack:
        chain = {(7, y) for y in range(6, 12)} | {(8, 12), (9, 13), (9, 14), (8, 15)}
    else:
        chain = {(7, y) for y in range(6, 16)} | {(8, y) for y in range(6, 16, 2)}

    def px(x, y):
        if (x, y) in chain:
            return shade((92, 96, 104) if (x + y) % 2 else (140, 144, 152), rng.randint(-6, 6))
        if (x, y) in ring:
            return shade((120, 124, 132), rng.randint(-8, 8))
        grain = x in (2, 6, 11, 14)
        return shade((74, 52, 32) if grain else (96, 70, 44), rng.randint(-8, 6))
    return grid(px)


def thornwall():
    """Tangled dark green vines over shadow, with pale thorns and a few red berries."""
    rng = random.Random(37)
    vines = set()
    for start in range(-8, 16, 4):
        for i in range(N):
            vines.add(((start + i) % N, i))
            vines.add(((start + 2 - i) % N, i))
    thorns = {(rng.randrange(N), rng.randrange(N)) for _ in range(14)}
    berries = {(3, 5), (12, 9), (7, 13)}

    def px(x, y):
        if (x, y) in berries:
            return (176, 30, 36, 255)
        if (x, y) in thorns:
            return shade((214, 204, 160), rng.randint(-10, 6))
        if (x, y) in vines:
            return shade((58, 92, 36), rng.randint(-12, 10))
        return shade((22, 38, 18), rng.randint(-6, 6))
    return grid(px)


def singing_door():
    """Grey stone blocks with a carved face: brows, closed eyes and a round, singing mouth."""
    rng = random.Random(41)
    brows = {(3, 4), (4, 3), (5, 3), (6, 4), (9, 4), (10, 3), (11, 3), (12, 4)}
    eyes = {(4, 6), (5, 6), (10, 6), (11, 6)}
    mouth_rim = {(x, y) for x in range(5, 11) for y in range(9, 15)
                 if 1.6 <= ((x - 7.5) ** 2 / 1.0 + (y - 11.5) ** 2 / 1.4) ** 0.5 <= 2.9}
    mouth = {(x, y) for x in range(5, 11) for y in range(9, 15)
             if ((x - 7.5) ** 2 / 1.0 + (y - 11.5) ** 2 / 1.4) ** 0.5 < 1.6}

    def px(x, y):
        if (x, y) in mouth:
            return (24, 22, 30, 255)
        if (x, y) in mouth_rim or (x, y) in brows or (x, y) in eyes:
            return shade((70, 70, 76), rng.randint(-6, 6))
        if x in (0, 15) or y in (0, 15):
            return (88, 88, 92, 255)
        if (x, y) in {(7, 7), (8, 7), (7, 8), (8, 8)}:
            return shade((104, 104, 110), rng.randint(-4, 4))
        return shade((136, 136, 140), rng.randint(-10, 8))
    return grid(px)


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, rows in [('guardian_post', guardian_post(False)), ('guardian_post_open', guardian_post(True)),
                       ('thornwall', thornwall()), ('singing_door', singing_door())]:
        pngio.write_png(os.path.join(OUT, name + '.png'), N, N, rows)


if __name__ == '__main__':
    main()
