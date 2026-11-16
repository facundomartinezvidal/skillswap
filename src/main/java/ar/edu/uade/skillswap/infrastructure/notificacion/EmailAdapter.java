package ar.edu.uade.skillswap.infrastructure.notificacion;

import ar.edu.uade.skillswap.application.notificacion.EnvioNotificacion;

import java.util.Objects;

/**
 * Patrón Adapter: hace que el proveedor de email simulado cumpla el puerto {@link EnvioNotificacion}
 * de la aplicación. Traduce el usuario a una dirección y el aviso a asunto y cuerpo. Cambiar de
 * proveedor es escribir otro adapter; la aplicación no cambia.
 */
public class EmailAdapter implements EnvioNotificacion {

    /** En la demo cada userId tiene una casilla en este dominio; con identidad real vendría del perfil. */
    static final String DOMINIO_DEMO = "@skillswap.demo";

    private final ProveedorEmailSimulado proveedor;

    public EmailAdapter(ProveedorEmailSimulado proveedor) {
        this.proveedor = Objects.requireNonNull(proveedor);
    }

    @Override
    public void enviar(String destinatarioId, String titulo, String detalle) {
        proveedor.enviarCorreo(new ProveedorEmailSimulado.Correo(destinatarioId + DOMINIO_DEMO,
                "[SkillSwap] " + titulo, detalle));
    }
}
