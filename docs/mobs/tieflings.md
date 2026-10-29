# Tieflings
The folk of the [Brimstone Sanctums](../structures/brimstone-sanctums.md): Tiefling Exiles who keep the sanctum and the Pactbroker who trades in its Pact Hall. They're neutral to players until sanctum law is broken, and fire can't hurt them.

![A Tiefling Exile side on: ram horns and a spade-tipped tail](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/brimstone-sanctums/bsc-exile-side.png)

## How it works
| Tiefling | Health | Damage | Speed | Carries | Behaviour |
|-|-|-|-|-|-|
| Exile | 26 (2 armour) | 5 + weapon | 0.28 | a golden sword (70%) or an iron axe | The guard: hunts monsters within 24 blocks of home, and throws fireballs. Keeps within 15 blocks of the sanctum's middle. |
| Pactbroker | 30 | 4 | 0.23 | a book and quill | Merchant and quest giver; keeps within 4 blocks of his spot by the Pact Hall's altar |

Every Tiefling has an infernal name such as "Akmenos" or a virtue name such as "Penance", never despawns, is **fire immune** (fire, lava, fireballs) and is a member of the **Brimstone Covenant** [faction](../systems/factions.md). Blows and fireballs from other Tieflings don't hurt them.

- **Hellish Rebuke**: anything that strikes a Tiefling in melee (a mob or a player, not arrows or spells) bursts into flame for 4 s, with flames and a fire-charge sound. Fire-immune attackers shrug it off.
- **Defence**: a player who hits any Tiefling brings every Exile within 16 blocks down on them.

### Exiles
- **Melee**: sword or axe, at 1.1x speed.
- **Fireball**: when what it's fighting is 6-16 blocks away and in sight, an Exile throws a blaze's small fireball at it, then every 4 s. It sets the target and the ground alight (fire on blackstone burns out; crimson planks don't burn).
- **Hunting**: monsters within 24 blocks of home, but never creepers, endermen, piglins or zombified piglins, so the Nether's own folk aren't stirred up.
- Exiles leave players alone unless the player is Hostile with the Covenant (attacked on sight), hits a Tiefling or breaks sanctum law where a Tiefling can see (see [Brimstone Sanctums](../structures/brimstone-sanctums.md#sanctum-law)). They stay angry for 30-50 s.

### Trades
Each trade has 12 uses (the netherite scrap 1) and restocks every morning. Prices follow your standing with the Brimstone Covenant (Unfriendly +50%, Friendly -10%, Honored -25%, Exalted -40%); the Pactbroker won't trade with Hostile players. Tieflings pay 25% less (kin prices).

| Sells | Price |
|-|-|
| 2 blaze rods | 1 emerald |
| a ghast tear | 3 |
| 4 magma cream | 2 |
| a Potion of Fire Resistance (3:00) | 4 |
| a netherite scrap (once a day) | 24 |
| 16 [Brimstone](../structures/brimstone-sanctums.md#building-blocks) | 1 |
| 8 Brimstone Bricks | 1 |
| **Tieflings only:** [Infernal Regalia](../items/armor.md#racial-armor) helmet, chestplate, leggings, boots | 12, 20, 18, 10 |
| **Warlocks only:** one piece of the Wither set (which piece is rolled per Pactbroker) | 20 |
| **Alchemists only:** a Fast Brewing Stand | 12 |

| Buys | For |
|-|-|
| 20 nether quartz | 1 emerald |
| 22 nether wart | 1 |
| 3 gold ingots | 1 |
| 2 crying obsidian | 1 |

**Bargains**, once per player ever (bought once, never offered to that player again, from any Pactbroker; kept in the player's data as `dndPactBargains`):

| Sells | Price | Standing |
|-|-|-|
| a [Portable Hole](../items/magic-items.md#portable-hole), identified | 32 emeralds | Honored or Exalted |
| the [Deck of Many Things](../items/magic-items.md#deck-of-many-things), identified | 48 emeralds and a netherite ingot | Exalted |

Buying the Deck seals it with a line in chat: a devil's bargain.

The gated offers come after the others, and only the players they're for see them: a Tiefling Warlock sees 16 trades, a Human Alchemist 12. The bargains come last. The brimstone offers come after the buys, and a Pactbroker from an older world gets them when he's next loaded.

### Dialogue
The Pactbroker has the quest-giver role `tiefling_pactbroker` (`dialogue/tiefling_pactbroker.json`, with a greeting for each standing tier). Quests whose `giver` is `tiefling_pactbroker` are offered by him; with nothing to say, right-clicking him opens his trades. See [NPC dialogue](../systems/dialogue.md).

### Looks
The player model with classic arms, with the ram horns Tiefling players get, and a tail: three 1x1x2 segments (6 long) from the small of the back, drooping then curling up to a spade. Five Exile skins (crimson, plum, ash, slate and ember skin under dark leathers and a hooded mantle, with solid glowing eyes) and the Pactbroker's plum skin under black and gold robes with a red sash and a gold circlet, from `tools/tiefling_skins.py`.

## Where to find it
Only in Brimstone Sanctums, where Exiles also spawn naturally on the platform (up to 8 Tieflings within 48 blocks). Spawn eggs: Tiefling Exile, Pactbroker.

## Commands
- `/summon dndclasses:tiefling_exile` and `/summon dndclasses:tiefling_pactbroker`

## Known limitations
- Players don't have a tail yet; only the NPCs do.
- Exiles don't use Hellish Rebuke's 20 s cooldown: every melee hit on them burns the attacker.

## For developers
- `entity/TieflingEntity` (base, a `SettlementNpcEntity`: variant, names, home and one range day and night, `setTarget` filter, Hellish Rebuke in `afterHurt`, Exiles called in `callGuards`), `TieflingExileEntity` (`GuardAnger`, `FireballGoal`, `throwFireball`, `hunts`), `TieflingPactbrokerEntity` (`NpcMerchant`, `ROLE_TAG`, three gated stocks), `TieflingTrades`, `SanctumLaw`.
- `KinStock` now takes an audience (`KinStock(factory, audience, nbtKey)`, `shownTo`), so one merchant can keep several gated stocks: the Pactbroker's kin stock (`KinStockOffers.brimstoneSanctum`, NBT `KinOffers`), Warlock stock (`WarlockOffers`) and Alchemist stock (`AlchemistOffers`).
- Logs `[BrimstoneSanctum] <name> rebuked <attacker> (fire 4 s)` and `[BrimstoneSanctum] Exile <name> threw a fireball at <target> (<d> blocks)`.
- Renderer: `RacialHumanoidRenderer.tiefling` / `pactbroker` (`Look TIEFLING`), with a `TailFeature` drawing `RaceFeatures.renderTail` on the posed body.
- `ModEntityTypes.TIEFLING_EXILE` / `TIEFLING_PACTBROKER` (both `fireImmune`), `ModSpawns.canExileSpawn`.
