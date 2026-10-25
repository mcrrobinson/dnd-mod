"""Generates the textures of the Underdark's plants, fungi and lake life (design 9, T3).

    python3 tools/underdark_flora_textures.py

Writes 16x16 textures under textures/block/ and textures/item/:
- zurkhwood_gills.png: the tan gills under a giant zurkhwood's cap.
- zurkhwood_spores.png / nightlight_spores.png: a little puff of violet or glowing blue spore-fluff (cross models).
- ripplebark.png (block) and item/ripplebark.png: a rippled orange-brown wall shelf, and a broken-off piece of it.
- barrelstalk_side_lower/upper.png, barrelstalk_top_filled/empty.png, item/barrelstalk.png: a bulbous ribbed
  grey-brown fungus barrel, its open top full of dark water or dry.
- waterorb.png: a float of pale glowing spheres on a lily-pad-like mat.
- faerie_lichen.png: violet glowing lichen strands (glow lichen's shape).
- spore_drip.png: a hanging string of glowing blue spores.
- pearl_clam.png: a dark clam shell bedded in grey clay.
- item/trillimac_slice.png, item/roasted_trillimac.png, item/glow_spores.png, item/darklake_pearl.png.
Deterministic (seeded), so reruns give the same pixels.
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from underdark_block_textures import (BLOCK, CLEAR, GILL, ITEM, N, gills, grid, mix, noise_field, rgba,  # noqa: E402
                                      save, shade)


def spores(rng, base, bright):
    rows = grid(CLEAR)
    # A low tuft of fluff on a few pale threads.
    for _ in range(3):
        x = rng.randrange(4, 12)
        for y in range(11, 16):
            if rng.random() < 0.7:
                rows[y][x] = rgba((200, 196, 206))
    for _ in range(46):
        x = int(round(7.5 + rng.gauss(0, 2.6)))
        y = int(round(10 + rng.gauss(0, 1.8)))
        if 0 <= x < N and 3 <= y < N:
            rows[y][x] = shade(rgba(base), rng.randint(-14, 14))
    for _ in range(8):
        x, y = rng.randrange(4, 12), rng.randrange(7, 13)
        rows[y][x] = rgba(bright)
    return rows


RIPPLE = (176, 104, 54)
RIPPLE_DARK = (116, 62, 34)
RIPPLE_EDGE = (226, 170, 110)


def ripplebark(rng):
    """Concentric wavy bands, like a bracket fungus seen from above."""
    rows = grid(rgba(RIPPLE))
    for y in range(N):
        for x in range(N):
            r = math.hypot(x - 7.5, (y - 1) * 1.1) + math.sin(x * 1.3) * 0.6
            band = int(r) % 3
            base = {0: RIPPLE_EDGE, 1: RIPPLE, 2: RIPPLE_DARK}[band]
            rows[y][x] = shade(rgba(base), rng.randint(-8, 8))
    return rows


def ripplebark_item(rng):
    rows = grid(CLEAR)
    for y in range(4, 13):
        for x in range(2, 14):
            if math.hypot((x - 7.5) / 6.0, (y - 12) / 8.0) < 1.0:
                r = math.hypot(x - 7.5, (y - 12) * 0.8) + math.sin(x * 1.4) * 0.5
                base = {0: RIPPLE_EDGE, 1: RIPPLE, 2: RIPPLE_DARK}[int(r) % 3]
                rows[y][x] = shade(rgba(base), rng.randint(-6, 6))
    for x in range(3, 13):
        rows[12][x] = rgba(RIPPLE_DARK)
    return rows


BARREL = (120, 104, 96)
BARREL_DARK = (78, 66, 64)
BARREL_LIGHT = (160, 144, 128)


def barrel_side(rng, top_band):
    rows = grid(rgba(BARREL))
    for y in range(N):
        for x in range(N):
            base = BARREL_DARK if x % 4 == 0 else (BARREL_LIGHT if x % 4 == 2 else BARREL)
            rows[y][x] = shade(rgba(base), rng.randint(-6, 6))
    if top_band:
        for x in range(N):
            rows[0][x] = rows[1][x] = shade(rgba(BARREL_LIGHT), 14)
    # Pale warts.
    for _ in range(6):
        x, y = rng.randrange(N), rng.randrange(2, N)
        rows[y][x] = rgba((196, 186, 168))
    return rows


def barrel_top(rng, filled):
    rows = grid(rgba(BARREL_LIGHT))
    for y in range(N):
        for x in range(N):
            r = math.hypot(x - 7.5, y - 7.5)
            if r < 4.5:
                if filled:
                    rows[y][x] = mix(rgba((24, 40, 86)), rgba((60, 90, 170)), max(0.0, 1 - r / 4.5))
                    if rng.random() < 0.08:
                        rows[y][x] = rgba((150, 190, 240))
                else:
                    rows[y][x] = shade(rgba(BARREL_DARK), -20 + rng.randint(-4, 4))
            elif r < 6:
                rows[y][x] = shade(rgba(BARREL_LIGHT), 12 + rng.randint(-5, 5))
            else:
                rows[y][x] = shade(rgba(BARREL), rng.randint(-6, 6))
    return rows


def barrel_item(rng):
    rows = grid(CLEAR)
    for y in range(1, 16):
        half = {1: 3, 2: 4, 3: 4}.get(y, 5 if y < 13 else 4)
        for x in range(8 - half, 8 + half):
            base = BARREL_DARK if x % 3 == 0 else BARREL
            rows[y][x] = shade(rgba(base), rng.randint(-6, 6))
    for x in range(6, 10):
        rows[1][x] = rgba((50, 80, 160))
    return rows


def waterorb(rng):
    rows = grid(CLEAR)
    mat = (36, 70, 72)
    for y in range(N):
        for x in range(N):
            if math.hypot(x - 7.5, y - 7.5) < 7.2 and not (x > 7 and abs(y - 7.5) < 1.2):
                rows[y][x] = shade(rgba(mat), rng.randint(-8, 8))
    # Glowing orbs.
    for cx, cy, r in ((5, 5, 2.0), (10, 4, 1.6), (4, 10, 1.5), (9, 11, 2.2), (12, 9, 1.2)):
        for y in range(N):
            for x in range(N):
                d = math.hypot(x - cx, y - cy)
                if d <= r:
                    rows[y][x] = mix(rgba((150, 230, 220)), rgba((236, 255, 250)), max(0.0, 1 - d / r))
    return rows


def faerie_lichen(rng):
    rows = grid(CLEAR)
    for _ in range(7):
        x, y = rng.randrange(N), rng.randrange(N)
        for _ in range(9):
            if 0 <= x < N and 0 <= y < N:
                rows[y][x] = shade(rgba((168, 92, 220)), rng.randint(-14, 10))
                if rng.random() < 0.25:
                    rows[y][x] = rgba((240, 190, 255))
            x += rng.choice((-1, 0, 1))
            y += rng.choice((-1, 0, 1))
    return rows


def spore_drip(rng):
    rows = grid(CLEAR)
    for x0 in (6, 9):
        length = rng.randint(10, 14)
        for y in range(length):
            x = x0 + (1 if (y // 4) % 2 else 0)
            rows[y][x] = shade(rgba((150, 200, 236)), rng.randint(-10, 10))
            if y % 3 == 2:
                rows[y][x] = rgba((226, 246, 255))
        rows[length][x0] = rgba((246, 252, 255))
    return rows


def pearl_clam(rng):
    field = noise_field(rng, 4)
    rows = grid(rgba((150, 156, 168)))
    for y in range(N):
        for x in range(N):
            rows[y][x] = shade(rgba((150, 156, 168)), int((field[y][x] - 0.5) * 22) + rng.randint(-4, 4))
    # The shell: a dark fan with ridges, slightly open with a pale glint.
    for y in range(4, 13):
        for x in range(3, 13):
            if math.hypot((x - 7.5) / 5.0, (y - 12) / 8.0) < 1.0:
                ridge = (int(math.degrees(math.atan2(12 - y, x - 7.5))) // 18) % 2
                rows[y][x] = shade(rgba((52, 50, 66) if ridge else (72, 68, 90)), rng.randint(-5, 5))
    for x in range(5, 11):
        rows[12][x] = rgba((32, 30, 40))
    rows[11][8] = rows[11][7] = rgba((220, 220, 236))
    return rows


def slice_item(rng, roasted):
    rows = grid(CLEAR)
    flesh = (196, 180, 150) if not roasted else (172, 110, 58)
    rim = (130, 116, 92) if not roasted else (98, 56, 28)
    for y in range(N):
        for x in range(N):
            d = math.hypot((x - 7.5) / 6.5, (y - 8.5) / 4.5)
            if d < 1.0:
                rows[y][x] = shade(rgba(rim if d > 0.75 else flesh), rng.randint(-8, 8))
                if roasted and rng.random() < 0.12:
                    rows[y][x] = rgba((70, 38, 18))
    return rows


def glow_spores(rng):
    rows = grid(CLEAR)
    for _ in range(40):
        x = int(round(7.5 + rng.gauss(0, 2.4)))
        y = int(round(8.5 + rng.gauss(0, 2.4)))
        if 0 <= x < N and 0 <= y < N:
            rows[y][x] = shade(rgba((120, 190, 240)), rng.randint(-20, 20))
    for _ in range(9):
        x, y = rng.randrange(4, 12), rng.randrange(5, 13)
        rows[y][x] = rgba((236, 250, 255))
    return rows


def darklake_pearl(rng):
    rows = grid(CLEAR)
    for y in range(N):
        for x in range(N):
            d = math.hypot(x - 7.5, y - 8.0)
            if d < 4.6:
                t = max(0.0, 1 - math.hypot(x - 6.0, y - 6.5) / 6.0)
                rows[y][x] = mix(rgba((22, 20, 36)), rgba((150, 130, 190)), t * t)
    rows[6][6] = rgba((236, 230, 255))
    return rows


def main():
    rng = random.Random(33)
    save(BLOCK, 'zurkhwood_gills', gills(rng, GILL, (150, 120, 86)))
    save(BLOCK, 'zurkhwood_spores', spores(rng, (128, 92, 140), (200, 170, 214)))
    save(BLOCK, 'nightlight_spores', spores(rng, (150, 200, 236), (240, 250, 255)))
    save(BLOCK, 'ripplebark', ripplebark(rng))
    save(ITEM, 'ripplebark', ripplebark_item(rng))
    save(BLOCK, 'barrelstalk_side_lower', barrel_side(rng, False))
    save(BLOCK, 'barrelstalk_side_upper', barrel_side(rng, True))
    save(BLOCK, 'barrelstalk_top_filled', barrel_top(rng, True))
    save(BLOCK, 'barrelstalk_top_empty', barrel_top(rng, False))
    save(ITEM, 'barrelstalk', barrel_item(rng))
    save(BLOCK, 'waterorb', waterorb(rng))
    save(BLOCK, 'faerie_lichen', faerie_lichen(rng))
    save(BLOCK, 'spore_drip', spore_drip(rng))
    save(BLOCK, 'pearl_clam', pearl_clam(rng))
    save(ITEM, 'trillimac_slice', slice_item(rng, False))
    save(ITEM, 'roasted_trillimac', slice_item(rng, True))
    save(ITEM, 'glow_spores', glow_spores(rng))
    save(ITEM, 'darklake_pearl', darklake_pearl(rng))


if __name__ == '__main__':
    main()
