package ar.edu.uade.skillswap.domain.contratacion.state;

import ar.edu.uade.skillswap.domain.shared.TransicionInvalidaException;

/**
 * Patrón State. Cada acción devuelve el estado siguiente o lanza
 * {@link TransicionInvalidaException}. Los estados concretos sobrescriben solo lo que permiten.
 * Los estados no tienen efectos: la {@link ar.edu.uade.skillswap.domain.contratacion.Contratacion}
 * valida roles y registra el cambio.
 */
public sealed interface EstadoContratacion
        permits Pendiente, Aceptada, Confirmada, EnCurso, Completada, Rechazada, Cancelada {

    String nombre();

    default EstadoContratacion aceptar() {
        throw new TransicionInvalidaException(nombre(), "aceptar");
    }

    default EstadoContratacion rechazar() {
        throw new TransicionInvalidaException(nombre(), "rechazar");
    }

    default EstadoContratacion pagar() {
        throw new TransicionInvalidaException(nombre(), "pagar");
    }

    default EstadoContratacion iniciar() {
        throw new TransicionInvalidaException(nombre(), "iniciar");
    }

    default EstadoContratacion completar() {
        throw new TransicionInvalidaException(nombre(), "completar");
    }

    default EstadoContratacion cancelar() {
        throw new TransicionInvalidaException(nombre(), "cancelar");
    }

    /** La reprogramación no cambia el estado; solo se admite antes de que la sesión empiece. */
    default boolean admiteReprogramacion() {
        return false;
    }
}
