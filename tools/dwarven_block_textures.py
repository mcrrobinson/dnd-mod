"""Generates the Dwarven Fortress building-block textures (Building Blocks epic).

    python3 tools/dwarven_block_textures.py

16x16 textures/block/runic_deepslate_bricks.png: large, even deepslate-grey bricks (two courses of 8x8,
the lower one offset by half a brick) with thin gold-inlaid angular runes on two of them. Stairs, slab and
wall share it (tools/block_families.py).
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/block')

N = 16
MORTAR = (38, 38, 44)
STONE = (78, 78, 84)
GOLD = (222, 178, 62)
GOLD_DARK = (190, 144, 48)

# Runes as pixel offsets inside an 8x8 brick (1-px straight strokes, Dethek-like): keyed by the brick's
# top-left corner in the texture.
RUNES = {
    # a stave with two arms raised to the right
    (0, 0): [(2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (3, 2), (4, 1), (3, 4), (4, 3)],
    # a forked stave, like a Y
    (12, 8): [(1, 1), (2, 2), (5, 1), (4, 2), (3, 3), (3, 4), (3, 5)],
}


def shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)


def runic_bricks():
    rng = random.Random(41)
    rows = [[None] * N for _ in range(N)]
    for y in range(N):
        course = y // 8
        offset = 4 if course == 1 else 0
        for x in range(N):
            bx = (x + offset) % 8
            by = y % 8
            if bx == 7 or by == 7:
                rows[y][x] = MORTAR + (255,)
                continue
            # Each brick has its own tone; bevel light on the top/left, dark on the bottom/right.
            brick = ((x + offset) % N) // 8 + course * 2
            tone = (-4, 3, 6, -2)[brick % 4]
            d = tone + rng.randint(-5, 5)
            if bx == 0 or by == 0:
                d += 12
            elif bx == 6 or by == 6:
                d -= 12
            rows[y][x] = shade(STONE, d)
    for (ox, oy), strokes in RUNES.items():
        for (dx, dy) in strokes:
            x, y = (ox + dx) % N, oy + dy
            # The inlay is bright at the top of each stroke and duller lower down.
            rows[y][x] = (GOLD if dy <= 3 else GOLD_DARK) + (255,)
    return rows


def main():
    os.makedirs(OUT, exist_ok=True)
    pngio.write_png(os.path.join(OUT, 'runic_deepslate_bricks.png'), N, N, runic_bricks())
    print('wrote runic_deepslate_bricks.png')


if __name__ == '__main__':
    main()
