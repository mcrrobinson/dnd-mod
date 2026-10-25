"""Writes the resources of the Underdark's plants, fungi and lake life (UnderdarkFlora.java, design 9 T3).

    python3 tools/underdark_flora.py

For each block: blockstate, block and item models, loot table, tags. For the items: item models. Plus the Roasted
Trillimac cooking recipes, the new drops of two foundation blocks (Trillimac gives slices, Nightlight Caps now and then
Nightlight Spores), the Underdark explorer maps' structure tags and loot tables, the music.underdark sound event and
the lang names. Textures come from tools/underdark_flora_textures.py; the giant fungi's worldgen from
tools/underdark_worldgen.py. Safe to rerun.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from block_families import ASSETS, DATA, NS, add_to_tag, blockstate, item_model, ref, write  # noqa: E402

BLOCKS = {
    'zurkhwood_gills': 'Zurkhwood Gills',
    'zurkhwood_spores': 'Zurkhwood Spores',
    'nightlight_spores': 'Nightlight Spores',
    'ripplebark': 'Ripplebark',
    'barrelstalk': 'Barrelstalk',
    'waterorb': 'Waterorb',
    'faerie_lichen': 'Faerie Lichen',
    'spore_drip': 'Glowing Spore Drip',
    'pearl_clam': 'Pearl-Clam',
}
ITEMS = {
    'ripplebark': 'Ripplebark',
    'trillimac_slice': 'Trillimac Slice',
    'roasted_trillimac': 'Roasted Trillimac',
    'glow_spores': 'Glow Spores',
    'darklake_pearl': 'Darklake Pearl',
    'underdark_map.drow_outpost': 'Map to a Drow Outpost',
    'underdark_map.duergar_hold': 'Map to a Duergar Hold',
    'underdark_map.rift': 'Map to the Rift',
}
# Each map kind: its marker, and the structures it may lead to (optional, so they count once their ticket adds them).
MAPS = {
    'drow_outpost': ('red_x', ['drow_outpost']),
    'duergar_hold': ('red_x', ['duergar_hold']),
    'rift': ('target_point', ['rift_landing', 'underdark_rift']),
}
# The lang entries go after this key (the foundation's last Underdark line), to keep the Underdark's names together.
LANG_ANCHOR = 'biome.dndclasses.grey_reaches'


def model(name, obj):
    write(os.path.join(ASSETS, 'models/block', name + '.json'), obj)


def tex(name):
    return ref('block/' + name)


def simple(name, parent='minecraft:block/cube_all', key='all'):
    model(name, {'parent': parent, 'textures': {key: tex(name)}})
    blockstate(name, {'variants': {'': {'model': ref('block/' + name)}}})


def flat_item(name, texture):
    write(os.path.join(ASSETS, 'models/item', name + '.json'),
          {'parent': 'minecraft:item/generated', 'textures': {'layer0': texture}})


def cross(name):
    simple(name, 'minecraft:block/cross', 'cross')
    flat_item(name, tex(name))


def ripplebark():
    """Cocoa-style: a shelf jutting from the wall on the north side, rotated for the other facings."""
    for age in range(3):
        half, depth, low = 3 + age, 3 + age, 7 - age
        model(f'ripplebark_stage{age}', {
            'ambientocclusion': False,
            'textures': {'particle': tex('ripplebark'), 'shelf': tex('ripplebark')},
            'elements': [{'from': [8 - half, low, 0], 'to': [8 + half, 9, depth], 'faces': {
                'up': {'uv': [8 - half, 0, 8 + half, depth], 'texture': '#shelf'},
                'down': {'uv': [8 - half, 16 - depth, 8 + half, 16], 'texture': '#shelf'},
                'south': {'uv': [8 - half, low, 8 + half, 9], 'texture': '#shelf'},
                'east': {'uv': [0, low, depth, 9], 'texture': '#shelf'},
                'west': {'uv': [16 - depth, low, 16, 9], 'texture': '#shelf'},
            }}]})
    turns = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    variants = {}
    for facing, y in turns.items():
        for age in range(3):
            v = {'model': ref(f'block/ripplebark_stage{age}')}
            if y:
                v['y'] = y
            variants[f'age={age},facing={facing}'] = v
    blockstate('ripplebark', {'variants': variants})
    item_model('ripplebark', layer='ripplebark')


def barrelstalk():
    for filled in ('filled', 'empty'):
        top = tex('barrelstalk_top_' + filled)
        model(f'barrelstalk_lower_{filled}', {
            'textures': {'particle': tex('barrelstalk_side_lower'), 'side': tex('barrelstalk_side_lower'),
                         'top': top},
            'elements': [{'from': [2, 0, 2], 'to': [14, 16, 14], 'faces': {
                f: {'uv': [2, 0, 14, 16], 'texture': '#side'} for f in ('north', 'south', 'east', 'west')}
                | {'up': {'uv': [2, 2, 14, 14], 'texture': '#top'},
                   'down': {'uv': [2, 2, 14, 14], 'texture': '#top', 'cullface': 'down'}}}]})
        model(f'barrelstalk_upper_{filled}', {
            'textures': {'particle': tex('barrelstalk_side_upper'), 'side': tex('barrelstalk_side_upper'),
                         'top': top},
            'elements': [{'from': [3, 0, 3], 'to': [13, 12, 13], 'faces': {
                f: {'uv': [3, 4, 13, 16], 'texture': '#side'} for f in ('north', 'south', 'east', 'west')}
                | {'up': {'uv': [3, 3, 13, 13], 'texture': '#top'},
                   'down': {'uv': [3, 3, 13, 13], 'texture': '#top'}}}]})
    variants = {}
    for half in ('lower', 'upper'):
        for filled, word in (('true', 'filled'), ('false', 'empty')):
            variants[f'filled={filled},half={half}'] = {'model': ref(f'block/barrelstalk_{half}_{word}')}
    blockstate('barrelstalk', {'variants': variants})
    item_model('barrelstalk', layer='barrelstalk')


def waterorb():
    model('waterorb', {'ambientocclusion': False,
                       'textures': {'particle': tex('waterorb'), 'texture': tex('waterorb')},
                       'elements': [{'from': [0, 0.25, 0], 'to': [16, 0.25, 16], 'faces': {
                           'down': {'uv': [0, 16, 16, 0], 'texture': '#texture'},
                           'up': {'uv': [0, 0, 16, 16], 'texture': '#texture'}}}]})
    blockstate('waterorb', {'variants': {'': {'model': ref('block/waterorb')}}})
    flat_item('waterorb', tex('waterorb'))


def faerie_lichen():
    """Glow lichen's multipart, with our face model."""
    model('faerie_lichen', {'ambientocclusion': False,
                            'textures': {'particle': tex('faerie_lichen'), 'lichen': tex('faerie_lichen')},
                            'elements': [{'from': [0, 0, 0.1], 'to': [16, 16, 0.1], 'faces': {
                                'north': {'uv': [16, 0, 0, 16], 'texture': '#lichen'},
                                'south': {'uv': [0, 0, 16, 16], 'texture': '#lichen'}}}]})
    turns = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270},
             'down': {'x': 90}}
    parts = []
    none = {face: 'false' for face in turns}
    for face, turn in turns.items():
        apply = {'model': ref('block/faerie_lichen')}
        if turn:
            apply.update(turn)
            apply['uvlock'] = True
        parts.append({'when': {face: 'true'}, 'apply': apply})
        parts.append({'when': dict(none), 'apply': apply})
    blockstate('faerie_lichen', {'multipart': parts})
    flat_item('faerie_lichen', tex('faerie_lichen'))


# --- loot ---------------------------------------------------------------------------------------------------------

SILK = {'condition': 'minecraft:match_tool', 'predicate': {'enchantments': [
    {'enchantment': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}
SHEARS_OR_SILK = {'condition': 'minecraft:alternative', 'terms': [
    {'condition': 'minecraft:match_tool', 'predicate': {'items': ['minecraft:shears']}}, SILK]}
EXPLOSION = {'condition': 'minecraft:survives_explosion'}


def item(name, functions=None, conditions=None):
    entry = {'type': 'minecraft:item', 'name': name}
    if functions:
        entry['functions'] = functions
    if conditions:
        entry['conditions'] = conditions
    return entry


def count(low, high=None):
    value = low if high is None else {'type': 'minecraft:uniform', 'min': low, 'max': high}
    return {'function': 'minecraft:set_count', 'count': value, 'add': False}


def table(name, pools):
    write(os.path.join(DATA, NS, 'loot_tables/blocks', name + '.json'), {'type': 'minecraft:block', 'pools': pools})


def pool(entries, conditions=None):
    p = {'rolls': 1.0, 'bonus_rolls': 0.0, 'entries': entries}
    if conditions:
        p['conditions'] = conditions
    return p


def self_or(name, other, other_functions):
    """Silk touch (or shears) gives the block; anything else `other`."""
    return [pool([{'type': 'minecraft:alternatives', 'children': [
        item(ref(name), conditions=[SHEARS_OR_SILK]),
        item(other, functions=other_functions + [{'function': 'minecraft:explosion_decay'}])]}])]


def loot_tables():
    for name in ('zurkhwood_spores', 'nightlight_spores', 'waterorb'):
        table(name, [pool([item(ref(name))], [EXPLOSION])])
    # Gills: themselves with silk touch, else Zurkhwood Spores 5% of the time (+2% per Fortune level).
    table('zurkhwood_gills', [pool([{'type': 'minecraft:alternatives', 'children': [
        item(ref('zurkhwood_gills'), conditions=[SILK]),
        item(ref('zurkhwood_spores'), conditions=[EXPLOSION, {
            'condition': 'minecraft:table_bonus', 'enchantment': 'minecraft:fortune',
            'chances': [0.05, 0.07, 0.09, 0.11]}])]}])])
    # Ripplebark: 1, or 2-3 once grown.
    grown = {'condition': 'minecraft:block_state_property', 'block': ref('ripplebark'),
             'properties': {'age': '2'}}
    table('ripplebark', [pool([item(ref('ripplebark'), functions=[
        count(1), {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 2, 'max': 3},
                   'add': False, 'conditions': [grown]}, {'function': 'minecraft:explosion_decay'}])])])
    # Barrelstalk: one, from the lower half.
    table('barrelstalk', [pool([item(ref('barrelstalk'), conditions=[
        {'condition': 'minecraft:block_state_property', 'block': ref('barrelstalk'),
         'properties': {'half': 'lower'}}, EXPLOSION])])])
    # Faerie Lichen: like glow lichen, shears only, one per face.
    faces = [{'function': 'minecraft:set_count', 'count': 1.0, 'add': True, 'conditions': [
        {'condition': 'minecraft:block_state_property', 'block': ref('faerie_lichen'), 'properties': {f: 'true'}}]}
        for f in ('down', 'up', 'north', 'south', 'west', 'east')]
    table('faerie_lichen', [pool([item(ref('faerie_lichen'), functions=faces + [
        {'function': 'minecraft:set_count', 'count': -1.0, 'add': True},
        {'function': 'minecraft:explosion_decay'}], conditions=[
        {'condition': 'minecraft:match_tool', 'predicate': {'items': ['minecraft:shears']}}])])])
    # Spore Drip: Glow Spores (Fortune adds more); Pearl-Clam: a Darklake Pearl.
    table('spore_drip', self_or('spore_drip', ref('glow_spores'), [
        count(1), {'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune',
                   'formula': 'minecraft:uniform_bonus_count', 'parameters': {'bonusMultiplier': 1}}]))
    table('pearl_clam', self_or('pearl_clam', ref('darklake_pearl'), [count(1)]))
    # The foundation's Trillimac now gives 1-2 slices (the plant itself with shears or silk touch).
    table('trillimac', self_or('trillimac', ref('trillimac_slice'), [count(1, 2)]))
    # The Nightlight Cap still drops itself, and 8% of the time Nightlight Spores too.
    table('nightlight_cap', [
        pool([item(ref('nightlight_cap'))], [EXPLOSION]),
        pool([item(ref('nightlight_spores'))], [EXPLOSION, {'condition': 'minecraft:random_chance', 'chance': 0.08}]),
    ])


# --- recipes, tags, maps, sounds, lang -------------------------------------------------------------------------------

def recipes():
    path = os.path.join(DATA, NS, 'recipes')
    for kind, suffix, ticks in (('minecraft:smelting', '', 200), ('minecraft:smoking', '_from_smoking', 100),
                                ('minecraft:campfire_cooking', '_from_campfire_cooking', 600)):
        write(os.path.join(path, 'roasted_trillimac' + suffix + '.json'), {
            'type': kind, 'category': 'food', 'ingredient': {'item': ref('trillimac_slice')},
            'result': ref('roasted_trillimac'), 'experience': 0.35, 'cookingtime': ticks})


def tags():
    add_to_tag('blocks', 'mineable/axe', [ref('zurkhwood_gills'), ref('barrelstalk'), ref('ripplebark')])
    add_to_tag('blocks', 'mineable/shovel', [ref('pearl_clam')])
    add_to_tag('blocks', 'mineable/hoe', [ref('zurkhwood_gills')])
    for kind, (_, structures) in MAPS.items():
        write(os.path.join(DATA, NS, 'tags/worldgen/structure/underdark_maps', kind + '.json'), {
            'replace': False, 'values': [{'id': ref(s), 'required': False} for s in structures]})


def maps():
    for kind, (marker, _) in MAPS.items():
        write(os.path.join(DATA, NS, 'loot_tables/underdark/map_to_' + kind + '.json'), {
            'type': 'minecraft:chest', 'pools': [pool([item('minecraft:map', functions=[
                {'function': 'minecraft:exploration_map', 'destination': ref('underdark_maps/' + kind),
                 'decoration': marker, 'zoom': 2, 'search_radius': 100, 'skip_existing_chunks': True},
                {'function': 'minecraft:set_name', 'name': {
                    'translate': f'item.{NS}.underdark_map.{kind}', 'italic': False}}])])]})


def sounds():
    path = os.path.join(ASSETS, 'sounds.json')
    with open(path) as fh:
        obj = json.load(fh)
    obj['music.underdark'] = {'sounds': [
        {'name': ref('underdark'), 'stream': True, 'weight': 3},
        {'name': 'minecraft:music.overworld.deep_dark', 'type': 'event', 'weight': 1}]}
    with open(path, 'w') as fh:
        json.dump(obj, fh, indent=4)
        fh.write('\n')


def lang():
    entries = {f'block.{NS}.{k}': v for k, v in BLOCKS.items()}
    entries.update({f'item.{NS}.{k}': v for k, v in ITEMS.items()})
    path = os.path.join(ASSETS, 'lang/en_us.json')
    with open(path) as fh:
        text = fh.read()
    current = json.loads(text)
    lines = text.rstrip().split('\n')
    at = next(i for i, line in enumerate(lines) if line.strip().startswith(json.dumps(LANG_ANCHOR)))
    new = [f'    {json.dumps(k)}: {json.dumps(v, ensure_ascii=False)},' for k, v in entries.items() if k not in current]
    if not lines[at].rstrip().endswith(','):
        lines[at] = lines[at].rstrip() + ','
        if new:
            new[-1] = new[-1].rstrip(',')
    lines[at + 1:at + 1] = new
    with open(path, 'w') as fh:
        fh.write('\n'.join(lines) + '\n')
    json.loads(open(path).read())


def main():
    simple('zurkhwood_gills')
    simple('pearl_clam')
    cross('zurkhwood_spores')
    cross('nightlight_spores')
    cross('spore_drip')
    ripplebark()
    barrelstalk()
    waterorb()
    faerie_lichen()
    for name in ('zurkhwood_gills', 'pearl_clam'):
        item_model(name, ref('block/' + name))
    for name in ('trillimac_slice', 'roasted_trillimac', 'glow_spores', 'darklake_pearl'):
        item_model(name, layer=name)
    loot_tables()
    recipes()
    tags()
    maps()
    sounds()
    lang()
    print(f'{len(BLOCKS)} Underdark flora blocks and {len(ITEMS)} items written')


if __name__ == '__main__':
    main()
