"""Writes the resources of the decorative block families (the Building Blocks epic).

    python3 tools/block_families.py

Each family in FAMILIES mirrors one in ModBlockFamilies.java (same name, stem and shapes). For each block it
writes the blockstate, block and item models, loot table (slabs drop 2 when double, doors only from the lower
half), recipes (crafting for every shape, stonecutting for stone families) and lang entry, and adds the
block to the mineable and shape tags (stairs, slabs, walls, ...) for blocks and items. Rerun it after
changing FAMILIES; it only touches the families' own files and entries.

Textures are drawn by the area's own script (e.g. tools/dwarven_block_textures.py) under
textures/block/<name>.png, see docs/dev/building-blocks.md for the names each shape needs.
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
NS = 'dndclasses'
ASSETS = os.path.join(ROOT, 'src/main/resources/assets', NS)
DATA = os.path.join(ROOT, 'src/main/resources/data')

SHAPES = ['stairs', 'slab', 'wall', 'pillar', 'fence', 'fence_gate', 'door', 'trapdoor']
DIRECTIONS = ['east', 'north', 'south', 'west']
# Model y rotation of a shape whose unrotated model faces south (fence gates, trapdoors)
Y = {'south': 0, 'west': 90, 'north': 180, 'east': 270}


class Family:
    """
    name:     the base block's id path (runic_deepslate_bricks)
    title:    its English name (Runic Deepslate Bricks)
    shapes:   any of SHAPES
    material: 'stone' (pickaxe, stonecutter) or 'wood' (axe, wooden_* tags and recipe groups)
    stem:     the shapes' id prefix; default the name minus "s" for bricks/tiles, as in BlockFamily.Builder
    axis:     the base lies along an axis like a log (textures <name> and <name>_top)
    top, bottom: separate top/bottom textures for a cube base (cube_bottom_top; also used by stairs/slab)
    craft:    a recipe for the base from vanilla items: {'pattern': [...], 'key': {'#': 'minecraft:...'},
              'count': n}
    """

    def __init__(self, name, title, shapes, material='stone', stem=None, stem_title=None, axis=False, top=None,
                 bottom=None, craft=None):
        self.name = name
        self.title = title
        self.shapes = shapes
        self.material = material
        if stem is None:
            stem = name[:-1] if name.endswith('bricks') or name.endswith('tiles') else name
        self.stem = stem
        if stem_title is None:
            stem_title = title[:-1] if title.endswith('Bricks') or title.endswith('Tiles') else title
        self.stem_title = stem_title
        self.axis = axis
        self.top = top
        self.bottom = bottom
        self.craft = craft
        unknown = set(shapes) - set(SHAPES)
        if unknown:
            raise ValueError(f'{name}: unknown shapes {unknown}')

    def id(self, shape=None):
        return self.name if shape is None else f'{self.stem}_{shape}'

    def wooden(self):
        return self.material == 'wood'


FAMILIES = [
    Family('runic_deepslate_bricks', 'Runic Deepslate Bricks', ['stairs', 'slab', 'wall'],
           craft={'pattern': ['###', '#G#', '###'], 'key': {'#': 'minecraft:deepslate_bricks',
                                                           'G': 'minecraft:gold_nugget'}, 'count': 8}),
]


def ref(path):
    return f'{NS}:{path}'


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def block_model(name, parent, textures):
    write(os.path.join(ASSETS, 'models/block', name + '.json'),
          {'parent': parent, 'textures': {k: ref('block/' + v) for k, v in textures.items()}})


def item_model(name, parent=None, layer=None):
    model = {'parent': parent} if layer is None else {'parent': 'minecraft:item/generated',
                                                      'textures': {'layer0': ref('item/' + layer)}}
    write(os.path.join(ASSETS, 'models/item', name + '.json'), model)


def blockstate(name, obj):
    write(os.path.join(ASSETS, 'blockstates', name + '.json'), obj)


def variant(model, x=0, y=0, uvlock=False):
    v = {'model': ref('block/' + model)}
    if uvlock:
        v['uvlock'] = True
    if x:
        v['x'] = x
    if y:
        v['y'] = y
    return v


# --- shapes -------------------------------------------------------------------------------------------------

def face_textures(f):
    """side/top/bottom of the base, for the base cube, stairs and slabs."""
    side = f.name
    return {'bottom': f.bottom or f.top or side, 'side': side, 'top': f.top or side}


def gen_base(f):
    if f.axis:
        textures = {'end': f.name + '_top', 'side': f.name}
        block_model(f.name, 'minecraft:block/cube_column', textures)
        block_model(f.name + '_horizontal', 'minecraft:block/cube_column_horizontal', textures)
        blockstate(f.name, axis_states(f.name))
    elif f.top or f.bottom:
        block_model(f.name, 'minecraft:block/cube_bottom_top', face_textures(f))
        blockstate(f.name, {'variants': {'': variant(f.name)}})
    else:
        block_model(f.name, 'minecraft:block/cube_all', {'all': f.name})
        blockstate(f.name, {'variants': {'': variant(f.name)}})
    item_model(f.name, ref('block/' + f.name))


def axis_states(name):
    return {'variants': {
        'axis=x': variant(name + '_horizontal', x=90, y=90),
        'axis=y': variant(name),
        'axis=z': variant(name + '_horizontal', x=90),
    }}


def gen_pillar(f):
    name = f.id('pillar')
    textures = {'end': name + '_top', 'side': name}
    block_model(name, 'minecraft:block/cube_column', textures)
    block_model(name + '_horizontal', 'minecraft:block/cube_column_horizontal', textures)
    blockstate(name, axis_states(name))
    item_model(name, ref('block/' + name))


def gen_stairs(f):
    name = f.id('stairs')
    textures = face_textures(f)
    for suffix, parent in (('', 'stairs'), ('_inner', 'inner_stairs'), ('_outer', 'outer_stairs')):
        block_model(name + suffix, 'minecraft:block/' + parent, textures)
    # Vanilla's table: an east-facing straight stair is the unrotated model.
    turn = {'east': 0, 'south': 90, 'west': 180, 'north': 270}
    variants = {}
    for facing in DIRECTIONS:
        for half in ('bottom', 'top'):
            for shape in ('inner_left', 'inner_right', 'outer_left', 'outer_right', 'straight'):
                # Corners turn a quarter back (left, bottom half) or on (right, top half, as it's flipped).
                y = turn[facing]
                if shape.endswith('left') and half == 'bottom':
                    y -= 90
                elif shape.endswith('right') and half == 'top':
                    y += 90
                y %= 360
                model = name + {'straight': ''}.get(shape, '_' + shape.split('_')[0])
                x = 180 if half == 'top' else 0
                variants[f'facing={facing},half={half},shape={shape}'] = variant(model, x=x, y=y,
                                                                                  uvlock=bool(x or y))
    blockstate(name, {'variants': variants})
    item_model(name, ref('block/' + name))


def gen_slab(f):
    name = f.id('slab')
    textures = face_textures(f)
    block_model(name, 'minecraft:block/slab', textures)
    block_model(name + '_top', 'minecraft:block/slab_top', textures)
    blockstate(name, {'variants': {
        'type=bottom': variant(name),
        'type=double': variant(f.name),
        'type=top': variant(name + '_top'),
    }})
    item_model(name, ref('block/' + name))


def gen_wall(f):
    name = f.id('wall')
    for suffix, parent in (('_post', 'template_wall_post'), ('_side', 'template_wall_side'),
                           ('_side_tall', 'template_wall_side_tall'), ('_inventory', 'wall_inventory')):
        block_model(name + suffix, 'minecraft:block/' + parent, {'wall': f.name})
    parts = [{'apply': variant(name + '_post'), 'when': {'up': 'true'}}]
    for height, suffix in (('low', '_side'), ('tall', '_side_tall')):
        for side, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            parts.append({'apply': variant(name + suffix, y=y, uvlock=True), 'when': {side: height}})
    blockstate(name, {'multipart': parts})
    item_model(name, ref(f'block/{name}_inventory'))


def gen_fence(f):
    name = f.id('fence')
    for suffix, parent in (('_post', 'fence_post'), ('_side', 'fence_side'), ('_inventory', 'fence_inventory')):
        block_model(name + suffix, 'minecraft:block/' + parent, {'texture': f.name})
    parts = [{'apply': variant(name + '_post')}]
    for side, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        parts.append({'apply': variant(name + '_side', y=y, uvlock=True), 'when': {side: 'true'}})
    blockstate(name, {'multipart': parts})
    item_model(name, ref(f'block/{name}_inventory'))


def gen_fence_gate(f):
    name = f.id('fence_gate')
    for suffix, parent in (('', 'template_fence_gate'), ('_open', 'template_fence_gate_open'),
                           ('_wall', 'template_fence_gate_wall'), ('_wall_open', 'template_fence_gate_wall_open')):
        block_model(name + suffix, 'minecraft:block/' + parent, {'texture': f.name})
    variants = {}
    for facing in DIRECTIONS:
        for in_wall in ('false', 'true'):
            for is_open in ('false', 'true'):
                model = name + ('_wall' if in_wall == 'true' else '') + ('_open' if is_open == 'true' else '')
                variants[f'facing={facing},in_wall={in_wall},open={is_open}'] = variant(model, y=Y[facing],
                                                                                         uvlock=True)
    blockstate(name, {'variants': variants})
    item_model(name, ref('block/' + name))


def gen_door(f):
    name = f.id('door')
    textures = {'bottom': name + '_bottom', 'top': name + '_top'}
    for half in ('bottom', 'top'):
        for hinge in ('left', 'right'):
            for is_open in ('', '_open'):
                block_model(f'{name}_{half}_{hinge}{is_open}', f'minecraft:block/door_{half}_{hinge}{is_open}',
                            textures)
    # A closed door facing east is the unrotated model; opening it turns the model a quarter towards the hinge.
    turn = {'east': 0, 'south': 90, 'west': 180, 'north': 270}
    variants = {}
    for facing in DIRECTIONS:
        for half, model_half in (('lower', 'bottom'), ('upper', 'top')):
            for hinge in ('left', 'right'):
                for is_open in ('false', 'true'):
                    y = turn[facing]
                    if is_open == 'true':
                        y += 90 if hinge == 'left' else 270
                    model = f'{name}_{model_half}_{hinge}' + ('_open' if is_open == 'true' else '')
                    variants[f'facing={facing},half={half},hinge={hinge},open={is_open}'] = variant(model,
                                                                                                    y=y % 360)
    blockstate(name, {'variants': variants})
    item_model(name, layer=name)


def gen_trapdoor(f):
    name = f.id('trapdoor')
    for suffix in ('bottom', 'top', 'open'):
        block_model(f'{name}_{suffix}', f'minecraft:block/template_trapdoor_{suffix}', {'texture': name})
    variants = {}
    for facing in DIRECTIONS:
        for half in ('bottom', 'top'):
            for is_open in ('false', 'true'):
                if is_open == 'true':
                    v = variant(name + '_open', y=(Y[facing] + 180) % 360)
                else:
                    v = variant(f'{name}_{half}')
                variants[f'facing={facing},half={half},open={is_open}'] = v
    blockstate(name, {'variants': variants})
    item_model(name, ref(f'block/{name}_bottom'))


# --- loot tables -------------------------------------------------------------------------------------------

def loot(block, entry_extra=None, pool_conditions=True):
    entry = {'type': 'minecraft:item', 'name': ref(block)}
    entry.update(entry_extra or {})
    pool = {'bonus_rolls': 0.0, 'entries': [entry], 'rolls': 1.0}
    if pool_conditions:
        pool['conditions'] = [{'condition': 'minecraft:survives_explosion'}]
    write(os.path.join(DATA, NS, 'loot_tables/blocks', block + '.json'), {'type': 'minecraft:block',
                                                                          'pools': [pool]})


def gen_loot(f):
    for shape in [None] + f.shapes:
        block = f.id(shape)
        if shape == 'slab':
            loot(block, {'functions': [
                {'add': False, 'conditions': [{'block': ref(block), 'condition': 'minecraft:block_state_property',
                                               'properties': {'type': 'double'}}],
                 'count': 2.0, 'function': 'minecraft:set_count'},
                {'function': 'minecraft:explosion_decay'}]}, pool_conditions=False)
        elif shape == 'door':
            loot(block, {'conditions': [{'block': ref(block), 'condition': 'minecraft:block_state_property',
                                         'properties': {'half': 'lower'}}]})
        else:
            loot(block)


# --- recipes -----------------------------------------------------------------------------------------------

def shaped(name, pattern, key, result, count, category='building', group=None):
    recipe = {'type': 'minecraft:crafting_shaped', 'category': category}
    if group:
        recipe['group'] = group
    recipe['key'] = {k: {'item': v} for k, v in key.items()}
    recipe['pattern'] = pattern
    recipe['result'] = {'count': count, 'item': result} if count != 1 else {'item': result}
    recipe['show_notification'] = True
    write(os.path.join(DATA, NS, 'recipes', name + '.json'), recipe)


def stonecut(name, ingredient, result, count):
    write(os.path.join(DATA, NS, 'recipes', name + '.json'), {
        'type': 'minecraft:stonecutting', 'count': count, 'ingredient': {'item': ingredient}, 'result': result})


# shape: (pattern, count, category, stonecutter count or None, wooden recipe group)
RECIPES = {
    'stairs': (['#  ', '## ', '###'], 4, 'building', 1, 'wooden_stairs'),
    'slab': (['###'], 6, 'building', 2, 'wooden_slab'),
    'wall': (['###', '###'], 6, 'misc', 1, None),
    'pillar': (['#', '#'], 2, 'building', 1, None),
    'fence': (['W#W', 'W#W'], 3, 'misc', None, 'wooden_fence'),
    'fence_gate': (['#W#', '#W#'], 1, 'redstone', None, 'wooden_fence_gate'),
    'door': (['##', '##', '##'], 3, 'redstone', None, 'wooden_door'),
    'trapdoor': (['###', '###'], 2, 'redstone', None, 'wooden_trapdoor'),
}


def gen_recipes(f):
    base = ref(f.name)
    if f.craft:
        shaped(f.name, f.craft['pattern'], f.craft['key'], base, f.craft.get('count', 1))
    for shape in f.shapes:
        pattern, count, category, cut, group = RECIPES[shape]
        result = ref(f.id(shape))
        key = {'W': base, '#': 'minecraft:stick'} if shape in ('fence', 'fence_gate') else {'#': base}
        shaped(f.id(shape), pattern, key, result, count, category, group if f.wooden() else None)
        if cut and not f.wooden():
            stonecut(f'{f.id(shape)}_from_{f.name}_stonecutting', base, result, cut)


# --- tags and lang -----------------------------------------------------------------------------------------

def shape_tags(f, shape):
    """The vanilla tags (for both blocks and items) a shape belongs in."""
    if shape in ('stairs', 'slab', 'door', 'trapdoor'):
        plural = {'stairs': 'stairs', 'slab': 'slabs', 'door': 'doors', 'trapdoor': 'trapdoors'}[shape]
        return ['wooden_' + plural if f.wooden() else plural]
    if shape == 'fence':
        return ['wooden_fences' if f.wooden() else 'fences']
    if shape == 'wall':
        return ['walls']
    if shape == 'fence_gate':
        return ['fence_gates']
    return []


def add_to_tag(kind, tag, values):
    path = os.path.join(DATA, 'minecraft/tags', kind, tag + '.json')
    obj = {'replace': False, 'values': []}
    if os.path.exists(path):
        with open(path) as fh:
            obj = json.load(fh)
    missing = [v for v in values if v not in obj['values']]
    if missing or not os.path.exists(path):
        obj['values'].extend(missing)
        write(path, obj)


def gen_tags():
    tags = {}
    for f in FAMILIES:
        for shape in [None] + f.shapes:
            block = ref(f.id(shape))
            tags.setdefault(('blocks', 'mineable/' + ('axe' if f.wooden() else 'pickaxe')), []).append(block)
            for tag in shape_tags(f, shape):
                tags.setdefault(('blocks', tag), []).append(block)
                tags.setdefault(('items', tag), []).append(block)
    for (kind, tag), values in sorted(tags.items()):
        add_to_tag(kind, tag, values)


def gen_lang():
    entries = {'itemgroup.dndclasses.building_blocks': 'DnD Building Blocks'}
    for f in FAMILIES:
        entries[f'block.{NS}.{f.name}'] = f.title
        for shape in f.shapes:
            words = shape.replace('_', ' ').title()
            entries[f'block.{NS}.{f.id(shape)}'] = f'{f.stem_title} {words}'
    path = os.path.join(ASSETS, 'lang/en_us.json')
    with open(path) as fh:
        text = fh.read()
    current = json.loads(text)
    # Edit the text in place (json.dump would rewrite the file's escapes): change lines whose value differs,
    # append missing keys before the closing brace.
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


GENERATORS = {'stairs': gen_stairs, 'slab': gen_slab, 'wall': gen_wall, 'pillar': gen_pillar, 'fence': gen_fence,
              'fence_gate': gen_fence_gate, 'door': gen_door, 'trapdoor': gen_trapdoor}


def main():
    for f in FAMILIES:
        gen_base(f)
        for shape in f.shapes:
            GENERATORS[shape](f)
        gen_loot(f)
        gen_recipes(f)
    gen_tags()
    gen_lang()
    print(f'{len(FAMILIES)} families written')


if __name__ == '__main__':
    main()
