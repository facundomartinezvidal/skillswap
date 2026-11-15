package ar.edu.uade.skillswap.domain.notificacion;

import java.util.Objects;
import java.util.UUID;

public record NotificacionId(UUID valor) {

    public NotificacionId {
        Objects.requireNonNull(valor, "El id de la notificación es obligatorio");
    }

    public static NotificacionId generate() {
        return new NotificacionId(UUID.randomUUID());
    }
}
