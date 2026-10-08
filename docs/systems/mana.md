# Mana and class specials
Every class has a special ability (its "power-up"), fired with a key once your mana bar is full.

## How it works
- **Mana bar:** 9 pips drawn above the hotbar, beside the health and food bars (hidden in creative and spectator like they are).
  - It sits in the row above the food bar and moves up out of the way of the air bubbles (underwater) and of a second row of mount hearts (riding a horse with more than 20 health).
  - The equipped active's cost is underlined in the 1px gap below the pips, gold once you can afford it.
- **Regeneration:** +1 pip every 2 seconds, so an empty bar fills in 18 seconds.
- **Firing:** press the **power-up key (Z by default**, "key.dnd-classes.power-up" in Controls). It only works with a full bar, and it empties the bar.
- Mana is kept through death and leaving the End (dying doesn't refill it), and the bar is re-synced on respawn.
- If the special can't do anything, your mana is kept: no class picked, a Druid with no animal forms, or a Blood Hunter with no target.
- A short **music sting** for your class plays and the background music dips under it (see [Music](../music.md)).

| Class | Special |
|-|-|
| Barbarian | Strength III, 15 s |
| Bard | Animals within 10 blocks attack monsters; untamed pets become yours |
| Cleric | Mobs can't target you, 15 s |
| Druid | Become a random animal you've killed, 30 s (Identity mod) |
| Fighter | Regeneration V, 10 s |
| Monk | None yet |
| Paladin | Heal all players within 10 blocks to full |
| Ranger | Arrow Storm: auto-firing bow with no ammo, 15 s |
| Rogue | Invisibility, 15 s |
| Necromancer | Summon an allied zombie and skeleton |
| Warlock | Fire breath, 20 s |
| Wizard | Power-40 explosion that spares you and blocks |
| Artificer | +8 armor, +4 toughness, 30 s |
| Blood Hunter | Possess a mob within 30 blocks, 20 s (Identity mod) |
| Alchemist | Upgrade inventory potions to their strongest version |

## Known limitations
- Mana is server-side and synced to the client. The bar can look out of date for a moment after joining.

## For developers
- Constants: `DnDClasses.MANA_ICONS` and `MANA_FULL_SECONDS`. Storage: `ManaManager` (player persistent NBT `manorMana`). HUD: `Client/Hud/PowerupOverlay`.
- Specials: `Misc/PowerUpEffect.play` returns false to keep the mana.
- Devscripts: `mana-hud.txt`, `powerup-no-class.txt`.
