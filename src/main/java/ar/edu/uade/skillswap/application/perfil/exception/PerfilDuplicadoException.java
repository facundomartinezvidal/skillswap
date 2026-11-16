package ar.edu.uade.skillswap.application.perfil.exception;

import ar.edu.uade.skillswap.application.shared.exception.ApplicationException;

/** Un usuario tiene un solo perfil. */
public class PerfilDuplicadoException extends ApplicationException {

    public PerfilDuplicadoException(String userId) {
        super("El usuario " + userId + " ya tiene un perfil");
    }
}
