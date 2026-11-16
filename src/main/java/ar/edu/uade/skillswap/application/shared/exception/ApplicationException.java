package ar.edu.uade.skillswap.application.shared.exception;

/**
 * Base de los errores de la capa de aplicación (no son reglas de un agregado, como las
 * {@code DomainException}). Los controllers atrapan las dos de la misma forma.
 */
public abstract class ApplicationException extends RuntimeException {

    protected ApplicationException(String message) {
        super(message);
    }
}
