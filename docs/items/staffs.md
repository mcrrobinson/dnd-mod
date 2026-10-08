# Staffs
Three elemental staffs that blast whatever you point them at, plus the Monk's staff.

## How it works
**Elemental staffs** (Staff of Fire, Ice, Lightning) can only be used by [Wizards](../classes/wizard.md). They hit like a diamond sword with +10 damage (14 attack damage, 5.2 attack speed). Right click fires a beam up to 40 blocks at the block you're looking at:
- **Fire:** a power-3 blast that sets fires and carves a crater (3-block radius sphere). The crater drops nothing and fills back in 5 seconds later (from 4 s, over 1 s).
- **Ice:** a small blast (no block damage). Every block in a 7x7x7 cube round the impact turns to ice for 5 seconds, and entities nearby are frozen in place for 10 seconds.
- **Lightning:** a small blast, then up to 5 lightning bolts on exposed blocks round the impact.

Each cast puts the staff on a 1 second cooldown. The staffs leave alone bedrock, obsidian-hard blocks and blocks with contents (chests, furnaces, spawners). Changed blocks are put back without block updates, so nothing next to them pops off or falls. A block is only put back if its spot is still the ice or empty (air, fire, water); anything placed there meanwhile stays. Pending restores are saved with the world, so they finish after a restart.

**Monk Staff:** a diamond sword with +9 damage (13 attack damage, 1.8 attack speed). It's the only weapon a [Monk](../classes/monk.md) can attack with.

## Where to find it / How to get it
- The mod's creative tab.
- The Staff of Lightning: a 15% (+5%/Looting) drop from a [Lightning Chaser](../mobs/dragons.md) killed by a player, and sometimes in its [lair](../structures/dragon-lairs.md) hoard.

## Known limitations
- Staffs use no durability.
- Mining the Ice staff's ice with Silk Touch gives ice items.

## For developers
- `Items/ExtendedSwordItem` (staff behaviour by item id), `Items/ScheduledBlockRestore` (per-world `PersistentState`, `data/dndclasses_staff_restores.dat`), `Items/MonkStaff`, `Effects/FreezeEffect`. Devscripts: `devscripts/staff-fire-dupe.txt`, `staff-restore-persist-a.txt` then `-b.txt`.
