package ar.edu.uade.skillswap.domain.shared;

/** Base de las excepciones que expresan una regla de negocio violada. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
