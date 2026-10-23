# Troll
A big, hunched, green-grey brute with long arms that lurks in swamps, taigas and dark forests. It heals every second and won't stay dead: knock it down and it gets back up unless you burn it. Fire and acid are the answer, which gives Warlocks, Ember Dragonborn and the Staff of Fire a clear job.

![A troll](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-troll/troll-front.png)

## How it works

| Stat | Value |
|-|-|
| Size | 1.3 x 2.9 blocks |
| Health | 70 |
| Armor | 6 |
| Knockback resistance | 50% |
| Claw | 7 |
| Speed | 0.28 |
| Follow range | 32 blocks |
| XP | 25 |

It can step up full blocks. It hunts players, villagers, wandering traders and iron golems, and fights back against anything that hurts it.

### Rend
Once its target is within 2.8 blocks it starts a two-claw combo:

1. A left claw for 7 damage, straight away. It hooks rather than shoves: no knockback, so the second claw can follow.
2. 0.4 seconds later a right claw for 7, if the target is still within 3.2 blocks. The second claw isn't blocked by the first one's hit cooldown.
3. If both claws land, the second one is a **Rend**: 11 damage (7 + 4) and a strong knockback (0.8).

It waits 1.5 seconds after the second claw before the next combo. Rend calls for no saving throw (a Study card says "No saving throws"). Backing off after the first claw dodges the second.

### Regeneration
Every second it heals **3 HP** (green sparkles). It doesn't heal while it's on fire, or for **5 seconds** after it took fire or acid damage (gray smoke instead).

Fire and acid are the damage types in `#dndclasses:troll_bane`: everything in `#minecraft:is_fire` (fire and lava, burning, Fire Aspect and Flame, fireballs, Warlock fireballs, Dragonborn fire, Hellish Rebuke) and everything in `#dndclasses:is_acid` (a Gelatinous Cube's acid for now).

### Felled, then burned
A blow that would kill it **fells** it instead, unless:
- the blow is fire or acid,
- it's on fire, or
- the damage skips death saves (the void, `/kill`).

A felled troll lies face down at 1 HP. It stops attacking, ignores every kind of damage except fire and acid, other mobs leave it alone, and it can't despawn. After **10 seconds** its wounds have knitted and it gets up with **20 HP** and a roar.

While it's down, it **dies for good** if:
- it takes any fire or acid damage,
- it catches fire (lava, a fire block, a burning arrow), or
- a player right-clicks it with **flint and steel** (uses 1 durability) or a **fire charge** (uses the charge).

A killing blow that is itself fire or acid, or lands while it's burning, kills it outright without felling it.

Only that true death drops its loot and XP. They're credited to the player who burned it, or else to the last player who hit it in the last 30 seconds, so player-kill drops still work when the burn comes from lava or a fire block. Killed any other way (`/kill`, the void), it drops nothing.

### Drops
On its true death: 2-4 leather (up to 1 more per Looting level), 1-3 bones and 0-2 rotten flesh. Killed by a player, it has a 6% chance (+2% per Looting level) to drop a [Periapt of Wound Closure](../items/magic-items.md#periapt-of-wound-closure) (unidentified).

### Creature study
A Nature check, DC 13 (it's in `lore/beasts`). A natural 20 shows its weakness: "fire or acid stops it healing; burn it once it falls or it gets back up".

### Tips
- Bring fire. A Fire Aspect sword, Flame bow, Warlock fireballs, Dragonborn fire breath or the Staff of Fire stops its healing and finishes it in one go.
- No fire? Carry flint and steel. Knock it down, then right-click it before it gets up.
- Step back after its first claw to dodge the Rend.

## Where to find it
- Spawns as a monster (weight 6, always alone) in **swamps, mangrove swamps, taigas, snowy taigas, old-growth pine and spruce taigas and dark forests**.
- Like other monsters it needs darkness. Not in peaceful mode, and it despawns like other monsters (but never while felled).
- The DM encounter **Troll under the Bridge** (hard, 1 troll).
- Spawn egg in the mod's creative tab.

## Commands
- `/summon dndclasses:troll`
- `/dm encounter spawn troll_bridge` (see [Dungeon Master](../systems/dungeon-master.md)).

## Known limitations
- Placeholder art and no sounds of its own: it uses ravager and hoglin sounds, pitched down.
- The hitbox stays upright while it lies felled.
- The Underdark doesn't exist yet. `data/dndclasses/underdark_spawns/troll.json` is ready for it, but nothing reads it.
- Rend doesn't knock you Prone yet. Once the Conditions system has merged, "STR 13 or Prone" is a small follow-up.

## For developers
- `entity/TrollEntity.java`:
  - Stats, goals and the combo state (`startCombo`, `tickCombo`, `claw`).
  - Regeneration and the felled timer run in `tick()`, so they also work with `NoAI`.
  - Constants: `REGEN_AMOUNT` 3, `REGEN_INTERVAL` 20, `BANE_TICKS` 100, `FELLED_TICKS` 200, `RISE_HEALTH` 20, `CREDIT_TICKS` 600, `COMBO_REACH` 2.8, `COMBO_FOLLOW_REACH` 3.2, `COMBO_GAP` 8, `COMBO_COOLDOWN` 30, `REND_BONUS` 4, `REND_KNOCKBACK` 0.8.
  - Felling happens in `applyDamage` (after armour), and while felled `damage` ignores everything except `TROLL_BANE` and `#dndclasses:bypasses_death_saves`.
  - `canTakeDamage()` is false while felled, so mob targeting skips it.
  - Loot and XP are gated on `isBurned()` (`dropLoot`, `shouldDropXp`).
  - Tracked data `FELLED` drives the `felled` loop. NBT: `Felled`, `FelledTicks`.
- `entity/ai/goal/TrollComboGoal.java`: a `MeleeAttackGoal` that starts the combo in range.
- Damage type tags: `data/dndclasses/tags/damage_type/troll_bane.json` (`#minecraft:is_fire` + `#dndclasses:is_acid`) and `is_acid.json`. A new acid source should add its damage type to `is_acid`.
- Logs (INFO): `[Troll] felled by <name>`, `[Troll] rises (<hp> HP)`, `[Troll] burned by <name>`. Debug `[MobAction]` lines for `rend`, `felled`, `rise` and `burned`.
- `client/model/TrollModel.java` (head turning) and `client/renderer/TrollRenderer.java`.
- Assets: `geo/entity/troll.geo.json`, `animations/entity/troll.animation.json` (`idle`, `walk`, `run`, `claw_left`, `claw_right`, `rend`, `felled`, `rise`, `roar`) and `textures/entity/troll.png`, all generated by `tools/troll_model.py`. Edit the script and rerun it rather than hand-editing them.
- Data:
  - `loot_tables/entities/troll.json`, and the Periapt drop in `magic/items/BossItemDrops.java`
  - `encounters/troll_bridge.json`
  - `underdark_spawns/troll.json` (`{"entity", "weight", "min", "max", "zones"}`), for the Underdark ticket's loader
  - the spawns and spawn restriction, in `world/gen/ModSpawns.java`
- Devscript `devscripts/troll.txt`: look and hitbox screenshots, regeneration and the bane window, felling, rising, burning, flint and steel, and a Rend fight against a survival player.
