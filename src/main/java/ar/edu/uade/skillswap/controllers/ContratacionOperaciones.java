package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.model.Models.Contratacion;

import java.util.List;

/**
 * Las operaciones explícitas de la contratación que pide la consigna. La UI de la cátedra usa un
 * {@code avanzar} genérico; esta interfaz del equipo no cambia {@link UiControllers}.
 */
public interface ContratacionOperaciones {

    /** Las contrataciones como cliente o proveedor, de la más nueva a la más vieja. */
    PaginaVista<Contratacion> listarContrataciones(String userId, int pagina, int tamanio);

    /**
     * La solicitud completa que pide la consigna: el cliente elige la franja e indica mensaje y objetivo.
     * El dominio rechaza una franja que ya empezó, sin cupo o de otra publicación.
     */
    Contratacion solicitar(String userId, String publicacionId, String franjaId, String mensaje, String objetivo);

    Contratacion aceptar(String userId, String contratacionId);

    Contratacion rechazar(String userId, String contratacionId, String motivo);

    Contratacion pagar(String userId, String contratacionId);

    Contratacion iniciar(String userId, String contratacionId);

    Contratacion completar(String userId, String contratacionId);

    Contratacion cancelar(String userId, String contratacionId, String motivo);

    Contratacion proponerReprogramacion(String userId, String contratacionId, String nuevaFranjaId);

    Contratacion aceptarReprogramacion(String userId, String contratacionId);

    Contratacion rechazarReprogramacion(String userId, String contratacionId);

    /** El historial de estados en texto, del primero al último: fecha, cambio, quién y motivo. */
    List<String> verHistorial(String userId, String contratacionId);

    /**
     * Qué le toca a quien mira la contratación: su rol, el texto del botón que hace lo mismo que
     * {@code avanzar} (o null si no le toca nada) y una explicación del estado.
     */
    PasoSiguienteVista pasoSiguiente(String userId, String contratacionId);

    /**
     * El comprobante de la contratación en texto, una línea por dato: publicación, partes, sesión, importe,
     * fechas de pago y de cierre, reintegro si se canceló, y calificaciones.
     */
    List<String> verComprobante(String userId, String contratacionId);

    /** Las franjas de la publicación a las que se puede mover la sesión: disponibles y distintas de la actual. */
    List<FranjaVista> listarFranjasParaReprogramar(String userId, String contratacionId);

    /** Si quien mira la contratación es el cliente (el único que puede volver a solicitar). */
    boolean esCliente(String userId, String contratacionId);

    /** Después de un rechazo, el cliente pide de nuevo la misma publicación: es una contratación nueva. */
    Contratacion volverASolicitar(String userId, String contratacionId);
}
