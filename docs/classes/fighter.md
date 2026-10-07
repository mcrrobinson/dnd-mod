# Fighter
A front-line tank. Strong and tough, it draws monsters to itself but has no bows and no potions.

## How it works
- **Health:** 26 max. You start at 25.
- **Damage:** base attack damage 6.
- **Attracts mobs:** monsters that already hunt players switch to the nearest Fighter in range, even if they were chasing someone else. Mobs that only attack when angry (zombified piglins, polar bears) still only go for Fighters they're angry at. Endermen are left alone.
- **No bows or crossbows:** using one is refused ("Fighters cannot use bows!").
- **No potions:** you can't drink or throw potions. Potion buffs from any potion source (drinking, splash, lingering clouds, tipped arrows) don't apply, but harmful potion effects still do (e.g. a witch's splash).
- **Special (power-up key, full mana): super regeneration.** Regeneration V for 10 seconds (about 2 hearts a second). It isn't a potion, so it works.

## For developers
- Mob attraction: `Goals/PriorityPlayerTargetGoal` (attached on entity load). Bow and potion refusal: the `UseItemCallback` in `DnDClasses`. Potion immunity: `PotionImmunity` plus the `Potion*Mixin`, `AreaEffectCloudEntityMixin` and `ArrowEntityMixin`.
- Devscript: `devscripts/brewing-use-fighter.txt`.
