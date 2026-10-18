"""Writes the resources of the Dragon Temple's one-off building blocks (ModTempleBlocks; Building Blocks epic).

    python3 tools/temple_blocks.py

Blockstates, block and item models, loot tables (each drops itself), recipes, mineable tags and lang entries for
the Ancestor Relief, Prayer Wheel, the Ember/Frost/Storm Incense Braziers, the Paper Lantern and the Sparring Post.
The temple's families (Dragonscale Tiles, Temple Lacquer Planks, Breath-Scorched Stone) are in
tools/block_families.py; every texture is drawn by tools/temple_block_textures.py. Safe to rerun.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from block_families import ASSETS, NS, add_to_tag, blockstate, loot, ref, shaped, write  # noqa: E402

KINDS = {'ember': ('Ember', 'red_dye'), 'frost': ('Frost', 'light_blue_dye'), 'storm': ('Storm', 'cyan_dye')}
FACING_Y = {'north': 0, 'east': 90, 'south': 180, 'west': 270}


def model(name, obj):
    write(os.path.join(ASSETS, 'models/block', name + '.json'), obj)


def item(name, parent=None):
    write(os.path.join(ASSETS, 'models/item', name + '.json'), {'parent': ref('block/' + (parent or name))})


def face(texture, uv=None, cull=None):
    f = {'texture': '#' + texture}
    if uv:
        f['uv'] = uv
    if cull:
        f['cullface'] = cull
    return f


def box(frm, to, faces, shade=True):
    e = {'from': frm, 'to': to, 'faces': faces}
    if not shade:
        e['shade'] = False
    return e


def all_faces(texture, sides_uv=None, ends_uv=None):
    out = {}
    for d in ('north', 'east', 'south', 'west'):
        out[d] = face(texture, sides_uv)
    for d in ('up', 'down'):
        out[d] = face(texture, ends_uv)
    return out


def facing_states(name):
    return {'variants': {f'facing={d}': ({'model': ref('block/' + name), 'y': y} if y else
                                         {'model': ref('block/' + name)}) for d, y in FACING_Y.items()}}


# --- the blocks -----------------------------------------------------------------------------------------------

def ancestor_relief():
    model('ancestor_relief', {'parent': 'minecraft:block/orientable', 'textures': {
        'front': ref('block/ancestor_relief_front'), 'side': ref('block/ancestor_relief_side'),
        'top': ref('block/ancestor_relief_side')}})
    blockstate('ancestor_relief', facing_states('ancestor_relief'))
    item('ancestor_relief')


def prayer_wheel():
    for name, drum in (('prayer_wheel', 'prayer_wheel_drum'), ('prayer_wheel_spinning', 'prayer_wheel_drum_spinning')):
        cap_faces = all_faces('cap', [2, 14, 14, 16], [2, 2, 14, 14])
        model(name, {'parent': 'minecraft:block/block', 'textures': {
            'particle': ref('block/prayer_wheel_drum'), 'drum': ref('block/' + drum),
            'cap': ref('block/prayer_wheel_cap')}, 'elements': [
            box([2, 0, 2], [14, 2, 14], dict(cap_faces, down=face('cap', [2, 2, 14, 14], 'down'))),
            box([3, 2, 3], [13, 14, 13], all_faces('drum', [3, 2, 13, 14], [3, 3, 13, 13])),
            box([2, 14, 2], [14, 16, 14], dict(cap_faces, up=face('cap', [2, 2, 14, 14], 'up'))),
        ]})
    blockstate('prayer_wheel', {'variants': {'spinning=false': {'model': ref('block/prayer_wheel')},
                                             'spinning=true': {'model': ref('block/prayer_wheel_spinning')}}})
    item('prayer_wheel')


def incense_braziers():
    legs = [box([x, 0, z], [x + 2, 5, z + 2], all_faces('bronze', [x, 11, x + 2, 16]))
            for x in (3, 11) for z in (3, 11)]
    bowl = box([2, 5, 2], [14, 9, 14], {
        'north': face('bronze', [2, 7, 14, 11]), 'east': face('bronze', [2, 7, 14, 11]),
        'south': face('bronze', [2, 7, 14, 11]), 'west': face('bronze', [2, 7, 14, 11]),
        'down': face('bronze', [2, 2, 14, 14]), 'up': face('embers', [2, 2, 14, 14])})
    rims = [box(f, t, all_faces('bronze', [0, 0, 12, 2])) for f, t in (
        ([2, 9, 2], [14, 11, 3]), ([2, 9, 13], [14, 11, 14]), ([2, 9, 3], [3, 11, 13]), ([13, 9, 3], [14, 11, 13]))]
    model('template_incense_brazier', {'parent': 'minecraft:block/block', 'textures': {'particle': '#bronze'},
                                       'elements': legs + [bowl] + rims})
    for kind in KINDS:
        name = f'{kind}_incense_brazier'
        model(name, {'parent': ref('block/template_incense_brazier'), 'textures': {
            'bronze': ref('block/incense_brazier_bronze'), 'embers': ref(f'block/{kind}_incense_brazier_embers')}})
        blockstate(name, {'variants': {'': {'model': ref('block/' + name)}}})
        item(name)


def paper_lantern():
    def lantern(y0, hanging):
        # Atlas (textures/block/paper_lantern.png): body side 0-8 x 0-9, body ends 8-16 x 8-16, cap faces
        # 8-14 x 0-6, cap sides 8-14 x 6-7, cord 14-15.
        cap = {d: face('lantern', [8, 6, 14, 7]) for d in ('north', 'east', 'south', 'west')}
        cap.update(up=face('lantern', [8, 0, 14, 6]), down=face('lantern', [8, 0, 14, 6]))
        body = {d: face('lantern', [0, 0, 8, 9]) for d in ('north', 'east', 'south', 'west')}
        body.update(up=face('lantern', [8, 8, 16, 16]), down=face('lantern', [8, 8, 16, 16]))
        elements = [box([5, y0, 5], [11, y0 + 1, 11], cap), box([4, y0 + 1, 4], [12, y0 + 10, 12], body, shade=False),
                    box([5, y0 + 10, 5], [11, y0 + 11, 11], cap)]
        if hanging:
            elements.append(box([7.5, y0 + 11, 7.5], [8.5, 16, 8.5], all_faces('lantern', [14, 0, 15, 2])))
        return {'parent': 'minecraft:block/block', 'textures': {
            'lantern': ref('block/paper_lantern'), 'particle': ref('block/paper_lantern')}, 'elements': elements}
    model('paper_lantern', lantern(0, False))
    model('paper_lantern_hanging', lantern(3, True))
    blockstate('paper_lantern', {'variants': {'hanging=false': {'model': ref('block/paper_lantern')},
                                              'hanging=true': {'model': ref('block/paper_lantern_hanging')}}})
    item('paper_lantern')


def sparring_post():
    post = all_faces('side', [4, 0, 12, 16], [4, 4, 12, 12])
    post['up'] = face('top', [4, 4, 12, 12], 'up')
    post['down'] = face('top', [4, 4, 12, 12], 'down')
    pad = {'north': face('front', [3, 2, 13, 14]), 'east': face('front', [0, 2, 1, 14]),
           'west': face('front', [15, 2, 16, 14]), 'up': face('front', [3, 0, 13, 1]),
           'down': face('front', [3, 15, 13, 16])}
    model('sparring_post', {'parent': 'minecraft:block/block', 'textures': {
        'particle': ref('block/sparring_post_side'), 'side': ref('block/sparring_post_side'),
        'front': ref('block/sparring_post_front'), 'top': ref('block/sparring_post_top')}, 'elements': [
        box([4, 0, 4], [12, 16, 12], post), box([3, 2, 3], [13, 14, 4], pad)]})
    blockstate('sparring_post', facing_states('sparring_post'))
    item('sparring_post')


# --- loot, recipes, tags, lang --------------------------------------------------------------------------------

PICKAXE = ['ancestor_relief', 'prayer_wheel'] + [f'{k}_incense_brazier' for k in KINDS]
AXE = ['paper_lantern', 'sparring_post']

LANG = {
    'block.dndclasses.ancestor_relief': 'Ancestor Relief',
    'block.dndclasses.prayer_wheel': 'Prayer Wheel',
    'block.dndclasses.ember_incense_brazier': 'Ember Incense Brazier',
    'block.dndclasses.frost_incense_brazier': 'Frost Incense Brazier',
    'block.dndclasses.storm_incense_brazier': 'Storm Incense Brazier',
    'block.dndclasses.paper_lantern': 'Paper Lantern',
    'block.dndclasses.sparring_post': 'Sparring Post',
    'message.dndclasses.sparring_post.hit': 'Sparring Post: %s damage (%s%% swing)',
    'message.dndclasses.sparring_post.crit': 'Sparring Post: %s damage, critical hit! (%s%% swing)',
}


def recipes():
    mc = 'minecraft:'
    shaped('ancestor_relief', [' # ', '#G#', ' # '], {'#': mc + 'chiseled_deepslate', 'G': mc + 'gold_nugget'},
           ref('ancestor_relief'), 4)
    shaped('prayer_wheel', ['C', 'R', 'C'], {'C': mc + 'copper_ingot', 'R': mc + 'red_wool'}, ref('prayer_wheel'), 1,
           'decorations')
    for kind, (_, dye) in KINDS.items():
        shaped(f'{kind}_incense_brazier', ['ICI', 'IDI'], {'I': mc + 'copper_ingot', 'C': mc + 'coal', 'D': mc + dye},
               ref(f'{kind}_incense_brazier'), 1, 'decorations')
    shaped('paper_lantern', [' S ', 'PTP', ' P '], {'S': mc + 'string', 'P': mc + 'paper', 'T': mc + 'torch'},
           ref('paper_lantern'), 1, 'decorations')
    shaped('sparring_post', ['W', 'H', 'F'], {'W': mc + 'white_wool', 'H': mc + 'hay_block', 'F': mc + 'oak_fence'},
           ref('sparring_post'), 1, 'decorations')


def lang():
    path = os.path.join(ASSETS, 'lang/en_us.json')
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
    ancestor_relief()
    prayer_wheel()
    incense_braziers()
    paper_lantern()
    sparring_post()
    for name in PICKAXE + AXE:
        loot(name)
    recipes()
    add_to_tag('blocks', 'mineable/pickaxe', [ref(n) for n in PICKAXE])
    add_to_tag('blocks', 'mineable/axe', [ref(n) for n in AXE])
    lang()
    print(f'{len(PICKAXE + AXE)} temple blocks written')


if __name__ == '__main__':
    main()
