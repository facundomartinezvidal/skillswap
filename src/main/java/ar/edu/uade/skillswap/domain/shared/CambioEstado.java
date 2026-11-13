package ar.edu.uade.skillswap.domain.shared;

import java.time.Instant;
import java.util.Objects;

/** Registro inmutable de una transición de estado. El motivo puede ser nulo. */
public record CambioEstado(String desde, String hacia, Instant fecha, String actorId, String motivo) {

    public CambioEstado {
        Objects.requireNonNull(desde, "El estado de origen es obligatorio");
        Objects.requireNonNull(hacia, "El estado de destino es obligatorio");
        Objects.requireNonNull(fecha, "La fecha del cambio es obligatoria");
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("El actor del cambio de estado es obligatorio");
        }
    }
}
