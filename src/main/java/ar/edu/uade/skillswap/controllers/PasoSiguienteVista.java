package ar.edu.uade.skillswap.controllers;

/**
 * Lo que la vista muestra de una contratación para quien la mira: su rol ("Cliente" o "Proveedor"), la acción
 * que le toca (el texto del botón, o {@code null} si no le toca nada) y una explicación del estado.
 */
public record PasoSiguienteVista(String rol, String accion, String detalle) {
}
