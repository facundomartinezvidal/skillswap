package ar.edu.uade.skillswap.application.publicacion.exception;

import ar.edu.uade.skillswap.application.shared.exception.ApplicationException;
import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;
import ar.edu.uade.skillswap.domain.publicacion.TipoPublicacion;

/**
 * Un profesional publica ofertas y una organización publica desafíos. La regla cruza dos módulos
 * (perfil y publicación), por eso la valida la capa de aplicación.
 */
public class TipoDePerfilIncompatibleException extends ApplicationException {

    public TipoDePerfilIncompatibleException(TipoPerfil perfil, TipoPublicacion publicacion) {
        super("Un perfil " + perfil + " no puede publicar una publicación de tipo " + publicacion);
    }
}
