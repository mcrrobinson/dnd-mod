# Medusa
A snake-haired lair boss in one of every three desert pyramids and jungle temples, the **Gorgon's Temple**. Meet her eyes and you start turning to stone; look away or raise a shield and her gaze turns back on her. Her hair snakes poison and she looses longbow volleys from range. Earlier visitors stand around her lair as statues.

![A Gorgon's Temple: the Medusa in a desert pyramid's hall, with statues of earlier visitors](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-medusa/medusa-temple-desert.png)

## How it works
**Stats**: 140 health, 8 armour, 2 toughness, 50% knockback resistance, speed 0.25, follow range 32, 80 XP. 0.8 x 1.95 blocks (low enough for a jungle temple's two-block rooms). She never despawns. As a boss she is immune to Petrified, Charmed and Frightened, and Stunned, Paralyzed, Restrained, Prone or Incapacitated last at most 1 s on her, then can't land again for 10 s (see [Conditions](../systems/conditions.md)).

**Petrifying Gaze** (from her own tick, so it works even when she isn't moving):
- Every 1 s (0.5 s once enraged) she checks every player within 24 blocks that she can see and **faces** (within 60° of where her face points). A player who **meets her gaze**, looking within 30° of her eyes, makes a **CON save**, DC 14 (15 enraged). It's one save per player per 5 s (one exposure); her eyes give the elder guardian's curse (high-pitched) and the action bar warns "Her eyes meet yours...".
- **Failure**: **[Restrained](../systems/conditions.md)** while you turn to stone, with a repeat CON save (same DC) every 2 s. Succeed and it ends; fail and you're **Petrified** for 15 s (300 ticks): a grey statue, unable to act, STR and DEX saves fail, damage against you halved, and mobs ignore you.
- **Failure by 5 or more** (total at or below DC - 5): Petrified for 15 s straight away.
- **Looking away** avoids it: only the eyes matter, so look at her feet, turn your back or fight from behind cover.
- **Immune**: blinded players (the Blinded condition or vanilla Blindness), creative and spectator players, and anyone already turning to stone.
- **Shield**: a player who raises a shield toward her (blocking, facing her within 60°) makes no save. Her gaze reflects instead: at most once every 10 s she's **Stunned** for 1 s and takes 4 magic damage, and nearby players read "Medusa reels from her own reflection!".
- **Mobs** within 16 blocks that face her (not bosses, and not creatures immune to Petrified, such as golems or the Warden) save each 5 s against the same DC with the usual +1 for mobs. A failure Petrifies them **for good**: a statue around her lair. A pickaxe shatters a statue for its loot.

**Snake hair**: when you're within 3 blocks, at most every 1.5 s, her snakes bite for 6 damage, then a **CON save** DC 13: failure leaves you **Poisoned** (disadvantage on attack rolls and checks) and gives vanilla Poison I, both for 5 s.

**Longbow**: from 6 to 24 blocks with a line of sight she draws (0.75 s) and looses a volley of 3 arrows fanned over 10°, every 3 s; enraged, 5 arrows every 2.25 s. Each arrow deals 4 damage and can't be picked up.

**Movement**: she keeps 8-14 blocks from her target so the gaze and the bow matter, backing off when you close in. If you stick to her (within 4 blocks, or close for 2 s) she closes in and bites. She ignores statues (Petrified players and mobs). She keeps to her lair: dragged more than 24 blocks from where she spawned, she drops the chase and walks home.

**Phases** (green boss bar, notched, visible within 32 blocks; no music):
- **Start**: as above.
- **Below 50%** (red bar, enraged): "Medusa shrieks, and every serpent on her head rears up!". Her gaze checks twice as often at DC 15, and her volleys are 5 arrows every 2.25 s.

**Saving throws** show on the [save lane](../systems/saving-throws.md), and studying her (V, Arcana DC 15) lists the Petrifying Gaze (CON 14) and Snake bite (CON 13). A natural 20 adds her weakness: "raise a shield at her and her gaze turns back on her; she can't petrify the blinded".

**Rewards** (`entities/medusa`): 4-8 emeralds (+ Looting) and 8-16 arrows. When killed by a player, also 80 XP, one roll on the [Rare magic pool](../systems/magic-loot.md) and 25% (+5% per Looting level) a [Gorgon's Gaze](../items/magic-items.md#gorgons-gaze) (unidentified; added by `BossItemDrops`).

## Where to find it
- **Gorgon's Temples**: one in three desert pyramids and jungle temples, chosen from the world seed and the temple's position (the same temple always is or isn't one). Only temples generated after this update can be one. She stands in the middle of the pyramid's main hall, or at the far end of the jungle temple's upper room, facing the entrance. A temple generated on Peaceful gets an invisible marker instead, which raises her the first time it's loaded on a higher difficulty.
- **Statues**: each Gorgon's Temple also holds 3-6 statues of earlier visitors facing her: zombies, pillagers, a zombie in iron armour and, in the desert, husks. They are indefinitely Petrified mobs, so they stay put, make no sound and only break to a pickaxe (normal loot). Two or three Petrified Remains blocks lie among them.
- **DM encounter**: `/dm encounter` "Gorgon" (deadly, 1 Medusa).
- **Medusa Spawn Egg**: in the mod's creative tab.

## Commands
- `/summon dndclasses:medusa`: a Medusa whose lair is where she spawns.
- `/locate structure minecraft:desert_pyramid` or `minecraft:jungle_pyramid`: find a temple. The log line `[GorgonTemple] desert pyramid at x, y, z: ...` says whether a newly generated temple is a Gorgon's Temple.

## Known limitations
- Only newly generated temples can become Gorgon's Temples.
- A pyramid's floor plan is fixed, so in rare terrain the statue spots can be blocked; statues only go on open floor.
- Model, animations and textures are generated placeholder art (`tools/medusa_model.py`).

## For developers
- `entity/MedusaEntity.java`: stats, the gaze (`gaze`, `faces`, `meetsGaze`, `shieldsFrom`, `reflect`), the bite and volleys (`bite`, `attack`), `CombatGoal` (distance keeping), the lair leash (`setHome`, NBT `Home`), the enrage phase (`BossFight`). Constants: `GAZE_INTERVAL` 20, `ENRAGED_GAZE_INTERVAL` 10, `GAZE_RANGE` 24, `MOB_GAZE_RANGE` 16, `MEET_ANGLE` 30, `FACE_ANGLE` 60, `REFLECT_COOLDOWN` 200, `REFLECT_STUN_TICKS` 20, `REFLECT_DAMAGE` 4, `BITE_RANGE` 3, `BITE_COOLDOWN` 30, `VOLLEY_*`, `KEEP_MIN` 8, `KEEP_MAX` 14, `LEASH` 24. Special actions log `[MobAction] Medusa gaze|reflect|bite|volley|enrage` at DEBUG.
- `entity/MedusaSaves.java`: the `GAZE` (CON 14, tags `magic`, `petrify`) and `SNAKES` (CON 13, tag `poison`) `MobSaveInfo` entries, `ENRAGED_GAZE_DC` 15, `GAZE_WINDOW` 100, `PETRIFY_REPEAT_TICKS` 40, `PETRIFIED_TICKS` 300, `FAIL_BY_PETRIFIES` 5, `SNAKE_POISON_TICKS` 100, and her Study weakness. Conditions she applies use the key `petrifying` (gaze), `snakes` (poison) and, on her, `reflection`.
- `world/gen/temple/GorgonTemple.java`: the 1 in 3 roll (`isGorgonTemple`), the desert and jungle layouts (boss spot, entrance, statue and remains spots) and the statues (`Conditions.preset(mob, PETRIFIED)`, tag `dndclasses.gorgon_statue`). Called per chunk from `classes/mixin/DesertTempleGeneratorMixin` and `JungleTempleGeneratorMixin` (`generate` TAIL).
- `entity/boss/StructureBosses.placeMedusa`: spawns her (tag `dndclasses.medusa_lair`, home at the spot) or a Peaceful marker.
- `magic/items/GorgonsGazeItem.java`: the Gorgon's Gaze.
- Assets: `tools/medusa_model.py` writes `geo/entity/medusa.geo.json`, `animations/entity/medusa.animation.json` (idle, walk, gaze, bow_draw, bow_release, bite, reel) and `textures/entity/medusa{,_glowmask}.png`. `client/model/MedusaModel`, `client/renderer/MedusaRenderer` (glow mask on her eyes).
- Data: `loot_tables/entities/medusa.json`, `encounters/medusa.json`, `tags/entity_types/lore/arcane.json`.
- Devscripts: `devscripts/medusa.txt` (gaze, escalation, looking away, shield reflection, statues, fail by 5, blind immunity, volleys and bite, Gorgon's Gaze on a cow) and `devscripts/medusa-temple.txt` (finds Gorgon's Temples in fresh terrain).
