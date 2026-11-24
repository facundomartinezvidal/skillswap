"""Exporta el diagrama a SVG con StarUML y verifica que nada quede superpuesto.

Comprueba sobre lo que StarUML dibujó de verdad (no sobre lo que se esperaba):
  1. etiquetas de relaciones que se pisan entre sí
  2. etiquetas sobre cajas de clases, notas o títulos de paquetes
  3. cajas de clases que se pisan entre sí o que se salen de su paquete
  4. texto que se sale de su caja
  5. líneas de relaciones que atraviesan cajas de clases (no las de sus extremos)
  6. etiquetas atravesadas por líneas de otras relaciones
  7. paquetes hermanos que se pisan
Uso: python3 verificar.py <archivo.mdj> [salida.svg]   (primero correr generar.py, que deja la geometría prevista)
Sale con código 1 si encuentra algún problema.
"""
import re
import subprocess
import sys
from pathlib import Path

from metricas import ancho

MDJ = sys.argv[1]
SVG = sys.argv[2] if len(sys.argv) > 2 else str(Path(MDJ).with_suffix('.svg'))

subprocess.run(['staruml', 'image', MDJ, '-f', 'svg', '-o', SVG], check=True, capture_output=True, timeout=900)
s = Path(SVG).read_text()

NUM = r'(-?[\d.]+)'
rects, lineas, textos, polis = [], [], [], []
for m in re.finditer(r'<path ([^>]*?)d="([^"]+)"([^>]*)/>', s):
    attrs = m.group(1) + m.group(3)
    d = m.group(2)
    fill = re.search(r'fill="([^"]+)"', attrs).group(1)
    stroke = re.search(r'stroke="([^"]+)"', attrs).group(1)
    if 'fill-opacity' in attrs:
        continue  # sombras
    pts = [(float(x), float(y)) for x, y in re.findall(NUM + ' ' + NUM, d)]
    if d.strip().endswith('Z'):
        xs, ys = [p[0] for p in pts], [p[1] for p in pts]
        caja = (min(xs), min(ys), max(xs) - min(xs), max(ys) - min(ys))
        if fill != 'none':
            rectangular = len(pts) == 4 and len({round(p[0], 1) for p in pts}) == 2 and len({round(p[1], 1) for p in pts}) == 2
            if rectangular and stroke == 'none':
                rects.append((caja, fill))
            else:
                polis.append((caja, fill))
    elif fill == 'none' and len(pts) >= 2:
        cerrada = len(pts) > 2 and abs(pts[0][0] - pts[-1][0]) < 0.6 and abs(pts[0][1] - pts[-1][1]) < 0.6
        if not cerrada:  # los contornos cerrados son rombos y puntas de flecha, no líneas
            lineas.append((pts, re.search(r'stroke-dasharray', attrs) is not None))
for m in re.finditer(r'<text ([^>]*)>([^<]*)</text>', s):
    a = m.group(1)
    x = float(re.search(r'x="([\d.-]+)"', a).group(1))
    y = float(re.search(r'y="([\d.-]+)"', a).group(1))
    bold = 'font-weight="bold"' in a
    txt = m.group(2).replace('&lt;', '<').replace('&gt;', '>').replace('&amp;', '&').replace('&quot;', '"')
    w = ancho(txt, bold=bold)
    textos.append(dict(t=txt, r=(x, y - 10.1, w, 13), bold=bold))

COLORES_PAQ = set()
for c in ['#FFE5E5', '#FFF0F0', '#FFF7F7', '#E3ECFF', '#EEF3FF', '#F6F9FF', '#DFFFDF', '#ECFFEC', '#F5FFF5',
          '#FFE8DF', '#FFF1EB', '#FFF8F5']:
    COLORES_PAQ.add(f'rgb({int(c[1:3], 16)},{int(c[3:5], 16)},{int(c[5:7], 16)})')

etiquetas = [r for r, f in rects if f == 'rgb(255,255,255)' and abs(r[3] - 13) < 0.6 and r[2] < 400]
# las cajas de clase: rectángulos rellenos que no son etiquetas, ni compartimentos internos, ni paquetes
candidatas = [r for r, f in rects if f not in COLORES_PAQ and not (f == 'rgb(255,255,255)' and abs(r[3] - 13) < 0.6)]
paquetes = [r for r, f in rects if f in COLORES_PAQ]
# compartimentos de una clase quedan contenidos en otra caja del mismo color: me quedo con las externas
def contiene(a, b, tol=0.6):
    return a[0] - tol <= b[0] and a[1] - tol <= b[1] and a[0] + a[2] + tol >= b[0] + b[2] and a[1] + a[3] + tol >= b[1] + b[3]
candidatas.sort(key=lambda r: -r[2] * r[3])
cajas = []
for r in candidatas:
    if not any(contiene(c, r) for c in cajas):
        cajas.append(r)
notas = [r for r, f in polis if f != 'none' and r[2] > 40 and r[3] > 20]
cuerpos_paq = [r for r in paquetes if r[3] > 16]
pestanas = [r for r in paquetes if r[3] <= 16]


def se_tocan(a, b, pad=0.0):
    return (a[0] - pad < b[0] + b[2] and b[0] - pad < a[0] + a[2] and
            a[1] - pad < b[1] + b[3] and b[1] - pad < a[1] + a[3])


def segmento_cruza(r, p, q, pad=0.0):
    x0, y0, x1, y1 = r[0] - pad, r[1] - pad, r[0] + r[2] + pad, r[1] + r[3] + pad
    dx, dy = q[0] - p[0], q[1] - p[1]
    t0, t1 = 0.0, 1.0
    for pp, qq in ((-dx, p[0] - x0), (dx, x1 - p[0]), (-dy, p[1] - y0), (dy, y1 - p[1])):
        if pp == 0:
            if qq < 0:
                return False
        else:
            t = qq / pp
            if pp < 0:
                t0 = max(t0, t)
            else:
                t1 = min(t1, t)
            if t0 > t1:
                return False
    return True


problemas = {k: [] for k in ['etiqueta-etiqueta', 'etiqueta-caja', 'etiqueta-texto', 'caja-caja', 'caja-paquete',
                             'texto-fuera', 'linea-caja', 'linea-etiqueta', 'paquete-paquete']}


def txt_en(r):
    return next((t['t'] for t in textos if contiene(r, t['r'], 2) or se_tocan(r, t['r'], -3)), '?')


# 1
for i, a in enumerate(etiquetas):
    for b in etiquetas[i + 1:]:
        if se_tocan(a, b, 1):
            problemas['etiqueta-etiqueta'].append((txt_en(a), txt_en(b), a[:2]))
# 2
for a in etiquetas:
    for c in cajas + notas:
        if se_tocan(a, c, 1):
            problemas['etiqueta-caja'].append((txt_en(a), a[:2]))
# 2b: títulos de paquetes y otros textos sueltos
textos_libres = [t for t in textos if not any(contiene(c, t['r'], 3) for c in cajas + notas + etiquetas)]
for a in etiquetas:
    for t in textos_libres:
        if se_tocan(a, t['r'], 1):
            problemas['etiqueta-texto'].append((txt_en(a), t['t'], a[:2]))
# 3
for i, a in enumerate(cajas):
    for b in cajas[i + 1:]:
        if se_tocan(a, b, 0):
            problemas['caja-caja'].append((txt_en(a), txt_en(b)))
for c in cajas + notas:
    dentro = [p for p in cuerpos_paq if contiene(p, c)]
    cruza = [p for p in cuerpos_paq if se_tocan(p, c) and not contiene(p, c) and not contiene(c, p)]
    if cruza:
        problemas['caja-paquete'].append((txt_en(c), c[:2]))
for a in cuerpos_paq:
    for b in cuerpos_paq:
        if a < b and se_tocan(a, b) and not contiene(a, b) and not contiene(b, a):
            problemas['paquete-paquete'].append((a[:2], b[:2]))
# 4
for t in textos:
    duenia = [c for c in cajas + notas if se_tocan(c, t['r'], -2)]
    for c in duenia:
        if not contiene(c, t['r'], 1.5) and t['r'][0] >= c[0] - 1:
            problemas['texto-fuera'].append((t['t'], c[:2]))
# 5 y 6
segs = []
for pts, _ in lineas:
    largo = sum(((p[0] - q[0]) ** 2 + (p[1] - q[1]) ** 2) ** 0.5 for p, q in zip(pts, pts[1:]))
    if largo < 30:
        continue  # puntas de flecha y rombos
    segs.extend(zip(pts, pts[1:]))
def adentro(c, p, tol=1.5):
    return c[0] - tol <= p[0] <= c[0] + c[2] + tol and c[1] - tol <= p[1] <= c[1] + c[3] + tol


segs = [(p, q) for p, q in segs if not any(adentro(c, p) and adentro(c, q) for c in cajas + notas)]
for p, q in segs:
    for c in cajas + notas:
        interior = (c[0] + 5, c[1] + 5, c[2] - 10, c[3] - 10)
        if interior[2] > 0 and interior[3] > 0 and segmento_cruza(interior, p, q):
            problemas['linea-caja'].append((txt_en(c), (round(p[0]), round(p[1])), (round(q[0]), round(q[1]))))
for a in etiquetas:
    n = sum(1 for p, q in segs if segmento_cruza((a[0] + 1, a[1] + 1, a[2] - 2, a[3] - 2), p, q))
    if n > 1:  # una puede ser la propia (la etiqueta del nombre va sobre su línea)
        problemas['linea-etiqueta'].append((txt_en(a), a[:2], n))

# 8: lo previsto por el generador contra lo que dibujó StarUML (etiquetas en su lugar)
import tempfile
geom = Path(tempfile.gettempdir(), Path(MDJ).stem + '.geom.json')
problemas['prevision'] = []
if geom.exists():
    import json
    from collections import Counter
    g = json.loads(geom.read_text())
    tam = Counter((round(c[2]), round(c[3])) for c in cajas)
    dif = Counter()
    for nombre, c in g['cajas'].items():
        for s_ in cajas:
            if abs(s_[2] - c[2]) < 1 and abs(s_[3] - c[3]) < 1 and tam[(round(s_[2]), round(s_[3]))] == 1:
                dif[(round(s_[0] - c[0]), round(s_[1] - c[1]))] += 1
    (ox, oy), _ = dif.most_common(1)[0]
    for e in g['etiquetas']:
        r = e['rect']
        esperado = (r[0] + ox, r[1] + oy)
        if not any(abs(a[0] - esperado[0]) < 8 and abs(a[1] - esperado[1]) < 8 for a in etiquetas):
            problemas['prevision'].append((e['texto'], esperado))
    for nombre, c in g['cajas'].items():
        if not any(abs(s_[0] - c[0] - ox) < 1.5 and abs(s_[1] - c[1] - oy) < 1.5 and s_[2] >= c[2] - 1
                   and s_[3] >= c[3] - 1 and s_[2] <= c[2] + 1.5 and s_[3] <= c[3] + 1.5 for s_ in cajas):
            problemas['prevision'].append(('caja', nombre))

# 9: líneas que pasan por títulos de paquetes u otros textos sueltos
problemas['linea-texto'] = []
for t in textos_libres:
    r = (t['r'][0] + 1, t['r'][1] + 2, t['r'][2] - 2, t['r'][3] - 4)
    for p, q in segs:
        if segmento_cruza(r, p, q):
            problemas['linea-texto'].append((t['t'], (round(p[0]), round(p[1])), (round(q[0]), round(q[1]))))
            break
# 10: tramos de líneas distintas encimados (misma recta y se superponen)
problemas['linea-encimada'] = []
tramos = []
for li, (pts, _) in enumerate(lineas):
    largo = sum(((p[0] - q[0]) ** 2 + (p[1] - q[1]) ** 2) ** 0.5 for p, q in zip(pts, pts[1:]))
    if largo < 30:
        continue
    for p, q in zip(pts, pts[1:]):
        if p[0] == q[0] or abs(p[0] - q[0]) < 0.6:
            tramos.append(('v', round(p[0]), min(p[1], q[1]), max(p[1], q[1]), li))
        elif abs(p[1] - q[1]) < 0.6:
            tramos.append(('h', round(p[1]), min(p[0], q[0]), max(p[0], q[0]), li))
idx = {}
for t in tramos:
    idx.setdefault((t[0], t[1]), []).append(t)
for k, lst in idx.items():
    lst.sort(key=lambda t: t[2])
    for i, a in enumerate(lst):
        for b in lst[i + 1:]:
            if b[2] >= a[3] - 4:
                break
            if a[4] != b[4] and min(a[3], b[3]) - b[2] > 4:
                problemas['linea-encimada'].append((k, round(b[2]), round(min(a[3], b[3]))))
# 10b: tramos encimados en cualquier dirección (misma recta, a menos de 2 px, superpuestos más de 6 px)
import math as _m
problemas['linea-encimada-diagonal'] = []
todos = []
for li, (pts, _) in enumerate(lineas):
    largo = sum(((p[0] - q[0]) ** 2 + (p[1] - q[1]) ** 2) ** 0.5 for p, q in zip(pts, pts[1:]))
    if largo < 30:
        continue
    for p, q in zip(pts, pts[1:]):
        L = _m.dist(p, q)
        if L > 6 and not any(adentro(c, p) and adentro(c, q) for c in cajas + notas):
            todos.append((p, q, li, L))
celda = {}
for t in todos:
    p, q = t[0], t[1]
    for cx in range(int(min(p[0], q[0]) // 200), int(max(p[0], q[0]) // 200) + 1):
        for cy in range(int(min(p[1], q[1]) // 200), int(max(p[1], q[1]) // 200) + 1):
            celda.setdefault((cx, cy), []).append(t)
from geometria import encimados as _enc
vistos = set()
for lst in celda.values():
    for i, a in enumerate(lst):
        for b in lst[i + 1:]:
            if a[2] == b[2] or (id(a), id(b)) in vistos:
                continue
            vistos.add((id(a), id(b)))
            # tramos que se tocan en un extremo común (una línea que dobla) no cuentan
            solape = _enc((a[0], a[1]), (b[0], b[1]), 5.0)
            if solape > 20:
                problemas['linea-encimada-diagonal'].append(((round(a[0][0]), round(a[0][1])), (round(a[1][0]), round(a[1][1])), round(solape)))
# 10c: nudos: vértices de dos relaciones distintas en el mismo punto
problemas['linea-nudo'] = []
vert = {}
for li, (pts, _) in enumerate(lineas):
    largo = sum(((p[0] - q[0]) ** 2 + (p[1] - q[1]) ** 2) ** 0.5 for p, q in zip(pts, pts[1:]))
    if largo < 30:
        continue
    for p in pts[1:-1]:
        if any(adentro(c, p, 3) for c in cajas + notas):
            continue
        k = (round(p[0] / 5), round(p[1] / 5))
        otro = next((vert[c] for c in ((k[0] + a, k[1] + b) for a in (-1, 0, 1) for b in (-1, 0, 1))
                     if c in vert and vert[c] != li), None)
        if otro is not None:
            problemas['linea-nudo'].append((round(p[0]), round(p[1])))
        vert.setdefault(k, li)
# 11: multiplicidad más cerca del extremo de otra relación que del propio (se lee ambigua)
problemas['etiqueta-ambigua'] = []
if geom.exists():
    import math
    extremos = []
    for i, a in enumerate(g['aristas']):
        extremos.append((i, a['puntos'][0]))
        extremos.append((i, a['puntos'][-1]))
    for e in g['etiquetas']:
        if 'Multiplicity' not in e['rol']:
            continue
        a = g['aristas'][e['arista']]
        propio = a['puntos'][0] if e['rol'].startswith('tail') else a['puntos'][-1]
        r = e['rect']
        c = (r[0] + r[2] / 2, r[1] + r[3] / 2)
        dp = math.dist(c, propio)
        for i, p in extremos:
            if i != e['arista'] and math.dist(c, p) < dp - 3 and math.dist(p, propio) > 6:
                problemas['etiqueta-ambigua'].append((e['texto'], a['o'], a['d']))
                break

# 12: etiqueta sobre una línea (ajena o propia), con la geometría de cada relación
problemas['etiqueta-sobre-linea'] = []
if geom.exists():
    for e in g['etiquetas']:
        r = e['rect']
        r = (r[0] + 1, r[1] + 1, r[2] - 2, r[3] - 2)
        for i, a in enumerate(g['aristas']):
            pts = a['puntos']
            if any(segmento_cruza(r, p, q) for p, q in zip(pts, pts[1:])):
                problemas['etiqueta-sobre-linea'].append(
                    (e['texto'], 'propia' if i == e['arista'] else f"de {a['o']}->{a['d']}"))

ancho_svg = re.search(r'width="([\d.]+)"', s).group(1)
alto_svg = re.search(r'height="([\d.]+)"', s).group(1)
print(f'SVG {ancho_svg}x{alto_svg}: {len(cajas)} cajas, {len(notas)} notas, {len(etiquetas)} etiquetas, '
      f'{len(segs)} segmentos, {len(cuerpos_paq)} paquetes')
total = 0
for k, v in problemas.items():
    total += len(v)
    print(f'{k:18s} {len(v)}')
    for x in v[:12]:
        print('      ', x)
print('TOTAL', total)
sys.exit(1 if total else 0)
