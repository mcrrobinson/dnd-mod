# Vampiric

An Artificer weapon enchantment: every melee hit heals the wielder for a share of the damage it dealt.

## How it works

- Levels I-III. Heals **10% of the damage dealt per level**: 10% at I, 20% at II, 30% at III.
- "Damage dealt" is the health plus absorption the target actually lost, so damage soaked by armor or overkill on a dying mob doesn't count.
- Melee only: the wielder must hit directly with a normal player or mob attack. Arrows, tridents, thorns and other indirect damage don't heal. Sweeping hits do count.
- Only works from the main hand. A mob holding a Vampiric weapon heals too.

## How to get it

- Applies to swords and axes. Rarity: rare.
- Only an Artificer can roll it at the enchanting table, like the other custom enchantments. Other classes never get it there.
- Test copy: `/give @s minecraft:netherite_sword{Enchantments:[{id:"dndclasses:vampiric",lvl:3s}]}`

## Known limitations

- It can't heal you above max health, so it does nothing at full health.

## For developers

- `src/main/java/mattonfire/dnd/classes/Enchantments/VampiricEnchantment.java`: levels and `HEAL_FRACTION_PER_LEVEL` (0.1).
- `src/main/java/mattonfire/dnd/classes/mixin/VampiricMixin.java`: hooks `LivingEntity.damage` on the server. It records the target's health plus absorption at HEAD and heals the attacker on a successful hit.
- Registered in `Registry/ModEnchantments.java` as `dndclasses:vampiric`. It's on the Artificer-only list in `mixin/EnchantingTableMixin.java`.
- Devscript: `timeout 300 ./gradlew runClient -PdevScript=devscripts/vampiric-check.txt`. It raises max health to 40 so the player starts at 20/40, then hits a frozen zombie with a Vampiric III sword. The player's Health should go above 20.0.
