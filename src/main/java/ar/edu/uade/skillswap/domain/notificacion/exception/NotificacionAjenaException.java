package ar.edu.uade.skillswap.domain.notificacion.exception;

import ar.edu.uade.skillswap.domain.notificacion.NotificacionId;
import ar.edu.uade.skillswap.domain.shared.DomainException;

/** Un usuario intentó operar sobre una notificación de otro. */
public class NotificacionAjenaException extends DomainException {

    private final String actorId;
    private final NotificacionId notificacionId;

    public NotificacionAjenaException(String actorId, NotificacionId notificacionId) {
        super("El usuario " + actorId + " no es el destinatario de esta notificación");
        this.actorId = actorId;
        this.notificacionId = notificacionId;
    }

    public String actorId() {
        return actorId;
    }

    public NotificacionId notificacionId() {
        return notificacionId;
    }
}
