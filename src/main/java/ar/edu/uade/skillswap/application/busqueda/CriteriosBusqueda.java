package ar.edu.uade.skillswap.application.busqueda;

import ar.edu.uade.skillswap.domain.publicacion.Modalidad;

/**
 * Filtros y orden de la búsqueda. Cualquier filtro nulo no filtra; sin orden, se ordena por relevancia.
 * {@code habilidad} busca en las etiquetas de la publicación y en las habilidades del autor.
 */
public record CriteriosBusqueda(String texto, String categoria, Modalidad modalidad, Long precioMinimo,
                                Long precioMaximo, String habilidad, Double reputacionMinima, OrdenBusqueda orden) {

    public CriteriosBusqueda {
        if (orden == null) {
            orden = OrdenBusqueda.RELEVANCIA;
        }
    }
}
