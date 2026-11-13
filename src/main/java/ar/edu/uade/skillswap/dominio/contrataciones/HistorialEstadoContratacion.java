package ar.edu.uade.skillswap.dominio.contrataciones;

import java.time.LocalDateTime;
import java.util.Objects;

public record HistorialEstadoContratacion(
        EstadoContratacion estadoAnterior,
        EstadoContratacion estadoNuevo,
        LocalDateTime fecha,
        String actorId,
        String motivo) {

    public HistorialEstadoContratacion {
        Objects.requireNonNull(estadoAnterior, "El estado anterior es obligatorio");
        Objects.requireNonNull(estadoNuevo, "El estado nuevo es obligatorio");
        Objects.requireNonNull(fecha, "La fecha es obligatoria");
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("El actor es obligatorio");
        }
        actorId = actorId.trim();
        motivo = motivo == null ? "" : motivo.trim();
    }
}
