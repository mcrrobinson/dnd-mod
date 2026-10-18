"""Generates the shared adventuring blocks' textures and resources (Building Blocks: "Shared adventuring blocks").

    python3 tools/adventuring_block_textures.py

Textures (16x16, textures/block/ unless noted):
- Bedrolls, for each colour (bedroll = red, green_bedroll, blue_bedroll, brown_bedroll): <name>_head (the blanket
  turned down under the pillow), <name>_foot (stitched canvas, two leather ties), <name>_side; bedroll_pillow
  (off-white ticking, shared). Item icons textures/item/<name> (the rolled-up Bedroll, recoloured from the red
  one's existing icon).
- adventurers_signpost_post (dark oak-brown post, vertical grain) and adventurers_signpost_board (weathered
  boards, horizontal grain); textures/item/adventurers_signpost.
- wanted_poster_0..3: yellowed parchment, a pin, a red WANTED header and a crude sketch (goblin, orc, highwayman,
  owlbear), with torn see-through edges.
- Tavern tables and benches use their wood's planks texture (oak, spruce, dark oak, Heartwood): no new textures.

Resources: blockstates, block and item models (templates for the tables and benches), loot tables, recipes, tags
and lang names. Safe to rerun.
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402
import block_families as bf  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
TEX = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/block')
ITEM_TEX = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/item')
N = 16
CLEAR = (0, 0, 0, 0)


def c(rgb, d=0, a=255):
    return tuple(max(0, min(255, int(v + d))) for v in rgb[:3]) + (a,)


def grid(fill=CLEAR):
    return [[fill] * N for _ in range(N)]


def save(name, rows, folder=TEX):
    os.makedirs(folder, exist_ok=True)
    pngio.write_png(os.path.join(folder, name + '.png'), N, N, rows)


# --- Bedrolls ----------------------------------------------------------------------------------------------

# Blanket colour per bedroll, and the dye that makes it.
BEDROLLS = {
    'bedroll': ((168, 52, 46), 'red_dye', 'Bedroll'),
    'green_bedroll': ((78, 112, 52), 'green_dye', 'Green Bedroll'),
    'blue_bedroll': ((56, 78, 140), 'blue_dye', 'Blue Bedroll'),
    'brown_bedroll': ((132, 94, 60), 'brown_dye', 'Brown Bedroll'),
}
CANVAS = (222, 206, 164)
LEATHER = (110, 72, 40)
BUCKLE = (200, 186, 110)


def weave(rng, base, x, y, amp=10):
    """Canvas: a faint diagonal twill plus noise."""
    d = rng.randint(-amp // 2, amp // 2)
    if (x + y) % 4 == 0:
        d -= amp // 2
    return c(base, d)


def blanket(rgb, seed):
    rng = random.Random(seed)
    rows = grid()
    for y in range(N):
        for x in range(N):
            rows[y][x] = weave(rng, rgb, x, y)
    # Stitched hem 1 px in from the long edges (the model's sides are x 1-15).
    for y in range(N):
        for x in (2, 13):
            if y % 3 != 2:
                rows[y][x] = c(rgb, 34)
        rows[y][1] = c(rgb, -26)
        rows[y][14] = c(rgb, -26)
    return rows


def bedroll_foot(rgb, seed):
    rows = blanket(rgb, seed)
    # Two leather ties across the roll, with a brass buckle each.
    for ty in (4, 11):
        for x in range(1, 15):
            rows[ty][x] = c(LEATHER, 10 if x % 5 else -10)
            rows[ty + 1][x] = c(LEATHER, -22)
        rows[ty][7] = c(BUCKLE, 20)
        rows[ty][8] = c(BUCKLE)
        rows[ty + 1][7] = c(BUCKLE, -30)
        rows[ty + 1][8] = c(BUCKLE, -40)
    return rows


def bedroll_head(rgb, seed):
    """The blanket's top end (north, under the pillow) is turned down, showing the canvas lining."""
    rows = blanket(rgb, seed)
    rng = random.Random(seed + 1)
    for y in range(0, 8):
        for x in range(1, 15):
            rows[y][x] = weave(rng, CANVAS, x, y, 8)
    for x in range(1, 15):
        rows[7][x] = c(CANVAS, -38)
        rows[8][x] = c(rgb, 30)
        rows[9][x] = c(rgb, -18)
    for y in range(0, 8):
        rows[y][1] = c(CANVAS, -30)
        rows[y][14] = c(CANVAS, -30)
    return rows


def bedroll_side(rgb, seed):
    """Side strip: the model maps rows 13-16 onto the roll's 3-px edge."""
    rng = random.Random(seed)
    rows = grid()
    for y in range(N):
        for x in range(N):
            rows[y][x] = weave(rng, rgb, x, y, 8)
    for x in range(N):
        rows[13][x] = c(rgb, 26)
        rows[15][x] = c(rgb, -34)
        if x % 3 == 1:
            rows[14][x] = c(rgb, 30)
    return rows


def pillow():
    rng = random.Random(77)
    rows = grid()
    for y in range(N):
        for x in range(N):
            rows[y][x] = c((232, 226, 210), rng.randint(-6, 6) - (8 if x % 4 == 0 else 0))
    for x in range(N):
        rows[0][x] = c((232, 226, 210), -34)
        rows[15][x] = c((232, 226, 210), -34)
    for y in range(N):
        rows[y][0] = c((232, 226, 210), -34)
        rows[y][15] = c((232, 226, 210), -34)
    return rows


def bedroll_item(rgb):
    """The red Bedroll's icon, recoloured: red pixels take the new colour at the same brightness."""
    _, _, px = pngio.read_png(os.path.join(ITEM_TEX, 'bedroll.png'))
    red = BEDROLLS['bedroll'][0]
    rows = grid()
    for y in range(N):
        for x in range(N):
            p = tuple(px[y][x]) + ((255,) if len(px[y][x]) == 3 else ())
            if p[3] and p[0] > p[1] + 40 and p[0] > p[2] + 40:
                k = p[0] / red[0]
                rows[y][x] = c(tuple(v * k for v in rgb), 0, p[3])
            else:
                rows[y][x] = p
    return rows


# --- Adventurer's Signpost ----------------------------------------------------------------------------------

POST = (92, 64, 38)
BOARD = (178, 140, 92)


def signpost_post():
    rng = random.Random(5)
    rows = grid()
    for x in range(N):
        streak = rng.randint(-10, 10)
        for y in range(N):
            rows[y][x] = c(POST, streak + rng.randint(-5, 5) + (-14 if x % 4 == 0 else 0))
    for y in (3, 11):
        rows[y][rng.randint(4, 12)] = c(POST, -30)
    return rows


def signpost_board():
    """Weathered boards: horizontal grain, a seam every 4 rows, two nail heads."""
    rng = random.Random(9)
    rows = grid()
    for y in range(N):
        streak = rng.randint(-8, 8)
        for x in range(N):
            rows[y][x] = c(BOARD, streak + rng.randint(-6, 6))
        if y % 4 == 3:
            for x in range(N):
                rows[y][x] = c(BOARD, -40)
    for x, y in ((2, 1), (13, 9)):
        rows[y][x] = c((70, 70, 72))
    return rows


def signpost_item():
    rows = grid()
    post = signpost_post()
    board = signpost_board()
    for y in range(2, 16):
        for x in (3, 4):
            rows[y][x] = post[y][x]
    rows[2][3] = c(POST, 20)
    # Arrow board pointing right, rows 3-8.
    for y in range(3, 9):
        for x in range(1, 12):
            rows[y][x] = board[y][x]
    for x, (y1, y2) in ((12, (3, 8)), (13, (4, 7)), (14, (5, 6))):
        for y in range(y1, y2 + 1):
            rows[y][x] = board[y][x]
    for x in range(1, 12):
        rows[9][x] = c(BOARD, -60)
    for x, y in ((12, 9), (13, 8), (14, 7), (15, 6)):
        rows[y][x] = c(BOARD, -60)
    # Lettering: a few dark marks.
    for x in (3, 4, 6, 8, 9, 10):
        rows[5][x] = c((46, 30, 15))
    for x in (6, 7, 9):
        rows[7][x] = c((120, 30, 24))
    return rows


# --- Wanted Poster ------------------------------------------------------------------------------------------

PARCHMENT = (226, 204, 150)
INK = (56, 40, 30)
RED = (170, 34, 28)

# Crude sketches, 8 wide x 7 high, drawn at x 4-11, y 6-12. Keys: palette below.
SKETCHES = [
    # 0: a goblin, big ears and a toothy grin
    ['g......g',
     'gg.gg.gg',
     '.gggggg.',
     '.gkggkg.',
     '.gggggg.',
     '..gwwg..',
     '...gg...'],
    # 1: an orc with tusks and a scar
    ['.oooooo.',
     'oooooooo',
     'okoossko',
     'oooooooo',
     'owoooowo',
     '.oooooo.',
     '..oooo..'],
    # 2: a hooded highwayman with a mask
    ['..hhhh..',
     '.hhhhhh.',
     'hhffffhh',
     'hkkkkkkh',
     'hhffffhh',
     '.hffffh.',
     '..hhhh..'],
    # 3: an owlbear: feathered crest, round eyes, beak
    ['b.b..b.b',
     'bbbbbbbb',
     'bwkbbkwb',
     'bbbyybbb',
     'bbbyybbb',
     '.bbbbbb.',
     '..b..b..'],
]
SKETCH_COLOURS = {
    'g': (104, 128, 62), 'k': INK, 'w': (238, 232, 210), 'o': (112, 120, 96), 's': (150, 70, 60),
    'h': (78, 66, 58), 'f': (196, 160, 128), 'b': (128, 94, 62), 'y': (214, 176, 70),
}


def wanted_poster(i):
    rng = random.Random(100 + i)
    rows = grid()
    for y in range(1, 15):
        for x in range(1, 15):
            d = rng.randint(-8, 6) - (10 if rng.random() < 0.06 else 0)
            # Yellower, darker towards the edges.
            edge = min(x - 1, 14 - x, y - 1, 14 - y)
            rows[y][x] = c(PARCHMENT, d - (12 if edge == 0 else 0))
    # Torn edges: knock out a few edge pixels.
    for _ in range(9):
        side = rng.randint(0, 3)
        k = rng.randint(2, 13)
        x, y = [(k, 1), (k, 14), (1, k), (14, k)][side]
        rows[y][x] = CLEAR
    rows[1][1] = rows[1][14] = rows[14][1] = CLEAR
    # Pin at the top.
    rows[2][7] = c((150, 150, 156))
    rows[2][8] = c((110, 110, 116))
    # WANTED: six blocky red letters, 1-px strokes in rows 3-4.
    letters = ['ww', 'a', 'n', 't', 'e', 'd']
    x = 2
    for letter in letters:
        if letter == 'ww':
            for dx in range(3):
                rows[3][x + dx] = c(RED, 10 if dx != 1 else 0)
            rows[4][x] = c(RED, -10)
            rows[4][x + 2] = c(RED, -10)
            rows[4][x + 1] = c(RED, -20)
            x += 4
        else:
            rows[3][x] = c(RED, 10)
            rows[4][x] = c(RED, -15)
            if letter in ('a', 'n', 'd'):
                rows[3][x + 1] = c(RED, 0)
                rows[4][x + 1] = c(RED, -25)
                x += 1
            x += 2
            if x > 13:
                break
    # The sketch.
    for sy, line in enumerate(SKETCHES[i]):
        for sx, key in enumerate(line):
            if key != '.':
                rows[6 + sy][4 + sx] = c(SKETCH_COLOURS[key], rng.randint(-6, 6))
    # Scrawled terms under it.
    for x in range(3, 13):
        if rng.random() < 0.7:
            rows[13][x] = c(INK, 40)
    return rows


def textures():
    for k, (name, (rgb, _, _)) in enumerate(BEDROLLS.items()):
        save(name + '_head', bedroll_head(rgb, 10 + k))
        save(name + '_foot', bedroll_foot(rgb, 20 + k))
        save(name + '_side', bedroll_side(rgb, 30 + k))
        if name != 'bedroll':
            save(name, bedroll_item(rgb), ITEM_TEX)
    save('bedroll_pillow', pillow())
    save('adventurers_signpost_post', signpost_post())
    save('adventurers_signpost_board', signpost_board())
    save('adventurers_signpost', signpost_item(), ITEM_TEX)
    for i in range(len(SKETCHES)):
        save(f'wanted_poster_{i}', wanted_poster(i))


# --- Resources ----------------------------------------------------------------------------------------------

def ref(path):
    return bf.ref(path)


def model(name, obj):
    bf.write(os.path.join(bf.ASSETS, 'models/block', name + '.json'), obj)


def item(name, obj):
    bf.write(os.path.join(bf.ASSETS, 'models/item', name + '.json'), obj)


def generated_item(name, texture):
    item(name, {'parent': 'minecraft:item/generated', 'textures': {'layer0': texture}})


def box(frm, to, faces):
    return {'from': frm, 'to': to, 'faces': faces}


ROTATION = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
WOODS = {
    'oak': ('minecraft:block/oak_planks', 'minecraft:oak_planks', 'minecraft:oak_slab', 'Oak'),
    'spruce': ('minecraft:block/spruce_planks', 'minecraft:spruce_planks', 'minecraft:spruce_slab', 'Spruce'),
    'dark_oak': ('minecraft:block/dark_oak_planks', 'minecraft:dark_oak_planks', 'minecraft:dark_oak_slab',
                 'Dark Oak'),
    'heartwood': (ref('block/heartwood_planks'), ref('heartwood_planks'), ref('heartwood_slab'), 'Heartwood'),
}


def bedroll_resources():
    for name in BEDROLLS:
        tex = {'particle': ref(f'block/{name}_side'), 'side': ref(f'block/{name}_side'),
               'pillow': ref('block/bedroll_pillow')}
        sides = {
            'north': {'uv': [1, 13, 15, 16], 'texture': '#side'},
            'south': {'uv': [1, 13, 15, 16], 'texture': '#side'},
            'east': {'uv': [0, 13, 16, 16], 'texture': '#side'},
            'west': {'uv': [0, 13, 16, 16], 'texture': '#side'},
            'down': {'uv': [1, 0, 15, 16], 'texture': '#side', 'cullface': 'down'},
        }
        # Facing north: the head is the north half, its pillow at the north end.
        model(name + '_foot', {'parent': 'minecraft:block/block', 'textures': {**tex, 'top': ref(f'block/{name}_foot')},
                               'elements': [box([1, 0, 0], [15, 3, 16], {
                                   **sides, 'up': {'uv': [1, 0, 15, 16], 'texture': '#top'}})]})
        pillow_faces = {d: {'uv': [0, 0, 10 if d in ('north', 'south') else 6, 3], 'texture': '#pillow'}
                        for d in ('north', 'south', 'east', 'west')}
        pillow_faces['up'] = {'uv': [3, 1, 13, 7], 'texture': '#pillow'}
        model(name + '_head', {'parent': 'minecraft:block/block', 'textures': {**tex, 'top': ref(f'block/{name}_head')},
                               'elements': [box([1, 0, 0], [15, 3, 16], {
                                   **sides, 'up': {'uv': [1, 0, 15, 16], 'texture': '#top'}}),
                                   box([3, 3, 1], [13, 6, 7], pillow_faces)]})
        variants = {}
        for facing, y in ROTATION.items():
            for part in ('foot', 'head'):
                for occupied in ('false', 'true'):
                    variants[f'facing={facing},occupied={occupied},part={part}'] = bf.variant(f'{name}_{part}', y=y)
        bf.blockstate(name, {'variants': variants})
        bf.loot(name, {'conditions': [{'block': ref(name), 'condition': 'minecraft:block_state_property',
                                       'properties': {'part': 'head'}}]})
        if name == 'bedroll':
            continue  # the red Bedroll keeps its item model, icon and recipe (3 wool over 3 leather)
        generated_item(name, ref('item/' + name))
        dye = BEDROLLS[name][1]
        bf.write(os.path.join(bf.DATA, bf.NS, 'recipes', name + '.json'), {
            'type': 'minecraft:crafting_shapeless', 'group': 'bedroll', 'category': 'misc',
            'ingredients': [{'tag': ref('bedrolls')}, {'item': 'minecraft:' + dye}],
            'result': {'item': ref(name)}})
    bf.write(os.path.join(bf.DATA, bf.NS, 'recipes', 'bedroll_from_dye.json'), {
        'type': 'minecraft:crafting_shapeless', 'group': 'bedroll', 'category': 'misc',
        'ingredients': [{'tag': ref('bedrolls')}, {'item': 'minecraft:red_dye'}],
        'result': {'item': ref('bedroll')}})
    bf.write(os.path.join(bf.DATA, bf.NS, 'tags/items/bedrolls.json'),
             {'replace': False, 'values': [ref(n) for n in BEDROLLS]})


def signpost_resources():
    post = {'uv': [7, 0, 9, 16], 'texture': '#post'}
    post_faces = {d: post for d in ('north', 'south', 'east', 'west')}
    post_faces['up'] = {'uv': [7, 7, 9, 9], 'texture': '#post'}
    post_faces['down'] = {'uv': [7, 7, 9, 9], 'texture': '#post', 'cullface': 'down'}

    def board(z1, z2, y1, y2):
        faces = {
            'east': {'uv': [16 - z2, 16 - y2, 16 - z1, 16 - y1], 'texture': '#board'},
            'west': {'uv': [z1, 16 - y2, z2, 16 - y1], 'texture': '#board'},
            'up': {'uv': [7, z1, 8, z2], 'texture': '#board'},
            'down': {'uv': [7, z1, 8, z2], 'texture': '#board'},
            'north': {'uv': [7, 16 - y2, 8, 16 - y1], 'texture': '#board'},
            'south': {'uv': [7, 16 - y2, 8, 16 - y1], 'texture': '#board'},
        }
        return box([7.5, y1, z1], [8.5, y2, z2], faces)

    # Pointing north: the post at the tail end (z 12-14), the board from the tail (z 16) to the tip (z 0).
    elements = [box([7, 0, 12], [9, 16, 14], post_faces), board(3, 16, 8, 15), board(2, 3, 9, 14),
                board(1, 2, 10, 13), board(0, 1, 11, 12)]
    model('adventurers_signpost', {'parent': 'minecraft:block/block', 'textures': {
        'particle': ref('block/adventurers_signpost_board'), 'post': ref('block/adventurers_signpost_post'),
        'board': ref('block/adventurers_signpost_board')}, 'elements': elements})
    bf.blockstate('adventurers_signpost', {'variants': {
        f'facing={f}': bf.variant('adventurers_signpost', y=y) for f, y in ROTATION.items()}})
    generated_item('adventurers_signpost', ref('item/adventurers_signpost'))
    bf.loot('adventurers_signpost')
    bf.write(os.path.join(bf.DATA, bf.NS, 'recipes', 'adventurers_signpost.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc',
        'key': {'#': {'tag': 'minecraft:planks'}, '|': {'item': 'minecraft:stick'}},
        'pattern': ['###', ' | ', ' | '], 'result': {'item': ref('adventurers_signpost')}, 'show_notification': True})


def poster_resources():
    variants = {}
    for i in range(len(SKETCHES)):
        tex = ref(f'block/wanted_poster_{i}')
        model(f'wanted_poster_{i}', {'parent': 'minecraft:block/block', 'textures': {'particle': tex, 'poster': tex},
                                     'elements': [box([1, 1, 15.8], [15, 15, 16], {
                                         'north': {'uv': [1, 1, 15, 15], 'texture': '#poster'},
                                         'south': {'uv': [1, 1, 15, 15], 'texture': '#poster', 'cullface': 'south'}})]})
        for facing, y in ROTATION.items():
            variants[f'facing={facing},sketch={i}'] = bf.variant(f'wanted_poster_{i}', y=y)
    bf.blockstate('wanted_poster', {'variants': variants})
    generated_item('wanted_poster', ref('block/wanted_poster_0'))
    bf.loot('wanted_poster')
    bf.write(os.path.join(bf.DATA, bf.NS, 'recipes', 'wanted_poster.json'), {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc',
        'ingredients': [{'item': 'minecraft:paper'}, {'item': 'minecraft:red_dye'}, {'item': 'minecraft:charcoal'}],
        'result': {'item': ref('wanted_poster'), 'count': 2}})


def plank_faces(frm, to, cull=()):
    """Faces for a plank box, uv from its own position so the grain lines up across boxes."""
    x1, y1, z1 = frm
    x2, y2, z2 = to
    faces = {
        'north': {'uv': [16 - x2, 16 - y2, 16 - x1, 16 - y1]}, 'south': {'uv': [x1, 16 - y2, x2, 16 - y1]},
        'west': {'uv': [z1, 16 - y2, z2, 16 - y1]}, 'east': {'uv': [16 - z2, 16 - y2, 16 - z1, 16 - y1]},
        'up': {'uv': [x1, z1, x2, z2]}, 'down': {'uv': [x1, 16 - z2, x2, 16 - z1]},
    }
    for face in faces.values():
        face['texture'] = '#planks'
    for side in cull:
        faces[side]['cullface'] = side
    return box(frm, to, faces)


TABLE_TOP = plank_faces([0, 13, 0], [16, 16, 16], ('up',))
TABLE_LEG = plank_faces([1, 0, 1], [4, 13, 4], ('down',))
BENCH_SEAT = plank_faces([3, 6, 0], [13, 9, 16])
BENCH_LEG = plank_faces([4, 0, 1], [12, 6, 3], ('down',))


def furniture_resources():
    template = {'parent': 'minecraft:block/block', 'textures': {'particle': '#planks'}}
    model('template_tavern_table_top', {**template, 'elements': [TABLE_TOP]})
    model('template_tavern_table_leg', {**template, 'elements': [TABLE_LEG]})
    legs = [TABLE_LEG, plank_faces([12, 0, 1], [15, 13, 4], ('down',)), plank_faces([1, 0, 12], [4, 13, 15], ('down',)),
            plank_faces([12, 0, 12], [15, 13, 15], ('down',))]
    model('template_tavern_table_inventory', {**template, 'elements': [TABLE_TOP] + legs})
    model('template_tavern_bench_seat', {**template, 'elements': [BENCH_SEAT]})
    model('template_tavern_bench_leg', {**template, 'elements': [BENCH_LEG]})
    # The inventory bench runs east-west, as one placed facing north does.
    model('template_tavern_bench_inventory', {**template, 'elements': [
        plank_faces([0, 6, 3], [16, 9, 13]), plank_faces([1, 0, 4], [3, 6, 12], ('down',)),
        plank_faces([13, 0, 4], [15, 6, 12], ('down',))]})
    for wood, (texture, planks, slab, _) in WOODS.items():
        table, bench = f'{wood}_tavern_table', f'{wood}_tavern_bench'
        for part in ('top', 'leg', 'inventory'):
            model(f'{table}_{part}', {'parent': ref(f'block/template_tavern_table_{part}'),
                                      'textures': {'planks': texture}})
        for part in ('seat', 'leg', 'inventory'):
            model(f'{bench}_{part}', {'parent': ref(f'block/template_tavern_bench_{part}'),
                                      'textures': {'planks': texture}})
        # Table legs stand at the corners with neither side beside them joined (the leg model is the north-west).
        parts = [{'apply': bf.variant(f'{table}_top')}]
        for (a, b), y in ((('north', 'west'), 0), (('north', 'east'), 90), (('south', 'east'), 180),
                          (('south', 'west'), 270)):
            parts.append({'apply': bf.variant(f'{table}_leg', y=y), 'when': {a: 'false', b: 'false'}})
        bf.blockstate(table, {'multipart': parts})
        # Bench seat along its axis; a leg at each end that isn't joined (the leg model is the north end).
        parts = [{'apply': bf.variant(f'{bench}_seat'), 'when': {'axis': 'z'}},
                 {'apply': bf.variant(f'{bench}_seat', y=90), 'when': {'axis': 'x'}}]
        for axis, side, y in (('z', 'north', 0), ('z', 'south', 180), ('x', 'east', 90), ('x', 'west', 270)):
            parts.append({'apply': bf.variant(f'{bench}_leg', y=y), 'when': {'axis': axis, side: 'false'}})
        bf.blockstate(bench, {'multipart': parts})
        item(table, {'parent': ref(f'block/{table}_inventory')})
        item(bench, {'parent': ref(f'block/{bench}_inventory')})
        bf.loot(table)
        bf.loot(bench)
        bf.shaped(table, ['SSS', 'P P'], {'S': slab, 'P': planks}, ref(table), 1, 'misc')
        bf.shaped(bench, ['SSS', '| |'], {'S': slab, '|': 'minecraft:stick'}, ref(bench), 2, 'misc')


def resources():
    bedroll_resources()
    signpost_resources()
    poster_resources()
    furniture_resources()

    bf.add_to_tag('blocks', 'mineable/axe', [ref('adventurers_signpost'), ref('wanted_poster')]
                  + [ref(f'{w}_tavern_{k}') for w in WOODS for k in ('table', 'bench')])

    names = {
        'block.dndclasses.bedroll': 'Bedroll',
        'block.dndclasses.bedroll.cant_sleep_here': "You can't sleep here: the ground is too restless.",
        'item.dndclasses.bedroll.place': 'Use it anywhere else to lay it out: sleep in it like a bed (no spawn point).',
        'block.dndclasses.adventurers_signpost': "Adventurer's Signpost",
        'block.dndclasses.adventurers_signpost.blank': 'A blank signpost. Name it in an anvil before you put it up.',
        'block.dndclasses.adventurers_signpost.cleared': 'Cleared',
        'block.dndclasses.wanted_poster': 'Wanted Poster',
        'block.dndclasses.wanted_poster.read.0': 'WANTED: Skarnak the goblin chief, for raiding the Bywater road. '
                                                 'Reward: 40 gold.',
        'block.dndclasses.wanted_poster.read.1': 'WANTED, dead or alive: Grimtusk One-Eye, orc raider. Reward: 75 gold.',
        'block.dndclasses.wanted_poster.read.2': 'WANTED: Black Ned, highwayman and horse thief. Reward: 30 gold.',
        'block.dndclasses.wanted_poster.read.3': 'WANTED: the Owlbear of Hollowmere Wood. Reward: 100 gold and a '
                                                 'free pint.',
    }
    for name, (_, _, title) in BEDROLLS.items():
        names[f'block.dndclasses.{name}'] = title
    for wood, (_, _, _, title) in WOODS.items():
        names[f'block.dndclasses.{wood}_tavern_table'] = f'{title} Tavern Table'
        names[f'block.dndclasses.{wood}_tavern_bench'] = f'{title} Tavern Bench'
    lang(names)


def lang(entries):
    """Appends missing keys to en_us.json before its closing brace (as block_families.gen_lang does)."""
    path = os.path.join(bf.ASSETS, 'lang/en_us.json')
    with open(path) as fh:
        text = fh.read()
    current = bf.json.loads(text)
    lines = text.rstrip().split('\n')
    for key, value in entries.items():
        if key in current:
            continue
        if not lines[-2].rstrip().endswith(','):
            lines[-2] = lines[-2].rstrip() + ','
        lines.insert(len(lines) - 1, f'    {bf.json.dumps(key)}: {bf.json.dumps(value)}')
    with open(path, 'w') as fh:
        fh.write('\n'.join(lines) + '\n')
    bf.json.loads(open(path).read())


def main():
    textures()
    resources()
    print('wrote the shared adventuring block textures and resources')


if __name__ == '__main__':
    main()
