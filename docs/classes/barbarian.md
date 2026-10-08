# Barbarian
A slow, short-sighted brawler with a huge health pool and a heavy punch. Inspired by *One Punch Man*.

## How it works
- **Health:** 40 max (vanilla 20). You start at 25 when you pick the class.
- **Damage:** base attack damage 6 (vanilla 1), so even bare fists hit hard.
- **Speed:** 0.08 movement speed (vanilla 0.1, so 20% slower).
- **Limited vision:** fog closes in from 4 to 24 blocks, whatever your render distance. Water, lava and blindness still win if they're closer.
- **Special (power-up key, full mana):** Strength III for 15 seconds.

## Known limitations
- The fog is client-side only. It can't be turned off.

## For developers
- Attributes: `ClassStats`. Fog: `mixin/BackgroundRendererMixin`. Special: `Misc/PowerUpEffect` (`BARBARIAN`).
- See [Mana and specials](../systems/mana.md) for how the special is triggered.
