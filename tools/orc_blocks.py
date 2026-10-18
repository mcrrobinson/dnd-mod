"""Writes the resources of the Orc Strongholds' one-off building blocks (Building Blocks epic).

    python3 tools/orc_blocks.py

The blocks registered in OrcBuildingBlocks.java: the tanned hide carpets and Hide Rug, the War Totem, the
Sharpened Stake Wall, the Tusk Rack and the War Drum. For each: blockstate, block and item models, loot table
(drops itself), crafting recipe, tag entries and lang name; plus the stake's damage type and the war drum's
message. The hide, mud brick and slag families are in tools/block_families.py; textures in
tools/orc_block_textures.py. Safe to rerun.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import block_families as bf  # noqa: E402

NS = bf.NS
TURN = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
CARPETS = [('tanned_hide_carpet', 'Tanned Hide Carpet', 'tanned_hide'),
           ('pale_tanned_hide_carpet', 'Pale Tanned Hide Carpet', 'pale_tanned_hide'),
           ('grey_tanned_hide_carpet', 'Grey Tanned Hide Carpet', 'grey_tanned_hide'),
           ('hide_rug', 'Hide Rug', 'hide_rug')]


def simple(name, model):
    bf.blockstate(name, {'variants': {'': bf.variant(model)}})


def facing_states(name, model):
    bf.blockstate(name, {'variants': {f'facing={d}': bf.variant(model, y=y) for d, y in TURN.items()}})


def recipe(name, pattern, key, count=1):
    bf.shaped(name, pattern, key, bf.ref(name), count)


def blocks():
    for name, _, texture in CARPETS:
        bf.block_model(name, 'minecraft:block/carpet', {'wool': texture})
        simple(name, name)
        bf.item_model(name, bf.ref('block/' + name))
        if name != 'hide_rug':
            recipe(name, ['##'], {'#': bf.ref(texture)}, 3)
    recipe('hide_rug', ['RHR'], {'R': 'minecraft:rabbit_hide', 'H': bf.ref('tanned_hide')}, 3)

    # War Totem: an orientable dark oak log, the carved face on the front.
    bf.write(os.path.join(bf.ASSETS, 'models/block/war_totem.json'), {
        'parent': 'minecraft:block/orientable',
        'textures': {'front': bf.ref('block/war_totem_front'), 'side': 'minecraft:block/dark_oak_log',
                     'top': 'minecraft:block/dark_oak_log_top'}})
    facing_states('war_totem', 'war_totem')
    bf.item_model('war_totem', bf.ref('block/war_totem'))
    recipe('war_totem', ['R', 'L', 'B'], {'R': 'minecraft:red_dye', 'L': 'minecraft:dark_oak_log',
                                          'B': 'minecraft:bone_meal'})

    # Sharpened Stake Wall: the wall shape (its facing doesn't change the model).
    stake = bf.Family('sharpened_stake', 'Sharpened Stake', ['wall'], material='wood')
    bf.gen_wall(stake)
    recipe('sharpened_stake_wall', ['LSL', 'LSL'], {'L': 'minecraft:dark_oak_log', 'S': 'minecraft:string'}, 6)

    # Tusk Rack: a plaque one pixel thick against the block behind, its front looking north unturned.
    rack = bf.ref('block/tusk_rack')
    bf.write(os.path.join(bf.ASSETS, 'models/block/tusk_rack.json'), {
        'parent': 'minecraft:block/block',
        'textures': {'particle': rack, 'rack': rack},
        'elements': [{'from': [0, 0, 15], 'to': [16, 16, 16], 'faces': {
            'north': {'uv': [0, 0, 16, 16], 'texture': '#rack'},
            'south': {'uv': [16, 0, 0, 16], 'texture': '#rack', 'cullface': 'south'},
            'east': {'uv': [15, 0, 16, 16], 'texture': '#rack'},
            'west': {'uv': [0, 0, 1, 16], 'texture': '#rack'},
            'up': {'uv': [0, 0, 16, 1], 'texture': '#rack'},
            'down': {'uv': [0, 15, 16, 16], 'texture': '#rack'}}}]})
    facing_states('tusk_rack', 'tusk_rack')
    bf.write(os.path.join(bf.ASSETS, 'models/item/tusk_rack.json'),
             {'parent': 'minecraft:item/generated', 'textures': {'layer0': rack}})
    recipe('tusk_rack', ['B B', 'PPP'], {'B': 'minecraft:bone', 'P': 'minecraft:dark_oak_planks'})

    # War Drum: a hide head on a barrel body.
    bf.block_model('war_drum', 'minecraft:block/cube_bottom_top',
                   {'top': 'war_drum_top', 'side': 'war_drum_side', 'bottom': 'war_drum_bottom'})
    simple('war_drum', 'war_drum')
    bf.item_model('war_drum', bf.ref('block/war_drum'))
    recipe('war_drum', ['H', 'B'], {'H': bf.ref('tanned_hide'), 'B': 'minecraft:barrel'})

    names = [c[0] for c in CARPETS] + ['war_totem', 'sharpened_stake_wall', 'tusk_rack', 'war_drum']
    for name in names:
        bf.loot(name)
    bf.add_to_tag('blocks', 'mineable/axe', [bf.ref(n) for n in names])
    for kind in ('blocks', 'items'):
        bf.add_to_tag(kind, 'walls', [bf.ref('sharpened_stake_wall')])

    bf.write(os.path.join(bf.DATA, NS, 'damage_type/stake.json'),
             {'exhaustion': 0.1, 'message_id': f'{NS}.stake', 'scaling': 'never'})

    lang = {f'block.{NS}.{name}': title for name, title, _ in CARPETS}
    lang.update({
        f'block.{NS}.war_totem': 'War Totem',
        f'block.{NS}.sharpened_stake_wall': 'Sharpened Stake Wall',
        f'block.{NS}.tusk_rack': 'Tusk Rack',
        f'block.{NS}.war_drum': 'War Drum',
        f'death.attack.{NS}.stake': '%1$s was impaled on a sharpened stake',
        f'death.attack.{NS}.stake.player': '%1$s was impaled on a sharpened stake while fleeing %2$s',
        f'message.{NS}.war_drum.rally': 'The war drum thunders: your blood runs hot!',
    })
    add_lang(lang)


def add_lang(entries):
    """Appends missing keys to en_us.json (in place, keeping the file's own formatting), as block_families does."""
    path = os.path.join(bf.ASSETS, 'lang/en_us.json')
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


if __name__ == '__main__':
    blocks()
    print('orc blocks written')
