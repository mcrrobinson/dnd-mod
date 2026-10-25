"""Generates the condition HUD icons (textures/gui/conditions.png): 10 cells of 16x16, in Condition order.

    python3 tools/condition_icons.py

Each is a glyph on a dark rounded tile: Prone, Frightened, Restrained, Blinded, Stunned, Petrified, Poisoned,
Charmed, Incapacitated, Paralyzed. The HUD draws them at 12x12 above the save lane.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/gui/conditions.png')

TILE = (28, 24, 22, 230)
RIM = (82, 72, 64, 240)
CLEAR = (0, 0, 0, 0)

# 12x12 glyphs placed at (2, 2) in the 16x16 cell. ' ' leaves the tile; letters pick a colour.
GLYPHS = [
    ('prone', [
        '            ',
        '            ',
        '            ',
        '            ',
        '            ',
        ' hh         ',
        'hhhh  a     ',
        'hhhbbbbbbbb ',
        ' hh bbbbbbbb',
        '      a   a ',
        'cccccccccccc',
        '            ',
    ], {'h': (240, 200, 160, 255), 'b': (90, 150, 220, 255), 'a': (70, 110, 180, 255), 'c': (120, 100, 80, 255)}),
    ('frightened', [
        '   aaaaaa   ',
        '  aaaaaaaa  ',
        ' aaaaaaaaaa ',
        ' aabbaabbaa ',
        ' aabbaabbaa ',
        ' aaaaaaaaaa ',
        ' aaaaccaaaa ',
        ' aaaccccaaa ',
        ' aaaccccaaa ',
        '  aaaccaaa  ',
        '   aaaaaa   ',
        '            ',
    ], {'a': (245, 232, 150, 255), 'b': (40, 30, 20, 255), 'c': (90, 40, 30, 255)}),
    ('restrained', [
        '  aa    aa  ',
        ' a  a  a  a ',
        ' a  a  a  a ',
        '  aabbbbaa  ',
        '    bccb    ',
        '    bccb    ',
        '    bccb    ',
        '    bccb    ',
        '  aabbbbaa  ',
        ' a  a  a  a ',
        ' a  a  a  a ',
        '  aa    aa  ',
    ], {'a': (190, 190, 200, 255), 'b': (150, 110, 70, 255), 'c': (200, 160, 100, 255)}),
    ('blinded', [
        '          r ',
        '         r  ',
        '   aaaaar   ',
        '  a    r a  ',
        ' a   bbr  a ',
        'a   bbr b  a',
        'a   brbbb  a',
        ' a  rbbb  a ',
        '  ar     a  ',
        '  raaaaaa   ',
        ' r          ',
        'r           ',
    ], {'a': (230, 230, 230, 255), 'b': (110, 160, 230, 255), 'r': (220, 60, 50, 255)}),
    ('stunned', [
        '   a        ',
        '   a        ',
        ' aaaaa   b  ',
        '  aaa    b  ',
        '  a a  bbbbb',
        '        bbb ',
        '    c   b b ',
        '   ccc      ',
        '    c       ',
        ' ccccccccc  ',
        'cc       cc ',
        ' ccccccccc  ',
    ], {'a': (255, 230, 90, 255), 'b': (255, 250, 200, 255), 'c': (200, 200, 120, 255)}),
    ('petrified', [
        '   aaaaaa   ',
        '  abbbbbba  ',
        ' abbcbbbbba ',
        ' abccbbbbba ',
        ' abbbbbcbba ',
        ' abbbbccbba ',
        ' abbbbbbbba ',
        ' abcbbbbbba ',
        ' abccbbbcba ',
        ' abbbbbbbba ',
        '  aaaaaaaa  ',
        '            ',
    ], {'a': (90, 90, 95, 255), 'b': (160, 160, 165, 255), 'c': (110, 110, 115, 255)}),
    ('poisoned', [
        '     a      ',
        '     a      ',
        '    aaa     ',
        '    aaa     ',
        '   aabaa    ',
        '  aabbaaa   ',
        '  abbaaaa   ',
        ' aabaaaaaa  ',
        ' aaaaaaaca  ',
        ' aaaaaacca  ',
        '  aaaaaca   ',
        '   aaaaa    ',
    ], {'a': (100, 200, 60, 255), 'b': (200, 250, 160, 255), 'c': (50, 120, 30, 255)}),
    ('charmed', [
        '            ',
        '  aaa  aaa  ',
        ' aabaaaaaaa ',
        ' abbaaaaaaa ',
        ' aaaaaaaaaa ',
        ' aaaaaaaaca ',
        '  aaaaaaca  ',
        '   aaaaca   ',
        '    aaaa    ',
        '     aa     ',
        '            ',
        '            ',
    ], {'a': (240, 110, 170, 255), 'b': (255, 210, 230, 255), 'c': (170, 50, 110, 255)}),
    ('incapacitated', [
        '   aaaaaa   ',
        '  aa    aa  ',
        ' aa      aa ',
        ' a       aa ',
        'a       a  a',
        'a      a   a',
        'a     a    a',
        'a    a     a',
        ' a  a     a ',
        ' aaa     aa ',
        '  aa    aa  ',
        '   aaaaaa   ',
    ], {'a': (230, 70, 60, 255)}),
    ('paralyzed', [
        '       aa   ',
        '      aa    ',
        '     aa     ',
        '    aa      ',
        '   aabbbb   ',
        '   bbbbaa   ',
        '      aa    ',
        '     aa     ',
        '    aa      ',
        '   aa       ',
        '  a         ',
        '            ',
    ], {'a': (120, 190, 255, 255), 'b': (220, 240, 255, 255)}),
]

CELL = 16


def main():
    width = CELL * len(GLYPHS)
    rows = [[CLEAR] * width for _ in range(CELL)]
    for i, (name, glyph, colours) in enumerate(GLYPHS):
        assert len(glyph) == 12 and all(len(r) == 12 for r in glyph), name
        ox = i * CELL
        for y in range(CELL):
            for x in range(CELL):
                corner = (x in (0, CELL - 1)) and (y in (0, CELL - 1))
                if corner:
                    continue
                edge = x in (0, CELL - 1) or y in (0, CELL - 1)
                rows[y][ox + x] = RIM if edge else TILE
        for y, line in enumerate(glyph):
            for x, ch in enumerate(line):
                if ch != ' ':
                    rows[2 + y][ox + 2 + x] = colours[ch]
    pngio.write_png(OUT, width, CELL, rows)
    print('wrote', OUT)


if __name__ == '__main__':
    main()
