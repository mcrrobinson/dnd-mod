"""Generates the kobolds' GeckoLib model and animations.

    python3 tools/kobold_model.py      (then python3 tools/kobold_skins.py for the textures)

Writes geo/entity/kobold.geo.json and animations/entity/kobold.animation.json. All four kobolds share the model;
the renderer hides the weapon bones a kobold doesn't carry (spear, knife, sling, shield) and the Dragonshield's
crest. Each cube face gets its own spot on the 64x64 texture (per-face UVs, packed in shelves); kobold_skins.py
paints the five skins over the same layout (LAYOUT below), by each cube's material.

A kobold is a small digitigrade biped: a snout, two horn nubs, a tail 0.6 blocks long. The top of the head (the
horn nubs) is at 15.2 px = 0.95 blocks, the same as its hitbox, so beside a player (1.8) its head reaches the
player's hip. The front is -Z (north), as in the other mod models.
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/dndclasses')
GEO = os.path.join(ASSETS, 'geo/entity/kobold.geo.json')
ANIM = os.path.join(ASSETS, 'animations/entity/kobold.animation.json')

TEX_W, TEX_H = 64, 64
#: The model's height in pixels (top of the horn nubs); 16 px = 1 block.
HEIGHT_PX = 15.2


def mirror(origin, size):
    """The same cube on the other side of the body (x flipped)."""
    return [-(origin[0] + size[0]), origin[1], origin[2]]


def leg(side):
    s = 1 if side == 'left' else -1

    def cube(origin, size, material):
        return (origin if s > 0 else mirror(origin, size), size, material)

    # Digitigrade: a thick thigh, a thin shin set back, a long clawed foot.
    return [('leg_' + side, 'root', [s * 1.5, 7, 0], [
        cube([0.5, 4, -1.25], [2, 3, 2.5], 'scale'),
        cube([0.75, 1, 0.25], [1.5, 3, 1.5], 'scale'),
        cube([0.5, 0, -2.25], [2, 1, 3], 'scale'),
        cube([0.75, 0, -2.75], [1.5, 0.5, 0.5], 'claw'),
    ])]


def arm(side):
    s = 1 if side == 'left' else -1

    def cube(origin, size, material):
        return (origin if s > 0 else mirror(origin, size), size, material)

    return [('arm_' + side, 'body', [s * 2.5, 11, 0], [
        cube([2.5, 7.25, -0.75], [1.5, 4, 1.5], 'scale'),
        cube([2.5, 6.25, -1], [1.5, 1, 1.75], 'hand'),
    ])]


# name, parent, pivot, [(origin, size, material)]
BONES = [
    ('root', None, [0, 0, 0], []),
    ('body', 'root', [0, 7, 0], [
        ([-2.5, 6.5, -1.5], [5, 5, 3.5], 'scale'),     # chest and belly
        ([-2, 7, -1.75], [4, 4, 0.25], 'belly'),       # paler belly scales
        ([-2.75, 6, -1.75], [5.5, 1.5, 4], 'cloth'),   # hide loincloth and belt
    ]),
    ('head', 'body', [0, 11.25, -0.5], [
        ([-2, 11.25, -3], [4, 3.5, 3.5], 'face'),
        ([-1.25, 11.25, -5.5], [2.5, 1.75, 2.5], 'snout'),
        ([-0.5, 12.5, -5.25], [1, 0.5, 1.5], 'scale'),  # ridge along the snout
        ([0.5, 14.25, -1.5], [0.75, 0.95, 1.75], 'horn'),
        ([-1.25, 14.25, -1.5], [0.75, 0.95, 1.75], 'horn'),
    ]),
    # The Dragonshield's crest: a red fin over the skull (hidden on the others).
    ('crest', 'head', [0, 14.75, -1], [
        ([-0.4, 14.75, -2.75], [0.8, 1.5, 4.25], 'crest'),
    ]),
    ('tail', 'body', [0, 7.5, 2], [
        ([-1, 6.5, 2], [2, 2, 4], 'scale'),
    ]),
    ('tail_tip', 'tail', [0, 7.25, 6], [
        ([-0.6, 6.4, 6], [1.2, 1.4, 5.5], 'scale'),
    ]),
] + arm('left') + arm('right') + [
    # Weapons, in the right hand (-x), drawn only on the kobold that carries one.
    ('spear', 'arm_right', [-3.25, 6.75, -0.25], [
        ([-3.5, 2, -0.5], [0.5, 12.5, 0.5], 'wood'),
        ([-3.75, 14.5, -0.75], [1, 2, 1], 'iron'),
    ]),
    ('knife', 'arm_right', [-3.25, 6.75, -0.25], [
        ([-3.5, 6.5, -2], [0.5, 0.5, 1.25], 'wood'),
        ([-3.5, 6.5, -4.5], [0.5, 0.5, 2.5], 'iron'),
    ]),
    ('sling', 'arm_right', [-3.25, 6.75, -0.25], [
        ([-3.4, 4, -0.35], [0.3, 2.5, 0.3], 'leather'),
        ([-3.75, 3.25, -0.7], [1, 0.75, 1], 'leather'),
    ]),
    # The Dragonshield's shield, on the left forearm, with a red dragon painted on it.
    ('shield', 'arm_left', [3.25, 7.5, -0.25], [
        ([4, 5.5, -2.5], [0.5, 5, 4.5], 'shield'),
    ]),
] + leg('left') + leg('right')

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


def layout():
    """The packed faces: a list of (bone, cube index, face, material, u, v, w, h), plus the geo bones."""
    packer = Packer()
    faces = []
    bones = []
    for name, parent, pivot, cubes in BONES:
        bone = {'name': name, 'pivot': pivot}
        if parent:
            bone['parent'] = parent
        out_cubes = []
        for index, (origin, size, material) in enumerate(cubes):
            uv = {}
            for face in FACES:
                w, h = face_size(face, size)
                u, v = packer.place(w, h)
                uv[face] = {'uv': [u, v], 'uv_size': [w, h]}
                faces.append((name, index, face, material, u, v, w, h))
            out_cubes.append({'origin': origin, 'size': size, 'uv': uv})
        if out_cubes:
            bone['cubes'] = out_cubes
        bones.append(bone)
    return faces, bones


def model_top():
    """The highest point of the rest pose without the crest and weapons, in pixels."""
    top = 0
    for name, parent, pivot, cubes in BONES:
        if name in ('crest', 'spear', 'knife', 'sling', 'shield'):
            continue
        for origin, size, material in cubes:
            top = max(top, origin[1] + size[1])
    return top


# ---- Animations ----

def sin(amp, speed=360, phase=0):
    """A Molang sine over the animation time, in degrees."""
    ph = f'+{phase}' if phase else ''
    return f'math.sin(query.anim_time*{speed}{ph})*{amp}'


ANIMATIONS = {
    'idle': {
        'loop': True, 'animation_length': 2,
        'bones': {
            'body': {'rotation': [sin(2, 180), 0, 0]},
            'head': {'rotation': [sin(4, 180, 60), sin(8, 90), 0]},
            'tail': {'rotation': [0, sin(12, 180), 0]},
            'tail_tip': {'rotation': [0, sin(14, 180, 40), 0]},
            'arm_left': {'rotation': [sin(3, 180), 0, 0]},
        },
    },
    'walk': {
        'loop': True, 'animation_length': 0.5,
        'bones': {
            'body': {'rotation': [6, sin(5, 720), 0], 'position': [0, sin(0.3, 1440), 0]},
            'tail': {'rotation': [0, sin(15, 720), 0]},
            'arm_left': {'rotation': [sin(25, 720), 0, 0]},
            'arm_right': {'rotation': ['-' + sin(15, 720), 0, 0]},
            'leg_left': {'rotation': ['-' + sin(30, 720), 0, 0]},
            'leg_right': {'rotation': [sin(30, 720), 0, 0]},
        },
    },
    # A quick forward jab with the spear or knife.
    'attack': {
        'animation_length': 0.35,
        'bones': {
            'body': {'rotation': {'0.0': [0, 0, 0], '0.1': [-8, 10, 0], '0.2': [12, -8, 0], '0.35': [0, 0, 0]}},
            'arm_right': {'rotation': {'0.0': [0, 0, 0], '0.1': [-20, 0, 0], '0.2': [-80, 0, 0],
                                       '0.35': [0, 0, 0]}},
        },
    },
    # Whirls the sling over its head twice and lets fly.
    'sling': {
        'animation_length': 0.5,
        'bones': {
            'arm_right': {'rotation': {'0.0': [0, 0, 0], '0.1': [-150, 0, 20], '0.2': [-150, 90, 20],
                                       '0.3': [-150, 180, 20], '0.4': [-60, 0, 0], '0.5': [0, 0, 0]}},
            'body': {'rotation': {'0.0': [0, 0, 0], '0.35': [-6, 0, 0], '0.45': [8, 0, 0], '0.5': [0, 0, 0]}},
        },
    },
    # Running scared: crouched low, arms up over the head, tail tucked.
    'cower': {
        'loop': True, 'animation_length': 0.4,
        'bones': {
            'body': {'rotation': [22, sin(6, 900), 0], 'position': [0, -1, 0]},
            'head': {'rotation': [20, 0, 0]},
            'arm_left': {'rotation': [-140, 0, -20]},
            'arm_right': {'rotation': [-140, 0, 20]},
            'tail': {'rotation': [-25, 0, 0]},
            'leg_left': {'rotation': ['-' + sin(40, 900), 0, 0]},
            'leg_right': {'rotation': [sin(40, 900), 0, 0]},
        },
    },
}


def main():
    faces, bones = layout()
    top = model_top()
    assert abs(top - HEIGHT_PX) < 0.01, f'model top {top} px, expected {HEIGHT_PX}'
    geo = {
        'format_version': '1.12.0',
        'minecraft:geometry': [{
            'description': {
                'identifier': 'geometry.kobold',
                'texture_width': TEX_W,
                'texture_height': TEX_H,
                'visible_bounds_width': 2,
                'visible_bounds_height': 2,
                'visible_bounds_offset': [0, 0.5, 0],
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
    print('wrote', os.path.relpath(GEO, ROOT), 'and', os.path.relpath(ANIM, ROOT),
          f'({len(faces)} faces, top {top} px = {top / 16:.3f} blocks)')


if __name__ == '__main__':
    main()
