"""Generates the Medusa's GeckoLib model, animations and textures.

    python3 tools/medusa_model.py

Writes geo/entity/medusa.geo.json, animations/entity/medusa.animation.json and textures/entity/medusa.png plus
medusa_glowmask.png (GeckoLib's AutoGlowingGeoLayer draws those pixels fullbright: her eyes and her snakes' eyes).
A snake-haired woman in a torn chiton with eight hair snakes (two bones each) and a longbow in her left hand. Box
UVs are packed automatically and every face is painted from a small per-material palette (as tools/lich_models.py),
so the model and texture always line up. Animations: idle (the snakes writhe), walk, gaze, bow_draw, bow_release,
bite, reel.
"""
import json
import math
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


SKIN = (148, 168, 128)
SKIN_DARK = (112, 132, 96)
CHITON = (226, 216, 190)
CHITON_DARK = (184, 172, 146)
GOLD = (204, 162, 58)
EYE = (236, 230, 70)
PUPIL = (40, 30, 10)
LIPS = (70, 90, 60)
SNAKE = (62, 128, 58)
SNAKE_DARK = (34, 82, 36)
SNAKE_BELLY = (196, 190, 92)
SNAKE_EYE = (250, 210, 40)
WOOD = (98, 64, 36)
STRING = (220, 214, 196)


def solid(colour, amount=8):
    return lambda face, x, y, w, h, rng: (jitter(rng, colour, amount), False)


def skin(face, x, y, w, h, rng):
    return jitter(rng, SKIN_DARK if rng.random() < 0.12 else SKIN, 6), False


def arm(face, x, y, w, h, rng):
    """Bare arm with a gold armlet near the shoulder and one at the wrist."""
    if face not in ('top', 'bottom') and y in (2, h - 2):
        return jitter(rng, GOLD, 8), False
    return skin(face, x, y, w, h, rng)


def face_mat(face, x, y, w, h, rng):
    """Her face: glowing yellow eyes with slit pupils, dark lips; grey-green skin elsewhere."""
    if face == 'north':
        if y == 3 and x in (1, 2, 4, 5):
            if x in (2, 4):
                return jitter(rng, PUPIL, 4), True
            return jitter(rng, EYE, 6), True
        if y == 2 and x in (1, 2, 4, 5):
            return jitter(rng, SKIN_DARK, 4), False  # brows
        if y == 5 and x in (2, 3, 4):
            return jitter(rng, LIPS, 4), False
    if face == 'top':
        return jitter(rng, SNAKE_DARK, 8), False  # snake roots
    if face == 'south' and y < 4:
        return jitter(rng, SNAKE_DARK, 8), False
    return skin(face, x, y, w, h, rng)


def torso(face, x, y, w, h, rng):
    """Chiton over one shoulder: the other shoulder bare, a gold brooch, folds."""
    if face in ('north', 'south'):
        diagonal = x + (h - 1 - y) < w // 2 + 1
        if y < 3 and diagonal:
            return skin(face, x, y, w, h, rng)
        if face == 'north' and (x, y) in ((w - 3, 1), (w - 3, 2)):
            return jitter(rng, GOLD, 6), False
        if y == h - 1:
            return jitter(rng, GOLD, 8), False
    if face in ('east', 'west') and y < 2 and face == 'west':
        return skin(face, x, y, w, h, rng)
    return jitter(rng, CHITON_DARK if x % 3 == 0 else CHITON, 6), False


def skirt(face, x, y, w, h, rng):
    """Lower chiton: long folds, a gold band, and a torn, ragged hem."""
    if face in ('top', 'bottom'):
        return jitter(rng, CHITON_DARK, 6), False
    if y == h - 1 and rng.random() < 0.4:
        return CLEAR, False
    if y == h - 3:
        return jitter(rng, GOLD, 8), False
    return jitter(rng, CHITON_DARK if (x + rng.randint(0, 1)) % 3 == 0 else CHITON, 6), False


def snake(face, x, y, w, h, rng):
    if face == 'north':
        return jitter(rng, SNAKE_BELLY, 8), False
    if (y + x) % 3 == 0:
        return jitter(rng, SNAKE_DARK, 6), False
    return jitter(rng, SNAKE, 8), False


def snake_head(face, x, y, w, h, rng):
    """Snake head; its eyes (on the side faces) glow."""
    if face in ('east', 'west') and y == 0 and x == 0:
        return jitter(rng, SNAKE_EYE, 4), True
    if face == 'north' and y == h - 1:
        return jitter(rng, (150, 40, 40), 6), False  # mouth
    return snake(face, x, y, w, h, rng)


MATERIALS = {
    'skin': skin, 'arm': arm, 'face': face_mat, 'torso': torso, 'skirt': skirt, 'gold': solid(GOLD, 10),
    'snake': snake, 'snake_head': snake_head, 'wood': solid(WOOD, 8), 'string': solid(STRING, 4),
}


# ---------------------------------------------------------------- model
# bone: (name, parent, pivot, rotation, [(origin, size, material), ...])

# Snakes: (base x, base y, base z, rotation [x, y, z]) round the head (head spans x/z -3.5..3.5, y 23..30).
SNAKES = [
    (-2.5, 29.5, -2.0, [-25, 0, 30]),
    (2.5, 29.5, -2.0, [-25, 0, -30]),
    (-3.0, 28.0, 0.5, [0, 0, 60]),
    (3.0, 28.0, 0.5, [0, 0, -60]),
    (-2.0, 28.5, 3.0, [45, 0, 25]),
    (2.0, 28.5, 3.0, [45, 0, -25]),
    (-0.8, 30.0, 0.5, [15, 0, 10]),
    (0.8, 30.0, 1.5, [30, 0, -10]),
]


def snake_bones():
    bones = []
    for i, (x, y, z, rot) in enumerate(SNAKES, 1):
        bones.append((f'snake{i}', 'head', [x, y, z], rot, [
            ([x - 0.75, y, z - 0.75], [1.5, 4, 1.5], 'snake'),
        ]))
        bones.append((f'snake{i}_tip', f'snake{i}', [x, y + 4, z], None, [
            ([x - 0.6, y + 4, z - 0.6], [1.2, 3, 1.2], 'snake'),
            ([x - 1, y + 7, z - 1.5], [2, 1.5, 2.5], 'snake_head'),
        ]))
    return bones


MEDUSA = [
    ('root', None, [0, 0, 0], None, []),
    ('skirt', 'root', [0, 13, 0], None, [
        ([-5, 0, -3.5], [10, 5, 7], 'skirt'),
        ([-4.5, 5, -3], [9, 8, 6], 'skirt'),
    ]),
    ('body', 'root', [0, 13, 0], None, [
        ([-4, 13, -2.5], [8, 10, 5], 'torso'),
        ([-4.5, 13, -3], [9, 1, 6], 'gold'),
    ]),
    ('head', 'body', [0, 23, 0], None, [
        ([-3.5, 23, -3.5], [7, 7, 7], 'face'),
        ([-3.5, 29.5, -0.5], [7, 1, 4], 'snake'),
    ]),
    ('left_arm', 'body', [5, 22, 0], None, [
        ([4, 12, -1.5], [3, 11, 3], 'arm'),
    ]),
    ('bow', 'left_arm', [5.5, 13, -2], None, [
        ([5, 12, -3.5], [1, 3, 1], 'wood'),
        ([5, 15, -3], [1, 6, 1], 'wood'),
        ([5, 21, -2.5], [1, 1, 1], 'wood'),
        ([5, 6, -3], [1, 6, 1], 'wood'),
        ([5, 5, -2.5], [1, 1, 1], 'wood'),
        ([5.25, 6, -1.75], [0.5, 15, 0.5], 'string'),
    ]),
    ('right_arm', 'body', [-5, 22, 0], None, [
        ([-7, 12, -1.5], [3, 11, 3], 'arm'),
    ]),
] + snake_bones()


def pack(bones, width):
    """Shelf-packs every cube's box UV footprint ((2d + 2w) x (d + h)); returns uv list and height."""
    cubes = [c for b in bones for c in b[4]]
    order = sorted(range(len(cubes)), key=lambda i: -(math.ceil(cubes[i][1][2]) + math.ceil(cubes[i][1][1])))
    uvs = [None] * len(cubes)
    x = y = shelf = 0
    for i in order:
        w, h, d = (math.ceil(v) for v in cubes[i][1])
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
            whole = [math.ceil(s) for s in size]
            for face, (fx, fy, fw, fh) in faces(u, v, *whole).items():
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


# ---------------------------------------------------------------- animations

def r(v):
    return round(v, 2)


def frames(times_values):
    """{time: [x, y, z]} with times as strings, as Blockbench writes them."""
    return {str(r(t)): [r(c) for c in v] for t, v in times_values}


def wave(length, steps, fn):
    """Keyframes sampling fn(phase in 0..1) -> [x, y, z] over a loop."""
    return frames([(length * i / steps, fn(i / steps)) for i in range(steps + 1)])


def snake_writhe(length, amp_a, amp_b, extra=(0, 0, 0)):
    bones = {}
    for i in range(1, len(SNAKES) + 1):
        phase = i * 0.37
        bones[f'snake{i}'] = {'rotation': wave(length, 8, lambda p, ph=phase: [
            extra[0] + amp_a * math.sin(2 * math.pi * (p + ph)), extra[1],
            extra[2] + amp_a * 0.6 * math.cos(2 * math.pi * (p + ph))])}
        bones[f'snake{i}_tip'] = {'rotation': wave(length, 8, lambda p, ph=phase: [
            amp_b * math.sin(2 * math.pi * (p + ph + 0.25)), 0,
            amp_b * 0.6 * math.cos(2 * math.pi * (p + ph + 0.25))])}
    return bones


def animations():
    anims = {}

    idle = snake_writhe(2.0, 10, 18)
    idle['left_arm'] = {'rotation': wave(2.0, 4, lambda p: [4 + 3 * math.sin(2 * math.pi * p), 0, -4])}
    idle['right_arm'] = {'rotation': wave(2.0, 4, lambda p: [2 - 3 * math.sin(2 * math.pi * p), 0, 4])}
    idle['skirt'] = {'rotation': wave(2.0, 4, lambda p: [0, 0, 1.5 * math.sin(2 * math.pi * p)])}
    anims['idle'] = {'loop': True, 'animation_length': 2.0, 'bones': idle}

    walk = snake_writhe(1.0, 14, 20)
    walk['left_arm'] = {'rotation': wave(1.0, 4, lambda p: [4 - 18 * math.sin(2 * math.pi * p), 0, -4])}
    walk['right_arm'] = {'rotation': wave(1.0, 4, lambda p: [18 * math.sin(2 * math.pi * p), 0, 4])}
    walk['skirt'] = {'rotation': wave(1.0, 4, lambda p: [6 * math.sin(2 * math.pi * p), 0,
                                                         3 * math.cos(2 * math.pi * p)])}
    walk['root'] = {'position': wave(1.0, 4, lambda p: [0, 0.6 * abs(math.sin(2 * math.pi * p)), 0])}
    anims['walk'] = {'loop': True, 'animation_length': 1.0, 'bones': walk}

    # Gaze: her head lifts, every snake rears outward, arms spread.
    gaze = {}
    for i in range(1, len(SNAKES) + 1):
        gaze[f'snake{i}'] = {'rotation': frames([(0, [0, 0, 0]), (0.2, [-25, 0, 0]), (0.6, [-25, 0, 0]),
                                                 (0.8, [0, 0, 0])])}
        gaze[f'snake{i}_tip'] = {'rotation': frames([(0, [0, 0, 0]), (0.2, [-30, 0, 0]), (0.6, [-30, 0, 0]),
                                                     (0.8, [0, 0, 0])])}
    gaze['head'] = {'rotation': frames([(0, [0, 0, 0]), (0.2, [-12, 0, 0]), (0.6, [-12, 0, 0]),
                                        (0.8, [0, 0, 0])])}
    gaze['left_arm'] = {'rotation': frames([(0, [0, 0, 0]), (0.2, [10, 0, -25]), (0.6, [10, 0, -25]),
                                            (0.8, [0, 0, 0])])}
    gaze['right_arm'] = {'rotation': frames([(0, [0, 0, 0]), (0.2, [10, 0, 25]), (0.6, [10, 0, 25]),
                                             (0.8, [0, 0, 0])])}
    anims['gaze'] = {'loop': False, 'animation_length': 0.8, 'bones': gaze}

    # Bow: the left arm raises the bow forward, the right hand draws the string back to her cheek.
    drawn_left = [85, -10, 0]
    drawn_right = [90, 35, 0]
    anims['bow_draw'] = {'loop': 'hold_on_last_frame', 'animation_length': 0.75, 'bones': {
        'left_arm': {'rotation': frames([(0, [0, 0, 0]), (0.3, drawn_left), (0.75, drawn_left)])},
        'right_arm': {'rotation': frames([(0, [0, 0, 0]), (0.3, [80, 10, 0]), (0.75, drawn_right)])},
        'bow': {'rotation': frames([(0, [0, 0, 0]), (0.3, [0, 0, -80]), (0.75, [0, 0, -80])])},
        'head': {'rotation': frames([(0, [0, 0, 0]), (0.75, [0, -8, 0])])},
    }}
    anims['bow_release'] = {'loop': False, 'animation_length': 0.5, 'bones': {
        'left_arm': {'rotation': frames([(0, drawn_left), (0.15, [80, -10, 0]), (0.5, [0, 0, 0])])},
        'right_arm': {'rotation': frames([(0, drawn_right), (0.08, [70, 70, 0]), (0.5, [0, 0, 0])])},
        'bow': {'rotation': frames([(0, [0, 0, -80]), (0.3, [0, 0, -80]), (0.5, [0, 0, 0])])},
    }}

    # Bite: she lunges, her snakes strike forward.
    bite = {}
    for i in range(1, len(SNAKES) + 1):
        bite[f'snake{i}'] = {'rotation': frames([(0, [0, 0, 0]), (0.15, [-50, 0, 0]), (0.5, [0, 0, 0])])}
        bite[f'snake{i}_tip'] = {'rotation': frames([(0, [0, 0, 0]), (0.15, [-40, 0, 0]), (0.5, [0, 0, 0])])}
    bite['body'] = {'rotation': frames([(0, [0, 0, 0]), (0.15, [18, 0, 0]), (0.5, [0, 0, 0])])}
    bite['head'] = {'rotation': frames([(0, [0, 0, 0]), (0.15, [15, 0, 0]), (0.5, [0, 0, 0])])}
    anims['bite'] = {'loop': False, 'animation_length': 0.5, 'bones': bite}

    # Reel: her reflection's gaze throws her back, hands up to her face.
    reel = {}
    for i in range(1, len(SNAKES) + 1):
        reel[f'snake{i}'] = {'rotation': frames([(0, [0, 0, 0]), (0.2, [30, 0, 0]), (0.7, [20, 0, 0]),
                                                 (1.0, [0, 0, 0])])}
    reel['body'] = {'rotation': frames([(0, [0, 0, 0]), (0.2, [-18, 0, 0]), (0.7, [-14, 0, 0]), (1.0, [0, 0, 0])])}
    reel['head'] = {'rotation': frames([(0, [0, 0, 0]), (0.2, [-20, 10, 0]), (0.7, [-15, -10, 0]),
                                        (1.0, [0, 0, 0])])}
    reel['left_arm'] = {'rotation': frames([(0, [0, 0, 0]), (0.2, [150, 0, 20]), (0.7, [140, 0, 20]),
                                            (1.0, [0, 0, 0])])}
    reel['right_arm'] = {'rotation': frames([(0, [0, 0, 0]), (0.2, [150, 0, -20]), (0.7, [140, 0, -20]),
                                             (1.0, [0, 0, 0])])}
    anims['reel'] = {'loop': False, 'animation_length': 1.0, 'bones': reel}

    out = {'format_version': '1.8.0', 'animations': anims}
    with open(os.path.join(ASSETS, 'animations/entity/medusa.animation.json'), 'w') as f:
        json.dump(out, f, indent='\t')
        f.write('\n')
    print('medusa animations:', ', '.join(anims))


if __name__ == '__main__':
    build('medusa', MEDUSA, 128, (2, 3))
    animations()
