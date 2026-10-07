# Parties
Players can form a party of up to 8 to adventure together. Party members split XP, can't hurt each other, see each other's health on the HUD, and get extra help from a party Paladin or Cleric.

## How it works
- **Shared XP**: XP from orbs is split evenly between the player who collects it and party members who are alive, not spectating, in the same dimension and within 48 blocks. Mending takes its share before the split. Leftover fractions carry over per player, so 1-XP orbs aren't lost. With nobody nearby, the collector gets everything.
- **No friendly fire**: party members can't damage each other. This includes arrows, fireballs, thrown potions and their tamed pets (wolves, cats and so on). Hurting yourself still works.
- **Party HUD**: the top-left corner lists the other members, updated every 10 ticks (half a second). Each entry shows:
  - the name, with a ★ for the leader. The name is grey when the member is out of XP range and dark grey when they're offline.
  - HP as text, plus a health bar that is green above 50%, yellow above 25% and red below. A gold strip shows absorption.
  - The HUD hides with F1 and the F3 debug screen.
- **Paladin power**: still fully heals every player within 10 blocks. Party members within 24 blocks (and the Paladin) are also healed to full and get Absorption I for 30s.
- **Cleric power**: the mob-repel circle (15s) also covers party members within 16 blocks. They get Mob Repel and Regeneration I for 10s, and mobs drop them as a target.
- **Leadership**: the player who creates the party leads it. If the leader leaves, the next member to have joined takes over. A party disbands when its last member leaves.
- Parties are saved with the world. Pending invites are not saved and expire after 60s.

## Commands
Every player can use these; no op is needed.

| Command | |
|---|---|
| `/party create` | Start a party with you as leader |
| `/party invite <player>` | Invite an online player. Creates a party if you're not in one. They get a clickable **[Accept]** message |
| `/party accept [inviter]` | Join the party of your most recent invite, or of the named inviter |
| `/party leave` | Leave your party |
| `/party list` | List members, the leader, HP and who's offline (also lists pending invites when you have no party) |
| `/party kick <player>` | Remove a member (leader only) |

## Known limitations
- XP is split, not copied: with one partner nearby, each player gets half.
- Class progression XP isn't shared yet, because that system isn't on main. It just needs to call the share hook (see below).
- Mobs that party members summon or control can still hit other members: Bard animals, Necromancer undead and Blood Hunter-controlled mobs.
- The party size (8), XP range (48) and heal range (24) are constants in the code, not config options.

## For developers
- `Party/PartyManager.java`: the party registry. It's a `PersistentState` saved in the overworld (`dndclasses_parties`) and also holds the invites.
  - Helpers: `areInSameParty(a, b)` and `nearbyMembers(player, radius)`.
- `Party/Party.java`: one party's member list (the first member is the leader).
- `Party/PartyCommand.java`: the `/party` commands.
- `Party/PartyEvents.java`: the friendly-fire check (`ServerLivingEntityEvents.ALLOW_DAMAGE`), the HUD sync packet (`dndclasses:party_hud`) and XP sharing.
  - Other XP systems share through `shareXp(player, amount, channel, grant)`, e.g. `PartyEvents.shareXp(player, xp, "progression", Progression::addXp)`.
- `mixin/ExperienceOrbEntityMixin.java`: redirects the orb's `addExperience` call to `shareXp`.
- `Client/Hud/PartyHud.java`: the client receiver and HUD drawing.
- Paladin and Cleric changes are in `Misc/PowerUpEffect.java` and `Misc/ClericHandler.java`.
- To test with two players, follow the LAN steps in `CLAUDE.md` (`devscripts/lan-host-check.txt` and `lan-guest-check.txt`).
