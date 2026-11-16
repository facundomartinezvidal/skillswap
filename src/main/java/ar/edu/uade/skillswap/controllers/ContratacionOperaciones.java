package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.model.Models.Contratacion;

/**
 * Las operaciones explícitas de la contratación que pide la consigna. La UI de la cátedra usa un
 * {@code avanzar} genérico; esta interfaz del equipo no cambia {@link UiControllers}.
 */
public interface ContratacionOperaciones {

    Contratacion aceptar(String userId, String contratacionId);

    Contratacion rechazar(String userId, String contratacionId, String motivo);

    Contratacion pagar(String userId, String contratacionId);

    Contratacion iniciar(String userId, String contratacionId);

    Contratacion completar(String userId, String contratacionId);

    Contratacion cancelar(String userId, String contratacionId, String motivo);

    Contratacion proponerReprogramacion(String userId, String contratacionId, String nuevaFranjaId);

    Contratacion aceptarReprogramacion(String userId, String contratacionId);

    Contratacion rechazarReprogramacion(String userId, String contratacionId);
}
