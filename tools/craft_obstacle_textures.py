"""Generates the craft and element obstacle block textures (Artificer, Alchemist, Warlock).

    python3 tools/craft_obstacle_textures.py

16x16: a brass Clockwork Door with a gear and rivets, translucent amber Alchemical Seal resin with bubbles,
a Miasma Vent grate (glowing green while it vents, dark once neutralised) and its iron sides, and the Fire
Curtain's brazier (dark iron bowl, soul-fire coals, and cold ash once doused). The Fire Curtain's own sheets
come from fire_curtain_textures.py.
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/block')

N = 16


def shade(c, d, a=255):
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (a,)


def grid(fn):
    return [[fn(x, y) for x in range(N)] for y in range(N)]


def clockwork_door():
    """Brass plate with a darker frame, corner rivets and a toothed gear in the middle."""
    rng = random.Random(11)
    brass = (196, 150, 64)

    def px(x, y):
        if x in (0, 15) or y in (0, 15):
            return shade((120, 84, 36), rng.randint(-6, 6))
        if (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
            return (236, 206, 120, 255)
        dx, dy = x - 7.5, y - 7.5
        r = math.hypot(dx, dy)
        angle = math.atan2(dy, dx)
        teeth = 4.6 + (0.9 if math.cos(angle * 8) > 0.2 else 0.0)
        if r < 1.4:
            return (70, 48, 22, 255)
        if r < 2.4:
            return shade((236, 200, 110), rng.randint(-6, 6))
        if r <= teeth:
            spoke = min(abs(dx), abs(dy)) < 0.8
            return shade((222, 178, 84) if spoke or r > 3.6 else (150, 108, 44), rng.randint(-8, 8))
        if r <= teeth + 0.8:
            return (98, 68, 30, 255)
        return shade(brass, rng.randint(-12, 10))
    return grid(px)


def alchemical_seal():
    """Amber resin, mostly see-through, with darker streaks and trapped bubbles."""
    rng = random.Random(5)
    bubbles = {(3, 4), (4, 4), (11, 2), (12, 9), (6, 11), (7, 11), (9, 6), (2, 13), (13, 13)}

    def px(x, y):
        if (x, y) in bubbles:
            return (255, 236, 170, 230)
        streak = (x + y * 2) % 7 == 0
        base = (196, 112, 18) if streak else (232, 150, 34)
        return shade(base, rng.randint(-14, 10), 225 if streak else 205)
    return grid(px)


def vent_top(active):
    """An iron grate over a pit: slats with glowing green gas (or dark, once neutralised) between them."""
    rng = random.Random(17)

    def px(x, y):
        if x in (0, 15) or y in (0, 15) or x in (1, 14) or y in (1, 14):
            return shade((88, 92, 96), rng.randint(-8, 8))
        if y % 3 == 1:
            return shade((112, 116, 120), rng.randint(-10, 8))
        if active:
            glow = 30 - abs(x - 7) * 3
            return shade((96, 180, 52), rng.randint(-20, 10) + glow)
        return shade((26, 28, 26), rng.randint(-4, 6))
    return grid(px)


def vent_side():
    """Riveted dark iron plating."""
    rng = random.Random(23)

    def px(x, y):
        if y in (0, 15) or x in (0, 15):
            return shade((60, 62, 66), rng.randint(-6, 6))
        if (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
            return (140, 144, 150, 255)
        if y == 7:
            return shade((74, 104, 50), rng.randint(-6, 6))
        return shade((84, 86, 92), rng.randint(-10, 8))
    return grid(px)


def brazier_side():
    """A dark iron bowl with two bands and a few soul-fire glints."""
    rng = random.Random(29)

    def px(x, y):
        if y in (2, 3, 12, 13):
            return shade((58, 60, 66), rng.randint(-6, 6))
        if (x, y) in ((4, 7), (11, 8), (7, 9)):
            return (110, 230, 236, 255)
        return shade((38, 38, 44), rng.randint(-8, 8))
    return grid(px)


def brazier_top(lit):
    """Soul-fire coals glowing cyan, or grey ash once doused."""
    rng = random.Random(31)

    def px(x, y):
        if x in (0, 15) or y in (0, 15):
            return shade((50, 52, 58), rng.randint(-6, 6))
        if lit:
            hot = rng.random() < 0.35
            return shade((120, 236, 240) if hot else (40, 120, 140), rng.randint(-14, 14))
        return shade((104, 102, 98), rng.randint(-18, 12))
    return grid(px)


TEXTURES = {
    'clockwork_door': clockwork_door,
    'alchemical_seal': alchemical_seal,
    'miasma_vent_top': lambda: vent_top(True),
    'miasma_vent_top_open': lambda: vent_top(False),
    'miasma_vent_side': vent_side,
    'fire_curtain_brazier_side': brazier_side,
    'fire_curtain_brazier_top': lambda: brazier_top(True),
    'fire_curtain_brazier_top_doused': lambda: brazier_top(False),
}


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, make in TEXTURES.items():
        path = os.path.join(OUT, name + '.png')
        pngio.write_png(path, N, N, make())
        print('wrote', os.path.relpath(path, ROOT))


if __name__ == '__main__':
    main()
