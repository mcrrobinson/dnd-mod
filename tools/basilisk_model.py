"""Generates the Basilisk's GeckoLib model, animations and textures, and the Basilisk Eye and Salve of
Stone to Flesh item icons.

    python3 tools/basilisk_model.py

Writes:
- geo/entity/basilisk.geo.json, animations/entity/basilisk.animation.json
- textures/entity/basilisk.png and basilisk_glowmask.png (the eyes glow: AutoGlowingGeoLayer)
- textures/item/basilisk_eye.png and salve_of_stone_to_flesh.png

An eight-legged lizard (four leg pairs) with a spiny back, a long tail and big glowing yellow eyes. Each
cube face gets its own spot on the texture (per-face UVs, packed in shelves) and is painted by the cube's
material: dark green-grey scales with a pale yellow belly, bone-coloured spines, yellow slit-pupil eyes.
"""
import json
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/dndclasses')
GEO = os.path.join(ASSETS, 'geo/entity/basilisk.geo.json')
ANIM = os.path.join(ASSETS, 'animations/entity/basilisk.animation.json')
PNG = os.path.join(ASSETS, 'textures/entity/basilisk.png')
GLOW = os.path.join(ASSETS, 'textures/entity/basilisk_glowmask.png')
EYE_ITEM = os.path.join(ASSETS, 'textures/item/basilisk_eye.png')
SALVE_ITEM = os.path.join(ASSETS, 'textures/item/salve_of_stone_to_flesh.png')

TEX_W, TEX_H = 128, 128

# Leg pairs along the body (z of the leg's centre). The model faces -Z (north).
LEG_Z = [-8, -3, 2, 7]

BONES = [
    ('root', None, [0, 0, 0], []),
    ('body', 'root', [0, 8, 0], [
        ([-5, 4, -11], [10, 7, 22], 'scale'),
    ]),
    ('spines', 'body', [0, 11, 0], [
        ([-1, 11, z], [2, 3 if i % 2 == 0 else 2, 2], 'spine') for i, z in enumerate(range(-10, 10, 3))
    ]),
    ('neck', 'body', [0, 8, -11], [
        ([-3.5, 5, -14], [7, 5, 3], 'scale'),
    ]),
    ('head', 'neck', [0, 8, -14], [
        ([-4, 5, -21], [8, 5, 7], 'head'),
        ([-3, 5, -24], [6, 3, 3], 'snout'),
        ([-4.5, 8.5, -20], [3, 3, 3], 'eye'),
        ([1.5, 8.5, -20], [3, 3, 3], 'eye'),
        ([-0.5, 10, -19], [1, 2, 4], 'spine'),
    ]),
    ('jaw', 'head', [0, 5, -18], [
        ([-3.5, 3.5, -24], [7, 2, 6], 'jaw'),
    ]),
    ('tail', 'body', [0, 8, 11], [
        ([-3.5, 5, 11], [7, 5, 8], 'scale'),
        ([-0.5, 10, 12], [1, 2, 2], 'spine'),
        ([-0.5, 10, 16], [1, 2, 2], 'spine'),
    ]),
    ('tail_tip', 'tail', [0, 7, 19], [
        ([-2, 5.5, 19], [4, 3, 9], 'scale'),
    ]),
]
for i, z in enumerate(LEG_Z):
    n = i + 1
    BONES.append((f'leg_left{n}', 'body', [5, 6, z], [
        ([4.5, 4.5, z - 1.5], [4, 2, 3], 'scale'),
        ([7, 0, z - 1.5], [3, 5, 3], 'leg'),
    ]))
    BONES.append((f'leg_right{n}', 'body', [-5, 6, z], [
        ([-8.5, 4.5, z - 1.5], [4, 2, 3], 'scale'),
        ([-10, 0, z - 1.5], [3, 5, 3], 'leg'),
    ]))

FACES = ('north', 'south', 'east', 'west', 'up', 'down')


def face_size(face, size):
    w, h, d = (max(1, int(round(s))) for s in size)
    return {'north': (w, h), 'south': (w, h), 'east': (d, h), 'west': (d, h),
            'up': (w, d), 'down': (w, d)}[face]


class Packer:
    """Shelf packer with a 1px gap so neighbouring faces never bleed into each other."""

    def __init__(self):
        self.x, self.y, self.row = 0, 0, 0

    def place(self, w, h):
        if self.x + w > TEX_W:
            self.x, self.y, self.row = 0, self.y + self.row + 1, 0
        pos = (self.x, self.y)
        self.x += w + 1
        self.row = max(self.row, h)
        assert self.y + h <= TEX_H, 'texture too small'
        return pos


rng = random.Random(11)


def jitter(c, amount):
    j = rng.randint(-amount, amount)
    return tuple(max(0, min(255, v + j)) for v in c)


def mix(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


SCALE = (66, 84, 58)
SCALE_DARK = (38, 50, 34)
SCALE_LIGHT = (98, 116, 78)
BELLY = (188, 172, 108)
SPINE = (206, 196, 160)
SPINE_DARK = (130, 118, 90)
EYE = (255, 214, 40)
EYE_PUPIL = (20, 14, 6)
CLAW = (60, 52, 40)
MOUTH = (120, 40, 36)


def paint_scale(face, x, y, w, h):
    if face == 'down':
        return jitter(BELLY, 8)
    # Staggered scale rows: a dark edge at the bottom of each 2px scale, lighter highlights now and then.
    row = y // 2
    c = jitter(SCALE, 7)
    if y % 2 == 1 and (x + row) % 3 != 0:
        c = mix(c, SCALE_DARK, 0.6)
    elif rng.random() < 0.08:
        c = mix(c, SCALE_LIGHT, 0.7)
    if face in ('east', 'west', 'north', 'south') and y >= h - 1 and h > 3:
        c = mix(c, BELLY, 0.5)  # the belly shows along the lower edge
    return c


def paint_leg(face, x, y, w, h):
    c = paint_scale(face, x, y, w, h)
    if face in ('north', 'south', 'east', 'west') and y >= h - 1:
        c = jitter(CLAW, 5)
    return c


def paint_spine(face, x, y, w, h):
    c = jitter(SPINE, 6)
    if face == 'down' or y >= h - 1:
        c = mix(c, SPINE_DARK, 0.6)
    return c


def paint_head(face, x, y, w, h):
    c = paint_scale(face, x, y, w, h)
    if face == 'north' and y >= h - 1:
        return jitter(MOUTH, 6)
    return c


def paint_snout(face, x, y, w, h):
    c = paint_scale(face, x, y, w, h)
    if face == 'up' and y == 0 and x in (1, w - 2):
        return (14, 12, 10)  # nostrils
    if face == 'north' and y >= h - 1:
        return jitter(MOUTH, 6)
    return c


def paint_jaw(face, x, y, w, h):
    if face == 'up':
        c = jitter(MOUTH, 8)
        if y == 0 or x in (0, w - 1):
            c = (230, 224, 200)  # teeth round the edge
        return c
    if face == 'down':
        return jitter(BELLY, 8)
    return paint_scale(face, x, y, w, h)


def paint_eye(face, x, y, w, h):
    if face in ('down',):
        return jitter(SCALE_DARK, 5)
    c = jitter(EYE, 10)
    if face in ('north', 'east', 'west') and x == w // 2:
        return EYE_PUPIL  # a slit pupil
    if (x, y) == (0, 0):
        c = (255, 250, 200)
    return c


PAINTERS = {'scale': paint_scale, 'leg': paint_leg, 'spine': paint_spine, 'head': paint_head,
            'snout': paint_snout, 'jaw': paint_jaw, 'eye': paint_eye}
GLOWING = {'eye'}


def model():
    pixels = [[(0, 0, 0, 0)] * TEX_W for _ in range(TEX_H)]
    glow = [[(0, 0, 0, 0)] * TEX_W for _ in range(TEX_H)]
    packer = Packer()
    bones = []
    for name, parent, pivot, cubes in BONES:
        bone = {'name': name, 'pivot': pivot}
        if parent:
            bone['parent'] = parent
        out_cubes = []
        for origin, size, material in cubes:
            uv = {}
            for face in FACES:
                w, h = face_size(face, size)
                u, v = packer.place(w, h)
                uv[face] = {'uv': [u, v], 'uv_size': [w, h]}
                for y in range(h):
                    for x in range(w):
                        r, g, b = PAINTERS[material](face, x, y, w, h)
                        pixels[v + y][u + x] = (r, g, b, 255)
                        if material in GLOWING and (r, g, b) != EYE_PUPIL and face != 'down':
                            glow[v + y][u + x] = (r, g, b, 255)
            out_cubes.append({'origin': origin, 'size': size, 'uv': uv})
        if out_cubes:
            bone['cubes'] = out_cubes
        bones.append(bone)

    geo = {
        'format_version': '1.12.0',
        'minecraft:geometry': [{
            'description': {
                'identifier': 'geometry.basilisk',
                'texture_width': TEX_W,
                'texture_height': TEX_H,
                'visible_bounds_width': 4,
                'visible_bounds_height': 2,
                'visible_bounds_offset': [0, 0.5, 0],
            },
            'bones': bones,
        }],
    }
    with open(GEO, 'w') as f:
        json.dump(geo, f, indent='\t')
        f.write('\n')
    pngio.write_png(PNG, TEX_W, TEX_H, pixels)
    pngio.write_png(GLOW, TEX_W, TEX_H, glow)


def legs(amplitude, speed, lift=0):
    """Eight-leg gait: on each side the pairs alternate, and the two sides are half a step apart."""
    out = {}
    for i in range(len(LEG_Z)):
        n = i + 1
        for side, phase in (('left', 0), ('right', 180)):
            p = phase + (180 if i % 2 else 0)
            bone = {'rotation': [f'math.sin(query.anim_time*{speed}+{p})*{amplitude}', 0, 0]}
            if lift:
                bone['position'] = [0, f'math.max(0, math.cos(query.anim_time*{speed}+{p}))*{lift}', 0]
            out[f'leg_{side}{n}'] = bone
    return out


def animations():
    walk = legs(22, 540, 0.6)
    walk['body'] = {'rotation': [0, 'math.sin(query.anim_time*540)*3', 0]}
    walk['tail'] = {'rotation': [0, 'math.sin(query.anim_time*540+90)*10', 0]}
    walk['tail_tip'] = {'rotation': [0, 'math.sin(query.anim_time*540+150)*14', 0]}
    walk['neck'] = {'rotation': [0, '-math.sin(query.anim_time*540)*4', 0]}
    anims = {
        'idle': {
            'loop': True,
            'animation_length': 4,
            'bones': {
                'body': {'position': [0, 'math.sin(query.anim_time*90)*0.15', 0]},
                'tail': {'rotation': [0, 'math.sin(query.anim_time*90)*6', 0]},
                'tail_tip': {'rotation': [0, 'math.sin(query.anim_time*90+40)*10', 0]},
                'jaw': {'rotation': ['math.max(0, math.sin(query.anim_time*180))*4', 0, 0]},
            },
        },
        'walk': {'loop': True, 'animation_length': 0.6667, 'bones': walk},
        # Rears its head up and stares (the gaze lands): eyes forward, jaw a little open.
        'gaze': {
            'animation_length': 1.0,
            'bones': {
                'neck': {'rotation': {'0.0': [0, 0, 0], '0.25': [-22, 0, 0], '0.75': [-22, 0, 0], '1.0': [0, 0, 0]}},
                'head': {'rotation': {'0.0': [0, 0, 0], '0.25': [12, 0, 0], '0.75': [12, 0, 0], '1.0': [0, 0, 0]}},
                'jaw': {'rotation': {'0.0': [0, 0, 0], '0.25': [10, 0, 0], '0.75': [10, 0, 0], '1.0': [0, 0, 0]}},
            },
        },
        'bite': {
            'animation_length': 0.5,
            'bones': {
                'neck': {'rotation': {'0.0': [0, 0, 0], '0.15': [-10, 0, 0], '0.3': [10, 0, 0], '0.5': [0, 0, 0]},
                         'position': {'0.0': [0, 0, 0], '0.15': [0, 0, 1], '0.3': [0, 0, -2], '0.5': [0, 0, 0]}},
                'jaw': {'rotation': {'0.0': [0, 0, 0], '0.15': [40, 0, 0], '0.3': [0, 0, 0]}},
            },
        },
        # A statue holds still: the stone tint does the rest.
        'statue': {
            'loop': True,
            'animation_length': 1,
            'bones': {'neck': {'rotation': [-8, 0, 0]}},
        },
    }
    with open(ANIM, 'w') as f:
        json.dump({'format_version': '1.8.0', 'animations': anims}, f, indent='\t')
        f.write('\n')


def icons():
    # Basilisk Eye: a big yellow eye with a slit pupil and a dark green scaly rim.
    eye = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) / 6.5) ** 2 + ((y - 7.5) / 5.5) ** 2
            if d <= 1.0:
                if d > 0.72:
                    c = jitter(SCALE_DARK, 6)
                elif abs(x - 7.5) <= 0.5 and abs(y - 7.5) <= 4:
                    c = EYE_PUPIL
                else:
                    c = mix(EYE, (200, 120, 10), max(0.0, d - 0.2))
                    if (x, y) in ((5, 4), (5, 5)):
                        c = (255, 252, 220)
                eye[y][x] = (*c, 255)
    pngio.write_png(EYE_ITEM, 16, 16, eye)

    # Salve: a little round tin, lid off, full of grey-green paste with a yellow fleck.
    salve = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    TIN = (150, 150, 160)
    TIN_DARK = (90, 90, 100)
    PASTE = (150, 170, 130)
    for y in range(16):
        for x in range(16):
            # Tin body: x 2-13, y 7-13; paste top ellipse y 5-8.
            top = ((x - 7.5) / 6) ** 2 + ((y - 7) / 2.2) ** 2
            if 2 <= x <= 13 and 7 <= y <= 13:
                c = mix(TIN, TIN_DARK, (y - 7) / 8)
                if x in (2, 13) or y == 13:
                    c = TIN_DARK
                salve[y][x] = (*jitter(c, 4), 255)
            if top <= 1.0:
                c = jitter(PASTE, 8)
                if top > 0.7:
                    c = TIN
                elif rng.random() < 0.12:
                    c = (230, 200, 70)
                salve[y][x] = (*c, 255)
    pngio.write_png(SALVE_ITEM, 16, 16, salve)


def main():
    model()
    animations()
    icons()
    for path in (GEO, ANIM, PNG, GLOW, EYE_ITEM, SALVE_ITEM):
        print('wrote', os.path.relpath(path, ROOT))


if __name__ == '__main__':
    main()
