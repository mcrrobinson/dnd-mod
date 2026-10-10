# Magmamuncher Alpha
A huge, hostile [Magmamuncher](../mobs/magmamunchers.md) that roams the Nether. Unlike its herd it hunts players on sight, its bites set you on fire, and at half health it calls for help and starts spitting fireballs.

![The Magmamuncher Alpha](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/magmamuncher_alpha.png)

## How it works

| Stat | Normal | Enraged (below 50%) |
|-|-|-|
| Health | 300 | |
| Armor | 10, plus 2 toughness | |
| Knockback resistance | 100% | |
| Bite | 14 | 20 |
| Bite sets you alight for | 4 s | 8 s |
| Speed | 0.25 | 0.325 (+30%) |
| Fireball volley | none | 3 small fireballs every 4 s |

It's immune to fire and lava, and its bites knock you back hard. It hunts players within 32 blocks, and it fights back against Magmamunchers that hit it. Wild Magmamunchers count as its allies.

**Enrage.** When its health drops below half, the bar turns red and it roars in a burst of lava and flame. Its speed and bite damage go up, its bites burn for twice as long, and two regular Magmamunchers crawl out of the ground 3 to 5.5 blocks away and join the fight. If there's no room for one, it doesn't come.

**Fireballs.** While enraged, every 4 seconds it spits a spread of 3 small fireballs at a target it can see that's 4 to 20 blocks away. The first volley comes a second after it enrages. The fireballs work like a blaze's: they set you on fire and light the ground.

**Boss bar.** A yellow, notched bar shows to players within 48 blocks while it fights a player, and **Tooth and Claw** plays. Once it has fought a player it never despawns, so you can't lose a half-killed Alpha by running off.

### Drops

| Drop | Count | Condition |
|-|-|-|
| Magma cream | 4-8 (+0-2 per Looting level) | always |
| Blaze rods | 2-5 (+0-1 per Looting level) | always |
| Gold ingots | 5-10 | killed by a player |
| Netherite scrap | 1-2 | killed by a player, 60% chance (+10% per Looting level) |
| Enchanted book: Fire Protection IV or Fire Aspect II | 1 | killed by a player, 25% chance (+5% per Looting level) |
| [Staff of Fire](../items/staffs.md) | 1 | killed by a player, 25% chance (+5% per Looting level) |

It's worth 100 XP.

### Tips
- Drink Fire Resistance before the fight. It cancels the burning bites and the fireballs, which is most of the danger after enrage.
- Bring a shield. It blocks the bite and the fireballs.
- Get it to half health somewhere you can see the ground around it, so the two summoned Magmamunchers don't come at you from behind.
- Its knockback can throw you into lava. Fight it on wide, flat ground away from lava lakes.

## Where to find it
- Rarely, in Basalt Deltas and Nether Wastes (monster, weight 1, alone), with an extra 1 in 4 roll on each spawn.
- Never within 96 blocks of another Alpha.
- Spawn egg in the mod's creative tab.

## Commands
- `/summon dndclasses:magmamuncher_alpha`

## For developers
- `entity/MagmamuncherAlphaEntity`: a `HostileEntity` with a `BossFight` (`.phase(0.5F, RED, enrage)`, `.xp(100)`), the enrage modifiers, `summonPack` and `spitFire`.
- Spawn rule: `ModSpawns.canMagmamuncherAlphaSpawn`. Loot: `loot_tables/entities/magmamuncher_alpha.json`.
- Devscript: `magmamuncher-alpha.txt`.
