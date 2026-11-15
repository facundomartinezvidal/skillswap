package ar.edu.uade.skillswap.domain.wallet;

import java.util.Objects;
import java.util.UUID;

public record MovimientoId(UUID valor) {

    public MovimientoId {
        Objects.requireNonNull(valor, "El id del movimiento es obligatorio");
    }

    public static MovimientoId generate() {
        return new MovimientoId(UUID.randomUUID());
    }
}
