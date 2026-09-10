#!/usr/bin/env python3
"""Draws the Spawn Manager icon: Minecraft's own compass item, the thing that points at
world spawn, over the mod name in the vanilla bitmap font.

The compass is not a redrawing - `compass-source.png` is frame 16 of the real item texture,
`assets/minecraft/textures/item/compass_16.png`, lifted straight out of the 26.2 client jar.
Frame 16 is the one where the needle sits dead north. It is scaled by whole numbers only, so
every size is the same pixels, just bigger.

Pure Python, no dependencies.  Run from the repo root:  python branding/icon-source.py
"""
import os
import struct
import sys
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import png  # noqa: E402  - vendored beside this script

BG = (0xE6, 0xE5, 0xE3)
TEXT = (0x14, 0x17, 0x1C)
SPRITE = 'branding/compass-source.png'

# Minecraft's ascii.png, the 8 rows each glyph uses; row 7 carries the descenders.
GLYPHS = {
    'S': [".####", "#....", "#....", ".###.", "....#", "....#", "####.", "....."],
    'M': ["#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#", "....."],
    'p': [".....", ".....", "####.", "#...#", "#...#", "####.", "#....", "#...."],
    'a': [".....", ".....", ".###.", "....#", ".####", "#...#", ".####", "....."],
    'w': [".....", ".....", "#...#", "#...#", "#.#.#", "##.##", "#...#", "....."],
    'n': [".....", ".....", "#.##.", "##..#", "#...#", "#...#", "#...#", "....."],
    'g': [".....", ".....", ".####", "#...#", "#...#", ".####", "....#", "####."],
    'e': [".....", ".....", ".###.", "#...#", "#####", "#....", ".###.", "....."],
    'r': [".....", ".....", "#.##.", "##..#", "#....", "#....", "#....", "....."],
}
LINES = ["Spawn", "Manager"]


def draw(size, sprite):
    k = size / 512.0
    sw, sh, rows = sprite
    px = [[BG] * size for _ in range(size)]

    def rect(x, y, w, h, colour):
        for yy in range(max(0, y), min(size, y + h)):
            row = px[yy]
            for xx in range(max(0, x), min(size, x + w)):
                row[xx] = colour

    block = max(1, round(18 * k))
    ox = round(size / 2 - sw * block / 2)
    oy = round(36 * k)
    for gy in range(sh):
        for gx in range(sw):
            r, g, b, a = rows[gy][gx]
            if a < 128:
                continue  # the texture's transparent border keeps the ground
            rect(ox + gx * block, oy + gy * block, block, block, (r, g, b))

    scale = max(1, round(7 * k))
    top = oy + sh * block + round(12 * k)
    for line in LINES:
        widths = [len(GLYPHS[ch][0]) + 1 for ch in line]
        pen = round(size / 2 - (sum(widths) - 1) * scale / 2)
        for ch, w in zip(line, widths):
            for yy, row in enumerate(GLYPHS[ch]):
                for xx, bit in enumerate(row):
                    if bit == '#':
                        rect(pen + xx * scale, top + yy * scale, scale, scale, TEXT)
            pen += w * scale
        top += round(10 * scale)
    return px


def save(path, px):
    size = len(px)
    raw = b''.join(b'\x00' + bytes(v for p in row for v in p) for row in px)

    def chunk(tag, data):
        return (struct.pack('>I', len(data)) + tag + data
                + struct.pack('>I', zlib.crc32(tag + data) & 0xFFFFFFFF))

    blob = (b'\x89PNG\r\n\x1a\n'
            + chunk(b'IHDR', struct.pack('>IIBBBBB', size, size, 8, 2, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9))
            + chunk(b'IEND', b''))
    open(path, 'wb').write(blob)


if __name__ == '__main__':
    art = png.read(SPRITE)
    assert art[0] == art[1] == 16, 'expected the 16x16 item texture, got %dx%d' % art[:2]
    os.makedirs('src/main/resources/assets/spawnmanager', exist_ok=True)
    for path, size in [('branding/icon-modrinth-512.png', 512),
                       ('branding/icon-curseforge-400.png', 400),
                       ('src/main/resources/assets/spawnmanager/icon.png', 128)]:
        save(path, draw(size, art))
        print('wrote %-50s %d x %d' % (path, size, size))
