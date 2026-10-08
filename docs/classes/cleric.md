# Cleric
A miner and support class: fast digging, permanent night vision and a circle that mobs won't enter.

## How it works
- **Haste III and Night Vision** all the time. They refresh every tick and show no particles.
- **Damage:** base attack damage 0.67 (vanilla 1, so a third less).
- **Shorter view:** fog closes in from 16 to 48 blocks.
- **Special (power-up key, full mana): circle of ignoring mobs.** For 15 seconds no mob can target you. A ring of particles with a 16-block radius surrounds you, and mobs within 32 blocks that were already chasing you give up (checked every half second).
- **Party:** party members within 16 blocks share the circle and get Regeneration I for 10 seconds (see [Party](../systems/party.md)).

## For developers
- Passives and repel: `Misc/ClericHandler`. Target blocking: `mixin/MobEntityMixin`. Effect: `ModEffects.MOB_REPEL`. Fog: `mixin/BackgroundRendererMixin`.
