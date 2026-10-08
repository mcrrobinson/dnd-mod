# Wizard
A glass cannon. You're the only class that can use the elemental staffs, and your special is a huge explosion centred on you.

![A Wizard's Staff of Fire blast](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/staff-fire.png)

## How it works
| Stat | Wizard | Vanilla |
|-|-|-|
| Max health | 10 (5 hearts) | 20 |

Picking the class sets your health to 10.

**Elemental staffs.** Only Wizards can cast or hit with the [Staffs of Fire, Ice and Lightning](../items/staffs.md). Other classes get "Only Wizards can wield elemental staffs!".

**Iron armor at most.** Any armor piece with more armor points than iron in its slot (diamond, netherite, the other classes' sets) is taken off into your inventory, or dropped if your inventory is full, with "Wizards can't wear armor heavier than iron!". The Wizard set is allowed.

**Immune to Freeze**, so you can't freeze yourself with a [Staff of Ice](../items/staffs.md).

**Special (power-up key, full mana): Arcane Explosion.** A power-40 explosion at your position. It breaks no blocks, but it hurts and throws back every entity in range except you, including other players and your pets. Players within 64 blocks see a sphere and hear the blast. You also get Resistance V for 5 seconds, which blocks all normal damage (not the void or `/kill`).

### Tips
With 5 hearts, one close creeper blast can end you. Fight from range with a staff and keep the explosion for when you're surrounded; it clears almost anything around you. Warn friends before you use it, because it hits them too. The Resistance V afterwards gives you 5 seconds to get away.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 10 at 2700 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) and rank up abilities at an Attunement Table (see [Class progression](../systems/class-progression.md)).

Wizards get 3 extra XP for each hostile mob killed with a staff, magic, an explosion or a meteor.

"Staff damage" below means a melee hit with an elemental staff, or a staff blast you set off yourself.

| Skill | Branch | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Arcane Explosion | Root | Active | 0 | 9 | The special above |
| Arcane Focus | Evocation | Passive | 1 | | 25% more staff damage |
| Frost Nova | Evocation | Active | 1 | 4 | Freezes hostile mobs within 6 blocks for 3 s |
| Spell Mastery | Evocation | Passive | 1 | | Your own blasts and meteors can't hurt you, and staff damage to frozen mobs is 50% higher |
| Mage Armor | Abjuration | Passive | 1 | | +4 armor |
| Arcane Shield | Abjuration | Active | 1 | 4 | Absorption II (4 hearts) for 15 s |
| Fortitude | Abjuration | Passive | 1 | | +2 hearts of max health |
| Meteor Swarm | Capstone | Active | 2 | 9 | A fireball falls every 3 ticks around the spot you're looking at (up to 30 blocks, 5-block spread) for 3 s. Each explodes at power 1.5 without breaking blocks or starting fires |

Frost Nova into a staff combo is the Evocation play: freeze them, then hit them for 50% more with Spell Mastery. Abjuration is the safer path and brings you up to 14 health with Fortitude. Without Spell Mastery, your own meteors can hurt you, so don't aim them at your feet.

## Commands
- `/dndclass set <player> wizard` (see [Admin commands](../systems/admin-commands.md)).

## For developers
- Special: `PowerUpEffect` (`WIZARD`, `WIZARD_INVULNERABLE_TICKS`), sphere `Client/MySphereRenderState`, damage type `dndclasses:wizard_explosion`. Freeze immunity: `mixin/LivingEntityMixin`. Staff and armor limits: `Items/ExtendedSwordItem.canWield`, `WizardSkills.removeHeavyArmor`.
- Skill tree: `Progression/Classes/WizardSkills.java`.
- Devscript: `devscripts/wizard-limits.txt`.
