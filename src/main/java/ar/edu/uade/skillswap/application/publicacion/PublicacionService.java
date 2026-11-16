package ar.edu.uade.skillswap.application.publicacion;

import ar.edu.uade.skillswap.application.publicacion.dto.DatosPublicacionCommand;
import ar.edu.uade.skillswap.application.publicacion.dto.PromocionDTO;
import ar.edu.uade.skillswap.application.publicacion.dto.PublicacionDTO;
import ar.edu.uade.skillswap.domain.perfil.PerfilRepository;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionRepository;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Casos de uso de las publicaciones. Lo usa el {@code PublicacionController}.
 * Todas las operaciones que modifican validan que quien actúa sea el autor (regla 10).
 */
public class PublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final PerfilRepository perfilRepository;
    private final Clock clock;

    public PublicacionService(PublicacionRepository publicacionRepository, PerfilRepository perfilRepository,
                              Clock clock) {
        this.publicacionRepository = Objects.requireNonNull(publicacionRepository);
        this.perfilRepository = Objects.requireNonNull(perfilRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Crea el borrador. El tipo sale del perfil del autor: un profesional publica ofertas y una
     * organización, desafíos. Como el tipo de perfil no cambia, la publicación siempre coincide con su
     * autor y {@link #publicar} no necesita volver a validarlo.
     */
    public PublicacionDTO crearBorrador(String userId, DatosPublicacionCommand datos) {
        throw pendiente();
    }

    public PublicacionDTO editar(String userId, String publicacionId, DatosPublicacionCommand datos) {
        throw pendiente();
    }

    public PublicacionDTO agregarFranja(String userId, String publicacionId, Instant inicio, Instant fin) {
        throw pendiente();
    }

    public PublicacionDTO publicar(String userId, String publicacionId) {
        throw pendiente();
    }

    public PublicacionDTO pausar(String userId, String publicacionId, String motivo) {
        throw pendiente();
    }

    public PublicacionDTO reanudar(String userId, String publicacionId) {
        throw pendiente();
    }

    public PublicacionDTO cerrar(String userId, String publicacionId, String motivo) {
        throw pendiente();
    }

    public PublicacionDTO cancelar(String userId, String publicacionId, String motivo) {
        throw pendiente();
    }

    public PublicacionDTO agregarPromocion(String userId, String publicacionId, PromocionDTO promocion) {
        throw pendiente();
    }

    public PublicacionDTO quitarPromocion(String userId, String publicacionId, PromocionDTO promocion) {
        throw pendiente();
    }

    /** Panel del autor: sus publicaciones en cualquier estado. */
    public Pagina<PublicacionDTO> listarPropias(String userId, PaginaSolicitada pagina) {
        throw pendiente();
    }

    /**
     * Detalle con franjas y lugares libres, para elegir al solicitar. Un BORRADOR o una PAUSADA solo
     * la ve su autor; para los demás es como si no existiera.
     */
    public PublicacionDTO verPublicacion(String userId, String publicacionId) {
        throw pendiente();
    }

    private static UnsupportedOperationException pendiente() {
        return new UnsupportedOperationException("Pendiente: se implementa en un PR siguiente");
    }
}
