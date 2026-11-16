package ar.edu.uade.skillswap.controllers.impl;

import ar.edu.uade.skillswap.application.shared.exception.ApplicationException;
import ar.edu.uade.skillswap.controllers.OperacionRechazadaException;
import ar.edu.uade.skillswap.domain.shared.DomainException;

import java.util.function.Supplier;

/**
 * El único lugar donde se traducen los errores para la UI. Todas las operaciones de los controllers
 * pasan por acá: una regla de negocio violada, un error del caso de uso o un dato inválido llegan a la
 * pantalla como {@link OperacionRechazadaException} con el mensaje original.
 */
final class ManejoDeErrores {

    private ManejoDeErrores() {
    }

    static <T> T ejecutar(Supplier<T> operacion) {
        try {
            return operacion.get();
        } catch (DomainException | ApplicationException | IllegalArgumentException e) {
            throw new OperacionRechazadaException(e.getMessage(), e);
        }
    }

    static void ejecutarSinResultado(Runnable operacion) {
        ejecutar(() -> {
            operacion.run();
            return null;
        });
    }
}
