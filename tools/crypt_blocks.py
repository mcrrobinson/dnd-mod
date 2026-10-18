"""Writes the resources of the Crypt's one-off building blocks (Building Blocks: Crypt dungeons).

    python3 tools/crypt_blocks.py

The blocks are registered in CryptBlocks.java. For each one this writes the blockstate, block and item models,
loot table and any recipe, and adds the mineable tag and lang entries. Ossuary Bricks, the Crypt's material with
shapes, is a family in tools/block_families.py instead. The textures are drawn by tools/crypt_block_textures.py.
Safe to rerun: it only touches these blocks' own files and entries.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from block_families import (ASSETS, DATA, NS, add_to_tag, blockstate, item_model, loot, ref, shaped,  # noqa: E402
                            stonecut, write)

TITLES = {
    'funerary_relief': 'Funerary Relief',
    'burial_niche_wall': 'Burial Niche Wall',
    'sarcophagus': 'Sarcophagus',
    'grave_candle_cluster': 'Grave Candle Cluster',
    'lich_sigil_floor': 'Lich Sigil Floor',
    'grave_moss': 'Grave Moss',
    'grave_moss_carpet': 'Grave Moss Carpet',
    'phylactery_pedestal': 'Phylactery Pedestal',
}
PICKAXE = ['funerary_relief', 'burial_niche_wall', 'sarcophagus', 'lich_sigil_floor', 'phylactery_pedestal']
HOE = ['grave_moss', 'grave_moss_carpet']
LANG = {
    'block.dndclasses.sarcophagus.sleeps': 'The dead within sleep on, untroubled by you.',
}
# Model y rotation of a model whose front (or head) faces north
Y = {'north': 0, 'east': 90, 'south': 180, 'west': 270}


def tex(name):
    return name if ':' in name else ref('block/' + name)


def model(name, obj):
    obj = dict(obj)
    if 'textures' in obj:
        obj['textures'] = {k: tex(v) for k, v in obj['textures'].items()}
    write(os.path.join(ASSETS, 'models/block', name + '.json'), obj)


def box(frm, to, faces):
    """An element; faces maps a direction to (texture key, cullface or None[, uv])."""
    out = {}
    for d, spec in faces.items():
        face = {'texture': '#' + spec[0]}
        if len(spec) > 2 and spec[2]:
            face['uv'] = spec[2]
        if spec[1]:
            face['cullface'] = spec[1]
        out[d] = face
    return {'from': frm, 'to': to, 'faces': out}


def facing_states(name, extra=''):
    return {'variants': {f'facing={d}{extra}': ({'model': ref('block/' + name), 'y': y} if y else
                                                {'model': ref('block/' + name)}) for d, y in Y.items()}}


# --- blocks ------------------------------------------------------------------------------------------------

def funerary_relief():
    model('funerary_relief', {'parent': 'minecraft:block/orientable',
                              'textures': {'front': 'funerary_relief', 'side': 'minecraft:block/stone_bricks',
                                           'top': 'minecraft:block/stone_bricks'}})
    blockstate('funerary_relief', facing_states('funerary_relief'))


def burial_niche_wall():
    """A full block with a recess in its north face (2 px frame at the sides, 3 below, 4 above, 6 deep)
    holding a wrapped body: head at the west end, lying on the recess floor."""
    o, f, i, s = 'outside', 'front', 'inside', 'shroud'
    elements = [
        # back wall of the block, behind the recess
        box([0, 0, 6], [16, 16, 16], {'north': (i, None), 'south': (o, 'south'), 'east': (o, 'east'),
                                      'west': (o, 'west'), 'up': (o, 'up'), 'down': (o, 'down')}),
        # sill, lintel and jambs round the recess
        box([0, 0, 0], [16, 3, 6], {'north': (f, 'north'), 'up': (i, None), 'east': (o, 'east'),
                                    'west': (o, 'west'), 'down': (o, 'down')}),
        box([0, 12, 0], [16, 16, 6], {'north': (f, 'north'), 'down': (i, None), 'east': (o, 'east'),
                                      'west': (o, 'west'), 'up': (o, 'up')}),
        box([0, 3, 0], [2, 12, 6], {'north': (f, 'north'), 'east': (i, None), 'west': (o, 'west')}),
        box([14, 3, 0], [16, 12, 6], {'north': (f, 'north'), 'west': (i, None), 'east': (o, 'east')}),
        # the body: a long bundle, and the head a little higher at the west end
        box([5, 3, 1], [14, 6, 5], {'north': (s, None, [5, 10, 14, 13]), 'up': (s, None, [5, 1, 14, 5]),
                                    'east': (s, None, [11, 10, 15, 13]), 'west': (s, None, [11, 10, 15, 13])}),
        box([2, 3, 1.5], [5, 7, 4.5], {'north': (s, None, [1, 9, 4, 13]), 'up': (s, None, [1, 1, 4, 4]),
                                       'east': (s, None, [11, 9, 14, 13]), 'west': (s, None, [11, 9, 14, 13])}),
    ]
    model('burial_niche_wall', {'parent': 'minecraft:block/block',
                                'textures': {'particle': 'ossuary_bricks', o: 'ossuary_bricks',
                                             f: 'burial_niche_wall', i: 'burial_niche_wall_inside',
                                             s: 'burial_niche_wall_shroud'},
                                'elements': elements})
    blockstate('burial_niche_wall', facing_states('burial_niche_wall'))


def sarcophagus():
    """Two models for the halves of a coffin lying north-south with its head to the north (facing=north):
    a plinth, the coffin body (inset 1 px) and an overhanging lid with the effigy on top."""
    for part, outer, joint in (('head', 'north', 'south'), ('foot', 'south', 'north')):
        z0, z1 = (0, 16)
        bz0, bz1 = (1, 16) if part == 'head' else (0, 15)
        top = f'sarcophagus_{part}_top'
        elements = [
            box([0, 0, z0], [16, 2, z1], {outer: ('end', outer), 'east': ('side', 'east'), 'west': ('side', 'west'),
                                          'down': ('bottom', 'down'), 'up': ('bottom', None)}),
            box([1, 2, bz0], [15, 11, bz1], {outer: ('end', None), 'east': ('side', None), 'west': ('side', None)}),
            box([0, 11, z0], [16, 14, z1], {outer: ('end', None), 'east': ('side', None), 'west': ('side', None),
                                            'down': ('bottom', None), 'up': ('top', None)}),
        ]
        model(f'sarcophagus_{part}', {'parent': 'minecraft:block/block',
                                      'textures': {'particle': 'sarcophagus_side', 'side': 'sarcophagus_side',
                                                   'end': 'sarcophagus_end', 'top': top,
                                                   'bottom': 'minecraft:block/polished_deepslate'},
                                      'elements': elements})
    variants = {}
    for part in ('head', 'foot'):
        for d, y in Y.items():
            v = {'model': ref(f'block/sarcophagus_{part}')}
            if y:
                v['y'] = y
            variants[f'facing={d},part={part}'] = v
    blockstate('sarcophagus', {'variants': variants})


# The five candle stubs: (x, z) of the 2x2 stub's corner and its height in pixels. GraveCandleClusterBlock.WICKS
# has the flame positions (the middle of each stub, just over its wick).
STUBS = [(3, 4, 7), (6, 8, 5), (9, 3, 8), (10, 10, 6), (4, 10, 4)]


def grave_candle_cluster():
    elements = [box([0, 0.25, 0], [16, 0.25, 16], {'up': ('wax', None), 'down': ('wax', None)})]
    for x, z, h in STUBS:
        # each stub shows a different 2-px strip of the stub texture, its top in the drippy rows
        u = (x * 3) % 14
        side = [u, 16 - h, u + 2, 16]
        elements.append(box([x, 0, z], [x + 2, h, z + 2], {
            'north': ('stub', None, side), 'south': ('stub', None, side), 'east': ('stub', None, side),
            'west': ('stub', None, side), 'up': ('stub', None, [14, 0, 16, 2])}))
        elements.append(box([x + 0.75, h, z + 0.75], [x + 1.25, h + 1, z + 1.25], {
            'north': ('stub', None, [14, 3, 15, 4]), 'south': ('stub', None, [14, 3, 15, 4]),
            'east': ('stub', None, [14, 3, 15, 4]), 'west': ('stub', None, [14, 3, 15, 4]),
            'up': ('stub', None, [14, 3, 15, 4])}))
    model('grave_candle_cluster', {'parent': 'minecraft:block/block', 'ambientocclusion': False,
                                   'textures': {'particle': 'grave_candle_cluster_stub',
                                                'wax': 'grave_candle_cluster', 'stub': 'grave_candle_cluster_stub'},
                                   'elements': elements})
    blockstate('grave_candle_cluster', {'variants': {'': {'model': ref('block/grave_candle_cluster')}}})


def lich_sigil_floor():
    model('lich_sigil_floor', {'parent': 'minecraft:block/cube_bottom_top',
                               'textures': {'top': 'lich_sigil_floor', 'side': 'lich_sigil_floor_side',
                                            'bottom': 'lich_sigil_floor_side'}})
    blockstate('lich_sigil_floor', {'variants': {'': {'model': ref('block/lich_sigil_floor')}}})


def grave_moss():
    model('grave_moss', {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'grave_moss'}})
    blockstate('grave_moss', {'variants': {'': {'model': ref('block/grave_moss')}}})
    model('grave_moss_carpet', {'parent': 'minecraft:block/carpet', 'textures': {'wool': 'grave_moss'}})
    blockstate('grave_moss_carpet', {'variants': {'': {'model': ref('block/grave_moss_carpet')}}})


def phylactery_pedestal():
    elements = [
        box([1, 0, 1], [15, 3, 15], {d: ('side', None) for d in ('north', 'south', 'east', 'west')}
            | {'up': ('stone', None), 'down': ('stone', 'down')}),
        box([3, 3, 3], [13, 13, 13], {d: ('side', None) for d in ('north', 'south', 'east', 'west')}),
        box([2, 13, 2], [14, 16, 14], {d: ('side', None) for d in ('north', 'south', 'east', 'west')}
            | {'up': ('top', 'up'), 'down': ('stone', None)}),
    ]
    model('phylactery_pedestal', {'parent': 'minecraft:block/block',
                                  'textures': {'particle': 'phylactery_pedestal_side',
                                               'side': 'phylactery_pedestal_side', 'top': 'phylactery_pedestal_top',
                                               'stone': 'minecraft:block/polished_blackstone'},
                                  'elements': elements})
    blockstate('phylactery_pedestal', {'variants': {'': {'model': ref('block/phylactery_pedestal')}}})


# --- loot, recipes, tags, lang -----------------------------------------------------------------------------

def gen_loot():
    for name in TITLES:
        if name == 'sarcophagus':
            # one drop from the head, like a bed
            loot(name, {'conditions': [{'block': ref(name), 'condition': 'minecraft:block_state_property',
                                        'properties': {'part': 'head'}}]})
        else:
            loot(name)


def shapeless(name, ingredients, result, count=1):
    write(os.path.join(DATA, NS, 'recipes', name + '.json'), {
        'type': 'minecraft:crafting_shapeless', 'category': 'building',
        'ingredients': [{'item': i} for i in ingredients],
        'result': {'count': count, 'item': result} if count != 1 else {'item': result}})


def gen_recipes():
    ossuary = ref('ossuary_bricks')
    stonecut('funerary_relief_from_ossuary_bricks_stonecutting', ossuary, ref('funerary_relief'), 1)
    stonecut('burial_niche_wall_from_ossuary_bricks_stonecutting', ossuary, ref('burial_niche_wall'), 1)
    shaped('sarcophagus', ['#B#', '###'], {'#': 'minecraft:polished_deepslate', 'B': 'minecraft:bone_block'},
           ref('sarcophagus'), 1, 'decorations')
    shapeless('grave_candle_cluster', ['minecraft:candle', 'minecraft:candle', 'minecraft:candle'],
              ref('grave_candle_cluster'))
    shapeless('grave_moss', ['minecraft:moss_block', 'minecraft:bone_meal'], ref('grave_moss'))
    shaped('grave_moss_carpet', ['##'], {'#': ref('grave_moss')}, ref('grave_moss_carpet'), 3, 'decorations')


def gen_tags():
    add_to_tag('blocks', 'mineable/pickaxe', [ref(n) for n in PICKAXE])
    add_to_tag('blocks', 'mineable/hoe', [ref(n) for n in HOE])


def item_models():
    for name in TITLES:
        parent = {'sarcophagus': 'sarcophagus_head'}.get(name, name)
        item_model(name, ref('block/' + parent))


def gen_lang():
    """Same in-place edit as block_families.gen_lang: changed lines rewritten, new keys appended."""
    entries = {f'block.{NS}.{n}': t for n, t in TITLES.items()}
    entries.update(LANG)
    path = os.path.join(ASSETS, 'lang/en_us.json')
    with open(path) as fh:
        text = fh.read()
    current = json.loads(text)
    lines = text.rstrip().split('\n')
    for key, value in entries.items():
        line = f'    {json.dumps(key)}: {json.dumps(value, ensure_ascii=False)}'
        if key in current:
            if current[key] != value:
                for i, old in enumerate(lines):
                    if old.strip().startswith(json.dumps(key) + ':'):
                        lines[i] = line + (',' if old.rstrip().endswith(',') else '')
        else:
            if not lines[-2].rstrip().endswith(','):
                lines[-2] = lines[-2].rstrip() + ','
            lines.insert(len(lines) - 1, line)
    with open(path, 'w') as fh:
        fh.write('\n'.join(lines) + '\n')
    json.loads(open(path).read())


def main():
    funerary_relief()
    burial_niche_wall()
    sarcophagus()
    grave_candle_cluster()
    lich_sigil_floor()
    grave_moss()
    phylactery_pedestal()
    item_models()
    gen_loot()
    gen_recipes()
    gen_tags()
    gen_lang()
    print(f'{len(TITLES)} crypt blocks written')


if __name__ == '__main__':
    main()
