package ar.edu.uade.skillswap.application.shared.exception;

/** Se pidió un perfil, publicación, contratación, wallet o notificación que no existe. */
public class RecursoNoEncontradoException extends ApplicationException {

    public RecursoNoEncontradoException(String recurso, String id) {
        super("No existe " + recurso + " con id " + id);
    }
}
