"""Generates the Ember Wyvern textures by recolouring the green wyvern texture.

    python3 tools/ember_wyvern_texture.py

Olive scales become charred black-red, the tan wing membranes molten orange, pale claws/horns ash
grey and the red eyes/mouth bright yellow-orange. Also writes ember_glowmask.png (GeckoLib's
AutoGlowingGeoLayer draws it fullbright), holding the eyes and the brightest membrane pixels.
"""
import colorsys
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
TEX = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/entity/wyvern')


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def recolour(r, g, b):
    """Returns (colour, glow) where glow is the glowmask colour or None."""
    h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
    hue = h * 360
    if s < 0.1:
        # Greys: keep, slightly warmer
        return lerp((20, 14, 14), (140, 120, 115), l / 0.6), None
    if (hue < 15 or hue > 340) and s >= 0.3:
        # Eyes and mouth: glowing
        c = lerp((255, 140, 20), (255, 230, 120), (l - 0.2) / 0.3)
        return c, c
    if s >= 0.8:
        # Odd saturated marker pixels: leave alone
        return (r, g, b), None
    if 15 <= hue < 48 and s < 0.35:
        if l >= 0.6:
            # Pale claws, teeth and horns: ash
            return lerp((95, 88, 86), (185, 175, 170), (l - 0.6) / 0.2), None
        # Wing membranes: molten orange, brightest bits glow
        c = lerp((120, 28, 8), (235, 105, 20), (l - 0.35) / 0.17)
        glow = lerp((90, 25, 0), (200, 80, 10), (l - 0.45) / 0.07) if l >= 0.45 else None
        return c, glow
    # Scales (olive/green): charred black-red
    return lerp((22, 10, 10), (115, 30, 20), (l - 0.03) / 0.3), None


def main():
    w, h, rows = pngio.read_png(os.path.join(TEX, 'green.png'))
    out, glow = [], []
    for row in rows:
        orow, grow = [], []
        for r, g, b, a in row:
            if a == 0:
                orow.append((0, 0, 0, 0))
                grow.append((0, 0, 0, 0))
                continue
            c, gl = recolour(r, g, b)
            orow.append(c + (a,))
            grow.append(gl + (a,) if gl else (0, 0, 0, 0))
        out.append(orow)
        glow.append(grow)
    pngio.write_png(os.path.join(TEX, 'ember.png'), w, h, out)
    pngio.write_png(os.path.join(TEX, 'ember_glowmask.png'), w, h, glow)
    print('wrote ember.png and ember_glowmask.png')


if __name__ == '__main__':
    main()
