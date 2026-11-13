package ar.edu.uade.skillswap.domain.publicacion;

import java.util.Objects;
import java.util.UUID;

public record PublicacionId(UUID valor) {

    public PublicacionId {
        Objects.requireNonNull(valor, "El id de la publicación es obligatorio");
    }

    public static PublicacionId generate() {
        return new PublicacionId(UUID.randomUUID());
    }
}
