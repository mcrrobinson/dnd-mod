"""Generates the textures of the Portable Hole and the Deck of Many Things (design 10, ticket I2).

    python3 tools/planar_item_textures.py

16x16 pixel art drawn from character maps below. Writes textures/item/portable_hole.png,
textures/item/deck_of_many_things.png and textures/block/portable_hole_passage.png (a see-through starry black,
drawn on the translucent layer).
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
TEX = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures')

CLEAR = (0, 0, 0, 0)

ART = {
    # A folded black cloth: a dark square with a purple hem and the stars of the pocket showing through
    'portable_hole': ({
        'o': (14, 8, 22, 255), 'b': (22, 16, 34, 255), 'B': (34, 26, 50, 255), 'h': (88, 54, 128, 255),
        'H': (130, 90, 180, 255), 's': (230, 226, 255, 255), 'S': (150, 140, 230, 255), 'k': (4, 2, 8, 255),
    }, [
        '................',
        '................',
        '...oooooooooo...',
        '..ohHhhhhhhHho..',
        '..ohbbbbbbbbho..',
        '..ohbkkkSkkbho..',
        '..ohbkskkkkbho..',
        '..ohbkkkkskbho..',
        '..ohbkSkkkkbho..',
        '..ohbkkkskkbho..',
        '..ohbBBBBBBbho..',
        '..ohhhhhhhhhho..',
        '..oBBBBBBBBBBo..',
        '...oooooooooo...',
        '................',
        '................',
    ]),
    # A fanned deck: three cards with gold edges, the front one showing a sun
    'deck_of_many_things': ({
        'o': (40, 22, 14, 255), 'g': (226, 186, 72, 255), 'G': (250, 226, 130, 255), 'p': (120, 40, 140, 255),
        'P': (160, 70, 190, 255), 'w': (240, 232, 210, 255), 'r': (200, 60, 50, 255), 'y': (255, 210, 60, 255),
    }, [
        '................',
        '.....ooooo......',
        '....ogPPPgo.....',
        '...ogPpppPgooo..',
        '..ogPpppPgoggGo.',
        '..ogPppoooowwgo.',
        '..ogPpogGGGGowgo',
        '..ogPpogwwwwgowo',
        '..ogPpogwyywgowo',
        '..ogPpogwyrywgoo',
        '..ogPpogwyywgo..',
        '..ogPpogwwwwgo..',
        '...ogPogwwwwgo..',
        '....ogogGGGGgo..',
        '.....ooooooooo..',
        '................',
    ]),
}


def render(palette, rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    return [[palette.get(ch, CLEAR) if ch != '.' else CLEAR for ch in row] for row in rows]


def passage():
    """Black with a little see-through, scattered with faint stars (fixed seed, so it's reproducible)."""
    rng = random.Random(10)
    rows = [[(6, 4, 12, 215) for _ in range(16)] for _ in range(16)]
    for _ in range(14):
        x, y = rng.randrange(16), rng.randrange(16)
        bright = rng.random() < 0.35
        rows[y][x] = (235, 230, 255, 255) if bright else (120, 100, 200, 235)
    return rows


def main():
    for name, (palette, rows) in ART.items():
        pngio.write_png(os.path.join(TEX, 'item', name + '.png'), 16, 16, render(palette, rows))
    pngio.write_png(os.path.join(TEX, 'block', 'portable_hole_passage.png'), 16, 16, passage())
    print('wrote', len(ART) + 1, 'textures')


if __name__ == '__main__':
    main()
