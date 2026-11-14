package ar.edu.uade.skillswap.domain.contratacion.exception;

import ar.edu.uade.skillswap.domain.shared.DomainException;

/** El usuario no tiene el rol que la acción exige en esta contratación. */
public class AccionNoPermitidaException extends DomainException {

    private final String actorId;
    private final String accion;

    public AccionNoPermitidaException(String actorId, String accion) {
        super("El usuario " + actorId + " no puede " + accion + " esta contratación");
        this.actorId = actorId;
        this.accion = accion;
    }

    public String actorId() {
        return actorId;
    }

    public String accion() {
        return accion;
    }
}
