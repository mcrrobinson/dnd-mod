"""Generates the Room Key item texture and the Well Rested effect icon (tavern rooms).

    python3 tools/tavern_room_textures.py

Writes textures/item/room_key.png (16x16) and textures/mob_effect/well_rested.png (18x18).
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
TEX = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures')

CLEAR = (0, 0, 0, 0)

ART = {
    # An old brass key on a green tag (the Green Dragon's colour).
    'item/room_key': ({
        'o': (58, 36, 14, 255), 'b': (176, 124, 44, 255), 'B': (222, 172, 70, 255),
        'h': (250, 226, 140, 255), 's': (90, 60, 30, 255), 'g': (46, 110, 52, 255),
        'G': (86, 160, 80, 255), 'w': (210, 196, 160, 255),
    }, [
        '................',
        '..........oooo..',
        '.........oBhBBo.',
        '........oBo..oBo',
        '........obo..obo',
        '........obBoobbo',
        '.......oobbbbbo.',
        '......obo.oooo..',
        '.....obo..s.....',
        '....obo...s.....',
        '...obBo..oggo...',
        '..obbbo.oGGgo...',
        '..oboboogGwgo...',
        '...o.o.ogGGgo...',
        '........oggo....',
        '.........oo.....',
    ]),
    # A crescent moon over a pillow: rested.
    'mob_effect/well_rested': ({
        'o': (40, 40, 70, 255), 'y': (250, 220, 110, 255), 'Y': (255, 244, 190, 255),
        'p': (220, 220, 236, 255), 'P': (180, 180, 210, 255), 'z': (150, 190, 255, 255),
    }, [
        '..................',
        '.....oooo.........',
        '....oYyyo.....zzz.',
        '...oYyoo.......z..',
        '...oyo........zzz.',
        '..oYyo............',
        '..oyyo.....zz.....',
        '..oyyo......z.....',
        '..oyyyo....zz.....',
        '...oyyyoooo.......',
        '...oyyyyyyyo......',
        '....ooyyyyo.......',
        '......oooo........',
        '..oooooooooooooo..',
        '.oppppppppppppppo.',
        '.opPPPPPPPPPPPPpo.',
        '..oooooooooooooo..',
        '..................',
    ]),
}


def main():
    for name, (palette, rows) in ART.items():
        size = len(rows)
        pixels = [[palette.get(c, CLEAR) for c in row] for row in rows]
        assert all(len(r) == size for r in rows), name
        path = os.path.join(TEX, name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        pngio.write_png(path, size, size, pixels)
        print('wrote', path)


if __name__ == '__main__':
    main()
