package ar.edu.uade.skillswap.application.busqueda;

import ar.edu.uade.skillswap.domain.publicacion.Publicacion;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;

/**
 * Puerto de consulta para la búsqueda. Está en aplicación y no en el repositorio de publicaciones
 * porque cruza dos módulos (reputación y habilidades viven en el perfil del autor). La
 * infraestructura lo implementa con una sola consulta, sin N+1. Solo devuelve publicaciones PUBLICADAS.
 */
public interface BuscadorPublicaciones {

    Pagina<Publicacion> buscar(CriteriosBusqueda criterios, PaginaSolicitada pagina);
}
