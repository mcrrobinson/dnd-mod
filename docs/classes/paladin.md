# Paladin
A holy tank that protects the players around it and calls down judgment on its foes. You ignore every potion, can't craft or brew, and the Nether drains your strength.

![Divine Judgment at rank IV: three beams strike, each with its shockwave](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/paladin-divine-judgment.png)

## How it works
| Stat | Paladin | Vanilla |
|-|-|-|
| Max health | 26 (13 hearts) | 20 |

Picking the class sets your health to 25.

**Unaffected by potions.** Potion effects from drinking, splash, lingering clouds and tipped arrows never apply to you, good or bad. You can't drink or throw potions either. Effects from abilities, beacons, food and so on still work, so golden apples and a beacon are your buffs.

**Can't craft.** You can't take results out of the crafting grid, crafting table, stonecutter, loom or smithing table ("Paladins cannot craft items!"). Furnaces still work.

**Can't brew.** Brewing stands, including the Fast Brewing Stand, won't open for you ("Paladins cannot brew potions!").

**Weak in the Nether.** In the Nether you deal half damage, have half armor and move 20% slower. "The Nether saps your holy strength..." shows when it starts, and it wears off as soon as you leave.

**Special (power-up key, full mana): Divine Judgment.** A beam of holy light strikes the mob you're looking at, up to 30 blocks away (not through walls). It sets the mob on fire for 5 seconds, and a shockwave around it hits and knocks back the other hostile mobs. The shockwave deals 40% of the beam's damage and sets undead on fire too. Undead take 50% more from both. From rank III, extra beams strike the nearest other hostile mobs within 12 blocks of the first target, each with its own shockwave. Each mob is hit once. The damage is magic, so armor doesn't reduce it. Players and your own pets are never hit. With nothing in sight, nothing happens and you keep your mana.

| Rank | Class level | Damage | Shockwave radius | Beams |
|-|-|-|-|-|
| I | | 8 | 3 blocks | 1 |
| II | 3 | 12 | 4 blocks | 1 |
| III | 6 | 16 | 5 blocks | 2 |
| IV | 9 | 20 | 6 blocks | 3 |

Rank it up with skill points at an Attunement Table (1 point per rank).

### Tips
A Paladin works best with friends: someone else crafts your tools and armor, and your auras keep them standing while Divine Judgment thins the crowd. Playing solo, collect villager trades and loot for gear, and keep a chest of crafted basics from before you picked the class. Avoid the Nether until you've unlocked Hellforged.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 10 at 2700 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) and rank up abilities at an Attunement Table (see [Class progression](../systems/class-progression.md)). The Paladin tree has a ninth node, Circle of Healing, straight above the root.

Paladins get 3 extra XP for killing undead, and 1 extra for a hostile kill while another player is within 16 blocks.

| Skill | Branch | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Divine Judgment | Root | Active | 0 | 9 | The special above |
| Divine Smite | Devotion | Passive | 1 | | +4 melee damage to undead |
| Sacred Weapon | Devotion | Active | 1 | 4 | Strength I for 15 s, and your melee hits set targets on fire for 4 s |
| Aura of Protection | Devotion | Passive | 1 | | You and players within 8 blocks take 15% less damage. Several auras don't stack |
| Hellforged | Conquest | Passive | 1 | | The Nether no longer weakens you |
| Divine Shield | Conquest | Active | 1 | 5 | Absorption III and no knockback for 15 s |
| Aura of Courage | Conquest | Passive | 1 | | Weakness and Slowness are removed from you and players within 8 blocks every second |
| Circle of Healing | Mercy | Active | 2 | 7 | Fully heals every player within 10 blocks. You and party members within 24 blocks are fully healed and get Absorption I for 30 s |
| Avenging Angel | Capstone | Active | 2 | 9 | Strength II, Regeneration II and Resistance II for 20 s; undead within 10 blocks are set on fire every second for the whole time |

The effects from your own skills aren't potions, so they apply to you.

## Commands
- `/dndclass set <player> paladin` (see [Admin commands](../systems/admin-commands.md)).

## For developers
- Nether weakness: `Misc/PaladinNetherWeakness` (skipped with `PaladinSkills.HELLFORGED`). Crafting: `mixin/SlotMixin`, `mixin/SmithingScreenHandlerMixin`. Brewing: `mixin/BrewingStandBlockMixin`. Potions: `PotionImmunity`. Divine Judgment: `PaladinSkills.divineJudgment`, fired from `PowerUpEffect` (`PALADIN`); its ranks are `PaladinSkills.DIVINE_JUDGMENT`. Circle of Healing: `PaladinSkills.circleOfHealing` (party lookup through `PartyManager.nearbyMembers`). Test: `devscripts/paladin-ranks.txt`.
- Skill tree: `Progression/Classes/PaladinSkills.java`. Aura of Protection is applied in `PaladinAuraMixin`, since it also changes damage to other players.
