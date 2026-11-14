package ar.edu.uade.skillswap.domain.publicacion.exception;

import ar.edu.uade.skillswap.domain.shared.DomainException;

public class FranjaInvalidaException extends DomainException {

    public FranjaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
