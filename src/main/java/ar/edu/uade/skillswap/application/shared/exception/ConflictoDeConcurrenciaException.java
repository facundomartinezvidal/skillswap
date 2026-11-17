package ar.edu.uade.skillswap.application.shared.exception;

/**
 * Otra operación modificó los mismos datos mientras esta transacción estaba en curso (por ejemplo, dos
 * proveedores aceptando a la vez el último lugar de una franja). La infraestructura la lanza cuando el
 * bloqueo optimista detecta el conflicto; la transacción ya se deshizo, así que se puede reintentar.
 */
public class ConflictoDeConcurrenciaException extends ApplicationException {

    public ConflictoDeConcurrenciaException() {
        super("Otra operación modificó los mismos datos al mismo tiempo; volvé a intentar");
    }
}
