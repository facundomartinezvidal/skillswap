"""Layout del diagrama completo.

1. Cada módulo (paquete de primer nivel dentro de una capa) se arma con dot por separado, con sus
   subpaquetes como clusters. Las relaciones que salen del módulo terminan en un "puerto" invisible en el
   borde de arriba o de abajo del bloque.
2. Los bloques se ubican en filas fijas (FILAS), con todas las dependencias apuntando hacia abajo (hacia el
   dominio). Entre filas y entre bloques quedan pasillos.
3. Las relaciones entre bloques se rutean en ángulo recto por los pasillos; cada una tiene su carril, así
   que dos líneas nunca van una encima de la otra.
"""
import json
import re
import subprocess

MARGEN_BLOQUE = 24        # margen interno del paquete de módulo
TITULO_BLOQUE = 40        # lugar para la pestaña y el nombre del paquete
MARGEN_CAPA = 30
TITULO_CAPA = 46
CARRIL = 12               # separación entre líneas paralelas en un pasillo
PASILLO_BASE = 2 * MARGEN_CAPA + 50   # lugar mínimo de un pasillo (márgenes de los marcos de capa)


def _dot(fuente):
    return json.loads(subprocess.run(['dot', '-Tjson'], input=fuente, capture_output=True, text=True,
                                     check=True).stdout)


def _puntos_ruta(ed, H, dx, dy):
    puntos = [p for p in ed['pos'].split(' ') if not p.startswith(('e,', 's,'))]
    xy = [(float(a) + dx, H - float(b) + dy) for a, b in (p.split(',') for p in puntos)]
    return xy[::3] if len(xy) > 1 else xy


def layout_bloque(clave, clases, T, med, notas, medida_nota, internas, puertos, FILA=4):
    """Arma un módulo con dot. Devuelve tamaño y posiciones locales (origen arriba a la izquierda)."""
    nodo = {k: f'n{i}' for i, k in enumerate(clases)}
    dot = ['digraph G {', 'graph [rankdir=TB, splines=polyline, nodesep=0.75, ranksep=1.0, newrank=true, '
           'fontname="Arial", fontsize=13];', 'node [shape=box, fixedsize=true, label=""];']
    arbol = {'_clases': []}
    for k in clases:
        n = arbol
        for p in T[k]['ruta'][T[k]['prof']:]:
            n = n.setdefault(p, {'_clases': []})
        n['_clases'].append(k)
    superiores = {a for (a, b, _) in internas} | {k for k in clases if k in notas}
    clusters = {}

    def emitir(n, nombre, ruta):
        if ruta:
            dot.append(f'subgraph cluster_{nombre} {{ label="\\n{ruta[-1]}"; margin=20;')
            clusters[f'cluster_{nombre}'] = tuple(ruta)
        for k in n['_clases']:
            w, h = med[k]
            dot.append(f'{nodo[k]} [width={w / 72:.3f}, height={h / 72:.3f}];')
            if k in notas:
                nw, nh = medida_nota(notas[k])
                dot.append(f'nota_{nodo[k]} [width={nw / 72:.3f}, height={nh / 72:.3f}];')
        hojas = [k for k in n['_clases'] if k not in superiores]
        for a, b in zip(hojas, hojas[FILA:]):
            dot.append(f'{nodo[a]} -> {nodo[b]} [style=invis, weight=0];')
        for hijo in sorted(x for x in n if x != '_clases'):
            emitir(n[hijo], f'{nombre}_{re.sub(r"[^a-zA-Z0-9]", "", hijo)}', ruta + [hijo])
        if ruta:
            dot.append('}')

    emitir(arbol, 'raiz', [])
    for k in clases:
        if k in notas:
            dot.append(f'{nodo[k]} -> nota_{nodo[k]} [id="nota_{nodo[k]}_r", weight=8, minlen=1];')
    pares, invertidas = set(), set()
    for i, (a, b, eid) in enumerate(internas):
        if (b, a) in pares:  # mismo par en sentido contrario: se emite igual que el otro para que vayan paralelas
            dot.append(f'{nodo[b]} -> {nodo[a]} [id="{eid}", weight=3];')
            invertidas.add(eid)
        else:
            dot.append(f'{nodo[a]} -> {nodo[b]} [id="{eid}", weight=3];')
            pares.add((a, b))
    arriba = [p for p in puertos if p['lado'] == 'arriba']
    abajo = [p for p in puertos if p['lado'] == 'abajo']
    for p in puertos:
        dot.append(f'{p["id"]} [width=0.12, height=0.12, fixedsize=true];')
        if p['lado'] == 'abajo':
            dot.append(f'{nodo[p["clase"]]} -> {p["id"]} [id="{p["id"]}_r", weight=2];')
        else:
            dot.append(f'{p["id"]} -> {nodo[p["clase"]]} [id="{p["id"]}_r", weight=2];')
    # techo y fondo invisibles: los puertos quedan estrictamente arriba o abajo de todas las cajas
    todos = [nodo[k] for k in clases] + [f'nota_{nodo[k]}' for k in clases if k in notas]
    if arriba:
        dot.append('techo [width=0.01, height=0.01, style=invis];')
        for n in todos:
            dot.append(f'techo -> {n} [style=invis, weight=0];')
        dot.append('{rank=min; techo; ' + '; '.join(p['id'] for p in arriba) + '}')
    if abajo:
        dot.append('fondo [width=0.01, height=0.01, style=invis];')
        for n in todos:
            dot.append(f'{n} -> fondo [style=invis, weight=0];')
        dot.append('{rank=max; fondo; ' + '; '.join(p['id'] for p in abajo) + '}')
    dot.append('}')
    js = _dot('\n'.join(dot))
    x1, y1, x2, y2 = map(float, js['bb'].split(','))
    H = y2
    dx, dy = MARGEN_BLOQUE - x1, MARGEN_BLOQUE + TITULO_BLOQUE
    pos, subpaq, puerto_pos, rutas = {}, {}, {}, {}
    for ob in js.get('objects', []):
        if ob['name'].startswith('cluster_'):
            a1, b1, a2, b2 = map(float, ob['bb'].split(','))
            subpaq[clusters[ob['name']]] = (a1 + dx, H - b2 + dy, a2 - a1, b2 - b1)
        elif 'pos' in ob:
            x, y = map(float, ob['pos'].split(','))
            w, h = float(ob['width']) * 72, float(ob['height']) * 72
            pos[ob['name']] = (x + dx - w / 2, H - y + dy - h / 2, w, h)
    for ed in js.get('edges', []):
        if 'id' in ed and 'pos' in ed:
            rutas[ed['id']] = _puntos_ruta(ed, H, dx, dy)
            if ed['id'] in invertidas:
                rutas[ed['id']] = rutas[ed['id']][::-1]
    ancho = (x2 - x1) + 2 * MARGEN_BLOQUE
    alto = (y2 - y1) + 2 * MARGEN_BLOQUE + TITULO_BLOQUE
    cajas = {k: pos[nodo[k]] for k in clases}
    cajas_notas = {k: pos[f'nota_{nodo[k]}'] for k in clases if k in notas}
    rutas_notas = {k: rutas.get(f'nota_{nodo[k]}_r') for k in clases if k in notas}
    for p in puertos:
        l, t, w, h = pos[p['id']]
        puerto_pos[p['id']] = (l + w / 2, t + h / 2)
    return dict(ancho=ancho, alto=alto, cajas=cajas, notas=cajas_notas, subpaq=subpaq, puertos=puerto_pos,
                rutas=rutas, rutas_notas=rutas_notas)


def mover(r, dx, dy):
    return (r[0] + dx, r[1] + dy, r[2], r[3])


def componer(T, R, med, notas, medida_nota, FILAS, ALINEAR, arriba_abajo, EXTRA_PASILLO=None):
    EXTRA_PASILLO = EXTRA_PASILLO or {}
    """FILAS: lista de filas; cada fila, lista de claves de bloque (capa, módulo)."""
    bloque_de = {k: T[k]['bloque'] for k in T}
    prefijo = {T[k]['bloque']: tuple(T[k]['ruta'][:T[k]['prof']]) for k in T}
    fila_de = {b: i for i, fila in enumerate(FILAS) for b in fila}
    for k, b in bloque_de.items():
        if b not in fila_de:
            raise ValueError(f'bloque sin fila: {b}')

    internas = {b: [] for b in fila_de}
    cruzadas = []
    puertos = {b: [] for b in fila_de}
    capa_filas = {}
    for b, f in fila_de.items():
        capa_filas.setdefault(b[0], []).append(f)
    destinos_paq = {}
    for i, r in enumerate(R):
        o, d = r['o'], r['d']
        if isinstance(d, tuple):  # dependencia a un paquete (módulo o capa entera): entra por arriba del marco
            bo = bloque_de[o]
            bd = ('CAPA', d[1]) if d[2] is None else (d[1], d[2])
            fd = min(capa_filas[d[1]]) if d[2] is None else fila_de[bd]
            po = dict(id=f'p{i}o', clase=o, lado='abajo' if fd > fila_de[bo] else 'arriba')
            puertos[bo].append(po)
            n = destinos_paq.setdefault(bd, 0)
            destinos_paq[bd] = n + 1
            cruzadas.append(dict(i=i, bo=bo, bd=bd, po=po, pd=dict(id=None, lado='arriba', orden=n), fd=fd))
            continue
        bo, bd = bloque_de[o], bloque_de[d]
        if bo == bd:
            a, b = arriba_abajo(r)
            internas[bo].append((a, b, f'e{i}'))
            continue
        fo, fd = fila_de[bo], fila_de[bd]
        if fd > fo:
            lo, ld = 'abajo', 'arriba'
        elif fd < fo:
            lo, ld = 'arriba', 'abajo'
        else:
            lo, ld = 'abajo', 'abajo'
        po = dict(id=f'p{i}o', clase=o, lado=lo)
        pd = dict(id=f'p{i}d', clase=d, lado=ld)
        puertos[bo].append(po)
        puertos[bd].append(pd)
        cruzadas.append(dict(i=i, bo=bo, bd=bd, po=po, pd=pd))

    bloques = {}
    for c in cruzadas:
        c.setdefault('fd', fila_de.get(c['bd']))
    for b in fila_de:
        clases = [k for k in T if bloque_de[k] == b]
        bloques[b] = layout_bloque(b, clases, T, med, notas, medida_nota, internas[b], puertos[b])

    # --- topología de las rutas: pasillo horizontal h (0 = arriba de la fila 0, n = abajo de la última)
    # y huecos verticales de cada fila (0 = margen izquierdo, len(fila) = margen derecho).
    def carriles_necesarios(xs_fila, rutas_top):
        return None

    n_filas = len(FILAS)
    huecos_v = {(f, g): 0 for f in range(n_filas) for g in range(len(FILAS[f]) + 1)}
    alto_pasillo = [PASILLO_BASE + EXTRA_PASILLO.get(h, 0) for h in range(n_filas + 1)]
    ancho_hueco = {k: PASILLO_BASE for k in huecos_v}

    def posicionar():
        """Coordenadas de los bloques según el tamaño actual de pasillos y huecos."""
        anchos = []
        for f, fila in enumerate(FILAS):
            w = sum(bloques[b]['ancho'] for b in fila) + sum(ancho_hueco[(f, g)] for g in range(len(fila) + 1))
            anchos.append(w)
        total = max(anchos)
        origen = {}
        y = alto_pasillo[0]
        tops, bottoms = [], []
        for f, fila in enumerate(FILAS):
            alto_fila = max(bloques[b]['alto'] for b in fila)
            al = ALINEAR.get(f, 'centro')
            x = (total - anchos[f]) / 2 if al == 'centro' else (total - anchos[f] if al == 'derecha' else 0)
            gx = []
            for g, b in enumerate(fila):
                gx.append(x + ancho_hueco[(f, g)] / 2)
                x += ancho_hueco[(f, g)]
                origen[b] = (x, y)
                x += bloques[b]['ancho']
            gx.append(x + ancho_hueco[(f, len(fila))] / 2)
            posicionar.gx[f] = gx
            tops.append(y)
            bottoms.append(y + alto_fila)
            y += alto_fila + alto_pasillo[f + 1]
        posicionar.total = total
        posicionar.tops, posicionar.bottoms = tops, bottoms
        return origen

    posicionar.gx = {}

    def rango_destino(bd, origen):
        if bd[0] == 'CAPA':
            bs = [b for b in origen if b[0] == bd[1]]
            return min(origen[b][0] for b in bs), max(origen[b][0] + bloques[b]['ancho'] for b in bs)
        return origen[bd][0], origen[bd][0] + bloques[bd]['ancho']

    def rutear(origen):
        """Para cada relación cruzada: lista de tramos [('h', pasillo, x1, x2) | ('v', fila, hueco)]."""
        caminos = []
        for c in cruzadas:
            (ox, oy) = origen[c['bo']]
            xs = ox + bloques[c['bo']]['puertos'][c['po']['id']][0]
            if c['pd']['id'] is None:
                l, r_ = rango_destino(c['bd'], origen)
                xt = (l + r_) / 2 + (c['pd']['orden'] - (destinos_paq[c['bd']] - 1) / 2) * 60
            else:
                (tx, ty) = origen[c['bd']]
                xt = tx + bloques[c['bd']]['puertos'][c['pd']['id']][0]
            fo, fd = fila_de[c['bo']], c['fd']
            ho = fo + 1 if c['po']['lado'] == 'abajo' else fo
            hd = fd if c['pd']['lado'] == 'arriba' else fd + 1
            tramos = []
            x = xs
            h = ho
            paso = 1 if hd > ho else -1
            while h != hd:
                f = h if paso == 1 else h - 1  # fila que se atraviesa
                gxs = posicionar.gx[f]
                g = min(range(len(gxs)), key=lambda g: abs(gxs[g] - x) + abs(gxs[g] - xt))
                tramos.append(('h', h, x, gxs[g]))
                tramos.append(('v', f, g))
                x = gxs[g]
                h += paso
            tramos.append(('h', h, x, xt))
            caminos.append(dict(c=c, xs=xs, xt=xt, ho=ho, hd=hd, tramos=tramos))
        return caminos

    def asignar(caminos):
        """Carril de cada tramo: intervalos que se superponen en un mismo pasillo van en carriles distintos."""
        por_pasillo, por_hueco = {}, {}
        for ci, cam in enumerate(caminos):
            for ti, tr in enumerate(cam['tramos']):
                if tr[0] == 'h':
                    a, b = sorted((tr[2], tr[3]))
                    por_pasillo.setdefault(tr[1], []).append((a - 1, b + 1, ci, ti))
                else:
                    por_hueco.setdefault((tr[1], tr[2]), []).append((ci, ti))
        carril = {}
        uso_p = {}
        for h, ivs in por_pasillo.items():
            ivs.sort()
            libres_hasta = []
            for a, b, ci, ti in ivs:
                for t, fin in enumerate(libres_hasta):
                    if fin < a:
                        libres_hasta[t] = b
                        carril[(ci, ti)] = t
                        break
                else:
                    libres_hasta.append(b)
                    carril[(ci, ti)] = len(libres_hasta) - 1
            uso_p[h] = len(libres_hasta)
        uso_h = {}
        for k, lst in por_hueco.items():
            # ordenadas por x de origen para cruzar lo menos posible
            lst.sort(key=lambda e: caminos[e[0]]['xs'])
            for t, (ci, ti) in enumerate(lst):
                carril[(ci, ti)] = t
            uso_h[k] = len(lst)
        return carril, uso_p, uso_h

    origen = posicionar()
    for _ in range(30):
        caminos = rutear(origen)
        carril, uso_p, uso_h = asignar(caminos)
        nuevo_p = [max(alto_pasillo[h], PASILLO_BASE + EXTRA_PASILLO.get(h, 0) + CARRIL * uso_p.get(h, 0))
                   for h in range(n_filas + 1)]
        nuevo_h = {k: max(ancho_hueco[k], PASILLO_BASE + CARRIL * uso_h.get(k, 0)) for k in huecos_v}
        if nuevo_p == alto_pasillo and nuevo_h == ancho_hueco:
            break
        alto_pasillo[:] = nuevo_p
        ancho_hueco.update(nuevo_h)
        origen = posicionar()
    else:
        raise RuntimeError('los pasillos no se estabilizan')

    # --- geometría absoluta
    def y_pasillo(h, t):
        if h == 0:
            base = 0
        else:
            base = posicionar.bottoms[h - 1]
        return base + MARGEN_CAPA + 25 + t * CARRIL

    def x_hueco(f, g, t):
        gx = posicionar.gx[f][g]
        return gx - (uso_h.get((f, g), 1) - 1) * CARRIL / 2 + t * CARRIL

    cajas, cajas_notas, subpaq, marcos_bloque, ruta_notas = {}, {}, {}, {}, {}
    for b, (ox, oy) in origen.items():
        bl = bloques[b]
        for k, r in bl['cajas'].items():
            cajas[k] = mover(r, ox, oy)
        for k, r in bl['notas'].items():
            cajas_notas[k] = mover(r, ox, oy)
            ruta_notas[k] = [(x + ox, y + oy) for x, y in bl['rutas_notas'][k]]
        for ruta, r in bl['subpaq'].items():
            subpaq[(b[0], prefijo[b] + ruta)] = mover(r, ox, oy)
        marcos_bloque[(b[0], prefijo[b])] = (ox, oy, bl['ancho'], bl['alto'])

    rutas = {}
    for b, (ox, oy) in origen.items():
        for eid, pts in bloques[b]['rutas'].items():
            rutas[eid] = [(x + ox, y + oy) for x, y in pts]

    puntos_cruzadas = {}
    for ci, cam in enumerate(caminos):
        c = cam['c']
        ro = rutas[c['po']['id'] + '_r']
        if c['pd']['id'] is None:
            bd = c['bd']
            if bd[0] == 'CAPA':
                top = min(origen[b][1] for b in origen if b[0] == bd[1]) - TITULO_CAPA
            else:
                top = origen[bd][1]
            rd = [(cam['xt'], top)]
        else:
            rd = rutas[c['pd']['id'] + '_r']
        # la ruta interna del puerto de origen va clase->puerto (abajo) o puerto->clase (arriba)
        tramo_o = ro if c['po']['lado'] == 'abajo' else ro[::-1]
        tramo_d = rd[::-1] if c['pd']['lado'] == 'abajo' and c['pd']['id'] else rd
        pts = list(tramo_o)
        x, y = pts[-1]
        for ti, tr in enumerate(cam['tramos']):
            t = carril[(ci, ti)]
            if tr[0] == 'h':
                yy = y_pasillo(tr[1], t)
                pts.append((x, yy))
                x = tr[3]
                pts.append((x, yy))
            else:
                x = x_hueco(tr[1], tr[2], t)
                pts.append((x, pts[-1][1]))
        pts.append((x, tramo_d[0][1]))
        pts.extend(tramo_d)
        puntos_cruzadas[c['i']] = pts

    # paquetes que envuelven a varios bloques (por ejemplo controllers, con impl en otra fila)
    marcos_union = {}
    for (capa, pref), r in marcos_bloque.items():
        for i in range(1, len(pref)):
            clave = (capa, pref[:i])
            if clave in marcos_bloque:
                continue
            m = marcos_union.get(clave)
            l, t, w, h = r
            marcos_union[clave] = (l, t, l + w, t + h) if m is None else (
                min(m[0], l), min(m[1], t), max(m[2], l + w), max(m[3], t + h))
    for clave, m in marcos_union.items():
        marcos_bloque[clave] = (m[0] - MARGEN_BLOQUE, m[1] - TITULO_BLOQUE - MARGEN_BLOQUE,
                                m[2] - m[0] + 2 * MARGEN_BLOQUE, m[3] - m[1] + TITULO_BLOQUE + 2 * MARGEN_BLOQUE)
    marcos_capa = {}
    for b, r in marcos_bloque.items():
        capa = b[0]
        l, t, w, h = r
        m = marcos_capa.get(capa)
        if m is None:
            marcos_capa[capa] = [l, t, l + w, t + h]
        else:
            m[0], m[1], m[2], m[3] = min(m[0], l), min(m[1], t), max(m[2], l + w), max(m[3], t + h)
    marcos_capa = {(c,): (m[0] - MARGEN_CAPA, m[1] - TITULO_CAPA, m[2] - m[0] + 2 * MARGEN_CAPA,
                          m[3] - m[1] + TITULO_CAPA + MARGEN_CAPA) for c, m in marcos_capa.items()}
    return dict(ruta_notas=ruta_notas, cajas=cajas, notas=cajas_notas, subpaq=subpaq, marcos_bloque=marcos_bloque,
                marcos_capa=marcos_capa, rutas=rutas, cruzadas=puntos_cruzadas,
                ancho=posicionar.total, alto=posicionar.bottoms[-1] + alto_pasillo[-1])
