package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.model.Models.Contratacion;

/**
 * Las operaciones explícitas de la contratación que pide la consigna. La UI de la cátedra usa un
 * {@code avanzar} genérico; esta interfaz del equipo no cambia {@link UiControllers}.
 */
public interface ContratacionOperaciones {

    /** Las contrataciones como cliente o proveedor, de la más nueva a la más vieja. */
    PaginaVista<Contratacion> listarContrataciones(String userId, int pagina, int tamanio);

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
