"""Generates the elf NPC skins (64x64 player-model layout, slim Alex arms).

    python3 tools/elf_skins.py

Writes textures/entity/elf/elf_0..5.png (wood elves and wardens: robes in greens, silver and white),
speaker.png (white and silver robes with gold trim and a silver circlet) and fletcher.png (a leather
jerkin over green, with a quiver strap). The ears used by RacialHumanoidRenderer read the skin's spare
corner at (56, 0): 4x4 pixels of skin.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/entity/elf')

N = 64
CLEAR = (0, 0, 0, 0)

SKINS = [(240, 214, 190), (226, 190, 160), (205, 168, 132), (236, 204, 176), (214, 178, 146), (244, 222, 204)]
HAIR = [(232, 206, 128), (226, 228, 232), (120, 74, 42), (40, 32, 30), (176, 92, 44), (246, 234, 186)]
ROBES = [(70, 122, 64), (176, 184, 196), (232, 232, 226), (96, 130, 70), (58, 110, 96), (150, 170, 120)]
TRIMS = [(216, 196, 120), (90, 120, 160), (120, 170, 110), (210, 190, 110), (220, 220, 230), (90, 110, 70)]
EYES = [(60, 120, 70), (70, 110, 170), (120, 90, 40), (50, 140, 140), (90, 140, 60), (110, 90, 150)]


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


def make(skin, hair, eyes, robe, trim, kind='elf'):
    s = Skin()

    def head(face, x, y, w, h):
        if face == 'top':
            return shade(hair, x, y)
        if face == 'bottom':
            return shade(skin, x, y)
        if face == 'back':
            return shade(hair if y < 7 else skin, x, y)
        if face in ('left', 'right'):
            # Long hair down the back half of the sides, swept behind the ears.
            back = x >= 4 if face == 'right' else x < 4
            if y < 2 or back and y < 7:
                return shade(hair, x, y)
            return shade(skin, x, y)
        # Front: fringe parted in the middle, brows, eyes, a small mouth.
        if y == 0 or y == 1 and x not in (3, 4):
            return shade(hair, x, y)
        if y == 1 or y == 2 and x in (0, 7):
            return shade(hair, x, y)
        if y == 3 and x in (1, 2, 5, 6):
            return shade(darker(hair, 0.7), x, y)
        if y == 4 and x in (1, 6):
            return (245, 245, 240, 255)
        if y == 4 and x in (2, 5):
            return eyes + (255,)
        if y == 6 and x in (3, 4):
            return darker(skin, 0.82) + (255,)
        return shade(skin, x, y, 3)
    s.box(0, 0, 8, 8, 8, head)

    # Hat layer: a silver circlet for the Speaker, otherwise only a few locks over the shoulders.
    def hat(face, x, y, w, h):
        if kind == 'speaker' and face in ('front', 'left', 'right', 'back') and y == 2:
            return (214, 220, 230, 255) if (face != 'front' or x not in (3, 4)) else (120, 200, 230, 255)
        if face == 'back' and y >= 6:
            return shade(hair, x, y)
        return None
    s.box(32, 0, 8, 8, 8, hat)

    # Ears: skin, a touch pinker at the tip.
    for y in range(4):
        for x in range(4):
            s.put(56 + x, y, shade(skin if y else darker(skin, 0.95), x, y, 2))

    def body(face, x, y, w, h):
        if face in ('top', 'bottom'):
            return shade(robe, x, y)
        if kind == 'fletcher':
            # Leather jerkin over a green shirt, a quiver strap from right shoulder to left hip.
            if face == 'front' and abs((7 - x) - y * 0.6) < 0.8:
                return shade((92, 60, 34), x, y)
            if face == 'back' and abs(x - y * 0.6) < 0.8:
                return shade((92, 60, 34), x, y)
            if y == 7:
                return shade((70, 46, 26), x, y)
            return shade((128, 88, 52) if face in ('front', 'back') and 1 < x < w - 2 else robe, x, y)
        if y == 0 and face == 'front':
            return shade(trim, x, y)
        if face == 'front' and x in (3, 4) and y < 7:
            return shade(trim if y < 2 else darker(robe, 0.85), x, y)
        if y == 7:
            return shade(trim if kind == 'speaker' else darker(robe, 0.6), x, y)
        return shade(robe, x, y)
    s.box(16, 16, 8, 12, 4, body)

    def arm(face, x, y, w, h):
        if face == 'bottom' or y >= 9:
            return shade(skin, x, y, 3)
        if y == 8:
            return shade(trim, x, y)
        if kind == 'fletcher' and face != 'top':
            return shade((70, 110, 60), x, y)
        return shade(robe, x, y)
    s.box(40, 16, 3, 12, 4, arm)
    s.box(32, 48, 3, 12, 4, arm)

    def leg(face, x, y, w, h):
        if face == 'bottom' or y >= 10:
            return shade((86, 60, 40), x, y)
        if kind == 'fletcher':
            return shade((84, 96, 60) if y < 10 else (86, 60, 40), x, y)
        # The robe falls to mid-shin, leggings beneath.
        if y < 7:
            return shade(trim if y == 6 else robe, x, y)
        return shade(darker(robe, 0.55), x, y)
    s.box(0, 16, 4, 12, 4, leg)
    s.box(16, 48, 4, 12, 4, leg)
    return s.px


def main():
    os.makedirs(OUT, exist_ok=True)
    for i in range(6):
        px = make(SKINS[i], HAIR[(i * 2) % 6], EYES[i], ROBES[i], TRIMS[i])
        pngio.write_png(os.path.join(OUT, 'elf_%d.png' % i), N, N, px)
    pngio.write_png(os.path.join(OUT, 'speaker.png'), N, N,
                    make(SKINS[5], (232, 234, 240), (90, 140, 200), (236, 238, 240), (212, 180, 90), 'speaker'))
    pngio.write_png(os.path.join(OUT, 'fletcher.png'), N, N,
                    make(SKINS[2], (120, 74, 42), (60, 120, 70), (70, 110, 60), (90, 60, 34), 'fletcher'))
    print('wrote', OUT)


if __name__ == '__main__':
    main()
