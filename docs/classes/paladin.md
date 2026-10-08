# Paladin
A holy tank that heals everyone around it. You ignore every potion, can't craft or brew, and the Nether drains your strength.

## How it works
| Stat | Paladin | Vanilla |
|-|-|-|
| Max health | 26 (13 hearts) | 20 |

Picking the class sets your health to 25.

**Unaffected by potions.** Potion effects from drinking, splash, lingering clouds and tipped arrows never apply to you, good or bad. You can't drink or throw potions either. Effects from abilities, beacons, food and so on still work, so golden apples and a beacon are your buffs.

**Can't craft.** You can't take results out of the crafting grid, crafting table, stonecutter, loom or smithing table ("Paladins cannot craft items!"). Furnaces still work.

**Can't brew.** Brewing stands, including the Fast Brewing Stand, won't open for you ("Paladins cannot brew potions!").

**Weak in the Nether.** In the Nether you deal half damage, have half armor and move 20% slower. "The Nether saps your holy strength..." shows when it starts, and it wears off as soon as you leave.

**Special (power-up key, full mana): Lay on Hands.** Heals every player within 10 blocks, you included, to full health. Members of your [party](../systems/party.md) are healed from up to 24 blocks away and also get Absorption I for 30 seconds.

### Tips
A Paladin works best with friends: someone else crafts your tools and armor, and you keep them alive. Playing solo, collect villager trades and loot for gear, and keep a chest of crafted basics from before you picked the class. Avoid the Nether until you've unlocked Hellforged.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 6 at 1000 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) at an Attunement Table.

Paladins get 3 extra XP for killing undead, and 1 extra for a hostile kill while another player is within 16 blocks.

| Skill | Branch | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Lay on Hands | Root | Active | 0 | 9 | The special above |
| Divine Smite | Devotion | Passive | 1 | | +4 melee damage to undead |
| Sacred Weapon | Devotion | Active | 1 | 4 | Strength I for 15 s, and your melee hits set targets on fire for 4 s |
| Aura of Protection | Devotion | Passive | 1 | | You and players within 8 blocks take 15% less damage. Several auras don't stack |
| Hellforged | Conquest | Passive | 1 | | The Nether no longer weakens you |
| Divine Shield | Conquest | Active | 1 | 5 | Absorption III and no knockback for 15 s |
| Aura of Courage | Conquest | Passive | 1 | | Weakness and Slowness are removed from you and players within 8 blocks every second |
| Avenging Angel | Capstone | Active | 2 | 9 | Strength II, Regeneration II and Resistance II for 20 s; undead within 10 blocks are set on fire every second for the whole time |

The effects from your own skills aren't potions, so they apply to you.

## Commands
- `/dndclass set <player> paladin` (see [Admin commands](../systems/admin-commands.md)).

## For developers
- Nether weakness: `Misc/PaladinNetherWeakness` (skipped with `PaladinSkills.HELLFORGED`). Crafting: `mixin/SlotMixin`, `mixin/SmithingScreenHandlerMixin`. Brewing: `mixin/BrewingStandBlockMixin`. Potions: `PotionImmunity`. Lay on Hands: `PowerUpEffect` (`PALADIN`, `PALADIN_PARTY_HEAL_RADIUS`).
- Skill tree: `Progression/Classes/PaladinSkills.java`. Aura of Protection is applied in `PaladinAuraMixin`, since it also changes damage to other players.
