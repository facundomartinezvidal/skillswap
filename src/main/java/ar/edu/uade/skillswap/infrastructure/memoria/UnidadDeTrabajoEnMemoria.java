package ar.edu.uade.skillswap.infrastructure.memoria;

import ar.edu.uade.skillswap.application.shared.UnidadDeTrabajo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Transacción para los repositorios en memoria, con rollback de verdad. Adentro de {@link #ejecutar},
 * cada {@code save} de un repositorio en memoria no se aplica enseguida: queda anotado. Si la operación
 * termina bien, se aplican todos juntos (confirmar); si lanza una excepción, se descartan (rollback).
 * Así la atomicidad no depende del orden en que el caso de uso llama a {@code save}.
 * <p>
 * Limitación: adentro de la transacción, un {@code find} no ve lo que se guardó en esa misma
 * transacción (ningún caso de uso lo necesita). Es para un solo hilo, como la demo.
 */
public class UnidadDeTrabajoEnMemoria implements UnidadDeTrabajo {

    /** Escrituras anotadas en la transacción en curso, o {@code null} si no hay una abierta. */
    private List<Runnable> pendientes;

    @Override
    public <T> T ejecutar(Supplier<T> operacion) {
        if (pendientes != null) {
            // Ya hay una transacción abierta: esta operación forma parte de ella.
            return operacion.get();
        }
        pendientes = new ArrayList<>();
        try {
            var resultado = operacion.get();
            pendientes.forEach(Runnable::run);
            return resultado;
        } finally {
            // Si hubo una excepción, las escrituras anotadas se pierden: es el rollback.
            pendientes = null;
        }
    }

    /** Lo llaman los repositorios en memoria al guardar. Fuera de una transacción, la escritura se aplica ya. */
    void registrar(Runnable escritura) {
        if (pendientes == null) {
            escritura.run();
        } else {
            pendientes.add(escritura);
        }
    }
}
