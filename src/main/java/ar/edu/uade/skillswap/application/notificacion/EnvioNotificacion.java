package ar.edu.uade.skillswap.application.notificacion;

/**
 * Puerto hacia un proveedor externo de envío (email o push). La consigna los deja simulados pero
 * reemplazables: la infraestructura los implementa con adapters mock.
 */
public interface EnvioNotificacion {

    void enviar(String destinatarioId, String titulo, String detalle);
}
