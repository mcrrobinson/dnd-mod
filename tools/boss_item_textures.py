"""Generates the textures of the boss and monster magic items (design 5, section 3.7, items 10-18 and 20).

    python3 tools/boss_item_textures.py

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
    'warlords_warhorn': ({
        'o': (40, 26, 16, 255), 'h': (214, 196, 160, 255), 'H': (240, 228, 200, 255),
        'b': (150, 120, 84, 255), 'g': (200, 150, 50, 255), 'r': (130, 40, 30, 255),
    }, [
        '................',
        '................',
        '..........ooo...',
        '.........ohHHo..',
        '........ohHhhbo.',
        '.......ohHhhbbo.',
        '......oghhhbbo..',
        '.....ogghhbbo...',
        '....ohhhhbbo....',
        '...ohhhbbbo.....',
        '..ohhbbbbo......',
        '.orrbbboo.......',
        '.orrboo.........',
        '..ooo...........',
        '................',
        '................',
    ]),
    'heart_of_the_furnace': ({
        'o': (40, 14, 10, 255), 'c': (90, 70, 60, 255), 'r': (200, 50, 20, 255),
        'R': (255, 120, 30, 255), 'y': (255, 220, 90, 255), 'k': (60, 40, 30, 255),
    }, [
        '......c..c......',
        '.....c....c.....',
        '....c......c....',
        '...c........c...',
        '...c........c...',
        '..oooo....oooo..',
        '.orrrro..orrrro.',
        'orRRRrroorRRRrro',
        'orRyyRrrrrRyRrro',
        'orRyyyRrrRyyRrro',
        '.orRyyyRRyyRrro.',
        '..orRyyyyyRrro..',
        '...orRyyyRrro...',
        '....orRRRrro....',
        '.....orrrro.....',
        '......oooo......',
    ]),
    'storm_chaser_boots': ({
        'o': (20, 30, 50, 255), 'b': (46, 58, 92, 255), 'B': (78, 96, 140, 255),
        'y': (242, 232, 90, 255), 'd': (100, 220, 230, 255),
    }, [
        '................',
        '................',
        '................',
        '..oooo....oooo..',
        '..oBbo....oBbo..',
        '..oyyo....oyyo..',
        '..oBbo....oBbo..',
        '..oByo....oByo..',
        '..obyo....obyo..',
        '..oBbo....oBbo..',
        '..oBbo....oBbo..',
        '.oBbbo...oBbbo..',
        'oBbbbo..oBbbbo..',
        'oddddo..oddddo..',
        'oooooo..oooooo..',
        '................',
    ]),
    'wyvern_scale_cloak': ({
        'o': (24, 40, 20, 255), 'g': (62, 107, 46, 255), 'G': (98, 150, 70, 255),
        's': (184, 196, 106, 255), 'c': (200, 160, 60, 255),
    }, [
        '................',
        '.....oooooo.....',
        '....ogccccgo....',
        '...ogGsGGsGgo...',
        '...oGsGGGGsGo...',
        '..ogGGsGGsGGgo..',
        '..oGsGGGGGGsGo..',
        '..oGGsGGGGsGGo..',
        '.ogGsGGsGGsGGgo.',
        '.oGGGsGGGGsGGGo.',
        '.oGsGGGsGsGGsGo.',
        '.ogGGsGGGGGsGgo.',
        'ogGsGGGsGsGGGsgo',
        'oggGGsGGGGsGGggo',
        'ogogggogggogggoo',
        '.o.ooo.ooo.ooo..',
    ]),
    'belt_of_owlbear_might': ({
        'o': (40, 26, 16, 255), 'l': (120, 76, 40, 255), 'L': (156, 104, 60, 255),
        'g': (200, 160, 60, 255), 'G': (240, 210, 110, 255), 'f': (220, 210, 190, 255),
    }, [
        '................',
        '................',
        '................',
        '................',
        '.....f....f.....',
        '....f.f..f.f....',
        'oooooooooooooooo',
        'LLLLLoGGGGoLLLLL',
        'lLlLloGooGolLlLl',
        'lllllogoogolllll',
        'ooooooggggoooooo',
        '................',
        '................',
        '................',
        '................',
        '................',
    ]),
    'mace_of_disruption': ({
        'o': (40, 36, 30, 255), 'w': (110, 66, 40, 255), 'g': (226, 186, 72, 255),
        's': (210, 214, 224, 255), 'S': (250, 250, 255, 255), 'y': (255, 240, 150, 255),
    }, [
        '..........y.....',
        '.........osoy...',
        '........osSso...',
        '.......osSySso..',
        '......ysSyyySsy.',
        '.......osSySso..',
        '........osSso...',
        '.......ogoso....',
        '......owgo.y....',
        '.....owo........',
        '....owo.........',
        '...owo..........',
        '..ogo...........',
        '.owo............',
        '.oo.............',
        '................',
    ]),
    'frostbrand': ({
        'o': (20, 40, 70, 255), 'i': (150, 200, 240, 255), 'I': (230, 245, 255, 255),
        'b': (60, 110, 190, 255), 'h': (70, 50, 40, 255), 'g': (180, 200, 220, 255),
    }, [
        '...............o',
        '..............oI',
        '.............oIi',
        '............oIio',
        '...........oIio.',
        '..........oIio..',
        '.........oIio...',
        '........oIio....',
        '...ob..oIio.....',
        '...obboIio......',
        '....obgio.......',
        '.....ohgo.......',
        '....ohoobbo.....',
        '...oho...oo.....',
        '..obo...........',
        '..oo............',
    ]),
    'phylactery_locket': ({
        'o': (30, 20, 40, 255), 'c': (150, 150, 160, 255), 'g': (90, 60, 120, 255),
        'G': (140, 100, 180, 255), 's': (127, 224, 255, 255), 'S': (220, 250, 255, 255),
    }, [
        '....c......c....',
        '....c......c....',
        '.....c....c.....',
        '.....c....c.....',
        '......c..c......',
        '.......cc.......',
        '......oooo......',
        '.....oGGGGo.....',
        '....oGgssgGo....',
        '...oGgsSSsgGo...',
        '...oGgsSSsgGo...',
        '....oGgssgGo....',
        '.....oGggGo.....',
        '......oGGo......',
        '.......oo.......',
        '................',
    ]),
    'burglars_ring': ({
        'o': (30, 24, 20, 255), 'g': (90, 80, 70, 255), 'G': (140, 130, 120, 255),
        'r': (120, 20, 30, 255), 'R': (200, 50, 70, 255),
    }, [
        '................',
        '................',
        '................',
        '......oooo......',
        '.....orRRro.....',
        '.....oRrrRo.....',
        '......oooo......',
        '.....oGGGGo.....',
        '....oGo..oGo....',
        '...oGo....oGo...',
        '...oGo....ogo...',
        '...oGo....ogo...',
        '....ogo..ogo....',
        '.....oggggo.....',
        '......oooo......',
        '................',
    ]),
    'eye_of_the_tyrant': ({
        'o': (40, 20, 30, 255), 'p': (122, 72, 96, 255), 'P': (170, 110, 140, 255),
        'w': (240, 230, 210, 255), 'e': (90, 200, 80, 255), 'k': (10, 10, 10, 255),
    }, [
        '................',
        '................',
        '.....oooooo.....',
        '...ooPPPPPPoo...',
        '..oPPpppppppPo..',
        '.oPpwwwwwwwwpPo.',
        '.oPwwweeeewwwPo.',
        'oPpwweekkeewwpPo',
        'oPpwweekkeewwpPo',
        '.oPwwweeeewwwPo.',
        '.oPpwwwwwwwwpPo.',
        '..oPPpppppppPo..',
        '...ooPPPPPPoo...',
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
