# Druid
A nature class that grows stronger with tamed animals and sunlight, and can take the shape of animals it has hunted.

## How it works
- **Animal hearts:** +1 heart (2 health) of max health for each living tamed animal you own in your current world, up to 5 hearts. It's recounted every second.
- **Light:** in light level 10 or higher you regenerate 0.5 health a second, even on an empty stomach. In light level 4 or lower you get hungry (0.1 exhaustion a second, about half a drumstick every 40 seconds).
- **Can't swim:** fully submerged in water, you sink (pulled down at 0.5 blocks a tick) unless you're in creative, spectator or flying. You can still walk along the bottom.
- **Special (power-up key, full mana):** turn into a random animal from the last 16 kinds of animal you've killed (an [Owlbear](../mobs/owlbear.md) counts once you've killed one), for 30 seconds. If you haven't killed an animal yet, nothing happens and you keep your mana.

## Known limitations
- Animal forms need the optional **Identity** mod. Without it the special does nothing and your mana is kept.
- Only animals loaded in your world count towards the extra hearts.

## For developers
- All mechanics: `Druid.java` (hearts, `lightTick`, `transform`). Swimming: `mixin/PlayerEntityMixin.tick`. The killed-animal list is saved in the player's persistent NBT (`druidKilledAnimals`, with the active form's end time in `druidFormExpiry`); `ClassLifecycle` copies both to the new player on death and End exit.
