package ar.edu.uade.skillswap.domain.contratacion.exception;

import ar.edu.uade.skillswap.domain.shared.DomainException;

/** Regla 1: el autor no puede solicitar su propia publicación. */
public class AutoSolicitudException extends DomainException {

    public AutoSolicitudException(String usuarioId) {
        super("El usuario " + usuarioId + " no puede solicitar su propia publicación");
    }
}
