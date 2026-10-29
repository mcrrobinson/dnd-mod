# Dungeon traps
Every dungeon's trap corridor holds 3-5 hidden traps: dart launchers, flame vents and pits that crumble under you. From Challenge II, some locked chests also hide a poison needle. Rogues and Rangers (and anyone with a sharp enough passive Perception) see the traps outlined, and so does the rest of their party. A Rogue or an Artificer can disarm one with Thieves' Tools; anyone can set one off from a distance by throwing an item onto it or shooting it. A spent trap stays spent until the dungeon is reset.

The dungeons themselves are on [Dungeons](../structures/dungeons.md), and the fights on [Dungeon encounters](dungeon-encounters.md).

## How it works
### The traps
Each trap has one **trigger tile** (`dndclasses:trap_trigger`) that looks like the floor around it. It holds the trap's state. Once the trap has gone off or been disarmed, the tile sits slightly lower.

| Trap | What you see | Set off by | Effect | Save |
|-|-|-|-|-|
| Dart | a chiseled wall block with a slit (`dart_launcher`), 2 rows ahead of or behind the tile | the trigger tile | 3 darts, 5 ticks apart, aimed at whoever set it off: 2 / 3 / 4 / 5 damage each by tier, Poison I for 4 s | DEX: no poison |
| Flame | a row of trigger + 2 grated vents (`flame_vent`) across the hall | the trigger or either vent | the vents flare for 1 s. Everyone over the row (up to 2.5 blocks up) takes 4 / 6 / 8 / 10 trap damage by tier and burns for 3 s | DEX: half damage, no burning |
| Pit | a trigger tile in the middle of a row, then 2 rows of faintly cracked floor (`crumbling_floor`) | the trigger or any cracked tile | after 0.5 s the cracked floor gives way. Anyone on it drops about 5 blocks onto pointed dripstone (vanilla stalagmite damage). A ladder on the far wall leads back out | DEX: you catch the edge and land back on the trigger tile |
| Poison needle | nothing (it's under a locked chest, T2+, 35% of chests) | a failed lockpick, or breaking the chest open | 2 trap damage + Poison II for 6 s | CON: half the poison time |
| Rockfall (kobold) | a thin tripline (`kobold_tripline`) across the floor, cracked loose ceiling (`loose_ceiling`) 4 above | the tile under the tripline (a buried trigger) | a 0.5 s rumble with dust, then the 3 x 3 ceiling falls: 4 + 2 x tier trap damage and Prone for 2 s. Rubble stays on the floor until it re-arms | DEX: half damage, not Prone |
| Spikes (kobold) | spike tiles (`spike_trap`) that look like stone, coarse dirt or basalt | any spike tile (a buried trigger beside them) | spikes shoot up for 2 s: 3 + tier trap damage and Slowness II for 3 s | DEX: half damage, no slowness |

- **DC** by tier: 12 / 13 / 15 / 17, the same as dungeon locks. It follows the dungeon's current tier, so `/dungeon tier` changes it.
- Traps are set off by survival/adventure players, mobs and dropped items standing on a tile. Creative and spectator players don't set them off. An arrow (or any projectile) that hits a tile sets the trap off too.
- The darts can't be picked up. Rogues still dodge them with Danger Sense, and the Rogue's poison immunity applies.
- Saves use the save lane (the [saving throws](saving-throws.md) API, with the `trap` tag for advantage filters). One save per trap per victim, even if the trap hits several times.
- Pits can have a straw lid (`covering=straw`) instead of cracked stone; kobold warrens use it.
- A trap goes off once and is then **spent**. Spent and disarmed traps **re-arm** when the dungeon is reset (`/dungeon reset`, or natural repopulation; see [Dungeon loot](dungeon-loot.md)): within a second of the trap's chunk being loaded, a pit's floor comes back and the tile rises again.

### Kobold traps
Traps with `Faction: dndclasses:kobolds` in their trigger are kobold traps, used by [Kobold Warrens](../structures/kobold-warrens.md). They work like the rest, except:
- They spare kobolds, players at Honored or better with the Kobold Tribes, and the drow's allies.
- They re-arm outside dungeons. A spike strip sinks back and re-arms 10 s (200 ticks) after it went off, unless it was disarmed. A spent (not disarmed) kobold trap with a kobold within 16 blocks re-arms itself 2 minutes (2400 ticks) after it went off. A Kobold Trapmaker within 24 blocks, out of combat, walks to any spent or disarmed kobold trap, works on it for 3 s and re-arms it. Re-arming lays the straw or pit floor back, clears the rubble and retracts the spikes.

### Spotting
Every half second, each player within 6 blocks of an armed trap is checked by the `TrapSense` rule. If they spot it, its tiles (or the needle's chest) are outlined in orange dust that only they can see, and the first time they're told "You spot a dart trap" on the action bar. By default:
- Rogues and Rangers always spot traps.
- Anyone else spots a trap if their passive Perception (10 + Perception bonus, see [ability scores](ability-scores.md)) is at least the trap's DC.
- A **Search** (V, see [Stealth and Perception](stealth-and-perception.md)) within 8 blocks whose Perception total meets the trap's DC also reveals it, until it is spent, disarmed or re-armed.
- **Party:** once anyone has spotted a trap, every member of their `/party` sees it too (while it stays armed), whatever their own Perception.

### Disarming
A **Rogue** (or an **Artificer**, as a secondary disarmer) sneak-uses any of a trap's floor tiles (the trigger, a vent, a cracked or straw tile, a spike tile or a tripline) with an empty hand. That rolls **Disarm**, a Thieves' Tools check against the trap's DC, shown on the HUD like lockpicking.
- **Success**: "the dart trap is disarmed". It stays safe until the dungeon resets. It gives up its parts (2 arrows from a dart trap, a redstone dust from a flame or pit trap) and class XP by tier: 5 / 10 / 15 / 25 for Challenge I-IV, once per dungeon reset (a trap re-armed by someone's fumble pays nothing more).
- **Failure**: the trap goes off on the Rogue.
- **Natural 1**: it goes off, and the nearest spent or disarmed trap within 16 blocks arms itself again.

Anyone else who has spotted the trap is told only a Rogue or Artificer could disarm it. They can still set it off from a distance with a thrown item or an arrow. Needles can't be disarmed: a Rogue who picks the lock never sets one off.

## Where to find it
The trap corridor of every dungeon (room 3 on the Crypt's route), on rows 3-12 of the 16-block hall, with at least one plain row between two traps. Each corridor has at least two kinds of trap and at most one pit. Poison needles can be under any dungeon chest from Challenge II.

Kobold traps (rockfall, spikes, straw pits and darts) guard every [Kobold Warren](../structures/kobold-warrens.md); `/dndhome koboldtrap <kind>` builds one to test.

The desert pyramid's pressure plate and the jungle temple's tripwires count as hidden traps too; see [Class-gated obstacles](obstacles.md#hidden-and-locked-rangers-rogues-artificers).

Only dungeons generated after this change have traps. Corridors generated before it stay empty.

## Commands
- `/dungeon reset` (and natural repopulation) re-arms every spent or disarmed trap (as each one's chunk loads).
- `/dungeon tier <1-4>` changes the trap DCs and damage.
- `/execute if block <x y z> dndclasses:trap_trigger[armed=true]` tests a trap; `/data get block <x y z>` shows its kind, links and state.
- `/dndclass forceroll` rigs the next d20 (for testing disarms and saves).

## Configuration
None.

## Known limitations
- Dungeon corridors only use dart, flame, pit and needle traps; rockfalls and spikes are only in kobold warrens.
- Spotting uses passive Perception, not a rolled check when you walk in; roll one yourself with Search (V).
- Dart damage is arrow damage, not the `dndclasses:trap` damage type (so Danger Sense works on it).
- Trap blocks have no items. Place them with `/setblock` plus block entity NBT if you need one by hand.

## For developers
- Blocks: `classes/Blocks/TrapTriggerBlock` (+ `TrapTriggerBlockEntity`, all the trap logic), `DartLauncherBlock`, `FlameVentBlock`, `CrumblingFloorBlock`, `TrapKind`, registered in `TrapBlocks`. All are unbreakable and drop nothing.
- Block entity NBT: `Kind` (dart/flame/pit/needle/rockfall/spikes), `StartKey`, `Tier`, `Armed`, `Disarmed`, `SpentAt` (the dungeon's reset count when spent), `Linked` (launchers, vents, the chest), `Crumbles`, `Ladder`, plus running effects; kobold traps add `Faction`, `Buried` (the trigger itself doesn't fire), `Covering`, `Rubble`, `SpentTime`, `RockfallAt`, `ImpactAt` and `SpikesUntil`.
- Re-arming outside dungeons: `TrapTriggerBlockEntity.rearmOutsideDungeon`; spent kobold traps are listed in `entity/kobold/KoboldTraps` for `KoboldRearmGoal`. `spares(entity)` decides who a kobold trap ignores (`KoboldHostility.sparedByTraps`).
- Kobold trap blocks: `SpikeTrapBlock`, `LooseCeilingBlock`, `KoboldTriplineBlock`; rigs in `world/gen/warren/KoboldTrapRigs`. Devscript `devscripts/kobold-traps.txt`.
- Re-arming: `DungeonState.resets()` goes up on every `reset()`. A trap whose `SpentAt` differs re-arms itself. Repopulation (`DungeonRegistry.repopulate`, used by both the timed repopulation and `/dungeon reset`) calls `reset()`, so it re-arms traps too.
- Party spotting: `TrapTriggerBlockEntity.spottedBy` also checks the in-memory `spotted` set for party members. Disarm XP: `claimDisarmReward()` stores `RewardedAt` (the dungeon's reset count + 1). `TrapDisarm.eligibility(DndCharacter)` is Rogue primary, Artificer secondary.
- Disarm and needles: `classes/SkillChecks/TrapDisarm` (label `D20.DISARM`, Thieves' Tools). `Lockpicking` calls `TrapDisarm.lockFailed` / `lockForced`.
- Spotting rule: `TrapSense.set(...)` replaces the default, which uses `PerceptionService.noticesPassively`. `TrapTriggerBlockEntity` is a `Perceivable`, so Search finds it; `spottedBy(player)` combines the two.
- Generation: `TrapCorridorPiece` lays out the traps from the piece seed. `DungeonPiece.Builder.chest` adds needles at T2+.
- Damage type: `data/dndclasses/damage_type/trap.json` (`ModDamageTypes.TRAP`).
- Logs: `[Trap]` lines for set off, spotted, disarmed, re-armed, every hit (`takes N (dart|trap|stalagmite)`) and pit falls. Disarm rolls log as `[D20] <player> Disarm ...`.
