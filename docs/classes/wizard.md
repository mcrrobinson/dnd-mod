# Wizard
A glass cannon. You're the only class that can use the elemental staffs, and your special is an explosion centred on you that grows as you rank it up.

![A Wizard's Staff of Fire blast](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/staff-fire.png)

## How it works
| Stat | Wizard | Vanilla |
|-|-|-|
| Max health | 10 (5 hearts) | 20 |

Picking the class sets your health to 10.

**Elemental staffs.** Only Wizards can cast or hit with the [Staffs of Fire, Ice and Lightning](../items/staffs.md). Other classes get "Only Wizards can wield elemental staffs!".

**Arcane training.** Every unidentified [magic item](../systems/magic-items.md#identification) that enters your inventory is identified within a second, curse included ("Your arcane training reveals: +2 Diamond Sword (Rare, cursed: Bloodthirst)"). It doesn't bind you, so you can check the party's loot.

**Iron armor at most.** Any armor piece with more armor points than iron in its slot (diamond, netherite, the other classes' sets) is taken off into your inventory, or dropped if your inventory is full, with "Wizards can't wear armor heavier than iron!". The Wizard set is allowed.

**Immune to Freeze**, so you can't freeze yourself with a [Staff of Ice](../items/staffs.md).

**Special (power-up key, full mana): Arcane Explosion.** An explosion at your position. It breaks no blocks, but it hurts and throws back every entity in range except you, including other players and your pets. Damage falls off with distance, like any explosion. Players within 64 blocks see a sphere that grows to the blast's radius and hear the blast. You also get Resistance V, which blocks all normal damage (not the void or `/kill`). Rank it up at an Attunement Table (see [Class progression](../systems/class-progression.md#ability-ranks)):

| Rank | Class level | Radius | Explosion power | Resistance V |
|-|-|-|-|-|
| I | 0 | 12 blocks | 6 | 2 s |
| II | 3 | 28 blocks | 14 | 3 s |
| III | 6 | 48 blocks | 24 | 4 s |
| IV | 9 | 72 blocks | 36 | 5 s |

A TNT blast is power 4 and reaches 8 blocks. The old unranked explosion was power 40 (80 blocks).

### Tips
With 5 hearts, one close creeper blast can end you. Fight from range with a staff and keep the explosion for when you're surrounded; at rank I it only clears what's close, and by rank IV almost anything around you. Warn friends before you use it, because it hits them too. The Resistance V afterwards gives you 2 to 5 seconds to get away.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 10 at 2700 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) and rank up abilities at an Attunement Table (see [Class progression](../systems/class-progression.md)).

Wizards get 3 extra XP for each hostile mob killed with a staff, magic, an explosion or a meteor.

"Staff damage" below means a melee hit with an elemental staff, or a staff blast you set off yourself.

| Skill | Subclass | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Arcane Explosion | Root | Active | 0 | 9 | The special above: 12 / 28 / 48 / 72-block radius and Resistance V for 2 / 3 / 4 / 5 s (ranks I-IV) |
| Arcane Focus | School of Evocation | Passive | 1 | | 25% more staff damage |
| Frost Nova | School of Evocation | Active | 1 | 4 | Freezes hostile mobs within 6 blocks for 3 s |
| Spell Mastery | School of Evocation | Passive | 1 | | Your own blasts and meteors can't hurt you, and staff damage to frozen mobs is 50% higher |
| Mage Armor | School of Abjuration | Passive | 1 | | +4 armor |
| Arcane Shield | School of Abjuration | Active | 1 | 4 | Absorption II (4 hearts) for 15 s |
| Fortitude | School of Abjuration | Passive | 1 | | +2 hearts of max health |
| Meteor Swarm | Capstone | Active | 2 | 9 | A fireball falls every 3 ticks around the spot you're looking at (up to 30 blocks, 5-block spread) for 3 s. Each explodes at power 1.5 without breaking blocks or starting fires |

Frost Nova into a staff combo is the Evocation play: freeze them, then hit them for 50% more with Spell Mastery. Abjuration is the safer path and brings you up to 14 health with Fortitude. Without Spell Mastery, your own meteors can hurt you, so don't aim them at your feet.

### Subclasses
From class level 3, choose one of these two at an Attunement Table (see [Subclasses](../systems/class-progression.md#subclasses)). Your subclass's branch opens fully. The other branch's first node stays open, but the rest of it is sealed.

| Subclass | Branch | Nodes | Feature (free, doesn't use a passive slot) |
|-|-|-|-|
| School of Evocation | Right | Arcane Focus (first node, open to both), Frost Nova, Spell Mastery | **Sculpt Spells**: your staff blasts, Arcane Explosion and Meteor Swarm don't hurt or knock back party members or their pets |
| School of Abjuration | Left | Mage Armor (first node, open to both), Arcane Shield, Fortitude | **Arcane Ward**: every active you fire gives 2 absorption hearts (up to 4), fading 60 seconds after the last |

How the features work:
- **Sculpt Spells** (Evocation): your staff blasts, Arcane Explosion and Meteor Swarm meteors treat your party members, your pets and theirs (tamed wolves, cats, horses...) as immune: no damage and no knockback. The Ice staff doesn't freeze them, and the Lightning staff's bolts don't land within 3 blocks of them. Party members already couldn't hurt each other; this also spares pets and stops the knockback. You still get blasted yourself unless you have Spell Mastery.
- **Arcane Ward** (Abjuration): every active you fire (Arcane Explosion included) adds 2 absorption hearts, up to 4 from the ward. Hits take it first. Whatever is left fades 60 s after the last active, or when you log off or change class.

## Commands
- `/dndclass set <player> wizard` (see [Admin commands](../systems/admin-commands.md)).

## For developers
- Special: `PowerUpEffect` (`WIZARD`); per-rank radius and Resistance length in `WizardSkills.ARCANE_EXPLOSION` (power = radius / 2). The `wizard_powerup` packet carries the blast position and radius, and the sphere (`Client/MySphereRenderState`) grows to that radius; damage type `dndclasses:wizard_explosion`. Freeze immunity: `mixin/LivingEntityMixin`. Staff and armor limits: `Items/ExtendedSwordItem.canWield`, `WizardSkills.removeHeavyArmor`.
- Skill tree: `Progression/Classes/WizardSkills.java`.
- Devscripts: `devscripts/wizard-limits.txt`, `devscripts/wizard-ranks.txt` (explosion at rank I and IV).
