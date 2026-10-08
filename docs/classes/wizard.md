# Wizard
A glass-cannon caster whose special is a massive explosion centred on itself.

## How it works
- **Health:** 10 max.
- **Elemental staffs:** only Wizards can cast or hit with the [Staffs of Fire, Ice and Lightning](../items/staffs.md). Other classes get "Only Wizards can wield elemental staffs!".
- **Max iron armor:** any armor piece with more armor points than iron in its slot (diamond, netherite, the other classes' sets) is taken off into your inventory, or dropped if it's full. The Wizard set is allowed.
- **Immune to Freeze**, so you can't freeze yourself with a [Staff of Ice](../items/staffs.md).
- **Special (power-up key, full mana):** a power-40 explosion at your position. It breaks no blocks, but it hurts every entity in range except you. Players within 64 blocks see a sphere and hear a blast. You also get Resistance V for 5 seconds, which blocks all normal damage (not the void or `/kill`).

## For developers
- Special: `PowerUpEffect` (`WIZARD`), sphere `Client/MySphereRenderState`, damage type `dndclasses:wizard_explosion`. Freeze immunity: `mixin/LivingEntityMixin`. Staff and armor limits: `Items/ExtendedSwordItem.canWield`, `WizardSkills.removeHeavyArmor`. Devscript: `devscripts/wizard-limits.txt`.
