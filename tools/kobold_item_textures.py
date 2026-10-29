"""Item and block textures for the kobolds (docs/mobs/kobolds.md).

    python3 tools/kobold_item_textures.py

Writes (16x16):
- textures/item/kobold_sling.png: a crude sling of knotted hide with a red-daubed pouch
- textures/item/kobold_scale.png: a single rust-red scale
- textures/item/kobold_trinket.png: a little bundle of odds and ends on a string
- textures/block/kobold_snare.png: leaf litter over dirt with a loop of cord hidden in it
- textures/block/dragon_tribute_bowl_{side,top,inside}.png: a basalt bowl with a gold rim
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
TEX = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures')
CLEAR = (0, 0, 0, 0)


def canvas():
    return [[CLEAR] * 16 for _ in range(16)]


def put(px, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        px[y][x] = c if len(c) == 4 else (*c, 255)


def mix(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def jitter(rng, c, n):
    j = rng.randint(-n, n)
    return tuple(max(0, min(255, v + j)) for v in c)


def write(path, px):
    full = os.path.join(TEX, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    pngio.write_png(full, 16, 16, px)
    print('wrote', os.path.relpath(full, ROOT))


def sling():
    rng = random.Random(1)
    px = canvas()
    hide = (110, 74, 44)
    dark = (70, 46, 26)
    # Two cords from the top-left loop down to a pouch at the bottom right.
    for i in range(10):
        put(px, 2 + i, 2 + i // 2, jitter(rng, hide, 8))
        put(px, 2 + i // 2, 2 + i, jitter(rng, hide, 8))
    put(px, 1, 1, dark)
    put(px, 2, 1, dark)
    put(px, 1, 2, dark)
    for y in range(8, 14):
        for x in range(8, 14):
            if (x - 10.5) ** 2 + (y - 10.5) ** 2 <= 9:
                c = jitter(rng, (128, 86, 52), 8)
                if abs(x - y) <= 1:
                    c = jitter(rng, (170, 40, 32), 10)  # a red daub across the pouch
                put(px, x, y, c)
    for x, y in ((9, 8), (12, 9), (13, 12), (8, 12)):
        put(px, x, y, dark)
    write('item/kobold_sling.png', px)


def scale():
    rng = random.Random(2)
    px = canvas()
    base = (156, 62, 40)
    for y in range(2, 15):
        half = min(y - 1, 15 - y) * 0.7 + 1.5
        for x in range(16):
            if abs(x - 7.5) <= half:
                c = jitter(rng, base, 8)
                if abs(x - 7.5) > half - 1.2:
                    c = mix(c, (80, 28, 18), 0.6)
                elif x < 7 and y < 8:
                    c = mix(c, (230, 150, 110), 0.35)  # shine
                if abs(x - 7.5) < 0.6 and y > 3:
                    c = mix(c, (100, 36, 22), 0.4)  # the scale's ridge
                put(px, x, y, c)
    write('item/kobold_scale.png', px)


def trinket():
    rng = random.Random(3)
    px = canvas()
    cord = (150, 120, 80)
    for x in range(2, 14):
        put(px, x, 3 + (abs(x - 8) // 3), jitter(rng, cord, 6))
    # A tooth, a bell, a bead and a coin hanging off the string.
    for y in range(5, 11):
        put(px, 4, y, (232, 224, 200))
    put(px, 3, 5, (232, 224, 200))
    for y in range(6, 10):
        for x in range(7, 10):
            put(px, x, y, jitter(rng, (196, 156, 60), 10))
    put(px, 8, 10, (120, 90, 30))
    for y in range(6, 9):
        for x in range(11, 13):
            put(px, x, y, jitter(rng, (70, 130, 160), 8))
    for y in range(10, 14):
        for x in range(10, 14):
            if (x - 11.5) ** 2 + (y - 11.5) ** 2 <= 3:
                put(px, x, y, jitter(rng, (236, 196, 64), 8))
    put(px, 11, 11, (170, 120, 30))
    write('item/kobold_trinket.png', px)


def snare():
    rng = random.Random(4)
    px = canvas()
    dirt = (110, 80, 56)
    leaves = [(120, 96, 52), (140, 104, 48), (96, 84, 44), (150, 120, 70), (104, 70, 40)]
    for y in range(16):
        for x in range(16):
            c = jitter(rng, dirt, 8)
            if rng.random() < 0.72:
                c = jitter(rng, rng.choice(leaves), 10)
            put(px, x, y, c)
    # A loop of cord, mostly hidden under the litter.
    for i in range(40):
        a = i / 40 * 6.283
        x = round(7.5 + 4.6 * __import__('math').cos(a))
        y = round(7.5 + 4.6 * __import__('math').sin(a))
        if rng.random() < 0.55:
            put(px, x, y, jitter(rng, (178, 156, 112), 6))
    write('block/kobold_snare.png', px)


def bowl():
    rng = random.Random(5)
    basalt = (70, 68, 72)
    gold = (230, 184, 60)
    side = canvas()
    for y in range(16):
        for x in range(16):
            c = jitter(rng, basalt, 10)
            if y % 4 == 0:
                c = mix(c, (40, 38, 42), 0.4)
            if y <= 1:
                c = jitter(rng, gold, 12)  # the gold rim
            put(side, x, y, c)
    write('block/dragon_tribute_bowl_side.png', side)
    top = canvas()
    for y in range(16):
        for x in range(16):
            put(top, x, y, jitter(rng, gold, 12) if x in (0, 15) or y in (0, 15) else jitter(rng, basalt, 10))
    write('block/dragon_tribute_bowl_top.png', top)
    inside = canvas()
    for y in range(16):
        for x in range(16):
            c = jitter(rng, mix(basalt, (0, 0, 0), 0.3), 8)
            if rng.random() < 0.06:
                c = jitter(rng, gold, 20)  # a few coins left in the bottom
            put(inside, x, y, c)
    write('block/dragon_tribute_bowl_inside.png', inside)


if __name__ == '__main__':
    sling()
    scale()
    trinket()
    snare()
    bowl()
