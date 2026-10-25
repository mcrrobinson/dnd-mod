"""Generates the Underdark building blocks' textures (design 9, T6).

    python3 tools/underdark_building_textures.py

Writes 16x16 textures under textures/block/ (and two item icons under textures/item/):
- drowstone_bricks.png, drowstone_brick_pillar.png / _top.png: black bricks with a faint violet sheen; a fluted
  pillar.
- polished_faerzress.png: Faerzress Stone ground smooth, a dark bevelled slab with glowing violet veins.
- spider_sigil_stone.png: drowstone carved with an eight-legged spider sigil, its inlay glowing violet.
- faerie_lantern.png (an atlas in vanilla's lantern layout) and item/faerie_lantern.png: black iron, violet flame.
- spidersilk_block.png, spidersilk_curtain.png / _top.png: pale silver woven silk; a hanging curtain of it.
- web_bridge.png / web_bridge_side.png: a woven silk walkway and its rope edge.
- greysteel_plate.png, greysteel_trapdoor.png: soot-dark steel plates with red-hot rivets; a grille hatch.
- basalt_forge_bricks.png / _top.png: dark basalt bricks with ember-glow mortar.
- duergar_brazier_side.png / duergar_brazier_coals.png: black iron bowl; glowing coals.
- chain_cage.png: black iron bars wrapped in chain (cut out).
- item/duergar_steel_ingot.png.
Deterministic (seeded), so reruns give the same pixels.
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from underdark_block_textures import (BLOCK, CLEAR, ITEM, N, grid, mix, noise_field, rgba, save,  # noqa: E402
                                      shade)

VIOLET = (206, 120, 255)
VIOLET_DIM = (132, 64, 180)


# --- drow stone --------------------------------------------------------------------------------------------------
DROW = (38, 34, 46)
DROW_LIGHT = (62, 54, 76)
DROW_MORTAR = (18, 16, 24)


def bricks(rng, base, light, mortar, sheen=None):
    """Bricks 8 wide and 4 tall, alternate rows offset by 4, mortar on their bottom and right edges."""
    rows = grid(rgba(base))
    for y in range(N):
        course = y // 4
        for x in range(N):
            bx = (x + (4 if course % 2 else 0)) % N
            if y % 4 == 3 or bx % 8 == 7:
                rows[y][x] = shade(rgba(mortar), rng.randint(-3, 3))
                continue
            c = rgba(base)
            if y % 4 == 0 or bx % 8 == 0:
                c = rgba(light)  # the brick's lit top-left edge
            rows[y][x] = shade(c, rng.randint(-5, 5))
            if sheen and rng.random() < 0.06:
                rows[y][x] = rgba(sheen)
    return rows


def drowstone_bricks(rng):
    return bricks(rng, DROW, DROW_LIGHT, DROW_MORTAR, sheen=(84, 64, 112))


def drowstone_pillar(rng):
    rows = grid(rgba(DROW))
    for x in range(N):
        flute = x % 4
        base = {0: DROW_MORTAR, 1: DROW_LIGHT, 2: DROW, 3: DROW}[flute]
        for y in range(N):
            rows[y][x] = shade(rgba(base), rng.randint(-4, 4))
    for x in range(N):  # bands top and bottom
        for y in (0, 1, 14, 15):
            rows[y][x] = shade(rgba(DROW_LIGHT if y in (0, 15) else DROW_MORTAR), rng.randint(-3, 3))
    for y in range(3, 13):  # a thin violet thread in one flute
        if rng.random() < 0.7:
            rows[y][9] = rgba((96, 70, 130))
    return rows


def drowstone_pillar_top(rng):
    rows = grid(rgba(DROW))
    for y in range(N):
        for x in range(N):
            edge = min(x, y, 15 - x, 15 - y)
            c = DROW_LIGHT if edge == 0 else DROW_MORTAR if edge == 1 else DROW
            rows[y][x] = shade(rgba(c), rng.randint(-4, 4))
            if edge >= 2 and math.hypot(x - 7.5, y - 7.5) < 2.2:
                rows[y][x] = rgba((88, 66, 120))
    return rows


def polished_faerzress(rng):
    deep = (64, 60, 70)
    field = noise_field(rng, 8)
    rows = grid(rgba(deep))
    for y in range(N):
        for x in range(N):
            rows[y][x] = shade(rgba(deep), int((field[y][x] - 0.5) * 10) + rng.randint(-3, 3))
            edge = min(x, y, 15 - x, 15 - y)
            if edge == 0:
                rows[y][x] = shade(rows[y][x], 14 if x == 0 or y == 0 else -18)
    # One soft vein and a few motes.
    x, y = 1, rng.randrange(4, 12)
    while x < 15:
        rows[y][x] = rgba(VIOLET)
        if 1 <= y + 1 < 15:
            rows[y + 1][x] = rgba(VIOLET_DIM)
        x += 1
        y = max(2, min(13, y + rng.choice((-1, 0, 0, 1))))
    for _ in range(3):
        rows[rng.randrange(2, 14)][rng.randrange(2, 14)] = rgba(VIOLET_DIM)
    return rows


def spider_sigil_stone(rng):
    rows = grid(rgba(DROW))
    for y in range(N):
        for x in range(N):
            edge = min(x, y, 15 - x, 15 - y)
            c = DROW_LIGHT if edge == 0 else DROW_MORTAR if edge == 1 else DROW
            rows[y][x] = shade(rgba(c), rng.randint(-4, 4))
    glow, dim = rgba(VIOLET), rgba(VIOLET_DIM)
    # Body: a head and an abdomen down the middle.
    for y, half in ((4, 1), (5, 1), (6, 1), (7, 2), (8, 2), (9, 2), (10, 2), (11, 1)):
        for x in range(8 - half, 8 + half):
            rows[y][x] = glow
    # Eight legs, bent like a spider's: four each side.
    legs = [((6, 5), (4, 3), (3, 2)), ((6, 6), (4, 6), (2, 5)), ((6, 8), (4, 9), (2, 10)),
            ((6, 9), (4, 12), (3, 13))]
    for leg in legs:
        for (x, y) in leg:
            rows[y][x] = dim
            rows[y][15 - x] = dim
        (x0, y0), (x1, y1), _ = leg
        rows[(y0 + y1) // 2][(x0 + x1) // 2] = dim
        rows[(y0 + y1) // 2][15 - (x0 + x1) // 2] = dim
    return rows


# --- faerie fire lantern -------------------------------------------------------------------------------------------
IRON = (34, 32, 38)
IRON_LIGHT = (70, 66, 78)


def faerie_lantern_atlas(rng):
    """Vanilla's lantern layout: body sides x0-5 y2-8, top/bottom x0-5 y9-14, cap sides x1-4 y0-1, chain x11-13."""
    rows = grid(CLEAR)
    # Body side: iron frame round violet glass, the flame brightest low in the middle.
    for y in range(2, 9):
        for x in range(0, 6):
            if x in (0, 5) or y in (2, 8):
                rows[y][x] = shade(rgba(IRON_LIGHT if y == 2 else IRON), rng.randint(-3, 3))
            else:
                d = math.hypot(x - 2.5, (y - 5.5) * 0.8)
                rows[y][x] = mix((255, 230, 255), (150, 70, 220), min(1.0, d / 2.4))
    # Top and bottom.
    for y in range(9, 15):
        for x in range(0, 6):
            edge = min(x, y - 9, 5 - x, 14 - y)
            rows[y][x] = shade(rgba(IRON_LIGHT if edge == 0 else IRON), rng.randint(-3, 3))
    # Cap sides.
    for y in range(0, 2):
        for x in range(1, 5):
            rows[y][x] = shade(rgba(IRON_LIGHT if y == 0 else IRON), rng.randint(-3, 3))
    # Chain / handle strips: links in dark iron.
    for y in range(1, 13):
        for x in range(11, 14):
            link = (y // 2) % 2
            if (x == 12) == bool(link):
                rows[y][x] = rgba(IRON_LIGHT if x == 12 else IRON)
            else:
                rows[y][x] = rgba(IRON) if link == 0 else CLEAR
    return rows


def faerie_lantern_item(rng):
    rows = grid(CLEAR)
    for y in range(1, 4):  # handle loop
        for x in range(6, 10):
            if y == 1 or x in (6, 9):
                rows[y][x] = rgba(IRON_LIGHT)
    for x in range(5, 11):  # cap
        rows[4][x] = rgba(IRON_LIGHT)
        rows[5][x] = rgba(IRON)
    for y in range(6, 14):
        for x in range(4, 12):
            if x in (4, 11) or y in (6, 13):
                rows[y][x] = shade(rgba(IRON), rng.randint(-3, 3))
            else:
                d = math.hypot(x - 7.5, (y - 10) * 0.8)
                rows[y][x] = mix((255, 230, 255), (150, 70, 220), min(1.0, d / 3.2))
    for x in range(5, 11):
        rows[14][x] = rgba(IRON_LIGHT)
    return rows


# --- spidersilk ----------------------------------------------------------------------------------------------------
SILK = (208, 210, 222)
SILK_DARK = (160, 162, 180)
SILK_SHINE = (236, 238, 248)


def spidersilk_block(rng):
    rows = grid(rgba(SILK))
    for y in range(N):
        for x in range(N):
            # A fine diagonal weave, two directions.
            a = (x + y) % 4 == 0
            b = (x - y) % 4 == 0
            c = SILK_SHINE if a and not b else SILK_DARK if b and not a else SILK
            rows[y][x] = shade(rgba(c), rng.randint(-4, 4))
    return rows


def spidersilk_curtain(rng):
    """Hanging strands, denser in folds, with see-through gaps between them (cut out)."""
    rows = grid(CLEAR)
    for x in range(N):
        fold = x % 5
        density = {0: 1.0, 1: 0.9, 2: 0.55, 3: 0.75, 4: 0.95}[fold]
        for y in range(N):
            if rng.random() < density:
                c = SILK_SHINE if fold == 1 else SILK_DARK if fold == 3 else SILK
                rows[y][x] = shade(rgba(c), rng.randint(-5, 5))
    for x in range(N):  # a hem along the top
        rows[0][x] = rgba(SILK_DARK)
        rows[1][x] = rgba(SILK)
    return rows


def spidersilk_curtain_top(rng):
    rows = grid(CLEAR)
    for y in range(N):
        for x in range(7, 9):
            rows[y][x] = shade(rgba(SILK if x == 7 else SILK_DARK), rng.randint(-4, 4))
    return rows


def web_bridge(rng):
    """Thick silk ropes woven over and under each other, dark gaps between."""
    gap = (44, 40, 54)
    rows = grid(rgba(gap))
    for y in range(N):
        for x in range(N):
            on_x = y % 4 in (0, 1)
            on_z = x % 4 in (0, 1)
            if on_x and on_z:
                over_x = ((x // 4) + (y // 4)) % 2 == 0
                c = SILK_SHINE if over_x else SILK_DARK
            elif on_x or on_z:
                c = SILK
            else:
                rows[y][x] = shade(rgba(gap), rng.randint(-4, 4))
                continue
            rows[y][x] = shade(rgba(c), rng.randint(-5, 5))
    return rows


def web_bridge_side(rng):
    rows = grid(CLEAR)
    for y in range(8):
        for x in range(N):
            twist = (x + y * 2) % 6
            c = SILK_SHINE if twist < 2 else SILK if twist < 4 else SILK_DARK
            rows[y][x] = shade(rgba(c), rng.randint(-4, 4))
    return rows


# --- duergar metal and stone -------------------------------------------------------------------------------------
STEEL = (70, 70, 76)
STEEL_LIGHT = (100, 100, 108)
STEEL_DARK = (40, 40, 46)
RIVET = (232, 96, 40)
RIVET_CORE = (255, 190, 90)


def greysteel_plate(rng):
    rows = grid(rgba(STEEL))
    for y in range(N):
        for x in range(N):
            px, py = x % 8, y % 8
            c = STEEL
            if px == 7 or py == 7:
                c = STEEL_DARK  # seam
            elif px == 0 or py == 0:
                c = STEEL_LIGHT
            rows[y][x] = shade(rgba(c), rng.randint(-4, 4))
            if rng.random() < 0.04 and c == STEEL:
                rows[y][x] = shade(rgba(STEEL_DARK), 8)  # soot
    # Rivets in the plates' corners.
    for ox in (0, 8):
        for oy in (0, 8):
            for dx, dy in ((1, 1), (5, 1), (1, 5), (5, 5)):
                rows[oy + dy][ox + dx] = rgba(RIVET)
                rows[oy + dy][ox + dx + 1] = shade(rgba(RIVET), -60)
    for ox in (0, 8):
        for oy in (0, 8):
            rows[oy + 1][ox + 1] = rgba(RIVET_CORE)
    return rows


def greysteel_trapdoor(rng):
    rows = greysteel_plate(rng)
    for y in range(N):
        for x in range(N):
            if 2 <= x <= 13 and 2 <= y <= 13 and x % 3 == 1 and y % 3 == 1:
                rows[y][x] = CLEAR  # grille
    for i in range(N):
        for j in (0, 15):
            rows[i][j] = rgba(STEEL_DARK)
            rows[j][i] = rgba(STEEL_DARK)
    return rows


BASALT = (52, 50, 56)
BASALT_LIGHT = (78, 76, 84)
EMBER = (214, 92, 30)


def basalt_forge_bricks(rng):
    rows = bricks(rng, BASALT, BASALT_LIGHT, EMBER)
    for y in range(N):  # brighter glints in the mortar
        for x in range(N):
            if rows[y][x][0] > 150 and rng.random() < 0.3:
                rows[y][x] = rgba((255, 170, 70))
    return rows


def basalt_forge_bricks_top(rng):
    rows = grid(rgba(BASALT))
    for y in range(N):
        for x in range(N):
            edge = min(x, y, 15 - x, 15 - y)
            c = BASALT_LIGHT if edge == 0 else BASALT
            if x in (7, 8) or y in (7, 8):
                c = EMBER
            rows[y][x] = shade(rgba(c), rng.randint(-4, 4))
    return rows


def brazier_side(rng):
    rows = grid(rgba(IRON))
    for y in range(N):
        for x in range(N):
            c = IRON_LIGHT if y in (4, 10) else IRON
            rows[y][x] = shade(rgba(c), rng.randint(-4, 4))
    for x in range(1, N, 4):  # rivets on the rim band
        rows[4][x] = rgba(RIVET)
    return rows


def brazier_coals(rng):
    rows = grid(rgba((40, 20, 16)))
    field = noise_field(rng, 4)
    for y in range(N):
        for x in range(N):
            heat = field[y][x]
            if heat > 0.62:
                c = (255, 200, 90)
            elif heat > 0.45:
                c = (236, 110, 34)
            elif heat > 0.3:
                c = (150, 40, 20)
            else:
                c = (40, 22, 20)
            rows[y][x] = shade(rgba(c), rng.randint(-6, 6))
    for x in range(N):  # the bowl's rim round the coals
        for y in (0, 15):
            rows[y][x] = rgba(IRON_LIGHT)
            rows[x][y] = rgba(IRON_LIGHT)
    return rows


def chain_cage(rng):
    rows = grid(CLEAR)
    for x in (1, 5, 10, 14):  # vertical bars
        for y in range(N):
            rows[y][x] = shade(rgba(IRON_LIGHT), rng.randint(-4, 4))
            rows[y][x + 1 if x < 15 else x] = shade(rgba(IRON), rng.randint(-4, 4))
    for y in (0, 1, 14, 15):  # top and bottom bands
        for x in range(N):
            rows[y][x] = shade(rgba(IRON_LIGHT if y in (0, 14) else IRON), rng.randint(-4, 4))
    # A chain wrapped diagonally across the middle, links alternating.
    for i in range(N):
        y = 4 + i // 2
        c = (120, 118, 130) if i % 2 == 0 else (84, 82, 96)
        rows[y][i] = rgba(c)
        if i % 2 == 0 and y + 1 < N:
            rows[y + 1][i] = rgba((60, 58, 70))
    return rows


def duergar_steel_ingot(rng):
    rows = grid(CLEAR)
    # An ingot seen at an angle, like vanilla's: a parallelogram band.
    for y in range(5, 12):
        for x in range(2, 14):
            if x - (11 - y) < 2 or x - (11 - y) > 10:
                continue
            top = y <= 6
            c = STEEL_LIGHT if top else STEEL if y < 10 else STEEL_DARK
            rows[y][x] = shade(rgba(c), rng.randint(-4, 4))
    # A reddish forge sheen along the top edge.
    for x in range(8, 14):
        if rows[5][x][3]:
            rows[5][x] = rgba((170, 110, 100))
    for y in range(5, 12):
        for x in range(N):
            if rows[y][x][3] and (x == 0 or not rows[y][x - 1][3]):
                rows[y][x] = rgba((28, 28, 32))
    return rows


def main():
    rng = random.Random(6)
    save(BLOCK, 'drowstone_bricks', drowstone_bricks(rng))
    save(BLOCK, 'drowstone_brick_pillar', drowstone_pillar(rng))
    save(BLOCK, 'drowstone_brick_pillar_top', drowstone_pillar_top(rng))
    save(BLOCK, 'polished_faerzress', polished_faerzress(rng))
    save(BLOCK, 'spider_sigil_stone', spider_sigil_stone(rng))
    save(BLOCK, 'faerie_lantern', faerie_lantern_atlas(rng))
    save(ITEM, 'faerie_lantern', faerie_lantern_item(rng))
    save(BLOCK, 'spidersilk_block', spidersilk_block(rng))
    save(BLOCK, 'spidersilk_curtain', spidersilk_curtain(rng))
    save(BLOCK, 'spidersilk_curtain_top', spidersilk_curtain_top(rng))
    save(BLOCK, 'web_bridge', web_bridge(rng))
    save(BLOCK, 'web_bridge_side', web_bridge_side(rng))
    save(BLOCK, 'greysteel_plate', greysteel_plate(random.Random(61)))
    save(BLOCK, 'greysteel_trapdoor', greysteel_trapdoor(random.Random(61)))
    save(BLOCK, 'basalt_forge_bricks', basalt_forge_bricks(rng))
    save(BLOCK, 'basalt_forge_bricks_top', basalt_forge_bricks_top(rng))
    save(BLOCK, 'duergar_brazier_side', brazier_side(rng))
    save(BLOCK, 'duergar_brazier_coals', brazier_coals(rng))
    save(BLOCK, 'chain_cage', chain_cage(rng))
    save(ITEM, 'duergar_steel_ingot', duergar_steel_ingot(rng))


if __name__ == '__main__':
    main()
