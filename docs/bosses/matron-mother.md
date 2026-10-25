# Matron Mother
The Underdark's first boss: the high priestess of a drow house, enthroned in the Sanctum at the top of the [Drow Spire](../structures/drow-spire.md). She fights with a poisoned scourge, calls spiders and darkness, lashes out with the Spider Queen's terror, and once she's hurt she fills the room with webs and calls her Elite guards to her side.

![The Matron Mother before her spider throne in the Sanctum](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-drow-spire/drow-spire-sanctum.png)

## How it works
**Stats**: 220 health, 10 armour, 50% knockback resistance, speed 0.28, follow range 32, 80 XP. 0.7 x 2.2 blocks, drawn a head taller than other drow. She never despawns. She's a drow ([Drow](../mobs/drow.md)): she can't be Frightened, spiders and drow are her allies (they can't hurt her or each other), her own webs don't slow her, and she attacks every player below Neutral with the Spider Houses on sight. As a boss she's also immune to Petrified and Charmed, and Stunned, Paralyzed, Restrained, Prone or Incapacitated last at most 1 s on her (see [Conditions](../systems/conditions.md)). She's named "Matron Mother *name* *house*".

| Ability | When | Effect |
|-|-|-|
| **Scourge** | melee | 8 damage, then a **CON save DC 14** against [Drow Poison](../mobs/drow.md#drow-poison) (Poisoned and slowed; three failures in 10 s and you fall asleep) |
| **Spider Swarm** | every 15 s, target within 24 blocks | 2 cave spiders appear beside her and attack her target, at most 6 alive. They drop nothing and vanish when she dies |
| **Darkness** | every 22 s, target within 16 blocks and in sight | A sphere of magical darkness of radius 6 round her target for 8 s, as the [Drow Mage](../mobs/drow.md)'s: everyone inside but drow and spiders is Blinded |
| **Lash of the Spider Queen** | every 18 s, target within 6 blocks | Every player within 6 blocks makes a **WIS save DC 15** or is **Frightened** of her for 6 s |
| **Web Storm** (phase 1) | on entering the phase, then every 12 s, target within 16 blocks | Every player within 16 blocks makes a **DEX save DC 14**. A failure leaves a **cobweb at your feet** (gone after 10 s) and **Restrains** you for 4 s; a success, nothing. She rises 3 blocks and hovers for 3 s, throwing **3 Magic Missiles a second** (2 force damage each, no attack roll) at her target |
| **Call the Elites** (phase 2) | once | Every [Drow Elite](../mobs/drow.md) within 48 blocks (the whole Spire) gets **Strength I** for 2 minutes and runs to her Sanctum before it fights anything else, then fights her foe |

She uses one spell at a time, with at least a second between them, and casts nothing else while the Web Storm lasts.

**Phases** (purple boss bar, notched, visible within 48 blocks; the Matron Mother's own fight music):
- **Start** (purple): "Surface filth, in my Sanctum? Kneel, and the Spider Queen may let you serve as food." when she first targets a player.
- **Below 50%** (pink): "Lolth's web catches every fly!" and the first Web Storm at once; it repeats every 12 s from then on.
- **Below 25%** (red): "To me, my blades! Defend your Mother!" and Call the Elites.
- **Death**: "The Queen of Spiders... will not... forget this..." Her spiders vanish and her webs come down.

**Her Sanctum**: she idles within 4 blocks of where she was placed, before her throne, and chases you anywhere, but drawn more than 14 blocks from it she steps through the shadows back home (a burst of ink).

**Saving throws** show on the [save lane](../systems/saving-throws.md), and studying her (V) lists the Scourge (CON 14), the Lash (WIS 15) and the Web Storm (DEX 14). A natural 20 adds: "her spiders vanish when she dies; past half health she rains webs, past a quarter her Elites come running".

**Rewards** (`entities/drow_matron`, when a player kills her): 80 XP, the **[Matron's Scourge](../items/magic-items.md#matrons-scourge)** (always, unidentified), 1-3 gold blocks, 4-8 Spidersilk (+ Looting), 1-3 Drow Poison Vials, a 15% roll on the [Legendary magic pool](../systems/magic-loot.md), and the **Mother of None** challenge. Killing her costs 400 standing with the Spider Houses, and like any drow earns +30 with the Sylvan Court and +15 with the Grey Clans (see [Factions](../systems/factions.md)).

### Tips
- Fight her from more than 6 blocks and the Lash can't reach you; the Web Storm reaches 16.
- Clear the Spire's lower floors first: below a quarter of her health, every Elite still alive comes running with Strength.
- A Dwarf's poison resilience and a Rogue's poison immunity blunt her scourge.

## Where to find it
- **The Drow Spire**: one Matron Mother in every spire, in the Sanctum at its top. A spire generated on Peaceful gets an invisible marker instead, which raises her the first time it's loaded on a higher difficulty.
- **DM encounter**: `/dm encounter` "Matron Mother" (deadly: the Matron and 2 Drow Elites).
- **Matron Mother Spawn Egg**: in the mod's creative tab.

## Commands
- `/summon dndclasses:drow_matron`: a Matron Mother whose Sanctum is where she spawns.
- `/dndhome drowspire [sanctum|throne]`: teleports you to the nearest spire's Sanctum (see [Drow Spire](../structures/drow-spire.md)).
- `/dndclass forceroll @s <n...>`: rigs your next d20s, to test her saves.

## Known limitations
- Her model is the drow's (the racial humanoid model, scaled up) with her own skin (`tools/drow_skins.py`), not a bespoke model.
- The Web Storm's cobwebs only go where your feet are in air; standing in water or in a doorway block, you're Restrained but no web appears.
- With the Conditions system in, the Web Storm Restrains; the design's "advantage on attacks against her" for Faerie Fire stays a +2 damage hook.

## For developers
- `entity/drow/DrowMatronEntity.java`: stats, the abilities (`swarm`, `darkness`, `lash`, `webStorm` with `ensnare` and `tickStorm`, `callTheElites`), the leash (`setHome`, `leash`, NBT `Home`), the `BossFight` (phases at 0.5 and 0.25), saved summons and webs (NBT `Summons`, `Webs`). Constants: `SWARM_COOLDOWN` 300, `MAX_SPIDERS` 6, `DARKNESS_COOLDOWN` 440, `DARKNESS_RADIUS` 6, `LASH_COOLDOWN` 360, `LASH_RANGE` 6, `WEB_STORM_COOLDOWN` 240, `WEB_STORM_RANGE` 16, `WEB_STORM_TICKS` 60, `WEB_STORM_RISE` 3, `WEB_RESTRAIN_TICKS` 80, `WEB_TICKS` 200, `STORM_DARTS` 3, `CALL_RANGE` 48, `HOME_RADIUS` 14, `WANDER_RADIUS` 4. Special actions log `[MobAction] Matron Mother ... spider swarm|darkness|lash of the spider queen|phase: web storm|web storm|storm missiles|call the elites|shadow step home` at DEBUG (`run/logs/debug.log`); each Web Storm save logs `[DrowMatron] Web Storm: <player> failed the DEX save, webbed at x, y, z` or `... saved, no web` at INFO, and the call `[DrowMatron] ... calls the elites: n answer`.
- `entity/drow/DrowSaves.java`: `MATRON_POISON` (CON 14), `MATRON_LASH` (WIS 15, `MATRON_LASH_TICKS` 120), `WEB_STORM` (DEX 14).
- `entity/drow/DrowEliteEntity.answerCall`: Strength I (`CALL_STRENGTH_TICKS` 2400), +32 follow range, `AnswerCallGoal` until within 5 blocks of the Sanctum (NBT `CalledTo`); logs `[MobAction] ... answer the call`.
- `entity/drow/DrowDarkness.cast(world, centre, radius, caster)`: the bigger Darkness.
- `entity/boss/StructureBosses.placeMatron`: spawns her with her home, or a Peaceful marker.
- Registered in `entity/drow/Drow.java` (`DROW_MATRON`, spawn egg); drawn by `client/drow/DrowClient` (`MATRON`, scale 1.13, `textures/entity/drow/matron.png`).
- Music: `music.matron_fight` (`ModSounds.MUSIC_MATRON_FIGHT`, `sounds/matron_fight.ogg` from `tools/music-gen` `matron_fight`).
- Data: `loot_tables/entities/drow_matron.json` (the Scourge is added by `BossItemDrops`, the Legendary roll by `MagicLoot`), `advancements/underdark/matron.json`, `encounters/drow_matron.json`, `factions/drow.json` (`kill_member_overrides` -400).
- Devscript: `devscripts/matron.txt` (a frozen Matron taken through each phase with `/damage`: Web Storm webs on a forced failed save and none on a forced success, the Elites converging and their Strength, the boss bar and music, the Scourge on her death).
