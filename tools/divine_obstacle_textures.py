"""Generates the divine and death obstacle block textures (Cleric, Paladin, Necromancer, Blood Hunter).

    python3 tools/divine_obstacle_textures.py

16x16: a gold-glyph Holy Ward in pale stone, a Desecrated Altar (cracked blackstone side, soul-fire
top) and its consecrated look, black-bannered Dread Ward stone, an Ossuary Gate of stacked bone with
a skull, and a dripping red Blood Seal sigil.
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


def holy_ward():
    """Pale limestone with a gold sunburst glyph and a gold rim."""
    rng = random.Random(3)
    glyph = set()
    for i in range(4, 12):
        glyph.add((i, 7))
        glyph.add((i, 8))
        glyph.add((7, i))
        glyph.add((8, i))
    for d in range(3, 6):
        for sx, sy in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
            glyph.add((int(7.5 + sx * d), int(7.5 + sy * d)))

    def px(x, y):
        if x in (0, 15) or y in (0, 15):
            return (214, 170, 58, 255)
        if (x, y) in glyph:
            return shade((246, 206, 92), rng.randint(-8, 8))
        return shade((222, 214, 196), rng.randint(-10, 8))
    return grid(px)


def altar_side(consecrated):
    """Blackstone with cracks; red-orange cracks while desecrated, pale gold once consecrated."""
    rng = random.Random(7)
    cracks = {(3, 2), (4, 3), (4, 4), (5, 5), (11, 3), (12, 4), (12, 5), (11, 6), (6, 10), (7, 11), (8, 11),
              (9, 12), (2, 12), (3, 13), (13, 11), (13, 12)}
    crack = (236, 214, 140) if consecrated else (64, 200, 214)

    def px(x, y):
        if y in (0, 15):
            return (40, 34, 40, 255) if not consecrated else (120, 108, 92, 255)
        if (x, y) in cracks:
            return crack + (255,)
        return shade((46, 40, 48), rng.randint(-8, 10))
    return grid(px)


def altar_top(consecrated):
    """Soul fire (cyan) in a blackstone bowl, or a gold cross on clean stone."""
    rng = random.Random(9)

    def px(x, y):
        if x in (0, 15) or y in (0, 15):
            return (34, 30, 36, 255)
        d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
        if consecrated:
            if x in (7, 8) and 3 <= y <= 12 or y in (6, 7) and 4 <= x <= 11:
                return (240, 206, 96, 255)
            return shade((196, 188, 172), rng.randint(-8, 8))
        if d < 4.5:
            return shade((90, 220, 230) if rng.random() < 0.6 else (40, 140, 160), rng.randint(-10, 10))
        return shade((46, 40, 48), rng.randint(-8, 8))
    return grid(px)


def dread_ward():
    """Dark deepslate bricks with a hanging black banner and a pale eye."""
    rng = random.Random(13)

    def px(x, y):
        if 4 <= x <= 11 and y <= 13:
            if y == 13 and x % 2 == 0:
                return (10, 8, 12, 255)
            if (x - 7.5) ** 2 / 4 + (y - 6) ** 2 < 2.2:
                return (180, 172, 150, 255) if abs(x - 7.5) > 0.6 else (20, 14, 20, 255)
            return shade((22, 18, 26), rng.randint(-4, 4))
        mortar = y in (0, 7, 8, 15) or (y < 8 and x in (0, 8)) or (y >= 8 and x in (4, 12))
        return (34, 34, 38, 255) if mortar else shade((62, 62, 68), rng.randint(-8, 8))
    return grid(px)


def ossuary_gate():
    """Stacked bones on their sides, with a skull in the middle."""
    rng = random.Random(17)
    skull = {(x, y) for x in range(5, 11) for y in range(4, 10)} - {(5, 4), (10, 4), (5, 9), (10, 9)}
    eyes = {(6, 6), (6, 7), (9, 6), (9, 7)}
    teeth = {(6, 9), (8, 9), (10, 9)}

    def px(x, y):
        if (x, y) in eyes:
            return (24, 20, 18, 255)
        if (x, y) in teeth:
            return (60, 54, 44, 255)
        if (x, y) in skull or (6 <= x <= 9 and y == 10):
            return shade((232, 226, 204), rng.randint(-6, 6))
        if y % 4 == 3:
            return (78, 70, 56, 255)
        end = x % 8 in (0, 7)
        return shade((214, 206, 180) if not end else (238, 232, 212), rng.randint(-10, 6))
    return grid(px)


def blood_seal():
    """A dark red slab with a bright blood sigil and drips running down."""
    rng = random.Random(21)
    sigil = set()
    for i in range(3, 13):
        sigil.add((i, 3))
        sigil.add((3, i))
        sigil.add((12, i))
        sigil.add((i, 12))
    for i in range(5, 11):
        sigil.add((i, i))
        sigil.add((15 - i, i))
    drips = {(5, 13), (5, 14), (10, 13), (10, 14), (10, 15), (13, 6), (13, 7), (2, 9)}

    def px(x, y):
        if (x, y) in sigil or (x, y) in drips:
            return shade((196, 18, 26), rng.randint(-14, 10))
        return shade((70, 14, 18), rng.randint(-10, 8))
    return grid(px)


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, rows in [('holy_ward', holy_ward()),
                       ('desecrated_altar_side', altar_side(False)), ('desecrated_altar_top', altar_top(False)),
                       ('consecrated_altar_side', altar_side(True)), ('consecrated_altar_top', altar_top(True)),
                       ('dread_ward', dread_ward()), ('ossuary_gate', ossuary_gate()),
                       ('blood_seal', blood_seal())]:
        pngio.write_png(os.path.join(OUT, name + '.png'), N, N, rows)


if __name__ == '__main__':
    main()
