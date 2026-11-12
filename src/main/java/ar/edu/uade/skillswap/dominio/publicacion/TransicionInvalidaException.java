package ar.edu.uade.skillswap.dominio.publicacion;

public class TransicionInvalidaException extends RuntimeException {

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
