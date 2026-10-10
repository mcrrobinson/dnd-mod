# Mana and class specials
Every class has an active ability that you fire with the power-up key. It costs mana, which refills on its own over time.

![The mana bar, full, above the food bar](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/hud-bard.png)

## How it works
Your mana bar is 9 pips drawn above the hotbar, in the row above the food bar. It moves up out of the way when your air bubbles show underwater, or when you ride a mount with more than 20 health. Like the health and food bars, it's hidden in creative and spectator.

Mana comes back at 1 pip every 2 seconds, so an empty bar fills in 18 seconds.

Press the **power-up key (Z by default)** to fire your equipped active. Each active has a mana cost, shown as a line under the pips it uses. The line is grey while you can't afford it and turns gold when you can. Your class's original special, the root of its [skill tree](class-selection.md#class-levels-and-the-skill-tree), costs all 9 pips. Most actives you unlock further up the tree cost less. Major actives (the root special, the capstone, and anything costing 7+ mana) also cost **charges**, the gems above the mana bar, which only come back with rests; see [Rests and charges](rests.md). The gold line needs both the mana and the charges. Firing spends only that skill's cost, plays a short music sting for your class and dips the background music under it (see [Music](../music.md)).

If the special can't do anything, you keep your mana (and charges). That happens with no class picked, as a Druid with no animal forms, as a Blood Hunter with nothing in your sights, and inside a [Beholder's](../bosses/beholder.md) anti-magic cone.

Mana is kept through death and leaving the End, so dying doesn't refill it. A short or long rest refills it.

### The original specials
These are the roots of each tree, all 9 mana and 1 charge:

| Class | Special | What it does |
|-|-|-|
| Barbarian | Rage | Strength I for 8 s, up to Strength III for 12 s with ranks |
| Bard | Animal Friends | Unlocked animals within 10-16 blocks become companions that follow you and fight monsters, 3-6 at once (by rank) |
| Cleric | Sanctuary | Mobs can't target you for 6-15 s. From rank II, party members nearby share it and get Regeneration (by rank) |
| Druid | Wild Shape | Become an animal you've unlocked for 15-30 s (by rank; needs the Identity mod) |
| Fighter | Super Regeneration | Regeneration V for 4-10 s (by rank) |
| Monk | Flurry Rush | Blink-strike chain on the mob in the crosshair and hostiles near it: 3 hits on 1 target, up to 10 hits across 5 targets with ranks |
| Paladin | Divine Judgment | A beam of holy light hits the mob you look at, up to 30 blocks away, and a shockwave hits hostiles around it; undead take more. Stronger with rank. Keeps the mana with nothing in sight |
| Ranger | Arrow Storm | For 8-15 s your bow needs no arrows and fires itself, faster with rank |
| Rogue | Vanish | Invisibility for 6-15 s (by rank) |
| Necromancer | Raise Dead | 2-5 allied undead rise and fight for you for 10-20 s; a Bone Wyvern joins at rank V |
| Warlock | Fire Breath | Breathe fire for 8-20 s, 3-7 blocks (by rank) |
| Wizard | Arcane Explosion | A blast centred on you that breaks no blocks, reaching 12-72 blocks by rank. You get Resistance V for 2-5 s so it doesn't hurt you |
| Artificer | Arcane Armor | +8 armor and +4 armor toughness for 30 s |
| Blood Hunter | Blood Control | Take control of the mob you're looking at for 8-20 s; range, duration and success chance grow with rank (needs the Identity mod) |
| Alchemist | Transmute | Throw your held potion (or an unstable brew) as a cloud with stronger effects: buffs for allies, harm for mobs |

Press O to see the rest of your tree.

### Tips
- Rebind the key under Options > Controls > Key Binds > D&D Classes. It is listed as "Power Up Abililty."
- The Wizard explosion hits everything around you, tamed pets and players outside your party included.
- A cheap active from your tree lets you fire more often than the 9-pip root, and costs no charges. Swap actives at an Attunement Table.

## Known limitations
- Mana lives on the server and is synced to your client, so the bar can look out of date for a moment after you join.

## For developers
- Constants: `DnDClasses.MANA_ICONS` (9) and `MANA_FULL_SECONDS` (18). Regeneration runs in the `END_WORLD_TICK` handler in `DnDClasses`.
- Storage: `ManaManager` (player persistent NBT `manorMana`). HUD: `Client/Hud/PowerupOverlay`.
- `DnDClasses.sendPowerupPacket` reads the equipped active (`ClassProgress.activeNode`), checks its mana cost, its charge cost (`Rest.Charges.canAfford`) and the anti-magic cone, then calls `PowerUpEffect.play` for a root or `Abilities.activate` for anything else. Either returns false to keep the mana and charges; on success `Charges.spend` pays the charges.
- Devscripts: `mana-hud.txt`, `powerup-no-class.txt`, `rests-charges.txt`.
