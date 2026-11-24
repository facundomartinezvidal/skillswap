"""Lee las clases Java de src/main/java y devuelve un modelo simple por tipo (incluye tipos anidados)."""
import re
from pathlib import Path

DECL = re.compile(r'(?:(public|private|protected)\s+)?((?:abstract|final|sealed|non-sealed|static)\s+)*'
                  r'(?<![\w.@])(class|interface|enum|record)\s+(\w+)(<[^>{]*>)?\s*(\((?P<comp>[^)]*)\))?(?P<resto>[^{;=]*)\{', re.S)
ANOT = r'(?:@\w+(?:\([^)]*\))?\s+)*'


def sin_comentarios(src):
    src = re.sub(r'(?m)^\s*(import|package)\s[^;]*;', '', src)
    src = re.sub(r'/\*.*?\*/', '', src, flags=re.S)
    src = re.sub(r'//[^\n]*', '', src)
    src = re.sub(r'"(?:\\.|[^"\\])*"', '""', src)
    src = re.sub(r"'(?:\\.|[^'\\])'", "''", src)
    return src


def partir(texto, sep=','):
    partes, prof, actual = [], 0, ''
    for ch in texto:
        if ch in '<(':
            prof += 1
        elif ch in '>)':
            prof -= 1
        if ch == sep and prof == 0:
            partes.append(actual)
            actual = ''
        else:
            actual += ch
    if actual.strip():
        partes.append(actual)
    return [p.strip() for p in partes]


def limpiar_tipo(t):
    return re.sub(r'\s+', '', t).replace(',', ', ')


def parametro(p):
    p = re.sub(ANOT, '', p).replace('final ', '').strip()
    tipo, nombre = p.rsplit(None, 1)
    return {'name': nombre, 'type': limpiar_tipo(tipo)}


def profundidades(src):
    prof, out = 0, [0] * len(src)
    for i, ch in enumerate(src):
        if ch == '{':
            out[i] = prof
            prof += 1
            continue
        if ch == '}':
            prof -= 1
        out[i] = prof
    return out


def fin_bloque(src, abre):
    prof = 0
    for i in range(abre, len(src)):
        if src[i] == '{':
            prof += 1
        elif src[i] == '}':
            prof -= 1
            if prof == 0:
                return i
    return len(src)


def cuerpo_plano(src, abre, cierra):
    """El cuerpo sin los bloques internos (cuerpos de métodos y de tipos anidados)."""
    out, prof = [], 0
    for ch in src[abre + 1:cierra]:
        if ch == '{':
            prof += 1
            if prof == 1:
                out.append('{')
            continue
        if ch == '}':
            prof -= 1
            continue
        if prof == 0:
            out.append(ch)
    return ''.join(out)


def parsear_tipo(src, m, prof_map, externo, en_interfaz=False):
    vis, kind, name = m.group(1), m.group(3), m.group(4)
    nombre = f'{externo}.{name}' if externo else name
    abre = m.end() - 1
    cierra = fin_bloque(src, abre)
    resto = m.group('resto')
    t = {'name': nombre, 'simple': name, 'kind': kind, 'abstract': 'abstract' in (m.group(2) or ''),
         'generic': (m.group(5) or '').strip(), 'attributes': [], 'operations': [], 'literals': [],
         'implements': [], 'extends': [], 'private': vis == 'private', 'nested': []}
    im = re.search(r'implements\s+(.+?)(?:\bpermits\b|$)', resto, re.S)
    if im:
        t['implements'] = [re.sub(r'<.*', '', x).strip() for x in partir(im.group(1))]
    ex = re.search(r'extends\s+(.+?)(?:\bimplements\b|\bpermits\b|$)', resto, re.S)
    if ex:
        t['extends'] = [re.sub(r'<.*', '', x).strip() for x in partir(ex.group(1))]
    if m.group('comp'):
        t['attributes'] = [dict(parametro(c), visibility='private', readOnly=True) for c in partir(m.group('comp'))]
    body = cuerpo_plano(src, abre, cierra)
    if kind == 'enum':
        primera = body.split(';')[0]
        t['literals'] = [re.sub(r'\(.*', '', x).strip() for x in partir(primera)
                         if re.match(r'^\s*[A-Z][A-Z0-9_]*\s*(\(.*\))?\s*\{?\s*$', x, re.S)]
    if kind != 'interface':
        for f in re.finditer(r'(?m)^\s*' + ANOT + r'((?:(?:private|protected|public|static|final|transient|volatile)\s+)*)'
                             r'([\w<>,.\s?\[\]]+?)\s+(\w+)\s*(=[^;]*)?;', body):
            mods, tipo = f.group(1).split(), limpiar_tipo(f.group(2))
            if 'static' in mods or re.search(r'\b(return|throw|new|package|import)\b', f.group(2)) or not tipo:
                continue
            if kind == 'enum' and not mods and tipo.isupper():
                continue
            vis = next((m for m in mods if m in ('private', 'protected', 'public')), 'package')
            t['attributes'].append({'name': f.group(3), 'type': tipo, 'visibility': vis, 'readOnly': 'final' in mods})
    patron = (r'(?m)^\s*' + ANOT + r'(public\s+|default\s+|protected\s+|private\s+)?(static\s+)?(abstract\s+)?(final\s+)?'
              r'(<[^>]+>\s+)?([\w<>\[\],.\s?]+?)\s+(\w+)\s*\(([^)]*)\)\s*(throws[^{;]*)?[{;]')
    es_interfaz = kind == 'interface'
    for o in re.finditer(patron, body):
        mod, ret, nom = (o.group(1) or '').strip(), o.group(6).strip(), o.group(7)
        if ret in ('new', 'return', 'throw', 'else', 'record', 'class', 'interface', 'enum') or nom == name:
            continue
        if re.search(r'\b(record|class|interface|enum)\b', ret):
            continue
        if mod == 'private':
            continue
        vis = 'public' if (mod in ('public', 'default') or es_interfaz) else ('protected' if mod == 'protected' else 'package')
        params = [parametro(p) for p in partir(o.group(8))] if o.group(8).strip() else []
        t['operations'].append({'name': nom, 'visibility': vis, 'static': bool(o.group(2)), 'abstract': bool(o.group(3)),
                                'returns': limpiar_tipo(ret), 'params': params})
    # tipos anidados (a profundidad del cuerpo)
    nivel = prof_map[abre] + 1
    for mm in DECL.finditer(src, abre + 1, cierra):
        if prof_map[mm.start()] == nivel and (mm.start() == 0 or src[mm.start() - 1] in ' \n\t;{}'):
            antes = src[max(abre, mm.start() - 1)]
            sub = parsear_tipo(src, mm, prof_map, nombre, es_interfaz)
            if not sub['private']:
                t['nested'].append(sub)
    return t


def parsear(archivo):
    src = sin_comentarios(Path(archivo).read_text())
    pm = profundidades(src)
    for m in DECL.finditer(src):
        if pm[m.start()] == 0:
            return parsear_tipo(src, m, pm, '')


def modelo(raiz):
    tipos = {}

    def agregar(t, rel):
        t['pkg'] = rel
        clave = t['name'] if t['name'] not in tipos else f'{t["name"]}@{rel}'
        t['key'] = clave
        tipos[clave] = t
        for n in t['nested']:
            agregar(n, rel)

    for f in sorted(Path(raiz).rglob('*.java')):
        t = parsear(f)
        if t:
            agregar(t, f.relative_to(raiz).parent.as_posix())
    return tipos


if __name__ == '__main__':
    import sys
    m = modelo(sys.argv[1])
    for k, t in m.items():
        if len(sys.argv) > 2 and not t['pkg'].startswith(sys.argv[2]):
            continue
        print(k, t['kind'], t['pkg'], 'impl', t['implements'], 'ext', t['extends'], t['literals'])
        for a in t['attributes']:
            print('   -', a['name'], ':', a['type'])
        for o in t['operations']:
            print('   +', o['name'], '(', ', '.join(p['name'] + ': ' + p['type'] for p in o['params']), '):', o['returns'])
    print(len(m), 'tipos')
