# Rogue
A fragile scout that never needs to eat, shrugs off poison and can vanish.

## How it works
- **Health:** 14 max.
- **No poison:** poison can't be applied to you.
- **No hunger:** your food bar never drains and you never starve, but you don't regenerate naturally from food either.
- **Special (power-up key, full mana):** Invisibility for 15 seconds.

## Known limitations
- The in-game blurb says Nether mobs are allies and Overworld mobs always attack. That isn't implemented.

## For developers
- Hunger: `mixin/HungerManagerMixin`. Poison: `mixin/LivingEntityMixin.onAddStatusEffect`.
- Devscript: `devscripts/rogue-no-hunger.txt`.
