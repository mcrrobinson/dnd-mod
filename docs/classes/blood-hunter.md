# Blood Hunter
A sword fighter that rules the night and can possess any mob it looks at.

## How it works
- **Burning blades:** every sword hit sets the target on fire for 8 seconds, like Fire Aspect II.
- **Night and day:** with a sword in hand you deal double damage at night (time 13000-23000) and half damage during the day.
- **Swords stay in hand:** you can't throw a sword away (Q, or Q / clicking outside the window in an inventory). Swords still drop when you die, and creative mode isn't affected.
- **Special (power-up key, full mana): take control.** Look at a mob within 30 blocks (not through walls; not the Ender Dragon or the Wither) to become it for 20 seconds. You teleport to it and take its shape. When the time runs out (or you log off), the mob reappears where you're standing with its original health, name and gear. If there's no valid target, nothing happens and you keep your mana.

## Known limitations
- Taking control needs the optional **Identity** mod.

## For developers
- Fire and damage: `DnDClasses` (`AttackEntityCallback`) and `mixin/LivingEntityMixin.modifyDamageAmount`. No sword drops: `Misc/BloodHunterSwords` and the `*SwordDropMixin` mixins. Control: `Misc/BloodHunterControl`, `Misc/BloodHunterIdentityCompat`.
