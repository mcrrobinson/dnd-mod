# Mana and class specials
Every class has an active ability that you fire with the power-up key. It costs mana, which refills on its own over time.

![The mana bar, full, above the food bar](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/hud-bard.png)

## How it works
Your mana bar is 9 pips drawn above the hotbar, in the row above the food bar. It moves up out of the way when your air bubbles show underwater, or when you ride a mount with more than 20 health. Like the health and food bars, it's hidden in creative and spectator.

Mana comes back at 1 pip every 2 seconds, so an empty bar fills in 18 seconds.

Press the **power-up key (Z by default)** to fire your equipped active. Each active has a mana cost, shown as a line under the pips it uses. The line is grey while you can't afford it and turns gold when you can. Your class's original special, the root of its [skill tree](class-selection.md#class-levels-and-the-skill-tree), costs all 9 pips. Most actives you unlock further up the tree cost less. Firing spends only that skill's cost, plays a short music sting for your class and dips the background music under it (see [Music](../music.md)).

If the special can't do anything, you keep your mana. That happens with no class picked, as a Druid with no animal forms, as a Blood Hunter with nothing in your sights, as an Alchemist with no potions to upgrade, and inside a [Beholder's](../bosses/beholder.md) anti-magic cone.

Mana is kept through death and leaving the End, so dying doesn't refill it.

### The original specials
These are the roots of each tree, all 9 mana:

| Class | Special | What it does |
|-|-|-|
| Barbarian | Rage | Strength III for 15 s |
| Bard | Animal Friends | Animals within 10 blocks attack monsters; untamed tameable animals become yours |
| Cleric | Sanctuary | Mobs can't target you for 15 s. Party members within 16 blocks share it and get Regeneration I for 10 s |
| Druid | Wild Shape | Become a random animal you've killed for 30 s (needs the Identity mod) |
| Fighter | Super Regeneration | Regeneration V for 10 s |
| Monk | Ki Surge | Speed II, Haste II and Jump Boost II for 15 s |
| Paladin | Lay on Hands | Heal every player within 10 blocks to full. Party members within 24 blocks are also healed and get Absorption I for 30 s |
| Ranger | Arrow Storm | For 15 s your bow needs no arrows and fires itself at full draw |
| Rogue | Vanish | Invisibility for 15 s |
| Necromancer | Raise Dead | An allied zombie and skeleton rise and fight for you for 10 s |
| Warlock | Fire Breath | Breathe fire for 8-20 s, 3-7 blocks (by rank) |
| Wizard | Arcane Explosion | A power 40 explosion centred on you that breaks no blocks. You get Resistance V for 5 s so it doesn't hurt you |
| Artificer | Arcane Armor | +8 armor and +4 armor toughness for 30 s |
| Blood Hunter | Blood Control | Take control of the mob you're looking at, up to 30 blocks away, for 20 s (needs the Identity mod) |
| Alchemist | Distill | Upgrade every potion in your inventory to its strongest version |

Press O to see the rest of your tree.

### Tips
- Rebind the key under Options > Controls > Key Binds > D&D Classes. It is listed as "Power Up Abililty."
- The Wizard explosion hits everything around you, tamed pets and players outside your party included.
- A cheap active from your tree lets you fire more often than the 9-pip root. Swap actives at an Attunement Table.

## Known limitations
- Mana lives on the server and is synced to your client, so the bar can look out of date for a moment after you join.

## For developers
- Constants: `DnDClasses.MANA_ICONS` (9) and `MANA_FULL_SECONDS` (18). Regeneration runs in the `END_WORLD_TICK` handler in `DnDClasses`.
- Storage: `ManaManager` (player persistent NBT `manorMana`). HUD: `Client/Hud/PowerupOverlay`.
- `DnDClasses.sendPowerupPacket` reads the equipped active (`ClassProgress.activeNode`), checks its cost and the anti-magic cone, then calls `PowerUpEffect.play` for a root or `Abilities.activate` for anything else. Either returns false to keep the mana.
- Devscripts: `mana-hud.txt`, `powerup-no-class.txt`.
