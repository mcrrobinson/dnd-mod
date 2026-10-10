"""Generates the Dragonborn NPC skins (64x64 player-model layout, classic Steve arms).

    python3 tools/dragonborn_skins.py

Writes textures/entity/dragonborn/dragonborn_0..4.png (Dragon Temple Templars: scales in red, white, blue,
bronze and gold, the variant order of DragonbornEntity, under a temple tabard) and elder.png (the Dragonborn
Elder: gold scales under deep purple robes with gold trim). The snout, frill and spines come from
RaceFeatures, tinted by ancestry.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/entity/dragonborn')

N = 64
CLEAR = (0, 0, 0, 0)

# Scale colours in DragonbornEntity variant order: red, white, blue, bronze, gold.
SCALES = [(170, 48, 38), (224, 226, 232), (50, 90, 168), (158, 102, 50), (212, 168, 58)]
# Tabard colours for each: the temple's red and gold, with the breath's colour on the trim.
TRIMS = [(232, 120, 40), (150, 200, 235), (110, 170, 240), (220, 150, 70), (250, 220, 120)]
TABARD = (122, 26, 30)
EYES = [(250, 210, 60), (120, 200, 250), (250, 240, 120), (250, 190, 70), (240, 120, 40)]


def shade(color, x, y, amount=6):
    """A little per-pixel variation so flat colours don't look like plastic."""
    n = ((x * 73 + y * 151) ^ (x * y * 7)) % 5 - 2
    return tuple(max(0, min(255, c + n * amount // 2)) for c in color[:3]) + (255,)


def darker(color, f=0.75):
    return tuple(int(c * f) for c in color[:3])


def scale(color, x, y):
    """Scales: a diamond lattice of darker edges over the base colour."""
    if (x + y) % 3 == 0 and (x - y) % 3 == 0:
        return shade(darker(color, 0.7), x, y, 3)
    if (x + 2 * y) % 4 == 0:
        return shade(darker(color, 0.85), x, y, 3)
    return shade(color, x, y, 4)


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


def make(skin, eyes, robe, trim, kind='templar'):
    s = Skin()
    belly = tuple(min(255, int(c * 1.15) + 20) for c in skin)

    def head(face, x, y, w, h):
        if face in ('top', 'back'):
            # A ridge of darker crest scales down the middle.
            if x in (3, 4):
                return shade(darker(skin, 0.6), x, y)
            return scale(skin, x, y)
        if face == 'bottom':
            return shade(belly, x, y)
        if face in ('left', 'right'):
            return scale(skin, x, y)
        # Front: heavy brow, slit eyes, nostrils and a scaled jaw (the snout model covers the lower face).
        if y == 2 and x in (1, 2, 5, 6):
            return shade(darker(skin, 0.6), x, y)
        if y == 3 and x in (1, 2, 5, 6):
            return eyes + (255,) if x in (1, 6) else (20, 16, 12, 255)
        if y >= 5:
            return shade(belly if 2 <= x <= 5 else skin, x, y)
        return scale(skin, x, y)
    s.box(0, 0, 8, 8, 8, head)

    def hat(face, x, y, w, h):
        if kind == 'elder' and face in ('front', 'left', 'right', 'back') and y == 1:
            return (230, 190, 70, 255) if (face != 'front' or x not in (3, 4)) else (200, 40, 40, 255)
        return None
    s.box(32, 0, 8, 8, 8, hat)

    def body(face, x, y, w, h):
        if face in ('top', 'bottom'):
            return shade(robe, x, y)
        if kind == 'templar':
            # A tabard over scales: red cloth down the middle front and back, the trim at the hem and collar.
            if face in ('front', 'back') and 1 <= x <= w - 2:
                if y == 0 or y == h - 1 or x in (1, w - 2):
                    return shade(trim, x, y)
                if face == 'front' and 2 <= y <= 4 and x in (3, 4):
                    return shade((230, 190, 70), x, y)
                return shade(robe, x, y)
            if y == 8:
                return shade((60, 40, 26), x, y)
            return scale(skin, x, y)
        # Elder: a long robe, a gold sash.
        if y == 0 and face == 'front' or face == 'front' and x in (3, 4):
            return shade(trim, x, y)
        if y == 7:
            return shade(trim, x, y)
        return shade(robe, x, y)
    s.box(16, 16, 8, 12, 4, body)

    def arm(face, x, y, w, h):
        if face == 'bottom' or y >= 10:
            return shade(darker(skin, 0.8), x, y) if y == h - 1 or face == 'bottom' else scale(skin, x, y)
        if kind == 'elder':
            return shade(trim if y == 9 else robe, x, y)
        if y < 3:
            return shade(robe, x, y)
        return scale(skin, x, y)
    s.box(40, 16, 4, 12, 4, arm)
    s.box(32, 48, 4, 12, 4, arm)

    def leg(face, x, y, w, h):
        if face == 'bottom' or y >= 10:
            return shade((70, 50, 36), x, y)
        if kind == 'elder':
            return shade(trim if y == 6 else robe, x, y)
        if y < 5:
            return shade(darker(robe, 0.6), x, y)
        return scale(skin, x, y)
    s.box(0, 16, 4, 12, 4, leg)
    s.box(16, 48, 4, 12, 4, leg)
    return s.px


def main():
    os.makedirs(OUT, exist_ok=True)
    for i in range(5):
        pngio.write_png(os.path.join(OUT, 'dragonborn_%d.png' % i), N, N,
                        make(SCALES[i], EYES[i], TABARD, TRIMS[i]))
    pngio.write_png(os.path.join(OUT, 'elder.png'), N, N,
                    make(SCALES[4], (240, 120, 40), (70, 34, 90), (226, 186, 70), 'elder'))
    print('wrote', OUT)


if __name__ == '__main__':
    main()
