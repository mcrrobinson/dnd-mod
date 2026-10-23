"""Generates the Troll's GeckoLib model, animations and texture.

    python3 tools/troll_model.py

Writes geo/entity/troll.geo.json, animations/entity/troll.animation.json and
textures/entity/troll.png. Each cube face gets its own spot on the texture (per-face UVs, packed in
shelves) and is painted by the cube's material: mottled green-grey hide, a paler warty belly, a long
drooping nose, mossy hair tufts, a hide loincloth and black three-claw hands.

The troll is hunched: the chest leans out over the hips, the head hangs in front of the shoulders and
the long arms reach almost to the ground. The front is -Z (north), as in the other mod models.
"""
import json
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/dndclasses')
GEO = os.path.join(ASSETS, 'geo/entity/troll.geo.json')
ANIM = os.path.join(ASSETS, 'animations/entity/troll.animation.json')
PNG = os.path.join(ASSETS, 'textures/entity/troll.png')

TEX_W, TEX_H = 128, 128


def mirror(origin, size):
    """The same cube on the other side of the body (x flipped)."""
    return [-(origin[0] + size[0]), origin[1], origin[2]]


def arm(side):
    """Upper arm, then a forearm bone with a hand and three claws, on the left (+x) or right (-x)."""
    s = 1 if side == 'left' else -1

    def cube(origin, size, material):
        return (origin if s > 0 else mirror(origin, size), size, material)

    upper = ('arm_' + side, 'body', [s * 10.5, 33, -3], [
        cube([8, 19, -5.5], [5, 15, 6], 'skin'),
        cube([8, 30, -6], [6, 5, 7], 'hair'),  # mossy shoulder tuft
    ])
    fore = ('forearm_' + side, 'arm_' + side, [s * 10.5, 20, -2.5], [
        cube([8.5, 6, -5], [4, 14, 5], 'skin'),
        cube([8, 2, -6], [5, 5, 6], 'hand'),
        cube([8, 0, -7], [1, 3, 1], 'claw'),
        cube([10, 0, -7], [1, 3, 1], 'claw'),
        cube([12, 0, -7], [1, 3, 1], 'claw'),
    ])
    return [upper, fore]


def leg(side):
    s = 1 if side == 'left' else -1

    def cube(origin, size, material):
        return (origin if s > 0 else mirror(origin, size), size, material)

    return [('leg_' + side, 'root', [s * 4, 15, 1], [
        cube([1.5, 2, -1.5], [5, 14, 6], 'skin'),
        cube([1, 0, -4.5], [6, 2, 8], 'foot'),
    ])]


# name, parent, pivot, [(origin, size, material)]
BONES = [
    ('root', None, [0, 0, 0], []),
    ('body', 'root', [0, 15, 1], [
        ([-6.5, 13, -3], [13, 8, 9], 'belly'),        # hips and gut
        ([-7, 12, -3.5], [14, 4, 10], 'cloth'),        # loincloth
        ([-8, 21, -7], [16, 12, 12], 'skin'),          # chest, leaning forward
        ([-6, 25, -8], [12, 7, 1], 'belly'),           # paler chest front
        ([-6.5, 32, -4], [13, 4, 9], 'hair'),          # mossy hump between the shoulders
    ]),
    ('head', 'body', [0, 31, -7], [
        ([-4, 28, -15], [8, 8, 8], 'face'),
        ([-3.5, 26, -14.5], [7, 2, 6], 'jaw'),
        ([-3, 35, -13], [6, 3, 6], 'hair'),            # hair tuft
        ([4, 31, -11], [2, 3, 1], 'ear'),
        ([-6, 31, -11], [2, 3, 1], 'ear'),
    ]),
    ('nose', 'head', [0, 32, -15], [
        ([-1, 28.5, -18], [2, 5, 3], 'nose'),
    ]),
] + arm('left') + arm('right') + leg('left') + leg('right')

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


SKIN = (92, 108, 78)
SKIN_DARK = (58, 70, 50)
SKIN_GREY = (104, 106, 96)
BELLY = (138, 146, 108)
WART = (70, 84, 56)
HAIR = (46, 70, 34)
HAIR_LIGHT = (78, 104, 50)
CLOTH = (96, 68, 44)
CLOTH_DARK = (64, 44, 28)
CLAW = (34, 30, 26)
EYE = (236, 210, 70)
MOUTH = (40, 24, 20)
TUSK = (226, 216, 180)


def paint_skin(face, x, y, w, h):
    # Mottled: big blotches of grey and dark green, a few warts.
    c = jitter(SKIN, 8)
    r = rng.random()
    if r < 0.18:
        c = mix(c, SKIN_GREY, 0.6)
    elif r < 0.30:
        c = mix(c, SKIN_DARK, 0.5)
    elif r < 0.34:
        c = jitter(WART, 6)
    if face == 'down':
        c = mix(c, SKIN_DARK, 0.4)
    return c


def paint_belly(face, x, y, w, h):
    c = jitter(BELLY, 8)
    if rng.random() < 0.12:
        c = mix(c, SKIN, 0.5)
    if face != 'north':
        c = mix(c, SKIN, 0.6)
    return c


def paint_hair(face, x, y, w, h):
    c = jitter(HAIR, 10)
    if (x + y * 3) % 5 == 0:
        c = mix(c, HAIR_LIGHT, 0.6)
    if face in ('north', 'south', 'east', 'west') and y == h - 1 and x % 2 == 0:
        c = mix(c, SKIN_DARK, 0.6)  # ragged hem
    return c


def paint_cloth(face, x, y, w, h):
    c = jitter(CLOTH, 6)
    if y == 0 or (x * 7 + y) % 9 == 0:
        c = mix(c, CLOTH_DARK, 0.7)
    return c


def paint_hand(face, x, y, w, h):
    c = paint_skin(face, x, y, w, h)
    return mix(c, SKIN_DARK, 0.3)


def paint_claw(face, x, y, w, h):
    return jitter(CLAW, 4)


def paint_foot(face, x, y, w, h):
    c = mix(paint_skin(face, x, y, w, h), SKIN_DARK, 0.3)
    if face == 'north' and y >= h - 1 and x % 2 == 1:
        c = jitter(CLAW, 4)  # toenails
    return c


def paint_face(face, x, y, w, h):
    if face != 'north':
        return paint_skin(face, x, y, w, h)
    # A heavy brow over small yellow eyes, sunken cheeks.
    c = paint_skin(face, x, y, w, h)
    if y <= 1:
        return mix(c, SKIN_DARK, 0.5)  # brow
    if y == 3 and x in (1, 2, w - 3, w - 2):
        return EYE if x in (2, w - 3) else (20, 16, 10)
    if y == 2 and 1 <= x <= w - 2:
        return mix(c, SKIN_DARK, 0.6)  # shadow under the brow
    return c


def paint_jaw(face, x, y, w, h):
    c = paint_skin(face, x, y, w, h)
    if face == 'north':
        if y == 0:
            return TUSK if x in (1, w - 2) else MOUTH
        if y == 1 and x in (1, w - 2):
            return TUSK
    return c


def paint_nose(face, x, y, w, h):
    c = jitter(mix(SKIN, SKIN_DARK, 0.3), 8)
    if rng.random() < 0.2:
        c = jitter(WART, 6)
    if face == 'down' or y == h - 1:
        c = mix(c, SKIN_DARK, 0.5)
    return c


def paint_ear(face, x, y, w, h):
    return jitter(mix(SKIN, SKIN_DARK, 0.4), 6)


PAINTERS = {'skin': paint_skin, 'belly': paint_belly, 'hair': paint_hair, 'cloth': paint_cloth,
            'hand': paint_hand, 'claw': paint_claw, 'foot': paint_foot, 'face': paint_face, 'jaw': paint_jaw,
            'nose': paint_nose, 'ear': paint_ear}


# ---- Animations ----

def sin(amp, speed=360, phase=0):
    """A Molang sine over the animation time, in degrees."""
    ph = f'+{phase}' if phase else ''
    return f'math.sin(query.anim_time*{speed}{ph})*{amp}'


ANIMATIONS = {
    'idle': {
        'loop': True, 'animation_length': 3,
        'bones': {
            'body': {'rotation': [sin(2, 120), 0, 0]},
            'head': {'rotation': [sin(3, 120, 40), sin(6, 60), 0]},
            'arm_left': {'rotation': [sin(3, 120), 0, sin(2, 120)]},
            'arm_right': {'rotation': [sin(3, 120, 60), 0, '-' + sin(2, 120)]},
        },
    },
    'walk': {
        'loop': True, 'animation_length': 1.2,
        'bones': {
            'body': {'rotation': [0, sin(4, 300), sin(3, 300)]},
            'arm_left': {'rotation': [sin(18, 300), 0, 0]},
            'arm_right': {'rotation': ['-' + sin(18, 300), 0, 0]},
            'leg_left': {'rotation': ['-' + sin(24, 300), 0, 0]},
            'leg_right': {'rotation': [sin(24, 300), 0, 0]},
        },
    },
    'run': {
        'loop': True, 'animation_length': 0.6,
        'bones': {
            'body': {'rotation': [8, sin(6, 600), sin(3, 600)]},
            'head': {'rotation': [-8, 0, 0]},
            'arm_left': {'rotation': [sin(35, 600), 0, 0]},
            'arm_right': {'rotation': ['-' + sin(35, 600), 0, 0]},
            'leg_left': {'rotation': ['-' + sin(35, 600), 0, 0]},
            'leg_right': {'rotation': [sin(35, 600), 0, 0]},
        },
    },
    'claw_left': {
        'animation_length': 0.4,
        'bones': {
            'body': {'rotation': {'0.0': [0, 0, 0], '0.12': [-6, -12, 0], '0.25': [6, 14, 0], '0.4': [0, 0, 0]}},
            'arm_left': {'rotation': {'0.0': [0, 0, 0], '0.12': [-125, 0, -25], '0.25': [-20, 0, 20],
                                      '0.4': [0, 0, 0]}},
            'forearm_left': {'rotation': {'0.0': [0, 0, 0], '0.12': [-30, 0, 0], '0.25': [0, 0, 0]}},
        },
    },
    'claw_right': {
        'animation_length': 0.4,
        'bones': {
            'body': {'rotation': {'0.0': [0, 0, 0], '0.12': [-6, 12, 0], '0.25': [6, -14, 0], '0.4': [0, 0, 0]}},
            'arm_right': {'rotation': {'0.0': [0, 0, 0], '0.12': [-125, 0, 25], '0.25': [-20, 0, -20],
                                       '0.4': [0, 0, 0]}},
            'forearm_right': {'rotation': {'0.0': [0, 0, 0], '0.12': [-30, 0, 0], '0.25': [0, 0, 0]}},
        },
    },
    # The second claw when the first one landed: both arms come down together and tear.
    'rend': {
        'animation_length': 0.6,
        'bones': {
            'body': {'rotation': {'0.0': [0, 0, 0], '0.15': [-14, 0, 0], '0.3': [16, 0, 0], '0.6': [0, 0, 0]}},
            'head': {'rotation': {'0.0': [0, 0, 0], '0.15': [-20, 0, 0], '0.3': [10, 0, 0], '0.6': [0, 0, 0]}},
            'arm_left': {'rotation': {'0.0': [0, 0, 0], '0.15': [-150, 0, -30], '0.3': [-30, 0, 25],
                                      '0.6': [0, 0, 0]}},
            'arm_right': {'rotation': {'0.0': [0, 0, 0], '0.15': [-150, 0, 30], '0.3': [-30, 0, -25],
                                       '0.6': [0, 0, 0]}},
        },
    },
    # Knocked flat on its back, arms flung out; a slow heave of breath while its wounds knit.
    'felled': {
        'loop': True, 'animation_length': 2,
        'bones': {
            'root': {'rotation': [-88, 0, 0], 'position': [0, 2, 6]},
            'head': {'rotation': [-10, 30, 0]},
            'arm_left': {'rotation': [0, 0, 60]},
            'arm_right': {'rotation': [0, 0, -75]},
            'leg_left': {'rotation': [0, 0, -10]},
            'leg_right': {'rotation': [0, 0, 10]},
            'body': {'rotation': [sin(2, 180), 0, 0]},
        },
    },
    # Gets back up, then roars.
    'rise': {
        'animation_length': 1.6,
        'bones': {
            'root': {'rotation': {'0.0': [-88, 0, 0], '0.5': [-40, 0, 0], '0.8': [0, 0, 0]},
                     'position': {'0.0': [0, 2, 6], '0.5': [0, 1, 3], '0.8': [0, 0, 0]}},
            'head': {'rotation': {'0.8': [0, 0, 0], '1.0': [-35, 0, 0], '1.4': [-35, 0, 0], '1.6': [0, 0, 0]}},
            'arm_left': {'rotation': {'0.0': [0, 0, 60], '0.8': [0, 0, 0], '1.0': [-40, 0, -50],
                                      '1.4': [-40, 0, -50], '1.6': [0, 0, 0]}},
            'arm_right': {'rotation': {'0.0': [0, 0, -75], '0.8': [0, 0, 0], '1.0': [-40, 0, 50],
                                       '1.4': [-40, 0, 50], '1.6': [0, 0, 0]}},
        },
    },
    'roar': {
        'animation_length': 1.0,
        'bones': {
            'body': {'rotation': {'0.0': [0, 0, 0], '0.2': [-12, 0, 0], '0.8': [-12, 0, 0], '1.0': [0, 0, 0]}},
            'head': {'rotation': {'0.0': [0, 0, 0], '0.2': [-35, 0, 0], '0.8': [-35, 0, 0], '1.0': [0, 0, 0]}},
            'arm_left': {'rotation': {'0.0': [0, 0, 0], '0.2': [-40, 0, -50], '0.8': [-40, 0, -50],
                                      '1.0': [0, 0, 0]}},
            'arm_right': {'rotation': {'0.0': [0, 0, 0], '0.2': [-40, 0, 50], '0.8': [-40, 0, 50],
                                       '1.0': [0, 0, 0]}},
        },
    },
}


def main():
    pixels = [[(0, 0, 0, 0)] * TEX_W for _ in range(TEX_H)]
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
            out_cubes.append({'origin': origin, 'size': size, 'uv': uv})
        if out_cubes:
            bone['cubes'] = out_cubes
        bones.append(bone)

    geo = {
        'format_version': '1.12.0',
        'minecraft:geometry': [{
            'description': {
                'identifier': 'geometry.troll',
                'texture_width': TEX_W,
                'texture_height': TEX_H,
                'visible_bounds_width': 4,
                'visible_bounds_height': 4,
                'visible_bounds_offset': [0, 1.5, 0],
            },
            'bones': bones,
        }],
    }
    with open(GEO, 'w') as f:
        json.dump(geo, f, indent='\t')
        f.write('\n')
    with open(ANIM, 'w') as f:
        json.dump({'format_version': '1.8.0', 'animations': ANIMATIONS}, f, indent='\t')
        f.write('\n')
    pngio.write_png(PNG, TEX_W, TEX_H, pixels)
    print('wrote', os.path.relpath(GEO, ROOT), os.path.relpath(ANIM, ROOT), 'and', os.path.relpath(PNG, ROOT))


if __name__ == '__main__':
    main()
