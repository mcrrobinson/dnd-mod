"""Generates the textures of quest items drawn after the launch chains (design 9, 9.4).

    python3 tools/quest_item_textures.py

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
    # An obsidian medallion with a silver spider and a violet gem for its body: a drow house's seal.
    'drow_house_sigil': ({
        'o': (14, 10, 20, 255), 'b': (36, 26, 48, 255), 'B': (58, 42, 76, 255),
        'h': (92, 70, 116, 255), 's': (196, 200, 214, 255), 'S': (240, 242, 250, 255),
        'v': (150, 70, 220, 255), 'V': (214, 160, 255, 255), 'r': (110, 96, 120, 255),
    }, [
        '................',
        '......rrrr......',
        '....rroooorr....',
        '...roBBBBBBor...',
        '..roBhsBBshBor..',
        '..rBBsBssBsBBr..',
        '.roBsBsSSsBsBor.',
        '.roBBssVvssBBor.',
        '.roBsBsvvsBsBor.',
        '.roBBssvvssBBor.',
        '..rBsBBssBBsBr..',
        '..roBsBBBBsBor..',
        '...roBBhhBBor...',
        '....rroooorr....',
        '......rrrr......',
        '................',
    ]),
}


def render(palette, rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    return [[palette.get(ch, CLEAR) if ch != '.' else CLEAR for ch in row] for row in rows]


def main():
    for name, (palette, rows) in ART.items():
        pngio.write_png(os.path.join(TEX, 'item', name + '.png'), 16, 16, render(palette, rows))


if __name__ == '__main__':
    main()
