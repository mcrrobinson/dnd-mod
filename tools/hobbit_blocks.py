"""Writes the resources of the Hobbit Villages' one-off building blocks (Building Blocks epic).

    python3 tools/hobbit_blocks.py

The materials with shapes (thatch, wattle and daub, ...) are block families, written by tools/block_families.py.
This script writes the rest, registered in classes/Blocks/hobbit/HobbitBlocks.java:
- larder_shelf: a barrel-like container facing the player (front, side and top textures)
- ale_cask: a barrel on its side with a tap in its head (front, back, side and top textures)
- hanging_herbs, onion_string: cross models that hang from a ceiling
and the extra recipes for the families (thatch from a hay bale, mossy thatch), mineable tags and lang entries.
Textures come from tools/hobbit_block_textures.py. Safe to rerun.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import block_families as bf  # noqa: E402

ref = bf.ref
write = bf.write
TURN = {'north': 0, 'east': 90, 'south': 180, 'west': 270}


def facing_block(name, textures, extra_props=None):
    """A cube facing north in its model, turned by its blockstate for each horizontal facing."""
    bf.block_model(name, 'minecraft:block/cube', textures)
    variants = {}
    for facing, y in TURN.items():
        if extra_props:
            for prop, values in extra_props.items():
                for v in values:
                    variants[f'facing={facing},{prop}={v}'] = bf.variant(name, y=y)
        else:
            variants[f'facing={facing}'] = bf.variant(name, y=y)
    bf.blockstate(name, {'variants': variants})
    bf.item_model(name, ref('block/' + name))


def hanging(name):
    bf.block_model(name, 'minecraft:block/cross', {'cross': name})
    bf.blockstate(name, {'variants': {'': bf.variant(name)}})
    write(os.path.join(bf.ASSETS, 'models/item', name + '.json'),
          {'parent': 'minecraft:item/generated', 'textures': {'layer0': ref('block/' + name)}})


def shapeless(name, ingredients, result, count, category='building'):
    recipe = {'type': 'minecraft:crafting_shapeless', 'category': category,
              'ingredients': [{'item': i} for i in ingredients],
              'result': {'count': count, 'item': result} if count != 1 else {'item': result}}
    write(os.path.join(bf.DATA, bf.NS, 'recipes', name + '.json'), recipe)


LANG = {
    'block.dndclasses.larder_shelf': 'Larder Shelf',
    'container.dndclasses.larder_shelf': 'Larder Shelf',
    'block.dndclasses.ale_cask': 'Ale Cask',
    'block.dndclasses.ale_cask.mugs': '%s mugs of ale left. Draw one with a glass bottle.',
    'block.dndclasses.ale_cask.dry': 'The cask is dry. It fills again in a few hours.',
    'block.dndclasses.hanging_herbs': 'Hanging Herbs',
    'block.dndclasses.onion_string': 'Onion String',
}


def lang():
    path = os.path.join(bf.ASSETS, 'lang/en_us.json')
    with open(path) as fh:
        text = fh.read()
    current = json.loads(text)
    lines = text.rstrip().split('\n')
    for key, value in LANG.items():
        if key in current:
            continue
        if not lines[-2].rstrip().endswith(','):
            lines[-2] = lines[-2].rstrip() + ','
        lines.insert(len(lines) - 1, f'    {json.dumps(key)}: {json.dumps(value, ensure_ascii=False)}')
    with open(path, 'w') as fh:
        fh.write('\n'.join(lines) + '\n')
    json.loads(open(path).read())


def main():
    facing_block('larder_shelf', {'particle': 'larder_shelf_side', 'north': 'larder_shelf_front',
                                  'south': 'larder_shelf_side', 'east': 'larder_shelf_side',
                                  'west': 'larder_shelf_side', 'up': 'larder_shelf_top', 'down': 'larder_shelf_top'})
    facing_block('ale_cask', {'particle': 'ale_cask_side', 'north': 'ale_cask_front', 'south': 'ale_cask_back',
                              'east': 'ale_cask_side', 'west': 'ale_cask_side', 'up': 'ale_cask_top',
                              'down': 'ale_cask_top'}, {'mugs': range(5)})
    hanging('hanging_herbs')
    hanging('onion_string')
    for block in ('larder_shelf', 'ale_cask', 'hanging_herbs', 'onion_string'):
        bf.loot(block)
    bf.add_to_tag('blocks', 'mineable/axe', [ref('larder_shelf'), ref('ale_cask')])
    bf.add_to_tag('blocks', 'mineable/hoe', [ref('hanging_herbs'), ref('onion_string')])
    # Turf is earth: grass, flowers and saplings grow on it (the smial hill's plants stand on it).
    bf.add_to_tag('blocks', 'dirt', [ref('smial_turf_wall')])

    bf.shaped('larder_shelf', [' S ', 'SBS', ' S '], {'S': 'minecraft:dark_oak_slab', 'B': 'minecraft:barrel'},
              ref('larder_shelf'), 1, 'misc')
    bf.shaped('ale_cask', ['IBI'], {'I': 'minecraft:iron_ingot', 'B': 'minecraft:barrel'}, ref('ale_cask'), 1, 'misc')
    bf.shaped('hanging_herbs', ['S', 'G', 'G'], {'S': 'minecraft:string', 'G': 'minecraft:grass'},
              ref('hanging_herbs'), 2, 'misc')
    bf.shaped('onion_string', ['S', 'P', 'P'], {'S': 'minecraft:string', 'P': 'minecraft:potato'},
              ref('onion_string'), 2, 'misc')
    shapeless('thatch_from_hay_block', ['minecraft:hay_block'], ref('thatch'), 4)
    shapeless('mossy_thatch', [ref('thatch'), 'minecraft:vine'], ref('mossy_thatch'), 1)
    shapeless('mossy_thatch_from_moss_block', [ref('thatch'), 'minecraft:moss_block'], ref('mossy_thatch'), 1)
    lang()
    print('hobbit blocks written')


if __name__ == '__main__':
    main()
