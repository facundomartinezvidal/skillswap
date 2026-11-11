package ar.edu.uade.skillswap.dominio.publicaciones;

import java.time.LocalDateTime;
import java.util.Objects;

public record HistorialEstadoPublicacion(
        EstadoPublicacion estadoAnterior,
        EstadoPublicacion estadoNuevo,
        LocalDateTime fecha,
        String actorId,
        String motivo) {

    public HistorialEstadoPublicacion {
        Objects.requireNonNull(estadoAnterior, "El estado anterior es obligatorio");
        Objects.requireNonNull(estadoNuevo, "El estado nuevo es obligatorio");
        Objects.requireNonNull(fecha, "La fecha es obligatoria");
        actorId = textoObligatorio(actorId, "El actor es obligatorio");
        motivo = motivo == null ? "" : motivo.trim();
    }

    private static String textoObligatorio(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.trim();
    }
}
