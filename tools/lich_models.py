"""Generates the Lich and Phylactery GeckoLib models and their textures.

    python3 tools/lich_models.py

Writes geo/entity/{lich,phylactery}.geo.json and textures/entity/{lich,phylactery}.png plus
*_glowmask.png (GeckoLib's AutoGlowingGeoLayer draws those pixels fullbright: the Lich's eyes,
crown and staff gems, and the Phylactery's soul gem). Box UVs are packed automatically and every
face is painted from a small per-material palette, so the model and texture always line up.
"""
import json
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/dndclasses')

CLEAR = (0, 0, 0, 0)


def jitter(rng, colour, amount=10):
    d = rng.randint(-amount, amount)
    return tuple(max(0, min(255, c + d)) for c in colour) + (255,)


# ---------------------------------------------------------------- materials
# Each takes (face, x, y, w, h, rng) and returns (rgba, glow) where glow is True for fullbright.

def solid(colour, amount=10):
    return lambda face, x, y, w, h, rng: (jitter(rng, colour, amount), False)


def glowing(colour, edge):
    def paint(face, x, y, w, h, rng):
        border = x in (0, w - 1) or y in (0, h - 1)
        return (jitter(rng, edge if border and w > 2 and h > 2 else colour, 6), True)
    return paint


ROBE = (58, 30, 78)
ROBE_DARK = (36, 18, 50)
TRIM = (196, 156, 54)
BONE = (214, 204, 178)
BONE_DARK = (150, 140, 118)
SOCKET = (20, 14, 24)
EYE = (120, 230, 255)
ICE = (110, 215, 255)
ICE_EDGE = (60, 150, 220)
SOUL = (80, 240, 220)
SOUL_EDGE = (30, 160, 150)
STONE = (78, 76, 88)
STONE_DARK = (52, 50, 60)
WOOD = (58, 40, 32)
METAL = (48, 42, 58)


def robe_hem(face, x, y, w, h, rng):
    """Lower robe: dark purple, gold band above a ragged, partly see-through hem."""
    if face in ('top', 'bottom'):
        return jitter(rng, ROBE_DARK), False
    if y == h - 1 and rng.random() < 0.45:
        return CLEAR, False
    if y == h - 3:
        return jitter(rng, TRIM, 8), False
    shade = ROBE_DARK if (x + rng.randint(0, 1)) % 4 == 0 else ROBE
    return jitter(rng, shade), False


def robe(face, x, y, w, h, rng):
    if face in ('top', 'bottom'):
        return jitter(rng, ROBE_DARK), False
    if face == 'north' and w >= 6 and x in (w // 2 - 1, w // 2):
        return jitter(rng, TRIM, 8), False
    return jitter(rng, ROBE_DARK if (x + y) % 5 == 0 else ROBE), False


def torso(face, x, y, w, h, rng):
    """Robe torso: open at the front showing ribs, with a glowing soul amulet."""
    if face == 'north':
        cx = w // 2
        if y == 2 and x in (cx - 1, cx):
            return jitter(rng, ICE, 4), True
        if y in (1, 3) and x in (cx - 1, cx) or y == 2 and x in (cx - 2, cx + 1):
            return jitter(rng, TRIM, 6), False
        if 2 <= x <= w - 3 and 4 <= y <= h - 3:
            if x == cx or x == cx - 1:
                return jitter(rng, BONE_DARK), False  # spine/sternum
            return (jitter(rng, BONE) if y % 2 == 0 else jitter(rng, SOCKET, 4)), False
        if x in (1, w - 2):
            return jitter(rng, TRIM, 8), False
    return robe(face, x, y, w, h, rng)


def skull(face, x, y, w, h, rng):
    if face == 'north' and w == 8 and h == 8:
        if y in (2, 3) and x in (1, 2, 5, 6):
            if y == 3 and x in (2, 5):
                return jitter(rng, EYE, 4), True
            return jitter(rng, SOCKET, 4), False
        if y == 4 and x in (3, 4):
            return jitter(rng, SOCKET, 4), False
        if y == 6:
            return (jitter(rng, SOCKET, 4) if x % 2 == 1 else jitter(rng, (235, 228, 205), 6)), False
        if y == 7:
            return jitter(rng, BONE_DARK), False
    if face == 'bottom':
        return jitter(rng, BONE_DARK), False
    if face in ('east', 'west') and y >= h - 3 and x in (w // 2 - 1,):
        return jitter(rng, SOCKET, 4), False
    return jitter(rng, BONE if rng.random() > 0.08 else BONE_DARK), False


def stone_carved(face, x, y, w, h, rng):
    if face not in ('top', 'bottom') and (y in (0, h - 1) or x in (0, w - 1)):
        return jitter(rng, STONE_DARK), False
    if face not in ('top', 'bottom') and (x + y) % 3 == 0 and 0 < y < h - 1:
        return jitter(rng, (96, 70, 120), 6), False  # violet runes
    return jitter(rng, STONE), False


def vessel(face, x, y, w, h, rng):
    """Dark metal box with gold corners and glowing soul light through slits in the sides."""
    if x in (0, w - 1) and face not in ('top', 'bottom') or y in (0, h - 1) and face not in ('top', 'bottom'):
        return jitter(rng, TRIM, 8), False
    if face not in ('top', 'bottom') and x == w // 2 and 1 <= y <= h - 2:
        return jitter(rng, SOUL, 6), True
    return jitter(rng, METAL, 6), False


MATERIALS = {
    'robe': robe, 'robe_hem': robe_hem, 'torso': torso, 'skull': skull,
    'bone': solid(BONE), 'gold': solid(TRIM, 12), 'ice': glowing(ICE, ICE_EDGE),
    'soul': glowing(SOUL, SOUL_EDGE), 'wood': solid(WOOD, 6), 'stone': solid(STONE, 8),
    'stone_carved': stone_carved, 'vessel': vessel,
}


# ---------------------------------------------------------------- models
# bone: (name, parent, pivot, rotation, [(origin, size, material), ...])

LICH = [
    ('root', None, [0, 0, 0], None, []),
    ('robe', 'root', [0, 18, 0], None, [
        ([-5, 0, -3.5], [10, 10, 7], 'robe_hem'),
        ([-4.5, 10, -3], [9, 8, 6], 'robe'),
    ]),
    ('body', 'root', [0, 18, 0], None, [
        ([-4.5, 18, -2.5], [9, 11, 5], 'torso'),
        ([-5, 27, -3], [10, 3, 6], 'gold'),
    ]),
    ('head', 'body', [0, 29, 0], None, [
        ([-4, 29, -4], [8, 8, 8], 'skull'),
        ([-4.5, 36, -4.5], [9, 2, 9], 'gold'),
        ([-4.5, 38, -4.5], [1, 2, 1], 'gold'),
        ([3.5, 38, -4.5], [1, 2, 1], 'gold'),
        ([-4.5, 38, 3.5], [1, 2, 1], 'gold'),
        ([3.5, 38, 3.5], [1, 2, 1], 'gold'),
        ([-0.5, 38, -4.5], [1, 3, 1], 'gold'),
        ([-1, 36, -5], [2, 2, 1], 'ice'),
    ]),
    ('left_arm', 'body', [5.5, 28, 0], None, [
        ([4.5, 18, -1.5], [3, 11, 3], 'robe'),
        ([5, 16, -1], [2, 2, 2], 'bone'),
    ]),
    ('right_arm', 'body', [-5.5, 28, 0], None, [
        ([-7.5, 18, -1.5], [3, 11, 3], 'robe'),
        ([-7, 16, -3], [2, 2, 3], 'bone'),
    ]),
    ('staff', 'right_arm', [-6, 17, -2], None, [
        ([-6.5, 2, -2.5], [1, 32, 1], 'wood'),
        ([-7.5, 34, -3.5], [3, 1, 3], 'gold'),
        ([-7, 35, -3], [2, 2, 2], 'ice'),
        ([-7.5, 35, -3.5], [1, 3, 1], 'gold'),
        ([-5.5, 35, -1.5], [1, 3, 1], 'gold'),
    ]),
]

PHYLACTERY = [
    ('root', None, [0, 0, 0], None, []),
    ('base', 'root', [0, 0, 0], None, [
        ([-5, 0, -5], [10, 2, 10], 'stone'),
        ([-3, 2, -3], [6, 6, 6], 'stone_carved'),
        ([-4, 8, -4], [8, 1, 8], 'stone'),
    ]),
    ('vessel', 'root', [0, 13, 0], None, [
        ([-2.5, 10, -2.5], [5, 6, 5], 'vessel'),
        ([-3, 16, -3], [6, 1, 6], 'gold'),
        ([-1, 17, -1], [2, 2, 2], 'soul'),
    ]),
]


def pack(bones, width):
    """Shelf-packs every cube's box UV footprint ((2d + 2w) x (d + h)); returns uv list and height."""
    cubes = [c for b in bones for c in b[4]]
    order = sorted(range(len(cubes)), key=lambda i: -(cubes[i][1][2] + cubes[i][1][1]))
    uvs = [None] * len(cubes)
    x = y = shelf = 0
    for i in order:
        w, h, d = cubes[i][1]
        fw, fh = 2 * d + 2 * w, d + h
        if x + fw > width:
            x, y, shelf = 0, y + shelf, 0
        uvs[i] = (x, y)
        x += fw
        shelf = max(shelf, fh)
    height = y + shelf
    size = 16
    while size < height:
        size *= 2
    return uvs, size


def faces(u, v, w, h, d):
    return {
        'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d),
        'east': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
        'west': (u + d + w, v + d, d, h), 'south': (u + 2 * d + w, v + d, w, h),
    }


def build(name, bones, tex_width, bounds):
    rng = random.Random(name)
    uvs, tex_height = pack(bones, tex_width)
    pixels = [[CLEAR] * tex_width for _ in range(tex_height)]
    glow = [[CLEAR] * tex_width for _ in range(tex_height)]
    geo_bones = []
    k = 0
    for bname, parent, pivot, rotation, cubes in bones:
        bone = {'name': bname, 'pivot': pivot}
        if parent:
            bone['parent'] = parent
        if rotation:
            bone['rotation'] = rotation
        if cubes:
            bone['cubes'] = []
        for origin, size, material in cubes:
            u, v = uvs[k]
            k += 1
            bone['cubes'].append({'origin': origin, 'size': size, 'uv': [u, v]})
            paint = MATERIALS[material]
            for face, (fx, fy, fw, fh) in faces(u, v, *size).items():
                for py in range(fh):
                    for px in range(fw):
                        colour, lit = paint(face, px, py, fw, fh, rng)
                        pixels[fy + py][fx + px] = colour
                        if lit:
                            glow[fy + py][fx + px] = colour
        geo_bones.append(bone)
    geo = {
        'format_version': '1.12.0',
        'minecraft:geometry': [{
            'description': {
                'identifier': 'geometry.' + name,
                'texture_width': tex_width,
                'texture_height': tex_height,
                'visible_bounds_width': bounds[0],
                'visible_bounds_height': bounds[1],
                'visible_bounds_offset': [0, bounds[1] / 2, 0],
            },
            'bones': geo_bones,
        }],
    }
    with open(os.path.join(ASSETS, 'geo/entity', name + '.geo.json'), 'w') as f:
        json.dump(geo, f, indent='\t')
        f.write('\n')
    tex_dir = os.path.join(ASSETS, 'textures/entity')
    pngio.write_png(os.path.join(tex_dir, name + '.png'), tex_width, tex_height, pixels)
    pngio.write_png(os.path.join(tex_dir, name + '_glowmask.png'), tex_width, tex_height, glow)
    print(f'{name}: {tex_width}x{tex_height}')


if __name__ == '__main__':
    build('lich', LICH, 128, (3, 3.5))
    build('phylactery', PHYLACTERY, 64, (2, 2))
