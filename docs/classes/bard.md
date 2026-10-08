# Bard
A fast, fragile charmer. Monsters ignore you, and the animals you've learned fight for you.

## How it works
| Stat | Bard | Vanilla |
|-|-|-|
| Max health | 15 | 20 |
| Movement speed | 0.12 (20% faster) | 0.1 |

Picking the class sets your health to 15.

Hostile mobs won't start targeting you. You can walk past zombies and creepers at night and they leave you alone. If you hit something it fights back as normal, and with the Identity mod installed, a mob you've attacked (Identity's "hostility") will hunt you again.

Poison heals you instead of hurting: each poison tick gives back 1 health. A splash Potion of Poison is a cheap emergency heal.

**Special (power-up key, full mana): Animal Friends.** Animals you've unlocked in your bestiary that are within range become your companions, up to your companion cap, nearest first. All your companions in range then go after the nearest hostile mob. Other players' pets, bosses and animals you haven't unlocked are left alone.

Companions follow you (and teleport to you when more than 24 blocks behind), fight hostile mobs (not creepers), go after whatever hurts you or whatever you hit, and never target or hurt you, your party or your other companions. They don't despawn and stay yours after reloads. Wolves, cats and parrots are tamed the vanilla way, so they can also sit. Foxes trust you, iron golems count as player-built, and hoglins don't turn into zoglins in the Overworld. Companions' hostile kills give you the 3 pet-kill XP.

| Rank | Class level | Radius | Companions | Animals you can unlock |
|-|-|-|-|-|
| I | 0 | 10 blocks | 3 | Tier I |
| II | 3 | 12 blocks | 4 | Tier I-II |
| III | 6 | 14 blocks | 5 | Tier I-III |
| IV | 9 | 16 blocks | 6 | Tier I-IV |

Rank up Animal Friends at an Attunement Table for a skill point per rank. The cap counts every companion you have loaded in your current world.

#### Bestiary
1. **Learn.** Killing a tiered animal for the first time as a Bard learns it ("Learned Fox. Unlock it at an Attunement Table."). Killing your own companion or a tamed pet doesn't count.
2. **Unlock.** Open the skill tree at an Attunement Table, switch to the **Bestiary** tab and click a learned animal. Unlocking is free, but the animal's tier needs Animal Friends at that rank. Only unlocked animals answer Animal Friends.
3. **Starter.** Every Bard has wolves unlocked from the start.

| Tier | Animals |
|-|-|
| I | Wolf, cat, fox, parrot |
| II | Goat, llama, trader llama, bee |
| III | Polar bear, panda |
| IV | Iron golem, hoglin |

Other animals (cows, pigs and so on) aren't learned.

### Tips
Hunt a fox or a llama early to fill your bestiary, then rank Animal Friends up as you level for the big animals. Use the special next to a wolf pack before a fight, then lead the mob in. Companions stay yours after the special ends, so the cap is the real limit. The [Bard instruments](../items/bard-instruments.md) are built for this class.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 10 at 2700 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) and rank up abilities at an Attunement Table (see [Class progression](../systems/class-progression.md)).

Bards get 3 XP when one of their pets kills a hostile mob, and 1 extra XP for their own hostile kills while another player is within 16 blocks.

| Skill | Branch | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Animal Friends | Root | Active | 0 | 9 | The special above; 4 ranks |
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
- `/dndclass rank <player> bard.animal_friends <1-4>` sets the rank; `/dndclass bestiary <player> learn|unlock <entity>` learns or unlocks an animal.

## Known limitations
- The class picker blurb used to mention a diamond-armor limit and longer jumps. Neither is in the game.
- Goats still pick their own ram targets. A ram can't hurt you or your friends, but it can still knock you back.
- Hoglins and goats are steered by their own brains as well as the companion goals, so they follow less smoothly than the others.
- Parrots have no attack; they follow and keep you company.
- The cap only sees companions in loaded chunks, so ones left far away don't count until they load again.

## For developers
- Attributes: `ClassStats`. Monster targeting: `mixin/ActiveTargetGoalMixin`. Poison: `mixin/StatusEffectMixin`. Special: `BardSkills.animalFriends` (called from `PowerUpEffect`); ranks are `BardSkills.ANIMAL_FRIENDS`, tiers `BardSkills.TIERS`.
- Skill tree: `Progression/Classes/BardSkills.java`. Pet kill XP is handed out from its own `AFTER_DEATH` hook, since a pet's kill isn't the player's.
- Companions: `Misc/BardCompanions.java`. The owner is a command tag `dnd_bard_companion:<uuid>`; the goals (follow, melee for mobs without one, defend owner, rally on hostiles, a guard that clears brain targets) are added on adoption and again on `ENTITY_LOAD`. `mixin/MobEntityMixin` stops companions targeting friends, and an `ALLOW_DAMAGE` hook stops them hurting friends. `mixin/FoxEntityInvoker` makes foxes trust the Bard.
- Test: `devscripts/bard-bestiary.txt`.
