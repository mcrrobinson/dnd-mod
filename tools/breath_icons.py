"""Generates the Dragonborn Breath Weapon cooldown icons (9x9, next to the mana bar).

    python3 tools/breath_icons.py

One per ancestry: an ember flame, a frost snowflake and a storm bolt, on a dark rounded tile.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/power')

TILE = (30, 22, 18, 220)
RIM = (70, 56, 44, 235)
CLEAR = (0, 0, 0, 0)

# '.' tile, 'a' main colour, 'b' highlight, 'c' shadow
GLYPHS = {
    'ember': [
        ' ....... ',
        '....a....',
        '...aa....',
        '...aba...',
        '..abba.a.',
        '..abbbaa.',
        '..acbbca.',
        '...cccc..',
        ' ....... ',
    ],
    'frost': [
        ' ....... ',
        '....a....',
        '.b..a..b.',
        '..b.a.b..',
        '.aaabaaa.',
        '..b.a.b..',
        '.b..a..b.',
        '....a....',
        ' ....... ',
    ],
    'storm': [
        ' ....... ',
        '.....aa..',
        '....aa...',
        '...aa....',
        '..abbbb..',
        '....bb...',
        '...bb....',
        '..bc.....',
        ' ....... ',
    ],
}

COLOURS = {
    'ember': {'a': (240, 110, 30, 255), 'b': (255, 220, 90, 255), 'c': (170, 40, 20, 255)},
    'frost': {'a': (150, 210, 255, 255), 'b': (240, 250, 255, 255), 'c': (80, 140, 210, 255)},
    'storm': {'a': (120, 170, 255, 255), 'b': (250, 245, 160, 255), 'c': (90, 90, 220, 255)},
}


def main():
    for name, glyph in GLYPHS.items():
        rows = []
        for y, line in enumerate(glyph):
            row = []
            for x, ch in enumerate(line):
                if ch == ' ':
                    row.append(CLEAR)
                elif ch == '.':
                    edge = x in (0, 8) or y in (0, 8)
                    row.append(RIM if edge else TILE)
                else:
                    row.append(COLOURS[name][ch])
            rows.append(row)
        pngio.write_png(os.path.join(OUT, 'breath_' + name + '.png'), 9, 9, rows)


if __name__ == '__main__':
    main()
