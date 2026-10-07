# Featherfall

Featherfall is an Artificer-only boots enchantment (I-III). It's a stronger replacement for Feather Falling: it cuts fall damage much further, and at levels II and III you float gently down from big drops.

## How it works
| Level | Fall damage taken | Slow falling |
|-|-|-|
| I | 50% less | No |
| II | 70% less | Yes, after falling 8 blocks |
| III | 90% less | Yes, after falling 3 blocks |

- Feather Falling IV only takes off about 48%. Featherfall can't go on the same boots as Feather Falling.
- **Slow falling** works like the vanilla effect but has no icon or particles. Once it starts, it keeps going until you land.
- **Hold sneak** while falling to drop at normal speed. You still get the damage reduction.
- Slow falling doesn't kick in while you're gliding with an elytra, flying, swimming, in lava, climbing, riding or in spectator.
- A Slow Falling potion you already have is never shortened.

## How to get it
- Enchant boots at an enchanting table as an **Artificer**. Other classes never roll it. It's Uncommon, with the same enchanting-level range as Feather Falling.
- Commands: `/give @s diamond_boots{Enchantments:[{id:"dndclasses:featherfall",lvl:3s}]}` or `/enchant @s dndclasses:featherfall 3`.

## Known limitations
- The damage reduction works on any entity wearing the boots, but only players get the slow falling.
- The slow falling is applied by the server, so it can start a tick or two late on a laggy connection.

## For developers
- `Enchantments/FeatherfallEnchantment.java` defines the enchantment: levels, power curve, and the Feather Falling conflict.
- `Featherfall.java` holds the reduction and threshold tables (`DAMAGE_REDUCTION`, `SLOW_FALL_AFTER`) and the server tick that applies slow falling. It's registered from `ModEnchantments.registerEnchantments()`.
- `mixin/FeatherfallMixin.java` scales the result of `LivingEntity.computeFallDamage`.
- `EnchantingTableMixin` limits it to Artificers.
- `devscripts/featherfall-check.txt` drops the player 25 blocks with level I and level III boots and logs their health. The saved dev player can be stuck `Invulnerable`, so the script clears that first.
