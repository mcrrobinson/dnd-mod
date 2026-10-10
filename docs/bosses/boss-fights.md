# Boss fights
Bosses and the big dragons share one fight system: a boss bar, fight music, health phases and kill rewards. This page covers what they have in common. Each boss has its own page for its attacks.

![A Lich in a stronghold library with its purple boss bar at the top of the screen](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/boss-fights-lich-bar.png)

## How it works
**Boss bar.** The bar appears for every player within range (48 or 64 blocks, depending on the boss) while the boss is fighting a player, which means it's targeting a player or a player has hurt it. The bar goes away when the fight ends, when the boss dies, or when you leave range. Spectators and dead players don't see it.

**Music.** A boss with a fight track plays it for everyone who can see the bar, looping over any other music. When the fight ends there are 20 seconds of quiet before normal music comes back. See [Music](../music.md).

**Phases.** At set health thresholds a boss moves into a new phase, once each, in order. The bar changes colour (yellow to red at half health, for example) and the boss gets new behaviour. The phase is saved with the boss, so logging out halfway through doesn't reset it.

**Downed players.** Bosses ignore [Downed](../systems/death-saves.md) players like any mob, but their area attacks still cost fails. The fight (and its bar) stays on while a Downed player from it is in range, even with nobody left standing for the boss to target. If **every** player in range is Downed, it's a **party wipe**: their death saves roll at disadvantage, so a fight can't stall with everyone crawling. Nobody in range can start a rest while the fight is on.

![A party wipe at a Lich: the death save rolls two dice and keeps the lower](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/downed-mobs/downed-wipe-3.png)

**Rewards.** Bosses give a fixed amount of XP and drop their own loot, and some grant an advancement to the player the game credits with the kill.

| Boss | Health | Bar | Range | Music | Phases | XP | Advancement |
|-|-|-|-|-|-|-|-|
| [Goblin Warlord](goblin-warlord.md) | 250 | yellow, notched | 48 | none | red enrage below 50% | 60 | none |
| [Magmamuncher Alpha](magmamuncher-alpha.md) | 300 | yellow, notched | 48 | Tooth and Claw | red enrage below 50% | 100 | none |
| [Lich](lich.md) | 300 | purple, notched | 48 | Lich theme | blue below 60%, red below 30% | 150 | Lichbane |
| [Beholder](beholder.md) | 250 | purple, notched | 48 | Tooth and Claw | red enrage below 50% | 120 | An Eye for an Eye |
| [Wyvern](../mobs/dragons.md) (wild) | 40 | red | 64 | Tooth and Claw | none | 20 | Dragon Slayer |
| [Lightning Chaser](../mobs/dragons.md) (wild) | 200 | yellow | 64 | Tooth and Claw | none | 80 | Dragon Slayer |
| [Frost Drake](../mobs/dragons.md) (wild) | 200 | blue | 64 | Tooth and Claw | none | 80 | Dragon Slayer |

Tamed dragons never start a boss fight. The Ember Wyvern is a dragon too, but a common one, so it has no bar or music.

### Tips
- The bar tells you when a boss changes phase. When it turns red, expect something new: summoned minions, faster attacks or more damage.
- Several bosses gain new attacks at range when they enrage, so plan your cover before you get them to half health.
- A boss that has fought a player doesn't despawn, so you can retreat, heal and come back.

## For developers
- See [Boss framework](../dev/boss-framework.md) for adding a boss with `BossFight`.
- `BossFight.fightNear(player)` / `inAnyFight(player)` find the fight a player is in (every `BossFight` that has ticked on the server is tracked until its boss is removed); `isPartyWiped()` is the party-wipe check, used by `DownedCombat`.
