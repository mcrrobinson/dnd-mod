"""Draws the 16x16 speech bubble particle used by /dm say (no dependencies).

Run: python3 tools/speech_bubble_texture.py
"""
import struct
import zlib
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/dndclasses/textures/particle/speech_bubble.png"

# . transparent, # outline, w fill, o dots
ART = [
    "................",
    "..############..",
    ".#wwwwwwwwwwww#.",
    "#wwwwwwwwwwwwww#",
    "#wwwwwwwwwwwwww#",
    "#wwwoowwoowwoow#",
    "#wwwoowwoowwoow#",
    "#wwwwwwwwwwwwww#",
    "#wwwwwwwwwwwwww#",
    ".#wwwwwwwwwwww#.",
    "..###ww#######..",
    "....#ww#........",
    "....#w#.........",
    "....##..........",
    "................",
    "................",
]
COLORS = {
    ".": (0, 0, 0, 0),
    "#": (40, 32, 24, 255),
    "w": (250, 246, 232, 255),
    "o": (70, 60, 50, 255),
}


def png(rows):
    raw = b"".join(b"\x00" + b"".join(bytes(COLORS[c]) for c in row) for row in rows)
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
    header = struct.pack(">IIBBBBB", len(rows[0]), len(rows), 8, 6, 0, 0, 0)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")


if __name__ == "__main__":
    OUT.write_bytes(png(ART))
    print("wrote", OUT)
