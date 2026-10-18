"""Writes the resources of Cogsdeep's one-off building blocks (CogsdeepBlocks.java).

    python3 tools/cogsdeep_blocks.py

The materials with shapes (Riveted Brass, Cut Riveted Brass, Gnomish Bricks) are families in
tools/block_families.py; this script covers the rest: Cogwork Panel, Gearwall, Pipework, the four Glowcrystals,
Clockface, Blueprint Table and Telescope Lens. For each it writes the blockstate, block and item models, loot
table (drops itself) and recipe, adds it to its mineable tag and adds its lang name. Textures come from
tools/cogsdeep_block_textures.py. Safe to rerun.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from block_families import (ASSETS, DATA, NS, add_to_tag, blockstate, item_model, loot, ref,  # noqa: E402
                            shaped, stonecut, variant, write)

GLOWCRYSTALS = {'red': 'red_dye', 'blue': 'blue_dye', 'green': 'green_dye', 'amber': 'orange_dye'}
FACINGS = {'north': 0, 'east': 90, 'south': 180, 'west': 270}


def model(name, obj):
    write(os.path.join(ASSETS, 'models/block', name + '.json'), obj)


def tex(name):
    return ref('block/' + name)


def cube(name, texture=None):
    model(name, {'parent': 'minecraft:block/cube_all', 'textures': {'all': tex(texture or name)}})
    blockstate(name, {'variants': {'': variant(name)}})
    item_model(name, ref('block/' + name))


def clockface():
    for hour in range(12):
        model(f'clockface_{hour}', {'parent': 'minecraft:block/orientable', 'textures': {
            'front': tex(f'clockface_{hour}'), 'side': tex('cut_riveted_brass'), 'top': tex('cut_riveted_brass')}})
    variants = {}
    for facing, y in FACINGS.items():
        for hour in range(12):
            variants[f'facing={facing},hour={hour}'] = variant(f'clockface_{hour}', y=y)
    blockstate('clockface', {'variants': variants})
    item_model('clockface', ref('block/clockface_2'))


def blueprint_table():
    model('blueprint_table', {'parent': 'minecraft:block/orientable_with_bottom', 'textures': {
        'top': tex('blueprint_table_top'), 'bottom': 'minecraft:block/spruce_planks',
        'side': tex('blueprint_table_side'), 'front': tex('blueprint_table_front'),
        'particle': tex('blueprint_table_side')}})
    blockstate('blueprint_table', {'variants': {f'facing={f}': variant('blueprint_table', y=y)
                                                for f, y in FACINGS.items()}})
    item_model('blueprint_table', ref('block/blueprint_table'))


SIX = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}


def telescope_lens():
    # A tube 10 pixels across along z, the lens on the north face; the blockstate turns it to its facing.
    ring = {'uv': [0, 3, 16, 13], 'texture': '#side'}
    model('telescope_lens', {'parent': 'minecraft:block/block', 'textures': {
        'particle': tex('telescope_lens'), 'side': tex('telescope_lens'), 'front': tex('telescope_lens_front'),
        'back': tex('telescope_lens_back')}, 'elements': [{'from': [3, 3, 0], 'to': [13, 13, 16], 'faces': {
            'north': {'uv': [3, 3, 13, 13], 'texture': '#front', 'cullface': 'north'},
            'south': {'uv': [3, 3, 13, 13], 'texture': '#back', 'cullface': 'south'},
            'east': dict(ring), 'west': dict(ring),
            'up': dict(ring, rotation=90), 'down': dict(ring, rotation=90)}}]})
    blockstate('telescope_lens', {'variants': {f'facing={f}': variant('telescope_lens', **r) for f, r in SIX.items()}})
    item_model('telescope_lens', ref('block/telescope_lens'))


def pipework():
    core = {'from': [5, 5, 5], 'to': [11, 11, 11], 'faces': {
        f: {'uv': [5, 5, 11, 11], 'texture': '#joint'} for f in ('north', 'south', 'east', 'west', 'up', 'down')}}

    def arm(z1, z2, cull):
        faces = {f: {'uv': [6, z1, 10, z2], 'texture': '#pipe'} for f in ('east', 'west', 'up', 'down')}
        faces[cull] = {'uv': [6, 6, 10, 10], 'texture': '#pipe', 'cullface': cull}
        return {'from': [6, 6, z1], 'to': [10, 10, z2], 'faces': faces}

    textures = {'particle': tex('pipework'), 'pipe': tex('pipework'), 'joint': tex('pipework_joint')}
    model('pipework_core', {'parent': 'minecraft:block/block', 'textures': textures, 'elements': [core]})
    model('pipework_side', {'parent': 'minecraft:block/block', 'textures': textures, 'elements': [arm(0, 5, 'north')]})
    # The item: a joint with a pipe through it, east to west.
    east_west = {'from': [0, 6, 6], 'to': [16, 10, 10], 'faces': {
        f: {'uv': [0, 6, 16, 10], 'texture': '#pipe'} for f in ('north', 'south', 'up', 'down')}}
    east_west['faces']['east'] = {'uv': [6, 6, 10, 10], 'texture': '#pipe'}
    east_west['faces']['west'] = {'uv': [6, 6, 10, 10], 'texture': '#pipe'}
    model('pipework_inventory', {'parent': 'minecraft:block/block', 'textures': textures,
                                 'elements': [core, east_west]})
    multipart = [{'apply': variant('pipework_core')}]
    for side, rot in SIX.items():
        multipart.append({'when': {side: 'true'}, 'apply': variant('pipework_side', uvlock=False, **rot)})
    blockstate('pipework', {'multipart': multipart})
    item_model('pipework', ref('block/pipework_inventory'))


def shapeless(name, ingredients, result, count=1):
    recipe = {'type': 'minecraft:crafting_shapeless', 'category': 'building',
              'ingredients': [{'item': i} for i in ingredients],
              'result': {'count': count, 'item': result} if count != 1 else {'item': result}}
    write(os.path.join(DATA, NS, 'recipes', name + '.json'), recipe)


def recipes():
    brass = ref('riveted_brass')
    shaped('cogwork_panel', [' # ', '#R#', ' # '], {'#': brass, 'R': 'minecraft:redstone'}, ref('cogwork_panel'), 4)
    shaped('gearwall', [' # ', '#I#', ' # '], {'#': brass, 'I': 'minecraft:iron_ingot'}, ref('gearwall'), 4)
    shaped('pipework', ['CNC'], {'C': 'minecraft:copper_ingot', 'N': 'minecraft:gold_nugget'}, ref('pipework'), 4)
    shaped('clockface', [' # ', '#C#', ' # '], {'#': brass, 'C': 'minecraft:clock'}, ref('clockface'), 1)
    for colour, dye in GLOWCRYSTALS.items():
        shapeless(f'glowcrystal_{colour}', ['minecraft:glowstone', brass, 'minecraft:' + dye],
                  ref(f'glowcrystal_{colour}'), 2)
    shapeless('blueprint_table', ['minecraft:crafting_table', 'minecraft:paper', 'minecraft:blue_dye'],
              ref('blueprint_table'))
    shapeless('telescope_lens', [brass, 'minecraft:glass_pane'], ref('telescope_lens'))
    stonecut('cut_riveted_brass_from_riveted_brass_stonecutting', brass, ref('cut_riveted_brass'), 1)


NAMES = {
    'cogwork_panel': 'Cogwork Panel',
    'gearwall': 'Gearwall',
    'pipework': 'Pipework',
    'glowcrystal_red': 'Red Glowcrystal',
    'glowcrystal_blue': 'Blue Glowcrystal',
    'glowcrystal_green': 'Green Glowcrystal',
    'glowcrystal_amber': 'Amber Glowcrystal',
    'clockface': 'Clockface',
    'blueprint_table': 'Blueprint Table',
    'telescope_lens': 'Telescope Lens',
}


def lang():
    entries = {f'block.{NS}.{k}': v for k, v in NAMES.items()}
    entries[f'container.{NS}.blueprint_table'] = 'Blueprint Table'
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


def main():
    for name in ['cogwork_panel', 'gearwall'] + [f'glowcrystal_{c}' for c in GLOWCRYSTALS]:
        cube(name)
    clockface()
    blueprint_table()
    telescope_lens()
    pipework()
    for name in NAMES:
        loot(name)
    recipes()
    add_to_tag('blocks', 'mineable/pickaxe', [ref(n) for n in NAMES if n != 'blueprint_table'])
    add_to_tag('blocks', 'mineable/axe', [ref('blueprint_table')])
    lang()
    print(f'{len(NAMES)} Cogsdeep blocks written')


if __name__ == '__main__':
    main()
