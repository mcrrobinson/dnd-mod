# Bard
A fast, fragile charmer that monsters ignore and animals rally around.

## How it works
- **Health:** 15 max.
- **Speed:** 0.12 movement speed (20% faster than vanilla).
- **Unnoticed by monsters:** hostile mobs won't start targeting a Bard. With the Identity mod installed, a Bard that has attacked a mob (Identity's "hostility") is fair game again.
- **Poison heals:** instead of hurting, each poison tick heals you 1 health.
- **Special (power-up key, full mana):** every animal within 10 blocks turns on nearby hostile mobs, and untamed tameable animals (wolves, cats, parrots, horses...) in that radius become yours.

## Known limitations
- The class picker blurb mentions a diamond-armor limit and longer jumps. Neither is implemented.

## For developers
- Attributes: `ClassStats`. Monster targeting: `mixin/ActiveTargetGoalMixin`. Poison: `mixin/StatusEffectMixin`. Special: `PowerUpEffect.bardEffect`.
