package ar.edu.uade.skillswap.application.notificacion;

import ar.edu.uade.skillswap.application.notificacion.dto.NotificacionDTO;
import ar.edu.uade.skillswap.application.shared.Ids;
import ar.edu.uade.skillswap.application.shared.UnidadDeTrabajo;
import ar.edu.uade.skillswap.application.shared.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.domain.notificacion.Notificacion;
import ar.edu.uade.skillswap.domain.notificacion.NotificacionId;
import ar.edu.uade.skillswap.domain.notificacion.NotificacionRepository;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;

import java.util.Objects;

/**
 * Bandeja de notificaciones del usuario. Lo usa el {@code NotificacionController}.
 * Que solo el destinatario marque su notificación lo valida la {@link Notificacion}.
 */
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UnidadDeTrabajo unidadDeTrabajo;

    public NotificacionService(NotificacionRepository notificacionRepository, UnidadDeTrabajo unidadDeTrabajo) {
        this.notificacionRepository = Objects.requireNonNull(notificacionRepository);
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
    }

    /** La bandeja del usuario, de la más reciente a la más vieja. */
    public Pagina<NotificacionDTO> listar(String userId, PaginaSolicitada pagina) {
        return notificacionRepository.findByDestinatario(userId, pagina).map(NotificacionService::aDTO);
    }

    public void marcarLeida(String userId, String notificacionId) {
        unidadDeTrabajo.ejecutar(() -> {
            var id = new NotificacionId(Ids.uuid("notificación", notificacionId));
            var notificacion = notificacionRepository.findById(id)
                    .orElseThrow(() -> new RecursoNoEncontradoException("notificación", notificacionId));
            notificacion.marcarLeida(userId);
            notificacionRepository.save(notificacion);
            return null;
        });
    }

    /** Marca todas en una transacción: o quedan todas leídas o ninguna. */
    public void marcarTodasLeidas(String userId) {
        unidadDeTrabajo.ejecutar(() -> {
            var noLeidas = notificacionRepository.findNoLeidas(userId);
            noLeidas.forEach(notificacion -> notificacion.marcarLeida(userId));
            noLeidas.forEach(notificacionRepository::save);
            return noLeidas.size();
        });
    }

    private static NotificacionDTO aDTO(Notificacion n) {
        return new NotificacionDTO(n.id().valor().toString(), n.tipo(), n.titulo(), n.detalle(), n.fecha(),
                n.estaLeida());
    }
}
