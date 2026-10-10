"""Generates the Frost Drake textures by recolouring the Lightning Chaser's blue.png.

    python3 tools/frost_drake_texture.py

The Frost Drake reuses the Lightning Chaser's model, so the two textures share a UV layout. The
chaser's dark slate scales become pale blue-white, the wing membranes (found from the model's
membrane bones, since they're the same greys as the body) deep blue, the spikes icy cyan, the
mouth a cold navy and the eyes glowing cyan. Also writes frost_drake_glowmask.png (GeckoLib's
AutoGlowingGeoLayer draws it fullbright), holding the eyes.
"""
import colorsys
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/dndclasses')
SOURCE = os.path.join(ASSETS, 'textures/entity/lightning_chaser/blue.png')
GEO = os.path.join(ASSETS, 'geo/lightning_chaser.geo.json')
OUT = os.path.join(ASSETS, 'textures/entity/frost_drake')


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def cube_texels(cube):
    """Every texel a cube's faces read, for box UV and per-face UV."""
    uv = cube.get('uv')
    if uv is None:
        return
    if isinstance(uv, dict):
        for face in uv.values():
            u, v = face['uv']
            du, dv = face.get('uv_size', [0, 0])
            for x in range(min(u, u + du), max(u, u + du)):
                for y in range(min(v, v + dv), max(v, v + dv)):
                    yield x, y
        return
    u, v = uv
    sx, sy, sz = (int(round(s)) for s in cube['size'])
    for x in range(u, u + 2 * (sx + sz)):
        for y in range(v, v + sz + sy):
            yield x, y


def bone_texels(predicate):
    with open(GEO) as f:
        bones = json.load(f)['minecraft:geometry'][0]['bones']
    texels = set()
    for bone in bones:
        if predicate(bone['name']):
            for cube in bone.get('cubes', []):
                texels.update(cube_texels(cube))
    return texels


def recolour(r, g, b, membrane, spike):
    """Returns (colour, glow) where glow is the glowmask colour or None."""
    h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
    hue = h * 360
    if 120 <= hue < 200 and s >= 0.6:
        # Eyes: glowing cyan
        c = lerp((70, 220, 255), (190, 250, 255), (l - 0.3) / 0.4)
        return c, c
    if (hue < 15 or hue > 340) and s >= 0.3:
        # Mouth: cold navy
        return lerp((20, 24, 70), (60, 70, 140), (l - 0.2) / 0.4), None
    if s >= 0.35:
        # Odd saturated marker pixels: leave alone
        return (r, g, b), None
    if spike:
        # Spikes and horns: icicles
        return lerp((110, 185, 225), (225, 250, 255), l / 0.4), None
    if membrane:
        # Wing membranes: deep blue
        return lerp((14, 30, 92), (78, 128, 210), (l - 0.2) / 0.55), None
    # Scales: pale blue-white, the darkest seams a frosty blue
    return lerp((92, 132, 176), (238, 246, 255), (l - 0.05) / 0.65), None


def main():
    membranes = bone_texels(lambda name: name.startswith('membrane'))
    spikes = bone_texels(lambda name: name.startswith('spike'))
    w, h, rows = pngio.read_png(SOURCE)
    out, glow = [], []
    for y, row in enumerate(rows):
        orow, grow = [], []
        for x, (r, g, b, a) in enumerate(row):
            if a == 0:
                orow.append((0, 0, 0, 0))
                grow.append((0, 0, 0, 0))
                continue
            c, gl = recolour(r, g, b, (x, y) in membranes, (x, y) in spikes)
            orow.append(c + (a,))
            grow.append(gl + (a,) if gl else (0, 0, 0, 0))
        out.append(orow)
        glow.append(grow)
    os.makedirs(OUT, exist_ok=True)
    pngio.write_png(os.path.join(OUT, 'frost_drake.png'), w, h, out)
    pngio.write_png(os.path.join(OUT, 'frost_drake_glowmask.png'), w, h, glow)
    print('wrote frost_drake.png and frost_drake_glowmask.png')


if __name__ == '__main__':
    main()
