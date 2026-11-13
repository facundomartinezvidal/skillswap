package ar.edu.uade.skillswap.domain.contratacion;

import ar.edu.uade.skillswap.domain.publicacion.FranjaId;

import java.time.Instant;
import java.util.Objects;

/** Reprogramación pendiente: la propone un participante y la acepta o rechaza el otro. */
public record PropuestaReprogramacion(String propuestaPor, FranjaId franjaId, Instant inicio) {

    public PropuestaReprogramacion {
        if (propuestaPor == null || propuestaPor.isBlank()) {
            throw new IllegalArgumentException("Quien propone la reprogramación es obligatorio");
        }
        Objects.requireNonNull(franjaId, "La franja propuesta es obligatoria");
        Objects.requireNonNull(inicio, "El inicio propuesto es obligatorio");
    }
}
