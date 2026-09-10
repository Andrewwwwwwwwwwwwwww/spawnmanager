"""A small PNG reader and writer, so nothing here needs an imaging library.

Handles bit depth 8, colour types 0/2/3/4/6, and all five scanline filters, which covers
anything a screenshot or a pixel-art export will be.
"""
import struct
import zlib


def read(path):
    """Returns (width, height, rows) with rows as lists of (r, g, b, a) tuples."""
    raw = open(path, 'rb').read()
    assert raw[:8] == b'\x89PNG\r\n\x1a\n', 'not a PNG'
    pos = 8
    idat = bytearray()
    palette = None
    trns = None
    width = height = depth = ctype = None
    while pos < len(raw):
        length, tag = struct.unpack('>I4s', raw[pos:pos + 8])
        data = raw[pos + 8:pos + 8 + length]
        pos += 12 + length
        if tag == b'IHDR':
            width, height, depth, ctype, comp, filt, interlace = struct.unpack('>IIBBBBB', data)
            # Palette images are often packed at 1, 2 or 4 bits per pixel; everything else
            # here is 8-bit channels.
            assert depth in (1, 2, 4, 8), 'unsupported bit depth %d' % depth
            assert depth == 8 or ctype == 3, 'sub-byte depth only supported for palettes'
            assert interlace == 0, 'interlaced PNG not supported'
        elif tag == b'PLTE':
            palette = [tuple(data[i:i + 3]) for i in range(0, len(data), 3)]
        elif tag == b'tRNS':
            trns = data
        elif tag == b'IDAT':
            idat += data
        elif tag == b'IEND':
            break

    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    stride = (width * channels * depth + 7) // 8
    flat = zlib.decompress(bytes(idat))
    out = bytearray(stride * height)
    prev = bytearray(stride)
    src = 0
    for y in range(height):
        f = flat[src]
        src += 1
        line = bytearray(flat[src:src + stride])
        src += stride
        if f == 1:
            for i in range(channels, stride):
                line[i] = (line[i] + line[i - channels]) & 0xFF
        elif f == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 0xFF
        elif f == 3:
            for i in range(stride):
                a = line[i - channels] if i >= channels else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 0xFF
        elif f == 4:
            for i in range(stride):
                a = line[i - channels] if i >= channels else 0
                b = prev[i]
                c = prev[i - channels] if i >= channels else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pred = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pred) & 0xFF
        out[y * stride:(y + 1) * stride] = line
        prev = line

    def index_at(base, x):
        """Palette index for pixel x, unpacking sub-byte depths."""
        if depth == 8:
            return out[base + x]
        per = 8 // depth
        byte = out[base + x // per]
        shift = 8 - depth * (x % per + 1)
        return (byte >> shift) & ((1 << depth) - 1)

    rows = []
    for y in range(height):
        row = []
        base = y * stride
        for x in range(width):
            i = base + x * channels
            if ctype == 3:
                idx = index_at(base, x)
                r, g, b = palette[idx]
                a = trns[idx] if trns is not None and idx < len(trns) else 255
                row.append((r, g, b, a))
            elif ctype == 0:
                v = out[i]
                row.append((v, v, v, 255))
            elif ctype == 2:
                row.append((out[i], out[i + 1], out[i + 2], 255))
            elif ctype == 3:
                idx = out[i]
                r, g, b = palette[idx]
                a = trns[idx] if trns is not None and idx < len(trns) else 255
                row.append((r, g, b, a))
            elif ctype == 4:
                v = out[i]
                row.append((v, v, v, out[i + 1]))
            else:
                row.append((out[i], out[i + 1], out[i + 2], out[i + 3]))
        rows.append(row)
    return width, height, rows


def write(path, rows):
    """Writes RGB rows (tuples of 3 or 4; alpha is dropped)."""
    height = len(rows)
    width = len(rows[0])
    raw = b''.join(b'\x00' + bytes(v for p in row for v in p[:3]) for row in rows)

    def chunk(tag, data):
        return (struct.pack('>I', len(data)) + tag + data
                + struct.pack('>I', zlib.crc32(tag + data) & 0xFFFFFFFF))

    png = (b'\x89PNG\r\n\x1a\n'
           + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0))
           + chunk(b'IDAT', zlib.compress(raw, 9))
           + chunk(b'IEND', b''))
    open(path, 'wb').write(png)
