# Ranger
An archer whose bow draws in a flash and, during its special, never runs out of arrows.

## How it works
- **Fast draw:** a bow reaches full power in about 3 ticks instead of 20.
- **Luck:** +5 luck (affects loot tables that use luck, such as fishing).
- **Lava is lethal:** standing in lava deals 20 damage every tick.
- **Special (power-up key, full mana): Arrow Storm** for 15 seconds. Hold right click with a bow and it fires automatically each time it's fully drawn, without needing or using arrows.

## Known limitations
- The README promises zooming in with a bow and not being able to pick up swords. Neither is in the code.

## For developers
- Draw speed: `mixin/BowItemMixin` (server) and the bow `pull` predicate in `DndClassesClient`. Auto-fire: `mixin/PlayerEntityMixin.tick`. Lava: `mixin/LavaDamageMixin`. Effect: `ModEffects.ARROW_STORM`.
