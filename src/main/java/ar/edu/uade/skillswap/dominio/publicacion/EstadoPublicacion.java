package ar.edu.uade.skillswap.dominio.publicacion;

/**
 * Patrón State (decisión D1). Cada acción devuelve el estado siguiente o lanza
 * {@link TransicionInvalidaException}. Los estados concretos sobrescriben solo lo que permiten.
 * Los estados no tienen efectos: la {@link Publicacion} valida sus requisitos y registra el cambio.
 */
public sealed interface EstadoPublicacion
        permits Borrador, Publicada, Pausada, Cerrada, Cancelada {

    String nombre();

    default EstadoPublicacion publicar() {
        throw new TransicionInvalidaException(nombre(), "publicar");
    }

    default EstadoPublicacion pausar() {
        throw new TransicionInvalidaException(nombre(), "pausar");
    }

    default EstadoPublicacion reanudar() {
        throw new TransicionInvalidaException(nombre(), "reanudar");
    }

    default EstadoPublicacion cerrar() {
        throw new TransicionInvalidaException(nombre(), "cerrar");
    }

    default EstadoPublicacion cancelar() {
        throw new TransicionInvalidaException(nombre(), "cancelar");
    }

    /** Lanza la excepción si en este estado no se puede modificar el contenido. */
    default void editar() {
        throw new TransicionInvalidaException(nombre(), "editar");
    }

    default boolean admiteSolicitudes() {
        return false;
    }
}
