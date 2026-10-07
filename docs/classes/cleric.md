# Cleric
A miner and support class: fast digging, permanent night vision and a circle that mobs won't enter.

## How it works
- **Haste III and Night Vision** all the time. They refresh every tick and show no particles.
- **Damage:** base attack damage 0.67 (vanilla 1, so a third less).
- **Shorter view:** fog closes in from 16 to 48 blocks.
- **Special (power-up key, full mana): circle of ignoring mobs.** For 15 seconds no mob can target you. A ring of particles with a 16-block radius surrounds you, and mobs within 32 blocks that were already chasing you give up (checked every half second).

## Known limitations
- The Cleric's in-game blurb says the special heals players in the area. It doesn't: it's the mob repel described above.

## For developers
- Passives and repel: `Misc/ClericHandler`. Target blocking: `mixin/MobEntityMixin`. Effect: `ModEffects.MOB_REPEL`. Fog: `mixin/BackgroundRendererMixin`.
