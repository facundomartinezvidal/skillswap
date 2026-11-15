package ar.edu.uade.skillswap.domain.contratacion;

import ar.edu.uade.skillswap.domain.shared.Puntaje;

import java.time.Instant;
import java.util.Objects;

/**
 * Calificación que un participante deja sobre la otra parte de una contratación completada.
 * Entidad interna del agregado {@link Contratacion}: solo se crea con {@link Contratacion#calificar}.
 * El comentario es opcional y puede ser nulo.
 */
public record Calificacion(CalificacionId id, String autorId, String destinatarioId, Puntaje puntaje,
                           String comentario, Instant fecha) {

    public Calificacion {
        Objects.requireNonNull(id, "El id de la calificación es obligatorio");
        requireText(autorId, "El autor de la calificación es obligatorio");
        requireText(destinatarioId, "El destinatario de la calificación es obligatorio");
        if (autorId.equals(destinatarioId)) {
            throw new IllegalArgumentException("Un usuario no puede calificarse a sí mismo");
        }
        Objects.requireNonNull(puntaje, "El puntaje es obligatorio");
        Objects.requireNonNull(fecha, "La fecha de la calificación es obligatoria");
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
