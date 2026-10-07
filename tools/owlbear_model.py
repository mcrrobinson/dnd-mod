"""Generates the Owlbear's GeckoLib model and texture.

    python3 tools/owlbear_model.py

Writes geo/entity/owlbear.geo.json and textures/entity/owlbear.png. Each cube face gets its own
spot on the texture (per-face UVs, packed in shelves), and is painted by the cube's material: brown
bear fur on the body and legs, a shaggy feather ruff round the shoulders, a feathered owl head with
a pale face disk and big orange eyes, a hooked beak, ear tufts and ivory claws.
"""
import json
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/dndclasses')
GEO = os.path.join(ASSETS, 'geo/entity/owlbear.geo.json')
PNG = os.path.join(ASSETS, 'textures/entity/owlbear.png')

TEX_W, TEX_H = 128, 64

# name, parent, pivot, [(origin, size, material)]
BONES = [
    ('root', None, [0, 0, 0], []),
    ('body', 'root', [0, 12, 8], [
        ([-7, 9, -10], [14, 13, 22], 'fur'),
        ([-7.5, 11, -11], [15, 12, 6], 'ruff'),
        ([-2, 17, 12], [4, 4, 3], 'feather'),
    ]),
    ('head', 'body', [0, 21, -10], [
        ([-5.5, 16, -19], [11, 11, 10], 'head'),
        ([3, 27, -15], [2, 3, 2], 'tuft'),
        ([-5, 27, -15], [2, 3, 2], 'tuft'),
    ]),
    ('beak', 'head', [0, 21, -19], [
        ([-1.5, 19, -21], [3, 4, 2], 'beak'),
    ]),
    ('jaw', 'head', [0, 19, -19], [
        ([-1, 17, -21], [2, 2, 2], 'beak_lower'),
    ]),
    ('arm_left', 'body', [4.5, 13, -6], [
        ([2, 0, -8.5], [5, 13, 5], 'leg'),
    ]),
    ('arm_right', 'body', [-4.5, 13, -6], [
        ([-7, 0, -8.5], [5, 13, 5], 'leg'),
    ]),
    ('leg_left', 'root', [4.5, 12, 8], [
        ([2, 0, 5.5], [5, 12, 5], 'leg'),
    ]),
    ('leg_right', 'root', [-4.5, 12, 8], [
        ([-7, 0, 5.5], [5, 12, 5], 'leg'),
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


rng = random.Random(7)


def jitter(c, amount):
    j = rng.randint(-amount, amount)
    return tuple(max(0, min(255, v + j)) for v in c)


def mix(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


FUR = (96, 64, 38)
FUR_DARK = (62, 40, 24)
FEATHER = (168, 128, 82)
FEATHER_DARK = (110, 78, 46)
CREAM = (232, 218, 182)
EYE = (245, 150, 25)
EYE_RIM = (40, 24, 12)
BEAK = (214, 178, 92)
BEAK_DARK = (120, 98, 52)
CLAW = (226, 216, 190)


def paint_fur(face, x, y, w, h):
    c = jitter(FUR, 10)
    if rng.random() < 0.15:
        c = mix(c, FUR_DARK, 0.6)
    if face == 'down':
        c = mix(c, FUR_DARK, 0.5)
    return c


def paint_leg(face, x, y, w, h):
    c = paint_fur(face, x, y, w, h)
    if face in ('north', 'south', 'east', 'west') and y >= h - 3:
        c = mix(c, FUR_DARK, 0.5)  # paw
        if face == 'north' and y >= h - 2 and x % 2 == 0:
            c = jitter(CLAW, 6)
    if face == 'down':
        c = jitter(FUR_DARK, 6)
    return c


def paint_feather(face, x, y, w, h):
    # Overlapping rows of feathers: a darker tip at the bottom of every 3px row, staggered.
    row = y // 3
    off = (x + (row % 2) * 2) % 4
    c = jitter(FEATHER, 8)
    if y % 3 == 2 and off != 0:
        c = mix(c, FEATHER_DARK, 0.7)
    elif rng.random() < 0.08:
        c = mix(c, CREAM, 0.5)
    if face == 'down':
        c = mix(c, FEATHER_DARK, 0.4)
    return c


def paint_ruff(face, x, y, w, h):
    c = paint_feather(face, x, y, w, h)
    return mix(c, FUR, 0.25)


def paint_head(face, x, y, w, h):
    if face != 'north':
        c = paint_feather(face, x, y, w, h)
        return mix(c, (140, 102, 62), 0.3)
    # The face: a pale disk rimmed in dark feathers, two big orange eyes under a stern brow.
    cx, cy = (w - 1) / 2, (h - 1) / 2 + 0.5
    dist = ((x - cx) / (w / 2)) ** 2 + ((y - cy) / (h / 2)) ** 2
    if dist > 0.95:
        return jitter(FEATHER_DARK, 6)
    c = jitter(CREAM, 6)
    for ex in (cx - 2.5, cx + 2.5):
        ey = cy - 1
        d = max(abs(x - ex), abs(y - ey))
        if d <= 0.5:
            return (12, 8, 4)  # pupil
        if d <= 1.5:
            return EYE if (x, y) != (int(ex - 1), int(ey - 1)) else (255, 235, 150)
        if d <= 2.5 and y < ey - 1:
            return EYE_RIM  # brow
    if abs(x - cx) < 1 and y >= cy:
        return mix(c, FEATHER, 0.4)
    return c


def paint_tuft(face, x, y, w, h):
    return jitter(mix(FUR_DARK, FEATHER_DARK, 0.4), 8)


def paint_beak(face, x, y, w, h):
    c = jitter(BEAK, 6)
    if y >= h - 1 or face == 'down':
        c = mix(c, BEAK_DARK, 0.6)
    return c


def paint_beak_lower(face, x, y, w, h):
    return jitter(mix(BEAK, BEAK_DARK, 0.4), 6)


PAINTERS = {'fur': paint_fur, 'leg': paint_leg, 'feather': paint_feather, 'ruff': paint_ruff,
            'head': paint_head, 'tuft': paint_tuft, 'beak': paint_beak, 'beak_lower': paint_beak_lower}


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
                if w == 0 or h == 0:
                    continue
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
                'identifier': 'geometry.owlbear',
                'texture_width': TEX_W,
                'texture_height': TEX_H,
                'visible_bounds_width': 4,
                'visible_bounds_height': 3,
                'visible_bounds_offset': [0, 1, 0],
            },
            'bones': bones,
        }],
    }
    with open(GEO, 'w') as f:
        json.dump(geo, f, indent='\t')
        f.write('\n')
    pngio.write_png(PNG, TEX_W, TEX_H, pixels)
    print('wrote', os.path.relpath(GEO, ROOT), 'and', os.path.relpath(PNG, ROOT))


if __name__ == '__main__':
    main()
