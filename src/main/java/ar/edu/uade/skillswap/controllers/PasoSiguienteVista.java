package ar.edu.uade.skillswap.controllers;

import java.time.LocalDateTime;

/**
 * Lo que la vista muestra de una contratación para quien la mira: su rol ("Cliente" o "Proveedor"), la acción
 * que le toca (el texto del botón, o {@code null} si no le toca nada) y una explicación del estado. Si hay una
 * reprogramación pendiente, {@code reprogramacionPropuesta} es el nuevo inicio y {@code propuestaPorMi} dice si
 * la propuso quien mira (entonces espera) o la otra parte (entonces la acepta o la rechaza).
 */
public record PasoSiguienteVista(String rol, String accion, String detalle, LocalDateTime reprogramacionPropuesta,
                                 boolean propuestaPorMi) {
}
