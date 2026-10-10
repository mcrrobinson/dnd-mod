"""Generates the Orc Stronghold textures (design 6, ticket T9).

    python3 tools/orc_stronghold_textures.py

- textures/entity/orc/orc_0..5.png: Orc Warriors and Archers (64x64 player layout, classic arms). Six skin
  tones from olive to grey-green, in hide jerkins and fur-trimmed leggings, with war paint. The tusks come from
  RaceFeatures.
- quartermaster.png (leather apron over a soot-dark shirt), champion.png (bare-chested, red war paint, scars)
  and war_chief.png (a bear-fur mantle with gold).
- textures/item/grog.png (a wooden tankard of brown grog) and goblin_war_token.png (a crude bronze disc
  stamped with a goblin skull).
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
TEX = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures')
OUT = os.path.join(TEX, 'entity/orc')
ITEM = os.path.join(TEX, 'item')

N = 64
CLEAR = (0, 0, 0, 0)

SKINS = [(110, 132, 66), (92, 120, 60), (126, 140, 84), (98, 112, 80), (84, 104, 58), (118, 124, 92)]
HIDES = [(104, 72, 44), (86, 60, 38), (120, 88, 56), (72, 54, 40), (110, 80, 50), (96, 66, 42)]
PAINT = [(150, 30, 26), (40, 40, 44), (190, 160, 70), (150, 30, 26), (230, 230, 220), (40, 40, 44)]
HAIR = (34, 28, 24)


def shade(color, x, y, amount=6):
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


def make(skin, hide, paint, kind='warrior'):
    s = Skin()
    trim = (200, 170, 70) if kind == 'chief' else (150, 140, 120)

    def head(face, x, y, w, h):
        if face == 'top':
            # A topknot of black hair down the middle, shaved sides.
            return shade(HAIR, x, y) if 2 <= x <= 5 else shade(darker(skin, 0.9), x, y)
        if face == 'back':
            return shade(HAIR, x, y) if 2 <= x <= 5 and y < 6 else shade(skin, x, y)
        if face == 'bottom':
            return shade(darker(skin, 0.8), x, y)
        if face in ('left', 'right'):
            if y < 2 and 2 <= x <= 5:
                return shade(HAIR, x, y)
            return shade(skin, x, y)
        # Front: a heavy brow, small red eyes, a broad nose, war paint and a wide jaw.
        if y == 2:
            return shade(darker(skin, 0.6), x, y)
        if y == 3 and x in (1, 2, 5, 6):
            if x in (2, 5):
                return (200, 40, 30, 255) if kind != 'quartermaster' else (230, 170, 40, 255)
            return (240, 230, 210, 255)
        if y in (4, 5) and x in (3, 4):
            return shade(darker(skin, 0.75), x, y)
        if kind == 'champion' and x in (1, 6) and 3 <= y <= 6:
            return shade(paint, x, y, 3)
        if y == 4 and x in (0, 7):
            return shade(paint, x, y, 3)
        if y == 6:
            return shade(darker(skin, 0.7), x, y)
        if y == 7:
            return shade(darker(skin, 0.85), x, y)
        return shade(skin, x, y)
    s.box(0, 0, 8, 8, 8, head)

    def hat(face, x, y, w, h):
        if kind == 'chief' and face != 'bottom' and (face == 'top' or y <= 1):
            # A crown of bone and gold.
            if face == 'top':
                return None
            return (230, 222, 196, 255) if x % 2 == 0 else (212, 168, 58, 255)
        return None
    s.box(32, 0, 8, 8, 8, hat)

    def body(face, x, y, w, h):
        if face in ('top', 'bottom'):
            return shade(hide, x, y)
        if kind == 'champion':
            # Bare chest, a belt, red war paint stripes across it.
            if y >= 9:
                return shade((60, 44, 30), x, y)
            if face == 'front' and (y == 3 or y == 5) and 1 <= x <= 6:
                return shade(paint, x, y, 3)
            if face == 'front' and x == 2 and 6 <= y <= 8:
                return shade(darker(skin, 0.7), x, y)
            return shade(skin, x, y)
        if kind == 'quartermaster':
            if face == 'front' and 1 <= x <= 6 and y >= 2:
                return shade((92, 64, 40), x, y, 4)
            if y == 8:
                return shade((50, 40, 30), x, y)
            return shade((54, 52, 56), x, y)
        if kind == 'chief':
            # A bear-fur mantle over the shoulders, gold clasps, hide below.
            if y <= 3:
                return shade((70, 52, 36), x, y, 10)
            if face == 'front' and y == 4 and x in (2, 5):
                return (212, 168, 58, 255)
            if y == 9:
                return shade(trim, x, y)
            return shade(hide, x, y)
        # Warriors and archers: a hide jerkin, a belt, a bone toggle.
        if y == 9:
            return shade((48, 36, 26), x, y)
        if face == 'front' and x in (3, 4) and y in (2, 5):
            return (230, 222, 196, 255)
        if face == 'front' and y < 2 and x in (2, 3, 4, 5):
            return shade(skin, x, y)
        return shade(hide, x, y, 8)
    s.box(16, 16, 8, 12, 4, body)

    def arm(face, x, y, w, h):
        if face == 'bottom' or y >= 10:
            return shade(darker(skin, 0.85), x, y)
        if kind == 'chief' and y < 4:
            return shade((70, 52, 36), x, y, 10)
        if kind == 'quartermaster' and y < 8:
            return shade((54, 52, 56), x, y)
        if y == 8:
            return shade((90, 70, 50), x, y)
        if kind == 'champion' and y == 5:
            return shade(paint, x, y, 3)
        return shade(skin, x, y)
    s.box(40, 16, 4, 12, 4, arm)
    s.box(32, 48, 4, 12, 4, arm)

    def leg(face, x, y, w, h):
        if face == 'bottom' or y >= 10:
            return shade((44, 34, 26), x, y)
        if y == 9:
            return shade(trim, x, y, 8)
        return shade(darker(hide, 0.7), x, y, 8)
    s.box(0, 16, 4, 12, 4, leg)
    s.box(16, 48, 4, 12, 4, leg)
    return s.px


def icon(rows, palette):
    px = []
    for row in rows:
        px.append([palette[c] if c in palette else CLEAR for c in row])
    return px


GROG = [
    "................",
    "................",
    "...wwwwwwww.....",
    "..wFFFFFFFFw....",
    "..bWWWWWWWWb....",
    "..bggggggggbhh..",
    "..BwddddddwB..h.",
    "..bggggggggb..h.",
    "..BwddddddwB..h.",
    "..bggggggggb..h.",
    "..BwddddddwBhh..",
    "..bggggggggb....",
    "..BwddddddwB....",
    "..bbbbbbbbbb....",
    "...BBBBBBBB.....",
    "................",
]
GROG_PALETTE = {
    'w': (226, 214, 180, 255), 'F': (238, 230, 206, 255), 'W': (200, 186, 150, 255),
    'b': (60, 44, 30, 255), 'B': (44, 32, 22, 255), 'g': (122, 84, 50, 255), 'd': (100, 68, 40, 255),
    'h': (70, 52, 36, 255),
}
TOKEN = [
    "................",
    ".....oooooo.....",
    "...ooCCCCCCoo...",
    "..oCCcccccCCCo..",
    "..oCcccccccCCo..",
    ".oCcckkkkkccCCo.",
    ".oCckkcckkkcCCo.",
    ".oCckeckekkcCCo.",
    ".oCckkkkkkkcCCo.",
    ".oCcckkkkkccCCo.",
    ".oCcckckckccCCo.",
    "..oCcccccccCCo..",
    "..oCCcccccCCCo..",
    "...ooCCCCCCoo...",
    ".....oooooo.....",
    "................",
]
TOKEN_PALETTE = {
    'o': (70, 44, 20, 255), 'C': (150, 96, 44, 255), 'c': (196, 132, 64, 255),
    'k': (90, 110, 50, 255), 'e': (230, 60, 40, 255),
}


def main():
    os.makedirs(OUT, exist_ok=True)
    for i in range(6):
        pngio.write_png(os.path.join(OUT, 'orc_%d.png' % i), N, N, make(SKINS[i], HIDES[i], PAINT[i]))
    pngio.write_png(os.path.join(OUT, 'quartermaster.png'), N, N, make(SKINS[3], HIDES[3], PAINT[1], 'quartermaster'))
    pngio.write_png(os.path.join(OUT, 'champion.png'), N, N, make(SKINS[1], HIDES[1], PAINT[0], 'champion'))
    pngio.write_png(os.path.join(OUT, 'war_chief.png'), N, N, make(SKINS[4], (90, 60, 36), PAINT[0], 'chief'))
    pngio.write_png(os.path.join(ITEM, 'grog.png'), 16, 16, icon(GROG, GROG_PALETTE))
    pngio.write_png(os.path.join(ITEM, 'goblin_war_token.png'), 16, 16, icon(TOKEN, TOKEN_PALETTE))
    print('wrote', OUT, 'and the grog and war token icons')


if __name__ == '__main__':
    main()
