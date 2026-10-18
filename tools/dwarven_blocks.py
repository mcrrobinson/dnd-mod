"""Writes the resources of the Dwarven Fortresses card's one-off blocks (Building Blocks epic).

    python3 tools/dwarven_blocks.py

The card's block families (Chiseled Runic Deepslate Bricks, Forge Bricks, Gold-Veined Pillar) come from
tools/block_families.py; this script covers the blocks that aren't a material with shapes: the Clan Crest,
Ancestor Statue and Ale Keg Wall (horizontal facing), the Treasury Coin Pile (layers, like snow), the Mithral
Lattice (a pane) and the Waystone (two blocks tall). For each it writes the blockstate, block and item models,
loot table, recipe, mineable tag entry and lang names. Textures come from tools/dwarven_block_textures.py.
Safe to rerun.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import block_families as bf  # noqa: E402
from block_families import ref, block_model, blockstate, item_model, variant, loot, shaped, stonecut  # noqa: E402

FACING_Y = {'north': 0, 'east': 90, 'south': 180, 'west': 270}

CLANS = ['Ironfist', 'Stonehelm', 'Deepdelve', 'Goldvein', 'Hammerfall', 'Emberforge', 'Granitebrow',
         'Anvilheart', 'Coppervault', 'Blackanvil', 'Runecarver', 'Frostbeard', 'Brightaxe', 'Thunderpeak',
         'Silverhelm', 'Cragborn']

LANG = {
    'block.dndclasses.clan_crest': 'Clan Crest',
    'block.dndclasses.ancestor_statue': 'Ancestor Statue',
    'block.dndclasses.ale_keg_wall': 'Ale Keg Wall',
    'block.dndclasses.treasury_coin_pile': 'Treasury Coin Pile',
    'block.dndclasses.mithral_lattice': 'Mithral Lattice',
    'block.dndclasses.waystone': 'Waystone',
    'block.dndclasses.waystone.hold': 'Here stands %s Hold',
    'block.dndclasses.waystone.standing': 'Your standing with the %s: %s',
}
for i, clan in enumerate(CLANS):
    LANG[f'block.dndclasses.waystone.clan.{i}'] = clan


def facing_block(name, side, top=None):
    """A cube with a front face, turned to face the player (like a furnace)."""
    block_model(name, 'minecraft:block/orientable', {'front': name, 'side': side, 'top': top or side})
    blockstate(name, {'variants': {f'facing={f}': variant(name, y=y) for f, y in FACING_Y.items()}})
    item_model(name, ref('block/' + name))
    loot(name)


def coin_pile():
    name = 'treasury_coin_pile'
    variants = {}
    for layers in range(1, 9):
        h = 2 * layers
        model = f'{name}_height{h}' if layers < 8 else f'{name}_block'
        faces = {
            'down': {'texture': '#top', 'cullface': 'down'},
            'up': {'texture': '#top'},
            'north': {'uv': [0, 16 - h, 16, 16], 'texture': '#side', 'cullface': 'north'},
            'south': {'uv': [0, 16 - h, 16, 16], 'texture': '#side', 'cullface': 'south'},
            'west': {'uv': [0, 16 - h, 16, 16], 'texture': '#side', 'cullface': 'west'},
            'east': {'uv': [0, 16 - h, 16, 16], 'texture': '#side', 'cullface': 'east'},
        }
        if layers == 8:
            faces['up']['cullface'] = 'up'
        bf.write(os.path.join(bf.ASSETS, 'models/block', model + '.json'), {
            'parent': 'minecraft:block/block',
            'textures': {'particle': ref('block/' + name), 'top': ref('block/' + name),
                         'side': ref(f'block/{name}_side')},
            'elements': [{'from': [0, 0, 0], 'to': [16, h, 16], 'faces': faces}],
        })
        variants[f'layers={layers}'] = variant(model)
    blockstate(name, {'variants': variants})
    item_model(name, ref(f'block/{name}_height2'))
    # Gold nuggets, not the block: one to three per layer.
    functions = [{'function': 'minecraft:set_count', 'add': False,
                  'count': {'type': 'minecraft:uniform', 'min': float(layers), 'max': float(3 * layers)},
                  'conditions': [{'condition': 'minecraft:block_state_property', 'block': ref(name),
                                  'properties': {'layers': str(layers)}}]}
                 for layers in range(1, 9)]
    functions.append({'function': 'minecraft:explosion_decay'})
    bf.write(os.path.join(bf.DATA, bf.NS, 'loot_tables/blocks', name + '.json'), {
        'type': 'minecraft:block',
        'pools': [{'bonus_rolls': 0.0, 'rolls': 1.0, 'entries': [
            {'type': 'minecraft:item', 'name': 'minecraft:gold_nugget', 'functions': functions}]}],
    })


def lattice():
    name = 'mithral_lattice'
    textures = {'pane': name, 'edge': name + '_edge'}
    for suffix in ('post', 'side', 'side_alt', 'noside', 'noside_alt'):
        block_model(f'{name}_{suffix}', f'minecraft:block/template_glass_pane_{suffix}', textures)
    m = lambda suffix, y=0: variant(f'{name}_{suffix}', y=y)  # noqa: E731
    blockstate(name, {'multipart': [
        {'apply': m('post')},
        {'when': {'north': 'true'}, 'apply': m('side')},
        {'when': {'east': 'true'}, 'apply': m('side', 90)},
        {'when': {'south': 'true'}, 'apply': m('side_alt')},
        {'when': {'west': 'true'}, 'apply': m('side_alt', 90)},
        {'when': {'north': 'false'}, 'apply': m('noside')},
        {'when': {'east': 'false'}, 'apply': m('noside_alt')},
        {'when': {'south': 'false'}, 'apply': m('noside_alt', 90)},
        {'when': {'west': 'false'}, 'apply': m('noside', 270)},
    ]})
    bf.write(os.path.join(bf.ASSETS, 'models/item', name + '.json'),
             {'parent': 'minecraft:item/generated', 'textures': {'layer0': ref('block/' + name)}})
    loot(name)


def waystone():
    """A standing stone two blocks tall, narrower than a block, with its runes on the front (north) face."""
    name = 'waystone'
    for half, top in (('lower', 16), ('upper', 14)):
        model = f'{name}_{half}'
        up = {'uv': [3, 4, 13, 12], 'texture': '#top'}
        if half == 'lower':
            up = None
        faces = {
            'north': {'uv': [3, 16 - top, 13, 16], 'texture': '#front'},
            'south': {'uv': [3, 16 - top, 13, 16], 'texture': '#side'},
            'east': {'uv': [4, 16 - top, 12, 16], 'texture': '#side'},
            'west': {'uv': [4, 16 - top, 12, 16], 'texture': '#side'},
        }
        if half == 'lower':
            faces['down'] = {'uv': [3, 4, 13, 12], 'texture': '#top', 'cullface': 'down'}
        else:
            faces['up'] = up
        bf.write(os.path.join(bf.ASSETS, 'models/block', model + '.json'), {
            'parent': 'minecraft:block/block',
            'textures': {'particle': ref(f'block/{name}_side'), 'front': ref(f'block/{model}'),
                         'side': ref(f'block/{name}_side'), 'top': ref(f'block/{name}_side')},
            'elements': [{'from': [3, 0, 4], 'to': [13, top, 12], 'faces': faces}],
        })
    variants = {}
    for f, y in FACING_Y.items():
        for half in ('lower', 'upper'):
            variants[f'facing={f},half={half}'] = variant(f'{name}_{half}', y=y)
    blockstate(name, {'variants': variants})
    item_model(name, ref(f'block/{name}_lower'))
    loot(name, {'conditions': [{'condition': 'minecraft:block_state_property', 'block': ref(name),
                                'properties': {'half': 'lower'}}]})


def recipes():
    shaped('clan_crest', ['G', '#'], {'G': 'minecraft:gold_ingot', '#': ref('chiseled_runic_deepslate_bricks')},
           ref('clan_crest'), 1)
    shaped('ale_keg_wall', ['#', '#', '#'], {'#': 'minecraft:barrel'}, ref('ale_keg_wall'), 3)
    shaped('mithral_lattice', ['###', '#D#', '###'], {'#': 'minecraft:iron_bars', 'D': 'minecraft:diamond'},
           ref('mithral_lattice'), 8)
    shaped('waystone', ['#', 'G', '#'], {'#': ref('runic_deepslate_bricks'), 'G': 'minecraft:glowstone_dust'},
           ref('waystone'), 1)
    stonecut('ancestor_statue_from_polished_deepslate_stonecutting', 'minecraft:polished_deepslate',
             ref('ancestor_statue'), 1)
    stonecut('chiseled_runic_deepslate_bricks_from_runic_deepslate_bricks_stonecutting',
             ref('runic_deepslate_bricks'), ref('chiseled_runic_deepslate_bricks'), 1)


def tags():
    bf.add_to_tag('blocks', 'mineable/pickaxe', [ref(b) for b in ('clan_crest', 'ancestor_statue', 'mithral_lattice',
                                                                  'waystone')])
    bf.add_to_tag('blocks', 'mineable/axe', [ref('ale_keg_wall')])
    bf.add_to_tag('blocks', 'mineable/shovel', [ref('treasury_coin_pile')])


def lang():
    """Adds or updates LANG in en_us.json, editing the text in place like block_families.gen_lang."""
    path = os.path.join(bf.ASSETS, 'lang/en_us.json')
    with open(path) as fh:
        text = fh.read()
    current = json.loads(text)
    lines = text.rstrip().split('\n')
    for key, value in LANG.items():
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
    facing_block('clan_crest', 'runic_deepslate_bricks')
    facing_block('ancestor_statue', 'ancestor_statue_side')
    facing_block('ale_keg_wall', 'ale_keg_wall_side')
    coin_pile()
    lattice()
    waystone()
    recipes()
    tags()
    lang()
    print('dwarven blocks written')


if __name__ == '__main__':
    main()
