"""Generates the feat icons (textures/gui/feats.png): 9 cells of 16x16, in Feat enum order.

    python3 tools/feat_icons.py

Each is a glyph on a dark rounded tile with a gold rim: Great Weapon Master (greataxe), Sharpshooter (target and
arrow), Lucky (four-leaf clover), Tough (heart), Mobile (winged boot), War Caster (staff with a spark), Alert (eye),
Sentinel (tower shield and spear), Resilient (shield with a plus). The picker draws them at 16x16, the Character
tab at 8x8.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/gui/feats.png')

TILE = (30, 26, 40, 235)
RIM = (176, 138, 64, 245)
CLEAR = (0, 0, 0, 0)

# 12x12 glyphs placed at (2, 2) in the 16x16 cell. ' ' leaves the tile; letters pick a colour.
GLYPHS = [
    ('great_weapon_master', [
        '   aaaa     ',
        '  aaabba    ',
        ' aaaabbba   ',
        ' aaaabbb    ',
        '  aaabhh    ',
        '   aa hh    ',
        '      hh    ',
        '     hh     ',
        '     hh     ',
        '    hh      ',
        '    hh      ',
        '   hh       ',
    ], {'a': (200, 205, 215, 255), 'b': (140, 145, 160, 255), 'h': (130, 85, 45, 255)}),
    ('sharpshooter', [
        '    rrrr    ',
        '  rrwwwwrr  ',
        ' rwwrrrrwwr ',
        ' rwrwwwwrwr ',
        'rwrwwrrwwrwr',
        'rwrwrrrrwrwr',
        'rwrwrrrrwrwr',
        'rwrwwrrwwrwr',
        ' rwrwwwwrwr ',
        ' rwwrrrrwwr ',
        '  rrwwwwrr  ',
        '    rrrr    ',
    ], {'r': (210, 60, 50, 255), 'w': (240, 235, 220, 255)}),
    ('lucky', [
        '            ',
        '  gg   gg   ',
        ' gggg gggg  ',
        ' ggggggggg  ',
        '  ggg ggg   ',
        '    gsg     ',
        '  ggg ggg   ',
        ' ggggggggg  ',
        ' gggg gggg  ',
        '  gg  sgg   ',
        '       s    ',
        '        s   ',
    ], {'g': (80, 190, 80, 255), 's': (60, 120, 50, 255)}),
    ('tough', [
        '            ',
        '  rr    rr  ',
        ' rrrr  rrrr ',
        'rrwrrrrrrrrr',
        'rwrrrrrrrrrr',
        'rrrrrrrrrrrr',
        ' rrrrrrrrrr ',
        '  rrrrrrrr  ',
        '   rrrrrr   ',
        '    rrrr    ',
        '     rr     ',
        '            ',
    ], {'r': (220, 50, 60, 255), 'w': (255, 200, 200, 255)}),
    ('mobile', [
        '        ww  ',
        '       www  ',
        '  bbb www   ',
        '  bbbww w   ',
        '  bbb  w    ',
        '  bbb       ',
        '  bbb       ',
        '  bbbb      ',
        '  bbbbbbb   ',
        '  bbbbbbbb  ',
        '  ssssssss  ',
        '            ',
    ], {'b': (150, 100, 60, 255), 's': (80, 55, 35, 255), 'w': (235, 240, 255, 255)}),
    ('war_caster', [
        '        y   ',
        '      y y y ',
        '       yyy  ',
        '      yyyyy ',
        '       yyy  ',
        '      p y y ',
        '     pp     ',
        '    pp      ',
        '   pp       ',
        '  pp        ',
        ' pp         ',
        'pp          ',
    ], {'p': (140, 95, 60, 255), 'y': (180, 140, 255, 255)}),
    ('alert', [
        '            ',
        '            ',
        '    wwww    ',
        '  wwwwwwww  ',
        ' wwwbbbbwww ',
        'wwwbbkkbbwww',
        'wwwbbkkbbwww',
        ' wwwbbbbwww ',
        '  wwwwwwww  ',
        '    wwww    ',
        '            ',
        '            ',
    ], {'w': (240, 240, 230, 255), 'b': (230, 180, 60, 255), 'k': (20, 20, 20, 255)}),
    ('sentinel', [
        '         s  ',
        ' aaaaaa ss  ',
        ' abbbba s   ',
        ' abbbba s   ',
        ' abbbba s   ',
        ' abbbba h   ',
        ' abbbba h   ',
        ' abbbba h   ',
        '  abba  h   ',
        '  abba  h   ',
        '   aa   h   ',
        '        h   ',
    ], {'a': (190, 195, 205, 255), 'b': (60, 90, 170, 255), 's': (220, 225, 235, 255), 'h': (130, 85, 45, 255)}),
    ('resilient', [
        ' aaaaaaaaaa ',
        ' abbbbbbbba ',
        ' abbbggbbba ',
        ' abbbggbbba ',
        ' abggggggba ',
        ' abggggggba ',
        ' abbbggbbba ',
        '  abbggbba  ',
        '  abbbbbba  ',
        '   abbbba   ',
        '    abba    ',
        '     aa     ',
    ], {'a': (200, 170, 90, 255), 'b': (120, 60, 60, 255), 'g': (240, 220, 120, 255)}),
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
