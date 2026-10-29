"""Generates the Ancient Red Dragon's texture by recolouring the Lightning Chaser's blue.png.

    python3 tools/ancient_dragon_model.py      (first: the model and its texture patches)
    python3 tools/ancient_dragon_texture.py

ancient_dragon.geo.json keeps the Lightning Chaser's UV rectangles (see ancient_dragon_model.py), so the
chaser's sheet is the starting point:
- scales: crimson, darkening to near black on the top faces of the neck, trunk and tail (the spine)
- belly plates (bottom faces of the neck, trunk and tail): ember orange with dark seams
- spikes and horns: charcoal; wing membranes: smoky dark red with ember veins
- eyes: molten yellow

Also paints the patches the model's new cubes read (horns, dorsal spines, frills, tail blade) and writes
red_glowmask.png (GeckoLib's AutoGlowingGeoLayer draws it fullbright): eyes, throat and belly seams.
"""
import colorsys
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402
from ancient_dragon_model import PATCHES  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/dndclasses')
SOURCE = os.path.join(ASSETS, 'textures/entity/lightning_chaser/blue.png')
GEO = os.path.join(ASSETS, 'geo/lightning_chaser.geo.json')
OUT = os.path.join(ASSETS, 'textures/entity/ancient_dragon')

TRUNK = ('front', 'back', 'neck1', 'neck2', 'neck3', 'tail1', 'tail2', 'tail3', 'tail4')


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def rect(u, v, w, h):
    for x in range(min(u, u + w), max(u, u + w)):
        for y in range(min(v, v + h), max(v, v + h)):
            yield x, y


def cube_texels(cube, face=None):
    """Texels a cube's faces read (box UV: every face, or just 'up' / 'down')."""
    uv = cube.get('uv')
    if uv is None:
        return
    if isinstance(uv, dict):
        for name, f in uv.items():
            if face is None or name == face:
                u, v = f['uv']
                du, dv = f.get('uv_size', [0, 0])
                yield from rect(int(u), int(v), int(du), int(dv))
        return
    u, v = uv
    sx, sy, sz = (int(math.floor(s)) for s in cube['size'])
    if face == 'up':
        yield from rect(u + sz, v, sx, sz)
    elif face == 'down':
        yield from rect(u + sz + sx, v, sx, sz)
    else:
        yield from rect(u, v, 2 * (sx + sz), sz + sy)


def bone_texels(predicate, face=None):
    with open(GEO) as f:
        bones = json.load(f)['minecraft:geometry'][0]['bones']
    texels = set()
    for bone in bones:
        if predicate(bone['name']):
            for cube in bone.get('cubes', []):
                texels.update(cube_texels(cube, face))
    return texels


def recolour(x, y, r, g, b, kind):
    """Returns (colour, glow) where glow is the glowmask colour or None."""
    h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
    hue = h * 360
    if 120 <= hue < 200 and s >= 0.6:
        # Eyes: molten yellow
        c = lerp((255, 170, 30), (255, 240, 150), (l - 0.3) / 0.4)
        return c, c
    if (hue < 15 or hue > 340) and s >= 0.3:
        # Mouth and throat: furnace red, glowing
        c = lerp((150, 30, 10), (255, 110, 30), (l - 0.2) / 0.4)
        return c, lerp((120, 20, 0), (255, 90, 20), (l - 0.2) / 0.4)
    if s >= 0.35:
        return (r, g, b), None
    if kind == 'spike':
        return lerp((24, 20, 20), (92, 84, 80), l / 0.5), None
    if kind == 'membrane':
        vein = math.sin(x * 0.31 + math.sin(y * 0.09) * 4.0) > 0.965 or math.sin(y * 0.27 + x * 0.05) > 0.98
        if vein:
            return lerp((150, 40, 10), (240, 110, 30), l), None
        return lerp((34, 14, 14), (110, 52, 46), (l - 0.2) / 0.55), None
    if kind == 'belly':
        if (x + y) % 5 == 0 or y % 6 == 0:
            seam = lerp((120, 30, 8), (200, 70, 16), l)
            return seam, lerp((140, 40, 0), (230, 90, 10), l)
        return lerp((176, 72, 18), (250, 156, 60), (l - 0.15) / 0.6), None
    if kind == 'top':
        return lerp((12, 6, 6), (88, 16, 14), (l - 0.05) / 0.6), None
    # Scales: crimson, the darkest seams near black
    return lerp((60, 8, 8), (214, 40, 28), (l - 0.05) / 0.65), None


def paint_patches(out):
    def put(px, py, c):
        out[py][px] = c + (255,)

    # Horns: charcoal to bone at the tips
    u, v, n = PATCHES['horn']
    for i in range(n):
        for j in range(n):
            t = j / (n - 1)
            c = lerp((30, 26, 26), (120, 110, 96), t)
            if (i * 7 + j * 3) % 11 == 0:
                c = lerp(c, (10, 8, 8), 0.5)
            put(u + i, v + j, c)
    # Dorsal spines: dark crimson to black
    u, v, n = PATCHES['spine']
    for i in range(n):
        for j in range(n):
            put(u + i, v + j, lerp((110, 18, 14), (20, 8, 8), j / (n - 1)))
    # Frills: smoky membrane with an ember edge
    u, v, n = PATCHES['frill']
    for i in range(n):
        for j in range(n):
            edge = i == 0 or j == 0 or i == n - 1 or j == n - 1
            put(u + i, v + j, (200, 80, 24) if edge else lerp((50, 18, 16), (110, 40, 34), (i + j) / (2 * n)))
    # Tail blade: crimson with a dark rim
    u, v, n = PATCHES['blade']
    for i in range(n):
        for j in range(n):
            edge = min(i, j, n - 1 - i, n - 1 - j) < 2
            put(u + i, v + j, (30, 10, 10) if edge else lerp((150, 20, 16), (90, 10, 10), j / (n - 1)))


def main():
    membranes = bone_texels(lambda name: name.startswith('membrane'))
    spikes = bone_texels(lambda name: name.startswith('spike'))
    belly = bone_texels(lambda name: name in TRUNK, 'down')
    top = bone_texels(lambda name: name in TRUNK, 'up')
    w, h, rows = pngio.read_png(SOURCE)
    out, glow = [], []
    for y, row in enumerate(rows):
        orow, grow = [], []
        for x, (r, g, b, a) in enumerate(row):
            if a == 0:
                orow.append((0, 0, 0, 0))
                grow.append((0, 0, 0, 0))
                continue
            kind = ('membrane' if (x, y) in membranes else 'spike' if (x, y) in spikes
                    else 'belly' if (x, y) in belly else 'top' if (x, y) in top else 'scale')
            c, gl = recolour(x, y, r, g, b, kind)
            orow.append(c + (a,))
            grow.append(gl + (a,) if gl else (0, 0, 0, 0))
        out.append(orow)
        glow.append(grow)
    paint_patches(out)
    os.makedirs(OUT, exist_ok=True)
    pngio.write_png(os.path.join(OUT, 'red.png'), w, h, out)
    pngio.write_png(os.path.join(OUT, 'red_glowmask.png'), w, h, glow)
    print('wrote red.png and red_glowmask.png')


if __name__ == '__main__':
    main()
