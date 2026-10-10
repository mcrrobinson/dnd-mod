# d20 Skill Checks
Some actions roll a d20 and add a class modifier. The roll appears just below the crosshair: the die tumbles with a rattle, lands on the natural roll, then shows the total, the DC and the outcome with a sound to match. A natural 20 always succeeds and a natural 1 always fails.

![A Rogue's lockpicking roll on the HUD](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/d20-lockpick.png)

## How it works
### Lockpicking (Rogue)
*This rule is still being decided and may change.*

Unopened loot chests are locked. A chest or trapped chest counts as locked while its `chests/...` loot table hasn't been rolled yet, which means nobody has opened it. Barrels and other containers are never locked, and neither are village chests (any loot table path containing `village`), `chests/hobbit_*` and the spawn bonus chest. A double chest is locked if either half is.

Only a Rogue can pick a lock. Right click the chest as if opening it (if you're sneaking, your hands must be empty). The roll is d20 + 5 (Dexterity +3, plus expertise with thieves' tools) against a DC set by the chest:

| DC | Chests |
|-|-|
| 15 | `dndclasses:chests/dragon_lair`, `dndclasses:chests/frost_lair`, `dndclasses:chests/dwarven_fortress_treasury`, and vanilla `end_city_treasure`, `bastion_treasure`, `ancient_city`, `woodland_mansion`, `stronghold_corridor`, `stronghold_crossing` and `stronghold_library` |
| 10 | Every other locked chest |

On a success the loot is rolled, so the chest (both halves) stays unlocked for good. It opens 1.3 seconds later so you can read the roll first. [Mountain Dwarves](../mobs/mountain-dwarves.md) who see you still count it as opening their chest.

On a failure you wait 1.5 seconds before you can try again. On a natural 1 the pick snaps and you wait 5 seconds.

Anyone else who tries to open a locked chest gets "Locked. A Rogue could pick it..." on the action bar and hears the locked-chest sound. Without a Rogue you can break the chest instead (anyone in survival can, Rogues included). The loot is rolled and drops, but each stack has a 40% chance of being ruined. The action bar tells you how many stacks were lost.

Creative and spectator players ignore locks.

### Persuasion (Bard)
Sneak and right click a villager with an empty main hand. The villager must have a job (not a nitwit or unemployed) and must not be a baby, asleep or already trading. The roll is d20 + 5 (Charisma +3, plus expertise) against DC 12. You get one try per villager, per player, per in-game day.

| Result | Effect |
|-|-|
| Success | +40 `minor_positive` gossip about you, with heart particles |
| Natural 20 | +80 `minor_positive` gossip |
| Failure | Nothing happens; the villager says no |
| Natural 1 | +25 `minor_negative` gossip, with angry particles |

Gossip is vanilla reputation, so it lowers or raises that villager's prices the same way curing a zombie villager does. It spreads to nearby villagers and fades slowly over time. Persuade the villagers whose trades you use most.

### Attack rolls (everyone)
Every full-strength melee swing (attack cooldown at 90% or more) at a living mob rolls a d20. Armor stands don't count. Wait for the cooldown bar to fill if you want your crits.

- **Critical hit:** a natural 20 deals double damage, with crit particles and the crit sound. Fighters crit on 19 or 20 (Improved Critical). The crit only applies to the swing that rolled it. If something else cancels that attack (a Monk swinging a sword, for example), the crit is lost.
- **Fumble:** a natural 1 misses entirely and resets your cooldown.

Only crits and fumbles show on the HUD. Every other roll is silent and hits as normal. The modifier shown is your class's attack bonus, which is for display only and doesn't change whether you hit. A [+N magic weapon](magic-items.md#1--2--3-gear) adds its bonus to it.

| Attack bonus | Classes |
|-|-|
| +7 | Barbarian, Fighter, Paladin |
| +6 | Ranger, Rogue, Monk, Blood Hunter |
| +5 | Cleric, Druid |
| +4 | Bard, Warlock, Necromancer, Artificer |
| +3 | Wizard, Alchemist |
| +2 | No class |

## Where to find it
Locked chests are the loot chests in structures: dungeons, mineshafts, temples, strongholds, dragon lairs, dwarven fortresses and so on. Bring a Rogue for the DC 15 hoards. Persuasion works on any villager with a job.

## Known limitations
- A locked chest looks like any other chest. You only find out when you try to open it.
- Hoppers can still pull loot out of a locked chest.
- Persuasion cooldowns and lockpick retry timers are kept in memory, so they reset when the server restarts.
- At small window sizes the roll panel can overlap the chat and the action bar.

## For developers
- `classes/SkillChecks/D20.java` holds the roll, the `Skill` and `Outcome` enums, and the `dndclasses:d20_roll` S2C packet (`D20.show`).
- `Lockpicking.java`, `Persuasion.java` and `AttackRolls.java` hold one check each. `D20.register()` is called before `DwarfGrudges` so that a failed pick never reaches the dwarves.
- Mixins: `mixin/LootableContainerBlockEntityAccessor` reads the chest's loot table, and `mixin/PlayerAttackRollMixin` applies crit damage and plays the crit effects.
- `Client/Hud/DiceRollHud.java` draws the HUD panel with `textures/gui/d20.png` (made by `tools/d20_texture.py`).
- Sounds are `dndclasses:dice.roll`, `.success`, `.failure`, `.critical` and `.fumble`, made by `tools/music-gen/music_gen.py dice_*`.
- `devscripts/d20-skill-checks.txt` tests a locked chest as a Fighter and as a Rogue, then Bard persuasion.
