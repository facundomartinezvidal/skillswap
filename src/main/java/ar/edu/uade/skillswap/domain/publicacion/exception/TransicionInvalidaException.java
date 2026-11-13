package ar.edu.uade.skillswap.domain.publicacion.exception;

import ar.edu.uade.skillswap.domain.shared.DomainException;

public class TransicionInvalidaException extends DomainException {

    private final String estado;
    private final String accion;

    public TransicionInvalidaException(String estado, String accion) {
        super("No se puede " + accion + " una publicación en estado " + estado);
        this.estado = estado;
        this.accion = accion;
    }

    public String estado() {
        return estado;
    }

    public String accion() {
        return accion;
    }
}
