package ar.edu.uade.skillswap.dominio.publicacion;

import java.util.Objects;
import java.util.UUID;

public record FranjaId(UUID valor) {

    public FranjaId {
        Objects.requireNonNull(valor, "El id de la franja es obligatorio");
    }

    public static FranjaId nueva() {
        return new FranjaId(UUID.randomUUID());
    }
}
