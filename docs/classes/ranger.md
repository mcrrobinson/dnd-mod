# Ranger
An archer. Your bow draws almost instantly, you can zoom while aiming, and your special fires arrows for free.

![A Ranger in Arrow Storm with no arrows in the inventory, arrows landing around the targets](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/ranger-arrow-storm.png)

## How it works
**Fast draw.** A bow reaches full power in about 3 ticks instead of 20, so you can fire full-power shots as fast as you can click.

**Bow zoom.** Drawing a bow zooms the view in 2x, like a spyglass. It only works in first person, and not during Arrow Storm.

**Luck.** You have +5 luck, which improves loot tables that use it, such as fishing.

**No swords.** You can't pick swords up off the ground; they stay where they are.

**Lava is lethal.** Standing in lava deals you 20 extra damage every tick on top of the normal burn. Fire Resistance doesn't stop it, so treat lava as instant death.

**Special (power-up key, full mana): Arrow Storm.** Hold right click with a bow and it fires each time it's fully drawn, without needing or using arrows. It scales with its rank, which you raise with skill points at an Attunement Table:

| Rank | Class level | Fire rate | Arrow speed | Duration |
|-|-|-|-|-|
| I | 0 | 2 shots/s (one every 10 ticks) | 100% | 8 s |
| II | 3 | 2.5 shots/s (every 8 ticks) | 115% | 10 s |
| III | 6 | 3.3 shots/s (every 6 ticks) | 130% | 12 s |
| IV | 9 | 5 shots/s (every 4 ticks) | 150% | 15 s |

Faster arrows fly flatter and further, and hit a bit harder, since arrow damage scales with speed. The bow zoom and the vanilla bow FOV change are off while Arrow Storm runs, so the view stays steady.

### Tips
Carry a bow with Power and keep a stack of arrows, since outside Arrow Storm you still use them. The zoom makes long shots easy, and you get bonus XP for kills from 20 blocks or more. In the Nether, build bridges over lava lakes with walls on the sides.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 10 at 2700 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) and rank up abilities at an Attunement Table (see [Class progression](../systems/class-progression.md)).

Rangers get 2 extra XP for each hostile mob killed with an arrow, or 4 if it was 20 or more blocks away.

| Skill | Branch | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Arrow Storm | Root | Active | 0 | 9 | The special above; 4 ranks |
| Sharpshooter | Hunter | Passive | 1 | | Your arrows deal 25% more damage |
| Volley | Hunter | Active | 1 | 4 | Fire a fan of 5 critical arrows, 10 degrees apart, where you're looking. You can't pick them up |
| Hunter's Mark | Hunter | Passive | 1 | | Your arrows make targets glow for 10 s, and glowing targets take 20% more damage from all your attacks |
| Fireproof | Survivalist | Passive | 1 | | Removes the 20-a-tick lava damage. Lava still burns like normal |
| Snare | Survivalist | Active | 1 | 3 | Hostile mobs within 6 blocks get Slowness V for 5 s |
| Natural Explorer | Survivalist | Passive | 1 | | 10% faster movement |
| Rain of Arrows | Capstone | Active | 2 | 9 | Arrows fall on a 6-block radius around the block you're looking at (up to 30 blocks away) for 3 s, 2 arrows a tick, 4 damage each. They don't hurt you |

Hunter's Mark doesn't boost the arrow that applies the mark, only the hits after it. Rain of Arrows needs something to aim at: if you're looking at the sky, nothing happens and you keep your mana.

## Commands
- `/dndclass set <player> ranger` (see [Admin commands](../systems/admin-commands.md)).

## Known limitations
- The sword ban only covers picking swords up off the ground. A Ranger can still take one out of a chest, craft one or keep one they had before switching class.

## For developers
- Draw speed: `mixin/BowItemMixin` (server) and the bow `pull` predicate in `DndClassesClient`. Zoom: `mixin/RangerBowZoomMixin` (client, `RANGER_DRAW_TICKS`, `RANGER_ZOOM`). Sword pickup: `mixin/RangerSwordPickupMixin`. Auto-fire: `mixin/PlayerEntityMixin.tick` (waits `RangerSkills.arrowStormShotDelay` ticks between shots). Arrow speed: `BowItemMixin.dnd$arrowStormSpeed`. Ranks: `RangerSkills.ARROW_STORM`, duration read in `PowerUpEffect` case `RANGER`. Lava: `mixin/LavaDamageMixin`. Effect: `ModEffects.ARROW_STORM`.
- Skill tree: `Progression/Classes/RangerSkills.java`.
- Devscripts: `devscripts/ranger-sword-pickup.txt`, `devscripts/ranger-ranks.txt` (Arrow Storm duration at ranks I and IV, plus a screenshot of the steady FOV mid-storm).
