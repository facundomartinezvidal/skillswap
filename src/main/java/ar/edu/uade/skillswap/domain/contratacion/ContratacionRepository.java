package ar.edu.uade.skillswap.domain.contratacion;

import ar.edu.uade.skillswap.domain.publicacion.PublicacionId;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/** Puerto de persistencia de las contrataciones. Lo implementa la infraestructura. */
public interface ContratacionRepository {

    Optional<Contratacion> findById(ContratacionId id);

    /** Guarda la contratación con su historial, propuesta de reprogramación, reintegro y calificaciones. */
    void save(Contratacion contratacion);

    /** Las contrataciones donde el usuario es cliente o proveedor, de la más reciente a la más vieja. */
    Pagina<Contratacion> findByParticipante(String userId, PaginaSolicitada pagina);

    /** Cuántas contrataciones completó el usuario como cliente o proveedor (historial resumido del perfil). */
    long countCompletadasDe(String userId);

    /**
     * Cuántas contrataciones se aceptaron desde {@code desde}, por publicación (popularidad reciente).
     * La fecha de aceptación está en el historial de estados.
     */
    Map<PublicacionId, Long> countAceptadasDesde(Instant desde);
}
