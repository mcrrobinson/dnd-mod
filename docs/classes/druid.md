# Druid
A nature class that gets tougher with every tamed animal, heals in sunlight and can take the shape of animals it has hunted and unlocked.

![A Druid in Wild Shape as an Owlbear](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/druid-wild-shape-owlbear.png)

## How it works
**Animal hearts.** You get 1 extra heart (2 health) of max health for each living tamed animal you own in your current world, up to 5 hearts. The count is redone every second, so a pet dying costs you a heart straight away. Only animals that are loaded count, so pets left at a far-away base don't help.

**Light.** In light level 10 or higher you regenerate 0.5 health a second, even with an empty food bar. In light level 4 or lower you get hungry: 0.1 exhaustion a second, which is about half a drumstick every 40 seconds. Light level 10 is daylight or standing near a torch, so caves and night-time are where you get hungry.

**No swimming.** When you're fully under water you sink at 0.5 blocks a tick unless you're in creative, spectator or flying. You can still walk along the bottom, so cross rivers by boat or bridge, and don't dive for drowned loot.

**Special (power-up key, 9 mana): Wild Shape.** You turn into an animal you have both killed and unlocked, for 15 to 30 seconds depending on Wild Shape's rank.
- **Learn by killing.** Killing an animal (or a creature in the `#dndclasses:druid_forms` tag, such as the [Owlbear](../mobs/owlbear.md)) as a Druid adds it to your bestiary for good ("Learned Wolf. Unlock it at an Attunement Table.").
- **Unlock at an Attunement Table.** Open the table, go to the **Bestiary** tab and click a learned creature. Unlocking is free, but stronger forms need a higher Wild Shape rank (ranked up on the Tree tab with right-click, see [Class progression](../systems/class-progression.md)).
- **Pick the form.** Sneak and press the power-up key to cycle through your unlocked forms and then "random". This is free and works without mana. Wild Shape then uses the picked form, or a random unlocked one on "random".
- If nothing is unlocked yet, nothing happens and you keep your mana.

| Rank | Class level | Duration | Forms you can unlock |
|-|-|-|-|
| I | 0 | 15 s | Tier I |
| II | 3 | 20 s | Up to tier II |
| III | 6 | 25 s | Up to tier III |
| IV | 9 | 30 s | Up to tier IV |

| Tier | Creatures |
|-|-|
| I | Every other animal: chicken, rabbit, fox, pig, cow, mooshroom, sheep, frog, turtle and so on |
| II | Wolf, goat, cat, ocelot, axolotl, bee, parrot, strider |
| III | Polar bear, horse, donkey, mule, skeleton horse, zombie horse, llama, trader llama, camel, panda, hoglin, sniffer |
| IV | The mod's own creatures (the Owlbear) and creatures from other mods |

### Tips
Tame a few wolves early: five pets is +5 hearts. Unlock a fast or tough form (a wolf early, a horse or polar bear later) and pick it with sneak + power-up before a fight. Carry torches for long cave trips so you heal instead of starving.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 10 at 2700 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) and rank up abilities at an Attunement Table (see [Class progression](../systems/class-progression.md)).

Druids get 3 XP for each animal they kill, and 4 extra for each hostile mob killed while in an animal form.

| Skill | Subclass | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Wild Shape | Root | Active | 0 | 9 | The special above; 4 ranks |
| Hardy Form | Circle of the Moon | Passive | 1 | | Resistance I while in animal form |
| Thorn Burst | Circle of the Moon | Active | 1 | 4 | Hostile mobs within 6 blocks take 3 thorns damage and get Slowness IV for 4 s |
| Beast Bond | Circle of the Moon | Passive | 1 | | Tamed animals give up to 10 extra hearts instead of 5 |
| Tidecaller | Circle of the Land | Passive | 1 | | You can swim again |
| Regrowth | Circle of the Land | Active | 1 | 4 | Regeneration II for 8 s to you, other players and your pets within 8 blocks |
| Photosynthesis | Circle of the Land | Passive | 1 | | In light level 10 or higher you also get half a drumstick of food every 3 seconds |
| Call of the Wild | Capstone | Active | 2 | 9 | Three tamed wolves appear around you and fight for 60 s |

With Beast Bond and ten pets you reach 40 health, the same as a Barbarian. Photosynthesis means you barely need food on the surface.

### Subclasses
From class level 3, choose one of these two at an Attunement Table (see [Subclasses](../systems/class-progression.md#subclasses)). Your subclass's branch opens fully. The other branch's first node stays open, but the rest of it is sealed.

| Subclass | Branch | Nodes | Feature (free, doesn't use a passive slot) |
|-|-|-|-|
| Circle of the Moon | Right | Hardy Form (first node, open to both), Thorn Burst, Beast Bond | **Primal Strike**: your attacks deal +2 damage in animal form |
| Circle of the Land | Left | Tidecaller (first node, open to both), Regrowth, Photosynthesis | **Natural Recovery**: +1 mana pip every 30 seconds while standing on grass, leaves or moss |

The features aren't active yet; they come with the subclass feature cards.

## Commands
- `/dndclass set <player> druid` (see [Admin commands](../systems/admin-commands.md)).

## Known limitations
- Animal forms need the optional **Identity** mod. Without it Wild Shape does nothing and your mana is kept.
- Only animals loaded in your world count towards the extra hearts.
- The Call of the Wild wolves vanish when their time is up, or if their chunk unloads.

## For developers
- All mechanics: `Druid.java` (hearts, `lightTick`, `transform`, `availableForms`, `cycleForm`). Swimming: `mixin/PlayerEntityMixin.tick`.
- Forms come from the shared bestiary (`ClassProgress.learned` / `bestiary`, see [Class progression](../systems/class-progression.md)). `availableForms` keeps unlocked entries whose tier is within Wild Shape's rank, so locked or unknown forms never come up.
- Ranks: `DruidSkills.WILD_SHAPE` (`Duration` per rank, plus the tier text). Tiers: `DruidSkills.bestiaryRank` (sets `TIER_2`, `TIER_3`, non-`minecraft` namespace = tier IV). No tags, so the client's table agrees with the server.
- Persistent NBT: the picked form in `druidChosenForm` (missing = random) and the active form's end time in `druidFormExpiry`. `ClassLifecycle` copies them on death and End exit.
- Sneak + power-up is caught in `DnDClasses.sendPowerupPacket` before the mana check (`Druid.cycleForm`), only while Wild Shape is the equipped active.
- Old saves: the pre-bestiary `druidKilledAnimals` list becomes learned but locked creatures (`Progression.get`); unlock them at a table.
- Test: `devscripts/druid-wild-shape.txt`.
- Skill tree: `Progression/Classes/DruidSkills.java`. Beast Bond, Photosynthesis and Tidecaller are checked in `Druid` and `PlayerEntityMixin`.
- Mobs in the `#dndclasses:druid_forms` entity tag are learned when killed, like animals.
