# d20 and combat racial traits
Four racial traits that hook into dice and combat: Halfling **Lucky**, Half-Orc **Savage Attacks** and **Relentless Endurance**, and Tiefling **Hellish Rebuke**. They need the `dndRaces` gamerule on, like every racial trait.

## How it works
### Lucky (Halfling)
- A natural 1 on a d20 is rerolled once, and the new roll stands, even if it's another 1.
- It covers every roll that reads the character sheet: skill and ability checks (lockpicking, trap disarming, persuasion, obstacles), saving throws, death saves and melee attack rolls. With advantage or disadvantage each die can be rerolled once.
- The HUD shows it: the roll panel's sum line ends in a green **☘ Lucky** that flashes white for a moment as the die settles, and the save lane adds a green ☘ to the row.
- It comes before other rerolls: a Fiend Warlock's Dark One's Own Luck or a Fighter's Indomitable can still reroll the result if it fails.
- `/dndclass sheet` lists it as "Halfling (Lucky): reroll natural 1s".

### Savage Attacks (Half-Orc)
- A critical hit (a natural 20 on the attack roll, or an auto-crit such as Assassinate) deals **×2.5** damage instead of ×2.

### Relentless Endurance (Half-Orc)
- Damage that would kill you leaves you at **1 HP** with **Resistance II for 2 s**, like a free totem without the totem's particles. A ravager roar plays and the action bar says "Relentless Endurance! You cling on at 1 HP (again in 10 min)".
- **Cooldown: 10 minutes.** A long rest recharges it straight away. "Relentless Endurance is ready again" shows on the action bar when it's back.
- It works against "massive damage" (a hit that would take you past 0 HP by your max health or more, which otherwise kills outright even with death saves on).
- It doesn't work against the void or `/kill`, while you're already Downed, or in creative or spectator.
- **Order when you'd die** (`ALLOW_DEATH` phases):
  1. Relentless Endurance (`dndclasses:relentless_endurance`).
  2. The [Phylactery Locket](../items/magic-items.md) (`dndclasses:phylactery_locket`).
  3. The default phase: the [Downed rules](../systems/death-saves.md), which let a held Totem of Undying pop, then go Downed (or Last Stand), or kill you.

  So the free, self-recharging trait is spent first and keeps your totem and the Locket's once-per-long-rest charge. Once it's on cooldown, the next killing blow goes to the totem, Locket and death saves as usual.

### Hellish Rebuke (Tiefling)
- When a living thing hits you in melee (a mob's or player's direct hit; not arrows, spells or thorns) and the hit does damage, the attacker is **set on fire for 4 s** and takes **2 fire damage** (`dndclasses:hellish_rebuke`, tagged `is_fire`, so fire-immune mobs only shrug). Flame particles and a fire charge sound mark it.
- **Cooldown: 20 s**, or **10 s** in the full [Infernal Regalia](../items/armor.md). The action bar shows "Hellish Rebuke! Ready again in 20 s" when it fires and "Hellish Rebuke is ready again" when it's back.
- Not while you're Downed.

### Cooldowns
- Relentless Endurance and Hellish Rebuke cooldowns are stored with your character, so dying or logging out doesn't reset them.

## Where to find it
Pick Halfling, Half-Orc or Tiefling in the race picker, or `/dndrace set <player> halfling|halforc|tiefling`.

## Commands
- `/dndrace cooldowns <player>` prints what's left on Relentless Endurance, Hellish Rebuke and the Breath Weapon; `/dndrace cooldowns <player> reset` readies them all.
- `/dndclass forceroll <player> <n...>` rigs the next d20s. Lucky's reroll takes the next rigged value too, so `forceroll @s 1 15` shows a 1 rerolled into a 15.
- `/dndclass rest <player> long` recharges Relentless Endurance.

## Configuration
- Gamerule `dndRaces`: off means none of these traits apply.
- Gamerule `dndDeathSaves` doesn't change Relentless Endurance: it runs before the death save rules either way.

## Known limitations
- Hellish Rebuke's 2 damage can be swallowed by the attacker's hurt cooldown if you hit it in the half second before; the fire still lands.
- Raw rolls that don't read the sheet (a Cleric's Remove Curse, a DM's flat roll) aren't Lucky yet.
- Hellish Resistance, Darkvision and the other passive traits are separate tickets.

## For developers
- `classes/Race/RaceTraits`, registered from `RaceLifecycle.register()`.
  - Lucky: a character sheet contributor (`dndclasses:race_traits`) that calls `Contribution.rerollNaturalOnes`. `D20.Builder` does the reroll and counts it in `Roll.rerolled`; `AttackRolls` now passes the sheet flag too. HUD: `DiceRollHud` and `SaveLaneHud` draw the clover when `rerolled > 0`.
  - Savage Attacks: `RaceTraits.critMultiplier(player)`, used by `AttackRolls.criticalDamage`.
  - Relentless Endurance: `ServerLivingEntityEvents.ALLOW_DEATH` in phase `RaceTraits.RELENTLESS_PHASE`, ordered before `dndclasses:phylactery_locket` and `Event.DEFAULT_PHASE`. Long rest reset through `RestEvents.AFTER_REST`.
  - Hellish Rebuke: `LivingEntityMixin` injects at the return of `LivingEntity.damage` and calls `RaceTraits.afterDamaged` when the hit landed. The cooldown goes through `RacialGear.rebukeCooldown(player, 400)`.
- Cooldowns are overworld game times in the persistent `dndRace` compound (shared with the Breath Weapon): `relentlessReadyAt`, `rebukeReadyAt`, plus `relentlessNotify`/`rebukeNotify` flags for the one-off "ready again" message, checked once a second.
- Lines in the log: `[Race] <player> Relentless Endurance: survived N damage at 1 HP`, `[Race] <player> Hellish Rebuke on <mob> (N s cooldown)`, and `[D20] ... [rerolled 1]` for Lucky.
