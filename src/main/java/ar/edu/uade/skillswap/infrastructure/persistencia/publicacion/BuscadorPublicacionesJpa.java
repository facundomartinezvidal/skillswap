package ar.edu.uade.skillswap.infrastructure.persistencia.publicacion;

import ar.edu.uade.skillswap.application.busqueda.BuscadorPublicaciones;
import ar.edu.uade.skillswap.application.busqueda.CriteriosBusqueda;
import ar.edu.uade.skillswap.domain.publicacion.Publicacion;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.infrastructure.persistencia.UnidadDeTrabajoJpa;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Búsqueda con JPQL: filtra, ordena y pagina en la base, con los mismos criterios que
 * {@code BuscadorPublicacionesEnMemoria}. Solo devuelve publicaciones PUBLICADAS.
 * <p>
 * La consulta une cada publicación con el perfil de su autor ({@code left join ... on}, porque se
 * conocen solo por el userId) para filtrar y ordenar por reputación sin otra consulta por autor. El
 * texto se busca en título, descripción y etiquetas; la habilidad, en las etiquetas y en las habilidades
 * del autor (con {@code exists}, así una publicación con varias etiquetas no aparece repetida). El precio
 * se filtra con la columna {@code precio_final}, que ya tiene aplicadas las promociones.
 * <p>
 * Fetch plan: una consulta trae solo los ids de la página pedida, otra cuenta el total, y después se
 * cargan esas publicaciones completas por lote ({@link PublicacionRepositoryJpa#cargar}).
 */
public class BuscadorPublicacionesJpa implements BuscadorPublicaciones {

    /** Promedio de reputación del autor; 0 si no tiene calificaciones o no tiene perfil. */
    private static final String REPUTACION = "(case when a.reputacionCantidad > 0"
            + " then 1.0 * a.reputacionSuma / a.reputacionCantidad else 0 end)";
    private static final String MAS_RECIENTE = "p.publicadaEn desc, p.secuencia";

    private final UnidadDeTrabajoJpa unidadDeTrabajo;

    public BuscadorPublicacionesJpa(UnidadDeTrabajoJpa unidadDeTrabajo) {
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
    }

    @Override
    public Pagina<Publicacion> buscar(CriteriosBusqueda criterios, PaginaSolicitada pagina) {
        var parametros = new HashMap<String, Object>();
        var desdeDonde = " from PublicacionEntity p left join PerfilEntity a on a.userId = p.autorId"
                + filtros(criterios, parametros);

        return unidadDeTrabajo.conEntityManager(em -> {
            var consultaIds = em.createQuery("select p.id" + desdeDonde + " order by " + orden(criterios), String.class);
            var consultaTotal = em.createQuery("select count(p)" + desdeDonde, Long.class);
            parametros.forEach((nombre, valor) -> {
                consultaIds.setParameter(nombre, valor);
                consultaTotal.setParameter(nombre, valor);
            });
            var ids = consultaIds.setFirstResult(pagina.desde()).setMaxResults(pagina.tamanio()).getResultList();
            var total = consultaTotal.getSingleResult();
            return new Pagina<>(PublicacionRepositoryJpa.cargar(em, ids), pagina.numero(), pagina.tamanio(), total);
        });
    }

    /** Arma el {@code where} con un filtro por cada criterio que no sea nulo, y anota sus parámetros. */
    private static String filtros(CriteriosBusqueda c, Map<String, Object> parametros) {
        var where = new StringBuilder(" where p.estado = 'PUBLICADA'");
        if (tieneTexto(c.texto())) {
            where.append(" and (lower(p.titulo) like :texto or lower(p.descripcion) like :texto"
                    + " or exists (select 1 from PublicacionEntity p2 join p2.etiquetas e"
                    + " where p2.id = p.id and lower(e) like :texto))");
            parametros.put("texto", "%" + normalizar(c.texto()) + "%");
        }
        if (c.categoria() != null) {
            where.append(" and lower(p.categoria) = :categoria");
            parametros.put("categoria", normalizar(c.categoria()));
        }
        if (c.modalidad() != null) {
            where.append(" and p.modalidad = :modalidad");
            parametros.put("modalidad", c.modalidad().name());
        }
        if (c.precioMinimo() != null) {
            where.append(" and p.precioFinal >= :precioMinimo");
            parametros.put("precioMinimo", c.precioMinimo());
        }
        if (c.precioMaximo() != null) {
            where.append(" and p.precioFinal <= :precioMaximo");
            parametros.put("precioMaximo", c.precioMaximo());
        }
        if (tieneTexto(c.habilidad())) {
            where.append(" and (exists (select 1 from PublicacionEntity p3 join p3.etiquetas e3"
                    + " where p3.id = p.id and lower(e3) = :habilidad)"
                    + " or exists (select 1 from PerfilEntity a2 join a2.habilidades h"
                    + " where a2.userId = p.autorId and lower(h) = :habilidad))");
            parametros.put("habilidad", normalizar(c.habilidad()));
        }
        if (c.reputacionMinima() != null) {
            where.append(" and ").append(REPUTACION).append(" >= :reputacionMinima");
            parametros.put("reputacionMinima", c.reputacionMinima());
        }
        return where.toString();
    }

    /**
     * Mismo orden que la búsqueda en memoria. Relevancia: primero las que tienen el texto en el título y
     * después las más recientes. El último criterio (orden de alta) hace que la paginación sea estable.
     */
    private static String orden(CriteriosBusqueda c) {
        return switch (c.orden()) {
            case MEJOR_REPUTACION -> REPUTACION + " desc, " + MAS_RECIENTE;
            case MENOR_PRECIO -> "p.precioFinal, " + MAS_RECIENTE;
            case MAS_RECIENTE -> MAS_RECIENTE;
            case RELEVANCIA -> tieneTexto(c.texto())
                    ? "case when lower(p.titulo) like :texto then 0 else 1 end, " + MAS_RECIENTE
                    : MAS_RECIENTE;
        };
    }

    private static boolean tieneTexto(String texto) {
        return texto != null && !texto.isBlank();
    }

    private static String normalizar(String texto) {
        return texto.toLowerCase(Locale.ROOT).trim();
    }
}
