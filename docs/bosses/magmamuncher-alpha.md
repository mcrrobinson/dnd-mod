# Magmamuncher Alpha
A huge, hostile [Magmamuncher](../mobs/magmamunchers.md) that roams the Nether. Its bites set you on fire.

## How it works
- 300 HP, 10 armor, 2 toughness, full knockback resistance, 14 attack damage, 0.25 speed. Fire immune. Hunts players.
- **Bites** set the target alight for 4 seconds.
- **Enrage (below 50% health):** the bar turns red and it roars, gaining +30% speed and +6 attack damage. It calls two regular Magmamunchers out of the ground, and its bites now burn for 8 seconds.
- While enraged, every 4 seconds it spits a volley of 3 small fireballs at a target it can see 4-20 blocks away.
- **Boss bar:** yellow, notched, for players within 48 blocks, with **Tooth and Claw** playing.

## Where to find it / How to get it
- Rarely in Basalt Deltas and Nether Wastes (weight 1, plus a 1 in 4 roll), never within 96 blocks of another Alpha. There's also a spawn egg.

## Rewards
- 4-8 magma cream (+0-2/Looting) and 2-5 blaze rods (+0-1/Looting).
- Killed by a player: 100 XP, 5-10 gold ingots, 60% (+10%/Looting) 1-2 netherite scrap, and 25% (+5%/Looting) an enchanted book (Fire Protection IV or Fire Aspect II).

## Commands
- `/summon dndclasses:magmamuncher_alpha`

## For developers
- `entity/MagmamuncherAlphaEntity`, loot `loot_tables/entities/magmamuncher_alpha.json`. Devscript: `magmamuncher-alpha.txt`.
