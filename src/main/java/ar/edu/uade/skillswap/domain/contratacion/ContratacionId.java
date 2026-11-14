package ar.edu.uade.skillswap.domain.contratacion;

import java.util.Objects;
import java.util.UUID;

public record ContratacionId(UUID valor) {

    public ContratacionId {
        Objects.requireNonNull(valor, "El id de la contratación es obligatorio");
    }

    public static ContratacionId generate() {
        return new ContratacionId(UUID.randomUUID());
    }
}
