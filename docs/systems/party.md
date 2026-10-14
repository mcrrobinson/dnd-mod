# Parties
Group up with up to 7 other players (8 in total). Party members split XP, can't hurt each other, see each other's health on screen, and get extra help from a Cleric in the group.

![The party HUD in the top-left corner showing the other member's name, health and health bar](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/party-hud.png)

## How it works
### Making a party
Type `/party invite <player>` to invite someone. If you aren't in a party yet, this creates one with you as leader. The other player gets a chat message with a clickable **[Accept]** button, or they can type `/party accept`. Invites expire after 60 seconds.

The player who creates the party leads it. If the leader leaves, the member who joined next takes over. The party disbands when its last member leaves. Parties are saved with the world, so they survive restarts. Pending invites aren't saved.

### What being in a party does
**Shared XP.** XP from orbs, and class XP from kills, is split evenly between you and every party member who is alive, not spectating, in the same dimension and within 48 blocks. Mending takes its share before the split. Leftover fractions carry over per player, so 1-XP orbs aren't lost. If nobody is nearby you keep it all. Class XP grows by 10-20% first when the members sharing it have three or more different roles ([Balanced party](party-synergies.md)).

**No friendly fire.** Party members can't damage each other. That covers melee, arrows, fireballs, thrown potions and explosions, and attacks by a member's tamed pets (wolves, cats and so on). You can still hurt yourself.

**Downed instead of dead.** At 0 HP with a party member within 64 blocks you go [Downed](death-saves.md) instead of dying. Members bring you back by holding right-click on you for 3 seconds (stabilise, then again to help you up), by healing you (Cure Wounds, Circle of Healing, a splash Healing potion), or by feeding you a Potion of Healing, a golden apple or a Mug of Ale.

**Party HUD.** The top-left corner lists the other members, updated every half second. Each entry shows the member's [party role](party-roles.md) icon, the name (with a ★ for the leader), HP as text, and a health bar that is green above 50%, yellow above 25% and red below that. A gold strip shows absorption. A grey name means that member is out of XP range, and dark grey means they're offline. A [Downed](death-saves.md) member shows `DOWNED` in red in place of their HP, with their death saves (`●●○ ✕○○`, passes and fails) at the left end of a pulsing red bar, or `STABLE` in yellow over a pulsing yellow bar; they're also outlined in red through walls within 64 blocks. While a boss bar is showing, the entries stop 8 pixels short of it (at least 50 pixels wide) and cut long names with "...". The HUD hides with F1 and the F3 debug screen.

**Role synergies.** Each member's [party role](party-roles.md) helps the others: Bodyguard, Triage, Opportunist, Inspire, Scout and assisted obstacle checks. See [Party synergies](party-synergies.md).

**Party heals and wards.** Two class skills reach party members further than other players:

| Special | Everyone | Party members |
|-|-|-|
| Paladin, Circle of Healing (tree skill) | Players within 10 blocks are healed to full | Members within 24 blocks (and the Paladin) are healed to full and get Absorption I for 30 s |
| Cleric, Sanctuary | The Cleric gets Mob Repel for 6 / 9 / 12 / 15 s (ranks I-IV) | From rank II, members within 8 / 12 / 16 blocks (ranks II-IV) also get Mob Repel for the same time and Regeneration I 5 s / I 8 s / II 10 s, and mobs drop them as a target. At rank I only the Cleric is covered |

### Elsewhere in the mod
Other systems check the party too:
- **Quests:** starting one shares it with members within 48 blocks; see [Quests](quests.md).
- **Dungeons:** room fights scale with the members within 48 blocks of whoever sets them off, and room and puzzle clear XP is split like kill XP; see [Dungeon encounters](dungeon-encounters.md).
- **Rests:** members resting at campfires within 8 blocks rest together (Song of Rest), and a [party camp](rests.md#party-camps) gives the whole party a long rest at night.
- **Your own area attacks spare the party:** Warlock Fire Breath, the Dragonborn [Breath Weapon](../races/breath-weapon.md) and the Evocation Wizard's Sculpt Spells don't hit members or their pets, and a tamed River Pikehorn won't fight its owner's party.
- **Roles:** the HUD and picker show each class's [party role](party-roles.md).

## Where to find it
There's nothing to craft: use the `/party` commands below. The HUD shows up in the top-left corner once you're in a party with someone else.

## Commands
Every player can use these. No op is needed.

| Command | What it does |
|-|-|
| `/party create` | Start a party with you as leader |
| `/party invite <player>` | Invite an online player. Creates a party if you're not in one |
| `/party accept [inviter]` | Join the party of your most recent invite, or of the named inviter |
| `/party leave` | Leave your party |
| `/party list` | List members, the leader, HP and who's offline. With no party, lists your pending invites |
| `/party kick <player>` | Remove a member (leader only) |

## Known limitations
- XP is split, not copied. With one partner nearby, each of you gets half.
- Only class XP from kills and from dungeon room and puzzle clears is shared. Class XP from other actions (mining, brewing, crafting, the once-a-minute trickle, kills by summons and pets, bounties, quests, obstacles) stays with whoever earned it. Each member gets their share in their own class, and a member with no class gets nothing from it.
- Mobs that members summon or control can still hit other members: Bard animals, Necromancer undead and mobs a Blood Hunter is controlling.
- Party members can hurt each other's pets.
- The party size (8) and XP range (48) are constants in the code, not config options.

## For developers
- `Party/PartyManager.java`: the party registry. It's a `PersistentState` saved in the overworld (`dndclasses_parties`) and also holds the invites.
  - Helpers: `areInSameParty(a, b)` and `nearbyMembers(player, radius)`.
- `Party/Party.java`: one party's member list (the first member is the leader).
- `Party/PartyCommand.java`: the `/party` commands.
- `Party/PartyEvents.java`: the friendly-fire check (`ServerLivingEntityEvents.ALLOW_DAMAGE`, which maps a tamed attacker to its owner), the HUD sync packet (`dndclasses:party_hud`, which carries each member's class id for the role icon, and their UUID, Downed status byte and death save passes and fails) and XP sharing.
  - Other XP systems share through `shareXp(player, amount, channel, grant)`. For example `ProgressionEvents.onKill` calls `PartyEvents.shareXp(player, xp, PartyEvents.XP_PROGRESSION, Progression::addXp)`.
- `mixin/ExperienceOrbEntityMixin.java`: redirects the orb's `addExperience` call to `shareXp`.
- `Client/Hud/PartyHud.java`: the client receiver and HUD drawing.
- The Cleric change is in `Misc/PowerUpEffect.java` and `ClericRules.java`.
- To test with two players, follow the LAN steps in `CLAUDE.md` (`devscripts/lan-host-check.txt` and `lan-guest-check.txt`).
