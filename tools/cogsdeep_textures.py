"""Generates the Cogsdeep textures (design 6, ticket T12: Cogsdeep gnome burrows).

    python3 tools/cogsdeep_textures.py

Writes:
- textures/entity/gnome/gnome_0..4.png: Gnome Tinker skins (64x64 player layout, classic arms) in the variant
  order of GnomeTinkerEntity: fair, tan, ruddy, brown, pale skin; white, ginger, chestnut, black, blond hair;
  a tunic under a leather apron with a tool belt, and brass goggles pushed up on the forehead (hat layer).
  The big nose is its own cuboid (RaceFeatures.renderNose) textured from the unused 8x16 strip at (56, 16).
- textures/entity/clockwork_sentinel/sentinel.png and sentinel_eyes.png: the Clockwork Sentinel, a copper golem
  on the iron golem's 128x128 layout (IronGolemEntityModel): riveted copper plates going green in patches, a
  clock face on the chest, banded arms and legs; the eyes layer glows (an emissive feature).
- textures/item/gnomish_repeater{,_pulling_0,_pulling_1,_pulling_2,_arrow,_firework}.png: the Gnomish Repeater,
  a brass-limbed crossbow with a drum magazine, in the vanilla crossbow's model states.
- textures/item/tinkers_goggles.png and textures/models/armor/tinkers_goggles/body_middle.png: Tinker's Goggles,
  the icon and the worn layer (vanilla 64x32 armor layout, head only), drawn by LayeredArmorRenderer.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
TEX = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures')

CLEAR = (0, 0, 0, 0)


def noise(x, y, seed=0):
    """Deterministic per-pixel noise in 0..1."""
    n = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def shade(color, x, y, amount=6, seed=0):
    n = int(noise(x, y, seed) * 5) - 2
    return tuple(max(0, min(255, c + n * amount // 2)) for c in color[:3]) + (255,)


def darker(color, f=0.75):
    return tuple(int(c * f) for c in color[:3])


def lighter(color, f=1.2):
    return tuple(min(255, int(c * f)) for c in color[:3])


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


class Canvas:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.px = [[CLEAR for _ in range(w)] for _ in range(h)]

    def put(self, x, y, color):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = color if len(color) == 4 else tuple(color) + (255,)

    def box(self, u, v, w, h, d, paint):
        """Paints a cuboid's six faces; paint(face, x, y, width, height) gives a colour or None."""
        faces = {
            'top': (u + d, v, w, d),
            'bottom': (u + d + w, v, w, d),
            'right': (u, v + d, d, h),
            'front': (u + d, v + d, w, h),
            'left': (u + d + w, v + d, d, h),
            'back': (u + 2 * d + w, v + d, w, h),
        }
        for face, (fx, fy, fw, fh) in faces.items():
            for y in range(fh):
                for x in range(fw):
                    c = paint(face, x, y, fw, fh)
                    if c is not None:
                        self.put(fx + x, fy + y, c)

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        pngio.write_png(path, self.w, self.h, self.px)


# ---- Gnome Tinkers ----

SKINS = [(236, 196, 164), (206, 156, 112), (226, 150, 128), (150, 98, 66), (244, 214, 196)]
HAIR = [(232, 232, 228), (196, 92, 40), (110, 62, 34), (36, 30, 30), (232, 196, 96)]
TUNIC = [(70, 120, 62), (56, 86, 150), (196, 150, 52), (128, 40, 48), (48, 130, 128)]
EYES = [(60, 110, 170), (60, 130, 70), (90, 60, 40), (40, 40, 40), (90, 140, 190)]
BEARD = [True, False, True, False, True]
APRON = (120, 78, 46)
BELT = (64, 42, 26)
BRASS = (214, 168, 64)
LENS = (150, 220, 210)
BOOTS = (78, 52, 34)
TROUSERS = (92, 76, 60)


def gnome(i):
    s = Canvas(64, 64)
    skin, hair, tunic, eyes = SKINS[i], HAIR[i], TUNIC[i], EYES[i]

    def head(face, x, y, w, h):
        if face in ('top', 'back'):
            return shade(hair, x, y, seed=i)
        if face in ('left', 'right'):
            if y < 3 or BEARD[i] and y >= 5:
                return shade(hair, x, y, seed=i)
            return shade(skin, x, y, seed=i)
        if face == 'bottom':
            return shade(hair if BEARD[i] else darker(skin, 0.85), x, y, seed=i)
        # Front: a fringe, bushy brows, big bright eyes, rosy cheeks and (some) a beard; the nose is a cuboid.
        if y == 0:
            return shade(hair, x, y, seed=i)
        if y == 2 and x in (1, 2, 5, 6):
            return shade(darker(hair, 0.9), x, y, seed=i)
        if y == 3 and x in (1, 6):
            return (250, 250, 250, 255)
        if y == 3 and x in (2, 5):
            return eyes + (255,)
        if y == 4 and x in (1, 6):
            return shade(mix(skin, (220, 90, 90), 0.35), x, y, seed=i)
        if BEARD[i] and (y >= 6 or y == 5 and x in (0, 1, 6, 7)):
            return shade(hair, x, y, seed=i)
        if y == 6 and 3 <= x <= 4:
            return shade(darker(skin, 0.5), x, y)
        return shade(skin, x, y, seed=i)
    s.box(0, 0, 8, 8, 8, head)

    def hat(face, x, y, w, h):
        # Brass goggles pushed up on the forehead (rows 1-2), the strap round the sides and back.
        if face == 'front' and y in (1, 2):
            if x in (1, 2, 5, 6) and y == 2:
                return LENS + (255,)
            if x in (3, 4) and y == 1:
                return None
            return shade(BRASS, x, y)
        if face in ('left', 'right', 'back') and y == 2:
            return shade((60, 40, 28), x, y)
        return None
    s.box(32, 0, 8, 8, 8, hat)

    def body(face, x, y, w, h):
        if face in ('top', 'bottom'):
            return shade(tunic, x, y, seed=i)
        if y == 7:
            if face == 'front' and x in (3, 4):
                return shade(BRASS, x, y)
            return shade(BELT, x, y)
        if face == 'front' and 1 <= x <= 6 and y >= 2:
            # The leather apron, with a pocket of tools.
            if y in (9, 10) and x in (2, 3):
                return shade((160, 160, 170), x, y)
            return shade(APRON, x, y, seed=i)
        if face == 'front' and y < 2 and x in (2, 5):
            return shade(APRON, x, y)
        return shade(tunic, x, y, seed=i)
    s.box(16, 16, 8, 12, 4, body)

    def arm(face, x, y, w, h):
        if face == 'bottom' or y >= 10:
            return shade(darker(skin, 0.9), x, y, seed=i)
        if y == 9:
            return shade(darker(tunic, 0.7), x, y)
        return shade(tunic, x, y, seed=i)
    s.box(40, 16, 4, 12, 4, arm)
    s.box(32, 48, 4, 12, 4, arm)

    def leg(face, x, y, w, h):
        if face == 'bottom' or y >= 8:
            return shade(BOOTS, x, y)
        return shade(TROUSERS, x, y, seed=i)
    s.box(0, 16, 4, 12, 4, leg)
    s.box(16, 48, 4, 12, 4, leg)

    # The nose: a 2x3x2 cuboid at uv (56, 16), skin with a rosy tip.
    def nose(face, x, y, w, h):
        base = darker(skin, 0.95)
        if face == 'bottom' or (face == 'front' and y == h - 1):
            return shade(mix(skin, (210, 100, 100), 0.4), x, y)
        return shade(base, x, y, seed=i)
    s.box(56, 16, 2, 3, 2, nose)
    return s


# ---- The Clockwork Sentinel ----

COPPER = (196, 108, 78)
EXPOSED = (164, 124, 100)
WEATHERED = (106, 152, 108)
OXIDISED = (78, 164, 132)
DARK = (94, 52, 36)
GOLD = (236, 196, 80)


def copper(x, y, seed):
    """Copper plate with green patches where it has started to weather."""
    n = noise(x // 3, y // 3, seed)
    if n > 0.95:
        base = OXIDISED
    elif n > 0.86:
        base = WEATHERED
    elif n > 0.78:
        base = EXPOSED
    else:
        base = COPPER
    return shade(base, x, y, 8, seed)


def sentinel():
    s = Canvas(128, 128)

    def head(face, x, y, w, h):
        if face == 'front':
            # Visor slit with two eyes, a grille mouth.
            if y == 3 and 1 <= x <= 6:
                return shade((30, 24, 22), x, y)
            if y == 4 and x in (2, 5):
                return (255, 200, 90, 255)
            if y == 4 and 1 <= x <= 6:
                return shade((30, 24, 22), x, y)
            if 7 <= y <= 8 and 2 <= x <= 5:
                return shade((40, 30, 26), x, y) if x % 2 == 0 else shade(DARK, x, y)
        if face == 'top' and (x in (0, w - 1) or y in (0, h - 1)):
            return shade(DARK, x, y)
        if y == 0 or y == h - 1 or x == 0 or x == w - 1:
            return shade(darker(COPPER, 0.8), x, y)
        if (x, y) in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
            return shade(GOLD, x, y)
        return copper(x, y, 1)
    s.box(0, 0, 8, 10, 8, head)

    def nose(face, x, y, w, h):
        return shade(DARK if y % 2 else darker(COPPER, 0.85), x, y)
    s.box(24, 0, 2, 4, 2, nose)

    def body(face, x, y, w, h):
        if face == 'front':
            cx, cy = (w - 1) / 2.0, (h - 1) / 2.0 - 0.5
            r = math.hypot(x - cx, (y - cy) * 1.1)
            if r <= 4.6:
                # The clock face: cream dial, gold rim, hands at ten past two.
                if r > 3.7:
                    return shade(GOLD, x, y)
                if abs(x - cx) < 0.6 and cy - 3.2 < y <= cy:
                    return (40, 30, 26, 255)
                if abs(y - cy) < 0.6 and cx <= x < cx + 2.6:
                    return (40, 30, 26, 255)
                if (abs(x - cx) < 0.6 and abs(abs(y - cy) - 3.0) < 0.6) or (abs(y - cy) < 0.6 and abs(abs(x - cx) - 3.0) < 0.6):
                    return (110, 70, 40, 255)
                return shade((236, 224, 196), x, y, 4)
        if x == 0 or x == w - 1 or y == 0 or y == h - 1:
            return shade(darker(COPPER, 0.78), x, y)
        if (x % 5 == 2 and y in (1, h - 2)) or (y % 4 == 2 and x in (1, w - 2)):
            return shade(GOLD, x, y)
        return copper(x, y, 2)
    s.box(0, 40, 18, 12, 11, body)

    def waist(face, x, y, w, h):
        if (x + y) % 3 == 0:
            return shade(DARK, x, y)
        return shade(darker(COPPER, 0.85), x, y)
    s.box(0, 70, 9, 5, 6, waist)

    def limb(face, x, y, w, h, seed, hand):
        if face in ('top', 'bottom'):
            return shade(DARK if face == 'bottom' else darker(COPPER, 0.85), x, y)
        if hand and y >= h - 5:
            # Hands: dark iron fingers.
            return shade((70, 66, 64) if x % 2 == 0 else (52, 48, 46), x, y)
        if y % 6 == 0:
            return shade(DARK, x, y)
        if y % 6 == 1 and x % 3 == 1:
            return shade(GOLD, x, y)
        return copper(x, y, seed)
    s.box(60, 21, 4, 30, 6, lambda f, x, y, w, h: limb(f, x, y, w, h, 3, True))
    s.box(60, 58, 4, 30, 6, lambda f, x, y, w, h: limb(f, x, y, w, h, 4, True))
    s.box(37, 0, 6, 16, 5, lambda f, x, y, w, h: limb(f, x, y, w, h, 5, False))
    s.box(60, 0, 6, 16, 5, lambda f, x, y, w, h: limb(f, x, y, w, h, 6, False))

    eyes = Canvas(128, 128)

    def glow(face, x, y, w, h):
        if face == 'front' and y == 4 and x in (2, 5):
            return (255, 210, 110, 255)
        if face == 'front' and y == 4 and x in (1, 3, 4, 6):
            return (200, 120, 40, 140)
        return None
    eyes.box(0, 0, 8, 10, 8, glow)

    def dial(face, x, y, w, h):
        # The clock's gold rim glows faintly too.
        if face != 'front':
            return None
        cx, cy = (w - 1) / 2.0, (h - 1) / 2.0 - 0.5
        r = math.hypot(x - cx, (y - cy) * 1.1)
        if 3.7 < r <= 4.6:
            return (255, 220, 120, 110)
        return None
    eyes.box(0, 40, 18, 12, 11, dial)
    return s, eyes


# ---- The Gnomish Repeater ----

WOOD = (110, 70, 40)
WOOD_DARK = (70, 44, 26)
STRING = (230, 230, 230)
IRON = (160, 160, 168)


def line(c, x0, y0, x1, y1, color):
    n = max(abs(x1 - x0), abs(y1 - y0))
    for k in range(n + 1):
        t = k / n if n else 0
        c.put(round(x0 + (x1 - x0) * t), round(y0 + (y1 - y0) * t), color)


def repeater(state):
    """state: 'standby', 'pulling_0..2', 'arrow' or 'firework'. Drawn bottom-left (butt) to top-right (bow), then
    mirrored: the vanilla crossbow's item model (the parent, with its hand transforms) expects the bow at the top
    left and the butt at the bottom right, so drawn the other way round it's held sideways across the body."""
    c = Canvas(16, 16)
    # The stock, diagonal, with a darker outline.
    line(c, 2, 13, 11, 4, WOOD_DARK)
    line(c, 3, 13, 12, 4, WOOD)
    line(c, 2, 12, 11, 3, WOOD)
    c.put(2, 14, WOOD_DARK)
    c.put(1, 13, WOOD_DARK)
    # The drum magazine under the stock.
    for (x, y) in ((6, 10), (7, 10), (6, 11), (7, 11), (8, 9)):
        c.put(x, y, shade(BRASS, x, y))
    c.put(7, 11, darker(BRASS, 0.7))
    # The brass limbs across the head of the stock (the bow, perpendicular to it), swept back further the
    # further it's drawn.
    pull = {'standby': 0, 'pulling_0': 1, 'pulling_1': 2, 'pulling_2': 3, 'arrow': 3, 'firework': 3}[state]
    sweep = (pull + 1) // 2
    tip_a = (7 - sweep, 0 + sweep)
    tip_b = (15 - sweep, 8 + sweep)
    # The string, from tip to tip through the nock point, pulled back along the stock.
    nock = (11 - pull, 4 + pull)
    if pull == 0:
        line(c, tip_a[0], tip_a[1] + 1, tip_b[0] - 1, tip_b[1], STRING)
    else:
        line(c, tip_a[0] + 1, tip_a[1] + 1, nock[0], nock[1], STRING)
        line(c, tip_b[0] - 1, tip_b[1] - 1, nock[0], nock[1], STRING)
    if state == 'arrow':
        line(c, nock[0], nock[1], 13, 2, (200, 200, 200))
        c.put(13, 2, IRON)
        c.put(14, 1, IRON)
    elif state == 'firework':
        line(c, nock[0], nock[1], 12, 3, (200, 40, 40))
        c.put(13, 2, (240, 220, 80))
        c.put(13, 1, (240, 120, 40))
    # The limbs go over the string.
    line(c, 11, 4, tip_a[0], tip_a[1], BRASS)
    line(c, 11, 4, tip_b[0], tip_b[1], BRASS)
    line(c, 10, 4, tip_a[0], tip_a[1] + 1, darker(BRASS, 0.8))
    c.put(tip_a[0], tip_a[1], darker(BRASS, 0.6))
    c.put(tip_b[0], tip_b[1], darker(BRASS, 0.6))
    c.px = [row[::-1] for row in c.px]
    return c


# ---- Tinker's Goggles ----

def goggles_icon():
    c = Canvas(16, 16)
    strap = (70, 46, 30)
    line(c, 1, 6, 3, 8, strap)
    line(c, 14, 6, 12, 8, strap)
    for cx in (5, 10):
        for y in range(6, 11):
            for x in range(cx - 2, cx + 3):
                d = math.hypot(x - cx, y - 8)
                if d <= 1.2:
                    c.put(x, y, (170, 236, 224) if (x + y) % 3 else (230, 255, 250))
                elif d <= 2.4:
                    c.put(x, y, shade(BRASS if d < 2.0 else darker(BRASS, 0.75), x, y))
    c.put(7, 8, darker(BRASS, 0.8))
    c.put(8, 8, darker(BRASS, 0.8))
    return c


def goggles_layer():
    """The worn goggles: the vanilla armour layout's head box (uv 0,0, 8x8x8), lenses over the eyes."""
    c = Canvas(64, 32)

    def head(face, x, y, w, h):
        if face == 'front' and 2 <= y <= 4:
            if x in (3, 4):
                return shade(darker(BRASS, 0.8), x, y) if y == 3 else None
            lens_x = x in (1, 2, 5, 6)
            if lens_x and y == 3:
                return (170, 236, 224, 255) if x in (2, 5) else (130, 200, 190, 255)
            if 0 <= x <= 7:
                return shade(BRASS, x, y)
        if face in ('left', 'right', 'back') and y == 3:
            return shade((70, 46, 30), x, y)
        return None
    c.box(0, 0, 8, 8, 8, head)
    return c


def main():
    for i in range(5):
        gnome(i).save(os.path.join(TEX, 'entity/gnome/gnome_%d.png' % i))
    body, eyes = sentinel()
    body.save(os.path.join(TEX, 'entity/clockwork_sentinel/sentinel.png'))
    eyes.save(os.path.join(TEX, 'entity/clockwork_sentinel/sentinel_eyes.png'))
    for state in ('standby', 'pulling_0', 'pulling_1', 'pulling_2', 'arrow', 'firework'):
        name = 'gnomish_repeater' if state == 'standby' else 'gnomish_repeater_' + state
        repeater(state).save(os.path.join(TEX, 'item/%s.png' % name))
    goggles_icon().save(os.path.join(TEX, 'item/tinkers_goggles.png'))
    goggles_layer().save(os.path.join(TEX, 'models/armor/tinkers_goggles/body_middle.png'))
    print('wrote Cogsdeep textures under', TEX)


if __name__ == '__main__':
    main()
