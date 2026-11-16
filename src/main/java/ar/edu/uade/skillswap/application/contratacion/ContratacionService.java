package ar.edu.uade.skillswap.application.contratacion;

import ar.edu.uade.skillswap.application.contratacion.dto.CalificacionDTO;
import ar.edu.uade.skillswap.application.contratacion.dto.ContratacionDTO;
import ar.edu.uade.skillswap.application.contratacion.dto.SolicitarContratacionCommand;
import ar.edu.uade.skillswap.application.evento.PublicadorEventos;
import ar.edu.uade.skillswap.application.shared.UnidadDeTrabajo;
import ar.edu.uade.skillswap.domain.contratacion.ContratacionRepository;
import ar.edu.uade.skillswap.domain.contratacion.strategy.PoliticaReintegroFactory;
import ar.edu.uade.skillswap.domain.perfil.PerfilRepository;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionRepository;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.domain.wallet.RetencionDePagos;
import ar.edu.uade.skillswap.domain.wallet.WalletRepository;

import java.time.Clock;
import java.util.Objects;

/**
 * Casos de uso de la contratación: el recorrido solicitar, aceptar, pagar, iniciar, completar y
 * calificar, más rechazar, cancelar con reintegro y reprogramar. Lo usan el {@code ContratacionController}
 * y el {@code CalificacionController}.
 * Coordina agregados que se conocen solo por id; las reglas viven en ellos. Las operaciones que tocan
 * más de un agregado son atómicas ({@link UnidadDeTrabajo}) y los eventos se publican después de confirmar.
 */
public class ContratacionService {

    private final ContratacionRepository contratacionRepository;
    private final PublicacionRepository publicacionRepository;
    private final WalletRepository walletRepository;
    private final PerfilRepository perfilRepository;
    private final RetencionDePagos retencionDePagos;
    private final PoliticaReintegroFactory politicaReintegroFactory;
    private final UnidadDeTrabajo unidadDeTrabajo;
    private final PublicadorEventos publicadorEventos;
    private final Clock clock;

    public ContratacionService(ContratacionRepository contratacionRepository,
                               PublicacionRepository publicacionRepository, WalletRepository walletRepository,
                               PerfilRepository perfilRepository, RetencionDePagos retencionDePagos,
                               PoliticaReintegroFactory politicaReintegroFactory, UnidadDeTrabajo unidadDeTrabajo,
                               PublicadorEventos publicadorEventos, Clock clock) {
        this.contratacionRepository = Objects.requireNonNull(contratacionRepository);
        this.publicacionRepository = Objects.requireNonNull(publicacionRepository);
        this.walletRepository = Objects.requireNonNull(walletRepository);
        this.perfilRepository = Objects.requireNonNull(perfilRepository);
        this.retencionDePagos = Objects.requireNonNull(retencionDePagos);
        this.politicaReintegroFactory = Objects.requireNonNull(politicaReintegroFactory);
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
        this.publicadorEventos = Objects.requireNonNull(publicadorEventos);
        this.clock = Objects.requireNonNull(clock);
    }

    /** Crea la contratación en PENDIENTE con el precio final de la publicación (con promociones). */
    public ContratacionDTO solicitar(SolicitarContratacionCommand command) {
        throw pendiente();
    }

    /** El proveedor acepta y se reserva el cupo de la franja, en una transacción. */
    public ContratacionDTO aceptar(String userId, String contratacionId) {
        throw pendiente();
    }

    public ContratacionDTO rechazar(String userId, String contratacionId, String motivo) {
        throw pendiente();
    }

    /** El cliente paga: el precio queda retenido en la Wallet de garantía, en una transacción. */
    public ContratacionDTO pagar(String userId, String contratacionId) {
        throw pendiente();
    }

    public ContratacionDTO iniciar(String userId, String contratacionId) {
        throw pendiente();
    }

    /** El proveedor completa y cobra lo retenido, en una transacción. */
    public ContratacionDTO completar(String userId, String contratacionId) {
        throw pendiente();
    }

    /**
     * Cancela aplicando la política de reintegro según quién cancela. Si estaba pagada reparte lo
     * retenido y si tenía cupo reservado lo libera, en una transacción.
     */
    public ContratacionDTO cancelar(String userId, String contratacionId, String motivo) {
        throw pendiente();
    }

    public ContratacionDTO proponerReprogramacion(String userId, String contratacionId, String nuevaFranjaId) {
        throw pendiente();
    }

    /** Aplica la propuesta: reserva la franja nueva y libera la anterior, en una transacción. */
    public ContratacionDTO aceptarReprogramacion(String userId, String contratacionId) {
        throw pendiente();
    }

    public ContratacionDTO rechazarReprogramacion(String userId, String contratacionId) {
        throw pendiente();
    }

    /** Califica a la otra parte y suma el puntaje a su reputación, en una transacción. */
    public CalificacionDTO calificar(String userId, String contratacionId, int puntaje, String comentario) {
        throw pendiente();
    }

    /** Las contrataciones del usuario como cliente o como proveedor. */
    public Pagina<ContratacionDTO> listarContrataciones(String userId, PaginaSolicitada pagina) {
        throw pendiente();
    }

    /** Detalle con historial y comprobante. Solo para los participantes. */
    public ContratacionDTO verContratacion(String userId, String contratacionId) {
        throw pendiente();
    }

    private static UnsupportedOperationException pendiente() {
        return new UnsupportedOperationException("Pendiente: se implementa en un PR siguiente");
    }
}
