# Warlock
A fire caster that throws fireballs from an empty hand, can't be burned, and hates water.

## How it works
- **Damage:** base attack damage 0.5.
- **Fireballs:** right click with an empty main hand to throw a slow small fireball (like a blaze's), once a second.
- **Fireproof:** immune to all fire damage: fire, lava, burning and fireballs, your own included.
- **Water and rain hurt:** 1 damage every 4 seconds while touching water or rain, but it never takes you below 1 heart. It doesn't apply in creative or spectator.
- **Special (power-up key, full mana): fire breath** for 20 seconds. A 5-block beam in front of you sets targets alight for 2 seconds and deals 2 magic damage to anything in it.

## For developers
- Fireballs, fire immunity and wet damage: `Warlock.java` with `mixin/WarlockFireballMixin` (client). Fire breath: the world tick in `DnDClasses` (`WARLOCK_FIREBREATH`).
- Devscript: `devscripts/warlock-passive.txt`.
