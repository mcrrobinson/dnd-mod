# Druid
A nature class that gets tougher with every tamed animal, heals in sunlight and can take the shape of animals it has hunted.

## How it works
**Animal hearts.** You get 1 extra heart (2 health) of max health for each living tamed animal you own in your current world, up to 5 hearts. The count is redone every second, so a pet dying costs you a heart straight away. Only animals that are loaded count, so pets left at a far-away base don't help.

**Light.** In light level 10 or higher you regenerate 0.5 health a second, even with an empty food bar. In light level 4 or lower you get hungry: 0.1 exhaustion a second, which is about half a drumstick every 40 seconds. Light level 10 is daylight or standing near a torch, so caves and night-time are where you get hungry.

**No swimming.** When you're fully under water you sink at 0.5 blocks a tick unless you're in creative, spectator or flying. You can still walk along the bottom, so cross rivers by boat or bridge, and don't dive for drowned loot.

**Special (power-up key, full mana): Wild Shape.** You turn into a random animal from the last 16 kinds of animal you've killed, for 30 seconds. Once you've killed an [Owlbear](../mobs/owlbear.md) it joins the list. If you haven't killed an animal yet, nothing happens and you keep your mana.

### Tips
Tame a few wolves early: five pets is +5 hearts. Kill a range of animals before relying on Wild Shape, since the form is picked at random. Carry torches for long cave trips so you heal instead of starving.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 6 at 1000 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) at an Attunement Table.

Druids get 3 XP for each animal they kill, and 4 extra for each hostile mob killed while in an animal form.

| Skill | Branch | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Wild Shape | Root | Active | 0 | 9 | The special above |
| Hardy Form | Moon | Passive | 1 | | Resistance I while in animal form |
| Thorn Burst | Moon | Active | 1 | 4 | Hostile mobs within 6 blocks take 3 thorns damage and get Slowness IV for 4 s |
| Beast Bond | Moon | Passive | 1 | | Tamed animals give up to 10 extra hearts instead of 5 |
| Tidecaller | Land | Passive | 1 | | You can swim again |
| Regrowth | Land | Active | 1 | 4 | Regeneration II for 8 s to you, other players and your pets within 8 blocks |
| Photosynthesis | Land | Passive | 1 | | In light level 10 or higher you also get half a drumstick of food every 3 seconds |
| Call of the Wild | Capstone | Active | 2 | 9 | Three tamed wolves appear around you and fight for 60 s |

With Beast Bond and ten pets you reach 40 health, the same as a Barbarian. Photosynthesis means you barely need food on the surface.

## Commands
- `/dndclass set <player> druid` (see [Admin commands](../systems/admin-commands.md)).

## Known limitations
- Animal forms need the optional **Identity** mod. Without it Wild Shape does nothing and your mana is kept.
- Only animals loaded in your world count towards the extra hearts.
- The Call of the Wild wolves vanish when their time is up, or if their chunk unloads.

## For developers
- All mechanics: `Druid.java` (hearts, `lightTick`, `transform`). Swimming: `mixin/PlayerEntityMixin.tick`. The killed-animal list is saved in the player's persistent NBT (`druidKilledAnimals`, with the active form's end time in `druidFormExpiry`); `ClassLifecycle` copies both to the new player on death and End exit.
- Skill tree: `Progression/Classes/DruidSkills.java`. Beast Bond, Photosynthesis and Tidecaller are checked in `Druid` and `PlayerEntityMixin`.
- Mobs in the `#dndclasses:druid_forms` entity tag count as forms when killed.
