# Hirelings
Every Hobbit Tavern has three adventurers looking for work each day. Pay one's fee in gold and they follow you, fight for you, use their role's ability and get you back on your feet when you're Downed, so a solo player isn't left alone. They also count for your [party synergies](party-synergies.md). Keep paying their daily wage and treat them well, or they leave.

![Candidates at the Green Dragon](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/hirelings/hirelings-roster.png)

## How it works
### The roster
- Each innkeeper keeps **3 candidates** a day, each a **different role** out of Tank, Healer, Damage, Support and Utility. The picks come from a seed of the inn's position and the day, so they're the same for everyone.
- At a new day (checked every 100 ticks, also while the innkeeper has no AI) yesterday's unhired candidates leave and today's turn up somewhere inside the inn. A candidate that wanders out is put back.
- A candidate has a name and title ("Brannoc Ashdown, Shieldbearer") and a race: Human 40%, Elf, Half-Orc, Tiefling, Dragonborn or Gnome 12% each.
- **Right-click a candidate with an empty hand** for its card: name, race, role, health, what it does, the wage and a clickable **[Hire: N gold nuggets]**. Ask the innkeeper "Anyone looking for work?" for a pointer.

### Cost, wage and limits
Paid in **gold** from your inventory: nuggets, ingots (9 nuggets) and blocks (81), nuggets first, change given back in nuggets.

| Role | Title | Wage a day | Hire fee (3 days up front) | Health | Gear | Melee |
|-|-|-|-|-|-|-|
| Tank | Shieldbearer | 18 nuggets (2 ingots) | 54 | 40 | chainmail helmet and chest, iron sword, shield | 6 |
| Healer | Acolyte | 27 (3 ingots) | 81 | 28 | leather, a staff (stick) | 3 |
| Damage | Sellsword | 18 | 54 | 30 | leather, bow (iron sword under 4 blocks) | arrows 4-6, sword 6 |
| Support | Minstrel | 18 | 54 | 26 | leather, lute | 3 |
| Utility | Guide | 9 (1 ingot) | 27 | 30 | leather, iron axe, lantern | 5 |

- Your standing with the hobbits changes wages and fees, rounded up: Unfriendly +50%, Friendly -10%, Honored -25%, Exalted -40%. Hostile: "They won't sign on with the likes of you."
- The fee fills the hireling's **purse** with 3 days' wages. Top it up by right-clicking it holding gold (the whole stack goes in, up to 7 days), with the card's **[Pay 1 day]**, or `/hireling pay`.
- **Limit:** 2 hirelings per player, 4 in a whole party.
- **Wage day:** at each new day while you're online, a day's wage leaves the purse. Days you spend offline are free. An empty purse at wage time is an **unpaid day**.
- Speed 0.3 for all. Gear can't be dropped or swapped.

### Loyalty
0 to 100, starting at 50.

| Event | Loyalty |
|-|-|
| A paid day | +5 |
| A kill it makes or helps with (once per mob, within 30 s of its last hit) | +1, at most +5 a day |
| An unpaid day | -25 |
| Knocked out | -10 |
| Hit by its owner (the hit does no damage) | -20 per hit |
| Its owner kills a hobbit or a villager | -50 |

- It **leaves** at 0 loyalty or after **2 unpaid days in a row**: "Brannoc has had enough and leaves your service." It walks off and is gone after 10 seconds. The purse isn't refunded.
- At **80 or more** it's **Devoted**: +10% damage (melee and arrows). The card and the HUD show it (a gold heart).

### Following and fighting
- **Follow** (default) or **Wait**. Sneak + right-click with an empty hand toggles; so do the card's buttons and `/hireling follow|wait`. Right-click with an empty hand for its status card: health, loyalty, purse, mode and **[Pay 1 day] [Wait]/[Follow] [Dismiss]**.
- **Following:** it starts after you beyond 6 blocks (the Healer keeps 4 behind, the others 2-3), teleports to you beyond 24 blocks or when it can't find a path beyond 12, opens doors and swims. Followers within 16 blocks go through portals with you.
- **Waiting:** it stays where you told it, fights anything hostile within 8 blocks and walks back to its spot.
- **Fighting:** it fights back, attacks what hurt you or what you attack, and engages any monster within 10 blocks that targets you, a party member or a hireling. It never targets players, hirelings, villagers and other settlement folk, iron golems, pets or companions. Only the Sellsword goes for creepers (from range). Bosses are fair game.
- The **Shieldbearer** takes half damage from melee blows in front of it.
- The **Sellsword** shoots an arrow every 30 ticks (20 to draw) from up to 14 blocks, and draws its sword when a target is within 4.
- **No friendly fire:** hirelings can't hurt you, your party, any hireling or your (or your party's) pets and companions, and they can't hurt your hirelings.

### Role abilities
Each hireling uses its role's ability on its own, on a cooldown. **Allies** here are you, your online party members and the hirelings of any of you. Abilities stop while the hireling is knocked out, Waiting doesn't stop them, and a [condition](conditions.md) that stops it acting (Stunned, Paralyzed, Petrified, Incapacitated) pauses them.

| Role | Ability | What it does |
|-|-|-|
| Tank | **Taunt** | Every 10 s (200 ticks), when hostile mobs within 8 blocks of it target you or a party member, they turn on the Shieldbearer and stay on it for 5 s (100 ticks), or until it's knocked out. A boss only turns 30% of the time (a roll per boss). Its shield still takes half of frontal melee |
| Healer | **Cure Wounds** | Every 15 s (300 ticks): heals the most hurt ally within 12 blocks who is under half health, Poisoned or Frightened, for 6 HP (7.5 on a player, from Triage), and ends Poisoned (the condition and the Poison effect) and Frightened on them |
| Healer | **Revivify** | It stabilises a Downed ally in 1 s (20 ticks) instead of 3 s. If Cure Wounds is ready, it then stands them straight up with 6 HP (and Cure Wounds goes on cooldown) |
| Damage | **Hunter's Mark** | Every 8 s (160 ticks): its next arrow or sword hit on a mob that is targeting someone else deals +6 damage and makes the mob glow for 5 s (100 ticks) |
| Support | **Rallying Song** | Every 20 s (400 ticks), when an ally within 8 blocks is under half health or Frightened: Regeneration I for 5 s (100 ticks) to every ally within 8 blocks (itself too), and it ends Frightened on them |
| Utility | **Scout** | Passive Perception 15: every second it checks hidden things within 8 blocks of it (traps, hidden doors, a disguised mimic). Anything with a DC of 15 or less is revealed to you and your party as if you'd found it with a Search: traps are outlined, hidden doors revealed |
| Utility | **Torchbearer** | While following you in the dark (light level 3 or less) it puts a torch on the floor every 10 blocks it walks. It carries 16; give it more by right-clicking it with torches (up to 16) |

**Devoted** hirelings heal 10% more (Cure Wounds 6.6, Revivify 6.6 HP), hit 10% harder (Hunter's Mark +6.6) and sing 10% longer. Each use is logged as `[Hireling] <name> <ability> -> <target>`; Scout logs `[Perception] <name> (hireling) spots ...`.

### Knocked out, not killed
- A blow that would kill a hireling **knocks it out** instead: it lies down at 1 HP, can't be hurt and mobs ignore it, for **30 seconds** (600 ticks). Then it gets up with a quarter of its health. It loses 10 loyalty.
- You or a party member can **bring it round early** by right-clicking it with any food or a Potion of Healing (one is used up).
- It's **killed for good** only by the void, `/kill`, or being knocked out while you're more than 64 blocks away (and online) for the whole 30 seconds: "Brannoc didn't make it."

### Death saves: no more lonely Last Stands
- A conscious hireling in **Follow** mode within 64 blocks of you (yours or a party member's) counts as an ally: at 0 HP you go [Downed](death-saves.md) instead of rolling a Last Stand.
- Any hireling within 24 blocks of its Downed owner or a Downed party member stops what it's doing, walks over and **stabilises** them after 3 seconds (60 ticks) beside them (a Healer: 1 second, see Revivify). You then stand up after the normal stable time (30 s). A hit interrupts it: it fights back for about 4 seconds, then tries again.

### When you log out
- Following hirelings leave with you and come back beside you when you join again. Waiting hirelings stay where they are.
- They're kept in a server-wide list, so a copy left in a chunk you didn't load never comes back as a duplicate.

### The HUD
One compact row per hireling under the party HUD (shown without a party too): the role icon, the name, a health bar, a heart with its loyalty (gold when Devoted), days left in the purse, and **wait**, **KO** or **away** (not loaded near you) tags.

## Where to find it
In the [Hobbit Tavern](../structures/hobbit-tavern.md) of every [hobbit village](../structures/hobbit-villages.md). Ops can also use the Hireling Spawn Egg (a candidate) or `/hireling admin spawn`.

## Commands
| Command | Who | Effect |
|-|-|-|
| `/hireling list` | anyone | Your hirelings: health, loyalty, purse, mode and where they are |
| `/hireling hire <uuid>\|nearest` | anyone | Hires a candidate (what the card's button runs), or the nearest within 8 blocks |
| `/hireling pay <name\|all> <days>` | anyone | Pays days into the purse from your gold (up to 7 days) |
| `/hireling follow\|wait <name\|all>` | anyone | Sets the mode |
| `/hireling dismiss <name\|all>` | anyone | Lets them go (the purse isn't refunded) |
| `/hireling admin spawn <owner> <role> [race] [first name]` | op | A hired hireling with a 3-day purse, no gold taken |
| `/hireling admin loyalty <hireling> <0-100>` | op | Sets loyalty (0 makes it leave) |
| `/hireling admin purse <hireling> <nuggets>` | op | Sets the purse |
| `/hireling admin newday <owner>` | op | Runs a wage day now for the owner's loaded hirelings |
| `/hireling admin roster <innkeeper> reroll` | op | New candidates now |
| `/hireling admin knockout <hireling>` | op | Knocks it out |
| `/hireling admin list [owner]` | op | The server-wide list, with stashed and unloaded hirelings |

Names are first names (a prefix works), the UUID, or `all`.

## Known limitations
- Taunt holds mobs by resetting their target every tick, so a mob with its own special targeting (a boss's phase AI) can still act on someone else between ticks.
- Cure Wounds and Rallying Song don't heal Downed players (Revivify does that) or knocked-out hirelings (feed them).
- Scout reveals to you and your party, but only your own Guide widens your obstacle hint panel (the client knows only your own hirelings).
- The Guide doesn't take torches back, and a torch it places is an ordinary torch.
- `/hireling pay`, `follow`, `wait` and `dismiss` only reach hirelings that are loaded; `/hireling list` shows the others as "not nearby".
- Gear can't be changed or dropped.

## For developers
- `entity/HirelingEntity`: the mob. State in tracked data (role, race, skin, flags `FOR_HIRE`/`WAITING`/`KNOCKED_OUT`/`DEPARTING`) and NBT (`Owner`, `Purse`, `Loyalty`, `PaidThrough`, `UnpaidStreak`, `Post`, `KnockedOutTicks`, `RosterInn`, `RosterDay`, `Generation`, ...). Constants for every number above. `knockOut()`, `wakeUp()`, `payDay(owner)`, `changeLoyalty(delta, why)`, `leave(unhappy)`, `setWaiting(...)`.
- `entity/hireling/`: `HirelingRole` (title, wage, health, melee, follow distance, `PartyRole`), `HirelingRace` (weights, skin counts), `HirelingNames`, `HirelingWages` (prices, reputation, taking gold and giving change), `HirelingRoster` (the innkeeper's daily candidates; `InnkeeperEntity.tick` calls it), `HirelingGoals` (knocked out, stabilise, bow, melee, follow, wait, inn wander, and the target goals), `HirelingRegistry` (`PersistentState` `dndclasses_hirelings`: owner -> records with an NBT snapshot, dimension, position, mode, generation and `stashed`), `HirelingCommand`, and `Hirelings` (entity type `dndclasses:hireling`, hiring and paying, the chat cards, friendly fire on `ALLOW_DAMAGE`, knock-outs on `ALLOW_DEATH`, kill/assist loyalty, logout/join/portal handling and the HUD packet `dndclasses:hireling_hud`).
- Persistence: on logout a following hireling is snapshotted and discarded and its record's generation goes up; on join it's recreated from the snapshot with the new generation. `ENTITY_LOAD` discards any hireling whose generation doesn't match its record (a stale copy) or that has no record (dismissed).
- Downed: `DownedEvents.EXTRA_ALLY` is a new event that `hasAlly` checks first; `Hirelings` registers the follower check on it.
- Role abilities: `entity/hireling/HirelingAbilities` (constants for every number in the table). `HirelingEntity.tick` calls `HirelingAbilities.tick` for an active hireling; one cooldown per hireling (NBT `AbilityCooldown`) and the Guide's stock (`Torches`). Taunts are held by a server tick (`TAUNTS`, mob -> tank and expiry). Hunter's Mark and the hireling's Opportunist run in `HirelingAbilities.modifyDamage`, called from `ProgressionEvents.modifyDamage`. Revivify: `HirelingGoals.StabiliseGoal` asks `stabiliseTicks(hireling)` and calls `revivify(...)` after `Downed.stabilise`; it stands the player up through `Revives.healFrom`. Scout uses `PerceptionService.near` and `Perceivable.perceive(player, true)`.
- Allies: `HirelingAbilities.alliedWith(hireling, player)`, `sameSide(a, b)` and `sideHirelings(player, radius)` (the hirelings that count for a player's synergies, used by `PartySynergies.rolesNear`).
- Client: `HirelingHud.roleNear(client, self, role, radius)` (Scout's hint range in `ObstacleHintHud.scouted`).
- Client: `client/renderer/HirelingRenderer` (one `RacialHumanoidRenderer` per race; new `HUMAN` and `HALF_ORC` looks), `Client/Hud/HirelingHud` (under `PartyHud.bottom()`). Human skins: `tools/hireling_skins.py`; the other races use the settlement folk's skins.
- Logs: `[Hireling] ...` for hiring, pay, loyalty changes, knock-outs, stabilising, stashing and rejoining.
- Devscripts: `devscripts/hireling-roles.txt` (every role ability, Revivify, and Balanced party / Opportunist / Inspire with hirelings), `devscripts/hirelings.txt` (roster, card, hire, follow, fight, wages and leaving), `devscripts/hirelings-downed.txt` (Downed instead of Last Stand, stabilised, knocked out and woken with bread), `devscripts/hirelings-relog-set.txt` then `devscripts/hirelings-relog-check.txt` (no duplicates after a relog).
