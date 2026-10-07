# Boss fights
Bosses (and the big dragons) share one fight system with a boss bar, fight music, health phases and kill rewards.

## How it works
- **Boss bar:** appears for every player within range (48 or 64 blocks, depending on the boss) while the boss is fighting a player, meaning it targets one or a player hurt it. It disappears when the fight ends, the boss dies or you leave range.
- **Music:** bosses with a fight track play it, looping over other music, for everyone who can see the bar. Currently **Tooth and Claw** (see [Music](../music.md)).
- **Phases:** at set health thresholds a boss enters a new phase. Its bar changes color (e.g. yellow → red at half health) and it gets new behaviour. Phases are saved with the boss.
- **Rewards:** bosses drop extra XP and their own loot, and some grant an advancement to the player credited with the kill.

| Boss | Health | Bar | Range | Music | Phase at 50% | XP |
|-|-|-|-|-|-|-|
| [Goblin Warlord](goblin-warlord.md) | 250 | Yellow → red, notched | 48 | none | Enrage | 60 |
| [Magmamuncher Alpha](magmamuncher-alpha.md) | 300 | Yellow → red, notched | 48 | Tooth and Claw | Enrage | 100 |
| [Wyvern](../mobs/dragons.md) (wild) | 40 | Red | 64 | Tooth and Claw | none | normal |
| [Lightning Chaser](../mobs/dragons.md) (wild) | 200 | Yellow | 64 | Tooth and Claw | none | 80 |

## For developers
- See [Boss framework](../dev/boss-framework.md).
