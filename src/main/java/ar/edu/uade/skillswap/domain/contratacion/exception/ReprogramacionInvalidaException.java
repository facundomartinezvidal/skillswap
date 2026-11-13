package ar.edu.uade.skillswap.domain.contratacion.exception;

import ar.edu.uade.skillswap.domain.shared.DomainException;

/** La propuesta de reprogramación no cumple sus reglas (momento, franja o propuesta pendiente). */
public class ReprogramacionInvalidaException extends DomainException {

    public ReprogramacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
