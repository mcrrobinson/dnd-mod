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
    'block.dndclasses.runic_column': 'Runic Column',
    'block.dndclasses.runic_corbel': 'Runic Corbel',
    'block.dndclasses.runic_deepslate_brick_vertical_slab': 'Runic Deepslate Brick Vertical Slab',
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


FACES = ('down', 'up', 'north', 'south', 'west', 'east')


def element(frm, to, textures, cull=True):
    """A box with every face; a face on the block's edge is culled against the neighbour there. {@code textures}
    maps a face name to a texture variable, with 'side' for any face it doesn't name."""
    edge = {'down': frm[1] == 0, 'up': to[1] == 16, 'north': frm[2] == 0, 'south': to[2] == 16,
            'west': frm[0] == 0, 'east': to[0] == 16}
    faces = {}
    for face in FACES:
        f = {'texture': '#' + textures.get(face, textures['side'])}
        if cull and edge[face]:
            f['cullface'] = face
        faces[face] = f
    return {'from': frm, 'to': to, 'faces': faces}


def shaped_model(model, textures, elements, particle):
    bf.write(os.path.join(bf.ASSETS, 'models/block', model + '.json'), {
        'parent': 'minecraft:block/block',
        'textures': dict({'particle': ref('block/' + particle)}, **{k: ref('block/' + v) for k, v in textures.items()}),
        'elements': elements,
    })


def column():
    """The Runic Column: a chamfered shaft, with a stepped foot at the bottom of a stack and a capital at the top."""
    name = 'runic_column'
    textures = {'shaft': name, 'end': name + '_top', 'cap': name + '_cap', 'cap_top': 'runic_deepslate_bricks'}
    shaft_tex = {'side': 'shaft', 'up': 'end', 'down': 'end'}
    cap_tex = {'side': 'cap', 'up': 'cap_top', 'down': 'cap_top'}

    def shaft(y1, y2):
        return [element([3, y1, 4], [13, y2, 12], shaft_tex), element([4, y1, 3], [12, y2, 13], shaft_tex)]

    foot = [element([1, 0, 1], [15, 3, 15], cap_tex), element([2, 3, 2], [14, 5, 14], cap_tex)]
    head = [element([2, 11, 2], [14, 13, 14], cap_tex), element([1, 13, 1], [15, 16, 15], cap_tex)]
    parts = {'single': foot + shaft(5, 11) + head, 'base': foot + shaft(5, 16), 'shaft': shaft(0, 16),
             'capital': shaft(0, 11) + head}
    for part, elements in parts.items():
        shaped_model(f'{name}_{part}', textures, elements, name)
    blockstate(name, {'variants': {f'part={part}': variant(f'{name}_{part}') for part in parts}})
    item_model(name, ref(f'block/{name}_single'))
    loot(name)


def corbel():
    """The Runic Corbel: a full plate on top stepping back to the wall in two steps (modelled facing north)."""
    name = 'runic_corbel'
    tex = {'side': 'side', 'up': 'top'}
    elements = [element([0, 11, 0], [16, 16, 16], tex), element([2, 6, 4], [14, 11, 16], tex),
                element([5, 0, 7], [11, 6, 16], tex)]
    shaped_model(name, {'side': name, 'top': 'runic_deepslate_bricks'}, elements, name)
    blockstate(name, {'variants': {f'facing={f}': variant(name, y=y) for f, y in FACING_Y.items()}})
    item_model(name, ref('block/' + name))
    loot(name)


def vertical_slab():
    """Half a runic brick on its edge, filling the back (south) half when facing north."""
    name = 'runic_deepslate_brick_vertical_slab'
    shaped_model(name, {'side': 'runic_deepslate_bricks'}, [element([0, 0, 8], [16, 16, 16], {'side': 'side'})],
                 'runic_deepslate_bricks')
    blockstate(name, {'variants': {f'facing={f}': variant(name, y=y, uvlock=True) for f, y in FACING_Y.items()}})
    item_model(name, ref('block/' + name))
    loot(name)


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
    for name, count in (('runic_column', 1), ('runic_corbel', 1), ('runic_deepslate_brick_vertical_slab', 2)):
        stonecut(f'{name}_from_runic_deepslate_bricks_stonecutting', ref('runic_deepslate_bricks'), ref(name), count)
    shaped('runic_deepslate_brick_vertical_slab', ['#', '#', '#'], {'#': ref('runic_deepslate_bricks')},
           ref('runic_deepslate_brick_vertical_slab'), 6)


def tags():
    bf.add_to_tag('blocks', 'mineable/pickaxe', [ref(b) for b in ('clan_crest', 'ancestor_statue', 'mithral_lattice',
                                                                  'waystone', 'runic_column', 'runic_corbel',
                                                                  'runic_deepslate_brick_vertical_slab')])
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
    column()
    corbel()
    vertical_slab()
    recipes()
    tags()
    lang()
    print('dwarven blocks written')


if __name__ == '__main__':
    main()
