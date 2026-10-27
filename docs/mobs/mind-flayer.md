# Mind Flayer
An illithid: a tall, robed humanoid with a mauve octopus head, four face tentacles and blank white eyes that glow in the dark. It haunts the deepest caves with a pair of charmed thralls. It blasts minds in a cone to stun them, grabs the stunned with its tentacles and extracts their brain. Badly hurt or crowded, it blinks away.

![A Mind Flayer grabbing a stunned player](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/mind-flayer/mind-flayer-grab.png)

## How it works

| Stat | Value |
|-|-|
| Size | 0.6 x 1.95 blocks |
| Health | 80 |
| Armor | 6 |
| Tentacle lash (melee) | 5 |
| Speed | 0.27 |
| Follow range | 32 blocks |
| XP | 40 |

It's an elite, not a boss: it has no boss bar, and conditions land on it normally (stun it and it lets go of its victim). It hunts players, villagers and wandering traders, and it takes no fall damage.

### Mind Blast
Every 8 seconds, when its target is in sight within 12 blocks and it's facing it, it winds up for 1 second (purple witch sparks round its head, a high warden charge sound, tentacles splayed). Then it blasts a **60° cone, 12 blocks long**.

Every creature in the cone that it can see makes an **INT saving throw, DC 14**: players, pets, villagers and its target. Other monsters, its own thralls and other Mind Flayers aren't affected.
- **Failure:** 6 psychic (magic) damage and **[Stunned](../systems/conditions.md)** for 4 seconds. Stunned, you can't move, attack or use items, and your STR and DEX saves fail. An **INT save every 2 seconds** (DC 14) ends it early.
- **Success:** 3 damage, not stunned.

One blast is one save per creature. Gnomes roll with advantage (Gnome Cunning: the save is tagged `magic`).

### Tentacles and Extract Brain
Anything **Stunned, Paralyzed or otherwise incapacitated** (but not Petrified) within 2.5 blocks is grabbed by its tentacles: it's held in front of the Mind Flayer and **[Restrained](../systems/conditions.md)**, and squeezed for 4 damage every second.

If it's still holding a victim that's still incapacitated after **2 seconds**, it **extracts the brain**: 16 magic damage that ignores armour, then it lets go. That Downs most characters (a Last Stand for a solo player).

It lets go when:
- the victim struggles free: once the stun ends, press jump for an escape check, the better of Athletics or Acrobatics against DC 14;
- the victim is no longer incapacitated and it's held them for 2 seconds;
- someone other than the victim deals it 10 damage in total;
- it's stunned itself, or it blinks away.

It waits 3 seconds after letting go before grabbing again.

### Blink and levitate
- **Blink:** below 30% health, or when a player who isn't incapacitated stands within 2 blocks, it teleports 8 to 14 blocks away (to a spot that can still see its target when there is one), with purple sparks at both ends. Once every 8 seconds.
- **Levitate:** when its target is 3 or more blocks above it and within 8 blocks sideways, it floats up (Levitation I, 1 second at a time).

### Thralls
A Mind Flayer that spawns naturally, from a spawn egg, a DM encounter or a dungeon room brings **2 thralls** (zombies or skeletons, from the entity tag `#dndclasses:mind_flayer_thralls`). They're **[Charmed](../systems/conditions.md)** by it for as long as it lives: they follow it, attack what it attacks, and can't hurt it.

**Dominate:** every 30 seconds, if it has fewer than 3 thralls within 32 blocks, it picks a hostile mob (not a boss) within 16 blocks. The mob makes a WIS save, DC 14 (+1 for non-players); on a failure it becomes a thrall. Players are never dominated.

Killing the Mind Flayer frees its thralls about 5 seconds later, and they go back to their own targets.

### Drops
1-2 ink sacs (up to 1 more per Looting level). Killed by a player: 15% chance (+2% per Looting level) of an echo shard, and 8% (+2% per Looting level) of a [Tome of Clear Thought](../items/magic-items.md) (Very Rare, unidentified).

### Creature study
It's in the arcane lore group (Arcana, DC 13). The card lists Mind Blast (INT 14, half damage, Stunned) and Dominate (WIS 14). A natural 20 adds its weakness: its tentacles only take hold of a stunned mind, and its thralls go free when it dies.

### Tips
- Break its line of sight or get out of the cone when its head tilts forward and the purple sparks start: you have 1 second.
- If a friend is stunned, hit the Mind Flayer hard: 10 damage makes it drop them before the brain goes.
- High INT helps twice: the first save, and the repeat saves that shorten the stun.
- Charge it and it blinks away, so ranged attacks are often better.

## Where to find it
- Spawns as a monster (weight 1, alone, with its thralls) anywhere in the Overworld **below y=-32**, in the dark and out of the sky.
- In the [Underdark](../systems/underdark.md): weight 3, alone with its thralls, in the Faerzress Hollows.
- An [Illithid Colony](../structures/underdark-lairs.md#illithid-colony) in the Faerzress Hollows: 2 or 3 Mind Flayers, persistent, with 3-5 drow and duergar thralls and captives in the larder.
- Dwarven Ruin dungeons, Challenge tier 4 rooms (weight 1, threat 8).
- DM encounter **Illithid and Thralls** (deadly): one Mind Flayer and its thralls.
- Not in peaceful mode, and it despawns like other monsters.
- Spawn egg in the mod's creative tab.

## Commands
- `/summon dndclasses:mind_flayer` (with no NBT it brings its thralls; with NBT it comes alone)
- `/dm encounter spawn illithid_thralls`

## Known limitations
- Thralls it calls up come from `#dndclasses:mind_flayer_thralls`, which includes `#dndclasses:underdark/thralls`, so drow and duergar can turn up as its thralls in the Overworld too.
- Thralls in a Dwarven Ruin are ordinary zombies and skeletons, not the ruin's revenants.
- It uses warden, squid and elder guardian sounds.

## For developers
- `entity/MindFlayerEntity.java`: stats, goals and every special, run from `mobTick` (which the Conditions mixin skips while it's incapacitated; `tick()` releases the hold if it's stunned). Tracked data `STATE` (blast/grab) and `LEVITATING` drive the animations; `lash`, `extract` and `blink` are triggered animations.
- Saves: `entity/MindFlayerSaves.java` (`MIND_BLAST` INT 14 halves damage, `DOMINATE` WIS 14, `GRAPPLE_DC` 14), registered with `MobSaveInfo`. The stun is `Conditions.of(victim, STUNNED).source(flayer).ticks(80).repeatSave(MIND_BLAST, 40).saveTags("magic", "psychic")`; the hold is `Grapple.hold(victim, flayer, 14, "grapple.dndclasses.mind_flayer")`.
- Thralls: `spawnThralls(world, count)` and `enthrall(mob)` are public, for structures (the Underdark's Illithid Colony) that want to give it more. `isThrall(entity)` checks the Charmed source. `setPendingThralls(0)` stops a placed flayer calling up its own. Mobs with the command tag `dndclasses.colony_thrall` (`COLONY_THRALL_TAG`) are charmed by any flayer within 24 blocks that finds them uncharmed (checked once a second, `claimColonyThralls`); the tag stays, so another flayer takes over if theirs dies.
- `entity/ai/Blink.java`: the shared short teleport (the Lich uses it too).
- Constants: `BLAST_COOLDOWN` 160, `BLAST_WINDUP` 20, `STUN_TICKS` 80, `STUN_SAVE_EVERY` 40, `EXTRACT_TICKS` 40, `EXTRACT_DAMAGE` 16, `SQUEEZE_DAMAGE` 4, `RESCUE_DAMAGE` 10, `BLINK_COOLDOWN` 160, `BLINK_HEALTH` 0.3, `DOMINATE_INTERVAL` 600, `MAX_THRALLS` 3.
- NBT `BlastCooldown` (ticks until it may blast; `0` fires at the next chance). A fresh one waits 60 ticks.
- `MobActions` lines in `debug.log`: `mind blast wind-up`, `Mind Blast`, `tentacle grab`, `Extract Brain`, `blink`, `levitate`, `thrall`, `dominate`. INFO lines: `[MindFlayer] Mind Blast hits ...`, `[MindFlayer] Extract Brain on ...`, `[MindFlayer] blink (...)`, `[MindFlayer] dominate ...`, `[Grapple] the mind flayer grabs|lets go of ...`.
- `client/model/MindFlayerModel.java`, `client/renderer/MindFlayerRenderer.java` (with `AutoGlowingGeoLayer` for the eyes). Assets (`geo/entity/mind_flayer.geo.json`, `animations/entity/mind_flayer.animation.json`, `textures/entity/mind_flayer.png` and `_glowmask.png`) come from `tools/mind_flayer_model.py`; edit the script and rerun it.
- Data: `loot_tables/entities/mind_flayer.json`, the Tome drop in `magic/items/BossItemDrops.java`, `tags/entity_types/mind_flayer_thralls.json`, `tags/entity_types/lore/arcane.json`, `encounters/illithid_thralls.json`, `dungeon_encounters/dwarven_ruin.json` (tier 4), `#dndclasses:habitat/mind_flayer`, and the spawn in `world/gen/ModSpawns.java`.
- Devscript: `devscripts/mind-flayer.txt` (failed and saved blasts with `forceroll`, the grab and Extract Brain, thralls from the spawn egg freed on death, Dominate, blink, a Gnome's advantage).
