"""Writes the resources of the Underdark's natural blocks (UnderdarkBlocks.java, design 9 T1).

    python3 tools/underdark_blocks.py

Zurkhwood Planks and its shapes are a family in tools/block_families.py; this script covers the rest: Zurkhwood
Stem, Zurkhwood Cap, Nightlight Cap, Fungal Loam, Faerzress Stone, Bluecap and Trillimac. For each it writes the
blockstate, block and item models and loot table (drops itself; Trillimac's and the Nightlight Cap's come from
tools/underdark_flora.py, see FLORA_LOOT), adds it to its tags (mineable, the vanilla
mushroom soil tag, the Dwarf's stonecunning tag) and adds its lang name, plus the five biomes' names. Textures come
from tools/underdark_block_textures.py. Safe to rerun.

It also writes the building blocks' one-offs (design 9 T6): Spider Sigil Stone, Faerie Fire Lantern, Spidersilk
Block / Carpet / Curtain, Web Bridge Planks, Duergar Brazier, Chain Cage and the Duergar Steel Ingot, with their
recipes, the recipes the families can't express (Drowstone Bricks needs Spidersilk, Faerzress Stone stonecuts into
Polished Faerzress, Greysteel Plate back into ingots) and the brazier / curtain entity tags. Spidersilk is the drow's
drop (`Drow.SPIDERSILK`). Their textures come from tools/underdark_building_textures.py.
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
BUILDING_NAMES = {
    'spider_sigil_stone': 'Spider Sigil Stone',
    'faerie_lantern': 'Faerie Fire Lantern',
    'spidersilk_block': 'Spidersilk Block',
    'spidersilk_carpet': 'Spidersilk Carpet',
    'spidersilk_curtain': 'Spidersilk Curtain',
    'web_bridge': 'Web Bridge Planks',
    'duergar_brazier': 'Duergar Brazier',
    'chain_cage': 'Chain Cage',
}
ITEM_NAMES = {
    'duergar_steel_ingot': 'Duergar Steel Ingot',
}
MESSAGES = {
    'block.dndclasses.chain_cage.locked': 'Locked. A Rogue could pick it.',
    'block.dndclasses.chain_cage.locked_now': 'Chain Cage locked',
    'block.dndclasses.chain_cage.unlocked_now': 'Chain Cage unlocked',
}
# Blocks whose loot table tools/underdark_flora.py owns (Trillimac drops slices, the Nightlight Cap now and then
# Nightlight Spores); this script leaves those files alone so the two scripts can run in any order.
FLORA_LOOT = ('nightlight_cap', 'trillimac')
SPIDERSILK = 'dndclasses:spidersilk'
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
    entries.update({f'block.{NS}.{k}': v for k, v in BUILDING_NAMES.items()})
    entries.update({f'item.{NS}.{k}': v for k, v in ITEM_NAMES.items()})
    entries.update(MESSAGES)
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


# --- Building blocks (T6) ---------------------------------------------------------------------------------------

def element(frm, to, faces):
    return {'from': frm, 'to': to, 'faces': faces}


def face(texture, uv, cull=None):
    f = {'uv': uv, 'texture': texture}
    if cull:
        f['cullface'] = cull
    return f


def sigil_stone():
    name = 'spider_sigil_stone'
    model(name, {'parent': 'minecraft:block/template_glazed_terracotta', 'textures': {'pattern': tex(name)}})
    turn = {'south': 0, 'west': 90, 'north': 180, 'east': 270}
    variants = {}
    for facing, y in turn.items():
        v = {'model': ref('block/' + name)}
        if y:
            v['y'] = y
        variants[f'facing={facing}'] = v
    blockstate(name, {'variants': variants})
    item_model(name, ref('block/' + name))


def lantern():
    name = 'faerie_lantern'
    model(name, {'parent': 'minecraft:block/template_lantern', 'textures': {'lantern': tex(name)}})
    model(name + '_hanging', {'parent': 'minecraft:block/template_hanging_lantern', 'textures': {'lantern': tex(name)}})
    blockstate(name, {'variants': {'hanging=false': {'model': ref('block/' + name)},
                                   'hanging=true': {'model': ref('block/' + name + '_hanging')}}})
    write(os.path.join(ASSETS, 'models/item', name + '.json'),
          {'parent': 'minecraft:item/generated', 'textures': {'layer0': ref('item/' + name)}})


def carpet():
    name = 'spidersilk_carpet'
    model(name, {'parent': 'minecraft:block/carpet', 'textures': {'wool': tex('spidersilk_block')}})
    blockstate(name, {'variants': {'': {'model': ref('block/' + name)}}})
    item_model(name, ref('block/' + name))


def curtain():
    """A pane: vanilla's glass pane models and multipart, with the curtain and its hem as pane and edge."""
    name = 'spidersilk_curtain'
    textures = {'pane': tex(name), 'edge': tex(name + '_top')}
    for part in ('post', 'side', 'side_alt', 'noside', 'noside_alt'):
        model(f'{name}_{part}', {'parent': f'minecraft:block/template_glass_pane_{part}', 'textures': textures})
    parts = [{'apply': {'model': ref(f'block/{name}_post')}}]
    sides = [('north', 'side', 0), ('east', 'side', 90), ('south', 'side_alt', 0), ('west', 'side_alt', 90)]
    for side, part, y in sides:
        apply = {'model': ref(f'block/{name}_{part}')}
        if y:
            apply['y'] = y
        parts.append({'apply': apply, 'when': {side: 'true'}})
    nosides = [('north', 'noside', 0), ('east', 'noside_alt', 0), ('south', 'noside_alt', 90), ('west', 'noside', 270)]
    for side, part, y in nosides:
        apply = {'model': ref(f'block/{name}_{part}')}
        if y:
            apply['y'] = y
        parts.append({'apply': apply, 'when': {side: 'false'}})
    blockstate(name, {'multipart': parts})
    write(os.path.join(ASSETS, 'models/item', name + '.json'),
          {'parent': 'minecraft:item/generated', 'textures': {'layer0': tex(name)}})


def web_bridge():
    name = 'web_bridge'
    top, side = '#top', '#side'
    model(name, {'parent': 'minecraft:block/block',
                 'textures': {'particle': tex(name), 'top': tex(name), 'side': tex(name + '_side')},
                 'elements': [element([0, 8, 0], [16, 16, 16], {
                     'up': face(top, [0, 0, 16, 16], 'up'), 'down': face(top, [0, 0, 16, 16]),
                     'north': face(side, [0, 0, 16, 8], 'north'), 'south': face(side, [0, 0, 16, 8], 'south'),
                     'west': face(side, [0, 0, 16, 8], 'west'), 'east': face(side, [0, 0, 16, 8], 'east')})]})
    blockstate(name, {'variants': {'': {'model': ref('block/' + name)}}})
    item_model(name, ref('block/' + name))


def brazier():
    """A black-iron bowl of coals on a squat foot; the coals are the bowl's top face."""
    name = 'duergar_brazier'
    s, c = '#side', '#coals'

    def box(frm, to, top=None):
        w, h = to[0] - frm[0], to[1] - frm[1]
        u0 = (16 - w) / 2
        v0 = 16 - to[1]
        sides = {d: face(s, [u0, v0, u0 + w, v0 + h]) for d in ('north', 'south', 'east', 'west')}
        sides['down'] = face(s, [frm[0], frm[2], to[0], to[2]])
        sides['up'] = face(top or s, [frm[0], frm[2], to[0], to[2]])
        return element(frm, to, sides)
    model(name, {'parent': 'minecraft:block/block', 'ambientocclusion': False,
                 'textures': {'particle': tex(name + '_side'), 'side': tex(name + '_side'),
                              'coals': tex(name + '_coals')},
                 'elements': [box([4, 0, 4], [12, 3, 12]), box([6, 3, 6], [10, 6, 10]),
                              box([1, 6, 1], [15, 12, 15], top=c)]})
    blockstate(name, {'variants': {'': {'model': ref('block/' + name)}}})
    item_model(name, ref('block/' + name))


def chain_cage():
    """A barred gate one block tall. Unrotated (facing south) it closes across z 7-9 and swings open against x 0-2."""
    name = 'chain_cage'
    b = '#bars'

    def panel(frm, to, axis_x):
        if axis_x:  # spans x, thin in z
            faces = {'north': face(b, [0, 0, 16, 16]), 'south': face(b, [16, 0, 0, 16]),
                     'west': face(b, [7, 0, 9, 16]), 'east': face(b, [7, 0, 9, 16]),
                     'up': face(b, [0, 7, 16, 9]), 'down': face(b, [0, 7, 16, 9])}
        else:  # spans z, thin in x
            faces = {'west': face(b, [0, 0, 16, 16]), 'east': face(b, [16, 0, 0, 16]),
                     'north': face(b, [7, 0, 9, 16]), 'south': face(b, [7, 0, 9, 16]),
                     'up': face(b, [7, 0, 9, 16]), 'down': face(b, [7, 0, 9, 16])}
        return element(frm, to, faces)
    textures = {'particle': tex(name), 'bars': tex(name)}
    model(name, {'parent': 'minecraft:block/block', 'textures': textures,
                 'elements': [panel([0, 0, 7], [16, 16, 9], True)]})
    model(name + '_open', {'parent': 'minecraft:block/block', 'textures': textures,
                           'elements': [panel([0, 0, 0], [2, 16, 16], False)]})
    turn = {'south': 0, 'west': 90, 'north': 180, 'east': 270}
    variants = {}
    for facing, y in turn.items():
        for is_open in ('false', 'true'):
            for locked in ('false', 'true'):
                v = {'model': ref('block/' + name + ('_open' if is_open == 'true' else ''))}
                if y:
                    v['y'] = y
                variants[f'facing={facing},locked={locked},open={is_open}'] = v
    blockstate(name, {'variants': variants})
    write(os.path.join(ASSETS, 'models/item', name + '.json'),
          {'parent': 'minecraft:item/generated', 'textures': {'layer0': tex(name)}})


def ingot():
    write(os.path.join(ASSETS, 'models/item', 'duergar_steel_ingot.json'),
          {'parent': 'minecraft:item/generated', 'textures': {'layer0': ref('item/duergar_steel_ingot')}})


def ingredient(value):
    return {'tag': value[1:]} if value.startswith('#') else {'item': value}


def recipe_file(name, obj):
    write(os.path.join(DATA, NS, 'recipes', name + '.json'), obj)


def shaped(name, pattern, key, result, count=1, category='building'):
    recipe = {'type': 'minecraft:crafting_shaped', 'category': category,
              'key': {k: ingredient(v) for k, v in key.items()}, 'pattern': pattern,
              'result': {'count': count, 'item': result} if count != 1 else {'item': result},
              'show_notification': True}
    recipe_file(name, recipe)


def shapeless(name, ingredients, result, count=1, category='building'):
    recipe = {'type': 'minecraft:crafting_shapeless', 'category': category,
              'ingredients': [ingredient(v) for v in ingredients],
              'result': {'count': count, 'item': result} if count != 1 else {'item': result}}
    recipe_file(name, recipe)


def stonecut(name, source, result, count=1):
    recipe_file(name, {'type': 'minecraft:stonecutting', 'count': count, 'ingredient': {'item': source},
                       'result': result})


def building_recipes():
    shaped('drowstone_bricks', ['FBF', 'BSB', 'FBF'],
           {'F': ref('faerzress_stone'), 'B': 'minecraft:polished_blackstone', 'S': SPIDERSILK},
           ref('drowstone_bricks'), 8)
    for result, count in (('polished_faerzress', 1), ('polished_faerzress_stairs', 1),
                          ('polished_faerzress_slab', 2), ('polished_faerzress_wall', 1)):
        stonecut(f'{result}_from_faerzress_stone_stonecutting', ref('faerzress_stone'), ref(result), count)
    shapeless('spider_sigil_stone', [ref('drowstone_bricks'), 'minecraft:glow_ink_sac'], ref('spider_sigil_stone'))
    stonecut('spider_sigil_stone_from_drowstone_bricks_stonecutting', ref('drowstone_bricks'),
             ref('spider_sigil_stone'))
    shaped('faerie_lantern', ['NNN', 'NFN', 'NNN'], {'N': 'minecraft:iron_nugget', 'F': ref('faerzress_stone')},
           ref('faerie_lantern'), 1, 'misc')
    shaped('spidersilk_block', ['SS', 'SS'], {'S': SPIDERSILK}, ref('spidersilk_block'))
    shaped('spidersilk_carpet', ['##'], {'#': ref('spidersilk_block')}, ref('spidersilk_carpet'), 3, 'misc')
    shaped('spidersilk_curtain', ['SS', 'SS', 'SS'], {'S': SPIDERSILK}, ref('spidersilk_curtain'), 3, 'misc')
    shaped('web_bridge', ['SSS', 'PPP'], {'S': SPIDERSILK, 'P': ref('zurkhwood_planks')}, ref('web_bridge'), 6)
    shaped('duergar_brazier', ['ICI', 'III', ' I '], {'I': 'minecraft:iron_ingot', 'C': 'minecraft:coal_block'},
           ref('duergar_brazier'), 1, 'misc')
    shaped('chain_cage', ['BCB', 'BCB'], {'B': 'minecraft:iron_bars', 'C': 'minecraft:chain'}, ref('chain_cage'), 2,
           'redstone')
    shapeless('duergar_steel_ingot_from_greysteel_plate', [ref('greysteel_plate')], ref('duergar_steel_ingot'), 9,
              'misc')


def entity_tag(name, values):
    write(os.path.join(DATA, NS, 'tags/entity_types', name + '.json'), {'replace': False, 'values': values})


def building_blocks():
    sigil_stone()
    lantern()
    cube('spidersilk_block')
    carpet()
    curtain()
    web_bridge()
    brazier()
    chain_cage()
    ingot()
    for name in BUILDING_NAMES:
        loot(name)
    building_recipes()
    add_to_tag('blocks', 'mineable/pickaxe', [ref('spider_sigil_stone'), ref('faerie_lantern'),
                                              ref('duergar_brazier'), ref('chain_cage')])
    add_to_tag('blocks', 'mineable/axe', [ref('web_bridge')])
    # The duergar and drow faction tags come with their tickets (T5, T4); optional until then.
    entity_tag('brazier_immune', ['#dndclasses:faction/duergar_clans'])
    entity_tag('web_walkers', ['minecraft:spider', 'minecraft:cave_spider',
                               '#dndclasses:faction/drow'])


def main():
    stem()
    mushroom_block('zurkhwood_cap')
    mushroom_block('nightlight_cap')
    cube('fungal_loam')
    cube('faerzress_stone')
    plant('bluecap')
    plant('trillimac')
    for name in NAMES:
        if name not in FLORA_LOOT:
            loot(name)
    add_to_tag('blocks', 'mineable/axe', [ref('zurkhwood_stem'), ref('zurkhwood_cap'), ref('nightlight_cap')])
    add_to_tag('blocks', 'mineable/shovel', [ref('fungal_loam')])
    add_to_tag('blocks', 'mineable/pickaxe', [ref('faerzress_stone')])
    # Vanilla mushrooms grow on Fungal Loam in any light, and huge-mushroom features (the zurkhwood) stand on it.
    add_to_tag('blocks', 'mushroom_grow_block', [ref('fungal_loam')])
    mod_tag('blocks', 'stonecunning', [ref('faerzress_stone')])
    building_blocks()
    lang()
    print(f'{len(NAMES)} Underdark blocks and {len(BUILDING_NAMES)} building blocks written')


if __name__ == '__main__':
    main()
