package ar.edu.uade.skillswap.dominio.publicacion;

import java.util.Objects;
import java.util.UUID;

public record PublicacionId(UUID valor) {

    public PublicacionId {
        Objects.requireNonNull(valor, "El id de la publicación es obligatorio");
    }

    public static PublicacionId nueva() {
        return new PublicacionId(UUID.randomUUID());
    }
}
