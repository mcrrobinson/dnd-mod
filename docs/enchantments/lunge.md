# Lunge
Right-click a weapon to dash forward.

![A player in mid-air, launched forward by a Lunge II sword](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/lunge-dash.png)

## How it works
- Right click with a Lunge item to launch forward along your look direction at 1 block/tick per level (I = 1, II = 2), plus a small hop (0.3 up).
- The item then has a 5-second cooldown. Nearby players see a puff of cloud.

## Where to find it / How to get it
- Enchanting table, as an [Artificer](../classes/artificer.md). Target: swords.

## Commands
- `/enchant @s dndclasses:lunge 2`

## For developers
- The second `UseItemCallback` in `DnDClasses` (`LUNGE_ENCHANTMENT`).
