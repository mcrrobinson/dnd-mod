"""Generates the Rust Monster's GeckoLib model, animations and texture.

    python3 tools/rust_monster_model.py

Writes geo/entity/rust_monster.geo.json, animations/entity/rust_monster.animation.json and
textures/entity/rust_monster.png. Each cube face gets its own spot on the texture (per-face UVs,
packed in shelves, like tools/owlbear_model.py) and is painted by the cube's material: a low,
armadillo-like body of rust-orange shell plates over a blue-grey belly, four stubby legs, a blunt head
with two long feathery antennae, and a tail ending in a propeller of flat fins. The antenna vanes and
the fins are zero-thickness cubes with see-through fringes.
"""
import json
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/dndclasses')
GEO = os.path.join(ASSETS, 'geo/entity/rust_monster.geo.json')
ANIM = os.path.join(ASSETS, 'animations/entity/rust_monster.animation.json')
PNG = os.path.join(ASSETS, 'textures/entity/rust_monster.png')

TEX_W, TEX_H = 128, 128

# name, parent, pivot, rotation, [(origin, size, material)]
BONES = [
    ('root', None, [0, 0, 0], None, []),
    ('body', 'root', [0, 8, 0], None, [
        ([-7, 5, -9], [14, 8, 18], 'shell'),
        ([-6, 4, -8], [12, 1, 16], 'belly'),
        ([-7.5, 11, -7], [15, 3, 3], 'plate'),
        ([-7.5, 11.5, -2], [15, 3, 3], 'plate'),
        ([-7.5, 11, 3], [15, 3, 3], 'plate'),
    ]),
    ('head', 'body', [0, 9, -9], None, [
        ([-4, 5, -15], [8, 7, 6], 'head'),
        ([-2.5, 5, -17], [5, 4, 2], 'snout'),
    ]),
    ('antenna_left', 'head', [2, 12, -13], [25, 0, 15], [
        ([1.5, 12, -13.5], [1, 13, 1], 'stalk'),
        ([2, 17, -15], [0, 9, 4], 'vane'),
    ]),
    ('antenna_right', 'head', [-2, 12, -13], [25, 0, -15], [
        ([-2.5, 12, -13.5], [1, 13, 1], 'stalk'),
        ([-2, 17, -15], [0, 9, 4], 'vane'),
    ]),
    ('tail', 'body', [0, 9, 9], [-10, 0, 0], [
        ([-1, 8, 9], [2, 2, 8], 'tail'),
    ]),
    ('tail_fin', 'tail', [0, 9, 17], None, [
        ([-5, 9, 16], [10, 0, 3], 'fin'),
        ([0, 4, 16], [0, 10, 3], 'fin'),
    ]),
    ('leg_front_left', 'root', [4.5, 6, -5], None, [
        ([3, 0, -7], [4, 6, 4], 'leg'),
    ]),
    ('leg_front_right', 'root', [-4.5, 6, -5], None, [
        ([-7, 0, -7], [4, 6, 4], 'leg'),
    ]),
    ('leg_back_left', 'root', [4.5, 6, 5], None, [
        ([3, 0, 3], [4, 6, 4], 'leg'),
    ]),
    ('leg_back_right', 'root', [-4.5, 6, 5], None, [
        ([-7, 0, 3], [4, 6, 4], 'leg'),
    ]),
]

FACES = ('north', 'south', 'east', 'west', 'up', 'down')


def face_size(face, size):
    w, h, d = (int(round(s)) for s in size)
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


RUST = (178, 86, 38)
RUST_DARK = (112, 50, 22)
RUST_LIGHT = (214, 128, 60)
BELLY = (112, 124, 142)
BELLY_DARK = (78, 88, 104)
EYE = (20, 16, 12)
VANE = (204, 112, 52)
VANE_TIP = (236, 170, 96)
CLEAR = None


def paint_shell(face, x, y, w, h):
    c = jitter(RUST, 10)
    if rng.random() < 0.12:
        c = mix(c, RUST_LIGHT, 0.6)  # flecks of fresh rust
    # Overlapping bands across the back: a dark seam every 4 px along the body.
    if face in ('up', 'east', 'west'):
        along = y if face == 'up' else x
        if along % 4 == 3:
            c = mix(c, RUST_DARK, 0.7)
    if face in ('east', 'west') and y >= h - 2:
        c = mix(c, BELLY, 0.5)  # the shell's rim fades into the belly
    if face == 'down':
        c = jitter(BELLY, 6)
    return c


def paint_plate(face, x, y, w, h):
    c = jitter(mix(RUST, RUST_DARK, 0.25), 8)
    if y == 0 and face in ('north', 'south', 'east', 'west'):
        c = mix(c, RUST_LIGHT, 0.5)
    return c


def paint_belly(face, x, y, w, h):
    c = jitter(BELLY, 6)
    if face == 'down' and y % 3 == 0:
        c = mix(c, BELLY_DARK, 0.5)
    return c


def paint_head(face, x, y, w, h):
    c = jitter(RUST, 8)
    if face == 'north':
        # Two small black eyes high on the face.
        if y == 1 and x in (1, w - 2):
            return EYE
        if y >= h - 2:
            c = mix(c, BELLY, 0.5)
    if face == 'down':
        c = jitter(BELLY, 6)
    return c


def paint_snout(face, x, y, w, h):
    c = jitter(mix(RUST, BELLY, 0.45), 6)
    if face == 'north' and y == h - 2 and 0 < x < w - 1:
        return (40, 26, 20)  # mouth
    return c


def paint_stalk(face, x, y, w, h):
    return jitter(RUST_DARK, 8)


def paint_vane(face, x, y, w, h):
    # Feathery: barbs fanning out from the middle, with a ragged see-through edge.
    if face not in ('east', 'west'):
        return jitter(RUST_DARK, 6)
    edge = min(x, w - 1 - x)
    if edge == 0 and (y + x) % 2 == 0:
        return CLEAR
    if y == 0 or (y == h - 1 and edge == 0):
        return CLEAR
    c = jitter(VANE, 10)
    if (y + edge) % 3 == 0:
        c = mix(c, RUST_DARK, 0.4)
    if y <= 1:
        c = mix(c, VANE_TIP, 0.6)
    return c


def paint_tail(face, x, y, w, h):
    c = jitter(mix(RUST, RUST_DARK, 0.3), 8)
    if face in ('east', 'west', 'up') and (x if face != 'up' else y) % 3 == 2:
        c = mix(c, RUST_DARK, 0.6)
    return c


def paint_fin(face, x, y, w, h):
    # A flat, propeller-like blade: rust in the middle, a ragged light edge.
    if face in ('north', 'south'):
        return jitter(RUST_DARK, 6)
    long_axis = max(w, h)
    i = x if w == long_axis else y
    j = y if w == long_axis else x
    short = min(w, h)
    if (i == 0 or i == long_axis - 1) and j != short // 2:
        return CLEAR
    c = jitter(VANE, 10)
    if j == short // 2:
        c = mix(c, RUST_DARK, 0.5)
    return c


def paint_leg(face, x, y, w, h):
    c = jitter(mix(RUST_DARK, BELLY_DARK, 0.4), 8)
    if face in ('north', 'south', 'east', 'west') and y >= h - 1:
        c = (60, 54, 50)  # blunt claws
    if face == 'up':
        c = jitter(RUST_DARK, 6)
    return c


PAINTERS = {'shell': paint_shell, 'plate': paint_plate, 'belly': paint_belly, 'head': paint_head,
            'snout': paint_snout, 'stalk': paint_stalk, 'vane': paint_vane, 'tail': paint_tail,
            'fin': paint_fin, 'leg': paint_leg}


def build():
    pixels = [[(0, 0, 0, 0)] * TEX_W for _ in range(TEX_H)]
    packer = Packer()
    bones = []
    for name, parent, pivot, rotation, cubes in BONES:
        bone = {'name': name, 'pivot': pivot}
        if parent:
            bone['parent'] = parent
        if rotation:
            bone['rotation'] = rotation
        out_cubes = []
        for origin, size, material in cubes:
            uv = {}
            for face in FACES:
                w, h = face_size(face, size)
                if w == 0 or h == 0:
                    continue
                u, v = packer.place(w, h)
                uv[face] = {'uv': [u, v], 'uv_size': [w, h]}
                for y in range(h):
                    for x in range(w):
                        c = PAINTERS[material](face, x, y, w, h)
                        if c is not None:
                            pixels[v + y][u + x] = (c[0], c[1], c[2], 255)
            out_cubes.append({'origin': origin, 'size': size, 'uv': uv})
        if out_cubes:
            bone['cubes'] = out_cubes
        bones.append(bone)
    return bones, pixels


def sine(amplitude, speed=360, phase=0, offset=0):
    expr = f'math.sin(query.anim_time*{speed}{"+" + str(phase) if phase else ""})*{amplitude}'
    return f'{offset}+{expr}' if offset else expr


def animations():
    """idle, walk, antenna_touch and eat. Bone rotations add to the rest pose's rotations."""
    fin_spin_slow = {'rotation': [0, 0, 'query.anim_time*90']}
    fin_spin_fast = {'rotation': [0, 0, 'query.anim_time*360']}
    return {
        'idle': {
            'loop': True,
            'animation_length': 4,
            'bones': {
                'antenna_left': {'rotation': [sine(6, 90), 0, sine(4, 180)]},
                'antenna_right': {'rotation': [sine(6, 90, 60), 0, sine(-4, 180, 30)]},
                'body': {'scale': [1, sine(0.02, 90, 0, 1), 1]},
                'tail': {'rotation': [0, sine(6, 90), 0]},
                'tail_fin': fin_spin_slow,
            },
        },
        'walk': {
            'loop': True,
            'animation_length': 1,
            'bones': {
                'leg_front_left': {'rotation': [sine(30), 0, 0]},
                'leg_back_right': {'rotation': [sine(30), 0, 0]},
                'leg_front_right': {'rotation': [sine(-30), 0, 0]},
                'leg_back_left': {'rotation': [sine(-30), 0, 0]},
                'body': {'rotation': [0, 0, sine(3)]},
                'head': {'rotation': [0, sine(5), 0]},
                'antenna_left': {'rotation': [sine(10, 720), 0, 0]},
                'antenna_right': {'rotation': [sine(-10, 720), 0, 0]},
                'tail': {'rotation': [0, sine(-10), 0]},
                'tail_fin': fin_spin_fast,
            },
        },
        'antenna_touch': {
            'animation_length': 0.75,
            'bones': {
                'head': {'rotation': {'0.0': [0, 0, 0], '0.25': [15, 0, 0], '0.5': [10, 0, 0], '0.75': [0, 0, 0]}},
                'antenna_left': {'rotation': {'0.0': [0, 0, 0], '0.25': [-70, 0, 10], '0.4': [-80, 0, 20],
                                              '0.75': [0, 0, 0]}},
                'antenna_right': {'rotation': {'0.0': [0, 0, 0], '0.25': [-70, 0, -10], '0.4': [-80, 0, -20],
                                               '0.75': [0, 0, 0]}},
                'body': {'position': {'0.0': [0, 0, 0], '0.25': [0, 0, -1.5], '0.75': [0, 0, 0]}},
            },
        },
        'eat': {
            'animation_length': 1,
            'bones': {
                'head': {'rotation': {'0.0': [0, 0, 0], '0.15': [35, 0, 0], '0.3': [20, 0, 0], '0.45': [38, 0, 0],
                                      '0.6': [20, 0, 0], '0.75': [35, 0, 0], '1.0': [0, 0, 0]}},
                'antenna_left': {'rotation': {'0.0': [0, 0, 0], '0.5': [-20, 0, 15], '1.0': [0, 0, 0]}},
                'antenna_right': {'rotation': {'0.0': [0, 0, 0], '0.5': [-20, 0, -15], '1.0': [0, 0, 0]}},
                'tail_fin': fin_spin_fast,
            },
        },
    }


def main():
    bones, pixels = build()
    geo = {
        'format_version': '1.12.0',
        'minecraft:geometry': [{
            'description': {
                'identifier': 'geometry.rust_monster',
                'texture_width': TEX_W,
                'texture_height': TEX_H,
                'visible_bounds_width': 3,
                'visible_bounds_height': 2,
                'visible_bounds_offset': [0, 0.75, 0],
            },
            'bones': bones,
        }],
    }
    with open(GEO, 'w') as f:
        json.dump(geo, f, indent='\t')
        f.write('\n')
    with open(ANIM, 'w') as f:
        json.dump({'format_version': '1.8.0', 'animations': animations()}, f, indent='\t')
        f.write('\n')
    pngio.write_png(PNG, TEX_W, TEX_H, pixels)
    print('wrote', os.path.relpath(GEO, ROOT), os.path.relpath(ANIM, ROOT), 'and', os.path.relpath(PNG, ROOT))


if __name__ == '__main__':
    main()
