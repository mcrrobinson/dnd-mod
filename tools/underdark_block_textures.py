"""Generates the Underdark's natural block textures (design 9, T1).

    python3 tools/underdark_block_textures.py

Writes 16x16 textures under textures/block/ (and the door's item icon under textures/item/):
- zurkhwood_stem.png / zurkhwood_stem_top.png: a purple-grey ribbed fungal stalk, and its cut end with growth rings.
- zurkhwood_planks.png, zurkhwood_door_{top,bottom}.png, zurkhwood_trapdoor.png, item/zurkhwood_door.png: the
  stalk sawn into dusky lavender-grey boards.
- zurkhwood_cap.png / zurkhwood_cap_inside.png: dusky violet-brown cap skin with pale specks; tan gills underneath.
- nightlight_cap.png / nightlight_cap_inside.png: a pale blue-white glowing cap; its paler glowing underside.
- fungal_loam.png: dark blue-grey spongy floor with tiny white spore specks.
- faerzress_stone.png: deepslate veined with glowing violet.
- bluecap.png / trillimac.png: small mushrooms (cross models): a glowing blue one and a grey-tan one.
Deterministic (seeded), so reruns give the same pixels.
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
BLOCK = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/block')
ITEM = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/item')
N = 16
CLEAR = (0, 0, 0, 0)


def clamp(v):
    return max(0, min(255, int(round(v))))


def rgba(c, a=255):
    return (clamp(c[0]), clamp(c[1]), clamp(c[2]), a)


def shade(c, d):
    return rgba((c[0] + d, c[1] + d, c[2] + d), c[3] if len(c) > 3 else 255)


def mix(a, b, t):
    return rgba(tuple(a[i] + (b[i] - a[i]) * t for i in range(3)))


def grid(fill):
    return [[fill for _ in range(N)] for _ in range(N)]


def save(folder, name, rows):
    os.makedirs(folder, exist_ok=True)
    pngio.write_png(os.path.join(folder, name + '.png'), N, len(rows), rows)
    print('wrote', name + '.png')


def noise_field(rng, scale=4):
    """Smooth value noise in 0..1, tiling at 16."""
    cells = N // scale
    lattice = [[rng.random() for _ in range(cells)] for _ in range(cells)]

    def at(x, y):
        gx, gy = x / scale, y / scale
        x0, y0 = int(gx) % cells, int(gy) % cells
        x1, y1 = (x0 + 1) % cells, (y0 + 1) % cells
        tx, ty = gx - int(gx), gy - int(gy)
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        top = lattice[y0][x0] * (1 - tx) + lattice[y0][x1] * tx
        bottom = lattice[y1][x0] * (1 - tx) + lattice[y1][x1] * tx
        return top * (1 - ty) + bottom * ty
    return [[at(x, y) for x in range(N)] for y in range(N)]


# --- zurkhwood ---------------------------------------------------------------------------------------------------
STEM = (112, 96, 118)
STEM_DARK = (74, 62, 82)
STEM_LIGHT = (146, 130, 150)


def zurkhwood_stem(rng):
    rows = grid(rgba(STEM))
    for x in range(N):
        rib = x % 4
        base = {0: STEM_DARK, 1: STEM, 2: STEM_LIGHT, 3: STEM}[rib]
        for y in range(N):
            rows[y][x] = shade(rgba(base), rng.randint(-7, 7))
    # Knots and fibrous streaks.
    for _ in range(5):
        x, y = rng.randrange(N), rng.randrange(N)
        for dy in range(rng.randint(2, 4)):
            rows[(y + dy) % N][x] = shade(rgba(STEM_DARK), -10)
    return rows


def zurkhwood_stem_top(rng):
    rows = grid(rgba(STEM))
    for y in range(N):
        for x in range(N):
            r = math.hypot(x - 7.5, y - 7.5)
            if r > 7.0:
                rows[y][x] = shade(rgba(STEM_DARK), rng.randint(-5, 5))
            else:
                ring = int(r) % 2
                base = (190, 170, 186) if ring else (168, 148, 166)
                rows[y][x] = shade(rgba(base), rng.randint(-6, 6))
    rows[7][7] = rows[8][8] = rgba((120, 100, 120))
    return rows


PLANK = (134, 116, 138)
PLANK_DARK = (92, 78, 98)


def planks(rng):
    rows = grid(rgba(PLANK))
    for y in range(N):
        board = y // 4
        for x in range(N):
            tone = (board * 7) % 13 - 6
            grain = 6 if (x + board * 5) % 7 == 0 else 0
            rows[y][x] = shade(rgba(PLANK), tone - grain + rng.randint(-4, 4))
        if y % 4 == 3:
            for x in range(N):
                rows[y][x] = shade(rgba(PLANK_DARK), rng.randint(-4, 2))
    # Board ends, staggered.
    for board in range(4):
        x = (board * 9 + 3) % N
        for y in range(board * 4, board * 4 + 3):
            rows[y][x] = shade(rgba(PLANK_DARK), -4)
    return rows


def door_half(rng, top):
    rows = planks(rng)
    for y in range(N):
        for x in (0, N - 1):
            rows[y][x] = shade(rgba(PLANK_DARK), -12)
    for x in range(N):
        rows[0 if top else N - 1][x] = shade(rgba(PLANK_DARK), -12)
    if top:
        # A small round window, dark violet glass.
        for y in range(3, 9):
            for x in range(4, 12):
                if math.hypot(x - 7.5, y - 5.5) < 3.2:
                    rows[y][x] = rgba((44, 30, 66))
    else:
        rows[4][12] = rows[4][11] = rgba((200, 196, 210))  # handle
    return rows


def trapdoor(rng):
    rows = planks(rng)
    for i in range(N):
        rows[0][i] = rows[N - 1][i] = rows[i][0] = rows[i][N - 1] = shade(rgba(PLANK_DARK), -12)
    for y in range(4, 12):
        for x in range(4, 12):
            if (x + y) % 4 == 0:
                rows[y][x] = CLEAR
    return rows


def door_item(rng):
    rows = grid(CLEAR)
    bottom, top = door_half(rng, False), door_half(rng, True)
    for y in range(N):
        for x in range(4, 12):
            src = top if y < 8 else bottom
            rows[y][x] = src[(y * 2) % N][(x - 4) * 2]
    return rows


CAP = (96, 64, 92)
CAP_DARK = (66, 42, 66)
GILL = (196, 168, 126)


def zurkhwood_cap(rng):
    field = noise_field(rng)
    rows = grid(rgba(CAP))
    for y in range(N):
        for x in range(N):
            rows[y][x] = mix(rgba(CAP_DARK), rgba(CAP), field[y][x]) if field[y][x] < 0.6 else shade(rgba(CAP), 10)
            rows[y][x] = shade(rows[y][x], rng.randint(-5, 5))
    for _ in range(9):
        x, y = rng.randrange(N), rng.randrange(N)
        rows[y][x] = rgba((178, 150, 170))
    return rows


def gills(rng, base, dark):
    rows = grid(rgba(base))
    for y in range(N):
        for x in range(N):
            d = -22 if (x + (y // 4)) % 3 == 0 else rng.randint(-6, 6)
            rows[y][x] = shade(rgba(base), d) if d > -20 else rgba(dark)
    return rows


NIGHT = (190, 222, 242)
NIGHT_DARK = (120, 168, 214)


def nightlight_cap(rng):
    field = noise_field(rng)
    rows = grid(rgba(NIGHT))
    for y in range(N):
        for x in range(N):
            rows[y][x] = mix(rgba(NIGHT_DARK), rgba(NIGHT), field[y][x])
            rows[y][x] = shade(rows[y][x], rng.randint(-4, 4))
    for _ in range(10):
        x, y = rng.randrange(N), rng.randrange(N)
        rows[y][x] = rgba((246, 252, 255))
    return rows


LOAM = (52, 60, 78)


def fungal_loam(rng):
    field = noise_field(rng, 2)
    rows = grid(rgba(LOAM))
    for y in range(N):
        for x in range(N):
            rows[y][x] = shade(rgba(LOAM), int((field[y][x] - 0.5) * 30) + rng.randint(-4, 4))
            if field[y][x] < 0.18:
                rows[y][x] = shade(rgba(LOAM), -18)  # spongy pores
    for _ in range(12):
        x, y = rng.randrange(N), rng.randrange(N)
        rows[y][x] = rgba((214, 222, 232))
    return rows


DEEP = (78, 78, 84)


def faerzress_stone(rng):
    field = noise_field(rng, 4)
    rows = grid(rgba(DEEP))
    for y in range(N):
        for x in range(N):
            rows[y][x] = shade(rgba(DEEP), int((field[y][x] - 0.5) * 24) + rng.randint(-5, 5))
            if (y % 4 == 0) and rng.random() < 0.5:
                rows[y][x] = shade(rows[y][x], -12)  # deepslate's layers
    # Glowing violet veins: two wandering lines.
    for start in ((0, rng.randrange(N)), (rng.randrange(N), 0)):
        x, y = start
        horizontal = start[0] == 0
        for _ in range(N + 4):
            if 0 <= x < N and 0 <= y < N:
                rows[y][x] = rgba((206, 120, 255))
                for nx, ny in ((x + 1, y), (x, y + 1)):
                    if 0 <= nx < N and 0 <= ny < N and rows[ny][nx][0] < 150:
                        rows[ny][nx] = rgba((132, 64, 180))
            if horizontal:
                x += 1
                y += rng.choice((-1, 0, 0, 1))
            else:
                y += 1
                x += rng.choice((-1, 0, 0, 1))
    return rows


def small_mushroom(rng, cap, cap_dark, stem, glow):
    rows = grid(CLEAR)
    # Stem.
    for y in range(9, 15):
        rows[y][7] = rgba(stem)
        rows[y][8] = shade(rgba(stem), -18)
    # Cap: a dome 9 wide.
    for y in range(4, 10):
        half = {4: 2, 5: 3, 6: 4, 7: 4, 8: 4, 9: 3}[y]
        for x in range(8 - half, 8 + half):
            c = cap if y < 8 else cap_dark
            rows[y][x] = shade(rgba(c), rng.randint(-6, 6))
    for _ in range(4):
        x, y = rng.randrange(5, 11), rng.randrange(4, 7)
        if rows[y][x][3]:
            rows[y][x] = rgba(glow)
    return rows


def main():
    rng = random.Random(9)
    save(BLOCK, 'zurkhwood_stem', zurkhwood_stem(rng))
    save(BLOCK, 'zurkhwood_stem_top', zurkhwood_stem_top(rng))
    save(BLOCK, 'zurkhwood_planks', planks(random.Random(91)))
    save(BLOCK, 'zurkhwood_door_top', door_half(random.Random(92), True))
    save(BLOCK, 'zurkhwood_door_bottom', door_half(random.Random(93), False))
    save(BLOCK, 'zurkhwood_trapdoor', trapdoor(random.Random(94)))
    save(ITEM, 'zurkhwood_door', door_item(random.Random(95)))
    save(BLOCK, 'zurkhwood_cap', zurkhwood_cap(rng))
    save(BLOCK, 'zurkhwood_cap_inside', gills(rng, GILL, (150, 120, 86)))
    save(BLOCK, 'nightlight_cap', nightlight_cap(rng))
    save(BLOCK, 'nightlight_cap_inside', gills(rng, (220, 238, 250), (160, 200, 232)))
    save(BLOCK, 'fungal_loam', fungal_loam(rng))
    save(BLOCK, 'faerzress_stone', faerzress_stone(rng))
    save(BLOCK, 'bluecap', small_mushroom(rng, (70, 120, 230), (40, 70, 170), (170, 190, 220), (170, 220, 255)))
    save(BLOCK, 'trillimac', small_mushroom(rng, (150, 136, 112), (110, 98, 80), (190, 180, 160), (200, 190, 168)))


if __name__ == '__main__':
    main()
