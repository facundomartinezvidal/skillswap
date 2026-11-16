package ar.edu.uade.skillswap.application.shared;

import java.util.function.Supplier;

/**
 * Puerto de transacción: lo que pasa adentro de {@link #ejecutar} se confirma todo junto o no se
 * confirma nada. Lo usan los casos de uso que tocan más de un agregado (aceptar, pagar, completar,
 * cancelar, reprogramar, calificar). La infraestructura lo implementa con {@code EntityTransaction}.
 */
public interface UnidadDeTrabajo {

    /** Ejecuta la operación en una transacción y devuelve su resultado. Si lanza una excepción, deshace todo. */
    <T> T ejecutar(Supplier<T> operacion);
}
