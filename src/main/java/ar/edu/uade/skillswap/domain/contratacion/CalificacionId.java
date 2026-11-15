package ar.edu.uade.skillswap.domain.contratacion;

import java.util.Objects;
import java.util.UUID;

public record CalificacionId(UUID valor) {

    public CalificacionId {
        Objects.requireNonNull(valor, "El id de la calificación es obligatorio");
    }

    public static CalificacionId generate() {
        return new CalificacionId(UUID.randomUUID());
    }
}
