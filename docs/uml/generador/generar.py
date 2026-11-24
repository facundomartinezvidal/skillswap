"""Genera docs/SkillSwap.mdj: un diagrama de clases con todas las capas y una vista por capa.

- El modelo (clases, atributos, operaciones, herencia) sale del código con parser.py.
- Las relaciones del dominio están declaradas a mano (relaciones.py); las de las otras capas salen de los
  campos (asociaciones), de implements/extends y del uso en el código (dependencias de clases sueltas).
- El layout lo calcula Graphviz (dot) y las etiquetas de las relaciones (multiplicidades, nombres,
  estereotipos) se ubican acá para que no se superpongan con nada (ver etiquetas.py).
- verificar.py exporta el SVG con StarUML y comprueba que no haya superposiciones.

Desde la raíz del repo (necesita Graphviz: el comando dot):
    python3 docs/uml/generador/generar.py src/main/java/ar/edu/uade/skillswap docs/SkillSwap.mdj
    python3 docs/uml/generador/verificar.py docs/SkillSwap.mdj /tmp/SkillSwap.svg
"""
import json
import math
import tempfile
import re
import subprocess
import sys
from itertools import count
from pathlib import Path

from metricas import ancho
from parser import modelo
import relaciones
import etiquetas
import layout
import geometria

RAIZ, SALIDA = sys.argv[1], sys.argv[2]
T = modelo(RAIZ)
FUENTES = {k: Path(RAIZ, t['pkg'], (t['name'].split('.')[0]) + '.java') for k, t in T.items()}

# ------------------------------------------------------------------ capas y paquetes
CAPAS = [  # (clave, nombre visible, color capa, color módulo, color subpaquete)
    ('presentacion', 'Presentación', '#FFE5E5', '#FFF0F0', '#FFF7F7'),
    ('controllers', 'Controllers (adaptadores de interfaz)', '#FFE5E5', '#FFF0F0', '#FFF7F7'),
    ('aplicacion', 'Aplicación', '#E3ECFF', '#EEF3FF', '#F6F9FF'),
    ('dominio', 'Dominio', '#DFFFDF', '#ECFFEC', '#F5FFF5'),
    ('infraestructura', 'Infraestructura', '#FFE8DF', '#FFF1EB', '#FFF8F5'),
]
CAPA_IDX = {c[0]: i for i, c in enumerate(CAPAS)}


def ubicar(pkg):
    """Paquete Java relativo -> (capa, ruta de paquetes dentro de la capa)."""
    if pkg in ('', '.'):
        return 'infraestructura', ['skillswap (main)']
    partes = pkg.split('/')
    if partes[0] in ('ui', 'mock'):
        return 'presentacion', partes
    if partes[0] in ('controllers', 'model'):
        return 'controllers', partes
    capa = {'application': 'aplicacion', 'domain': 'dominio', 'infrastructure': 'infraestructura'}[partes[0]]
    return capa, partes[1:] or [partes[0]]


for k, t in T.items():
    t['capa'], t['ruta'] = ubicar(t['pkg'])
    # bloque de layout: el módulo; controllers.impl va en su propio bloque, en otra fila
    if t['capa'] == 'controllers' and t['ruta'][:2] == ['controllers', 'impl']:
        t['bloque'], t['prof'] = ('controllers', 'controllers/impl'), 2
    else:
        t['bloque'], t['prof'] = (t['capa'], t['ruta'][0]), 1


# ------------------------------------------------------------------ resolución de nombres
por_nombre = {}
for k, t in T.items():
    por_nombre.setdefault(t['name'], []).append(k)
    if t['simple'] != t['name']:
        por_nombre.setdefault(t['simple'], []).append(k)


BASE = 'ar.edu.uade.skillswap'
_imports = {}


def imports_de(k):
    f = str(FUENTES[k])
    if f not in _imports:
        _imports[f] = re.findall(r'(?m)^import\s+(?!static)([\w.]+(?:\.\*)?)\s*;', Path(f).read_text())
    return _imports[f]


def nombre_completo(k):
    pkg = T[k]['pkg'].strip('./')
    return '.'.join(x for x in (BASE, pkg.replace('/', '.'), T[k]['name']) if x)


def resolver(nombre, desde):
    """Como Java: import explícito, después el mismo paquete, después import con *, después la misma capa."""
    cands = por_nombre.get(nombre, [])
    if not cands:
        return None
    if len(cands) == 1:
        return cands[0]
    t0 = T[desde]
    imps = imports_de(desde)
    for c in cands:
        if nombre_completo(c) in imps:
            return c
    for c in cands:
        if T[c]['pkg'] == t0['pkg']:
            return c
    for c in cands:
        fq = nombre_completo(c)
        if any(i.endswith('.*') and fq.startswith(i[:-1]) and '.' not in fq[len(i) - 1:] for i in imps):
            return c
    for c in cands:
        if T[c]['capa'] == t0['capa']:
            return c
    return cands[0]


def tipos_en(tipo):
    return re.findall(r'[A-Z][\w]*(?:\.[A-Z]\w*)?', tipo)


# ------------------------------------------------------------------ relaciones
R = []  # dict(o, d, tipo, nombre, mo, md, est)


def rel(o, d, tipo, nombre='', mo='', md='', est=''):
    if o not in T or d not in T or o == d:
        raise KeyError((o, d))
    if any(r['o'] == o and r['d'] == d and r['tipo'] == tipo and r['nombre'] == nombre for r in R):
        return
    R.append(dict(o=o, d=d, tipo=tipo, nombre=nombre, mo=mo, md=md, est=est))


relaciones.dominio(rel)

# herencia y realización (todas las capas)
for k, t in T.items():
    for i in t['implements']:
        d = resolver(i, k)
        if d:
            rel(k, d, 'real')
    for e in t['extends']:
        d = resolver(e, k)
        if d:
            rel(k, d, 'real' if T[k]['kind'] == 'interface' and False else 'gen')

# asociaciones desde los campos (fuera del dominio, que está curado): solo hacia clases e interfaces
POR_ASOCIACION = {(r['o'], r['nombre']) for r in R if r['tipo'] in ('assoc', 'comp', 'agg')}
CABLEADO = {'BackendEnMemoria', 'BackendJpa', 'DatosDemo', 'SkillSwapControllers'}
for k, t in T.items():
    if t['capa'] == 'dominio' or k in CABLEADO:
        continue
    for a in t['attributes']:
        tipo = a['type']
        coleccion = re.match(r'(List|Set|Collection|Map|Iterable)<', tipo)
        opcional = tipo.startswith('Optional<')
        cands = [resolver(x, k) for x in tipos_en(tipo)]
        cands = [c for c in cands if c and T[c]['kind'] in ('class', 'interface') and c != k]
        if not cands:
            continue
        d = cands[-1]
        md = '0..*' if coleccion else ('0..1' if opcional else '1')
        comp = coleccion and T[k]['name'].endswith('Entity') and T[d]['name'].endswith('Entity')
        inversa = next((r for r in R if r['o'] == d and r['d'] == k and r['tipo'] in ('assoc', 'comp')), None)
        if inversa:  # el otro lado ya la tiene: es una sola asociación bidireccional
            inversa['mo'] = md
            inversa['nombre'] = f"{inversa['nombre']} / {a['name']}"
            inversa['bidir'] = True
        else:
            rel(k, d, 'comp' if comp else 'assoc', a['name'], '1' if comp else '0..*', md)
        POR_ASOCIACION.add((k, a['name']))

relaciones.extra(rel)

# raíz de composición: una dependencia «create» al paquete que arma (el módulo si es de su capa, si no la capa)
for k in sorted(CABLEADO):
    destinos = []
    for a in T[k]['attributes']:
        for x in tipos_en(a['type']):
            d = resolver(x, k)
            if not d or T[d]['kind'] not in ('class', 'interface'):
                continue
            if T[d]['capa'] == T[k]['capa']:
                if T[d]['ruta'][0] == T[k]['ruta'][0]:
                    continue
                pk = ('PKG', T[d]['capa'], T[d]['ruta'][0])
            else:
                pk = ('PKG', T[d]['capa'], None)
            if pk not in destinos:
                destinos.append(pk)
    for pk in destinos:
        R.append(dict(o=k, d=pk, tipo='dep', nombre='', mo='', md='', est='create'))


# clases sueltas: dependencia «use» desde quien las usa en el código (o hacia lo que usan)
def conectadas():
    c = set()
    for r in R:
        c.add(r['o'])
        if not isinstance(r['d'], tuple):
            c.add(r['d'])
    return c


def es_paquete(x):
    return isinstance(x, tuple)


def clave_paq(x):
    return (x[1], (x[2],) if x[2] else ())


textos = {}
for k, f in FUENTES.items():
    textos.setdefault(str(f), re.sub(r'(?m)^(import|package) .*$', '', f.read_text()))
superiores_archivo = {}
for k, t in T.items():
    if '.' not in t['name']:
        superiores_archivo[str(FUENTES[k])] = k

for k in sorted(set(T) - conectadas()):
    t = T[k]
    usuarios = []
    for f, txt in textos.items():
        if f == str(FUENTES[k]) and '.' not in t['name']:
            continue
        if re.search(r'\b' + re.escape(t['simple']) + r'\b', txt):
            u = superiores_archivo.get(f)
            if u and u != k and (t['capa'] != 'dominio' or T[u]['capa'] == 'dominio'):
                usuarios.append(u)
    usuarios.sort(key=lambda u: (T[u]['capa'] != t['capa'], abs(CAPA_IDX[T[u]['capa']] - CAPA_IDX[t['capa']]),
                                 T[u]['pkg'] != t['pkg'], u))
    if usuarios:
        rel(usuarios[0], k, 'dep', est='use')
    else:
        txt = textos[str(FUENTES[k])]
        usados = []
        for m in re.finditer(r'\bnew ([A-Z]\w*)', txt):
            d = resolver(m.group(1), k)
            if d and d != k and d not in usados:
                usados.append(d)
        for d in usados[:5]:
            rel(k, d, 'dep', est='use')

sueltas = sorted(set(T) - conectadas())
if sueltas:
    print('AVISO clases sin relaciones:', sueltas)

# ------------------------------------------------------------------ estereotipos
ESTEREOTIPO = relaciones.estereotipos(T)
PATRONES = {'State', 'Strategy', 'Decorator', 'Simple Factory', 'Observer', 'Adapter'}

# ------------------------------------------------------------------ contenido de las cajas
VIS = {'private': '-', 'public': '+', 'protected': '#', 'package': '~'}


def es_getter(t, o):
    return not o['params'] and any(a['name'] == o['name'] for a in t['attributes'])


def operaciones_visibles(k, t):
    ops = [o for o in t['operations'] if o['name'] not in ('reconstruir', 'equals', 'hashCode', 'toString')
           and not es_getter(t, o)]
    est = ESTEREOTIPO.get(k)
    if t['capa'] == 'dominio' and est == 'State' and t['kind'] == 'record':
        ops = [o for o in ops if o['name'] != 'nombre']
    if est == 'Domain Event' and t['kind'] == 'record':
        ops = []
    if t['name'].endswith('Exception'):
        ops = []
    return ops


def atributos_visibles(k, t):
    if t['name'].endswith('Exception'):
        return []
    return [a for a in t['attributes'] if (k, a['name']) not in POR_ASOCIACION]


def firma(o):
    return (f"{VIS[o.get('visibility', 'public')]}{o['name']}("
            + ', '.join(f"{p['name']}: {p['type']}" for p in o['params']) + f"): {o['returns']}")


def texto_attr(a):
    return f"{VIS[a['visibility']]}{a['name']}: {a['type']}" + (' {readOnly}' if a.get('readOnly') else '')


# ------------------------------------------------------------------ modelo StarUML
ids = count(1)


def nid():
    return f'SKS{next(ids):06d}'


def ref(x):
    return {'$ref': x}


proyecto = {'_type': 'Project', '_id': nid(), 'name': 'SkillSwap', 'ownedElements': []}
raiz = {'_type': 'UMLModel', '_id': nid(), '_parent': ref(proyecto['_id']), 'name': 'SkillSwap', 'ownedElements': []}
proyecto['ownedElements'].append(raiz)
paquetes = {}


def paquete(capa, ruta):
    clave = (capa, tuple(ruta))
    if clave in paquetes:
        return paquetes[clave]
    if not ruta:
        nombre = next(c[1] for c in CAPAS if c[0] == capa)
        padre = raiz
    else:
        nombre = ruta[-1]
        padre = paquete(capa, ruta[:-1])
    p = {'_type': 'UMLPackage', '_id': nid(), '_parent': ref(padre['_id']), 'name': nombre, 'ownedElements': []}
    padre['ownedElements'].append(p)
    paquetes[clave] = p
    return p


for c in CAPAS:
    paquete(c[0], [])

elementos = {}
for k, t in T.items():
    pkg = paquete(t['capa'], t['ruta'])
    tipo = {'class': 'UMLClass', 'record': 'UMLClass', 'interface': 'UMLInterface', 'enum': 'UMLEnumeration'}[t['kind']]
    e = {'_type': tipo, '_id': nid(), '_parent': ref(pkg['_id']), 'name': t['name'] + t['generic']}
    if k in ESTEREOTIPO:
        e['stereotype'] = ESTEREOTIPO[k]
    if t['abstract']:
        e['isAbstract'] = True
    attrs = atributos_visibles(k, t)
    if attrs:
        e['attributes'] = [{'_type': 'UMLAttribute', '_id': nid(), '_parent': ref(e['_id']), 'name': a['name'],
                            'visibility': a['visibility'], 'type': a['type'],
                            **({'isReadOnly': True} if a.get('readOnly') else {})} for a in attrs]
    ops = operaciones_visibles(k, t)
    if ops:
        e['operations'] = []
        for o in ops:
            op = {'_type': 'UMLOperation', '_id': nid(), '_parent': ref(e['_id']), 'name': o['name'],
                  'visibility': o.get('visibility', 'public'), 'parameters': []}
            if o['static']:
                op['isStatic'] = True
            if o.get('abstract'):
                op['isAbstract'] = True
            for p in o['params']:
                op['parameters'].append({'_type': 'UMLParameter', '_id': nid(), '_parent': ref(op['_id']),
                                         'name': p['name'], 'type': p['type']})
            op['parameters'].append({'_type': 'UMLParameter', '_id': nid(), '_parent': ref(op['_id']),
                                     'type': o['returns'], 'direction': 'return'})
            e['operations'].append(op)
    if t['literals']:
        e['literals'] = [{'_type': 'UMLEnumerationLiteral', '_id': nid(), '_parent': ref(e['_id']), 'name': lit}
                         for lit in t['literals']]
    pkg['ownedElements'].append(e)
    elementos[k] = e
    cab = []
    if t['kind'] == 'interface':
        cab.append(('«interface»', False))
    elif t['kind'] == 'enum':
        cab.append(('«enumeration»', False))
    elif k in ESTEREOTIPO:
        cab.append((f'«{ESTEREOTIPO[k]}»', False))
    cab.append((e['name'], True))
    e['_cab'] = cab
    e['_attrs'] = [texto_attr(a) for a in attrs]
    e['_lits'] = list(t['literals'])
    e['_ops'] = [firma(o) for o in ops]

for r in R:
    o = elementos[r['o']]
    d = paquetes[clave_paq(r['d'])] if es_paquete(r['d']) else elementos[r['d']]
    if r['tipo'] in ('assoc', 'comp', 'agg'):
        m = {'_type': 'UMLAssociation', '_id': nid(), '_parent': ref(o['_id']), 'name': r['nombre']}
        m['end1'] = {'_type': 'UMLAssociationEnd', '_id': nid(), '_parent': ref(m['_id']), 'reference': ref(o['_id']),
                     'navigable': 'navigable' if r.get('bidir') else 'unspecified', 'multiplicity': r['mo']}
        if r['tipo'] == 'comp':
            m['end1']['aggregation'] = 'composite'
        if r['tipo'] == 'agg':
            m['end1']['aggregation'] = 'shared'
        m['end2'] = {'_type': 'UMLAssociationEnd', '_id': nid(), '_parent': ref(m['_id']), 'reference': ref(d['_id']),
                     'navigable': 'navigable', 'multiplicity': r['md']}
    elif r['tipo'] in ('real', 'gen'):
        tipo = 'UMLInterfaceRealization' if r['tipo'] == 'real' else 'UMLGeneralization'
        m = {'_type': tipo, '_id': nid(), '_parent': ref(o['_id']), 'source': ref(o['_id']), 'target': ref(d['_id'])}
    else:
        m = {'_type': 'UMLDependency', '_id': nid(), '_parent': ref(o['_id']), 'source': ref(o['_id']),
             'target': ref(d['_id'])}
        if r['est']:
            m['stereotype'] = r['est']
    o.setdefault('ownedElements', []).append(m)
    r['modelo'] = m

# ------------------------------------------------------------------ medidas (calibradas contra StarUML)
LINEA = 15


def compartimento(lineas):
    return 10 if not lineas else 7 + LINEA * len(lineas)


def medida(k, resumida=False):
    """Tamaño de la caja; resumida: solo el nombre (las clases de otra capa en las vistas por capa)."""
    e = elementos[k]
    if resumida:
        return round(max(ancho(x, bold=b) for x, b in e['_cab']) + 14), 10 + LINEA * len(e['_cab']) + 2
    w = max([ancho(x, bold=b) for x, b in e['_cab']] + [ancho(x) for x in e['_attrs'] + e['_ops'] + e['_lits']])
    h = 10 + LINEA * len(e['_cab'])
    if e['_type'] == 'UMLEnumeration':
        h += compartimento(e['_lits'])
    h += compartimento(e['_attrs']) + compartimento(e['_ops'])
    return round(w + 14), h + 2


NOTAS = relaciones.notas()


def medida_nota(txt):
    lineas = txt.split('\n')
    return round(max(ancho(x) for x in lineas) + 24), 15 * len(lineas) + 16


# ------------------------------------------------------------------ layout
FILAS = [
    [('infraestructura', 'skillswap (main)'), ('infraestructura', 'configuracion')],
    [('presentacion', 'ui'), ('presentacion', 'mock'), ('infraestructura', 'persistencia'),
     ('infraestructura', 'memoria'), ('infraestructura', 'notificacion'), ('infraestructura', 'identidad')],
    [('controllers', 'controllers'), ('controllers', 'model')],
    [('controllers', 'controllers/impl')],
    [('aplicacion', 'publicacion'), ('aplicacion', 'contratacion'), ('aplicacion', 'busqueda'),
     ('aplicacion', 'perfil'), ('aplicacion', 'wallet'), ('aplicacion', 'notificacion')],
    [('aplicacion', 'evento'), ('aplicacion', 'shared')],
    [('dominio', 'publicacion'), ('dominio', 'contratacion'), ('dominio', 'perfil'), ('dominio', 'wallet'),
     ('dominio', 'notificacion')],
    [('dominio', 'shared')],
]
ALINEAR = {0: 'derecha', 1: 'derecha', 2: 'izquierda', 3: 'izquierda'}
# lugar extra arriba de impl y abajo de controllers: ahí van el marco y el título del paquete que los envuelve
EXTRA_PASILLO = {2: 80, 4: 60}
# Filas de cada vista por capa: la capa en varias filas, para que no quede una sola tira horizontal. Debajo va
# una fila más con las clases de otras capas que esta usa (el contexto).
FILAS_CAPA = {
    'presentacion': [['ui', 'mock']],
    'controllers': [['controllers', 'model'], ['controllers/impl']],
    'aplicacion': [['publicacion', 'contratacion', 'busqueda'], ['perfil', 'wallet', 'notificacion', 'evento', 'shared']],
    'dominio': [['publicacion', 'contratacion'], ['perfil', 'wallet', 'notificacion', 'shared']],
    'infraestructura': [['skillswap (main)', 'configuracion'], ['persistencia', 'memoria', 'notificacion', 'identidad']],
}


def arriba_abajo(r):
    """Sentido para dot: interfaz/padre/todo arriba."""
    return (r['d'], r['o']) if r['tipo'] in ('real', 'gen') else (r['o'], r['d'])


def dibujar(titulo, clases, R, NOTAS, FILAS, ALINEAR, EXTRA_PASILLO, resumidas=frozenset()):
    """Arma un UMLClassDiagram con esas clases y relaciones; devuelve (diagrama, geometría prevista).

    resumidas: clases que se dibujan solo con el nombre (contexto de otra capa en una vista por capa).
    """
    med = {k: medida(k, k in resumidas) for k in clases}
    # una caja con muchas relaciones necesita lado suficiente para que cada línea llegue a su propio punto
    grado = {k: 0 for k in clases}
    for r in R:
        grado[r['o']] += 1
        if not isinstance(r['d'], tuple):
            grado[r['d']] += 1
    for k in clases:
        w, h = med[k]
        med[k] = (max(w, 24 * math.ceil(grado[k] * 0.75) + 40), h)

    L = layout.componer({k: T[k] for k in clases}, R, med, NOTAS, medida_nota, FILAS, ALINEAR, arriba_abajo,
                        EXTRA_PASILLO=EXTRA_PASILLO)
    cajas = {k: tuple(round(v) for v in L['cajas'][k]) for k in clases}
    cajas_notas = {k: tuple(round(v) for v in r) for k, r in L['notas'].items()}
    cajas_paq = {}
    cajas_paq.update(L['marcos_capa'] and {(k[0], ()): v for k, v in L['marcos_capa'].items()})
    cajas_paq.update(L['marcos_bloque'])
    cajas_paq.update(L['subpaq'])


    def recortar(caja, hacia):
        """Punto donde el segmento centro->hacia sale de la caja (lo mismo que hace StarUML)."""
        l, t, w, h = caja
        cx, cy = l + w / 2, t + h / 2
        dx, dy = hacia[0] - cx, hacia[1] - cy
        if dx == 0 and dy == 0:
            return cx, cy
        escalas = []
        if dx:
            escalas.append((w / 2) / abs(dx))
        if dy:
            escalas.append((h / 2) / abs(dy))
        s = min(escalas)
        return cx + dx * s, cy + dy * s


    def limpiar_puntos(pts):
        out = []
        for p in pts:
            p = (round(p[0]), round(p[1]))
            if out and p == out[-1]:
                continue
            out.append(p)
        # sin puntos intermedios colineales
        i = 1
        while i < len(out) - 1:
            a, b, c = out[i - 1], out[i], out[i + 1]
            if (b[0] - a[0]) * (c[1] - b[1]) == (b[1] - a[1]) * (c[0] - b[0]):
                out.pop(i)
            else:
                i += 1
        return out


    aristas = []
    for i, r in enumerate(R):
        if i in L['cruzadas']:
            xy = L['cruzadas'][i]
        else:
            xy = L['rutas'][f'e{i}']
            if r['tipo'] in ('real', 'gen'):
                xy = xy[::-1]
        co = cajas[r['o']]
        cd = tuple(cajas_paq[clave_paq(r['d'])]) if es_paquete(r['d']) else cajas[r['d']]
        medio = xy[1:-1]
        if not medio:
            medio_o = (cd[0] + cd[2] / 2, cd[1] + cd[3] / 2)
            medio_d = (co[0] + co[2] / 2, co[1] + co[3] / 2)
            puntos = [recortar(co, medio_o), recortar(cd, medio_d)]
        else:
            puntos = [recortar(co, medio[0])] + medio + [recortar(cd, medio[-1])]
        aristas.append(dict(r=r, puntos=limpiar_puntos(puntos)))

    # ------------------------------------------------------------------ desvíos alrededor de los títulos de paquetes
    def titulo_paquete(clave, r):
        l, t, w, h = r
        nombre = clave[1][-1] if clave[1] else next(c[1] for c in CAPAS if c[0] == clave[0])
        tw = ancho(nombre, bold=True)
        return (l + w / 2 - tw / 2 - 6, t + 20, tw + 12, 19)


    TITULOS = [titulo_paquete(k, r) for k, r in cajas_paq.items()]


    def cruce(r, p, q):
        x0, y0, x1, y1 = r[0], r[1], r[0] + r[2], r[1] + r[3]
        dx, dy = q[0] - p[0], q[1] - p[1]
        t0, t1 = 0.0, 1.0
        for pp, qq in ((-dx, p[0] - x0), (dx, x1 - p[0]), (-dy, p[1] - y0), (dy, y1 - p[1])):
            if pp == 0:
                if qq < 0:
                    return None
            else:
                t = qq / pp
                if pp < 0:
                    t0 = max(t0, t)
                else:
                    t1 = min(t1, t)
                if t0 > t1:
                    return None
        if t0 <= 0 or t1 >= 1:
            return None  # un extremo adentro: no se desvía
        return t0, t1


    def perimetro(r, p):
        x0, y0, w, h = r
        x, y = p
        if abs(y - y0) < 1e-6:
            return x - x0
        if abs(x - (x0 + w)) < 1e-6:
            return w + (y - y0)
        if abs(y - (y0 + h)) < 1e-6:
            return w + h + (x0 + w - x)
        return 2 * w + h + (y0 + h - y)


    def esquinas(r):
        x0, y0, w, h = r
        return [(0, (x0, y0)), (w, (x0 + w, y0)), (w + h, (x0 + w, y0 + h)), (2 * w + h, (x0, y0 + h))]


    USO_TITULO = {}


    def desviar(pts, propios=()):
        usados = set()
        for _ in range(40):
            cambio = False
            for i in range(len(pts) - 1):
                p, q = pts[i], pts[i + 1]
                for ti, r0 in enumerate(OBSTACULOS_RUTA):
                    if ti in propios:
                        continue
                    if ti in usados:
                        continue
                    k = USO_TITULO.get(ti, 0)
                    r = (r0[0] - 8 * k, r0[1] - 8 * k, r0[2] + 16 * k, r0[3] + 16 * k)
                    c = cruce(r, p, q)
                    if not c:
                        continue
                    e = (p[0] + c[0] * (q[0] - p[0]), p[1] + c[0] * (q[1] - p[1]))
                    x = (p[0] + c[1] * (q[0] - p[0]), p[1] + c[1] * (q[1] - p[1]))
                    per = 2 * (r[2] + r[3])
                    se, sx = perimetro(r, e), perimetro(r, x)
                    adelante = (sx - se) % per
                    if adelante <= per / 2:
                        camino = [pt for d, pt in sorted(esquinas(r)) if 0 < (d - se) % per < adelante]
                        camino.sort(key=lambda pt: (perimetro(r, pt) - se) % per)
                    else:
                        atras = per - adelante
                        camino = [pt for d, pt in sorted(esquinas(r)) if 0 < (se - d) % per < atras]
                        camino.sort(key=lambda pt: (se - perimetro(r, pt)) % per)
                    pts = pts[:i + 1] + [e] + camino + [x] + pts[i + 1:]
                    USO_TITULO[ti] = k + 1
                    usados.add(ti)
                    cambio = True
                    break
                if cambio:
                    break
            if not cambio:
                break
        return [(round(a), round(b)) for a, b in pts]


    OBSTACULOS_RUTA = list(TITULOS)
    indice_caja = {}
    for k in clases:
        l, t, w, h = cajas[k]
        indice_caja[k] = len(OBSTACULOS_RUTA)
        OBSTACULOS_RUTA.append((l - 10, t - 10, w + 20, h + 20))
    for k, (l, t, w, h) in cajas_notas.items():
        indice_caja['nota:' + k] = len(OBSTACULOS_RUTA)
        cajas['nota:' + k] = cajas_notas[k]
        OBSTACULOS_RUTA.append((l - 10, t - 10, w + 20, h + 20))
    # las uniones de las notas pasan por los mismos pasos que las relaciones (desvíos, separación, extremos)
    for k in cajas_notas:
        ruta = L['ruta_notas'][k][::-1]  # nota -> clase
        medio = ruta[1:-1]
        cn, cc = cajas_notas[k], cajas[k]
        if medio:
            pts = [recortar(cn, medio[0])] + medio + [recortar(cc, medio[-1])]
        else:
            pts = [recortar(cn, (cc[0] + cc[2] / 2, cc[1] + cc[3] / 2)), recortar(cc, (cn[0] + cn[2] / 2, cn[1] + cn[3] / 2))]
        aristas.append(dict(r=dict(o='nota:' + k, d=k, tipo='nota', nombre='', mo='', md='', est=''),
                            puntos=limpiar_puntos(pts)))
    for a in aristas:
        r = a['r']
        propios = {indice_caja[r['o']]} | ({indice_caja[r['d']]} if not es_paquete(r['d']) else set())
        a['puntos'] = desviar(a['puntos'], propios)

    # ------------------------------------------------------------------ separar tramos encimados de relaciones distintas
    def encimados(a, b):
        return max(_encimados(a, b), _encimados(b, a))


    def _encimados(a, b):
        """Tramos sobre la misma recta (a menos de 7 px) que se superponen: cuánto se superponen."""
        (p, q), (r, t) = a, b
        L = math.dist(p, q)
        if L < 4 or math.dist(r, t) < 4:
            return 0
        ux, uy = (q[0] - p[0]) / L, (q[1] - p[1]) / L
        if abs((r[0] - p[0]) * uy - (r[1] - p[1]) * ux) > 7 or abs((t[0] - p[0]) * uy - (t[1] - p[1]) * ux) > 7:
            return 0
        s1 = (r[0] - p[0]) * ux + (r[1] - p[1]) * uy
        s2 = (t[0] - p[0]) * ux + (t[1] - p[1]) * uy
        return min(L, max(s1, s2)) - max(0, min(s1, s2))


    def separar(lista):
        for vuelta in range(30):
            cambios = 0
            segs = []
            for ei, pts in enumerate(lista):
                for si, (p, q) in enumerate(zip(pts, pts[1:])):
                    segs.append((ei, si, p, q))
            grilla = {}
            for sg in segs:
                p, q = sg[2], sg[3]
                for cx in range(int(min(p[0], q[0]) // 200), int(max(p[0], q[0]) // 200) + 1):
                    for cy in range(int(min(p[1], q[1]) // 200), int(max(p[1], q[1]) // 200) + 1):
                        grilla.setdefault((cx, cy), []).append(sg)
            mover = {}
            for lst in grilla.values():
                for i, a in enumerate(lst):
                    for b in lst[i + 1:]:
                        if a[0] == b[0] or encimados((a[2], a[3]), (b[2], b[3])) <= 5:
                            continue
                        ei, si = max(a, b, key=lambda x: x[0])[:2]
                        mover.setdefault(ei, set()).add(si)
            # vértices intermedios compartidos por varias relaciones (un "nudo"): se corren los de las siguientes
            vistos_v = {}
            for ei, pts in enumerate(lista):
                for j in range(1, len(pts) - 1):
                    clave = (round(pts[j][0] / 6), round(pts[j][1] / 6))
                    cerca = [c for c in ((clave[0] + a, clave[1] + b) for a in (-1, 0, 1) for b in (-1, 0, 1))
                             if c in vistos_v and vistos_v[c] != ei]
                    if cerca:
                        mover.setdefault(ei, set()).add(j - 1)
                    else:
                        vistos_v[clave] = ei
            for ei, sis in mover.items():
                pts = list(lista[ei])
                for si in sorted(sis):
                    p, q = pts[si], pts[si + 1]
                    L = math.dist(p, q) or 1
                    nx, ny = -(q[1] - p[1]) / L * 13, (q[0] - p[0]) / L * 13
                    if si + 1 == len(pts) - 1 and si == 0:  # recta de dos puntos: se agrega un quiebre
                        m = ((p[0] + q[0]) / 2 + nx, (p[1] + q[1]) / 2 + ny)
                        pts = [p, m, q]
                        break
                    for j in (si, si + 1):
                        if 0 < j < len(pts) - 1:
                            pts[j] = (pts[j][0] + nx, pts[j][1] + ny)
                lista[ei] = [(round(x), round(y)) for x, y in pts]
                cambios += 1
            if not cambios:
                return vuelta
        return -1


    _rutas = [a['puntos'] for a in aristas]
    vueltas = separar(_rutas)
    for a, pts in zip(aristas, _rutas):
        r = a['r']
        propios = {indice_caja[r['o']]} | ({indice_caja[r['d']]} if not es_paquete(r['d']) else set())
        a['puntos'] = desviar(pts, propios)
    print('separación de tramos encimados:', 'ok' if vueltas >= 0 else 'quedan encimados')


    # ------------------------------------------------------------------ cada línea con su propio punto en el borde de la caja
    def lado(caja, p):
        l, t, w, h = caja
        d = {'arriba': abs(p[1] - t), 'abajo': abs(p[1] - (t + h)), 'izq': abs(p[0] - l), 'der': abs(p[0] - (l + w))}
        return min(d, key=d.get)


    def repartir_extremos():
        extremos = {}
        for ai, a in enumerate(aristas):
            r = a['r']
            pts = a['puntos']
            a['puntos'] = pts = [pts[0], *pts[1:-1], pts[-1]]
            if len(pts) < 2:
                continue
            # extremos tal como los va a calcular StarUML (centro de la caja -> punto vecino)
            caja_o = cajas[r['o']]
            caja_d = tuple(cajas_paq[clave_paq(r['d'])]) if es_paquete(r['d']) else cajas[r['d']]
            if len(pts) == 2:  # recta: sobre la línea de centro a centro, así es estable
                co = (caja_o[0] + caja_o[2] / 2, caja_o[1] + caja_o[3] / 2)
                cd = (caja_d[0] + caja_d[2] / 2, caja_d[1] + caja_d[3] / 2)
                pts[0] = tuple(round(v) for v in recortar(caja_o, cd))
                pts[-1] = tuple(round(v) for v in recortar(caja_d, co))
            else:
                pts[0] = tuple(round(v) for v in recortar(caja_o, pts[1]))
                pts[-1] = tuple(round(v) for v in recortar(caja_d, pts[-2]))
            extremos.setdefault(r['o'], []).append((ai, 0))
            if not es_paquete(r['d']):
                extremos.setdefault(r['d'], []).append((ai, -1))
        for k, lst in extremos.items():
            caja = cajas[k]
            l, t, w, h = caja
            por_lado = {}
            for ai, idx in lst:
                p = aristas[ai]['puntos'][idx]
                por_lado.setdefault(lado(caja, p), []).append((ai, idx, p))
            for ld, items in por_lado.items():
                horizontal = ld in ('arriba', 'abajo')
                items.sort(key=lambda it: it[2][0] if horizontal else it[2][1])
                coords = [it[2][0] if horizontal else it[2][1] for it in items]
                ini, fin = (l + 16, l + w - 16) if horizontal else (t + 16, t + h - 16)
                if all(b - a >= 22 for a, b in zip(coords, coords[1:])) and all(ini <= c <= fin for c in coords):
                    continue
                n = len(items)
                paso = min(26, (fin - ini) / max(n - 1, 1))
                centro = sum(coords) / n
                base = min(max(centro - paso * (n - 1) / 2, ini), fin - paso * (n - 1))
                for j, (ai, idx, p) in enumerate(items):
                    c = base + j * paso
                    if horizontal:
                        afuera = (c, t - 16) if ld == 'arriba' else (c, t + h + 16)
                    else:
                        afuera = (l - 16, c) if ld == 'izq' else (l + w + 16, c)
                    afuera = (round(afuera[0]), round(afuera[1]))
                    pts = aristas[ai]['puntos']
                    if idx == 0:
                        pts.insert(1, afuera)
                        pts[0] = tuple(round(v) for v in recortar(caja, afuera))
                    else:
                        pts.insert(len(pts) - 1, afuera)
                        pts[-1] = tuple(round(v) for v in recortar(caja, afuera))


    repartir_extremos()


    def extremos_finales(a):
        """Los dos extremos como los calcula StarUML, después de todos los cambios de la ruta."""
        r = a['r']
        pts = [tuple(p) for p in a['puntos']]
        caja_o = cajas[r['o']]
        caja_d = tuple(cajas_paq[clave_paq(r['d'])]) if es_paquete(r['d']) else cajas[r['d']]
        if len(pts) == 2:
            co = (caja_o[0] + caja_o[2] / 2, caja_o[1] + caja_o[3] / 2)
            cd = (caja_d[0] + caja_d[2] / 2, caja_d[1] + caja_d[3] / 2)
            pts[0], pts[-1] = recortar(caja_o, cd), recortar(caja_d, co)
        else:
            pts[0], pts[-1] = recortar(caja_o, pts[1]), recortar(caja_d, pts[-2])
        return [(round(x), round(y)) for x, y in pts]


    for a in aristas:
        a['puntos'] = extremos_finales(a)
    for _ in range(8):  # separar y recalcular extremos hasta que no cambie nada
        _rutas = [list(a['puntos']) for a in aristas]
        antes = [list(x) for x in _rutas]
        separar(_rutas)
        for a, pts in zip(aristas, _rutas):
            a['puntos'] = pts
            a['puntos'] = extremos_finales(a)
        if [a['puntos'] for a in aristas] == antes:
            break
    else:
        print('AVISO: la separación final no se estabilizó')

    # ------------------------------------------------------------------ cruces rasantes y nudos: un quiebre que los evite
    def _segs_de(lista):
        out = []
        for ei, pts in enumerate(lista):
            for si, (p, q) in enumerate(zip(pts, pts[1:])):
                out.append((ei, si, tuple(p), tuple(q)))
        return out


    def _problemas_de(ei, pts, otros):
        """(tramos pegados a otras rutas, cajas ajenas cruzadas, vértices pegados a otras rutas)."""
        enc = cajas_ = nud = 0
        r = aristas[ei]['r']
        propios = {indice_caja[r['o']]} | ({indice_caja[r['d']]} if not es_paquete(r['d']) else set())
        for p, q in zip(pts, pts[1:]):
            for (ej, _, a, b) in otros:
                if ej != ei and geometria.encimados((p, q), (a, b), 6.0) > 20:
                    enc += 1
            for oi, ob in enumerate(CAJAS_REALES):
                if oi not in propios and etiquetas.segmento_cruza(ob, p, q):
                    cajas_ += 1
        for v in pts[1:-1]:
            for (ej, _, a, b) in otros:
                if ej != ei and (math.dist(v, a) < 8 or math.dist(v, b) < 8):
                    nud += 1
                    break
        return enc, cajas_, nud


    def _mejor(c, base):
        """c mejora a base: menos problemas en total sin empeorar las cajas ni los nudos."""
        return sum(c) < sum(base) and c[1] <= base[1] and c[2] <= base[2]


    # cajas y títulos reales (sin el margen de 10 px de los desvíos), achicados 2 px: un cruce de verdad
    CAJAS_REALES = [(o[0] + 12, o[1] + 12, o[2] - 24, o[3] - 24) if i >= len(TITULOS) else o
                    for i, o in enumerate(OBSTACULOS_RUTA)]


    def corregir_rasantes():
        lista = [list(a['puntos']) for a in aristas]
        segs = _segs_de(lista)
        malos = set()
        for i, (ei, si, p, q) in enumerate(segs):
            for (ej, sj, a, b) in segs[i + 1:]:
                if ei != ej and geometria.encimados((p, q), (a, b), 6.0) > 20:
                    malos.add(ei)
                    malos.add(ej)
        for v_ei, pts in enumerate(lista):
            for v in pts[1:-1]:
                for (ej, _, a, b) in segs:
                    if ej < v_ei and (math.dist(v, a) < 6 or math.dist(v, b) < 6):
                        malos.add(v_ei)
        arreglados = 0
        for ei in sorted(malos) + sorted(malos):
            pts = lista[ei]
            otros = [sg for sg in _segs_de(lista) if sg[0] != ei]
            actual = _problemas_de(ei, pts, otros)
            mejor = (actual, pts)
            if sum(actual) == 0:
                continue
            # 1) mover un vértice intermedio que ya existe
            for vj in range(1, len(pts) - 1):
                for dx, dy in [(0, 20), (0, -20), (20, 0), (-20, 0), (0, 40), (0, -40), (40, 0), (-40, 0),
                               (0, 70), (0, -70), (70, 0), (-70, 0), (30, 30), (-30, 30), (30, -30), (-30, -30),
                               (0, 110), (0, -110), (0, 160), (0, -160), (0, 220), (0, -220)]:
                    cand = list(pts)
                    cand[vj] = (pts[vj][0] + dx, pts[vj][1] + dy)
                    c = _problemas_de(ei, cand, otros)
                    if _mejor(c, mejor[0]):
                        mejor = (c, cand)
                if sum(mejor[0]) == 0:
                    break
            # 2) cambiar un tramo diagonal por una L (horizontal + vertical)
            for si in range(len(pts) - 1):
                if sum(mejor[0]) == 0:
                    break
                p, q = pts[si], pts[si + 1]
                if abs(p[0] - q[0]) < 20 or abs(p[1] - q[1]) < 3:
                    continue
                for codo in ((q[0], p[1]), (p[0], q[1])):
                    cand = pts[:si + 1] + [codo] + pts[si + 1:]
                    c = _problemas_de(ei, cand, otros)
                    if _mejor(c, mejor[0]):
                        mejor = (c, cand)
            # 3) agregar un quiebre
            for si in range(len(pts) - 1):
                if sum(mejor[0]) == 0:
                    break
                p, q = pts[si], pts[si + 1]
                L = math.dist(p, q)
                if L < 30:
                    continue
                nx, ny = -(q[1] - p[1]) / L, (q[0] - p[0]) / L
                for f in (0.5, 0.3, 0.7):
                    m = (p[0] + f * (q[0] - p[0]), p[1] + f * (q[1] - p[1]))
                    for d in (20, -20, 40, -40, 60, -60, 80, -80, 150, -150, 250, -250, 400, -400):
                        cand = pts[:si + 1] + [(round(m[0] + nx * d), round(m[1] + ny * d))] + pts[si + 1:]
                        c = _problemas_de(ei, cand, otros)
                        if _mejor(c, mejor[0]):
                            mejor = (c, cand)
                    if sum(mejor[0]) == 0:
                        break
            if mejor[1] is not pts:
                lista[ei] = mejor[1]
                arreglados += 1
        for a, pts in zip(aristas, lista):
            a['puntos'] = pts
            a['puntos'] = extremos_finales(a)
        return len(malos), arreglados


    print('cruces rasantes y nudos: %d rutas con problema, %d corregidas' % corregir_rasantes())

    def fijar_extremos():
        """StarUML pone el extremo donde la recta centro->primer quiebre corta el borde. Si el quiebre quedó
        lejos (por la separación de tramos), los extremos de un mismo lado se amontonan hacia el centro y las
        multiplicidades se leen ambiguas. Se agrega un quiebre corto sobre la recta centro->extremo buscado
        (el punto del borde frente al quiebre), así el extremo queda donde se previó."""
        fijados = 0
        # solo donde hace falta: extremos de asociaciones (llevan multiplicidad) amontonados en la misma caja
        ext = {}
        for ai, a in enumerate(aristas):
            if a['r']['tipo'] in ('assoc', 'comp', 'agg') and len(a['puntos']) >= 3:
                ext.setdefault(a['r']['o'], []).append((ai, 0, a['puntos'][0]))
                if not es_paquete(a['r']['d']):
                    ext.setdefault(a['r']['d'], []).append((ai, -1, a['puntos'][-1]))
        amontonados = {(ai, idx) for lst in ext.values() for ai, idx, p in lst
                       if any(math.dist(p, q) < 22 for aj, jdx, q in lst if (aj, jdx) != (ai, idx))}
        for ai, a in enumerate(aristas):
            pts = a['puntos']
            r = a['r']
            for idx, vecino in ((0, 1), (-1, -2)):
                if (ai, idx) not in amontonados:
                    continue
                k = r['o'] if idx == 0 else r['d']
                l, t, w, h = cajas[k]
                cx, cy = l + w / 2, t + h / 2
                nx, ny = pts[vecino]
                if l + 4 < nx < l + w - 4 and (ny > t + h or ny < t):
                    buscado, escala = (nx, t + h if ny > t + h else t), (h / 2 + 8) / (h / 2)
                elif t + 4 < ny < t + h - 4 and (nx > l + w or nx < l):
                    buscado, escala = (l + w if nx > l + w else l, ny), (w / 2 + 8) / (w / 2)
                else:
                    continue
                if math.dist(recortar(cajas[k], (nx, ny)), buscado) <= 4:
                    continue
                stub = (round(cx + (buscado[0] - cx) * escala), round(cy + (buscado[1] - cy) * escala))
                if idx == 0:
                    pts[:1] = [tuple(round(v) for v in buscado), stub]
                else:
                    pts[-1:] = [stub, tuple(round(v) for v in buscado)]
                fijados += 1
            a['puntos'] = extremos_finales(a)
        return fijados

    print('extremos fijados con un quiebre corto:', fijar_extremos())

    links_nota = {a['r']['d']: a['puntos'] for a in aristas if a['r']['tipo'] == 'nota'}

    # ------------------------------------------------------------------ etiquetas
    textos_etq = []
    for i, a in enumerate(aristas):
        r = a['r']
        if r['tipo'] in ('assoc', 'comp', 'agg'):
            if r['mo']:
                textos_etq.append(dict(arista=i, rol='tailMultiplicityLabel', pos=2, texto=r['mo']))
            textos_etq.append(dict(arista=i, rol='headMultiplicityLabel', pos=0, texto=r['md']))
            if r['nombre']:
                textos_etq.append(dict(arista=i, rol='nameLabel', pos=1, texto=r['nombre']))
        elif r['tipo'] == 'dep' and r['est']:
            textos_etq.append(dict(arista=i, rol='stereotypeLabel', pos=1, texto=f"«{r['est']}»"))

    obstaculos_cajas = list(cajas.values()) + list(cajas_notas.values())
    titulos_paq = []
    for clave, (l, t, w, h) in cajas_paq.items():
        nombre = clave[1][-1] if clave[1] else next(c[1] for c in CAPAS if c[0] == clave[0])
        tw = ancho(nombre, bold=True)
        titulos_paq.append((l + w / 2 - tw / 2 - 4, t + 15, tw + 8, 22))
        titulos_paq.append((l, t, w * 0.4, 15))  # pestaña
    etiquetas.ubicar(textos_etq, aristas, obstaculos_cajas + titulos_paq)

    # ------------------------------------------------------------------ vistas
    diagrama = {'_type': 'UMLClassDiagram', '_id': nid(), '_parent': ref(raiz['_id']),
                'name': titulo, 'ownedViews': []}
    # los diagramas van primero en el modelo, en el orden en que se arman
    raiz['ownedElements'].insert(sum(x['_type'] == 'UMLClassDiagram' for x in raiz['ownedElements']), diagrama)
    vistas_paq, vistas = {}, {}
    for clave in sorted(cajas_paq, key=lambda c: len(c[1])):
        l, t, w, h = cajas_paq[clave]
        capa = clave[0]
        colores = next(c for c in CAPAS if c[0] == capa)
        color = colores[2 + min(len(clave[1]), 2)]
        v = {'_type': 'UMLPackageView', '_id': nid(), '_parent': ref(diagrama['_id']),
             'model': ref(paquete(capa, list(clave[1]))['_id']), 'left': round(l), 'top': round(t),
             'width': round(w), 'height': round(h), 'fillColor': color, 'containedViews': []}
        padre = (capa, clave[1][:-1]) if clave[1] else None
        if padre in vistas_paq:
            v['containerView'] = ref(vistas_paq[padre]['_id'])
            vistas_paq[padre]['containedViews'].append(ref(v['_id']))
        vistas_paq[clave] = v
        diagrama['ownedViews'].append(v)

    for k in clases:
        e, t = elementos[k], T[k]
        l, tp, w, h = cajas[k]
        tipo_vista = {'UMLClass': 'UMLClassView', 'UMLInterface': 'UMLInterfaceView',
                      'UMLEnumeration': 'UMLEnumerationView'}[e['_type']]
        v = {'_type': tipo_vista, '_id': nid(), '_parent': ref(diagrama['_id']), 'model': ref(e['_id']),
             'left': l, 'top': tp, 'width': w, 'height': h}
        if e['_type'] == 'UMLInterface':
            v['stereotypeDisplay'] = 'label'
        if k in resumidas:
            v.update(suppressAttributes=True, suppressOperations=True)
            if e['_type'] == 'UMLEnumeration':
                v['suppressLiterals'] = True
        elif ESTEREOTIPO.get(k) in PATRONES:
            v['fillColor'] = '#FFF6D5'
        clave = (t['capa'], tuple(t['ruta']))
        if clave in vistas_paq:
            v['containerView'] = ref(vistas_paq[clave]['_id'])
            vistas_paq[clave]['containedViews'].append(ref(v['_id']))
        vistas[k] = v
        diagrama['ownedViews'].append(v)

    for k, txt in NOTAS.items():
        l, t, w, h = cajas_notas[k]
        nv = {'_type': 'UMLNoteView', '_id': nid(), '_parent': ref(diagrama['_id']), 'text': txt,
              'left': l, 'top': t, 'width': w, 'height': h}
        diagrama['ownedViews'].append(nv)
        diagrama['ownedViews'].append({'_type': 'UMLNoteLinkView', '_id': nid(), '_parent': ref(diagrama['_id']),
                                       'head': ref(vistas[k]['_id']), 'tail': ref(nv['_id']), 'lineStyle': 1,
                                       'points': ';'.join(f'{x}:{y}' for x, y in links_nota[k])})

    ROLES = {'UMLAssociationView': [('nameLabel', 1), ('stereotypeLabel', 1), ('propertyLabel', 1),
                                    ('tailRoleNameLabel', 2), ('tailPropertyLabel', 2), ('tailMultiplicityLabel', 2),
                                    ('headRoleNameLabel', 0), ('headPropertyLabel', 0), ('headMultiplicityLabel', 0)],
             'UMLDependencyView': [('nameLabel', 1), ('stereotypeLabel', 1), ('propertyLabel', 1)],
             'UMLInterfaceRealizationView': [('nameLabel', 1), ('stereotypeLabel', 1), ('propertyLabel', 1)],
             'UMLGeneralizationView': [('nameLabel', 1), ('stereotypeLabel', 1), ('propertyLabel', 1)]}
    por_arista = {}
    for e in textos_etq:
        por_arista.setdefault(e['arista'], {})[e['rol']] = e
    for i, a in enumerate(aristas):
        r = a['r']
        if r['tipo'] == 'nota':
            continue
        tipo_vista = {'UMLAssociation': 'UMLAssociationView', 'UMLInterfaceRealization': 'UMLInterfaceRealizationView',
                      'UMLGeneralization': 'UMLGeneralizationView', 'UMLDependency': 'UMLDependencyView'}[r['modelo']['_type']]
        vid = nid()
        v = {'_type': tipo_vista, '_id': vid, '_parent': ref(diagrama['_id']), 'model': ref(r['modelo']['_id']),
             'tail': ref(vistas[r['o']]['_id']),
             'head': ref((vistas_paq[clave_paq(r['d'])] if es_paquete(r['d']) else vistas[r['d']])['_id']), 'lineStyle': 1,
             'points': ';'.join(f'{x}:{y}' for x, y in a.get('puntos_vista', a['puntos'])), 'subViews': []}
        for rol, posicion in ROLES[tipo_vista]:
            etq = por_arista.get(i, {}).get(rol)
            lv = {'_type': 'EdgeLabelView', '_id': nid(), '_parent': ref(vid), 'model': ref(r['modelo']['_id']),
                  'hostEdge': ref(vid), 'edgePosition': posicion,
                  'alpha': etq['alpha'] if etq else 0, 'distance': etq['distancia'] if etq else 0,
                  'visible': bool(etq)}
            v['subViews'].append(lv)
            v[rol] = ref(lv['_id'])
        if tipo_vista == 'UMLAssociationView':
            v['showEndOrder'] = 'hide'
        diagrama['ownedViews'].append(v)
    geom = {
        'cajas': {elementos[k]['name']: cajas[k] for k in clases},
        'etiquetas': [dict(texto=e['texto'], rect=e['rect'], arista=e['arista'], rol=e['rol'], alpha=e['alpha'],
                           distancia=e['distancia']) for e in textos_etq],
        'aristas': [dict(o=a['r']['o'], d=str(a['r']['d']), tipo=a['r']['tipo'],
                         puntos=a.get('puntos_vista', a['puntos'])) for a in aristas],
        'notas': {k: v for k, v in cajas_notas.items()},
    }
    print(f'ok «{titulo}»: {len(clases)} clases, {len(R)} relaciones, {len(textos_etq)} etiquetas, '
          f'{len(NOTAS)} notas, {round(L["ancho"])}x{round(L["alto"])} px')
    return diagrama, geom


def limpiar(x):
    if isinstance(x, dict):
        return {k: limpiar(v) for k, v in x.items() if not k.startswith('_') or k in ('_type', '_id', '_parent')}
    if isinstance(x, list):
        return [limpiar(v) for v in x]
    return x


DIAGRAMA_COMPLETO = 'SkillSwap · Clean Architecture (todas las capas)'
geometrias = {}
diagrama_completo, geometrias[DIAGRAMA_COMPLETO] = dibujar(
    DIAGRAMA_COMPLETO, list(T), R, NOTAS, FILAS, ALINEAR, EXTRA_PASILLO)
diagrama_completo['defaultDiagram'] = True


# ------------------------------------------------------------------ una vista por capa
def vista_de_capa(capa):
    """Las clases de la capa con sus relaciones, más las clases de otras capas que la capa usa o implementa
    (solo con el nombre, en el marco de su capa). Como las dependencias apuntan hacia el dominio, el contexto
    siempre son capas más internas: la vista de Dominio no tiene contexto."""
    propias = [k for k in T if T[k]['capa'] == capa]
    rels, contexto = [], []
    for r in R:
        o, d = r['o'], r['d']
        if es_paquete(d):
            continue
        if T[o]['capa'] != capa:
            continue
        rels.append(r)
        if T[d]['capa'] != capa and d not in contexto:
            contexto.append(d)
    clases = propias + [k for k in T if k in contexto]  # en el orden del modelo, como en el diagrama completo
    bloques = {T[k]['bloque'] for k in clases}
    capas = {T[k]['capa'] for k in clases}
    # «create» de las raíces de composición: al módulo o a la capa, si están en la vista
    for r in R:
        if es_paquete(r['d']) and T[r['o']]['capa'] == capa and (
                (r['d'][2] is None and r['d'][1] in capas) or (r['d'][1], r['d'][2]) in bloques):
            rels.append(r)
    filas = [[(capa, m) for m in fila if (capa, m) in bloques] for fila in FILAS_CAPA[capa]]
    otras = [b for fila in FILAS for b in fila if b in bloques and b[0] != capa]
    otras.sort(key=lambda b: CAPA_IDX[b[0]])  # de afuera hacia adentro, cada capa junta
    filas = [f for f in filas + [otras] if f]
    if any(b[0] == capa for f in filas for b in f) is False or sum(len(f) for f in filas) != len(bloques):
        raise ValueError(f'bloques sin fila en la vista de {capa}')
    # lugar extra arriba y abajo de las filas de controllers: el marco y el título del paquete que envuelve a impl
    con_controllers = [i for i, f in enumerate(filas) if any(b[0] == 'controllers' for b in f)]
    extra = {con_controllers[0]: 80, con_controllers[-1] + 1: 60} if con_controllers else {}
    notas = {k: v for k, v in NOTAS.items() if k in propias}
    nombre = 'Capa ' + next(c[1] for c in CAPAS if c[0] == capa)
    _, geometrias[nombre] = dibujar(nombre, clases, rels, notas, filas, {}, extra, frozenset(contexto))


for c in CAPAS:
    vista_de_capa(c[0])

Path(SALIDA).write_text(json.dumps(limpiar(proyecto), ensure_ascii=False, indent=1))
# geometría esperada de cada diagrama, para que verificar.py compare contra el SVG
Path(tempfile.gettempdir(), Path(SALIDA).stem + '.geom.json').write_text(json.dumps(geometrias, ensure_ascii=False))
print(f'ok {SALIDA}: {len(geometrias)} diagramas')
