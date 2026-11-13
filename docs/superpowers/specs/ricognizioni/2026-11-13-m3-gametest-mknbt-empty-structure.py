import gzip, struct, sys
# Minimal NBT writer: empty 8x8x8 structure template filled with air (same shape as Fabric's empty.snbt).
def s(x):
    b = x.encode()
    return struct.pack('>H', len(b)) + b
def tag(t, name, payload):
    return bytes([t]) + s(name) + payload
def i(v):
    return struct.pack('>i', v)
def lst(t, items):
    return bytes([t]) + i(len(items)) + b''.join(items)
N = 8
air_palette = tag(8, 'Name', s('minecraft:air')) + b'\x00'
blocks = []
for x in range(N):
    for y in range(N):
        for z in range(N):
            blocks.append(tag(9, 'pos', lst(3, [i(x), i(y), i(z)])) + tag(3, 'state', i(0)) + b'\x00')
root = (tag(3, 'DataVersion', i(3465))
        + tag(9, 'size', lst(3, [i(N), i(N), i(N)]))
        + tag(9, 'palette', lst(10, [air_palette]))
        + tag(9, 'blocks', lst(10, blocks))
        + tag(9, 'entities', lst(10, []))
        + b'\x00')
data = bytes([10]) + s('') + root
for out in sys.argv[1:]:
    with gzip.GzipFile(out, 'wb', mtime=0) as f:
        f.write(data)
