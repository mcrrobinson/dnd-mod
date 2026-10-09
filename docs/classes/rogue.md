# Rogue
A fragile scout. You never need to eat, poison can't touch you, and you can turn invisible.

## How it works
| Stat | Rogue | Vanilla |
|-|-|-|
| Max health | 14 (7 hearts) | 20 |

**No poison.** Poison can't be applied to you, from cave spiders, potions or anything else.

**No hunger.** Your food bar never drains and you never starve. The trade-off is that you don't regenerate health from a full food bar either, so you heal with golden apples, potions, beacons or a friend's healing instead.

**Special (power-up key, full mana): Vanish.** Invisibility for 6 seconds at rank I, 9 at II, 12 at III and 15 at IV. Rank it up at an Attunement Table (see [Class progression](../systems/class-progression.md#ability-ranks)). Armor you're wearing still shows, so take it off if you want mobs to lose you completely.

**Lockpicking.** You're the only class that can open locked loot chests. Right click one and you roll d20 + 5 against DC 10, or DC 15 for treasure chests like dragon lairs and dwarven treasuries. See [d20 skill checks](../systems/d20-skill-checks.md#lockpicking-rogue).

![Picking a lock as a Rogue](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/d20-lockpick.png)

### Tips
With no natural regeneration, every hit counts. Carry Potions of Healing or Regeneration and golden apples. You don't need to pack food at all, which frees up inventory space on long trips. Use Vanish to get past a group rather than to fight it, unless you've taken Backstab.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 10 at 2700 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) and rank up abilities at an Attunement Table (see [Class progression](../systems/class-progression.md)).

Rogues get 4 extra XP for each hostile mob killed while sneaking or invisible.

| Skill | Branch | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Vanish | Root | Active | 0 | 9 | Invisibility for 6 / 9 / 12 / 15 s (ranks I-IV) |
| Backstab | Assassin | Passive | 1 | | 50% more melee damage while sneaking or invisible |
| Shadowstep | Assassin | Active | 1 | 4 | Teleport up to 12 blocks the way you're looking, to the furthest spot you fit. No fall damage from the jump |
| Poisoned Blades | Assassin | Passive | 1 | | Your melee hits give Poison I for 3 s |
| Light Feet | Thief | Passive | 1 | | Half fall damage |
| Smoke Bomb | Thief | Active | 1 | 3 | Hostile mobs within 6 blocks get Blindness and Slowness II for 5 s and stop targeting you; you get Speed I for 3 s |
| Danger Sense | Thief | Active | 1 | 4 | For 5 / 6 / 7 / 8 s (ranks I-IV), every projectile that would hit you (arrows, tridents, fireballs, shulker bullets, boss projectiles) flies through you instead. Each one you dodge nudges you sideways with a whoosh and a puff of smoke where it would have landed |
| Fleet | Thief | Passive | 1 | | 15% faster movement |
| Death Mark | Capstone | Active | 2 | 9 | Invisibility, Strength II and Speed II for 10 s |

Vanish followed by sneak attacks with Backstab is the core Assassin play. Shadowstep goes through gaps but not through walls: it stops at the first block in the way.

Danger Sense needs Smoke Bomb. Unlike the Monk's Deflect Missiles (a passive 50% chance), it's a short active window where every projectile misses, so fire it when the skeletons line up. Melee hits still land.

## Commands
- `/dndclass set <player> rogue` (see [Admin commands](../systems/admin-commands.md)).

## Known limitations
- An older class blurb said Nether mobs are allies and Overworld mobs always attack. That isn't in the game.

## For developers
- Hunger: `mixin/HungerManagerMixin`. Poison: `mixin/LivingEntityMixin.onAddStatusEffect`.
- Skill tree: `Progression/Classes/RogueSkills.java`. Ranks: `RogueSkills.VANISH` (read in `PowerUpEffect` case `ROGUE`) and `RogueSkills.DANGER_SENSE`.
- Danger Sense: the `dndclasses:danger_sense` effect (`ModEffects.DANGER_SENSE`). `mixin/ProjectileEntityMixin.dnd$dangerSense` makes `ProjectileEntity.canHit` false for a player who has it, so projectiles pass through, and calls `RogueSkills.sidestep` once per projectile whose path crosses the player's box. `RogueSkills.modifyTakenDamage` also zeroes any projectile damage that still gets through.
- Devscripts: `devscripts/rogue-no-hunger.txt`, `devscripts/rogue-ranks.txt` (Vanish rank durations, Danger Sense against a skeleton).
