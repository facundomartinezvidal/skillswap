package ar.edu.uade.skillswap.application.notificacion;

import ar.edu.uade.skillswap.domain.perfil.CanalNotificacion;

import java.util.Objects;

/**
 * Simple Factory: elige cómo se envía una notificación según el canal que prefiere el usuario.
 * Email y push son proveedores externos (adapters); la notificación interna ya queda en la bandeja,
 * así que no se envía por ningún lado. Agregar un canal nuevo es sumar un caso acá y su adapter.
 */
public class CanalEnvioFactory {

    /** Para el canal interno: la bandeja alcanza, no hay que mandar nada afuera. */
    private static final EnvioNotificacion SIN_ENVIO = (destinatarioId, titulo, detalle) -> { };

    private final EnvioNotificacion email;
    private final EnvioNotificacion push;

    public CanalEnvioFactory(EnvioNotificacion email, EnvioNotificacion push) {
        this.email = Objects.requireNonNull(email, "El envío por email es obligatorio");
        this.push = Objects.requireNonNull(push, "El envío push es obligatorio");
    }

    public EnvioNotificacion para(CanalNotificacion canal) {
        Objects.requireNonNull(canal, "El canal es obligatorio");
        return switch (canal) {
            case EMAIL -> email;
            case PUSH -> push;
            case INTERNA -> SIN_ENVIO;
        };
    }
}
