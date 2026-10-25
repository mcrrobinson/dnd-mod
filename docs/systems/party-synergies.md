# Party synergies
Each [party role](party-roles.md) helps the rest of its `/party`: Tanks pull monsters off their friends, Healers heal party members harder, Damage dealers hit harder next to a Tank, Supports give advantage on obstacle checks and Utility characters spot obstacles from further away. A party with three or more roles also earns more class XP, and members who could open an obstacle themselves help each other's rolls.

![An Arcane Seal roll with the "+2 assist" bonus from a Warlock party member](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-party-synergies/synergy-assist.png)

## How it works
Synergies only work between members of the same `/party`, and with [hirelings](hirelings.md) (see below). Each one uses the **primary** role of the class (the secondary role does nothing here). Solo players without hirelings get none of them. Members who are [Downed](death-saves.md) or are the Dungeon Master neither give nor get them. All of them are passive.

| Role | Synergy | Numbers |
|-|-|-|
| Tank | **Bodyguard**: a hostile mob that hurts a party member while it's within 12 blocks of a party Tank may turn on the Tank instead. The nearest Tank gets an action-bar message | 30% chance per hit. A mob already on a party Tank stays there |
| Healer | **Triage**: a Healer's heals on other party members are stronger, and they stabilise a Downed member faster | Cure Wounds heals 10 instead of 8 (+25%). Regrowth's and the Sanctuary party Regeneration last 25% longer. Holding right-click on a Downed member takes 1 s instead of 3 s |
| Damage | **Opportunist**: extra damage to a mob whose current target is a party Tank (not you) | +15% damage |
| Support | **Inspire**: party members within 8 blocks of a Support roll [obstacle](obstacles.md) checks with advantage (two d20s, the higher one counts). The Support doesn't inspire their own rolls | Advantage |
| Utility | **Scout**: party members within 16 blocks of a Utility character, and the Utility character, see the obstacle hint panel from 12 blocks instead of 5. A hint from beyond 5 blocks says "Scouted from afar" | 12-block hints |
| Everyone | **Balanced party**: when class XP from a kill is shared, count the different primary roles among the members sharing it (within 48 blocks) | 3 roles: +10%, 4 roles: +15%, 5 roles: +20%, added before the split |

Healers are the Cleric and the Druid; Bards and Paladins have Healer only as a secondary role, so Song of Rest and Circle of Healing don't get Triage.

Spotted traps and found hidden doors are already shared with your whole party (see [dungeon traps](dungeon-traps.md) and [obstacles](obstacles.md)), so Scout doesn't change that.

### Hirelings
Hirelings count with their role for their owner's party, and for a solo owner as a party of one plus hirelings. A hireling of any party member counts for the whole party.

| Synergy | With hirelings |
|-|-|
| **Bodyguard** | A Tank hireling within 12 blocks of a mob that hurts you (or a party member) is a Bodyguard: 30% the mob turns on it. The nearest Tank, player or hireling, is picked. A mob already on a Tank hireling stays there |
| **Triage** | A Healer hireling's Cure Wounds heals players (you and party members) for 7.5 instead of 6 |
| **Opportunist** | A mob whose target is a Tank hireling counts as targeting a party Tank: Damage players deal +15% to it, and so does a Damage hireling (also against a mob on a Tank player of its side) |
| **Inspire** | A Support hireling within 8 blocks gives your obstacle checks advantage |
| **Scout** | Your own Utility hireling within 16 blocks gives you 12-block obstacle hints, party or not |
| **Balanced party** | Hireling roles count towards the different roles, but hirelings take no share of the XP. A solo Damage player with a Tank and a Healer hireling is a 3-role party: +10% class XP |

The hirelings' own abilities (Taunt, Cure Wounds, Revivify, Hunter's Mark, Rallying Song, Scout and Torchbearer) are on the [hirelings](hirelings.md) page. Each synergy's `[Party]` log line names the hireling as "Brannoc (hireling)".

### Assist on obstacle checks
When you roll an obstacle check (or take your time over one), each other party member within 6 blocks who could attempt that obstacle themselves, as one of its primary or secondary classes, assists. Assistance adds +2 to the roll, and never more than +2 however many help. The d20 panel shows it as "+2 assist". Assist stacks with Inspire: the panel then shows both dice and the bonus.

When an assisted roll opens the obstacle, each assisting member gets half the solver's class XP (Hard: 15 for the solver, 7 for each helper). Like the solver's XP, this is paid once per obstacle group until it's reset.

## Where to find it
Form a `/party` and play near each other. The guidebook's "Your role" page has an "In a party" line with your role's synergy.

## Commands
None. `/dndclass forceroll <player> <naturals>` rigs the dice for testing (with advantage, give two).

## Configuration
None. The numbers are constants in `Party/PartySynergies.java`.

## Known limitations
- Balanced party needs three different primary roles, so a two-player party without hirelings never gets it. Each player has one primary role, so the +15% and +20% steps need four and five players. A Downed member still takes a share of the XP but their role doesn't count.
- Only Cleric and Druid heals use Triage: Cure Wounds, Regrowth and the Sanctuary party Regeneration. Potions, Hit Dice and other healing don't.
- Bodyguard only moves the mob's target. A mob whose AI picks a new target soon after (a creeper, a mob that loses sight) may switch back.
- Inspire and Assist only apply to obstacle checks, not to lockpicking, trap disarming, searching or saving throws.
- Scout only widens the hint panel. You still have to walk up to the obstacle to work it, and a mob under the crosshair hides the hint.

## For developers
- `Party/PartySynergies.java`: every synergy and its constants. `roleOf(player)` is the primary role from `ClassInfo` (works on both sides). Each synergy logs a `[Party]` line in `latest.log` (Bodyguard rolls, Triage amounts, Opportunist damage, Inspire, Assist, Balanced party).
  - Bodyguard and Opportunist: called from `ProgressionEvents.modifyDamage` (the `LivingEntityMixin` damage hook).
  - Triage: `triage(healer, target, amount)` and `triageTicks(...)` in `ClericSkills` (Cure Wounds), `DruidSkills` (Regrowth) and `PowerUpEffect` (Sanctuary); `stabiliseRate(helper, target)` in `Downed/Stabilise`.
  - Inspire and Assist: `obstacleRoll(builder, player, type)` from `ObstacleInteractions.builder`; the assisters' half XP goes through `ObstacleInteractions.solve(..., assisters)`. Uses `D20.Builder.advantage()` and `bonus("obstacle.dndclasses.assist", 2)`, which `DiceRollHud` already draws.
  - Balanced party: `distinctRoles` and `balancedMultiplier` in `PartyEvents.shareXp`, for the `XP_PROGRESSION` channel only.
- Scout: `ObstacleHintHud.scouted` (client) uses the party HUD's member list (`PartyHud.inParty`) and the members' roles from it (`PartyHud.memberRole`), and your own hirelings from the hireling HUD packet (`HirelingHud.roleNear`).
- Hirelings: `rolesNear(player, radius)` returns `RoleHolder`s (name, role, entity, hireling or null) for the player, nearby active members and their side's hirelings (`HirelingAbilities.sideHirelings`). Inspire uses it; Bodyguard and Opportunist also accept a Tank hireling; `distinctRoles(sharers, hirelings)` and `sharingHirelings` feed `PartyEvents.shareXp`. A hireling's Triage and Opportunist are in `HirelingAbilities`.
- Guidebook: `role.dndclasses.<role>.synergy` lang keys on the "Your role" page.
- Devscripts: `party-synergies-host.txt` / `party-synergies-guest.txt` (LAN port 25647; start the guest once the host logs "Started serving on").
- Balanced party: `balanced-party-host.txt` with `balanced-party-guest.txt` and `balanced-party-guest3.txt` (three clients, LAN port 25692; run lines in the host script). Five zombie kills each with 2 roles (x1.0), 3 roles (x1.1), then with the third role Downed (x1.0); `/dndclass progress` before and after each round.
