# Parties
Group up with up to 7 other players (8 in total). Party members split XP, can't hurt each other, see each other's health on screen, and get extra help from a Cleric in the group.

## How it works
### Making a party
Type `/party invite <player>` to invite someone. If you aren't in a party yet, this creates one with you as leader. The other player gets a chat message with a clickable **[Accept]** button, or they can type `/party accept`. Invites expire after 60 seconds.

The player who creates the party leads it. If the leader leaves, the member who joined next takes over. The party disbands when its last member leaves. Parties are saved with the world, so they survive restarts. Pending invites aren't saved.

### What being in a party does
**Shared XP.** XP from orbs, and class XP from kills, is split evenly between you and every party member who is alive, not spectating, in the same dimension and within 48 blocks. Mending takes its share before the split. Leftover fractions carry over per player, so 1-XP orbs aren't lost. If nobody is nearby you keep it all.

**No friendly fire.** Party members can't damage each other. That covers melee, arrows, fireballs, thrown potions and explosions, and attacks by a member's tamed pets (wolves, cats and so on). You can still hurt yourself.

**Party HUD.** The top-left corner lists the other members, updated every half second. Each entry shows the name (with a ★ for the leader), HP as text, and a health bar that is green above 50%, yellow above 25% and red below that. A gold strip shows absorption. A grey name means that member is out of XP range, and dark grey means they're offline. The HUD hides with F1 and the F3 debug screen.

**Cleric special.** It reaches party members too:

| Special | Everyone | Party members |
|-|-|-|
| Paladin, Circle of Healing (tree skill) | Players within 10 blocks are healed to full | Members within 24 blocks (and the Paladin) are healed to full and get Absorption I for 30 s |
| Cleric, Sanctuary | The Cleric gets Mob Repel for 6 / 9 / 12 / 15 s (ranks I-IV) | From rank II, members within 8 / 12 / 16 blocks (ranks II-IV) also get Mob Repel for the same time and Regeneration I 5 s / I 8 s / II 10 s, and mobs drop them as a target. At rank I only the Cleric is covered |

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
- Only class XP from kills is shared. Class XP from other actions (mining, brewing, crafting, the once-a-minute trickle, kills by summons and pets) stays with whoever earned it. Each member gets their share in their own class, and a member with no class gets nothing from it.
- Mobs that members summon or control can still hit other members: Bard animals, Necromancer undead and mobs a Blood Hunter is controlling.
- Party members can hurt each other's pets.
- The party size (8) and XP range (48) are constants in the code, not config options.

## For developers
- `Party/PartyManager.java`: the party registry. It's a `PersistentState` saved in the overworld (`dndclasses_parties`) and also holds the invites.
  - Helpers: `areInSameParty(a, b)` and `nearbyMembers(player, radius)`.
- `Party/Party.java`: one party's member list (the first member is the leader).
- `Party/PartyCommand.java`: the `/party` commands.
- `Party/PartyEvents.java`: the friendly-fire check (`ServerLivingEntityEvents.ALLOW_DAMAGE`, which maps a tamed attacker to its owner), the HUD sync packet (`dndclasses:party_hud`) and XP sharing.
  - Other XP systems share through `shareXp(player, amount, channel, grant)`. For example `ProgressionEvents.onKill` calls `PartyEvents.shareXp(player, xp, PartyEvents.XP_PROGRESSION, Progression::addXp)`.
- `mixin/ExperienceOrbEntityMixin.java`: redirects the orb's `addExperience` call to `shareXp`.
- `Client/Hud/PartyHud.java`: the client receiver and HUD drawing.
- The Cleric change is in `Misc/PowerUpEffect.java` and `Misc/ClericHandler.java`.
- To test with two players, follow the LAN steps in `CLAUDE.md` (`devscripts/lan-host-check.txt` and `lan-guest-check.txt`).
