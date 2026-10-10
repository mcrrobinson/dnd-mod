"""Generates the racial gear textures (design 6, ticket T8: racial gear and kin-only trades).

    python3 tools/racial_gear_textures.py

- Armor: each set's worn layers (textures/models/armor/<set>/) and item icons are recoloured from another
  set's textures: every pixel's brightness is mapped onto the set's palette, keeping alpha.
- Weapons: the Elven Longbow (and its 3 pulling frames) is drawn procedurally; the Dwarven Waraxe, Orcish
  Greataxe and Halfling Sling are 16x16 character maps.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
TEX = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures')
ARMOR = os.path.join(TEX, 'models/armor')
ITEM = os.path.join(TEX, 'item')

CLEAR = (0, 0, 0, 0)

# set: (source set folder, source item prefix, palette dark -> light)
SETS = {
    'elven_chain': ('wooden', 'wooden', [(28, 48, 40), (82, 120, 100), (160, 196, 176), (232, 244, 236)]),
    'dwarven_plate': ('warrior', 'warrior', [(46, 30, 18), (130, 82, 38), (200, 148, 70), (246, 220, 140)]),
    'orcish_warplate': ('steampunk', 'steampunk', [(22, 16, 16), (74, 44, 36), (128, 92, 76), (186, 170, 156)]),
    'draconic_scale': ('prismarine', 'prismarine', [(48, 8, 10), (150, 30, 24), (214, 96, 40), (250, 206, 96)]),
    'infernal_regalia': ('holy_armor', 'holy_armor', [(14, 4, 16), (70, 14, 44), (150, 30, 52), (232, 112, 64)]),
}
# Item icons taken from another set than the rest of the set (the steampunk helmet is goggles).
ICON_OVERRIDES = {('orcish_warplate', 'helmet'): 'wither_helmet'}
PIECES = ['helmet', 'chestplate', 'leggings', 'boots']


def lum(px):
    return 0.299 * px[0] + 0.587 * px[1] + 0.114 * px[2]


def gradient(palette, t):
    t = max(0.0, min(1.0, t)) * (len(palette) - 1)
    i = min(int(t), len(palette) - 2)
    f = t - i
    a, b = palette[i], palette[i + 1]
    return tuple(int(round(a[k] + (b[k] - a[k]) * f)) for k in range(3))


def recolor(src, dst, palette):
    w, h, rows = pngio.read_png(src)
    values = [lum(px) for row in rows for px in row if px[3] > 0]
    lo, hi = (min(values), max(values)) if values else (0, 255)
    span = max(1.0, hi - lo)
    out = []
    for row in rows:
        line = []
        for px in row:
            if px[3] == 0:
                line.append(CLEAR)
            else:
                line.append(gradient(palette, (lum(px) - lo) / span) + (px[3],))
        out.append(line)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    pngio.write_png(dst, w, h, out)


def armor():
    for name, (folder, prefix, palette) in SETS.items():
        src_dir = os.path.join(ARMOR, folder)
        for f in sorted(os.listdir(src_dir)):
            if f.startswith('body_') or f.startswith('leggings_'):
                recolor(os.path.join(src_dir, f), os.path.join(ARMOR, name, f), palette)
        for piece in PIECES:
            src = ICON_OVERRIDES.get((name, piece), f'{prefix}_{piece}')
            recolor(os.path.join(ITEM, src + '.png'), os.path.join(ITEM, f'{name}_{piece}.png'), palette)


def draw(path, legend, rows):
    out = [[legend[c] if c != '.' else CLEAR for c in row] for row in rows]
    assert len(out) == 16 and all(len(r) == 16 for r in out), path
    pngio.write_png(path, 16, 16, out)


# --- Elven Longbow: pale wood limbs curving to the top left, silver string, leaf-green grip ---
WOOD_DARK = (92, 70, 40, 255)
WOOD = (196, 170, 112, 255)
WOOD_LIGHT = (232, 214, 160, 255)
GRIP = (62, 120, 70, 255)
STRING = (220, 228, 232, 255)
ARROW_SHAFT = (150, 110, 60, 255)
ARROW_HEAD = (200, 205, 210, 255)
FLETCH = (240, 240, 240, 255)


def bezier(p0, p1, p2, steps=60):
    pts = []
    for i in range(steps + 1):
        t = i / steps
        x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0]
        y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]
        pts.append((int(round(x)), int(round(y))))
    return pts


def line(a, b):
    pts = []
    n = max(abs(b[0] - a[0]), abs(b[1] - a[1]), 1)
    for i in range(n + 1):
        pts.append((int(round(a[0] + (b[0] - a[0]) * i / n)), int(round(a[1] + (b[1] - a[1]) * i / n))))
    return pts


def longbow(pull):
    """pull 0 = standby, 1-3 = the pulling frames (string drawn back toward the bottom right)."""
    img = [[CLEAR] * 16 for _ in range(16)]

    def put(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            img[y][x] = c

    top, bottom = (14, 1), (1, 14)
    limb = bezier(top, (3, 3), bottom)
    # String first, so the limbs draw over its ends.
    mid = (7 + pull, 8 + pull)
    for p in line(top, mid) + line(mid, bottom):
        put(p[0], p[1], STRING)
    if pull > 0:
        # Arrow along the diagonal, nocked at the string, head past the grip.
        tail = mid
        head = (mid[0] - 8, mid[1] - 8)
        for p in line(tail, head):
            put(p[0], p[1], ARROW_SHAFT)
        put(head[0], head[1], ARROW_HEAD)
        put(head[0] + 1, head[1], ARROW_HEAD)
        put(head[0], head[1] + 1, ARROW_HEAD)
        put(tail[0] + 1, tail[1], FLETCH)
        put(tail[0], tail[1] + 1, FLETCH)
    for i, (x, y) in enumerate(limb):
        put(x, y, WOOD)
        put(x + 1, y, WOOD_DARK)
        put(x, y - 1, WOOD_LIGHT)
    # Grip in the middle of the limb.
    for (x, y) in limb[len(limb) // 2 - 4:len(limb) // 2 + 5]:
        put(x, y, GRIP)
        put(x + 1, y, GRIP)
    # Tips.
    put(top[0], top[1], WOOD_DARK)
    put(bottom[0], bottom[1], WOOD_DARK)
    return img


WARAXE = ({
    'o': (34, 24, 20, 255), 'h': (110, 72, 40, 255), 'H': (150, 104, 60, 255),
    's': (150, 150, 158, 255), 'S': (210, 212, 220, 255), 'g': (220, 170, 60, 255),
}, [
    '................',
    '.......oo..oo...',
    '......oSSooSSo..',
    '.....oSSsSSsSSo.',
    '.....oSsgggsSo..',
    '......osgHgso...',
    '.....oSsgggsSo..',
    '.....oSSohoSSo..',
    '......ooHhooo...',
    '.......oHho.....',
    '......oHho......',
    '.....oHho.......',
    '....oHho........',
    '...oggo.........',
    '...ooo..........',
    '................',
])

GREATAXE = ({
    'o': (20, 14, 12, 255), 'h': (70, 46, 30, 255), 'H': (104, 70, 44, 255),
    's': (90, 86, 84, 255), 'S': (150, 146, 140, 255), 'r': (130, 30, 24, 255),
}, [
    '.......oooo.....',
    '......oSSSSo....',
    '.....oSSsSSSo...',
    '....oSSssSSSo...',
    '....oSsssSSSo.o.',
    '....oSsrssSo.oHo',
    '.....oSsrSo.oHo.',
    '......oSSooHHo..',
    '.......oooHHo...',
    '.........oHho...',
    '........oHho....',
    '.......oHho.....',
    '......oHho......',
    '.....oHho.......',
    '....orro........',
    '....ooo.........',
])

SLING = ({
    'o': (40, 26, 16, 255), 'l': (130, 90, 50, 255), 'L': (176, 128, 76, 255),
    'c': (110, 110, 110, 255), 'C': (160, 160, 160, 255), 'k': (70, 70, 70, 255),
}, [
    '................',
    '..oo............',
    '.oLLo...........',
    '.oLlo...........',
    '..oLo...........',
    '...oLo..........',
    '....oLo.........',
    '.....oLo........',
    '......oLoo......',
    '.......oLLoo....',
    '......oLCCcLo...',
    '......oLCcckLo..',
    '.......oLckkLo..',
    '........oLLLo...',
    '.........ooo....',
    '................',
])


def weapons():
    for pull in range(4):
        name = 'elven_longbow' if pull == 0 else f'elven_longbow_pulling_{pull - 1}'
        img = longbow(pull)
        pngio.write_png(os.path.join(ITEM, name + '.png'), 16, 16, img)
    draw(os.path.join(ITEM, 'dwarven_waraxe.png'), *WARAXE)
    draw(os.path.join(ITEM, 'orcish_greataxe.png'), *GREATAXE)
    draw(os.path.join(ITEM, 'halfling_sling.png'), *SLING)


if __name__ == '__main__':
    armor()
    weapons()
    print('racial gear textures written')
