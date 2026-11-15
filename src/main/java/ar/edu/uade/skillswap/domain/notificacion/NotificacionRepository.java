package ar.edu.uade.skillswap.domain.notificacion;

import java.util.Optional;

/** Puerto de la bandeja de notificaciones. Lo implementa la infraestructura. */
public interface NotificacionRepository {

    Optional<Notificacion> findById(NotificacionId id);

    void save(Notificacion notificacion);
}
