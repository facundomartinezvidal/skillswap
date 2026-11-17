package ar.edu.uade.skillswap.infrastructure.notificacion;

import ar.edu.uade.skillswap.application.notificacion.EnvioNotificacion;

import java.util.Objects;

/**
 * Patrón Adapter: hace que el proveedor push simulado cumpla el puerto {@link EnvioNotificacion}
 * de la aplicación. Traduce el usuario a un token de dispositivo y el aviso a un texto de una línea.
 */
public class PushAdapter implements EnvioNotificacion {

    /** En la demo cada userId tiene un dispositivo con este prefijo; en real lo registraría la app móvil. */
    static final String PREFIJO_TOKEN_DEMO = "dispositivo-";

    private final ProveedorPushSimulado proveedor;

    public PushAdapter(ProveedorPushSimulado proveedor) {
        this.proveedor = Objects.requireNonNull(proveedor);
    }

    @Override
    public void enviar(String destinatarioId, String titulo, String detalle) {
        proveedor.notificar(PREFIJO_TOKEN_DEMO + destinatarioId, titulo + ": " + detalle);
    }
}
