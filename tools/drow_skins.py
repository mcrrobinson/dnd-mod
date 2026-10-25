"""Generates the drow NPC skins (64x64 player-model layout, slim Alex arms), based on elf_skins.py.

    python3 tools/drow_skins.py

Writes textures/entity/drow/:
- drow_0..5.png: warriors (obsidian-to-slate skin, white or silver hair, violet or red eyes, black and purple
  leathers with silver trim);
- mage_0..1.png: a hooded robe in deep violet with silver runes;
- priestess_0..1.png: a black robe with a red spider emblem on the chest and a silver circlet;
- elite.png: the Spire guard, black leathers under a silver-trimmed mantle (the chain shirt is the armour layer).
The ears come from RaceFeatures, which textures them from the head's side pixels (dark skin), and the spare corner
at (56, 0) is painted with skin too.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402
from elf_skins import Skin, shade, darker, N  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/entity/drow')

SKINS = [(44, 40, 58), (58, 54, 74), (36, 34, 46), (70, 66, 86), (50, 44, 66), (62, 60, 70)]
HAIR = [(236, 236, 244), (198, 202, 214), (226, 224, 236), (176, 180, 196), (244, 240, 250), (210, 210, 224)]
EYES = [(200, 60, 70), (170, 90, 220), (220, 40, 50), (150, 80, 210), (230, 70, 90), (190, 110, 240)]
LEATHER = [(28, 24, 32), (40, 26, 52), (22, 20, 26), (52, 30, 66), (30, 26, 40), (46, 24, 44)]
TRIM = (190, 196, 210)


def make(skin, hair, eyes, cloth, trim, kind='warrior'):
    s = Skin()
    hooded = kind == 'mage'

    def head(face, x, y, w, h):
        if face == 'top':
            return shade(cloth if hooded else hair, x, y)
        if face == 'bottom':
            return shade(skin, x, y)
        if face == 'back':
            return shade(cloth if hooded and y < 6 else hair if y < 7 else skin, x, y)
        if face in ('left', 'right'):
            back = x >= 4 if face == 'right' else x < 4
            if hooded and (y < 3 or back):
                return shade(cloth, x, y)
            if y < 2 or back and y < 7:
                return shade(hair, x, y)
            return shade(skin, x, y, 3)
        # Front: white hair parted in the middle (or the hood's rim), pale brows, glowing eyes, a dark mouth.
        if hooded and (y == 0 or x in (0, 7) and y < 6):
            return shade(cloth, x, y)
        if y == 0 or y == 1 and x not in (3, 4):
            return shade(hair, x, y)
        if y == 1 or y == 2 and x in (0, 7):
            return shade(hair, x, y)
        if y == 3 and x in (1, 2, 5, 6):
            return shade(darker(hair, 0.85), x, y)
        if y == 4 and x in (1, 6):
            return (20, 18, 24, 255)
        if y == 4 and x in (2, 5):
            return eyes + (255,)
        if y == 6 and x in (3, 4):
            return darker(skin, 0.7) + (255,)
        return shade(skin, x, y, 3)
    s.box(0, 0, 8, 8, 8, head)

    def hat(face, x, y, w, h):
        if kind == 'priestess' and face in ('front', 'left', 'right', 'back') and y == 2:
            return (214, 220, 232, 255) if (face != 'front' or x not in (3, 4)) else (200, 40, 50, 255)
        if hooded and face == 'front' and y < 2:
            return shade(darker(cloth, 0.8), x, y)
        if face == 'back' and y >= 6 and not hooded:
            return shade(hair, x, y)
        return None
    s.box(32, 0, 8, 8, 8, hat)

    for y in range(4):
        for x in range(4):
            s.put(56 + x, y, shade(skin, x, y, 2))

    def body(face, x, y, w, h):
        if face in ('top', 'bottom'):
            return shade(cloth, x, y)
        if kind == 'priestess' and face == 'front':
            # A red spider: a round body, a head and four legs a side.
            if (x, y) in ((3, 3), (4, 3), (3, 4), (4, 4), (3, 5), (4, 5), (3, 2), (4, 2)):
                return shade((176, 30, 40), x, y)
            if y in (3, 5) and x in (1, 2, 5, 6) or y in (2, 6) and x in (0, 7):
                return shade((140, 24, 34), x, y)
        if kind == 'elite' and y < 3:
            # A mantle over the shoulders.
            return shade(trim if y == 2 else (60, 56, 70), x, y)
        if y == 0 and face == 'front':
            return shade(trim, x, y)
        if face == 'front' and x in (3, 4) and y < 7 and kind in ('warrior', 'elite'):
            return shade(trim if y < 2 else darker(cloth, 0.8), x, y)
        if y == 7:
            return shade(trim if kind in ('priestess', 'mage') else darker(cloth, 0.6), x, y)
        if kind == 'mage' and face == 'front' and (x + y) % 5 == 0 and y > 1:
            return shade((170, 150, 220), x, y)
        return shade(cloth, x, y)
    s.box(16, 16, 8, 12, 4, body)

    def arm(face, x, y, w, h):
        if face == 'bottom' or y >= 10:
            return shade(skin, x, y, 3)
        if y == 9:
            return shade(trim, x, y)
        return shade(cloth if kind != 'warrior' or y < 4 else darker(cloth, 0.85), x, y)
    s.box(40, 16, 3, 12, 4, arm)
    s.box(32, 48, 3, 12, 4, arm)

    robe = kind in ('mage', 'priestess')

    def leg(face, x, y, w, h):
        if face == 'bottom' or y >= 10:
            return shade((18, 16, 20), x, y)
        if robe and y < 9:
            return shade(trim if y == 8 else cloth, x, y)
        return shade(darker(cloth, 0.75), x, y)
    s.box(0, 16, 4, 12, 4, leg)
    s.box(16, 48, 4, 12, 4, leg)
    return s.px


def main():
    os.makedirs(OUT, exist_ok=True)
    for i in range(6):
        px = make(SKINS[i], HAIR[i], EYES[i], LEATHER[i], TRIM)
        pngio.write_png(os.path.join(OUT, 'drow_%d.png' % i), N, N, px)
    mage = [((44, 40, 58), (236, 236, 244), (170, 90, 220), (58, 30, 92)),
            ((62, 60, 70), (198, 202, 214), (200, 60, 70), (40, 24, 70))]
    for i, (skin, hair, eyes, cloth) in enumerate(mage):
        pngio.write_png(os.path.join(OUT, 'mage_%d.png' % i), N, N, make(skin, hair, eyes, cloth, TRIM, 'mage'))
    priestess = [((50, 44, 66), (244, 240, 250), (220, 40, 50), (20, 16, 24)),
                 ((36, 34, 46), (226, 224, 236), (190, 110, 240), (34, 18, 30))]
    for i, (skin, hair, eyes, cloth) in enumerate(priestess):
        pngio.write_png(os.path.join(OUT, 'priestess_%d.png' % i), N, N,
                        make(skin, hair, eyes, cloth, (200, 200, 214), 'priestess'))
    pngio.write_png(os.path.join(OUT, 'elite.png'), N, N,
                    make((40, 36, 52), (240, 240, 248), (220, 40, 60), (24, 20, 30), TRIM, 'elite'))
    print('wrote', OUT)


if __name__ == '__main__':
    main()
