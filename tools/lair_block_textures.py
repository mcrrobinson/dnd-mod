"""Draws the Dragon & Frost lairs building-block textures and writes the card's one-off blocks' resources
(Building Blocks epic).

    python3 tools/lair_block_textures.py [preview_dir]

Families (resources from tools/block_families.py, textures here):
- fulgurite: dark glassy rock with branching tan and white streaks of fused sand.
- rimestone, rimestone_bricks: grey stone with frost-white crystals and pale blue ice in the cracks.
- dragon_nest (sides) and dragon_nest_top: woven branches with bones and a few scales; the top a basket weave.

One-off blocks (textures and resources here, registered in LairBlocks.java):
- fulgurite_spike: a cross model, a branching tube of fused sand.
- storm_glass (animated: white cracks of lightning flicker across dark blue-grey glass) and storm_glass_pane
  (storm_glass_pane_top for its edge).
- hoarfrost_crystal: pale blue-white ice crystals, an amethyst-like cluster.
- storm_scale_pile / frost_scale_pile (and their _side textures): layers like snow, yellow-and-blue or pale blue
  scales; they drop the items storm_dragon_scale / frost_dragon_scale (textures/item).
- red_scale_pile: the Ancient Red's crimson and ember scales, on its caldera floor; it drops 0-1 Ancient Red Scale
  (the boss's item, textures from tools/boss_item_textures.py), whatever its layers.

With a preview_dir it also writes each texture upscaled to 256 px (and 3x3 tiled) there, for checking by eye.
Safe to rerun.
"""
import json
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
SAVED = []


def c(rgb, d=0, a=255):
    return tuple(max(0, min(255, v + d)) for v in rgb[:3]) + (a,)


def grid(fill=CLEAR):
    return [[fill] * N for _ in range(N)]


def save(name, rows, folder=TEX):
    os.makedirs(folder, exist_ok=True)
    pngio.write_png(os.path.join(folder, name + '.png'), N, len(rows), rows)
    SAVED.append((name, rows))


# --- Fulgurite ---------------------------------------------------------------------------------------------

GLASS_ROCK = (46, 43, 48)
SAND = (192, 160, 108)
SAND_DARK = (146, 116, 76)
FUSED = (236, 226, 198)
WHITE = (250, 248, 240)


def jagged(rng, x, y, steps, dx_choices, dy):
    """A lightning-like path: each step goes dy down/up and wanders sideways. Returns (x, y) points."""
    points = []
    for _ in range(steps):
        points.append((x % N, y % N))
        x += rng.choice(dx_choices)
        y += dy
    return points


def fulgurite():
    rng = random.Random(7)
    rows = grid()
    for y in range(N):
        for x in range(N):
            d = rng.randint(-5, 5)
            # Glassy sheen: a few lighter, bluish specks, and darker pits.
            r = rng.random()
            if r < 0.07:
                rows[y][x] = c((70, 72, 86), rng.randint(-4, 10))
                continue
            if r < 0.15:
                d -= 12
            rows[y][x] = c(GLASS_ROCK, d)
    # Thin branching streaks of fused sand, wrapping round the edges so the texture tiles.
    trunk = jagged(rng, 4, 0, 16, (0, 1, -1, 1, 0), 1)
    branch1 = jagged(rng, trunk[6][0] + 1, 7, 5, (1, 1, 2), 1)
    branch2 = jagged(rng, trunk[11][0] - 1, 12, 4, (-1, -2, -1), 1)
    twig = jagged(rng, 13, 2, 5, (0, -1, 1), 1)
    for path, core in ((trunk, WHITE), (branch1, FUSED), (branch2, FUSED), (twig, SAND)):
        for i, (x, y) in enumerate(path):
            # A tan crust down one side of the fused core, now and then.
            if rng.random() < 0.45:
                nx = (x + 1) % N
                if rows[y][nx][0] < 120:
                    rows[y][nx] = c(SAND_DARK, rng.randint(-10, 4))
            rows[y][x] = c(core if i % 4 != 3 else SAND, rng.randint(-6, 0))
    return rows


def fulgurite_spike():
    """A cross model: a fused tube standing up from the ground, with two branches."""
    rng = random.Random(11)
    rows = grid()

    def tube(points, width):
        for i, (x, y) in enumerate(points):
            for ox in range(width):
                px = x + ox
                if 0 <= px < N and 0 <= y < N:
                    edge = ox == 0 or ox == width - 1
                    if edge:
                        col = c(SAND_DARK if ox == width - 1 else SAND, rng.randint(-10, 6))
                    else:
                        r = rng.random()
                        col = c(FUSED, rng.randint(-10, 0)) if r < 0.45 else c(GLASS_ROCK, rng.randint(-4, 18)) \
                            if r < 0.7 else c(SAND, rng.randint(-4, 12))
                    rows[y][px] = col

    # The trunk, 4 wide at the foot narrowing to 2 at the tip, wandering a little.
    x = 6
    for y in range(15, 1, -1):
        width = 4 if y > 9 else 3 if y > 4 else 2
        tube([(x, y)], width)
        if y in (11, 6):
            x += rng.choice((-1, 1))
    tube([(9 + (15 - y) // 2, y) for y in range(12, 6, -1)], 2)
    tube([(4 - (13 - y) // 2, y) for y in range(13, 8, -1)], 2)
    # Bright fused tips.
    for (tx, ty) in ((x, 2), (x + 1, 2)):
        if rows[ty][tx][3]:
            rows[ty][tx] = c(WHITE)
    return rows


# --- Storm Glass -------------------------------------------------------------------------------------------

STORM = (54, 66, 92)
STORM_FRAME = (88, 100, 128)
BOLT = (244, 248, 255)
GLOW = (164, 196, 255)


def storm_glass_frame(bolts, bright):
    """One frame: dark blue-grey glass in a darker frame, with the bolt paths drawn at brightness 0-2."""
    rows = grid()
    for y in range(N):
        for x in range(N):
            if x in (0, 15) or y in (0, 15):
                rows[y][x] = c(STORM_FRAME, -18 if x == 15 or y == 15 else 0, 230)
            else:
                rows[y][x] = c(STORM, ((x * 7 + y * 3) % 5) - 2, 150)
    if bright == 0:
        return rows
    for path in bolts:
        for (x, y) in path:
            if not (0 < x < 15 and 0 < y < 15):
                continue
            for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + ox, y + oy
                if 0 < nx < 15 and 0 < ny < 15 and rows[ny][nx][0] < 120:
                    rows[ny][nx] = c(GLOW, -30 if bright == 1 else 0, 120 if bright == 1 else 170)
            rows[y][x] = c(BOLT, -40 if bright == 1 else 0, 190 if bright == 1 else 245)
    return rows


def storm_glass():
    """Four stacked frames for the animation (see storm_glass.png.mcmeta)."""
    rng = random.Random(23)
    a = [jagged(rng, 4, 1, 14, (0, 1, -1, 1), 1), jagged(rng, 7, 6, 6, (1, 1, 0), 1)]
    b = [jagged(rng, 11, 1, 14, (0, -1, 1, -1), 1), jagged(rng, 9, 9, 5, (-1, -1, 0), 1)]
    a = [[(x, y) for (x, y) in p] for p in a]
    frames = [storm_glass_frame(a + b, 1), storm_glass_frame(a, 2), storm_glass_frame(b, 2),
              storm_glass_frame(a + b, 2)]
    return [row for frame in frames for row in frame]


def storm_glass_mcmeta():
    frames = [{'index': 0, 'time': 40}, {'index': 1, 'time': 2}, {'index': 0, 'time': 3}, {'index': 1, 'time': 2},
              {'index': 0, 'time': 30}, {'index': 2, 'time': 3}, {'index': 3, 'time': 2}, {'index': 0, 'time': 25},
              {'index': 3, 'time': 2}, {'index': 2, 'time': 2}]
    with open(os.path.join(TEX, 'storm_glass.png.mcmeta'), 'w') as fh:
        json.dump({'animation': {'interpolate': False, 'frames': frames}}, fh, indent=2)
        fh.write('\n')


def storm_pane_top():
    rows = grid()
    for y in range(N):
        for x in range(N):
            rows[y][x] = c(STORM_FRAME, -12 if x in (0, 15) else 0, 230)
    return rows


# --- Rimestone ---------------------------------------------------------------------------------------------

RIME = (122, 124, 130)
ICE = (150, 200, 232)
ICE_LIGHT = (196, 228, 248)
FROST = (238, 246, 252)


def frost_specks(rows, rng, count, mask=None):
    for _ in range(count):
        x, y = rng.randrange(N), rng.randrange(N)
        if mask and not mask(x, y):
            continue
        rows[y][x] = c(FROST, rng.randint(-8, 0))
        if rng.random() < 0.5:
            nx = (x + 1) % N
            if not mask or mask(nx, y):
                rows[y][nx] = c(ICE_LIGHT, rng.randint(-8, 4))


def rimestone():
    rng = random.Random(31)
    rows = grid()
    for y in range(N):
        for x in range(N):
            d = rng.randint(-8, 8)
            if rng.random() < 0.1:
                d -= 14
            rows[y][x] = c(RIME, d)
    # Cracks filled with pale blue ice, frost crystals clustered along them.
    crack1 = jagged(rng, 2, 3, 9, (1, 1, 0), 1)
    crack2 = [(x, (10 + (x // 3) % 2 - (1 if x > 11 else 0)) % N) for x in range(6, 16)]
    crack3 = jagged(rng, 12, 0, 5, (0, -1, 1), 1)
    for path in (crack1, crack2, crack3):
        for i, (x, y) in enumerate(path):
            rows[y][x] = c(ICE if i % 3 else ICE_LIGHT, rng.randint(-6, 6))
            below = (y + 1) % N
            if rows[below][x][2] < 160:
                rows[below][x] = c(RIME, -26)
            if rng.random() < 0.35:
                ox = (x + rng.choice((-1, 1))) % N
                oy = (y + rng.choice((-1, 1))) % N
                rows[oy][ox] = c(FROST, rng.randint(-10, 0))
    frost_specks(rows, rng, 7)
    return rows


def rimestone_bricks():
    rng = random.Random(37)
    rows = grid()
    for y in range(N):
        course = y // 4
        offset = 4 if course % 2 else 0
        for x in range(N):
            bx = (x + offset) % 8
            by = y % 4
            if by == 3 or bx == 7:
                # Mortar joints full of pale ice, with frost in places.
                r = rng.random()
                rows[y][x] = c(ICE, rng.randint(-24, -8)) if r < 0.35 else c(FROST, -10) if r < 0.45 \
                    else c(RIME, rng.randint(-46, -38))
                continue
            d = (-3, 4, 1, -5)[(((x + offset) % N) // 8 + course) % 4] + rng.randint(-6, 6)
            if by == 0 or bx == 0:
                d += 10
            elif by == 2 and bx == 6:
                d -= 8
            rows[y][x] = c(RIME, d)
    frost_specks(rows, rng, 9, mask=lambda x, y: y % 4 != 3)
    return rows


# --- Hoarfrost Crystal -------------------------------------------------------------------------------------

def hoarfrost_crystal():
    """Shards of ice rising from the foot like an amethyst cluster; a cross model."""
    rows = grid()
    # (centre x, height, half width at the foot)
    shards = [(8, 14, 2), (4, 9, 1), (12, 10, 1), (6, 6, 1), (11, 6, 1)]
    for (cx, h, w) in sorted(shards, key=lambda s: -s[1]):
        top = 16 - h
        for y in range(top, 16):
            t = (y - top) / max(1, h - 1)
            half = 0 if y == top else max(1, round(w * min(1.0, t * 2.0)))
            for x in range(cx - half, cx + half + 1):
                if not 0 <= x < N:
                    continue
                if y == top or (y == top + 1 and x == cx):
                    col = c(FROST)
                elif x == cx - half:
                    col = c(FROST, -6)
                elif x == cx + half:
                    col = c(ICE, -22)
                elif x < cx:
                    col = c(ICE_LIGHT)
                else:
                    col = c(ICE, 6)
                rows[y][x] = col
    return rows


# --- Dragon Nest -------------------------------------------------------------------------------------------

TWIG = (110, 78, 48)
TWIG_LIGHT = (142, 104, 64)
GAP = (44, 32, 24)
BONE = (228, 220, 194)
SCALE_TEAL = (72, 120, 136)
SCALE_GOLD = (204, 168, 72)


def dragon_nest_side():
    rng = random.Random(41)
    rows = grid()
    for y in range(N):
        for x in range(N):
            rows[y][x] = c(GAP, rng.randint(-4, 4))
    # Branches lying roughly level, each 2 px thick, wavering by a pixel.
    for i, y0 in enumerate((1, 5, 9, 13)):
        shift = rng.randint(0, 15)
        for x in range(N):
            y = y0 + (1 if ((x + shift) // 5) % 2 else 0)
            rows[y % N][x] = c(TWIG_LIGHT, rng.randint(-8, 8))
            rows[(y + 1) % N][x] = c(TWIG, rng.randint(-8, 4))
            # A gap now and then where one branch ends and the next begins.
            if (x + shift + i * 3) % 11 == 0:
                rows[y % N][x] = c(GAP, 6)
    # A few twigs slanting through.
    for (x0, y0) in ((3, 0), (12, 6)):
        for k in range(6):
            x, y = (x0 + k // 2) % N, (y0 + k) % N
            rows[y][x] = c(TWIG, 14)
    # A bone across the weave, and a few scales caught in it.
    for k in range(5):
        rows[(10 - k // 2) % N][(5 + k) % N] = c(BONE, -10 if k % 2 else 0)
    rows[11][4] = c(BONE, -20)
    rows[8][10] = c(BONE, -14)
    for (x, y, col) in ((13, 3, SCALE_TEAL), (14, 3, SCALE_TEAL), (2, 12, SCALE_GOLD), (9, 15, SCALE_TEAL)):
        rows[y][x] = c(col, rng.randint(-6, 6))
    return rows


def dragon_nest_top():
    """A basket weave: 4x4 squares of two twigs each, alternately running across and along, with a bone and
    scales caught in it."""
    rng = random.Random(43)
    rows = grid()
    for y in range(N):
        for x in range(N):
            across = ((x // 4) + (y // 4)) % 2 == 0
            k = (y % 4) if across else (x % 4)   # position across the twig pair
            along = (x % 4) if across else (y % 4)  # position along the twig
            d = rng.randint(-3, 3) + (18 if k % 2 == 0 else -12)
            if along in (0, 3):
                d -= 34  # tucked under the neighbouring square
            rows[y][x] = c(TWIG_LIGHT, d)
    for k in range(6):
        rows[(3 + k) % N][(9 + k // 2) % N] = c(BONE, -8 if k % 2 else 0)
    for (x, y, col) in ((2, 1, SCALE_TEAL), (13, 12, SCALE_GOLD), (5, 13, SCALE_TEAL), (6, 13, SCALE_GOLD)):
        rows[y][x] = c(col, rng.randint(-6, 6))
    return rows


# --- Shed scales -------------------------------------------------------------------------------------------

STORM_SCALES = ((222, 188, 70), (78, 124, 200))
FROST_SCALES = ((182, 216, 238), (216, 236, 250))
RED_SCALES = ((164, 30, 26), (226, 104, 34))


# The rim of one scale in its 8x4 cell: a U, open at the top where it tucks under the two scales above.
SCALE_RIM = {(0, 0), (0, 1), (1, 2), (2, 3), (3, 3), (4, 3), (5, 3), (6, 2), (7, 1), (7, 0)}
# The corners of the cell outside the U, below its rim.
SCALE_OUTSIDE = {(0, 2), (0, 3), (1, 3), (6, 3), (7, 3), (7, 2)}


def scales(colours, seed, rim):
    """Overlapping rounded scales, 8 px wide in rows 4 px apart, each row offset by half a scale: a dark U-shaped
    rim round each scale's foot, the scale darker at the top where it tucks under the row above and lighter on
    its left."""
    rng = random.Random(seed)
    rows = grid()
    for y in range(N):
        row = y // 4
        for x in range(N):
            shifted = (x + 4 * (row % 2)) % N
            sx, sy = shifted % 8, y % 4
            d = rng.randint(-4, 4)
            if (sx, sy) in SCALE_RIM:
                rows[y][x] = c(rim, d)
                continue
            if (sx, sy) in SCALE_OUTSIDE:
                # The top of a scale in the row below, tucked under this one.
                below = (row + 1) % 4
                k = ((x + 4 * (below % 2)) % N) // 8
                rows[y][x] = c(colours[1] if (k + below * 3) % 5 in (1, 3) else colours[0], d - 18)
                continue
            k = shifted // 8
            colour = colours[1] if (k + row * 3) % 5 in (1, 3) else colours[0]
            d += -16 + 9 * sy + (10 if sx <= 2 else -8 if sx >= 6 else 0)
            rows[y][x] = c(colour, d)
    return rows


def scale_item(colours, rim):
    """A single scale: a rounded shield with a ridge down the middle."""
    rows = grid()
    widths = [0, 0, 3, 5, 7, 8, 9, 9, 9, 9, 8, 7, 6, 4, 2, 0]
    for y, w in enumerate(widths):
        if not w:
            continue
        x0 = 8 - (w + 1) // 2
        for x in range(x0, x0 + w):
            edge = x in (x0, x0 + w - 1) or y in (2, 14) or widths[y + 1] < w and (x <= x0 + 1 or x >= x0 + w - 2)
            if edge:
                rows[y][x] = c(rim)
            elif x == 7 or x == 8:
                rows[y][x] = c(colours[1] if x == 7 else colours[0], 30)
            else:
                rows[y][x] = c(colours[0], 10 if x < 7 else -14)
    return rows


# --- Resources of the one-off blocks -----------------------------------------------------------------------

def ref(path):
    return bf.ref(path)


def model(name, obj):
    bf.write(os.path.join(bf.ASSETS, 'models/block', name + '.json'), obj)


def item(name, obj):
    bf.write(os.path.join(bf.ASSETS, 'models/item', name + '.json'), obj)


def generated_item(name, texture):
    item(name, {'parent': 'minecraft:item/generated', 'textures': {'layer0': ref(texture)}})


SILK = {'condition': 'minecraft:match_tool', 'predicate': {
    'enchantments': [{'enchantment': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}


def layered(name, drop):
    """Layers 1-8, like snow (block textures <name> on top and <name>_side round it); drops {drop}, one per
    layer."""
    variants = {}
    for layers in range(1, 9):
        h = 2 * layers
        mdl = f'{name}_height{h}' if layers < 8 else f'{name}_block'
        faces = {'down': {'texture': '#top', 'cullface': 'down'}, 'up': {'texture': '#top'}}
        for side in ('north', 'south', 'west', 'east'):
            faces[side] = {'uv': [0, 16 - h, 16, 16], 'texture': '#side', 'cullface': side}
        if layers == 8:
            faces['up']['cullface'] = 'up'
        model(mdl, {'parent': 'minecraft:block/block',
                    'textures': {'particle': ref('block/' + name), 'top': ref('block/' + name),
                                 'side': ref(f'block/{name}_side')},
                    'elements': [{'from': [0, 0, 0], 'to': [16, h, 16], 'faces': faces}]})
        variants[f'layers={layers}'] = bf.variant(mdl)
    bf.blockstate(name, {'variants': variants})
    item(name, {'parent': ref(f'block/{name}_height2')})
    functions = [{'function': 'minecraft:set_count', 'add': False, 'count': float(layers),
                  'conditions': [{'condition': 'minecraft:block_state_property', 'block': ref(name),
                                  'properties': {'layers': str(layers)}}]} for layers in range(1, 9)]
    functions.append({'function': 'minecraft:explosion_decay'})
    bf.write(os.path.join(bf.DATA, bf.NS, 'loot_tables/blocks', name + '.json'), {
        'type': 'minecraft:block',
        'pools': [{'bonus_rolls': 0.0, 'rolls': 1.0, 'entries': [
            {'type': 'minecraft:item', 'name': ref(drop), 'functions': functions}]}]})


def resources():
    # Fulgurite spike: a cross model.
    model('fulgurite_spike', {'parent': 'minecraft:block/cross', 'textures': {'cross': ref('block/fulgurite_spike')}})
    bf.blockstate('fulgurite_spike', {'variants': {'': bf.variant('fulgurite_spike')}})
    generated_item('fulgurite_spike', 'block/fulgurite_spike')
    bf.loot('fulgurite_spike')

    # Storm glass and its pane (vanilla's glass pane models); like glass, only silk touch keeps them.
    model('storm_glass', {'parent': 'minecraft:block/cube_all', 'textures': {'all': ref('block/storm_glass')}})
    bf.blockstate('storm_glass', {'variants': {'': bf.variant('storm_glass')}})
    item('storm_glass', {'parent': ref('block/storm_glass')})
    pane, edge = ref('block/storm_glass'), ref('block/storm_glass_pane_top')
    for part in ('post', 'side', 'side_alt'):
        model('storm_glass_pane_' + part, {'parent': 'minecraft:block/template_glass_pane_' + part,
                                           'textures': {'edge': edge, 'pane': pane}})
    for part in ('noside', 'noside_alt'):
        model('storm_glass_pane_' + part, {'parent': 'minecraft:block/template_glass_pane_' + part,
                                           'textures': {'pane': pane}})
    parts = [{'apply': bf.variant('storm_glass_pane_post')}]
    for side, mdl, y in (('north', 'side', 0), ('east', 'side', 90), ('south', 'side_alt', 0),
                         ('west', 'side_alt', 90)):
        parts.append({'apply': bf.variant('storm_glass_pane_' + mdl, y=y), 'when': {side: 'true'}})
    for side, mdl, y in (('north', 'noside', 0), ('east', 'noside_alt', 0), ('south', 'noside_alt', 90),
                         ('west', 'noside', 270)):
        parts.append({'apply': bf.variant('storm_glass_pane_' + mdl, y=y), 'when': {side: 'false'}})
    bf.blockstate('storm_glass_pane', {'multipart': parts})
    generated_item('storm_glass_pane', 'block/storm_glass_pane_item')
    for name in ('storm_glass', 'storm_glass_pane'):
        bf.loot(name, {'conditions': [SILK]}, pool_conditions=False)

    # Hoarfrost crystal: a cross model turned to the face it grows from, like an amethyst cluster.
    model('hoarfrost_crystal', {'parent': 'minecraft:block/cross',
                                'textures': {'cross': ref('block/hoarfrost_crystal')}})
    turns = {'down': (180, 0), 'up': (0, 0), 'north': (90, 0), 'south': (90, 180), 'east': (90, 90),
             'west': (90, 270)}
    bf.blockstate('hoarfrost_crystal', {'variants': {f'facing={f}': bf.variant('hoarfrost_crystal', x=x, y=y)
                                                     for f, (x, y) in turns.items()}})
    generated_item('hoarfrost_crystal', 'block/hoarfrost_crystal')
    bf.loot('hoarfrost_crystal')

    # Shed scales piles.
    layered('storm_scale_pile', 'storm_dragon_scale')
    layered('frost_scale_pile', 'frost_dragon_scale')
    layered('red_scale_pile', 'ancient_red_scale')
    # The Ancient Red's scales are a boss reward, so a pile only gives up one now and then, whatever its size.
    bf.write(os.path.join(bf.DATA, bf.NS, 'loot_tables/blocks', 'red_scale_pile.json'), {
        'type': 'minecraft:block',
        'pools': [{'bonus_rolls': 0.0, 'rolls': 1.0, 'entries': [
            {'type': 'minecraft:item', 'name': ref('ancient_red_scale'), 'functions': [
                {'function': 'minecraft:set_count', 'add': False,
                 'count': {'type': 'minecraft:uniform', 'min': 0.0, 'max': 1.0}},
                {'function': 'minecraft:explosion_decay'}]}]}]})
    generated_item('storm_dragon_scale', 'item/storm_dragon_scale')
    generated_item('frost_dragon_scale', 'item/frost_dragon_scale')

    # Recipes. The families' own come from block_families.py.
    bf.shaped('storm_glass', ['###', '#S#', '###'], {'#': 'minecraft:glass', 'S': ref('storm_dragon_scale')},
              ref('storm_glass'), 8)
    bf.shaped('storm_glass_pane', ['###', '###'], {'#': ref('storm_glass')}, ref('storm_glass_pane'), 16,
              'decorations')
    bf.shaped('hoarfrost_crystal', ['S', 'I'], {'S': ref('frost_dragon_scale'), 'I': 'minecraft:blue_ice'},
              ref('hoarfrost_crystal'), 4, 'decorations')
    bf.shaped('fulgurite_spike', ['#', '#'], {'#': ref('fulgurite')}, ref('fulgurite_spike'), 4, 'decorations')
    for pile, scale in (('storm_scale_pile', 'storm_dragon_scale'), ('frost_scale_pile', 'frost_dragon_scale')):
        bf.shaped(pile, ['###'], {'#': ref(scale)}, ref(pile), 3)
    for result, count in (('rimestone_bricks', 1), ('rimestone_brick_stairs', 1), ('rimestone_brick_slab', 2),
                          ('rimestone_brick_wall', 1)):
        bf.stonecut(f'{result}_from_rimestone_stonecutting', ref('rimestone'), ref(result), count)

    # Tags.
    bf.add_to_tag('blocks', 'mineable/pickaxe', [ref(n) for n in ('fulgurite_spike', 'hoarfrost_crystal')])
    bf.add_to_tag('blocks', 'mineable/shovel', [ref(n) for n in ('storm_scale_pile', 'frost_scale_pile', 'red_scale_pile')])
    bf.add_to_tag('blocks', 'impermeable', [ref('storm_glass')])

    lang({
        'block.dndclasses.fulgurite_spike': 'Fulgurite Spike',
        'block.dndclasses.storm_glass': 'Storm Glass',
        'block.dndclasses.storm_glass_pane': 'Storm Glass Pane',
        'block.dndclasses.hoarfrost_crystal': 'Hoarfrost Crystal',
        'block.dndclasses.storm_scale_pile': 'Shed Storm Scales',
        'block.dndclasses.frost_scale_pile': 'Shed Frost Scales',
        'block.dndclasses.red_scale_pile': 'Shed Red Scales',
        'item.dndclasses.storm_dragon_scale': 'Storm Dragon Scale',
        'item.dndclasses.frost_dragon_scale': 'Frost Dragon Scale',
    })


def lang(entries):
    """Appends missing keys to en_us.json before its closing brace (as block_families.gen_lang does)."""
    path = os.path.join(bf.ASSETS, 'lang/en_us.json')
    with open(path) as fh:
        text = fh.read()
    current = json.loads(text)
    lines = text.rstrip().split('\n')
    for key, value in entries.items():
        if key in current:
            continue
        if not lines[-2].rstrip().endswith(','):
            lines[-2] = lines[-2].rstrip() + ','
        lines.insert(len(lines) - 1, f'    {json.dumps(key)}: {json.dumps(value)}')
    with open(path, 'w') as fh:
        fh.write('\n'.join(lines) + '\n')
    json.loads(open(path).read())


def textures():
    save('fulgurite', fulgurite())
    save('fulgurite_spike', fulgurite_spike())
    glass = storm_glass()
    save('storm_glass', glass)
    storm_glass_mcmeta()
    save('storm_glass_pane_item', glass[16:32])
    save('storm_glass_pane_top', storm_pane_top())
    save('rimestone', rimestone())
    save('rimestone_bricks', rimestone_bricks())
    save('hoarfrost_crystal', hoarfrost_crystal())
    save('dragon_nest', dragon_nest_side())
    save('dragon_nest_top', dragon_nest_top())
    storm_rim, frost_rim = (52, 70, 116), (92, 132, 172)
    storm = scales(STORM_SCALES, 51, storm_rim)
    frost = scales(FROST_SCALES, 53, frost_rim)
    save('storm_scale_pile', storm)
    save('storm_scale_pile_side', storm[2:] + storm[:2])
    save('frost_scale_pile', frost)
    save('frost_scale_pile_side', frost[2:] + frost[:2])
    red = scales(RED_SCALES, 59, (58, 14, 12))
    save('red_scale_pile', red)
    save('red_scale_pile_side', red[2:] + red[:2])
    save('storm_dragon_scale', scale_item(STORM_SCALES, storm_rim), ITEM_TEX)
    save('frost_dragon_scale', scale_item(FROST_SCALES, frost_rim), ITEM_TEX)


def preview(folder):
    """Each texture at 16x scale, and opaque ones tiled 3x3, on a mid-grey background."""
    os.makedirs(folder, exist_ok=True)
    for name, rows in SAVED:
        h = len(rows)
        for tiles in (1, 3):
            if tiles == 3 and (h != N or any(p[3] < 255 for r in rows for p in r)):
                continue
            s = 16 if tiles == 1 else 8
            out = []
            for y in range(h * tiles * s):
                line = []
                for x in range(N * tiles * s):
                    p = rows[(y // s) % h][(x // s) % N]
                    a = p[3] / 255
                    line.append(tuple(int(p[i] * a + 128 * (1 - a)) for i in range(3)) + (255,))
                out.append(line)
            pngio.write_png(os.path.join(folder, f'{name}{"_tiled" if tiles == 3 else ""}.png'), N * tiles * s,
                            h * tiles * s, out)


def main():
    textures()
    resources()
    if len(sys.argv) > 1:
        preview(sys.argv[1])
    print('wrote the Dragon & Frost lair block textures and resources')


if __name__ == '__main__':
    main()
