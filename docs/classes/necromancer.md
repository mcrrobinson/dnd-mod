# Necromancer
A weak fighter that the undead leave alone and that can raise undead allies.

## How it works
- **Damage:** base attack damage 0.5 (half of vanilla).
- **Health:** picking the class drops your current health to 5 (max health stays at 20).
- **Wither touch:** every melee hit gives the target Wither I for 5 ticks.
- **Undead don't attack:** undead monsters (zombies, skeletons, wither skeletons...) won't target you.
- **Special (power-up key, full mana):** summons a zombie and a skeleton beside you. They follow you, attack monsters, and despawn after a short time (100 goal ticks).

## Known limitations
- The Wither from melee hits is very short, so it rarely does any damage.

## For developers
- Wither: the `AttackEntityCallback` in `DnDClasses`. Undead: `mixin/ActiveTargetGoalMixin`. Summons: `PowerUpEffect.spawnUndead`, `Goals/FollowSummonerGoal`, `Goals/TimedDespawnGoal`.
