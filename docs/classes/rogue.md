# Rogue
A fragile scout. You never need to eat, poison can't touch you, and you can turn invisible.

## How it works
| Stat | Rogue | Vanilla |
|-|-|-|
| Max health | 14 (7 hearts) | 20 |

**No poison.** Poison can't be applied to you, from cave spiders, potions or anything else.

**No hunger.** Your food bar never drains and you never starve. The trade-off is that you don't regenerate health from a full food bar either, so you heal with golden apples, potions, beacons or a friend's healing instead.

**Special (power-up key, full mana): Vanish.** Invisibility for 15 seconds. Armor you're wearing still shows, so take it off if you want mobs to lose you completely.

**Lockpicking.** You're the only class that can open locked loot chests. Right click one and you roll d20 + 5 against DC 10, or DC 15 for treasure chests like dragon lairs and dwarven treasuries. See [d20 skill checks](../systems/d20-skill-checks.md#lockpicking-rogue).

![Picking a lock as a Rogue](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/d20-lockpick.png)

### Tips
With no natural regeneration, every hit counts. Carry Potions of Healing or Regeneration and golden apples. You don't need to pack food at all, which frees up inventory space on long trips. Use Vanish to get past a group rather than to fight it, unless you've taken Backstab.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 6 at 1000 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) at an Attunement Table.

Rogues get 4 extra XP for each hostile mob killed while sneaking or invisible.

| Skill | Branch | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Vanish | Root | Active | 0 | 9 | Invisibility for 15 s |
| Backstab | Assassin | Passive | 1 | | 50% more melee damage while sneaking or invisible |
| Shadowstep | Assassin | Active | 1 | 4 | Teleport up to 12 blocks the way you're looking, to the furthest spot you fit. No fall damage from the jump |
| Poisoned Blades | Assassin | Passive | 1 | | Your melee hits give Poison I for 3 s |
| Light Feet | Thief | Passive | 1 | | Half fall damage |
| Smoke Bomb | Thief | Active | 1 | 3 | Hostile mobs within 6 blocks get Blindness and Slowness II for 5 s and stop targeting you; you get Speed I for 3 s |
| Fleet | Thief | Passive | 1 | | 15% faster movement |
| Death Mark | Capstone | Active | 2 | 9 | Invisibility, Strength II and Speed II for 10 s |

Vanish followed by sneak attacks with Backstab is the core Assassin play. Shadowstep goes through gaps but not through walls: it stops at the first block in the way.

## Commands
- `/dndclass set <player> rogue` (see [Admin commands](../systems/admin-commands.md)).

## Known limitations
- An older class blurb said Nether mobs are allies and Overworld mobs always attack. That isn't in the game.

## For developers
- Hunger: `mixin/HungerManagerMixin`. Poison: `mixin/LivingEntityMixin.onAddStatusEffect`.
- Skill tree: `Progression/Classes/RogueSkills.java`.
- Devscript: `devscripts/rogue-no-hunger.txt`.
