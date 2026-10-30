"""Generates the textures of the signature magic items (design 10, section 8): the Vorpal Sword and the Sending Stone.

    python3 tools/signature_item_textures.py

16x16 pixel art drawn from character maps below. Writes textures/item/<item>.png.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
TEX = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures')

CLEAR = (0, 0, 0, 0)

ART = {
    # A long silver-blue blade with a faint ripple down its edge, a dark netherite hilt and a gold guard
    'vorpal_sword': ({
        'o': (24, 28, 44, 255), 'e': (236, 244, 255, 255), 's': (176, 196, 224, 255),
        'b': (112, 140, 200, 255), 'r': (150, 120, 230, 255), 'g': (226, 186, 72, 255),
        'G': (150, 110, 40, 255), 'h': (54, 46, 52, 255), 'H': (90, 78, 86, 255), 'p': (120, 60, 160, 255),
    }, [
        '..............oo',
        '.............oeo',
        '............oesb',
        '...........oesro',
        '..........oesbo.',
        '.........oerbo..',
        '........oesbo...',
        '.......oesro....',
        '..og..oesbo.....',
        '..ogGoerbo......',
        '...oggsbo.......',
        '....oHgo........',
        '...oHhoGgo......',
        '..oHho..oo......',
        '.oppo...........',
        '.oo.............',
    ]),
    # A smooth river stone with a glowing aqua rune; its twin bears the same mark
    'sending_stone': ({
        'o': (36, 40, 48, 255), 'd': (88, 96, 108, 255), 's': (128, 138, 150, 255),
        'l': (170, 180, 190, 255), 'a': (90, 220, 240, 255), 'A': (200, 250, 255, 255),
    }, [
        '................',
        '................',
        '................',
        '.....oooooo.....',
        '...oolllllsoo...',
        '..olllssssssdo..',
        '.olllsaAAasssdo.',
        '.olssssAasssddo.',
        '.olssaAAAAassdo.',
        '.osssssAasssddo.',
        '.osssaAAAasdddo.',
        '..odsssssssddo..',
        '...oodddddddo...',
        '.....oooooo.....',
        '................',
        '................',
    ]),
}


def render(palette, rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    return [[palette.get(ch, CLEAR) if ch != '.' else CLEAR for ch in row] for row in rows]


def main():
    for name, (palette, rows) in ART.items():
        pngio.write_png(os.path.join(TEX, 'item', name + '.png'), 16, 16, render(palette, rows))
    print('wrote', len(ART), 'textures')


if __name__ == '__main__':
    main()
