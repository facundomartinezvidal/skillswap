package ar.edu.uade.skillswap.domain.contratacion.exception;

import ar.edu.uade.skillswap.domain.shared.DomainException;

/** Cada participante califica una sola vez cada contratación. */
public class CalificacionDuplicadaException extends DomainException {

    private final String autorId;

    public CalificacionDuplicadaException(String autorId) {
        super("El usuario " + autorId + " ya calificó esta contratación");
        this.autorId = autorId;
    }

    public String autorId() {
        return autorId;
    }
}
