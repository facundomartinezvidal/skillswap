package ar.edu.uade.skillswap.controllers;

/**
 * Único error que los controllers le muestran a la UI. Lleva el mensaje de la regla de negocio o del
 * caso de uso que rechazó la operación, listo para mostrar en pantalla.
 */
public class OperacionRechazadaException extends RuntimeException {

    public OperacionRechazadaException(String mensaje) {
        super(mensaje);
    }

    public OperacionRechazadaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
