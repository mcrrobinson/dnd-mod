# Warlock
A fire caster that throws fireballs from an empty hand, can't be burned, and hates water.

## How it works
- **Damage:** base attack damage 0.5.
- **Fireballs:** right click with an empty main hand at the air or a hostile mob to throw a slow small fireball (like a blaze's), once a second (the server enforces the cooldown). If your off hand holds something usable (shield, food, potion...), right click uses that instead.
- **Fireproof:** immune to all fire damage: fire, lava, burning and fireballs, your own included.
- **Water and rain hurt:** 1 damage every 4 seconds while touching water or rain, but it never takes you below 1 heart. It doesn't apply in creative or spectator.
- **Special (power-up key, full mana): fire breath** for 20 seconds. A 5-block beam in front of you sets targets alight for 2 seconds and deals 2 magic damage to anything in it, credited to you (kills count as yours). Your party members and your own pets are left alone. Only your current dimension is affected.

## For developers
- Fireballs, fire immunity and wet damage: `Warlock.java` with `mixin/WarlockFireballMixin` (client). Fire breath: `Warlock.tickFireBreath` (end times in `DnDClasses.WARLOCK_FIREBREATH`, in world time); the client draws your own flames until that world time.
- Devscript: `devscripts/warlock-passive.txt`.
