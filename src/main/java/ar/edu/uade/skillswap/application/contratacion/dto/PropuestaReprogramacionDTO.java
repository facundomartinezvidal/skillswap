package ar.edu.uade.skillswap.application.contratacion.dto;

import java.time.Instant;

/** Reprogramación pendiente de respuesta. */
public record PropuestaReprogramacionDTO(String propuestaPor, String franjaId, Instant inicio) {
}
