"""Generates the Tattered Carpet textures and resources for desolate settlements.

    python3 tools/tattered_carpet_textures.py

For each of the 16 dye colours:
- textures/block/tattered_<color>_wool.png: woven wool in the dye's colour, worn threadbare in patches, stained dark,
  with a few holes torn right through (transparent, so the floor shows) and frayed threads round them
- blockstates/, models/block/, models/item/ and loot_tables/blocks/ tattered_<color>_carpet.json
The lang names are in en_us.json by hand ("Tattered <Colour> Carpet").
"""
import json
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/dndclasses')
DATA = os.path.join(ROOT, 'src/main/resources/data/dndclasses')

N = 16
CLEAR = (0, 0, 0, 0)

# Roughly the vanilla wool colours.
COLORS = {
    'white': (226, 228, 228), 'orange': (232, 112, 20), 'magenta': (182, 66, 172), 'light_blue': (60, 168, 210),
    'yellow': (240, 190, 40), 'lime': (108, 178, 26), 'pink': (228, 136, 166), 'gray': (64, 70, 72),
    'light_gray': (140, 140, 132), 'cyan': (22, 132, 140), 'purple': (118, 42, 166), 'blue': (54, 58, 152),
    'brown': (112, 70, 40), 'green': (82, 106, 28), 'red': (156, 38, 34), 'black': (24, 24, 30),
}
DIRT = (74, 58, 42)


def clamp(v):
    return max(0, min(255, int(v)))


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def texture(name, base):
    rng = random.Random('tattered-' + name)
    rows = [[None] * N for _ in range(N)]
    # Weave: alternating light and dark threads, with noise.
    for y in range(N):
        for x in range(N):
            weave = 10 if (x + y) % 2 == 0 else -10
            d = weave + rng.randint(-12, 12)
            rows[y][x] = tuple(clamp(c + d) for c in base)
    # Threadbare patches: faded towards grey-white.
    for _ in range(3):
        cx, cy, r = rng.randrange(N), rng.randrange(N), rng.uniform(1.5, 3.5)
        for y in range(N):
            for x in range(N):
                dx = min(abs(x - cx), N - abs(x - cx))
                dy = min(abs(y - cy), N - abs(y - cy))
                if dx * dx + dy * dy <= r * r and rng.random() < 0.8:
                    rows[y][x] = mix(rows[y][x], (176, 168, 152), 0.35)
    # Stains of dirt.
    for _ in range(2):
        cx, cy, r = rng.randrange(N), rng.randrange(N), rng.uniform(1.5, 3.0)
        for y in range(N):
            for x in range(N):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r and rng.random() < 0.75:
                    rows[y][x] = mix(rows[y][x], DIRT, 0.45)
    # Holes torn through, ragged, with frayed lighter threads round the edge.
    holes = set()
    for _ in range(rng.randint(2, 3)):
        cx, cy = rng.randrange(2, N - 2), rng.randrange(2, N - 2)
        r = rng.uniform(1.2, 2.4)
        for y in range(N):
            for x in range(N):
                if (x - cx) ** 2 + (y - cy) ** 2 <= (r + rng.uniform(-0.6, 0.6)) ** 2:
                    holes.add((x, y))
    out = []
    for y in range(N):
        row = []
        for x in range(N):
            if (x, y) in holes:
                row.append(CLEAR)
                continue
            c = rows[y][x]
            if any((x + dx, y + dy) in holes for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                c = mix(c, (200, 190, 170), 0.3) if rng.random() < 0.6 else mix(c, (0, 0, 0), 0.25)
            row.append(c + (255,))
        out.append(row)
    return out


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def main():
    for color, base in COLORS.items():
        block = 'tattered_%s_carpet' % color
        wool = 'tattered_%s_wool' % color
        pngio.write_png(os.path.join(ASSETS, 'textures/block', wool + '.png'), N, N, texture(color, base))
        write_json(os.path.join(ASSETS, 'blockstates', block + '.json'),
                   {'variants': {'': {'model': 'dndclasses:block/' + block}}})
        write_json(os.path.join(ASSETS, 'models/block', block + '.json'),
                   {'parent': 'minecraft:block/carpet', 'textures': {'wool': 'dndclasses:block/' + wool}})
        write_json(os.path.join(ASSETS, 'models/item', block + '.json'), {'parent': 'dndclasses:block/' + block})
        write_json(os.path.join(DATA, 'loot_tables/blocks', block + '.json'), {
            'type': 'minecraft:block',
            'pools': [{'bonus_rolls': 0.0, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                       'entries': [{'type': 'minecraft:item', 'name': 'dndclasses:' + block}], 'rolls': 1.0}]})


if __name__ == '__main__':
    main()
