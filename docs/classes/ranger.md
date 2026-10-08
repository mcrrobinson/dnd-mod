# Ranger
An archer whose bow draws in a flash and, during its special, never runs out of arrows.

## How it works
- **Fast draw:** a bow reaches full power in about 3 ticks instead of 20.
- **Bow zoom:** drawing a bow zooms the view in 2x (first person only, like a spyglass).
- **No swords:** Rangers can't pick swords up off the ground; they stay where they are.
- **Luck:** +5 luck (affects loot tables that use luck, such as fishing).
- **Lava is lethal:** standing in lava deals 20 damage every tick.
- **Special (power-up key, full mana): Arrow Storm** for 15 seconds. Hold right click with a bow and it fires automatically each time it's fully drawn, without needing or using arrows.

## Known limitations
- The sword ban only covers picking swords up off the ground. A Ranger can still take one out of a chest, craft one or keep one they had before switching class.

## For developers
- Draw speed: `mixin/BowItemMixin` (server) and the bow `pull` predicate in `DndClassesClient`. Zoom: `mixin/RangerBowZoomMixin` (client). Sword pickup: `mixin/RangerSwordPickupMixin`. Devscript: `devscripts/ranger-sword-pickup.txt`. Auto-fire: `mixin/PlayerEntityMixin.tick`. Lava: `mixin/LavaDamageMixin`. Effect: `ModEffects.ARROW_STORM`.
