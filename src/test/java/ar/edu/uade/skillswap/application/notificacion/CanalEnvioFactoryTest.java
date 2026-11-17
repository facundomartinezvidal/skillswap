package ar.edu.uade.skillswap.application.notificacion;

import ar.edu.uade.skillswap.domain.perfil.CanalNotificacion;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CanalEnvioFactoryTest {

    private final List<String> enviados = new ArrayList<>();
    private final EnvioNotificacion email = (destinatario, titulo, detalle) -> enviados.add("email:" + destinatario);
    private final EnvioNotificacion push = (destinatario, titulo, detalle) -> enviados.add("push:" + destinatario);
    private final CanalEnvioFactory factory = new CanalEnvioFactory(email, push);

    @Test
    void eligeElEnvioSegunElCanal() {
        assertSame(email, factory.para(CanalNotificacion.EMAIL));
        assertSame(push, factory.para(CanalNotificacion.PUSH));
    }

    @Test
    void elCanalInternoNoEnviaNada() {
        factory.para(CanalNotificacion.INTERNA).enviar("user-ana", "Título", "Detalle");

        assertTrue(enviados.isEmpty());
    }

    @Test
    void rechazaCanalYEnviosNulos() {
        assertThrows(NullPointerException.class, () -> factory.para(null));
        assertThrows(NullPointerException.class, () -> new CanalEnvioFactory(null, push));
    }
}
