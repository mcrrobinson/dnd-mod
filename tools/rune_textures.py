"""Generates the six rune block face textures (src/main/resources/assets/dndclasses/textures/block/rune_<glyph>.png).

Each face is dark carved stone with the glyph's 3x3 mural pattern (see RuneBlock.Glyph) scaled up to 9x9
in the glyph's colour, so the runes match the terracotta murals. Run: python3 tools/rune_textures.py
"""
import os
import random
import struct
import sys
import zlib

GLYPHS = [
    ("sun", (240, 160, 48), [".#.", "###", ".#."]),
    ("moon", (143, 184, 232), ["##.", "#..", "##."]),
    ("eye", (127, 208, 64), [".#.", "#.#", ".#."]),
    ("flame", (224, 74, 42), [".#.", "##.", "###"]),
    ("crown", (242, 210, 58), ["#.#", "###", "###"]),
    ("skull", (238, 232, 216), ["###", "#.#", ".#."]),
]


def png(path, pixels):
    raw = b"".join(b"\x00" + bytes(c for px in row for c in px) for row in pixels)
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0)))
        f.write(chunk(b"IDAT", zlib.compress(raw, 9)))
        f.write(chunk(b"IEND", b""))


def face(colour, pattern, seed):
    rng = random.Random(seed)
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            base = 58 if edge else 38 + rng.randint(-5, 5)
            row.append((base, base, base + 6, 255))
        pixels.append(row)
    # The glyph: each pattern cell is 3x3 pixels, centred (offset 3/4), with a dark carved rim.
    for r in range(3):
        for c in range(3):
            if pattern[r][c] != "#":
                continue
            for dy in range(3):
                for dx in range(3):
                    x, y = 3 + c * 3 + dx, 3 + r * 3 + dy
                    shade = 1.0 if (dx, dy) != (2, 2) else 0.8
                    pixels[y][x] = tuple(int(v * shade) for v in colour) + (255,)
    return pixels


def main():
    root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets",
                        "dndclasses", "textures", "block")
    for i, (name, colour, pattern) in enumerate(GLYPHS):
        png(os.path.join(root, "rune_" + name + ".png"), face(colour, pattern, i))
    print("wrote", len(GLYPHS), "textures to", os.path.normpath(root), file=sys.stderr)


if __name__ == "__main__":
    main()
