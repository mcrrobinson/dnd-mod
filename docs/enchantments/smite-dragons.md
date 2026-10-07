# Smite Dragons

Smite Dragons is an Artificer enchantment for swords and axes. It's Smite for dragons: extra melee damage against every dragon and wyvern in the mod. It pairs with the Dragon Slayer advancement.

## How it works

- Levels I to V, each adding **+2.5 melee damage** against dragons, the same as Smite against undead. At level V that's +12.5: a diamond sword hits a Wyvern for 19.5 instead of 7.
- "Dragons" means the `#dndclasses:dragons` entity tag, which Dragon Slayer also uses: Wyvern, Ember Wyvern, Lightning Chaser and River Pikehorn.
- The bonus scales with the attack cooldown and shows the blue enchanted-hit particles, as vanilla damage enchantments do.
- Hits on a dragon's wing, neck or tail count, not just its body.
- Other mobs take no extra damage.
- It can't be combined with Sharpness, Smite or Bane of Arthropods.

## How to get it

- At an enchanting table as an **Artificer**: swords and axes can roll it, at the same power levels as Smite. Other classes never roll it.
- On items an Artificer crafts, through the class's chance for crafted gear to come out enchanted.
- `/give @s diamond_sword{Enchantments:[{id:"dndclasses:smite_dragons",lvl:5s}]}`

## Known limitations

- Only player melee hits get the bonus. Mobs holding an enchanted weapon and thrown tridents don't.
- New dragons must be added to `#dndclasses:dragons` to be affected.

## For developers

- `src/main/java/mattonfire/dnd/classes/Enchantments/SmiteDragonsEnchantment.java`: the enchantment, the `DRAGONS` tag key and `getBonus()`.
- `src/main/java/mattonfire/dnd/classes/mixin/SmiteDragonsMixin.java`: vanilla only passes an enchantment the target's `EntityGroup`, so the bonus is added with `@ModifyVariable` after both `EnchantmentHelper.getAttackDamage` calls in `PlayerEntity.attack`. A living target uses the first call and a `DragonPart` the second; a `DragonPart` has its dragon's entity type, so the tag check matches it too.
- Registered in `ModEnchantments`. `EnchantingTableMixin` limits it to Artificers.
- Tag: `src/main/resources/data/dndclasses/tags/entity_types/dragons.json`.
- Devscript: `devscripts/smite-dragons.txt` hits a Husk and a Wyvern with a plain sword and a Smite Dragons V sword. In `latest.log`, the Wyvern loses 12.5 more health to the enchanted hit, and the Husk loses the same to both.
