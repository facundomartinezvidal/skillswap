package ar.edu.uade.skillswap.application.contratacion.dto;

import java.time.Instant;

/** Una línea del historial de estados (regla 8). El motivo puede ser nulo. */
public record CambioEstadoDTO(String desde, String hacia, Instant fecha, String actorId, String motivo) {
}
