# Monk
A nimble martial artist with the fastest attacks and a triple jump, but it may only fight with a staff or bare fists.

## How it works
- **Speed:** 0.12 movement speed (20% faster than vanilla).
- **Attack speed:** 6 (vanilla 4).
- **Triple jump:** press jump in mid-air while falling for up to two extra jumps, which reset when you land or climb. You can't do it while wearing a usable elytra, gliding, riding, in water or levitating.
- **Staff or fists only:** attacking while holding anything other than the [Monk Staff](../items/staffs.md#monk-staff) or an empty hand is cancelled.
- **Armor penalty:** your damage is multiplied by `0.75 / (1 + armor / 10)`. That's 75% unarmored, 37.5% at 10 armor points and 25% in full diamond (20 points).
- **Special (power-up key):** none yet. Pressing the key with full mana spends it for nothing.

## Known limitations
- No class special is implemented for the Monk.

## For developers
- Rules and damage modifier: `MonkHandler`. Jump: `mixin/DoubleJumpMixin` (client) plus the `doublejump` packets for the cloud effect.
