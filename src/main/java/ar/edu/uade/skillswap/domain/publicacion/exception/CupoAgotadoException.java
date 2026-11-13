package ar.edu.uade.skillswap.domain.publicacion.exception;

import ar.edu.uade.skillswap.domain.publicacion.FranjaId;
import ar.edu.uade.skillswap.domain.shared.DomainException;

public class CupoAgotadoException extends DomainException {

    public CupoAgotadoException(FranjaId franjaId, int cupo) {
        super("La franja " + franjaId.valor() + " ya tiene sus " + cupo + " lugares reservados");
    }
}
