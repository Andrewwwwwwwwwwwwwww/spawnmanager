#!/usr/bin/env python3
"""Draws the Spawn Manager icon: a compass, the item that points at world spawn, with the
name under it in the vanilla Minecraft bitmap font. Pure Python, no dependencies; writes
the 512/400/128 sizes.

The dial is generated on a 16x16 grid - the size of a real item sprite - so the circle is
a proper pixel stair rather than a smooth curve, and every size is a whole-number scale of
the same art.

Run from the repo root:  python branding/icon-source.py
"""
import math
import os
import struct
import zlib

BG = (0x26, 0x2B, 0x31)
RING = (0xB4, 0xBC, 0xC6)
RING_LIT = (0xDD, 0xE3, 0xE9)
FACE = (0x39, 0x41, 0x4D)
FACE_SHADE = (0x2C, 0x33, 0x3D)
NEEDLE_N = (0xE0, 0x4B, 0x45)
NEEDLE_S = (0xFF, 0xFF, 0xFF)
PIN = (0x2F, 0x34, 0x3B)
TEXT = (0xE6, 0xE9, 0xED)

GRID = 16
CENTRE = (GRID - 1) / 2.0

# Minecraft's ascii.png, the 7 rows each glyph uses.
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


def dial():
    """The compass as {(x, y): colour} on a 16x16 grid."""
    cells = {}
    for y in range(GRID):
        for x in range(GRID):
            dx, dy = x - CENTRE, y - CENTRE
            d = math.hypot(dx, dy)
            if d > 7.7:
                continue
            if d > 6.1:
                # Light catches the upper left of the rim.
                cells[(x, y)] = RING_LIT if dx + dy < -3 else RING
            else:
                cells[(x, y)] = FACE_SHADE if dx + dy > 3.2 else FACE

    # The needle, drawn by hand: a tapered diamond, north red and south white. Half-width
    # per row from the centre outwards, so the point stays sharp instead of clipping round.
    for step, half in enumerate([2, 2, 1, 1, 1]):
        for side, colour in ((-1, NEEDLE_N), (1, NEEDLE_S)):
            y = int(CENTRE + side * (step + 0.5))
            for x in range(int(CENTRE + 0.5) - half, int(CENTRE + 0.5) + half):
                cells[(x, y)] = colour
    return cells


def draw(size):
    k = size / 512.0
    px = [[BG] * size for _ in range(size)]

    def rect(x, y, w, h, colour):
        for yy in range(max(0, y), min(size, y + h)):
            row = px[yy]
            for xx in range(max(0, x), min(size, x + w)):
                row[xx] = colour

    block = max(1, round(18 * k))
    ox = round(size / 2 - GRID * block / 2)
    oy = round(34 * k)
    for (gx, gy), colour in dial().items():
        rect(ox + gx * block, oy + gy * block, block, block, colour)

    scale = max(1, round(7 * k))
    top = oy + GRID * block + round(22 * k)
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

    png = (b'\x89PNG\r\n\x1a\n'
           + chunk(b'IHDR', struct.pack('>IIBBBBB', size, size, 8, 2, 0, 0, 0))
           + chunk(b'IDAT', zlib.compress(raw, 9))
           + chunk(b'IEND', b''))
    open(path, 'wb').write(png)


if __name__ == '__main__':
    os.makedirs('branding', exist_ok=True)
    os.makedirs('src/main/resources/assets/spawnmanager', exist_ok=True)
    for path, size in [('branding/icon-modrinth-512.png', 512),
                       ('branding/icon-curseforge-400.png', 400),
                       ('src/main/resources/assets/spawnmanager/icon.png', 128)]:
        save(path, draw(size))
        print('wrote %-50s %d x %d' % (path, size, size))
