package ar.edu.uade.skillswap.domain.notificacion;

import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;

import java.util.List;
import java.util.Optional;

/** Puerto de la bandeja de notificaciones. Lo implementa la infraestructura. */
public interface NotificacionRepository {

    Optional<Notificacion> findById(NotificacionId id);

    void save(Notificacion notificacion);

    /** La bandeja del usuario, de la más reciente a la más vieja. */
    Pagina<Notificacion> findByDestinatario(String destinatarioId, PaginaSolicitada pagina);

    /** Las notificaciones sin leer del usuario, para marcarlas todas como leídas. */
    List<Notificacion> findNoLeidas(String destinatarioId);
}
