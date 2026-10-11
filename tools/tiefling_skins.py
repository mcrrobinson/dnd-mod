"""Generates the Tiefling NPC skins (64x64 player-model layout, classic Steve arms).

    python3 tools/tiefling_skins.py

Writes textures/entity/tiefling/tiefling_0..4.png (Brimstone Sanctum Exiles: skin in crimson, plum, ash,
slate and ember, the variant order of TieflingEntity, under dark leathers and a hooded mantle) and
pactbroker.png (the Pactbroker: plum skin under black and gold robes with a red sash). The horns and tail come
from RaceFeatures.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/entity/tiefling')

N = 64
CLEAR = (0, 0, 0, 0)

# Skin in TieflingEntity variant order: crimson, plum, ash, slate, ember.
SKINS = [(168, 44, 48), (120, 58, 112), (150, 140, 146), (84, 92, 122), (196, 84, 40)]
HAIR = [(30, 18, 22), (40, 24, 50), (220, 220, 226), (22, 22, 30), (60, 20, 14)]
# Solid-colour eyes: gold, silver, red, white, amber.
EYES = [(250, 200, 60), (210, 210, 230), (230, 40, 40), (245, 245, 245), (250, 150, 40)]
LEATHER = (52, 34, 30)
MANTLE = [(70, 20, 26), (40, 26, 56), (60, 56, 60), (30, 34, 52), (80, 34, 18)]


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


def make(skin, hair, eyes, robe, trim, kind='exile'):
    s = Skin()

    def head(face, x, y, w, h):
        if face == 'top':
            return shade(hair, x, y)
        if face == 'back':
            return shade(hair, x, y)
        if face in ('left', 'right'):
            return shade(hair if y < 4 else skin, x, y)
        if face == 'bottom':
            return shade(darker(skin, 0.85), x, y)
        # Front: a fringe, a sharp brow, solid glowing eyes, a dark mouth.
        if y <= 1:
            return shade(hair, x, y)
        if y == 2 and x in (1, 2, 5, 6):
            return shade(darker(skin, 0.6), x, y)
        if y == 3 and x in (1, 2, 5, 6):
            return eyes + (255,)
        if y == 6 and 3 <= x <= 4:
            return shade(darker(skin, 0.45), x, y)
        return shade(skin, x, y)
    s.box(0, 0, 8, 8, 8, head)

    def hat(face, x, y, w, h):
        # Exiles wear a hood's rim; the Pactbroker a gold circlet.
        if kind == 'broker' and face in ('front', 'left', 'right', 'back') and y == 1:
            return (226, 186, 70, 255) if (face != 'front' or x not in (3, 4)) else (200, 30, 40, 255)
        if kind == 'exile' and face in ('left', 'right', 'back') and y <= 2:
            return shade(robe, x, y)
        return None
    s.box(32, 0, 8, 8, 8, hat)

    def body(face, x, y, w, h):
        if face in ('top', 'bottom'):
            return shade(robe, x, y)
        if kind == 'exile':
            # A mantle over the shoulders, leathers below, a belt with a gold buckle.
            if y < 3:
                return shade(robe, x, y)
            if y == 8:
                return shade((230, 190, 70), x, y) if face == 'front' and x in (3, 4) else shade((30, 20, 16), x, y)
            if face == 'front' and x in (3, 4) and y < 8:
                return shade(darker(skin, 0.9), x, y)
            return shade(LEATHER, x, y)
        # Pactbroker: long black robes, a gold-trimmed collar, a red sash.
        if y == 0 or face == 'front' and x in (3, 4) and y < 7:
            return shade(trim, x, y)
        if y == 7:
            return shade((170, 30, 40), x, y)
        return shade(robe, x, y)
    s.box(16, 16, 8, 12, 4, body)

    def arm(face, x, y, w, h):
        if face == 'bottom' or y >= 10:
            return shade(darker(skin, 0.85), x, y)
        if kind == 'broker':
            return shade(trim if y == 9 else robe, x, y)
        if y < 3:
            return shade(robe, x, y)
        if y >= 8:
            return shade(LEATHER, x, y)
        return shade(skin, x, y)
    s.box(40, 16, 4, 12, 4, arm)
    s.box(32, 48, 4, 12, 4, arm)

    def leg(face, x, y, w, h):
        if face == 'bottom' or y >= 9:
            return shade((26, 18, 16), x, y)
        if kind == 'broker':
            return shade(trim if y == 7 else robe, x, y)
        return shade(darker(LEATHER, 0.8), x, y)
    s.box(0, 16, 4, 12, 4, leg)
    s.box(16, 48, 4, 12, 4, leg)
    return s.px


def main():
    os.makedirs(OUT, exist_ok=True)
    for i in range(5):
        pngio.write_png(os.path.join(OUT, 'tiefling_%d.png' % i), N, N,
                        make(SKINS[i], HAIR[i], EYES[i], MANTLE[i], (226, 186, 70)))
    pngio.write_png(os.path.join(OUT, 'pactbroker.png'), N, N,
                    make(SKINS[1], (20, 14, 20), (250, 200, 60), (24, 18, 26), (226, 186, 70), 'broker'))
    print('wrote', OUT)


if __name__ == '__main__':
    main()
