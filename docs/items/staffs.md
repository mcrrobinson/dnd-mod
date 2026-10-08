# Staffs
Three elemental staffs that blast whatever you point them at, plus the Monk's staff.

## How it works
**Elemental staffs** (Staff of Fire, Ice, Lightning) can only be used by [Wizards](../classes/wizard.md). They hit like a diamond sword with +10 damage (14 attack damage, 5.2 attack speed). Right click fires a beam up to 40 blocks at the block you're looking at:
- **Fire:** a power-3 explosion that sets fires. The blocks round the impact (3-block radius) are restored 5 seconds later.
- **Ice:** a small blast (no block damage). Every block in a 3-block radius turns to ice for 5 seconds, and entities nearby are frozen in place for 10 seconds.
- **Lightning:** a small blast, then lightning strikes every third solid block round the impact.

**Monk Staff:** a diamond sword with +9 damage (13 attack damage, 1.8 attack speed). It's the only weapon a [Monk](../classes/monk.md) can attack with.

## Where to find it / How to get it
- The mod's creative tab.
- The Staff of Lightning: a 15% (+5%/Looting) drop from a [Lightning Chaser](../mobs/dragons.md) killed by a player, and sometimes in its [lair](../structures/dragon-lairs.md) hoard.

## Known limitations
- Staffs have no cooldown and use no durability.
- Restored blocks lose their contents (chests etc.).

## For developers
- `Items/ExtendedSwordItem` (staff behaviour by item id), `Items/ScheduledBlockRestore`, `Items/MonkStaff`, `Effects/FreezeEffect`.
