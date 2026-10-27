# Basilisk
A big, slow, heavily armoured eight-legged lizard, about 7 blocks from snout to tail, of the deep caves, with a spiny back and big glowing yellow eyes. Meet its eyes up close and you start turning to stone; raise a shield and its gaze turns back on itself. Its eye makes a **Salve of Stone to Flesh**, which cures petrification.

![A basilisk](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-basilisk/basilisk-big-look.png)

## How it works

| Stat | Value |
|-|-|
| Size | 3.2 x 2.0 hitbox (the model is drawn 2.25x: about 2.8 wide, 2 tall and 7.3 long with head and tail) |
| Health | 75 |
| Armor | 10 |
| Knockback resistance | 80% |
| Speed | 0.18 (slow) |
| Step height | 1 block (climbs full blocks without jumping) |
| Bite | 6, reach 4 blocks from its centre (it bites with its head, not its flank) |
| Follow range | 16 blocks |
| XP | 20 |

It hunts players, villagers and wandering traders, and fights back against anything that hurts it.

### Petrifying gaze
The gaze is passive: the basilisk doesn't have to face you, you have to look at it.

- Every second it checks each player within **8 blocks** who can see it and whose look is within **30°** of its eyes (they're at the front of its head, about 1.6 blocks up, at a player's eye level, and 2.6 blocks ahead of its centre). Looking at its body, its feet or away is safe.
- That player makes a **CON saving throw, DC 12** ("Petrifying gaze" on the save lane), at most once every 5 seconds.
- **Failure:** you start turning to stone. You're **[Restrained](../systems/conditions.md)** (can't move) and make a CON save (DC 12) every 2 seconds. A success ends it; a failure turns you **[Petrified](../systems/conditions.md)** for 10 seconds.
- **Failure by 5 or more** (a total of 7 or less): Petrified for 10 seconds at once.
- Blind players (the Blinded condition or vanilla Blindness), creative and spectator players are safe. Anything you're already turning to stone from (or already Petrified) doesn't call a new save.
- **Other creatures** within 8 blocks that face it and can see it save every 5 seconds (non-players get +1). A failure turns them into **statues**: Petrified for good. Undead aren't spared, but constructs, the Gelatinous Cube, the Warden and bosses are immune (see [Conditions](../systems/conditions.md)), and basilisks don't petrify each other.
- It stops while the basilisk itself can't act (Stunned, Petrified, ...).

### Shield reflection
Raise a shield toward it (blocking, facing within 60°) while within 8 blocks and you make no save. Instead its gaze reflects: the basilisk makes the CON save (DC 12, +1) against its own gaze, at most once every 10 seconds. On a failure it becomes a **statue**, Petrified for good. A statue is silent and persistent, ignores every hit but a player's pickaxe, and a pickaxe shatters it at once with its normal loot (including the eye chance) and kill credit.

### Bite
6 damage, then a **CON saving throw, DC 12**: a failure leaves you **[Poisoned](../systems/conditions.md)** and with vanilla Poison I for 5 seconds.

### Drops
1-3 leather (up to 1 more per Looting level). Killed by a player (a shattered statue counts), a **Basilisk Eye** 35% of the time, +5% per Looting level.

### Salve of Stone to Flesh
- Crafted (shapeless): **Basilisk Eye + Honey Bottle + Glow Berries → 2 salves**. The honey bottle's glass bottle comes back. Stacks to 16. The recipe unlocks in the recipe book once you hold a Basilisk Eye.
- **Use it** (right-click) to apply it to yourself, or **right-click another creature** with it to apply it to them. It ends Petrified and the turning-to-stone Restrained (the Basilisk's and the Medusa's, condition key `petrifying`), and uses up one salve. Nothing to cure: nothing happens and it isn't used.
- A Petrified player can't use it on themselves (they're incapacitated): an ally has to. No hold is needed.
- On a statue mob it brings the mob back to life; a hostile one turns on you straight away.
- Right-clicking with the salve comes before the creature's own interaction, so a petrified villager is cured rather than opening its trades.

### Tips
- Fight it with your eyes down: aim at its legs and body. It's slow, so you can back off and come in from the side.
- A shield is the best answer: hold it up when you close in and the basilisk may turn itself to stone, and then a pickaxe finishes it.
- Carry a salve before going deep, and give one to a friend: they can cure you while you're a statue.
- Strong Constitution helps against both the gaze and the bite.

## Where to find it
- Spawns as a monster (weight 2, always alone) anywhere in the Overworld, **below y=-16**, in the dark and out of the sky. It needs room for its body: a clear 4 x 4 x 2 space (vanilla's spawn space check), so it turns up in the big open caves rather than tunnels.
- In the [Underdark](../systems/underdark.md): weight 4, alone, in the Grey Reaches and the Faerzress Hollows.
- A [Basilisk Den](../structures/underdark-lairs.md#basilisk-den) in the Grey Reaches or the Faerzress Hollows: one basilisk, persistent, among its petrified drow and duergar victims.
- Dungeon rooms and DM encounters place it through `EncounterSpawner`, which looks for a spot with room for its hitbox.
- Dwarven Ruin dungeons, tier 4 rooms (weight 1, threat 4).
- The DM encounter **Basilisk Den** (hard, 1 basilisk).
- Spawn egg in the mod's creative tab.

## Commands
- `/summon dndclasses:basilisk`
- `/dm encounter spawn basilisk_den` (see [Dungeon Master](../systems/dungeon-master.md))

## Known limitations
- Placeholder art, and no sounds of its own: it uses hoglin and turtle sounds.
- The gaze checks where its head is from its body's facing; a basilisk turning fast can be a few degrees off for a tick.
- The hitbox is square and covers the body only: the head and tail stick out of it and can't be hit. At 3.2 wide it can't path through gaps narrower than 4 blocks.
- Too wide for a Drow Outpost's pen cell, so it isn't in `#dndclasses:underdark/pen_beasts`.

## For developers
- `entity/BasiliskEntity.java`: stats, goals, the bite, the gaze from the server `tick()` (`tickGaze`, `gaze`, `reflect`, `gazeAtMobs`, guarded by `Conditions.canAct`), `eyesPos()` and `isLookedAtBy(viewer, degrees)`, the natural spawn rule `canSpawn`. Size: `WIDTH` 3.2, `HEIGHT` 2.0 (registered in `ModEntityTypes`), `MODEL_SCALE` 2.25 (the renderer's `withScale`; eye height and head offset are the .geo.json's 0.7 and 1.15 times it), `BITE_REACH` 4.0 (its `MeleeAttackGoal` override), step height 1.0, and `getVisibilityBoundingBox` grown 2.6 blocks so the head and tail don't vanish at the screen edge. Constants: `GAZE_RANGE` 8, `GAZE_ANGLE` 30, `GAZE_INTERVAL` 20, `GAZE_ROLL_TICKS` 100, `SHIELD_ANGLE` 60, `REFLECT_COOLDOWN` 200, `MOB_GAZE_INTERVAL` 100, `MAX_SPAWN_Y` -16.
- `BasiliskEntity.petrifying(target, basilisk)` applies the turning-to-stone Restrained (key `petrifying`, repeat save every 40 ticks, escalating to Petrified 200).
- Saves: `entity/BasiliskSaves.java` (`GAZE` and `BITE`, CON 12, registered with `MobSaveInfo` so the [Study card](../systems/creature-study.md) lists them; `PETRIFYING_KEY`, `PETRIFIED_TICKS` 200, `FAIL_BY` 5, `POISON_TICKS` 100). Weakness (natural 20 study): "raise a shield and its gaze turns it to stone". Lore: `#dndclasses:lore/beasts`.
- `classes/Items/SalveOfStoneToFleshItem.java`: `needsSalve`, `cure(target, user)` (logs `[Salve] ... cured|statue restored|nothing to cure`), and a `UseEntityCallback` for right-clicking creatures.
- `client/model/BasiliskModel.java`, `client/renderer/BasiliskRenderer.java`: `withScale(MODEL_SCALE)`, shadow radius 1.8, glowing eyes (`AutoGlowingGeoLayer`, off while petrified), and the stone tint for a petrified basilisk (GeckoLib renderers don't go through `LivingEntityRendererMixin`).
- Assets: `geo/entity/basilisk.geo.json`, `animations/entity/basilisk.animation.json` (`idle`, `walk`, `gaze`, `bite`, `statue`), `textures/entity/basilisk.png` and `basilisk_glowmask.png`, and the item icons, all generated by `tools/basilisk_model.py`; edit the script and rerun it rather than hand-editing them.
- Data: `loot_tables/entities/basilisk.json`, `recipes/salve_of_stone_to_flesh.json` (+ its recipe-book advancement), `encounters/basilisk_den.json`, the tier 4 entry in `dungeon_encounters/dwarven_ruin.json`, and the Underdark habitat tag `#dndclasses:habitat/basilisk`. Spawns in `world/gen/ModSpawns.java`; dens in `world/underdark/lair/`.
- Logs: `[MobAction] Basilisk gaze|reflected gaze|bite -> <target>` (DEBUG, `debug.log`), `[Basilisk] <player> reflects its gaze back with a shield: ...` and the usual `[Conditions]` lines.
- Devscript: `devscripts/basilisk.txt` (gaze, range and angle, fail by 5, salve on yourself and on a statue, mob statues, shield reflection and shattering, bite, recipe).
