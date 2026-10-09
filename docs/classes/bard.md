# Bard
A fast, fragile charmer. Monsters ignore you and animals fight for you.

![Animal Friends: wolves, a polar bear and an iron golem become companions (hearts) and an iron golem flings a zombie](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/bard-animal-friends.png)

## How it works
| Stat | Bard | Vanilla |
|-|-|-|
| Max health | 15 | 20 |
| Movement speed | 0.12 (20% faster) | 0.1 |

Picking the class sets your health to 15.

Hostile mobs won't start targeting you. You can walk past zombies and creepers at night and they leave you alone. If you hit something it fights back as normal, and with the Identity mod installed, a mob you've attacked (Identity's "hostility") will hunt you again.

Poison heals you instead of hurting: each poison tick gives back 1 health. A splash Potion of Poison is a cheap emergency heal.

**Special (power-up key, full mana): Animal Friends.** Every animal within 10 blocks turns on nearby hostile mobs, and untamed wolves, cats, parrots and other tameable animals in that radius become yours. Rallied animals stay hostile to monsters until they unload. Using the special again on the same animals doesn't stack. Bosses such as the Wyvern and Lightning Chaser can't be charmed.

### Tips
Use the special next to a wolf pack or a farm before a fight, then lead the mob in. Tamed wolves and cats stay yours after the special ends. The [Bard instruments](../items/bard-instruments.md) are built for this class.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 6 at 1000 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) at an Attunement Table.

Bards get 3 XP when one of their pets kills a hostile mob, and 1 extra XP for their own hostile kills while another player is within 16 blocks.

| Skill | Branch | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Animal Friends | Root | Active | 0 | 9 | The special above |
| Inspiring Presence | Valor | Passive | 1 | | Other players within 8 blocks get Speed I |
| Thunderwave | Valor | Active | 1 | 4 | 4 damage to everything within 6 blocks except players and your pets, and a big knockback |
| Battle Hymn | Valor | Passive | 1 | | Other players within 8 blocks get Strength I |
| Silver Tongue | Lore | Passive | 1 | | Permanent Hero of the Village (cheaper villager trades) |
| Song of Rest | Lore | Active | 1 | 3 | Heals you, other players and your pets within 8 blocks by 3 hearts and clears harmful effects |
| Jack of All Trades | Lore | Passive | 1 | | +1 heart of max health and 10% more damage |
| Crescendo | Capstone | Active | 2 | 9 | Players and your pets within 12 blocks get Strength II, Speed II and Resistance I for 20 s; hostile mobs within 12 blocks glow and get Weakness I |

Inspiring Presence and Battle Hymn only buff other players, not you. They're the reason to bring a Bard on a group trip.

## Commands
- `/dndclass set <player> bard` (see [Admin commands](../systems/admin-commands.md)).

## Known limitations
- The class picker blurb used to mention a diamond-armor limit and longer jumps. Neither is in the game.

## For developers
- Attributes: `ClassStats`. Monster targeting: `mixin/ActiveTargetGoalMixin`. Poison: `mixin/StatusEffectMixin`. Special: `PowerUpEffect.bardEffect`.
- Skill tree: `Progression/Classes/BardSkills.java`. Pet kill XP is handed out from its own `AFTER_DEATH` hook, since a pet's kill isn't the player's.
