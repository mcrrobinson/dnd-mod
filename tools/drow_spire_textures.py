"""Generates the Piwafwi's worn layer (design 9, 9.4): the Cloak of the Drow, a 64x32 vanilla armour layout.

    python3 tools/drow_spire_textures.py

Writes textures/models/armor/piwafwi/body.png (client/drow/PiwafwiRenderer draws it on body, arms and head): a hood
open at the face, a cloak of near-black spidersilk with a violet sheen and a silver clasp, sleeves to the wrist, a
violet hem. The item icon is in boss_item_textures.py; the Matron Mother's skin in drow_skins.py.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402
from elf_skins import shade  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/models/armor/piwafwi')

W, H = 64, 32
CLEAR = (0, 0, 0, 0)
CLOTH = (30, 24, 40)
SHEEN = (52, 38, 70)
HEM = (96, 58, 132)
CLASP = (196, 202, 218)


def box(px, u, v, w, h, d, paint):
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
                if c is not None and 0 <= fx + x < W and 0 <= fy + y < H:
                    px[fy + y][fx + x] = c


def cloth(x, y):
    return shade(SHEEN if (x + 2 * y) % 7 == 0 else CLOTH, x, y, 4)


def main():
    px = [[CLEAR for _ in range(W)] for _ in range(H)]

    def hood(face, x, y, w, h):
        if face == 'bottom':
            return None
        if face == 'front':
            # Open at the face: only the rim of the hood shows.
            return cloth(x, y) if y < 2 or x in (0, 7) else None
        return cloth(x, y)
    box(px, 0, 0, 8, 8, 8, hood)

    def body(face, x, y, w, h):
        if face in ('top', 'bottom'):
            return cloth(x, y)
        if face == 'front' and y == 0 and x in (3, 4):
            return shade(CLASP, x, y, 2)
        if face == 'front' and x in (3, 4) and y > 0:
            return shade(HEM, x, y, 3)  # the open front shows the violet lining
        if y == h - 1:
            return shade(HEM, x, y, 3)
        return cloth(x, y)
    box(px, 16, 16, 8, 12, 4, body)

    def arm(face, x, y, w, h):
        if face == 'bottom' or y >= 10:
            return None
        if y == 9:
            return shade(HEM, x, y, 3)
        return cloth(x, y)
    box(px, 40, 16, 4, 12, 4, arm)

    os.makedirs(OUT, exist_ok=True)
    pngio.write_png(os.path.join(OUT, 'body.png'), W, H, px)
    print('wrote', OUT)


if __name__ == '__main__':
    main()
