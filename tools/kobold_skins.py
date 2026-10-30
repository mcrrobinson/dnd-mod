"""Paints the five kobold skins over the model's texture layout.

    python3 tools/kobold_skins.py

Writes textures/entity/kobold/kobold_<0-4>.png (64x64): rust red, ochre, umber, slate and ash scales, each with
paler belly scales, bone-white horn nubs, yellow slit eyes, a hide loincloth, and the shared weapon, shield and
crest colours (the renderer hides what a kobold doesn't carry). The layout comes from kobold_model.py, so run that
first when the model changes.
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402
import kobold_model  # noqa: E402

ROOT = kobold_model.ROOT
OUT = os.path.join(kobold_model.ASSETS, 'textures/entity/kobold')

#: (name, scale colour, belly colour)
HUES = [
    ('rust', (150, 62, 40), (206, 150, 104)),
    ('ochre', (178, 128, 50), (226, 196, 128)),
    ('umber', (102, 72, 46), (168, 136, 98)),
    ('slate', (78, 90, 104), (150, 160, 166)),
    ('ash', (122, 118, 110), (186, 180, 168)),
]

HORN = (226, 214, 184)
CLAW = (40, 32, 28)
EYE = (240, 206, 50)
PUPIL = (24, 16, 10)
CLOTH = (112, 78, 48)
CLOTH_DARK = (72, 50, 30)
WOOD = (120, 86, 52)
IRON = (176, 178, 182)
LEATHER = (96, 64, 40)
SHIELD_WOOD = (110, 76, 46)
SHIELD_PAINT = (176, 30, 28)
CREST = (186, 34, 30)
CREST_TIP = (232, 182, 60)


def mix(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def jitter(rng, c, amount):
    j = rng.randint(-amount, amount)
    return tuple(max(0, min(255, v + j)) for v in c)


def painter(scale, belly, rng):
    dark = mix(scale, (0, 0, 0), 0.35)

    def paint_scale(face, x, y, w, h):
        c = jitter(rng, scale, 8)
        # Rows of overlapping scales: a darker rim every other pixel, staggered.
        if (x + (y // 1) % 2) % 2 == 0 and y % 2 == 1:
            c = mix(c, dark, 0.35)
        if face == 'down':
            c = mix(c, dark, 0.4)
        if face == 'up':
            c = mix(c, (255, 255, 255), 0.08)
        return c

    def paint_belly(face, x, y, w, h):
        c = jitter(rng, belly, 6)
        if y % 2 == 0:
            c = mix(c, scale, 0.25)
        return c

    def paint_face(face, x, y, w, h):
        c = paint_scale(face, x, y, w, h)
        if face == 'north':
            # A heavy brow over two small yellow eyes set wide, at the corners of the skull.
            if y == 0:
                return mix(c, dark, 0.5)
            if y == 1 and x in (0, w - 1):
                return EYE
        if face in ('east', 'west') and y == 1:
            # The eyes wrap round the corners: a slit pupil on the side, at the front edge.
            front = w - 1 if face == 'east' else 0
            if x == front:
                return PUPIL
        return c

    def paint_snout(face, x, y, w, h):
        c = paint_scale(face, x, y, w, h)
        if face == 'north' and y == 0 and x in (0, w - 1):
            return mix(c, (0, 0, 0), 0.6)  # nostrils
        if face in ('east', 'west', 'north') and y == h - 1:
            return mix(c, (60, 20, 20), 0.5)  # the mouth line
        return c

    def paint_hand(face, x, y, w, h):
        c = mix(paint_scale(face, x, y, w, h), dark, 0.2)
        if face == 'down':
            return CLAW
        return c

    def paint_cloth(face, x, y, w, h):
        c = jitter(rng, CLOTH, 6)
        if y == 0 or (x * 5 + y) % 7 == 0:
            c = mix(c, CLOTH_DARK, 0.7)
        return c

    def paint_horn(face, x, y, w, h):
        c = jitter(rng, HORN, 6)
        return mix(c, (120, 110, 90), 0.3) if y == h - 1 else c

    def paint_claw(face, x, y, w, h):
        return jitter(rng, CLAW, 4)

    def paint_wood(face, x, y, w, h):
        c = jitter(rng, WOOD, 6)
        return mix(c, (60, 40, 24), 0.4) if (y + x) % 4 == 0 else c

    def paint_iron(face, x, y, w, h):
        c = jitter(rng, IRON, 10)
        return mix(c, (90, 90, 96), 0.4) if y == h - 1 else c

    def paint_leather(face, x, y, w, h):
        return jitter(rng, LEATHER, 6)

    def paint_shield(face, x, y, w, h):
        c = jitter(rng, SHIELD_WOOD, 6)
        if face in ('east', 'west'):
            # A red dragon's head daubed on the face: a wedge pointing up, with an eye.
            cx = (w - 1) / 2
            if y >= 1 and abs(x - cx) <= (y + 1) / 2 and y <= h - 2:
                c = jitter(rng, SHIELD_PAINT, 8)
                if y == 2 and round(x) == round(cx):
                    c = (240, 206, 50)
            if x in (0, w - 1) or y in (0, h - 1):
                c = mix(c, (60, 60, 64), 0.7)  # iron rim
        return c

    def paint_crest(face, x, y, w, h):
        c = jitter(rng, CREST, 8)
        if face in ('east', 'west') and y == 0:
            c = CREST_TIP
        return c

    return {'scale': paint_scale, 'belly': paint_belly, 'face': paint_face, 'snout': paint_snout,
            'hand': paint_hand, 'cloth': paint_cloth, 'horn': paint_horn, 'claw': paint_claw, 'wood': paint_wood,
            'iron': paint_iron, 'leather': paint_leather, 'shield': paint_shield, 'crest': paint_crest}


def main():
    faces, _ = kobold_model.layout()
    os.makedirs(OUT, exist_ok=True)
    for index, (name, scale, belly) in enumerate(HUES):
        rng = random.Random(400 + index)
        paint = painter(scale, belly, rng)
        pixels = [[(0, 0, 0, 0)] * kobold_model.TEX_W for _ in range(kobold_model.TEX_H)]
        for bone, cube, face, material, u, v, w, h in faces:
            for y in range(h):
                for x in range(w):
                    r, g, b = paint[material](face, x, y, w, h)
                    pixels[v + y][u + x] = (r, g, b, 255)
        path = os.path.join(OUT, f'kobold_{index}.png')
        pngio.write_png(path, kobold_model.TEX_W, kobold_model.TEX_H, pixels)
        print('wrote', os.path.relpath(path, ROOT), f'({name})')


if __name__ == '__main__':
    main()
