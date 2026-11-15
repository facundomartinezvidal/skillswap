package ar.edu.uade.skillswap.domain.perfil.exception;

import ar.edu.uade.skillswap.domain.shared.DomainException;

/** Un perfil dado de baja no se puede editar. */
public class PerfilInactivoException extends DomainException {

    private final String userId;

    public PerfilInactivoException(String userId, String accion) {
        super("El perfil " + userId + " está dado de baja: no se puede " + accion);
        this.userId = userId;
    }

    public String userId() {
        return userId;
    }
}
