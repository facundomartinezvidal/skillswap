package ar.edu.uade.skillswap.application.shared.exception;

/** Regla 10: el usuario que actúa no tiene permiso sobre lo que quiere modificar. */
public class PermisoDenegadoException extends ApplicationException {

    public PermisoDenegadoException(String userId, String accion) {
        super("El usuario " + userId + " no tiene permiso para " + accion);
    }
}
