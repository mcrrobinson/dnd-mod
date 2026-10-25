"""Writes the resources of the Underdark's natural blocks (UnderdarkBlocks.java, design 9 T1).

    python3 tools/underdark_blocks.py

Zurkhwood Planks and its shapes are a family in tools/block_families.py; this script covers the rest: Zurkhwood
Stem, Zurkhwood Cap, Nightlight Cap, Fungal Loam, Faerzress Stone, Bluecap and Trillimac. For each it writes the
blockstate, block and item models and loot table (drops itself), adds it to its tags (mineable, the vanilla
mushroom soil tag, the Dwarf's stonecunning tag) and adds its lang name, plus the five biomes' names. Textures come
from tools/underdark_block_textures.py. Safe to rerun.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from block_families import (ASSETS, DATA, NS, add_to_tag, axis_states, blockstate, item_model, loot, ref,  # noqa: E402
                            write)

NAMES = {
    'zurkhwood_stem': 'Zurkhwood Stem',
    'zurkhwood_cap': 'Zurkhwood Cap',
    'nightlight_cap': 'Nightlight Cap',
    'fungal_loam': 'Fungal Loam',
    'faerzress_stone': 'Faerzress Stone',
    'bluecap': 'Bluecap',
    'trillimac': 'Trillimac',
}
BIOMES = {
    'fungal_forest': 'Fungal Forest',
    'mycelial_caverns': 'Mycelial Caverns',
    'darklake': 'Darklake',
    'faerzress_hollows': 'Faerzress Hollows',
    'grey_reaches': 'Grey Reaches',
}


def model(name, obj):
    write(os.path.join(ASSETS, 'models/block', name + '.json'), obj)


def tex(name):
    return ref('block/' + name)


def cube(name):
    model(name, {'parent': 'minecraft:block/cube_all', 'textures': {'all': tex(name)}})
    blockstate(name, {'variants': {'': {'model': ref('block/' + name)}}})
    item_model(name, ref('block/' + name))


def stem():
    name = 'zurkhwood_stem'
    textures = {'end': tex(name + '_top'), 'side': tex(name)}
    model(name, {'parent': 'minecraft:block/cube_column', 'textures': textures})
    model(name + '_horizontal', {'parent': 'minecraft:block/cube_column_horizontal', 'textures': textures})
    blockstate(name, axis_states(name))
    item_model(name, ref('block/' + name))


def mushroom_block(name):
    """A huge-mushroom cap block: its skin on the faces that show, its underside on the others (vanilla's layout)."""
    model(name, {'parent': 'minecraft:block/template_single_face', 'textures': {'texture': tex(name)}})
    model(name + '_inside', {'parent': 'minecraft:block/template_single_face',
                             'textures': {'texture': tex(name + '_inside')}})
    model(name + '_inventory', {'parent': 'minecraft:block/cube_all', 'textures': {'all': tex(name)}})
    turns = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270},
             'down': {'x': 90}}
    parts = []
    for face, turn in turns.items():
        for showing, suffix in (('true', ''), ('false', '_inside')):
            apply = {'model': ref('block/' + name + suffix)}
            if turn:
                apply.update(turn)
                apply['uvlock'] = True
            parts.append({'when': {face: showing}, 'apply': apply})
    blockstate(name, {'multipart': parts})
    item_model(name, ref('block/' + name + '_inventory'))


def plant(name):
    model(name, {'parent': 'minecraft:block/cross', 'textures': {'cross': tex(name)}})
    blockstate(name, {'variants': {'': {'model': ref('block/' + name)}}})
    write(os.path.join(ASSETS, 'models/item', name + '.json'),
          {'parent': 'minecraft:item/generated', 'textures': {'layer0': tex(name)}})


def lang():
    entries = {f'block.{NS}.{k}': v for k, v in NAMES.items()}
    entries.update({f'biome.{NS}.{k}': v for k, v in BIOMES.items()})
    path = os.path.join(ASSETS, 'lang/en_us.json')
    with open(path) as fh:
        text = fh.read()
    current = json.loads(text)
    lines = text.rstrip().split('\n')
    for key, value in entries.items():
        if key in current:
            continue
        if not lines[-2].rstrip().endswith(','):
            lines[-2] = lines[-2].rstrip() + ','
        lines.insert(len(lines) - 1, f'    {json.dumps(key)}: {json.dumps(value, ensure_ascii=False)}')
    with open(path, 'w') as fh:
        fh.write('\n'.join(lines) + '\n')
    json.loads(open(path).read())


def mod_tag(kind, tag, values):
    path = os.path.join(DATA, NS, 'tags', kind, tag + '.json')
    with open(path) as fh:
        obj = json.load(fh)
    missing = [v for v in values if v not in obj['values']]
    if missing:
        obj['values'].extend(missing)
        write(path, obj)


def main():
    stem()
    mushroom_block('zurkhwood_cap')
    mushroom_block('nightlight_cap')
    cube('fungal_loam')
    cube('faerzress_stone')
    plant('bluecap')
    plant('trillimac')
    for name in NAMES:
        loot(name)
    add_to_tag('blocks', 'mineable/axe', [ref('zurkhwood_stem'), ref('zurkhwood_cap'), ref('nightlight_cap')])
    add_to_tag('blocks', 'mineable/shovel', [ref('fungal_loam')])
    add_to_tag('blocks', 'mineable/pickaxe', [ref('faerzress_stone')])
    # Vanilla mushrooms grow on Fungal Loam in any light, and huge-mushroom features (the zurkhwood) stand on it.
    add_to_tag('blocks', 'mushroom_grow_block', [ref('fungal_loam')])
    mod_tag('blocks', 'stonecunning', [ref('faerzress_stone')])
    lang()
    print(f'{len(NAMES)} Underdark blocks written')


if __name__ == '__main__':
    main()
