"""Writes the resources of the Underdark Rift blocks (RiftBlocks.java, design 9 T2).

    python3 tools/rift_blocks.py

Rift Veil (a pool surface 14 px high, animated texture) and Rift Anchor (a column; the top block, top=true, wears a
violet crystal): blockstates, block and item models, and lang names, plus the rift messages' lang lines. Neither
block drops anything, so they have no loot tables. Textures come from tools/rift_block_textures.py. Safe to rerun.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from block_families import ASSETS, NS, blockstate, item_model, ref, write  # noqa: E402

LANG = {
    f'block.{NS}.rift_veil': 'Rift Veil',
    f'block.{NS}.rift_anchor': 'Rift Anchor',
    f'underdark.{NS}.arrive.title': 'The Underdark',
    f'underdark.{NS}.arrive.subtitle': 'You fall through the bottom of the world',
    f'underdark.{NS}.anchor.pull': 'The anchor pulls you upward  [%s]',
    f'underdark.{NS}.anchor.cancel.moved': 'You stepped away from the anchor. Its pull fades.',
    f'underdark.{NS}.anchor.cancel.hurt': 'The pain breaks your hold on the anchor.',
    f'underdark.{NS}.anchor.cooldown': 'The anchor is still gathering itself.',
    f'underdark.{NS}.anchor.still': 'The anchor is still. It only pulls upward from below.',
    f'underdark.{NS}.beholder_rift': 'The floor where the eye floated splits open. Cold air rises from below.',
    f'structure.{NS}.underdark_rift': 'Underdark Rift',
    f'structure.{NS}.rift_landing': 'Rift Landing',
}


def model(name, obj):
    write(os.path.join(ASSETS, 'models/block', name + '.json'), obj)


def tex(name):
    return ref('block/' + name)


def veil():
    model('rift_veil', {
        'parent': 'minecraft:block/block',
        'textures': {'particle': tex('rift_veil'), 'veil': tex('rift_veil')},
        'elements': [{
            'from': [0, 0, 0], 'to': [16, 14, 16],
            'faces': {face: {'texture': '#veil', 'cullface': face} if face != 'up' else {'texture': '#veil'}
                      for face in ('down', 'up', 'north', 'south', 'east', 'west')},
        }],
    })
    blockstate('rift_veil', {'variants': {'': {'model': ref('block/rift_veil')}}})
    item_model('rift_veil', ref('block/rift_veil'))


def anchor():
    model('rift_anchor', {'parent': 'minecraft:block/cube_column',
                          'textures': {'end': tex('rift_anchor_top'), 'side': tex('rift_anchor')}})
    model('rift_anchor_crystal', {'parent': 'minecraft:block/cube_column',
                                  'textures': {'end': tex('rift_anchor_crystal_top'), 'side': tex('rift_anchor_crystal')}})
    blockstate('rift_anchor', {'variants': {
        'top=false': {'model': ref('block/rift_anchor')},
        'top=true': {'model': ref('block/rift_anchor_crystal')},
    }})
    item_model('rift_anchor', ref('block/rift_anchor_crystal'))


def lang_entries(entries):
    """Appends the missing entries to en_us.json, keeping its one-entry-per-line layout."""
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
    veil()
    anchor()
    lang_entries(LANG)
    print('Rift blocks written')


if __name__ == '__main__':
    main()
