# Rests and charges
Your class's big abilities cost **charges** as well as mana. Each class has a few, shown as gems above the mana bar, and you get them back by resting.

![Two of a Barbarian's three charge gems left above the mana bar, after a Rage](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/rest-core/charges-hud-two.png)

## How it works
### What costs a charge
Charges are spent by **major actives**, on top of their mana:

| Active | Charges |
|-|-|
| Your class's original special (the root of the tree: Rage, Sanctuary, Flurry Rush, ...) | 1 |
| The capstone at the top of the tree (Titan, Meteor Swarm, ...) | 2 |
| Any other active costing 7 or more mana (today only the Paladin's Circle of Healing) | 1 |
| Every other active (War Cry, Volley, Cure Wounds, ...) | 0 |

If the active can't do anything (nothing in sight, inside a Beholder's anti-magic cone), you keep both the mana and the charges. With no charges left the power-up key says "No charges left: rest to recover" and nothing is spent.

### How many you have
Classes are in one of two recharge groups:

| Group | Classes | Max charges at class level 0-3 / 4-6 / 7-9 / 10 | A short rest gives | A long rest gives |
|-|-|-|-|-|
| Short rest | Fighter, Monk, Warlock, Druid, Bard, Rogue, Blood Hunter | 2 / 2 / 3 / 3 | all of them | all of them |
| Long rest | Barbarian, Cleric, Paladin, Ranger, Necromancer, Wizard, Artificer, Alchemist | 3 / 4 / 5 / 6 | 1 | all of them |

- **Wizard, Arcane Recovery:** the first short rest after a long rest gives 2 charges instead of 1.
- **Trickle:** every 10 real minutes you spend alive below your max, you get 1 charge back on your own, so you're never stuck without one.
- Without a class you have no charges, and nothing costs any.

### The HUD
The gems sit in the row above the mana pips, lined up with their right end, and move up with the pips when your air bubbles show or you ride a big mount. Blue gems are charges you have, dark gems are spent ones, and gold gems are temporary charges (spent first). The gold line under the mana pips your active costs only turns gold when you can pay both its mana and its charges.

The skill tree (O) shows the cost too: an active's tooltip says "Costs 2 charges, recharges on a long rest", and the footer reads "Active: Titan (9 mana, 2 charges)".

![The skill tree with Titan's tooltip showing its 2-charge cost](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/rest-core/charges-tree.png)

### Rests
A rest's benefits:

| | Short rest | Long rest |
|-|-|-|
| Health | Spends Hit Dice (below) | Full |
| Mana | Full | Full |
| Charges | By recharge group (above) | All |
| Hit Dice | Spends up to half your pool | Half your pool back (rounded down, at least 1) |
| Effects | - | Clears Poison, Wither, Hunger, Weakness, Slowness and Mining Fatigue. Bad Omen stays |
| Limits | 2 per long rest, 3 minutes apart | 1 per in-game day |

**Hit Dice:** you have one per class level (at least 1). The die is a d12 for Barbarians; d10 for Fighters, Paladins, Rangers and Blood Hunters; d6 for Wizards and Necromancers; and d8 for everyone else. A short rest spends dice one at a time until you're at full health or you've spent half your pool (rounded up), and says what it rolled in chat, for example "Short rest: spent 2 Hit Dice (d10): 7 + 4 = 11 HP".

Charges, Hit Dice and the rest counters are kept through death, relogging and leaving the End, so dying doesn't refill them. Changing class (`/dndclass set`) keeps your charges but caps them at the new class's max, and refills your Hit Dice. Picking your first class starts you full.

## Where to find it
For now the only way to rest is `/dndclass rest`. Campfire short rests, bed and tavern long rests and party camps are coming.

## Commands
Permission level 2.
- `/dndclass rest <player> short|long`: gives the player that rest's benefits, ignoring its limits. It still counts towards them (a long rest uses up today's).
- `/dndclass charges <player> [n]`: prints charges, recharge group, Hit Dice and short rests left; with `n`, sets the charges (capped at the max).
- `/dndclass hitdice <player> [n]`: prints the same; with `n`, sets the Hit Dice left (capped at the pool).

## Configuration
- `/gamerule dndRests false`: turns charges off. Actives only cost mana, as before charges existed, and the gems are hidden. Rests still heal and refill mana.
- `/gamerule dndChargeTrickleMinutes <n>`: minutes per trickle charge (default 10, 0 turns it off).
- `dndDeathSaves`, `dndLastStand` and `dndPvpDowned` are registered for the downed state and do nothing yet.

## Known limitations
- No rest triggers yet besides the command.
- Level-ups raise your max but don't hand out the new charge; rest to fill it.

## For developers
- Package `classes/Rest/`:
  - `RestState`: persistent NBT `dndRest` (charges, temp charges, Hit Dice spent, short rests since the last long rest, last short rest end in overworld time, last long rest day, trickle ticks, Wizard recovery flag, Last Stand cooldown).
  - `Charges`: the max table, `cost(node)` (`SkillNode.chargeCost()` unless `Charges.overrideCost(id, n)` replaced it), `canAfford`, `spend` (temporary charges first), `restore`, `set`, the trickle.
  - `Rests`: `canShortRest` / `canLongRest` return a refusal `Text` or null; `complete(player, kind, source[, companions, startedDay])` applies the benefits without checking limits. A bed rest that skips the night should pass the evening's day as `startedDay`.
  - `RestEvents.AFTER_REST` (items that recharge on a rest, quests) and `RestEvents.ALLOW_REST` (refuse a rest with a reason: dungeons, downed players, curses).
  - `HitDice`: die sizes and spending. `HitDice.conModifier` adds the CON modifier from the [character sheet](ability-scores.md) to each die.
  - `RestSync`: S2C `dndclasses:rest_state` with a `RestSnapshot` (also `RestSnapshot.client`), sent on join, respawn, every change and once a second if something moved. `RestSync.setSession` / `clearSession` show a rest in progress as a bar left of the gems.
  - `DndRules`: the five gamerules.
- `ClassSkills` hooks: `rechargeGroup()`, `shortRestCharges(player, state, max)` (Wizard overrides it), `onShortRest(player, companions)`.
- `DnDClasses.sendPowerupPacket` checks `Charges.canAfford` after the mana check and calls `Charges.spend` on success. `ClassLifecycle.change` calls `Charges.onClassChange`.
- HUD: `Client/Hud/PowerupOverlay.renderCharges`, textures `textures/power/charge_full|empty|temp.png`.
- Devscript: `devscripts/rests-charges.txt` (Rage 3 times, "No charges left", long rest, Titan costs 2, War Cry costs 0, `dndRests false`, air bubbles, death).
