"""Generates the textures of the wondrous and utility magic items (design 5, section 3.7).

    python3 tools/wondrous_item_textures.py

16x16 pixel art drawn from character maps below; the Doss Lute is the Lute recoloured in rosewood
and gold. Writes textures/item/<item>.png and textures/block/immovable_rod.png.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
TEX = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures')

CLEAR = (0, 0, 0, 0)

ART = {
    'wand_of_magic_missiles': ({
        'o': (44, 26, 18, 255), 'w': (110, 66, 40, 255), 'l': (150, 98, 60, 255),
        'g': (226, 186, 72, 255), 'c': (186, 120, 255, 255), 'C': (236, 210, 255, 255),
        's': (150, 90, 240, 160),
    }, [
        '...........s.s..',
        '............C...',
        '..........sCcCs.',
        '...........occ..',
        '..........ogc.s.',
        '.........owgo...',
        '........owlo....',
        '.......owlo.....',
        '......owlo......',
        '.....owlo.......',
        '....ogwo........',
        '...owlo.........',
        '..owlo..........',
        '.owlo...........',
        '.ooo............',
        '................',
    ]),
    'bag_of_holding': ({
        'o': (40, 24, 40, 255), 'b': (92, 52, 110, 255), 'B': (124, 76, 146, 255),
        'h': (160, 110, 180, 255), 'g': (226, 186, 72, 255), 'r': (170, 120, 70, 255),
        's': (30, 12, 50, 255), 'x': (120, 230, 255, 255),
    }, [
        '................',
        '......r..r......',
        '.....r.rr.r.....',
        '......oggo......',
        '......obbo......',
        '.....obBBbo.....',
        '....obBhhBbo....',
        '...obBhBBBBbo...',
        '..obBBBssBBBbo..',
        '..obBBsxxsBBbo..',
        '..obBBsxxsBBbo..',
        '..obBBBssBBBbo..',
        '..obbBBBBBBbbo..',
        '...obbbbbbbbo...',
        '....oooooooo....',
        '................',
    ]),
    'decanter_of_endless_water': ({
        'o': (24, 46, 70, 255), 'g': (120, 170, 200, 200), 'G': (190, 225, 240, 220),
        'w': (40, 110, 210, 230), 'W': (90, 160, 240, 230), 'c': (150, 110, 60, 255),
        's': (200, 230, 255, 255),
    }, [
        '.......s........',
        '......scs.......',
        '.......c........',
        '......occo......',
        '......oGgo......',
        '......oGgo......',
        '.....ogGggo.....',
        '....ogGgggggo...',
        '...ogGWWWWWWgo..',
        '..ogGWWwwwwWWgo.',
        '..ogWWwwwwwwWgo.',
        '..ogWwwwwwwwwgo.',
        '..ogWwwwwwwwwgo.',
        '...ogwwwwwwwgo..',
        '....ooooooooo...',
        '................',
    ]),
    'immovable_rod': ({
        'o': (34, 34, 40, 255), 'i': (120, 124, 134, 255), 'I': (180, 184, 194, 255),
        'd': (82, 86, 96, 255), 'b': (200, 60, 50, 255), 'B': (255, 120, 100, 255),
    }, [
        '................',
        '............oo..',
        '...........oIBo.',
        '..........oIbbo.',
        '.........oIido..',
        '........oIido...',
        '.......oIido....',
        '......oIido.....',
        '.....oIido......',
        '....oIido.......',
        '...oIido........',
        '..oIido.........',
        '.oIddo..........',
        '.oddo...........',
        '..oo............',
        '................',
    ]),
    'periapt_of_wound_closure': ({
        'o': (40, 30, 20, 255), 'c': (150, 150, 160, 255), 'g': (226, 186, 72, 255),
        'G': (150, 110, 40, 255), 'r': (200, 30, 50, 255), 'R': (255, 110, 120, 255),
        'd': (110, 10, 30, 255),
    }, [
        '...cc......cc...',
        '..c..c....c..c..',
        '.c....c..c....c.',
        '.c.....cc.....c.',
        '..c..........c..',
        '...c........c...',
        '....c......c....',
        '.....c....c.....',
        '......cggc......',
        '.....oGggGo.....',
        '....oGRrrrGo....',
        '....ogRrrrgo....',
        '....ogrrrdgo....',
        '.....oGrdGo.....',
        '......oGGo......',
        '.......oo.......',
    ]),
    'ring_of_protection': ({
        'o': (60, 50, 30, 255), 's': (190, 196, 210, 255), 'S': (240, 244, 250, 255),
        'd': (120, 126, 140, 255), 'b': (40, 90, 220, 255), 'B': (130, 180, 255, 255),
    }, [
        '................',
        '................',
        '......oooo......',
        '.....obBbbo.....',
        '.....obbbbo.....',
        '....osobboso....',
        '...oSso..osdo...',
        '..oSso....osdo..',
        '..oSo......odo..',
        '..oso......odo..',
        '..oso......odo..',
        '..osdo....oddo..',
        '...osdo..oddo...',
        '....oddddddo....',
        '.....oooooo.....',
        '................',
    ]),
    'amulet_of_health': ({
        'o': (40, 30, 20, 255), 'c': (226, 186, 72, 255), 'g': (226, 186, 72, 255),
        'G': (150, 110, 40, 255), 'e': (40, 170, 70, 255), 'E': (140, 240, 150, 255),
        'd': (20, 90, 40, 255),
    }, [
        '...cc......cc...',
        '..c..c....c..c..',
        '.c....c..c....c.',
        '.c.....cc.....c.',
        '..c..........c..',
        '...c........c...',
        '....c......c....',
        '.....c....c.....',
        '......cggc......',
        '.....oggggo.....',
        '....ogEEeego....',
        '...ogEeeeedgo...',
        '...ogeeeeddgo...',
        '....ogeeddgo....',
        '.....oggggo.....',
        '......oooo......',
    ]),
    'tome_of_clear_thought': ({
        'o': (24, 20, 40, 255), 'b': (52, 70, 150, 255), 'B': (80, 104, 190, 255),
        'p': (236, 228, 200, 255), 'P': (200, 190, 160, 255), 'g': (226, 186, 72, 255),
        'x': (170, 220, 255, 255),
    }, [
        '................',
        '..oooooooooooo..',
        '.oBBBBBBBBBBBBo.',
        '.oBbbbbbbbbbbBo.',
        '.oBbggggggggbBo.',
        '.oBbgbbbbbbgbBo.',
        '.oBbgbbxxbbgbBo.',
        '.oBbgbxxxxbgbBo.',
        '.oBbgbbxxbbgbBo.',
        '.oBbgbbbbbbgbBo.',
        '.oBbggggggggbBo.',
        '.oBbbbbbbbbbbBo.',
        '.oBBBBBBBBBBBBo.',
        '.oppppppppppppo.',
        '.oPPPPPPPPPPPPo.',
        '..oooooooooooo..',
    ]),
}

# The rod block's texture: brushed iron, with the red button's band near the top.
ROD_BLOCK = ({
    'i': (120, 124, 134, 255), 'I': (170, 174, 184, 255), 'd': (80, 84, 94, 255),
    'b': (190, 50, 40, 255), 'B': (240, 110, 90, 255),
}, ['IIiiiidd' * 2] * 3 + ['BBbbbbbb' * 2] * 2 + ['IIiiiidd' * 2] * 11)

LUTE_RECOLOR = {
    (74, 44, 22, 255): (58, 18, 28, 255),     # outline -> dark rosewood
    (139, 90, 43, 255): (112, 36, 50, 255),   # body -> rosewood
    (184, 128, 64, 255): (168, 66, 80, 255),  # highlight
    (230, 226, 210, 255): (242, 204, 96, 255),  # strings -> gold
    (220, 180, 60, 255): (90, 226, 210, 255),   # bridge -> teal gem
}


def render(palette, rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    return [[palette.get(ch, CLEAR) if ch != '.' else CLEAR for ch in row] for row in rows]


def main():
    for name, (palette, rows) in ART.items():
        pngio.write_png(os.path.join(TEX, 'item', name + '.png'), 16, 16, render(palette, rows))
    pngio.write_png(os.path.join(TEX, 'block', 'immovable_rod.png'), 16, 16, render(*ROD_BLOCK))
    w, h, lute = pngio.read_png(os.path.join(TEX, 'item', 'lute.png'))
    doss = [[LUTE_RECOLOR.get(px, px) for px in row] for row in lute]
    pngio.write_png(os.path.join(TEX, 'item', 'doss_lute.png'), w, h, doss)
    print('wrote', len(ART) + 2, 'textures')


if __name__ == '__main__':
    main()
