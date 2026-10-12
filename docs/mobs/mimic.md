# Mimic
A chest that isn't. A sleeping mimic looks exactly like a normal chest until you open it or hit it. Then it bites, grabs you in its jaws and hops after you. When it dies it drops the loot of the chest it replaced.

![A Mimic in its chest disguise](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/mimic.png)

## How it works

| Stat | Value |
|-|-|
| Health | 30 |
| Armor | 4 |
| Knockback resistance | 60% |
| Bite | 6 |
| Speed | 0.28 |
| Follow range | 16 blocks |
| XP | 10 |

**The disguise.** A sleeping mimic is drawn with the real chest texture, so it matches your resource pack. It sits in the middle of a block facing north, south, east or west, makes no ambient sound, casts no shadow and can't be pushed. It's solid, so you can bump into it and stand on it. Mountain Dwarves ignore a sleeping one too.

**Waking.** Right-click it as if to open it, or hit it. It creaks open with a growl, bites straight away and starts hunting you.

**The grab.** A bite that lands tries to grab you: a **STR saving throw, DC 12** (on the [save lane](../systems/saving-throws.md)).
- **Success:** you're bitten (6) but pull free of the jaws. It can't try to grab again for 3 seconds.
- **Failure:** it holds you for 1.5 seconds. You're pulled to its mouth, slowed (Slowness IV) and can't jump out. Once it lets go it can't grab again for 3 seconds. The grab also ends if you get more than 4 blocks away, for example from knockback.

**Struggling free.** While it holds you, the action bar says "You're held! Press jump to struggle free". Each jump press is an escape check: the better of your Athletics (STR) or Acrobatics (DEX) against DC 12, shown on the main d20 panel. A success breaks the grab at once. You can struggle at most once every half second (10 ticks). Mobs and pets it grabs save silently with +1 and can't struggle.

**Going back to sleep.** After 10 seconds with no target, it closes its lid, shuffles back to the middle of its block, squares up and is a chest again.

**Loot.** It drops the loot table of the chest it replaced, plus 10 XP. One from a spawn egg or `/summon` drops simple dungeon chest loot (`dndclasses:entities/mimic`). Killed by a player, every mimic also rolls its own magic (`entities/mimic_magic`): 25% an Uncommon magic item and 10% a Bag of Holding, both unidentified (see [Magic loot](../systems/magic-loot.md)).

**Noticing one.** A dormant mimic gives itself away to a player with passive Perception 13 or more within 6 blocks: "That chest just... breathed." and faint puffs over its lid that only they see. A Search (V) within 8 blocks that totals 13 outlines it in red for 5 s. See [Stealth and Perception](../systems/stealth-and-perception.md).

### Tips
- Be suspicious of a chest in a dungeon, a dragon lair or a dwarven fortress. Left-click it before you open it. A real chest starts to break; a mimic takes the hit and wakes up.
- Don't fight one with your back to a wall. Being held at its mouth for 1.5 seconds hurts less if you can step away right after.
- Caught? Tap jump straight away. The grab is short, so you get two or three tries at most; a nimble character (good Acrobatics) or a strong one (Athletics) usually gets out on the first.
- A mimic you run from goes back to sleep after 10 seconds, still holding its loot. You can come back for it.

## Where to find it
- **Dungeons (monster rooms):** 20% of their chests are mimics with dungeon loot.
- **[Dragon lairs](../structures/dragon-lairs.md) and [frost lairs](../structures/frost-lairs.md):** half of all lairs have a second "chest" 2 blocks from the hoard, holding hoard loot (`chests/dragon_lair` or `chests/frost_lair`).
- **[Dwarven fortresses](../structures/dwarven-fortresses.md):** 10% of chests (treasury, forge, barracks, mine) are mimics with that room's loot.
- Mimic Spawn Egg in the mod's creative tab.

## Commands
- `/summon dndclasses:mimic ~ ~ ~` spawns a sleeping one.
- Add `{Dormant:0b}` to spawn it awake, or `{MimicLoot:"<loot table id>"}` to set what it drops.

## Known limitations
- It's a hostile mob, so in peaceful it disappears along with its loot.
- Mimics only appear in newly generated chunks. A fortress or lair whose generation started before mimics were added may lay out slightly differently in its remaining chunks, because the mimic roll uses extra random numbers.

## For developers
- `entity/MimicEntity.java`: sleep state, waking, bite and grab (`GRAB_TICKS` 30, `GRAB_COOLDOWN` 60), going back to sleep (`SETTLE_DELAY` 200) and the loot override. It's a `Grapple.Holder`: the grab registers with `Grapple.hold` and ends with `Grapple.release` / `breakFree`.
- The grab save is `GrabSaves.MIMIC_GRAB` (`entity/GrabSaves.java`, STR `MIMIC_GRAB_DC` 12, registered with `MobSaveInfo` so creature study lists it). The jump escape is `classes/SkillChecks/Grapple.java`, see [Saving throws](../systems/saving-throws.md#grabs-and-escapes). NBT: `Dormant`, `RestYaw`, `MimicLoot`. Worldgen uses `MimicEntity.disguised(...)`.
- `client/MimicTexture.java` builds the texture at runtime from the vanilla chest texture plus `textures/entity/mimic/mouth.png`, and rebuilds it on every resource reload. `client/model/MimicModel.java` and `client/renderer/MimicRenderer.java` do the model and rendering (no shadow while asleep).
- `geo/mimic.geo.json` uses the vanilla chest UV layout. The teeth and tongue sit inside the closed chest, so they only show when the lid opens. `animations/mimic.animation.json`: dormant, awake, walk, wake, bite.
- Worldgen: `classes/mixin/DungeonFeatureMixin.java` (redirects the chest's `setLootTable` call), `world/gen/lair/LairPiece.java` and `world/gen/fortress/FortressPiece.java` (`Builder.chest` / `mimic`), each with its own `MIMIC_CHANCE`.
- Mountain Dwarves skip sleeping mimics: see the target check in `MountainDwarfEntity`.
- Devscripts: `devscripts/mimic.txt` (disguise, wake, bite, kill); `devscripts/grab-saves.txt` (grab save with `forceroll`, struggle free).
