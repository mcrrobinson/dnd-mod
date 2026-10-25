"""Writes the Underdark's worldgen data: dimension type, dimension, noise settings, biomes, features and biome tags.

    python3 tools/underdark_worldgen.py

The noise settings start from vanilla's `caves` preset (tools/underdark/caves.json, dumped from the 1.19.4 server jar
with `java -DbundlerMainClass=net.minecraft.data.Main -jar server.jar --server`). Its router keeps every density
function; only the absolute y anchors of its `y_clamped_gradient`s move down by SHIFT (192), so the preset's
-64..128 world becomes -256..-65, under the Overworld's bedrock. The surface rule is replaced with the Underdark's
own (bedrock floor and roof, each biome's floor and ceiling blocks). See docs/systems/underdark.md.

Every biome lists its features in one global order (FEATURES), so no two biomes disagree about the order of two
features (Minecraft refuses to load a biome source with a feature order cycle). Safe to rerun.
"""
import copy
import json
import math
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
NS = 'dndclasses'
DATA = os.path.join(ROOT, 'src/main/resources/data', NS)
CAVES = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'underdark/caves.json')

SHIFT = -192
MIN_Y = -256
HEIGHT = 192
# Every open cell below this is water (aquifers are off): the deepest basins flood into lakes.
SEA_LEVEL = -222
# The climate. The Caves preset reuses the Overworld's temperature and vegetation noises at the Overworld's scale,
# which makes regions thousands of blocks across, and the temperature noise drifts over even larger distances (its
# first octave is -10). Instead "temperature" here samples the erosion noise (first octave -9) at xz scale 2 and
# "humidity" the vegetation noise (-8) at xz scale 1: both have a wavelength of about 256 blocks, so every region
# turns up within a few hundred blocks.
TEMPERATURE_NOISE, TEMPERATURE_SCALE = 'minecraft:erosion', 2.0
HUMIDITY_NOISE, HUMIDITY_SCALE = 'minecraft:vegetation', 1.0
# The two noises' spread round 0, measured with `/underdark survey 1024` (its temperature=... humidity=... figures).
TEMPERATURE_SD = 0.32
HUMIDITY_SD = 0.25
# Standard normal quantiles: 40% of values fall below -0.2533 sd, a third below -0.4307 sd.
Q40, Q33 = 0.2533, 0.4307
# Bedrock rises at most this many blocks from the floor and hangs this far from the roof.
BEDROCK = 5


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as fh:
        json.dump(obj, fh, indent=2)
        fh.write('\n')


def ref(path):
    return f'{NS}:{path}'


def block(name, **props):
    state = {'Name': name}
    if props:
        state['Properties'] = props
    return state


def colour(hex_string):
    return int(hex_string.lstrip('#'), 16)


# --- dimension type and dimension -------------------------------------------------------------------------------

def dimension_type():
    write(os.path.join(DATA, 'dimension_type/underdark.json'), {
        'ultrawarm': False,
        'natural': False,
        'coordinate_scale': 1.0,
        'has_skylight': False,
        'has_ceiling': True,
        'ambient_light': 0.05,
        'fixed_time': 18000,
        'monster_spawn_light_level': {'type': 'minecraft:uniform', 'value': {'min_inclusive': 0, 'max_inclusive': 7}},
        'monster_spawn_block_light_limit': 0,
        'piglin_safe': False,
        'bed_works': True,
        'respawn_anchor_works': False,
        'has_raids': False,
        'logical_height': HEIGHT,
        'min_y': MIN_Y,
        'height': HEIGHT,
        'infiniburn': '#minecraft:infiniburn_overworld',
        'effects': ref('underdark'),
    })


def climate_ranges():
    """Each biome's box of the climate, splitting it into even fifths: the driest 40% (by humidity) is the Faerzress
    Hollows (cooler half) and the Grey Reaches (warmer half); the wetter 60% is the Darklake, the Fungal Forest and the
    Mycelial Caverns, by thirds of temperature from cold to warm. Multi-noise ranges must stay within -2..2."""
    dry = round(-Q40 * HUMIDITY_SD, 4)
    cold, warm = round(-Q33 * TEMPERATURE_SD, 4), round(Q33 * TEMPERATURE_SD, 4)
    return {
        'faerzress_hollows': ([-2.0, 0.0], [-2.0, dry]),
        'grey_reaches': ([0.0, 2.0], [-2.0, dry]),
        'darklake': ([-2.0, cold], [dry, 2.0]),
        'fungal_forest': ([cold, warm], [dry, 2.0]),
        'mycelial_caverns': ([warm, 2.0], [dry, 2.0]),
    }


def dimension():
    ranges = climate_ranges()
    biomes = []
    for b in BIOMES:
        temperature, humidity = ranges[b['id']]
        biomes.append({'biome': ref(b['id']), 'parameters': {
            'temperature': temperature, 'humidity': humidity, 'continentalness': 0.0, 'erosion': 0.0,
            'weirdness': 0.0, 'depth': 0.0, 'offset': 0.0}})
    write(os.path.join(DATA, 'dimension/underdark.json'), {
        'type': ref('underdark'),
        'generator': {'type': 'minecraft:noise', 'settings': ref('underdark'),
                      'biome_source': {'type': 'minecraft:multi_noise', 'biomes': biomes}},
    })


# --- noise settings ---------------------------------------------------------------------------------------------

def shift_router(node):
    """Moves every absolute y in the router down by SHIFT; returns how many anchors moved."""
    moved = 0
    if isinstance(node, dict):
        if node.get('type') == 'minecraft:y_clamped_gradient':
            node['from_y'] += SHIFT
            node['to_y'] += SHIFT
            moved += 1
        for value in node.values():
            moved += shift_router(value)
    elif isinstance(node, list):
        for value in node:
            moved += shift_router(value)
    return moved


def floor(depth=0, add_surface_depth=False):
    return {'type': 'minecraft:stone_depth', 'offset': depth, 'surface_type': 'floor',
            'add_surface_depth': add_surface_depth, 'secondary_depth_range': 0}


def ceiling():
    return {'type': 'minecraft:stone_depth', 'offset': 0, 'surface_type': 'ceiling', 'add_surface_depth': False,
            'secondary_depth_range': 0}


def put(state):
    return {'type': 'minecraft:block', 'result_state': state}


def when(condition, rule):
    return {'type': 'minecraft:condition', 'if_true': condition, 'then_run': rule}


def patches(share, rule):
    """`rule` in noise patches covering about `share` of the ground (the surface noise is roughly normal, sd 0.4)."""
    # Thresholds of the vanilla surface noise for a given cover, from sampling it: 0.30 -> ~0.18, 0.35 -> ~0.12.
    threshold = {0.30: 0.18, 0.35: 0.12}[share]
    return when({'type': 'minecraft:noise_threshold', 'noise': 'minecraft:surface', 'min_threshold': threshold,
                 'max_threshold': 10.0}, rule)


def biome_rule(b):
    rules = []
    top, under, roof = b['floor'], b['under'], b['ceiling']
    top_rules = []
    if 'floor_patch' in b:
        top_rules.append(patches(b['floor_patch'][0], put(block(b['floor_patch'][1]))))
    if 'floor_wet' in b:
        # Below the waterline (plus 2 blocks of shore) the floor is the wet block instead.
        top_rules.append(when({'type': 'minecraft:not', 'invert': {'type': 'minecraft:y_above', 'anchor': {
            'absolute': SEA_LEVEL + 2}, 'surface_depth_multiplier': 0, 'add_stone_depth': False}},
            put(block(b['floor_wet']))))
    top_rules.append(put(block(top)))
    rules.append(when(floor(), {'type': 'minecraft:sequence', 'sequence': top_rules}))
    if under:
        rules.append(when(floor(0, True), put(block(under))))
    if roof:
        rules.append(when(ceiling(), put(block(roof))))
    return when({'type': 'minecraft:biome', 'biome_is': [ref(b['id'])]}, {'type': 'minecraft:sequence',
                                                                          'sequence': rules})


def surface_rule():
    bedrock = put(block('minecraft:bedrock'))
    return {'type': 'minecraft:sequence', 'sequence': [
        when({'type': 'minecraft:vertical_gradient', 'random_name': ref('underdark_bedrock_floor'),
              'true_at_and_below': {'above_bottom': 0}, 'false_at_and_above': {'above_bottom': BEDROCK}}, bedrock),
        when({'type': 'minecraft:not', 'invert': {
            'type': 'minecraft:vertical_gradient', 'random_name': ref('underdark_bedrock_roof'),
            'true_at_and_below': {'below_top': BEDROCK}, 'false_at_and_above': {'below_top': 0}}}, bedrock),
    ] + [biome_rule(b) for b in BIOMES]}


def noise_settings():
    with open(CAVES) as fh:
        settings = json.load(fh)
    settings = copy.deepcopy(settings)
    moved = shift_router(settings['noise_router'])
    assert moved == 2, f'expected the caves router to have 2 y gradients, found {moved}'
    for field, noise, scale in (('temperature', TEMPERATURE_NOISE, TEMPERATURE_SCALE),
                                ('vegetation', HUMIDITY_NOISE, HUMIDITY_SCALE)):
        settings['noise_router'][field]['noise'] = noise
        settings['noise_router'][field]['xz_scale'] = scale
    settings['noise'] = {'min_y': MIN_Y, 'height': HEIGHT, 'size_horizontal': 1, 'size_vertical': 2}
    settings['default_block'] = block('minecraft:deepslate', axis='y')
    settings['default_fluid'] = block('minecraft:water', level='0')
    settings['sea_level'] = SEA_LEVEL
    settings['aquifers_enabled'] = False
    settings['ore_veins_enabled'] = False
    settings['legacy_random_source'] = False
    settings['disable_mob_generation'] = False
    settings['surface_rule'] = surface_rule()
    write(os.path.join(DATA, 'worldgen/noise_settings/underdark.json'), settings)


# --- features ---------------------------------------------------------------------------------------------------

# Generation steps, in vanilla's order (GenerationStep.Feature).
STEPS = ['raw_generation', 'lakes', 'local_modifications', 'underground_structures', 'surface_structures',
         'strongholds', 'underground_ores', 'underground_decoration', 'fluid_springs', 'vegetal_decoration',
         'top_layer_modification']

WHOLE = {'type': 'minecraft:uniform', 'min_inclusive': {'above_bottom': 6}, 'max_inclusive': {'below_top': 6}}
AIR = {'type': 'minecraft:matching_blocks', 'blocks': 'minecraft:air'}


def count(n):
    return [{'type': 'minecraft:count', 'count': n}, {'type': 'minecraft:in_square'}]


def on_floor(n):
    """n tries per chunk, each dropped onto the first solid floor below a random point (the cave-vegetation chain)."""
    return count(n) + [
        {'type': 'minecraft:height_range', 'height': WHOLE},
        {'type': 'minecraft:environment_scan', 'direction_of_search': 'down', 'max_steps': 12,
         'target_condition': {'type': 'minecraft:solid'}, 'allowed_search_condition': AIR},
        {'type': 'minecraft:random_offset', 'xz_spread': 0, 'y_spread': 1},
        {'type': 'minecraft:biome'}]


def on_ceiling(n):
    return count(n) + [
        {'type': 'minecraft:height_range', 'height': WHOLE},
        {'type': 'minecraft:environment_scan', 'direction_of_search': 'up', 'max_steps': 12,
         'target_condition': {'type': 'minecraft:solid'}, 'allowed_search_condition': AIR},
        {'type': 'minecraft:random_offset', 'xz_spread': 0, 'y_spread': -1},
        {'type': 'minecraft:biome'}]


def anywhere(n, height=None):
    return count(n) + [{'type': 'minecraft:height_range', 'height': height or WHOLE}, {'type': 'minecraft:biome'}]


def rare(chance, height=None):
    return [{'type': 'minecraft:rarity_filter', 'chance': chance}, {'type': 'minecraft:in_square'},
            {'type': 'minecraft:height_range', 'height': height or WHOLE}, {'type': 'minecraft:biome'}]


def ore(n, height):
    return count(n) + [{'type': 'minecraft:height_range', 'height': height}, {'type': 'minecraft:biome'}]


TRAPEZOID = {'type': 'minecraft:trapezoid', 'min_inclusive': {'above_bottom': 0}, 'max_inclusive': {'below_top': 0},
             'plateau': 0}
UNIFORM = {'type': 'minecraft:uniform', 'min_inclusive': {'above_bottom': 0}, 'max_inclusive': {'below_top': 0}}
LOWER_HALF = {'type': 'minecraft:trapezoid', 'min_inclusive': {'above_bottom': -32},
              'max_inclusive': {'above_bottom': HEIGHT // 2}, 'plateau': 0}


def patch(state, tries=32, spread=6):
    """A random patch of `state` on the floor (each one placed only where it can stand)."""
    return {'type': 'minecraft:random_patch', 'config': {'tries': tries, 'xz_spread': spread, 'y_spread': 3, 'feature': {
        'feature': {'type': 'minecraft:simple_block', 'config': {
            'to_place': {'type': 'minecraft:simple_state_provider', 'state': state}}},
        'placement': [{'type': 'minecraft:block_predicate_filter', 'predicate': AIR}]}}}


def ore_config(target, state, size):
    return {'type': 'minecraft:ore', 'config': {'discard_chance_on_air_exposure': 0.0, 'size': size, 'targets': [
        {'target': {'predicate_type': 'minecraft:block_match', 'block': target}, 'state': state}]}}


def near_water(reach=3):
    """A filter: water within `reach` blocks across, at the floor's level or one below (a shore)."""
    offsets = [[dx, dy, dz] for dx in range(-reach, reach + 1) for dz in range(-reach, reach + 1) for dy in (-1, -2)
               if (dx or dz) and max(abs(dx), abs(dz)) == reach]
    return {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:any_of', 'predicates': [
        {'type': 'minecraft:matching_fluids', 'fluids': 'minecraft:water', 'offset': o} for o in offsets]}}


def on_water(n):
    """n tries per chunk on the Darklake's surface: dropped from up to 12 blocks above the waterline onto water."""
    return count(n) + [
        {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {
            'absolute': SEA_LEVEL}, 'max_inclusive': {'absolute': SEA_LEVEL + 12}}},
        {'type': 'minecraft:environment_scan', 'direction_of_search': 'down', 'max_steps': 16,
         'target_condition': {'type': 'minecraft:matching_fluids', 'fluids': 'minecraft:water'},
         'allowed_search_condition': AIR},
        {'type': 'minecraft:random_offset', 'xz_spread': 0, 'y_spread': 1},
        {'type': 'minecraft:biome'}]


def lake_bed(n, chance=None):
    """n tries per chunk (in 1 chunk of `chance`) on the floor under the Darklake's water: the bed block itself."""
    rarity = [{'type': 'minecraft:rarity_filter', 'chance': chance}] if chance else []
    return rarity + count(n) + [
        {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {
            'above_bottom': 6}, 'max_inclusive': {'absolute': SEA_LEVEL - 2}}},
        {'type': 'minecraft:environment_scan', 'direction_of_search': 'down', 'max_steps': 16,
         'target_condition': {'type': 'minecraft:solid'}, 'allowed_search_condition': {
             'type': 'minecraft:matching_fluids', 'fluids': 'minecraft:water'}},
        {'type': 'minecraft:block_predicate_filter', 'predicate': {
            'type': 'minecraft:matching_fluids', 'fluids': 'minecraft:water', 'offset': [0, 1, 0]}},
        {'type': 'minecraft:biome'}]


def in_water(n):
    """n tries per chunk just above the Darklake's bed, under water."""
    return count(n) + [
        {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {
            'above_bottom': 6}, 'max_inclusive': {'absolute': SEA_LEVEL - 2}}},
        {'type': 'minecraft:environment_scan', 'direction_of_search': 'down', 'max_steps': 16,
         'target_condition': {'type': 'minecraft:solid'}, 'allowed_search_condition': {
             'type': 'minecraft:matching_fluids', 'fluids': 'minecraft:water'}},
        {'type': 'minecraft:random_offset', 'xz_spread': 0, 'y_spread': 1},
        {'type': 'minecraft:biome'}]


def patch_where(state, predicate, tries=32, spread=6, y_spread=3):
    """A random patch of `state` at the spots matching `predicate`."""
    return {'type': 'minecraft:random_patch', 'config': {'tries': tries, 'xz_spread': spread, 'y_spread': y_spread,
                                                         'feature': {
        'feature': {'type': 'minecraft:simple_block', 'config': {
            'to_place': {'type': 'minecraft:simple_state_provider', 'state': state}}},
        'placement': [{'type': 'minecraft:block_predicate_filter', 'predicate': predicate}]}}}


def survives(state):
    return {'type': 'minecraft:would_survive', 'state': state}


WATER = {'type': 'minecraft:matching_blocks', 'blocks': 'minecraft:water'}
LICHEN_ROCK = ['minecraft:stone', 'minecraft:deepslate', 'minecraft:tuff', 'minecraft:granite', 'minecraft:diorite',
               'minecraft:andesite', 'minecraft:calcite', 'minecraft:dripstone_block', ref('faerzress_stone'),
               ref('fungal_loam')]

# Our own configured features (written to worldgen/configured_feature/underdark/<name>.json).
CONFIGURED = {
    # The giant fungi (T3; Java features in world/underdark/feature). The Spores and the Bluecap grow these too.
    'giant_zurkhwood': {'type': ref('giant_zurkhwood'), 'config': {}},
    'nightlight_tower': {'type': ref('nightlight_tower'), 'config': {'min_height': 10, 'max_height': 22}},
    'small_nightlight_tower': {'type': ref('nightlight_tower'), 'config': {'min_height': 6, 'max_height': 9}},
    'shelf_fungus': {'type': ref('shelf_fungus'), 'config': {}},
    'barrelstalk_patch': patch(block(ref('barrelstalk'), filled='true', half='lower'), tries=4, spread=3),
    'waterorb_patch': patch_where(block(ref('waterorb')), {'type': 'minecraft:all_of', 'predicates': [
        AIR, survives(block(ref('waterorb')))]}, tries=6, spread=3, y_spread=1),
    'seagrass_patch': patch_where(block('minecraft:seagrass'), {'type': 'minecraft:all_of', 'predicates': [
        WATER, survives(block('minecraft:seagrass'))]}, tries=24, spread=4, y_spread=2),
    'faerie_lichen': {'type': 'minecraft:multiface_growth', 'config': {
        'block': ref('faerie_lichen'), 'search_range': 20, 'chance_of_spreading': 0.5, 'can_place_on_floor': False,
        'can_place_on_ceiling': True, 'can_place_on_wall': True, 'can_be_placed_on': LICHEN_ROCK}},
    'darklake_shore': {'type': 'minecraft:disk', 'config': {
        'state_provider': {'fallback': {'type': 'minecraft:weighted_state_provider', 'entries': [
            {'weight': 3, 'data': block('minecraft:clay')}, {'weight': 2, 'data': block('minecraft:gravel')}]},
            'rules': []},
        'target': {'type': 'minecraft:matching_blocks', 'blocks': ['minecraft:gravel', 'minecraft:deepslate',
                                                                    'minecraft:dirt', 'minecraft:tuff']},
        'radius': {'type': 'minecraft:uniform', 'value': {'min_inclusive': 2, 'max_inclusive': 4}},
        'half_height': 1}},
    'pearl_clam': {'type': 'minecraft:replace_single_block', 'config': {'targets': [
        {'target': {'predicate_type': 'minecraft:block_match', 'block': b}, 'state': block(ref('pearl_clam'))}
        for b in ('minecraft:clay', 'minecraft:gravel', 'minecraft:deepslate')]}},
    'bluecap_patch': patch(block(ref('bluecap'))),
    'trillimac_patch': patch(block(ref('trillimac')), tries=24, spread=5),
    'faerzress_veins': ore_config('minecraft:deepslate', block(ref('faerzress_stone')), 33),
    'magma_patches': ore_config('minecraft:tuff', block('minecraft:magma_block'), 20),
    'darklake_pool': {'type': 'minecraft:lake', 'config': {
        'fluid': {'type': 'minecraft:simple_state_provider', 'state': block('minecraft:water', level='0')},
        'barrier': {'type': 'minecraft:simple_state_provider', 'state': block('minecraft:clay')}}},
}

# Every placed feature, in the one order all biomes use: (name, step, configured feature, placement).
FEATURES = [
    ('darklake_pool', 'lakes', ref('underdark/darklake_pool'), [{'type': 'minecraft:rarity_filter', 'chance': 2}]
     + on_floor(1)),
    ('amethyst_geode', 'local_modifications', 'minecraft:amethyst_geode', rare(12)),
    ('ore_gravel', 'underground_ores', 'minecraft:ore_gravel', ore(8, UNIFORM)),
    ('ore_tuff', 'underground_ores', 'minecraft:ore_tuff', ore(4, UNIFORM)),
    ('ore_iron', 'underground_ores', 'minecraft:ore_iron', ore(10, TRAPEZOID)),
    ('ore_gold', 'underground_ores', 'minecraft:ore_gold', ore(6, UNIFORM)),
    ('ore_redstone', 'underground_ores', 'minecraft:ore_redstone', ore(6, UNIFORM)),
    ('ore_lapis', 'underground_ores', 'minecraft:ore_lapis', ore(3, UNIFORM)),
    ('ore_diamond', 'underground_ores', 'minecraft:ore_diamond_small', ore(9, LOWER_HALF)),
    ('faerzress_veins', 'underground_ores', ref('underdark/faerzress_veins'), ore(8, UNIFORM)),
    ('magma_patches', 'underground_ores', ref('underdark/magma_patches'), ore(6, UNIFORM)),
    ('darklake_shore', 'underground_ores', ref('underdark/darklake_shore'), on_floor(4) + [near_water()]),
    ('pearl_clam', 'underground_ores', ref('underdark/pearl_clam'), lake_bed(2, chance=4)),
    ('spring_water', 'fluid_springs', 'minecraft:spring_water', anywhere(20)),
    ('spring_lava', 'fluid_springs', 'minecraft:spring_lava_overworld', anywhere(20)),
    ('large_dripstone', 'vegetal_decoration', 'minecraft:large_dripstone', anywhere(12)),
    ('dripstone_cluster', 'vegetal_decoration', 'minecraft:dripstone_cluster', anywhere(8)),
    ('moss_patch_ceiling', 'vegetal_decoration', 'minecraft:moss_patch_ceiling', on_ceiling(24)),
    ('clay_pool_with_dripleaves', 'vegetal_decoration', 'minecraft:clay_pool_with_dripleaves', on_floor(16)),
    ('giant_zurkhwood', 'vegetal_decoration', ref('underdark/giant_zurkhwood'), on_floor(4)),
    ('giant_zurkhwood_sparse', 'vegetal_decoration', ref('underdark/giant_zurkhwood'), on_floor(1)),
    ('nightlight_tower', 'vegetal_decoration', ref('underdark/nightlight_tower'), on_floor(2)),
    ('nightlight_tower_shore', 'vegetal_decoration', ref('underdark/nightlight_tower'), on_floor(3) + [near_water()]),
    ('shelf_fungus', 'vegetal_decoration', ref('underdark/shelf_fungus'), anywhere(12)),
    ('shelf_fungus_sparse', 'vegetal_decoration', ref('underdark/shelf_fungus'), anywhere(6)),
    ('barrelstalk', 'vegetal_decoration', ref('underdark/barrelstalk_patch'), on_floor(1)),
    ('seagrass', 'vegetal_decoration', ref('underdark/seagrass_patch'), in_water(6)),
    ('waterorb', 'vegetal_decoration', ref('underdark/waterorb_patch'), on_water(3)),
    ('huge_red_mushroom', 'vegetal_decoration', 'minecraft:huge_red_mushroom', on_floor(3)),
    ('huge_brown_mushroom', 'vegetal_decoration', 'minecraft:huge_brown_mushroom', on_floor(3)),
    ('brown_mushrooms', 'vegetal_decoration', 'minecraft:patch_brown_mushroom', on_floor(3)),
    ('red_mushrooms', 'vegetal_decoration', 'minecraft:patch_red_mushroom', on_floor(3)),
    ('bluecap_fungal', 'vegetal_decoration', ref('underdark/bluecap_patch'), on_floor(4)),
    ('bluecap_sparse', 'vegetal_decoration', ref('underdark/bluecap_patch'), on_floor(2)),
    ('trillimac_fungal', 'vegetal_decoration', ref('underdark/trillimac_patch'), on_floor(2)),
    ('trillimac_mycelial', 'vegetal_decoration', ref('underdark/trillimac_patch'), on_floor(3)),
    ('glow_lichen_sparse', 'vegetal_decoration', 'minecraft:glow_lichen', anywhere(20)),
    ('glow_lichen', 'vegetal_decoration', 'minecraft:glow_lichen', anywhere(40)),
    ('glow_lichen_dense', 'vegetal_decoration', 'minecraft:glow_lichen', anywhere(60)),
    ('glow_lichen_faerzress', 'vegetal_decoration', 'minecraft:glow_lichen', anywhere(120)),
    ('faerie_lichen_sparse', 'vegetal_decoration', ref('underdark/faerie_lichen'), anywhere(10)),
    ('faerie_lichen', 'vegetal_decoration', ref('underdark/faerie_lichen'), anywhere(60)),
]
ORES = ['ore_gravel', 'ore_tuff', 'ore_iron', 'ore_gold', 'ore_redstone', 'ore_lapis', 'ore_diamond']

# --- biomes -----------------------------------------------------------------------------------------------------

MONSTERS = [('minecraft:spider', 100, 1, 3), ('minecraft:cave_spider', 40, 2, 4), ('minecraft:zombie', 60, 1, 4),
            ('minecraft:skeleton', 60, 1, 4), ('minecraft:creeper', 20, 1, 2), ('minecraft:enderman', 4, 1, 1),
            ('minecraft:witch', 4, 1, 1)]

BIOMES = [
    {'id': 'fungal_forest', 'name': 'Fungal Forest',
     'fog': '#1F2D3D', 'water': '#3D57D6', 'water_fog': '#050533', 'particle': ('minecraft:spore_blossom_air', 0.008),
     'floor': ref('fungal_loam'), 'under': 'minecraft:deepslate', 'ceiling': None,
     'features': ['giant_zurkhwood', 'nightlight_tower', 'shelf_fungus', 'barrelstalk', 'bluecap_fungal',
                  'trillimac_fungal', 'glow_lichen', 'faerie_lichen_sparse']},
    {'id': 'mycelial_caverns', 'name': 'Mycelial Caverns',
     'fog': '#38212F', 'water': '#4C3A8F', 'water_fog': '#0A0520', 'particle': ('minecraft:mycelium', 0.02),
     'floor': 'minecraft:mycelium', 'under': 'minecraft:dirt', 'ceiling': None,
     'features': ['giant_zurkhwood_sparse', 'huge_red_mushroom', 'huge_brown_mushroom', 'brown_mushrooms',
                  'red_mushrooms', 'trillimac_mycelial', 'glow_lichen_sparse']},
    {'id': 'darklake', 'name': 'Darklake',
     'fog': '#0B1626', 'water': '#1E2B5A', 'water_fog': '#020814', 'particle': ('minecraft:dripping_dripstone_water', 0.003),
     'floor': 'minecraft:gravel', 'floor_wet': 'minecraft:clay', 'under': 'minecraft:gravel', 'ceiling': None,
     'features': ['darklake_pool', 'darklake_shore', 'pearl_clam', 'spring_water', 'moss_patch_ceiling',
                  'clay_pool_with_dripleaves', 'nightlight_tower_shore', 'shelf_fungus_sparse', 'barrelstalk',
                  'seagrass', 'waterorb', 'glow_lichen_dense'],
     'water_mobs': True},
    {'id': 'faerzress_hollows', 'name': 'Faerzress Hollows',
     'fog': '#2B1745', 'water': '#5B3FBF', 'water_fog': '#14082A', 'particle': ('minecraft:portal', 0.006),
     'floor': 'minecraft:deepslate', 'floor_patch': (0.35, ref('faerzress_stone')), 'under': None, 'ceiling': None,
     'features': ['amethyst_geode', 'faerzress_veins', 'bluecap_sparse', 'glow_lichen_faerzress', 'faerie_lichen']},
    {'id': 'grey_reaches', 'name': 'Grey Reaches',
     'fog': '#2E2A28', 'water': '#3F4A5A', 'water_fog': '#101317', 'particle': ('minecraft:white_ash', 0.025),
     'floor': 'minecraft:tuff', 'floor_patch': (0.30, 'minecraft:basalt'), 'under': 'minecraft:tuff',
     'ceiling': 'minecraft:tuff',
     'features': ['magma_patches', 'spring_lava', 'large_dripstone', 'dripstone_cluster']},
]

# Which Underdark biomes each later ticket's creature lives in (design 9, 2.4). Other tickets add Overworld biomes.
HABITATS = {
    'drow': ['fungal_forest', 'faerzress_hollows'],
    'duergar': ['grey_reaches'],
    'troll': ['fungal_forest', 'darklake'],
    'basilisk': ['grey_reaches', 'faerzress_hollows'],
    'mind_flayer': ['faerzress_hollows'],
    'rust_monster': ['grey_reaches', 'mycelial_caverns'],
}


def spawner(entity, weight, low, high):
    return {'type': entity, 'weight': weight, 'minCount': low, 'maxCount': high}


def biome(b):
    names = set(b['features']) | set(ORES)
    features = [[] for _ in STEPS]
    for name, step, _, _ in FEATURES:
        if name in names:
            features[STEPS.index(step)].append(ref('underdark/' + name))
    water = b.get('water_mobs', False)
    write(os.path.join(DATA, 'worldgen/biome', b['id'] + '.json'), {
        'has_precipitation': False,
        'temperature': 0.8,
        'downfall': 0.4,
        'effects': {
            'fog_color': colour(b['fog']),
            'water_color': colour(b['water']),
            'water_fog_color': colour(b['water_fog']),
            'sky_color': 0,
            'particle': {'options': {'type': b['particle'][0]}, 'probability': b['particle'][1]},
            'mood_sound': {'sound': 'minecraft:ambient.cave', 'tick_delay': 6000, 'block_search_extent': 8,
                           'offset': 2.0},
            # Distant drips and rumbles now and then, wherever you are (T3).
            'additions_sound': {'sound': 'minecraft:ambient.cave', 'tick_chance': 0.0111},
        },
        'spawners': {
            'monster': [spawner(*m) for m in MONSTERS],
            'creature': [],
            'ambient': [spawner('minecraft:bat', 10, 2, 4)],
            'axolotls': [spawner('minecraft:axolotl', 10, 2, 4)] if water else [],
            'underground_water_creature': [spawner('minecraft:glow_squid', 10, 2, 4)] if water else [],
            'water_creature': [],
            'water_ambient': [],
            'misc': [],
        },
        'spawn_costs': {},
        # No carvers: the Caves router already makes the caverns, and carver tunnels are cut after the surface rules,
        # so they'd leave bare deepslate floors in every region.
        'carvers': {},
        'features': features,
    })


def features():
    for name, config in CONFIGURED.items():
        write(os.path.join(DATA, 'worldgen/configured_feature/underdark', name + '.json'), config)
    for name, _, configured, placement in FEATURES:
        write(os.path.join(DATA, 'worldgen/placed_feature/underdark', name + '.json'),
              {'feature': configured, 'placement': placement})


def tags():
    write(os.path.join(DATA, 'tags/worldgen/biome/is_underdark.json'),
          {'replace': False, 'values': [ref(b['id']) for b in BIOMES]})
    for name, biomes in HABITATS.items():
        write(os.path.join(DATA, 'tags/worldgen/biome/habitat', name + '.json'),
              {'replace': False, 'values': [ref(b) for b in biomes]})


def main():
    dimension_type()
    dimension()
    noise_settings()
    features()
    for b in BIOMES:
        biome(b)
    tags()
    print(f'Underdark worldgen written: {len(BIOMES)} biomes, {len(FEATURES)} placed features')


if __name__ == '__main__':
    main()
