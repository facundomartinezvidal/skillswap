package ar.edu.uade.skillswap.domain.publicacion;

import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia de las publicaciones. Lo implementa la infraestructura.
 * La búsqueda con filtros no está acá: cruza publicación y perfil, así que es un puerto de aplicación
 * ({@code BuscadorPublicaciones}).
 */
public interface PublicacionRepository {

    Optional<Publicacion> findById(PublicacionId id);

    /** Guarda la publicación con sus franjas, cupos reservados, promociones e historial. */
    void save(Publicacion publicacion);

    /** Las publicaciones de un autor en cualquier estado, para su panel. */
    Pagina<Publicacion> findByAutor(String autorId, PaginaSolicitada pagina);

    /** Las publicaciones PUBLICADAS de un autor, para su perfil público. */
    List<Publicacion> findActivasByAutor(String autorId);

    /** Varias publicaciones de una vez (las que existan), para no hacer una consulta por cada una al listar. */
    List<Publicacion> findByIds(Collection<PublicacionId> ids);

    /**
     * Las publicaciones en estado PUBLICADA: son las candidatas para recomendar.
     * Limitación conocida: no tiene límite, así que carga todas las publicadas. Alcanza para el volumen
     * del TP; con muchos datos, la estrategia debería recibir candidatas ya acotadas en la consulta.
     */
    List<Publicacion> findPublicadas();

    /** Las categorías de las publicaciones visibles, sin repetir y ordenadas. */
    List<String> categorias();
}
