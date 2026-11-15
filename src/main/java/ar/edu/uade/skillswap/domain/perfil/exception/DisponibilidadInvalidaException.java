package ar.edu.uade.skillswap.domain.perfil.exception;

import ar.edu.uade.skillswap.domain.shared.DomainException;

/** La franja semanal está incompleta, termina antes de empezar o se superpone con otra del mismo día. */
public class DisponibilidadInvalidaException extends DomainException {

    public DisponibilidadInvalidaException(String mensaje) {
        super(mensaje);
    }
}
