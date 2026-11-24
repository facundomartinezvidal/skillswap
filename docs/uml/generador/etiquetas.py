"""Ubica las etiquetas de las relaciones (multiplicidades, nombres y estereotipos) sin superposiciones.

StarUML dibuja cada etiqueta centrada en  P + distancia * (cos(θ - alpha), sin(θ - alpha)),  donde P es el
extremo (o el punto medio) de la línea y θ el ángulo hacia el punto vecino. Se calibró exportando el SVG.
Acá se prueban varias (alpha, distancia) por etiqueta y se elige la que no pisa cajas, títulos de paquetes,
otras etiquetas ni otras líneas.
"""
import math

from metricas import ancho

ALTO = 13
PAD = 5
CELDA = 120


def ancla(puntos, pos):
    n = len(puntos)
    if pos == 2:
        return puntos[0], puntos[1]
    if pos == 0:
        return puntos[-1], puntos[-2]
    if n % 2:
        return puntos[n // 2], puntos[n // 2 + 1]
    a, b = puntos[n // 2 - 1], puntos[n // 2]
    return ((a[0] + b[0]) / 2, (a[1] + b[1]) / 2), b


def centro(puntos, pos, alpha, dist):
    p, q = ancla(puntos, pos)
    return centro_desde(p, q, alpha, dist)


def centro_desde(p, q, alpha, dist):
    th = math.atan2(q[1] - p[1], q[0] - p[0])
    return p[0] + dist * math.cos(th - alpha), p[1] + dist * math.sin(th - alpha)


def anclas_medio(pts):
    """Puntos candidatos para la etiqueta del medio: (segmento, punto, punto siguiente)."""
    largos = [math.dist(a, b) for a, b in zip(pts, pts[1:])]
    total = sum(largos)
    out = []
    for f in (0.5, 0.4, 0.6, 0.3, 0.7, 0.2, 0.8, 0.12, 0.88, 0.45, 0.55, 0.35, 0.65, 0.25, 0.75, 0.16, 0.84, 0.08, 0.92):
        objetivo, acum = f * total, 0
        for i, l in enumerate(largos):
            if acum + l >= objetivo and l > 0:
                t = (objetivo - acum) / l
                a, b = pts[i], pts[i + 1]
                p = (round(a[0] + t * (b[0] - a[0])), round(a[1] + t * (b[1] - a[1])))
                if p != tuple(a) and p != tuple(b):
                    out.append((i, p, tuple(b)))
                break
            acum += l
    return out


def anclar(pts, seg, p):
    """Devuelve los puntos con p como punto del medio (StarUML ancla ahí la etiqueta del medio)."""
    q = [tuple(x) for x in pts[:seg + 1]] + [p] + [tuple(x) for x in pts[seg + 1:]]
    i = seg + 1
    antes, despues = i, len(q) - 1 - i

    def partir(a, b, k):
        return [(round(a[0] + (b[0] - a[0]) * j / (k + 1)), round(a[1] + (b[1] - a[1]) * j / (k + 1)))
                for j in range(1, k + 1)]

    if antes < despues:
        k = despues - antes
        q = q[:i] + partir(q[i - 1], p, k) + q[i:]
    elif despues < antes:
        k = antes - despues
        q = q[:i + 1] + partir(p, q[i + 1], k) + q[i + 1:]
    return q


def rect_de(c, w):
    return (c[0] - w / 2, c[1] - ALTO / 2, w, ALTO)


def se_tocan(a, b, pad=0):
    return (a[0] - pad < b[0] + b[2] and b[0] - pad < a[0] + a[2] and
            a[1] - pad < b[1] + b[3] and b[1] - pad < a[1] + a[3])


def segmento_cruza(r, p, q, pad=0):
    """Liang-Barsky: ¿el segmento p-q pasa por el rectángulo r (agrandado en pad)?"""
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


class Grilla:
    def __init__(self):
        self.celdas = {}

    def _celdas(self, r):
        for cx in range(int(r[0] // CELDA), int((r[0] + r[2]) // CELDA) + 1):
            for cy in range(int(r[1] // CELDA), int((r[1] + r[3]) // CELDA) + 1):
                yield cx, cy

    def agregar(self, r, dato):
        for c in self._celdas(r):
            self.celdas.setdefault(c, []).append((r, dato))

    def cerca(self, r):
        vistos = set()
        for c in self._celdas(r):
            for item in self.celdas.get(c, []):
                if id(item) not in vistos:
                    vistos.add(id(item))
                    yield item


def caja_segmento(p, q):
    return (min(p[0], q[0]), min(p[1], q[1]), abs(p[0] - q[0]) + 1, abs(p[1] - q[1]) + 1)


CANDIDATOS_EXTREMO = [(a, d) for d in (16, 22, 30, 40, 52, 66, 84, 104, 126, 150, 180) for a in
                      (0.45, -0.45, 0.75, -0.75, 1.1, -1.1, 0.25, -0.25, 1.45, -1.45, 1.9, -1.9, 0.12, -0.12,
                       2.4, -2.4)]
CANDIDATOS_MEDIO = [(a, d) for d in (11, 16, 24, 34, 46, 60, 78, 98) for a in
                    (1.5708, -1.5708, 1.2, -1.2, 1.9, -1.9, 0.9, -0.9, 2.3, -2.3)]


def ubicar(etqs, aristas, obstaculos):
    g_obst, g_seg, g_etq = Grilla(), Grilla(), Grilla()
    for r in obstaculos:
        g_obst.agregar(r, None)
    extremos = []
    for i, a in enumerate(aristas):
        pts = a['puntos']
        for p, q in zip(pts, pts[1:]):
            g_seg.agregar(caja_segmento(p, q), (i, p, q))
        extremos.append((pts[0], pts[-1]))
    g_ext = Grilla()
    for i, (p0, p1) in enumerate(extremos):
        g_ext.agregar((p0[0] - 1, p0[1] - 1, 2, 2), (i, p0))
        g_ext.agregar((p1[0] - 1, p1[1] - 1, 2, 2), (i, p1))

    def costo(e, r, c):
        total = 0
        for ro, _ in g_obst.cerca(r):
            if se_tocan(r, ro, PAD):
                total += 10000
        for ro, otra in g_etq.cerca(r):
            if se_tocan(r, ro, PAD):
                total += 10000
        for _, (i, p, q) in g_seg.cerca((r[0] - PAD, r[1] - PAD, r[2] + 2 * PAD, r[3] + 2 * PAD)):
            if segmento_cruza(r, p, q, PAD):
                total += 1500 if i == e['arista'] else 2000
        if e['pos'] != 1:
            propio = extremos[e['arista']][0 if e['pos'] == 2 else 1]
            d_propio = math.dist(c, propio)
            zona = (c[0] - d_propio, c[1] - d_propio, 2 * d_propio, 2 * d_propio)
            for _, (i, p) in g_ext.cerca(zona):
                if i != e['arista'] and math.dist(c, p) < d_propio - 2:
                    total += 3000
                    break
        return total

    # primero las aristas cortas, que tienen menos lugar
    def largo(a):
        return sum(math.dist(p, q) for p, q in zip(a['puntos'], a['puntos'][1:]))

    orden = sorted(range(len(etqs)), key=lambda j: (largo(aristas[etqs[j]['arista']]), etqs[j]['pos'] == 1))
    conflictos = 0
    for j in orden:
        e = etqs[j]
        w = ancho(e['texto'])
        pts = aristas[e['arista']]['puntos']
        mejor = None
        if e['pos'] == 1:
            cands = [(a, d, anc) for anc in anclas_medio(pts) for (a, d) in CANDIDATOS_MEDIO]
        else:
            cands = [(a, d, None) for (a, d) in CANDIDATOS_EXTREMO]
        for k, (alpha, dist, anc) in enumerate(cands):
            if anc is None:
                c = centro(pts, e['pos'], alpha, dist)
            else:
                c = centro_desde(anc[1], anc[2], alpha, dist)
            r = rect_de(c, w)
            costo_total = costo(e, r, c) + dist * 0.8 + k * 0.002
            if mejor is None or costo_total < mejor[0]:
                mejor = (costo_total, alpha, dist, r, anc)
            if costo_total < 60 and dist >= 16:
                break
        costo_total, alpha, dist, r, anc = mejor
        if anc is not None:
            aristas[e['arista']]['puntos_vista'] = anclar(pts, anc[0], anc[1])
        if costo_total >= 2000:
            conflictos += 1
        e.update(alpha=alpha, distancia=dist, rect=[round(v, 1) for v in r], costo=round(costo_total))
        g_etq.agregar(r, j)
    print(f'etiquetas: {len(etqs)}, con conflicto en el layout previsto: {conflictos}')
    return conflictos
