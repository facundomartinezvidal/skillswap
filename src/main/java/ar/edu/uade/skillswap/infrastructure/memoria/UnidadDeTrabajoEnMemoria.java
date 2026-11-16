package ar.edu.uade.skillswap.infrastructure.memoria;

import ar.edu.uade.skillswap.application.shared.UnidadDeTrabajo;

import java.util.function.Supplier;

/**
 * Transacción para los repositorios en memoria. Alcanza con ejecutar la operación porque esos
 * repositorios guardan y devuelven copias: si la operación falla antes de llamar a {@code save},
 * los objetos que se modificaron se descartan y lo guardado no cambia, igual que con un rollback.
 * Por eso los casos de uso modifican todos los agregados primero y guardan al final.
 */
public class UnidadDeTrabajoEnMemoria implements UnidadDeTrabajo {

    @Override
    public <T> T ejecutar(Supplier<T> operacion) {
        return operacion.get();
    }
}
