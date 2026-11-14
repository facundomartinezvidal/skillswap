package ar.edu.uade.skillswap.domain.shared;

/** Acción que el estado actual de una máquina de estados del dominio no permite. */
public class TransicionInvalidaException extends DomainException {

    private final String estado;
    private final String accion;

    public TransicionInvalidaException(String estado, String accion) {
        super("No se puede " + accion + " en estado " + estado);
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
