# Downed and death saves
At 0 HP you don't die straight away if someone could help you. You go **Downed**: you fall flat, crawl, and roll a **death save** on the d20 HUD every 6 seconds until you stabilise, get back up or die. Playing alone you get one desperate **Last Stand** roll instead.

![A Downed player crawling, seen by a party member](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/downed-death-saves/downed-lan-guest-sees-host.png)

![Your own view while Downed: the tally on the action bar](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/downed-death-saves/downed-lan-guest-crawl.png)

![A death save on the d20 HUD](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/downed-death-saves/downed-save-1.png)

## How it works
### When you go Downed
When a hit would kill you, these are checked in order:

1. **A Totem of Undying** in either hand pops as in vanilla. Nothing else happens.
2. **The void and `/kill`** (damage types in the tag `#dndclasses:bypasses_death_saves`) kill you outright.
3. **Massive damage** (5e): if what's left of the hit after 0 HP is at least your max health, you die outright. A Rogue with 14 max HP at 14 HP dies outright from 28 damage, but goes Downed from 27.
4. **`dndDeathSaves 0`**: you die as in vanilla.
5. **A PvP blow** from a player outside your party kills outright if `dndPvpDowned` is false.
6. **An ally nearby**: another party member who's online, alive, not spectating, not Downed themselves, in the same dimension and within 64 blocks (with `dndDeathSaves 2`, any other player there who didn't land the blow). Then you go Downed.
7. **Nobody nearby**: a [Last Stand](#last-stand).

Going Downed sets you to 1 HP, removes Regeneration and Absorption and puts out fire (unless you're still standing in it). You're thrown off a mount and stop gliding. Your party gets a chat line ("Steve is Downed!").

### Being Downed
- You crawl (the swimming pose, 0.6 blocks tall) at half the vanilla crawl speed, about 15% of walking speed. You can't jump, sprint, swim up (you sink), or glide.
- You can't attack, use or break blocks, use items or entities (so no eating or drinking), drop items with Q, or fire your special. Inventory and container screens close. You can look around, chat and change hotbar slot.
- You don't heal (natural regeneration, potions, Regeneration) and you don't regain mana or trickle charges. You can't rest, and going Downed interrupts a campfire short rest in progress (within half a second).
- The action bar shows your tally: `DOWNED ●●○ ✕○○ next save in 4 s`, or `STABLE standing up in 22 s`.

### Death saves
Every 6 seconds (120 ticks), starting 6 seconds after you go down, you roll a flat d20 (no ability modifier) against **DC 10**. It shows on the d20 HUD as "Death Save".

| Roll | Result |
|-|-|
| Natural 20 | Back on your feet with 1 HP |
| 10 or more | A pass. Three passes and you're **stable** |
| 9 or less | A fail. Three fails and you **die** |
| Natural 1 | Two fails |

Halfling Lucky rerolls a natural 1 as it does on other rolls, and advantage sources on your [character sheet](ability-scores.md) that apply to death saves count.

**Stable:** no more saves. After 30 seconds you stand up with 1 HP and Weakness I for 10 seconds. A hit while stable makes you unstable again and counts as a fail.

**Getting up** by any route gives you Resistance I for 3 seconds and one second of immunity, so the same blow can't down you again.

### Hits while Downed
Hits don't take health while you're Downed; each one is a failed death save instead:

- a melee hit (a mob's or player's attack): **2 fails**, at most once per attacker per half second
- anything else (lava, fire, falling, drowning, arrows, explosions): **1 fail**, at most once every 1.5 seconds, so lava kills a Downed player in about 3 seconds
- the void and `/kill`: death

Party members still can't hurt each other. Mobs keep attacking Downed players for now (see Known limitations).

### Helping a Downed player
Anyone who isn't Downed can help. In PvP, the player who downed you won't help you up.

- **Stabilise:** look at the Downed player within 3 blocks and **hold right-click** for 3 seconds with an empty hand (or any item you can't use, like a sword or a block). A progress bar shows on both players' action bars. Letting go, looking away, walking off or taking a hit stops it; keep holding and it starts again. They're **stable**, and you roll **Medicine** (d20 + your sheet's Medicine bonus) against DC 15 on your d20 HUD: on a success they stand up in **10 seconds** instead of 30. Stabilising itself always works.
- **Help up:** let go, then hold right-click for another 3 seconds on a **stable** player: they stand up with **4 HP** (2 hearts).
- **Heal them:** any heal from someone else stands a Downed player up with that much HP (at least 1): Cure Wounds (8 HP), Song of Rest, Elixir of Healing, Circle of Healing, Divine Intervention, splash and lingering Healing potions, Healing arrows, and Regeneration put on after they went down (Druid Regrowth, a Bard's lute, a beacon). Their own healing stays blocked: natural regeneration, Fighter Second Wind, life drain, Hit Dice. Classes immune to potions stay immune: a splash Healing potion stands a Rogue up but not a Paladin, Artificer or Fighter.
- **Feed them:** right-click a Downed player holding:
  - a **Potion of Healing**: its normal heal, so they stand up (not a potion-immune class; the potion isn't used up)
  - a **golden apple**: up with 4 HP and the apple's effects
  - an **enchanted golden apple**: up with 4 HP and all of its effects (Regeneration II, Absorption IV, Resistance, Fire Resistance)
  - a **Mug of Ale**: stabilises them ("A stiff drink.")

### Class features
- **Paladin, Aura of Protection:** Downed players within 8 blocks of a standing Paladin with the aura get **+2** on death saves (shown as "+2 bonus" on the d20 HUD). A Downed Paladin's aura doesn't help anyone.
- **Fighter, Indomitable:** a Fighter who has unlocked Indomitable rerolls their first failed death save each time they're Downed ("Indomitable! You reroll the failed save.").

### Dying
Three fails, giving up, or logging out while unstable kills you with whatever downed you: the death message ("Steve was slain by Zombie"), kill credit and advancements all go to the original attacker. A Totem of Undying in hand at that moment still saves you.

**Give up:** hold the power-up key (Z) for 3 seconds while Downed to die now, so a party that can't reach you doesn't keep you waiting.

**Logging out** while Downed and unstable kills you (no combat logging). If you're stable you stand up with 1 HP as you leave. If the server stops, you're still Downed when you join again, with the same tally and a fresh 6 second timer.

### Last Stand
With nobody to help you, you get one roll against **DC 15** instead (it shows as "Last Stand"):

- **Success** (30% with no bonus): you're back up with 1 HP and Resistance II for 3 seconds. "You refuse to fall!"
- **Failure:** you die as normal.

You get one Last Stand every 10 minutes (12000 ticks of world time). While it's cooling down, solo deaths are instant.

## Where to find it
Anywhere: it's how dying works with the default `dndDeathSaves 1`. To try it, party up with a friend and take a lethal hit, or use `/dndclass down`. To help someone up, hold right-click on them.

## Commands
Permission level 2.
- `/dndclass down <player>`: downs the player, even with nobody near.
- `/dndclass stabilise <player>`: a Downed player stops rolling and stands up in 30 seconds.
- `/dndclass revive <player> [hp]`: a Downed player stands up with `hp` HP (default 1).
- `/dndclass downed <player>`: prints the tally ("Steve: Downed, 1 passes, 2 fails, next save in 4 s").
- `/dndclass laststand <player> [ready]`: prints the Last Stand cooldown; `ready` clears it.
- `/dndclass forceroll <player> <n...>` rigs the next naturals, so you can test any outcome.

## Configuration
- `/gamerule dndDeathSaves <0|1|2>`: 0 off (vanilla deaths), 1 Downed when a party member is near (default), 2 Downed when any player is near.
- `/gamerule dndLastStand false`: no Last Stand; solo deaths are instant.
- `/gamerule dndPvpDowned false`: a blow from a player outside your party kills outright.
- Data pack: add damage types to `data/dndclasses/tags/damage_type/bypasses_death_saves.json` to make them kill outright (default `minecraft:out_of_world`).

## Known limitations
- The stabilise progress is an action-bar line for now (the target's own tally line replaces it once a second); a proper progress bar comes with the downed HUD.
- Heals from new sources (magic items, scripted healers) only revive if they're marked as coming from someone else (see For developers); anything else counts as the Downed player's own and is blocked.
- Mobs don't ignore Downed players yet, so a mob that keeps attacking finishes you in two hits.
- No downed overlay, party HUD status or ally outline yet; the tally is on the action bar. Other players see you crawling.
- Inventory items can still be thrown out of an open inventory screen for the moment it takes the screen to close.
- Massive damage deaths use the hit's normal death message.

## For developers
- Package `classes/Downed/`:
  - `Downed`: the state API, `is(player)` / `isStable(player)` (both sides, from DataTracker bits on the player), `down(player, source)`, `stabilise(player[, ticks])`, `revive(player, hp)`, `bleedOut(player)` (dies with the stored source; a held totem still works), `tally(player)`. The tally is persistent NBT `dndDowned`, cleared on death and respawn.
  - `DownedEvents`: `ALLOW_DEATH` (the order above), `ALLOW_DAMAGE` (hits become fails), the restriction callbacks (in an early phase, before attack rolls), the `RestEvents.ALLOW_REST` veto, join resume, logout death, the C2S `dndclasses:downed_give_up` hold packet, and `AFTER_DOWNED`.
  - `DeathSaves`: `roll(player, label, dc, Advantage)` builds a `RollKind.DEATH` roll through `D20.roll`; `lastStand(player)`.
  - `DeathSaveModifier.EVENT`: `bonus(player)` (summed, capped at +5) and `mode(player)` (advantage / disadvantage) for auras, racial traits, items and boss party wipes.
- Mixins: `PlayerEntityMixin` (the `DND$DOWNED` tracked byte, `updatePose` forced to `SWIMMING`, no `jump` or `checkFallFlying`, records the overflow past 0 HP in `applyDamage`), `LivingEntityMixin` (no `heal`, no sprinting, no `swimUpward`), `LivingEntityInvoker` (`tryUseTotem`), `DownedServerPlayerMixin` (no Q drop).
- `Stabilise`: hold-use help. The client (`StabiliseClient`) sends C2S `dndclasses:downed_help_hold` with the target's entity id while use is held on a Downed player (refreshed every second, -1 on release); the server checks range 3.5, line of sight, an empty or no-use main hand, the helper not hurt or Downed and not the PvP attacker, and counts 60 ticks. `progress(helper)` gives 0-1 for a HUD.
- `Revives`: the heal rule and feeding. `LivingEntity.heal` on a Downed player calls `Revives.onHeal`: it revives when the heal is an outside heal, else it's dropped. Outside heals: anything inside `Revives.asHealer(caster, ...)` landing on someone other than the caster (the power-up key wraps every active skill and special in it), `Revives.healFrom(healer, target, amount)`, and Instant Health / Regeneration effect heals (`DownedHealMixin` redirects the `heal` calls in `StatusEffect`). Feeding is a `UseEntityCallback` (claimed on the client too, so the helper doesn't drink the potion themselves).
- `DeathSaveModifier.rerollFailure(player)`: reroll a failed death save (Fighter Indomitable). Paladin's +2 is `PaladinSkills.auraDeathSaveBonus`, Indomitable is `FighterSkills.indomitableReroll`, both registered in `Revives.register`.
- Client `DownedClient`: closes handled screens and sends the give-up hold state of the power-up key.
- DevScript `holdkey <key> on|off` holds a key binding.
- Devscripts: `devscripts/downed-revive-lan-host.txt` + `downed-revive-lan-guest.txt` (stabilise with a failed and a successful Medicine roll, help up, interruption, golden apple, potion on a Rogue and a Paladin, ale, Cure Wounds, Paladin aura), `devscripts/downed-revive-solo.txt` (splash potion on a Rogue and a Paladin, natural regen and Second Wind blocked, Regeneration revives, Indomitable), `devscripts/downed-solo-laststand.txt` (solo, Last Stand, saves, hits, lava, void, give up), `devscripts/downed-lan-host.txt` + `downed-lan-guest.txt` (party, crawl seen by the guest, bleed out with the original message, logout), `devscripts/downed-interrupts-rest.txt` (going Downed interrupts a campfire short rest).
