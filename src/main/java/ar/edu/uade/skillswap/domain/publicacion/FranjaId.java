package ar.edu.uade.skillswap.domain.publicacion;

import java.util.Objects;
import java.util.UUID;

public record FranjaId(UUID valor) {

    public FranjaId {
        Objects.requireNonNull(valor, "El id de la franja es obligatorio");
    }

    public static FranjaId generate() {
        return new FranjaId(UUID.randomUUID());
    }
}
