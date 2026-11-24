"""Medidas geométricas compartidas por el generador y el verificador."""
import math


def dist_punto_segmento(p, a, b):
    dx, dy = b[0] - a[0], b[1] - a[1]
    L2 = dx * dx + dy * dy
    if L2 == 0:
        return math.dist(p, a)
    t = max(0.0, min(1.0, ((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / L2))
    return math.dist(p, (a[0] + t * dx, a[1] + t * dy))


def tramo_pegado(a, b, tol=6.0, paso=6.0):
    """Largo del tramo a (aprox.) que corre a menos de tol px del tramo b: detecta líneas encimadas,
    paralelas muy cercanas y cruces rasantes, que a la vista se funden en una sola línea."""
    (p, q), (r, s) = a, b
    L = math.dist(p, q)
    if L < 1:
        return 0.0
    n = max(1, int(L // paso))
    mejor = actual = 0.0
    for i in range(n + 1):
        t = i / n
        pt = (p[0] + t * (q[0] - p[0]), p[1] + t * (q[1] - p[1]))
        if dist_punto_segmento(pt, r, s) < tol:
            actual += L / n
            mejor = max(mejor, actual)
        else:
            actual = 0.0
    return mejor


def encimados(a, b, tol=6.0):
    (p, q), (r, s) = a, b
    # cajas que no se tocan: no pueden correr pegados
    if (max(p[0], q[0]) + tol < min(r[0], s[0]) or max(r[0], s[0]) + tol < min(p[0], q[0]) or
            max(p[1], q[1]) + tol < min(r[1], s[1]) or max(r[1], s[1]) + tol < min(p[1], q[1])):
        return 0.0
    # con 30° o más de ángulo, lo que corren pegados es a lo sumo 2*tol/sen(30°) = 4*tol
    la, lb = math.dist(p, q), math.dist(r, s)
    if la < 1 or lb < 1:
        return 0.0
    seno = abs((q[0] - p[0]) * (s[1] - r[1]) - (q[1] - p[1]) * (s[0] - r[0])) / (la * lb)
    if seno >= 0.5:
        return 0.0
    return max(tramo_pegado(a, b, tol), tramo_pegado(b, a, tol))
