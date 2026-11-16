package ar.edu.uade.skillswap.application.shared;

import ar.edu.uade.skillswap.application.shared.exception.RecursoNoEncontradoException;

import java.util.UUID;

/** Convierte los ids que llegan como texto desde los controllers. Un id mal formado es un recurso que no existe. */
public final class Ids {

    private Ids() {
    }

    public static UUID uuid(String recurso, String id) {
        if (id == null) {
            throw new RecursoNoEncontradoException(recurso, "null");
        }
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new RecursoNoEncontradoException(recurso, id);
        }
    }
}
