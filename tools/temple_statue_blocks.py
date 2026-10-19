"""Writes the resources of the Dragon Temple statues' one-off blocks (the temple rework card).

    python3 tools/temple_statue_blocks.py

Blockstates, block and item models, loot tables (each drops itself), recipes, mineable tags and lang entries for
the Ember/Frost/Storm Dragonstone Eyes (ModTempleBlocks, facing like a furnace) and the Dragon Horn (points away
from the face it's placed on, like an end rod). The statue stone families (Dragonstone, Dragon Belly) are in
tools/block_families.py; their textures are drawn by tools/temple_statue_textures.py. Safe to rerun.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from block_families import add_to_tag, blockstate, loot, ref, shaped  # noqa: E402
from temple_blocks import box, face, facing_states, item, model  # noqa: E402
import temple_blocks  # noqa: E402

KINDS = {'ember': 'Ember', 'frost': 'Frost', 'storm': 'Storm'}

# The horn pointing up, curving back (north) towards its tip: (from, to) of each tier.
HORN = [([5, 0, 5], [11, 6, 11]), ([6, 6, 4.5], [10, 11, 8.5]), ([7, 11, 3], [9, 16, 5])]
# variants like the end rod's
HORN_FACING = {'up': {}, 'down': {'x': 180}, 'north': {'x': 90}, 'south': {'x': 90, 'y': 180},
               'east': {'x': 90, 'y': 90}, 'west': {'x': 90, 'y': 270}}


def eyes():
    for kind in KINDS:
        name = f'{kind}_dragonstone_eye'
        model(name, {'parent': 'minecraft:block/orientable', 'textures': {
            'front': ref(f'block/{kind}_dragonstone_eye'), 'side': ref(f'block/{kind}_dragonstone'),
            'top': ref(f'block/{kind}_dragonstone')}})
        blockstate(name, facing_states(name))
        item(name)


def horn():
    elements = []
    for frm, to in HORN:
        # The texture runs from the tip (v = 0) to the base (v = 16), so each tier takes its own band.
        v0, v1 = 16 - to[1], 16 - frm[1]
        sides = {d: face('horn', [frm[0], v0, to[0], v1]) for d in ('north', 'east', 'south', 'west')}
        sides['up'] = face('horn', [frm[0], 0, to[0], 2])
        sides['down'] = face('horn', [frm[0], 14, to[0], 16])
        elements.append(box(frm, to, sides))
    model('dragon_horn', {'parent': 'minecraft:block/block', 'textures': {
        'horn': ref('block/dragon_horn'), 'particle': ref('block/dragon_horn')}, 'elements': elements,
        'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]}}})
    blockstate('dragon_horn', {'variants': {
        f'facing={d}': dict({'model': ref('block/dragon_horn')}, **r) for d, r in HORN_FACING.items()}})
    item('dragon_horn')


BLOCKS = [f'{k}_dragonstone_eye' for k in KINDS] + ['dragon_horn']

LANG = {f'block.dndclasses.{k}_dragonstone_eye': f'{t} Dragonstone Eye' for k, t in KINDS.items()}
LANG['block.dndclasses.dragon_horn'] = 'Dragon Horn'


def recipes():
    for kind in KINDS:
        shaped(f'{kind}_dragonstone_eye', ['G', '#'], {'G': 'minecraft:glowstone_dust',
                                                       '#': f'dndclasses:{kind}_dragonstone'},
               ref(f'{kind}_dragonstone_eye'), 1)
    shaped('dragon_horn', ['  B', ' B ', 'B  '], {'B': 'minecraft:bone'}, ref('dragon_horn'), 2, 'decorations')


def main():
    eyes()
    horn()
    for name in BLOCKS:
        loot(name)
    recipes()
    add_to_tag('blocks', 'mineable/pickaxe', [ref(n) for n in BLOCKS])
    temple_blocks.LANG = LANG
    temple_blocks.lang()
    print(f'{len(BLOCKS)} temple statue blocks written')


if __name__ == '__main__':
    main()
