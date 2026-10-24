"""Generates the human hireling skins (64x64 player-model layout, classic Steve arms).

    python3 tools/hireling_skins.py

Writes textures/entity/hireling/human_0..5.png: travelling clothes (a tunic over a shirt, a belt, trousers
and boots) in road-worn colours, short hair, some with beards. The other hireling races reuse their
settlement folk's skins (elf_skins.py, tiefling_skins.py, dragonborn_skins.py, the orc and gnome skins).
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402
from elf_skins import Skin, shade, darker, N  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/entity/hireling')

SKINS = [(232, 190, 160), (198, 150, 112), (150, 104, 70), (240, 206, 178), (112, 76, 50), (214, 170, 132)]
HAIR = [(90, 58, 32), (40, 30, 24), (20, 16, 14), (196, 150, 80), (60, 40, 28), (150, 60, 30)]
EYES = [(70, 110, 160), (90, 70, 40), (50, 40, 30), (80, 130, 90), (60, 45, 30), (90, 110, 120)]
TUNICS = [(118, 46, 40), (60, 86, 120), (86, 104, 56), (140, 110, 60), (80, 60, 92), (110, 80, 50)]
SHIRTS = [(214, 204, 180), (190, 180, 160), (200, 190, 168), (180, 170, 150), (206, 196, 176), (196, 184, 160)]
TROUSERS = [(70, 56, 44), (58, 54, 60), (84, 70, 50), (60, 60, 70), (74, 60, 48), (66, 70, 56)]
BEARD = [True, False, True, False, True, False]

BOOT = (64, 42, 26)
BELT = (52, 34, 20)
BUCKLE = (200, 170, 90)


def make(skin, hair, eyes, tunic, shirt, trousers, beard):
    s = Skin()

    def head(face, x, y, w, h):
        if face == 'top':
            return shade(hair, x, y)
        if face == 'bottom':
            return shade(skin, x, y)
        if face == 'back':
            return shade(hair if y < 5 else skin, x, y)
        if face in ('left', 'right'):
            back = x >= 5 if face == 'right' else x < 3
            if y < 2 or back and y < 5:
                return shade(hair, x, y)
            if beard and y >= 5 and (x < 3 if face == 'right' else x >= 5):
                return shade(darker(hair, 0.9), x, y)
            return shade(skin, x, y)
        # Front: short fringe, brows, eyes, nose, mouth and maybe a beard.
        if y < 2:
            return shade(hair, x, y)
        if y == 3 and x in (1, 2, 5, 6):
            return shade(darker(hair, 0.7), x, y)
        if y == 4 and x in (1, 6):
            return (245, 245, 240, 255)
        if y == 4 and x in (2, 5):
            return eyes + (255,)
        if y == 5 and x in (3, 4):
            return darker(skin, 0.88) + (255,)
        if beard and (y == 7 or y == 6 and x not in (3, 4) or y == 5 and x in (0, 7)):
            return shade(darker(hair, 0.9), x, y)
        if y == 6 and x in (3, 4):
            return darker(skin, 0.7) + (255,)
        return shade(skin, x, y, 3)
    s.box(0, 0, 8, 8, 8, head)

    def body(face, x, y, w, h):
        if face in ('top', 'bottom'):
            return shade(tunic, x, y)
        if y == 8:
            # Belt with a buckle at the front.
            return shade(BUCKLE if face == 'front' and x in (3, 4) else BELT, x, y)
        if face == 'front' and y < 3 and x in (3, 4):
            # The shirt shows at the open collar.
            return shade(shirt, x, y)
        if y >= 9:
            return shade(darker(tunic, 0.85), x, y)
        return shade(tunic, x, y)
    s.box(16, 16, 8, 12, 4, body)

    def arm(face, x, y, w, h):
        if face == 'bottom' or y >= 10:
            return shade(skin, x, y, 3)
        if y >= 5:
            return shade(shirt, x, y)
        return shade(tunic, x, y)
    s.box(40, 16, 4, 12, 4, arm)
    s.box(32, 48, 4, 12, 4, arm)

    def leg(face, x, y, w, h):
        if face == 'bottom' or y >= 8:
            return shade(BOOT if y != 8 else darker(BOOT, 0.8), x, y)
        return shade(trousers, x, y)
    s.box(0, 16, 4, 12, 4, leg)
    s.box(16, 48, 4, 12, 4, leg)
    return s.px


def main():
    os.makedirs(OUT, exist_ok=True)
    for i in range(6):
        px = make(SKINS[i], HAIR[i], EYES[i], TUNICS[i], SHIRTS[i], TROUSERS[i], BEARD[i])
        pngio.write_png(os.path.join(OUT, 'human_%d.png' % i), N, N, px)
    print('wrote', OUT)


if __name__ == '__main__':
    main()
