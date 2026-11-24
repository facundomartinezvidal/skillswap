"""Ancho de texto en px con las métricas reales de Liberation Sans (métricamente igual a Arial)."""
import os
import struct

FUENTES = {False: ['/usr/share/fonts/liberation/LiberationSans-Regular.ttf',  # Linux
                    '/System/Library/Fonts/Supplemental/Arial.ttf'],  # macOS (Arial: mismas métricas)
           True: ['/usr/share/fonts/liberation/LiberationSans-Bold.ttf',
                  '/System/Library/Fonts/Supplemental/Arial Bold.ttf']}
_cache = {}


def _cargar(bold):
    if bold in _cache:
        return _cache[bold]
    ruta = next((f for f in FUENTES[bold] if os.path.exists(f)), None)
    if ruta is None:
        raise FileNotFoundError(f'no está Liberation Sans ni Arial: {FUENTES[bold]}')
    data = open(ruta, 'rb').read()
    n = struct.unpack('>H', data[4:6])[0]
    tablas = {}
    for i in range(n):
        tag, _, off, ln = struct.unpack('>4sIII', data[12 + 16 * i:28 + 16 * i])
        tablas[tag.decode()] = (off, ln)
    upem = struct.unpack('>H', data[tablas['head'][0] + 18:tablas['head'][0] + 20])[0]
    nhm = struct.unpack('>H', data[tablas['hhea'][0] + 34:tablas['hhea'][0] + 36])[0]
    ho = tablas['hmtx'][0]
    avances = [struct.unpack('>H', data[ho + 4 * i:ho + 4 * i + 2])[0] for i in range(nhm)]
    # cmap formato 4 (unicode BMP)
    co = tablas['cmap'][0]
    nt = struct.unpack('>H', data[co + 2:co + 4])[0]
    sub = None
    for i in range(nt):
        pid, eid, off = struct.unpack('>HHI', data[co + 4 + 8 * i:co + 12 + 8 * i])
        if (pid, eid) in ((3, 1), (0, 3)):
            sub = co + off
            break
    segx2 = struct.unpack('>H', data[sub + 6:sub + 8])[0]
    seg = segx2 // 2
    ends = struct.unpack(f'>{seg}H', data[sub + 14:sub + 14 + segx2])
    starts = struct.unpack(f'>{seg}H', data[sub + 16 + segx2:sub + 16 + 2 * segx2])
    deltas = struct.unpack(f'>{seg}h', data[sub + 16 + 2 * segx2:sub + 16 + 3 * segx2])
    roff_pos = sub + 16 + 3 * segx2
    roffs = struct.unpack(f'>{seg}H', data[roff_pos:roff_pos + segx2])
    cmap = {}
    for i in range(seg):
        for c in range(starts[i], ends[i] + 1):
            if c == 0xFFFF:
                continue
            if roffs[i] == 0:
                g = (c + deltas[i]) & 0xFFFF
            else:
                p = roff_pos + 2 * i + roffs[i] + 2 * (c - starts[i])
                g = struct.unpack('>H', data[p:p + 2])[0]
                if g:
                    g = (g + deltas[i]) & 0xFFFF
            cmap[c] = g
    _cache[bold] = (upem, avances, cmap)
    return _cache[bold]


def ancho(texto, size=13, bold=False):
    upem, av, cmap = _cargar(bold)
    total = 0
    for ch in texto:
        g = cmap.get(ord(ch), 0)
        total += av[min(g, len(av) - 1)]
    return total * size / upem


if __name__ == '__main__':
    print(ancho('+publicar(actorId: String): void'), ancho('Publicacion', bold=True))
