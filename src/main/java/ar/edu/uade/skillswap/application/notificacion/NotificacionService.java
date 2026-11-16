package ar.edu.uade.skillswap.application.notificacion;

import ar.edu.uade.skillswap.application.notificacion.dto.NotificacionDTO;
import ar.edu.uade.skillswap.application.shared.UnidadDeTrabajo;
import ar.edu.uade.skillswap.domain.notificacion.NotificacionRepository;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;

import java.util.Objects;

/** Bandeja de notificaciones del usuario. Lo usa el {@code NotificacionController}. */
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UnidadDeTrabajo unidadDeTrabajo;

    public NotificacionService(NotificacionRepository notificacionRepository, UnidadDeTrabajo unidadDeTrabajo) {
        this.notificacionRepository = Objects.requireNonNull(notificacionRepository);
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
    }

    public Pagina<NotificacionDTO> listar(String userId, PaginaSolicitada pagina) {
        throw pendiente();
    }

    public void marcarLeida(String userId, String notificacionId) {
        throw pendiente();
    }

    /** Marca todas en una transacción: o quedan todas leídas o ninguna. */
    public void marcarTodasLeidas(String userId) {
        throw pendiente();
    }

    private static UnsupportedOperationException pendiente() {
        return new UnsupportedOperationException("Pendiente: se implementa en un PR siguiente");
    }
}
