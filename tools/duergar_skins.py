"""Generates the duergar skins (64x64 player-model layout, classic Steve arms; drawn on the mountain dwarf's
reshaped model by DuergarRenderer).

    python3 tools/duergar_skins.py

Writes textures/entity/duergar/duergar_0..5.png (soldiers and arbalesters: ash-grey skin, bald heads, black or
grey beards, soot-dark plate with red-hot rivets; the variant order of DuergarEntity), factor.png (a leather apron
and a gold chain over grey robes) and taskmaster.png (heavier plate with a red sash and a spiked iron cap).
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/entity/duergar')

N = 64
CLEAR = (0, 0, 0, 0)

# Ash-grey skins, light to dark, in DuergarEntity variant order.
SKINS = [(132, 128, 124), (116, 112, 110), (146, 140, 132), (104, 102, 104), (124, 118, 112), (96, 92, 90)]
BEARDS = [(34, 32, 32), (150, 148, 144), (22, 20, 20), (110, 108, 106), (46, 40, 38), (180, 176, 170)]
# Eyes: dull red, ember, pale grey.
EYES = [(190, 50, 40), (220, 120, 40), (200, 200, 196), (190, 50, 40), (220, 120, 40), (170, 40, 40)]
PLATE = [(58, 56, 56), (50, 48, 50), (64, 60, 58), (46, 44, 46), (60, 56, 52), (54, 52, 54)]
RIVET = (232, 96, 40)
LEATHER = (66, 46, 34)


def shade(color, x, y, amount=6):
    """A little per-pixel variation so flat colours don't look like plastic."""
    n = ((x * 73 + y * 151) ^ (x * y * 7)) % 5 - 2
    return tuple(max(0, min(255, c + n * amount // 2)) for c in color[:3]) + (255,)


def darker(color, f=0.75):
    return tuple(int(c * f) for c in color[:3])


class Skin:
    def __init__(self):
        self.px = [[CLEAR for _ in range(N)] for _ in range(N)]

    def put(self, x, y, color):
        if 0 <= x < N and 0 <= y < N:
            self.px[y][x] = color if len(color) == 4 else color + (255,)

    def box(self, u, v, w, h, d, paint):
        """Paints a cuboid's six faces; paint(face, x, y, width, height) gives a colour or None."""
        faces = {
            'top': (u + d, v, w, d),
            'bottom': (u + d + w, v, w, d),
            'right': (u, v + d, d, h),
            'front': (u + d, v + d, w, h),
            'left': (u + d + w, v + d, d, h),
            'back': (u + 2 * d + w, v + d, w, h),
        }
        for face, (fx, fy, fw, fh) in faces.items():
            for y in range(fh):
                for x in range(fw):
                    c = paint(face, x, y, fw, fh)
                    if c is not None:
                        self.put(fx + x, fy + y, c)


def make(skin, beard, eyes, plate, kind='soldier'):
    s = Skin()

    def head(face, x, y, w, h):
        # Bald: the whole scalp is skin, a little darker on top.
        if face == 'top':
            return shade(darker(skin, 0.9), x, y)
        if face == 'bottom':
            return shade(beard, x, y)
        if face in ('left', 'right'):
            # Beard down the jaw.
            return shade(beard if y >= 5 else skin, x, y)
        if face == 'back':
            return shade(skin, x, y)
        # Front: a heavy brow, small glowing eyes, a long beard from the cheeks down.
        if y == 2 and 1 <= x <= 6:
            return shade(darker(skin, 0.6), x, y)
        if y == 3 and x in (1, 2, 5, 6):
            return eyes + (255,) if x in (2, 5) else shade(darker(skin, 0.7), x, y)
        if y == 4 and 3 <= x <= 4:
            return shade(darker(skin, 0.8), x, y)
        if y >= 5:
            return shade(beard, x, y)
        if y == 4 and x in (0, 1, 6, 7):
            return shade(beard, x, y)
        return shade(skin, x, y)
    s.box(0, 0, 8, 8, 8, head)

    def hat(face, x, y, w, h):
        # Beard braid hanging over the chest (front, lower rows); the taskmaster's spiked iron cap.
        if kind == 'taskmaster' and face != 'bottom' and (face == 'top' or y <= 2):
            if face == 'top' and x in (3, 4) and y in (3, 4):
                return (200, 200, 200, 255)
            return shade((70, 70, 74), x, y)
        if face == 'front' and y == 7 and x in (2, 5):
            return shade(darker(beard, 0.8), x, y)
        return None
    s.box(32, 0, 8, 8, 8, hat)

    def body(face, x, y, w, h):
        if face in ('top', 'bottom'):
            return shade(plate, x, y)
        if face == 'front' and y < 4 and 2 <= x <= 5:
            # The beard over the chest.
            return shade(beard, x, y)
        if kind == 'factor':
            if face == 'front' and y == 4 and 1 <= x <= 6:
                return shade((214, 176, 64), x, y)  # a gold chain
            if face == 'front' and y >= 5:
                return shade(LEATHER, x, y)  # the apron
            return shade((92, 90, 92), x, y)
        if kind == 'taskmaster' and y == 7:
            return shade((150, 30, 26), x, y)  # a red sash
        if y == 8:
            return shade(LEATHER, x, y)
        if (x + y) % 4 == 0 and y in (1, 5, 10):
            return RIVET + (255,)
        return shade(plate, x, y)
    s.box(16, 16, 8, 12, 4, body)

    def arm(face, x, y, w, h):
        if face == 'bottom' or y >= 10:
            return shade(darker(skin, 0.85), x, y)
        if kind == 'factor':
            return shade((92, 90, 92), x, y)
        if y < 4:
            return RIVET + (255,) if (face == 'front' and x == 1 and y == 2) else shade(plate, x, y)
        if y >= 8:
            return shade(LEATHER, x, y)
        return shade(darker(plate, 1.1), x, y)
    s.box(40, 16, 4, 12, 4, arm)
    s.box(32, 48, 4, 12, 4, arm)

    def leg(face, x, y, w, h):
        if face == 'bottom' or y >= 9:
            return shade((30, 26, 24), x, y)
        if kind == 'factor':
            return shade(LEATHER if face == 'front' and y < 4 else (80, 78, 80), x, y)
        return shade(darker(plate, 0.85), x, y)
    s.box(0, 16, 4, 12, 4, leg)
    s.box(16, 48, 4, 12, 4, leg)
    return s.px


def main():
    os.makedirs(OUT, exist_ok=True)
    for i in range(6):
        pngio.write_png(os.path.join(OUT, 'duergar_%d.png' % i), N, N, make(SKINS[i], BEARDS[i], EYES[i], PLATE[i]))
    pngio.write_png(os.path.join(OUT, 'factor.png'), N, N,
                    make(SKINS[2], BEARDS[1], EYES[2], PLATE[0], 'factor'))
    pngio.write_png(os.path.join(OUT, 'taskmaster.png'), N, N,
                    make(SKINS[5], BEARDS[2], EYES[0], (40, 38, 40), 'taskmaster'))
    print('wrote', OUT)


if __name__ == '__main__':
    main()
