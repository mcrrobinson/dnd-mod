# Staffs
Three elemental staffs that blast whatever you point them at, for [Wizards](../classes/wizard.md) only, plus the Monk's Staff.

![The Staff of Ice turning the ground to ice](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/staff-ice.png)

## How it works
### Elemental staffs
The elemental staffs are **Rare** [magic items](../systems/magic-items.md) (blue names, "Rare weapon (Wizard only)"). They don't need attunement. The Monk's Staff is Common.

The Staff of Fire, Staff of Ice and Staff of Lightning only work for Wizards. Anyone else who tries to cast or hit with one gets "Only Wizards can wield elemental staffs!" on the action bar.

Right click to fire a beam at the block you're looking at, up to 40 blocks away. If the beam doesn't reach a block, nothing happens. Each cast sets off a power 3 explosion where it lands and puts the staff on a 1 second cooldown.

| Staff | What the cast does |
|-|-|
| Fire | Carves out a crater (solid blocks within 3 of the impact), sets fires, and fills the crater back in 5 seconds later. The removed blocks don't drop |
| Ice | Turns the solid blocks in a 7x7x7 cube around the impact to ice for 5 seconds, and freezes living things in a 7 wide, 11 tall box around it for 10 seconds |
| Lightning | Calls down up to 5 lightning bolts on exposed blocks around the impact |

Frozen mobs and players are held in place and get an icy tint. The lightning bolts are real lightning, so they can turn villagers into witches and pigs into zombified piglins.

The staffs don't touch bedrock, obsidian or anything as hard, or block entities such as chests, furnaces and spawners. Changed blocks go back without block updates, so torches and sand next to them don't pop off or fall. A block only goes back if its spot is still ice or empty (air, fire or water). Anything you place there in the meantime stays. Pending restores are saved with the world, so they finish after a restart.

As melee weapons the staffs are diamond swords with extra damage:

| Item | Attack damage | Attack speed |
|-|-|-|
| Staff of Fire, Ice or Lightning | 14 | 5.2 |
| Monk's Staff | 13 | 1.8 |

Tips:
- The blast can hurt you. Aim at something a few blocks away, not at your feet. The Wizard skill tree has a passive that makes your own blasts harmless.
- The Staff of Ice freezes you too if you're inside its box, so keep your distance.
- Ice is the safest crowd control: the blast breaks nothing and everything nearby stops moving for 10 seconds.
- The Fire crater is temporary, so it's no use for mining.

### Monk's Staff
The Monk's Staff is the only weapon a [Monk](../classes/monk.md) can attack with (bare fists also work). It has no right click ability.

Anyone can craft it from 3 sticks in a diagonal line, either way round:

```
    S
  S
S
```

## How to get it
The elemental staffs can't be crafted.

- **Monk's Staff:** craft it from 3 sticks in a diagonal line (see [Monk's Staff](#monks-staff)).
- **Staff of Lightning:** a 15% drop (+5% per level of Looting) from a [Lightning Chaser](../mobs/dragons.md) killed by a player. Each chest in a [dragon lair](../structures/dragon-lairs.md) hoard also has a 25% chance to hold one.
- **Staff of Ice:** a 15% drop (+5% per level of Looting) from a [Frost Drake](../mobs/dragons.md) killed by a player. Each chest in a [frost lair](../structures/frost-lairs.md) hoard on Frozen Peaks also has a 25% chance to hold one.
- **Staff of Fire:** a 25% drop (+5% per level of Looting) from a [Magmamuncher Alpha](../bosses/magmamuncher-alpha.md) killed by a player, or a rare 2% drop (+1% per level of Looting) from an [Ember Wyvern](../mobs/dragons.md) killed by a player.
- **All four:** the D&D Classes creative tab.

## Known limitations
- Casting uses no durability. Melee hits wear the staff down like any diamond sword.
- Mining the Staff of Ice's ice with Silk Touch gives you ice blocks.

## For developers
- `Items/ExtendedSwordItem`: staff behaviour by item id, `canWield` for the Wizard check, `BEAM_RANGE` and the cast and restore timings.
- `Items/ScheduledBlockRestore`: a per-world `PersistentState` (`data/dndclasses_staff_restores.dat`) that puts blocks back.
- `Items/MonkStaff`, `Effects/FreezeEffect`. The melee Wizard check is an `AttackEntityCallback` in `Progression/Classes/WizardSkills`.
- Recipe: `recipes/monk_staff.json`.
- Unused loot tables `chests/staff_of_fire.json`, `staff_of_ice.json`, `staff_of_lightning.json` and `monk_staff.json` each give one staff; nothing references them yet.
- Devscripts: `devscripts/staff-fire-dupe.txt`, `staff-restore-persist-a.txt` then `-b.txt`.
