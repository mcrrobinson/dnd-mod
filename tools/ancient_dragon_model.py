"""Builds the Ancient dragon model from the Lightning Chaser's (design 10, section 5.3).

    python3 tools/ancient_dragon_model.py

Reads geo/lightning_chaser.geo.json and writes geo/ancient_dragon.geo.json with the same 98 bones, pivots
and rotations, so lightning_chaser.animation.json (copied to ancient_dragon.animation.json, since
DragonPartLayout loads animations by model name) and the hit-shape groups work unchanged. Only cubes
change:

- trunk (front, back): width and height x1.8, length x1.3
- neck x1.5 thick; head and jaw x1.4 (x1.15 long); tail x1.5 at the base tapering to x1.1 at the tip
- legs x1.6 thick, feet x1.4; wing arms and fingers x1.3 thick
- spikes move out with the surface they sit on
- new cubes: two swept-back horn pairs and cheek frills on the head, dorsal spines along neck and
  trunk, and a tail blade on tail4

Box-UV cubes are rewritten as per-face UV over the same texture rectangles, so a thicker cube stretches
its old texture rather than reading its neighbours'. New cubes map every face to small patches that
tools/ancient_dragon_texture.py paints (PATCHES below). The renderer draws the result at 2.5x.
"""
import copy
import json
import math
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/dndclasses')
SOURCE = os.path.join(ASSETS, 'geo/lightning_chaser.geo.json')
OUT = os.path.join(ASSETS, 'geo/ancient_dragon.geo.json')
ANIM_SOURCE = os.path.join(ASSETS, 'animations/lightning_chaser.animation.json')
ANIM_OUT = os.path.join(ASSETS, 'animations/ancient_dragon.animation.json')

# Texture patches for the new cubes: (u, v, size). Unused space on the 512x512 sheet.
PATCHES = {
    'horn': (320, 320, 16),
    'spine': (340, 320, 16),
    'frill': (360, 320, 16),
    'blade': (380, 320, 16),
}

HEAD_ANCHOR = (0.0, 26.75, -54.5)

# bone -> (kx, ky, kz, anchor) ; anchor None means each cube's own centre
SCALES = {
    'front': (1.8, 1.8, 1.3, None),
    'back': (1.8, 1.8, 1.3, None),
    'neck1': (1.5, 1.5, 1.0, None),
    'neck2': (1.5, 1.5, 1.0, None),
    'neck3': (1.5, 1.5, 1.0, None),
    'head': (1.4, 1.4, 1.15, HEAD_ANCHOR),
    'blink': (1.4, 1.4, 1.15, HEAD_ANCHOR),
    'jaw': (1.4, 1.4, 1.15, HEAD_ANCHOR),
    'tail1': (1.5, 1.5, 1.0, None),
    'tail2': (1.37, 1.37, 1.0, None),
    'tail3': (1.23, 1.23, 1.0, None),
    'tail4': (1.1, 1.1, 1.0, None),
}
for side in ('left', 'right'):
    for bone in ('leg', 'leg_mid', 'leg_ground'):
        SCALES[f'{bone}_{side}'] = (1.6, 1.0, 1.6, None)
    SCALES[f'feet_{side}'] = (1.4, 1.2, 1.4, None)
    for bone in ('wing', 'shoulder_arm', 'fingers', 'wing_finger1', 'wing_finger2', 'wing_finger3',
                 'wing_finger4', 'wing_finger5'):
        SCALES[f'{bone}_{side}'] = (1.0, 1.3, 1.3, 'pivot')


def box_faces(uv, size, mirror):
    """GeckoLib's box UV rectangles (BakedModelFactory.buildQuad) as per-face UV."""
    u, v = uv
    sx, sy, sz = (math.floor(s) for s in size)
    faces = {
        'west': {'uv': [u + sz + sx, v + sz], 'uv_size': [sz, sy]},
        'east': {'uv': [u, v + sz], 'uv_size': [sz, sy]},
        'north': {'uv': [u + sz, v + sz], 'uv_size': [sx, sy]},
        'south': {'uv': [u + sz + sx + sz, v + sz], 'uv_size': [sx, sy]},
        'up': {'uv': [u + sz, v], 'uv_size': [sx, sz]},
        'down': {'uv': [u + sz + sx, v + sz], 'uv_size': [sx, -sz]},
    }
    if mirror:
        # Per-face UV on a mirrored cube draws "up" on the bottom quad and "down" on the top one
        faces['up'], faces['down'] = faces['down'], faces['up']
    return faces


def patch_faces(name):
    u, v, n = PATCHES[name]
    return {face: {'uv': [u, v], 'uv_size': [n, n]} for face in ('north', 'south', 'east', 'west', 'up', 'down')}


def centre(cube):
    return [cube['origin'][i] + cube['size'][i] / 2 for i in range(3)]


def scale_cube(cube, k, anchor):
    if anchor is None:
        anchor = centre(cube)
    out = dict(cube)
    if isinstance(cube.get('uv'), list):
        out['uv'] = box_faces(cube['uv'], cube['size'], cube.get('mirror'))
    out['origin'] = [round(anchor[i] + k[i] * (cube['origin'][i] - anchor[i]), 4) for i in range(3)]
    out['size'] = [round(cube['size'][i] * k[i], 4) for i in range(3)]
    if 'pivot' in cube:
        out['pivot'] = [round(anchor[i] + k[i] * (cube['pivot'][i] - anchor[i]), 4) for i in range(3)]
    return out


def cube(origin, size, patch, rotation=None, pivot=None):
    c = {'origin': [round(x, 4) for x in origin], 'size': size, 'uv': patch_faces(patch)}
    if rotation:
        c['rotation'] = rotation
        c['pivot'] = [round(x, 4) for x in (pivot or origin)]
    return c


def main():
    with open(SOURCE) as f:
        model = json.load(f)
    geo = model['minecraft:geometry'][0]
    geo['description']['identifier'] = 'geometry.ancient_dragon'
    geo['description']['visible_bounds_width'] = 50
    geo['description']['visible_bounds_height'] = 30
    geo['description']['visible_bounds_offset'] = [0, 6, 0]
    bones = geo['bones']
    original = {b['name']: copy.deepcopy(b) for b in bones}

    for bone in bones:
        name = bone['name']
        if name in SCALES:
            kx, ky, kz, anchor = SCALES[name]
            new = []
            for c in bone.get('cubes', []):
                a = anchor
                if anchor == 'pivot':
                    a = c.get('pivot') if 'rotation' in c else None
                new.append(scale_cube(c, (kx, ky, kz), a))
            bone['cubes'] = new
        elif name.startswith('spike'):
            # Move the spike out with the surface of the cube it sits on
            parent = original[bone['parent']]
            if parent['name'] in SCALES and parent.get('cubes'):
                pc = parent['cubes'][0]
                ky = SCALES[parent['name']][1]
                grow = (ky - 1.0) * pc['size'][1] / 2.0
                for c in bone['cubes']:
                    up = centre(c)[1] > centre(pc)[1]
                    c['origin'][1] = round(c['origin'][1] + (grow if up else -grow), 4)
                    c['size'][1] = round(c['size'][1] * 1.3, 4)

    by_name = {b['name']: b for b in bones}

    # Horns and frills on the head: the head block after scaling
    head = by_name['head']['cubes'][0]
    hx0, hy0, hz0 = head['origin']
    hsx, hsy, hsz = head['size']
    top = hy0 + hsy
    back = hz0 + hsz
    extra = []
    for s in (-1, 1):
        # Great horns: from the top back corners, swept back and up
        x = (hsx / 2 - 2.0) * s
        base = [x - 1.25, top - 1.5, back - 3.0]
        extra.append(cube(base, [2.5, 2.5, 13], 'horn', [28, 14 * s, 0], [x, top - 0.25, back - 3.0]))
        # Lesser horns: lower and wider, along the cheeks
        x2 = (hsx / 2 - 0.5) * s
        base2 = [x2 - 0.75, hy0 + hsy * 0.45, back - 2.0]
        extra.append(cube(base2, [1.5, 1.5, 9], 'horn', [12, 30 * s, 0], [x2, hy0 + hsy * 0.45 + 0.75, back - 2.0]))
        # Cheek frills: thin plates behind the jaw
        fx = (hsx / 2) * s - (0.5 if s > 0 else -0.5)
        extra.append(cube([fx - 0.25, hy0 + 0.5, back - 4.0], [0.5, 5, 6], 'frill', [0, 20 * s, 0],
                          [fx, hy0 + 3.0, back - 4.0]))
    by_name['head']['cubes'].extend(extra)

    # Dorsal spines on the neck and trunk
    for bone_name, count in (('neck1', 2), ('neck2', 2), ('neck3', 2), ('front', 3), ('back', 3)):
        c0 = by_name[bone_name]['cubes'][0]
        ox, oy, oz = c0['origin']
        sx, sy, sz = c0['size']
        for i in range(count):
            z = oz + sz * (i + 0.5) / count
            height = 7.0 if bone_name in ('front', 'back') else 5.0
            by_name[bone_name]['cubes'].append(cube([-0.75, oy + sy - 1.0, z - 1.5], [1.5, height, 3.0], 'spine',
                                                    [-35, 0, 0], [0, oy + sy - 1.0, z]))

    # Tail blade on the tip
    t4 = by_name['tail4']['cubes'][0]
    tz = t4['origin'][2] + t4['size'][2]
    ty = t4['origin'][1] + t4['size'][1] / 2
    by_name['tail4']['cubes'].append(cube([-5, ty - 0.5, tz - 5], [10, 1, 8], 'blade', [0, 45, 0], [0, ty, tz - 1]))

    with open(OUT, 'w') as f:
        json.dump(model, f, indent=1)
    with open(ANIM_SOURCE) as f:
        anim = f.read()
    with open(ANIM_OUT, 'w') as f:
        f.write(anim)
    print('wrote', os.path.relpath(OUT, ROOT), 'and', os.path.relpath(ANIM_OUT, ROOT))


if __name__ == '__main__':
    main()
