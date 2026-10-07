# Wizard
A glass-cannon caster whose special is a massive explosion centred on itself.

## How it works
- **Health:** 10 max.
- **Immune to Freeze**, so you can't freeze yourself with a [Staff of Ice](../items/staffs.md).
- **Special (power-up key, full mana):** a power-40 explosion at your position. It breaks no blocks, but it hurts every entity in range except you. Players within 64 blocks see a sphere and hear a blast.

## Known limitations
- The README says Wizards alone can wield the elemental staffs and are limited to iron armor. In the code anyone can use a staff and there's no armor limit.
- The special doesn't give the "few seconds of invulnerability" the description mentions. You're safe only because the explosion skips its caster.

## For developers
- Special: `PowerUpEffect` (`WIZARD`), sphere `Client/MySphereRenderState`, damage type `dndclasses:wizard_explosion`. Freeze immunity: `mixin/LivingEntityMixin`.
