# d20 Skill Checks
Some actions roll a d20 and add a class modifier. The roll shows just below the crosshair: the die tumbles with a rattle, then lands on the natural roll and shows the sum, the DC and the outcome, with a sound for how it went. A natural 20 always succeeds and a natural 1 always fails.

## How it works

### Lockpicking (Rogue)
*Still being decided. This documents the rule as currently implemented, and it may change.*

- **Which chests are locked:** any chest or trapped chest whose `chests/...` loot table hasn't been rolled yet (nobody has opened it). Barrels and other containers are never locked. These chests are exempt:
  - village chests (any path containing `village`)
  - `chests/hobbit_*`
  - `chests/spawn_bonus_chest`

  A double chest is locked if either half is.
- **Picking the lock:** only a Rogue can try. Right-click with an empty hand or while not sneaking. The roll is d20 + 5 (Dexterity +3, plus expertise with thieves' tools) against a DC that depends on the chest:
  - **DC 15:** `dndclasses:chests/dragon_lair`, `dndclasses:chests/dwarven_fortress_treasury`, and the vanilla `end_city_treasure`, `bastion_treasure`, `ancient_city`, `woodland_mansion` and `stronghold_corridor/crossing/library` chests
  - **DC 10:** every other locked chest
- **Success:** the loot is rolled, so the chest (both halves) stays unlocked for good. It opens about 1.3 s (26 ticks) later, so the roll stays readable. Dwarves who see this still count it as opening their chest.
- **Failure:** you must wait 1.5 s (30 ticks) before retrying. On a natural 1 the pick snaps and you wait 5 s (100 ticks).
- **Other classes:** you get "Locked. A Rogue could pick it..." on the action bar and hear the locked-chest sound.
- **Breaking a locked chest** (anyone in survival, Rogues included): the loot is rolled, and then each stack has a 40% chance of being ruined. The action bar tells you how many stacks were lost.
- Creative and spectator players ignore locks.

### Persuasion (Bard)
- Sneak + right-click a villager with an empty main hand. The villager must have a job, so not a nitwit or jobless, and must not be a baby, asleep or already trading.
- The roll is d20 + 5 (Charisma +3, plus expertise) against DC 12. You get one try per villager, per player, per in-game day.

| Result | Effect |
|-|-|
| Success | +40 `minor_positive` gossip about you, with heart particles |
| Natural 20 | +80 `minor_positive` gossip |
| Failure | No effect; the villager says no |
| Natural 1 | +25 `minor_negative` gossip, with angry particles |

- Gossip is vanilla reputation, so it lowers or raises prices the same way curing a zombie villager does. It spreads to nearby villagers and fades slowly over time.

### Attack rolls (everyone)
- Every full-strength melee swing (attack cooldown at 90% or more) at a living mob rolls. Armor stands are excluded.
- **Critical hit:** a natural 20 deals double damage, with crit particles and the crit sound. Fighters crit on 19-20 (Improved Critical). The crit only applies to the swing that rolled it: if something else cancels that attack (e.g. a Monk holding a sword), it's lost rather than saved for the next hit.
- **Fumble:** on a natural 1 the swing misses entirely and the cooldown resets.
- Only crits and fumbles show on the HUD. All other rolls are silent and hit as normal.
- The modifier shown is the class's attack bonus. It's for display only: it doesn't change whether you hit.

| Attack bonus | Classes |
|-|-|
| +7 | Barbarian, Fighter, Paladin |
| +6 | Ranger, Rogue, Monk, Blood Hunter |
| +5 | Cleric, Druid |
| +4 | Bard, Warlock, Necromancer, Artificer |
| +3 | Wizard, Alchemist |
| +2 | No class |

## Where to find it
Locked chests are found in structure loot chests: dungeons, mineshafts, temples, strongholds, dragon lairs, dwarven fortresses, and so on. Persuasion works on any professional villager.

## Known limitations
- A locked chest looks like any other chest; you only find out it's locked when you try to open it.
- Hoppers can still pull loot out of a locked chest.
- Persuasion cooldowns and lockpick retry timers are kept in memory, so they reset when the server restarts.
- At small window sizes the roll panel can overlap the chat and the action bar.

## For developers
- `classes/SkillChecks/D20.java` holds the roll, the `Skill` and `Outcome` enums, and the `dndclasses:d20_roll` S2C packet (`D20.show`).
- `Lockpicking.java`, `Persuasion.java` and `AttackRolls.java` hold one check each. `D20.register()` is called before `DwarfGrudges` so that a failed pick never reaches the dwarves.
- Mixins:
  - `mixin/LootableContainerBlockEntityAccessor` reads the chest's loot table
  - `mixin/PlayerAttackRollMixin` applies crit damage and plays the crit effects
- `Client/Hud/DiceRollHud.java` draws the HUD panel, using `textures/gui/d20.png` (made by `tools/d20_texture.py`).
- Sounds are `dndclasses:dice.roll`, `.success`, `.failure`, `.critical` and `.fumble`, made by `tools/music-gen/music_gen.py dice_*`.
- `devscripts/d20-skill-checks.txt` tests a locked chest as a Fighter and as a Rogue, then Bard persuasion. It uses the new DevScript step `sneak on|off`.
