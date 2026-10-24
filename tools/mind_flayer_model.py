"""Generates the Mind Flayer's GeckoLib model, texture, glowmask and animations.

    python3 tools/mind_flayer_model.py

Writes geo/entity/mind_flayer.geo.json, textures/entity/mind_flayer.png and mind_flayer_glowmask.png
(GeckoLib's AutoGlowingGeoLayer draws those pixels fullbright: the blank white eyes), and
animations/entity/mind_flayer.animation.json. A tall, thin humanoid in a dark high-collared robe with
an octopus head (bulbous mauve cranium, four face tentacles in two segments each) and long fingers.
Box UVs are packed automatically and every face is painted from a per-material palette.
"""
import json
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/dndclasses')
NAME = 'mind_flayer'
CLEAR = (0, 0, 0, 0)


def jitter(rng, colour, amount=8):
    d = rng.randint(-amount, amount)
    return tuple(max(0, min(255, c + d)) for c in colour) + (255,)


def mix(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


SKIN = (148, 104, 150)
SKIN_DARK = (104, 70, 112)
SKIN_LIGHT = (184, 146, 184)
ROBE = (46, 32, 58)
ROBE_DARK = (28, 20, 36)
TRIM = (132, 34, 58)
COLLAR_IN = (92, 22, 44)
EYE = (244, 244, 255)
MOUTH = (60, 24, 52)


# Each material takes (face, x, y, w, h, rng) and returns (rgba, glow).

def skin(face, x, y, w, h, rng):
    c = SKIN_DARK if face == 'bottom' else SKIN
    if rng.random() < 0.18:
        c = mix(c, SKIN_DARK, 0.5)  # mottled
    elif rng.random() < 0.06:
        c = SKIN_LIGHT
    return jitter(rng, c), False


def head(face, x, y, w, h, rng):
    """The face: two blank white eyes under a heavy brow, a dark beak-mouth among the tentacles."""
    if face != 'north':
        return skin(face, x, y, w, h, rng)
    if y == 2 and x in (1, 2, w - 3, w - 2):
        return jitter(rng, EYE, 4), True
    if y == 1 and x in (1, 2, w - 3, w - 2):
        return jitter(rng, SKIN_DARK), False  # brow
    if y >= h - 2 and 2 <= x <= w - 3:
        return jitter(rng, MOUTH, 6), False
    return skin(face, x, y, w, h, rng)


def cranium(face, x, y, w, h, rng):
    """The bulging brain-case: veined, a shade lighter on top."""
    c, _ = skin(face, x, y, w, h, rng)
    if face == 'top' or (face != 'bottom' and y == 0):
        c = jitter(rng, mix(SKIN, SKIN_LIGHT, 0.4))
    if face in ('east', 'west', 'south', 'top') and (x * 3 + y * 2) % 7 == 0:
        c = jitter(rng, SKIN_DARK, 4)  # veins
    return c, False


def tentacle(face, x, y, w, h, rng):
    c, _ = skin(face, x, y, w, h, rng)
    if face == 'south' and y % 2 == 1:
        c = jitter(rng, SKIN_LIGHT, 6)  # suckers on the inner side
    return c, False


def robe(face, x, y, w, h, rng):
    if face in ('top', 'bottom'):
        return jitter(rng, ROBE_DARK), False
    if face == 'north' and w >= 6 and x in (w // 2 - 1, w // 2):
        return jitter(rng, TRIM, 8), False
    return jitter(rng, ROBE_DARK if (x + y) % 5 == 0 else ROBE), False


def robe_hem(face, x, y, w, h, rng):
    if face in ('top', 'bottom'):
        return jitter(rng, ROBE_DARK), False
    if y == h - 1 and rng.random() < 0.4:
        return CLEAR, False
    if y == h - 2:
        return jitter(rng, TRIM, 8), False
    return jitter(rng, ROBE_DARK if (x + rng.randint(0, 1)) % 3 == 0 else ROBE), False


def collar(face, x, y, w, h, rng):
    """High stiff collar: dark outside, a deep red lining on the side facing the head."""
    if face == 'north':
        return jitter(rng, COLLAR_IN, 8), False
    if y == 0:
        return jitter(rng, TRIM, 8), False
    return jitter(rng, ROBE_DARK), False


def boots(face, x, y, w, h, rng):
    if y >= h - 3:
        return jitter(rng, (24, 18, 22), 4), False
    return jitter(rng, ROBE_DARK, 6), False


MATERIALS = {'skin': skin, 'head': head, 'cranium': cranium, 'tentacle': tentacle, 'robe': robe,
             'robe_hem': robe_hem, 'collar': collar, 'boots': boots}

# bone: (name, parent, pivot, [(origin, size, material), ...]). Front is -z, 16 px = 1 block.
TENTACLE_X = (-2.25, -0.75, 0.75, 2.25)


def tentacles():
    bones = []
    for i, cx in enumerate(TENTACLE_X, 1):
        top = 23.5 if i in (2, 3) else 24.5
        bones.append((f'tentacle_{i}', 'head', [cx, top, -4], [
            ([cx - 0.5, top - 5, -4.5], [1, 5, 1], 'tentacle'),
        ]))
        bones.append((f'tentacle_{i}_tip', f'tentacle_{i}', [cx, top - 5, -4], [
            ([cx - 0.5, top - 9, -4.5], [1, 4, 1], 'tentacle'),
        ]))
    return bones


BONES = [
    ('root', None, [0, 0, 0], []),
    ('leg_left', 'root', [2, 12, 0], [
        ([0.5, 0, -1.5], [3, 12, 3], 'boots'),
    ]),
    ('leg_right', 'root', [-2, 12, 0], [
        ([-3.5, 0, -1.5], [3, 12, 3], 'boots'),
    ]),
    ('body', 'root', [0, 12, 0], [
        ([-4.5, 5, -2.5], [9, 8, 5], 'robe_hem'),
        ([-4, 12, -2], [8, 11, 4], 'robe'),
        ([-5, 20, 2], [10, 10, 1], 'collar'),
        ([-5, 20, -1], [1, 8, 3], 'collar'),
        ([4, 20, -1], [1, 8, 3], 'collar'),
    ]),
    ('head', 'body', [0, 23, 0], [
        ([-3.5, 23, -3.5], [7, 7, 7], 'head'),
        ([-4, 26, -2], [8, 6, 8], 'cranium'),
    ]),
    *tentacles(),
    ('arm_left', 'body', [5, 22, 0], [
        ([4, 11, -1], [2, 12, 2], 'robe'),
        ([4, 9, -1], [2, 2, 2], 'skin'),
    ]),
    ('fingers_left', 'arm_left', [5, 9, 0], [
        ([4, 5, -1], [1, 4, 1], 'skin'),
        ([5, 5, 0], [1, 4, 1], 'skin'),
        ([5, 6, -1], [1, 3, 1], 'skin'),
    ]),
    ('arm_right', 'body', [-5, 22, 0], [
        ([-6, 11, -1], [2, 12, 2], 'robe'),
        ([-6, 9, -1], [2, 2, 2], 'skin'),
    ]),
    ('fingers_right', 'arm_right', [-5, 9, 0], [
        ([-5, 5, -1], [1, 4, 1], 'skin'),
        ([-6, 5, 0], [1, 4, 1], 'skin'),
        ([-6, 6, -1], [1, 3, 1], 'skin'),
    ]),
]


def pack(bones, width):
    """Shelf-packs every cube's box UV footprint ((2d + 2w) x (d + h)); returns uv list and height."""
    cubes = [c for b in bones for c in b[3]]
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


# ---------------------------------------------------------------- animations

def sway(amp, speed=90, phase=0):
    return f'math.sin(query.anim_time*{speed}+{phase})*{amp}'


def tentacle_sway(amp, speed, extra_x=0):
    bones = {}
    for i in range(1, 5):
        phase = i * 70
        bones[f'tentacle_{i}'] = {'rotation': [f'{extra_x}+math.sin(query.anim_time*{speed}+{phase})*{amp}',
                                               0, f'math.sin(query.anim_time*{speed / 2}+{phase})*{amp / 2}']}
        bones[f'tentacle_{i}_tip'] = {'rotation': [f'math.sin(query.anim_time*{speed}+{phase + 60})*{amp * 1.5}',
                                                   0, 0]}
    return bones


def splay(x, z):
    """Tentacles thrown outward: the outer pair further to the sides."""
    zs = (z, z / 3, -z / 3, -z)
    bones = {}
    for i in range(1, 5):
        bones[f'tentacle_{i}'] = {'rotation': [x, 0, zs[i - 1]]}
        bones[f'tentacle_{i}_tip'] = {'rotation': [x / 2, 0, 0]}
    return bones


def animations():
    anims = {}
    anims['idle'] = {'loop': True, 'animation_length': 4, 'bones': {
        'head': {'rotation': [sway(2, 90), 0, 0]},
        'arm_left': {'rotation': [0, 0, sway(2, 90, 90)]},
        'arm_right': {'rotation': [0, 0, sway(-2, 90, 90)]},
        **tentacle_sway(8, 90),
    }}
    anims['walk'] = {'loop': True, 'animation_length': 1.2, 'bones': {
        'leg_left': {'rotation': [sway(22, 300), 0, 0]},
        'leg_right': {'rotation': [sway(-22, 300), 0, 0]},
        'arm_left': {'rotation': [sway(-12, 300), 0, 0]},
        'arm_right': {'rotation': [sway(12, 300), 0, 0]},
        **tentacle_sway(10, 300),
    }}
    blast = {
        'head': {'rotation': {'0.0': [0, 0, 0], '0.6': [18, 0, 0], '1.0': [22, 0, 0]}},
        'arm_left': {'rotation': {'0.0': [0, 0, 0], '0.6': [-70, 0, -25], '1.0': [-80, 0, -30]}},
        'arm_right': {'rotation': {'0.0': [0, 0, 0], '0.6': [-70, 0, 25], '1.0': [-80, 0, 30]}},
        'fingers_left': {'rotation': [-30, 0, 0]},
        'fingers_right': {'rotation': [-30, 0, 0]},
    }
    for bone, spec in splay(-70, 35).items():
        blast[bone] = {'rotation': {'0.0': [0, 0, 0], '0.8': spec['rotation']}}
    anims['mind_blast'] = {'loop': 'hold_on_last_frame', 'animation_length': 1.0, 'bones': blast}
    grab = {
        'head': {'rotation': [f'15+{sway(4, 720)}', 0, 0]},
        'arm_left': {'rotation': [f'-65+{sway(6, 720)}', -20, 0]},
        'arm_right': {'rotation': [f'-65+{sway(-6, 720)}', 20, 0]},
        'fingers_left': {'rotation': [-40, 0, 0]},
        'fingers_right': {'rotation': [-40, 0, 0]},
        **tentacle_sway(12, 720, extra_x=-75),
    }
    anims['grab'] = {'loop': True, 'animation_length': 0.5, 'bones': grab}
    extract = {
        'head': {'rotation': {'0.0': [15, 0, 0], '0.3': [35, 0, 0], '0.7': [35, 0, 0], '1.0': [0, 0, 0]}},
        'arm_left': {'rotation': {'0.0': [-65, -20, 0], '0.3': [-90, -10, 0], '1.0': [0, 0, 0]}},
        'arm_right': {'rotation': {'0.0': [-65, 20, 0], '0.3': [-90, 10, 0], '1.0': [0, 0, 0]}},
    }
    for i in range(1, 5):
        extract[f'tentacle_{i}'] = {'rotation': {'0.0': [-75, 0, 0], '0.3': [-100, 0, 0], '0.7': [-20, 0, 0],
                                                 '1.0': [0, 0, 0]}}
        extract[f'tentacle_{i}_tip'] = {'rotation': {'0.0': [0, 0, 0], '0.3': [-40, 0, 0], '1.0': [0, 0, 0]}}
    anims['extract'] = {'animation_length': 1.0, 'bones': extract}
    anims['blink'] = {'animation_length': 0.5, 'bones': {
        'root': {'rotation': {'0.0': [0, 0, 0], '0.5': [0, 360, 0]},
                 'scale': {'0.0': [1, 1, 1], '0.15': [0.6, 1.2, 0.6], '0.5': [1, 1, 1]}},
        **splay(-40, 50),
    }}
    anims['levitate'] = {'loop': True, 'animation_length': 2, 'bones': {
        'root': {'position': [0, sway(1, 180), 0]},
        'leg_left': {'rotation': [12, 0, -4]},
        'leg_right': {'rotation': [8, 0, 4]},
        'arm_left': {'rotation': [-10, 0, f'-25+{sway(4, 180)}']},
        'arm_right': {'rotation': [-10, 0, f'25-{sway(4, 180)}']},
        **tentacle_sway(14, 180),
    }}
    lash = {
        'arm_right': {'rotation': {'0.0': [0, 0, 0], '0.15': [-110, 20, 0], '0.4': [0, 0, 0]}},
        'head': {'rotation': {'0.0': [0, 0, 0], '0.15': [20, 0, 0], '0.4': [0, 0, 0]}},
    }
    for i in range(1, 5):
        lash[f'tentacle_{i}'] = {'rotation': {'0.0': [0, 0, 0], '0.12': [-85, 0, 0], '0.4': [0, 0, 0]}}
        lash[f'tentacle_{i}_tip'] = {'rotation': {'0.0': [0, 0, 0], '0.12': [-30, 0, 0], '0.4': [0, 0, 0]}}
    anims['lash'] = {'animation_length': 0.4, 'bones': lash}
    return {'format_version': '1.8.0', 'animations': anims}


def build(tex_width=64):
    rng = random.Random(NAME)
    uvs, tex_height = pack(BONES, tex_width)
    pixels = [[CLEAR] * tex_width for _ in range(tex_height)]
    glow = [[CLEAR] * tex_width for _ in range(tex_height)]
    geo_bones = []
    k = 0
    for bname, parent, pivot, cubes in BONES:
        bone = {'name': bname, 'pivot': pivot}
        if parent:
            bone['parent'] = parent
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
                'identifier': 'geometry.' + NAME,
                'texture_width': tex_width,
                'texture_height': tex_height,
                'visible_bounds_width': 2,
                'visible_bounds_height': 3,
                'visible_bounds_offset': [0, 1.5, 0],
            },
            'bones': geo_bones,
        }],
    }
    with open(os.path.join(ASSETS, 'geo/entity', NAME + '.geo.json'), 'w') as f:
        json.dump(geo, f, indent='\t')
        f.write('\n')
    tex_dir = os.path.join(ASSETS, 'textures/entity')
    pngio.write_png(os.path.join(tex_dir, NAME + '.png'), tex_width, tex_height, pixels)
    pngio.write_png(os.path.join(tex_dir, NAME + '_glowmask.png'), tex_width, tex_height, glow)
    with open(os.path.join(ASSETS, 'animations/entity', NAME + '.animation.json'), 'w') as f:
        json.dump(animations(), f, indent='\t')
        f.write('\n')
    print(f'{NAME}: {tex_width}x{tex_height}')


if __name__ == '__main__':
    build()
