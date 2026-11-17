package ar.edu.uade.skillswap.infrastructure.notificacion;

import ar.edu.uade.skillswap.application.notificacion.EnvioNotificacion;
import ar.edu.uade.skillswap.infrastructure.identidad.ServicioIdentidadSimulado;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Cada adapter traduce el puerto {@link EnvioNotificacion} a la API de su proveedor simulado. */
class AdaptersDeNotificacionTest {

    @Test
    void elAdapterDeEmailArmaDireccionAsuntoYCuerpo() {
        var proveedor = new ProveedorEmailSimulado();
        EnvioNotificacion envio = new EmailAdapter(proveedor);

        envio.enviar("user-ana", "Pago confirmado", "El cliente pagó la contratación.");

        assertEquals(1, proveedor.enviados().size());
        var correo = proveedor.enviados().get(0);
        assertEquals("user-ana@skillswap.demo", correo.para());
        assertEquals("[SkillSwap] Pago confirmado", correo.asunto());
        assertEquals("El cliente pagó la contratación.", correo.cuerpo());
    }

    @Test
    void elAdapterDePushArmaTokenYTextoDeUnaLinea() {
        var proveedor = new ProveedorPushSimulado();
        EnvioNotificacion envio = new PushAdapter(proveedor);

        envio.enviar("user-ana", "Nueva solicitud", "Podés aceptarla o rechazarla.");

        var mensaje = proveedor.enviados().get(0);
        assertEquals("dispositivo-user-ana", mensaje.tokenDispositivo());
        assertEquals("Nueva solicitud: Podés aceptarla o rechazarla.", mensaje.texto());
    }

    @Test
    void elServicioDeIdentidadSimuladoConoceSoloASusUsuarios() {
        var identidad = ServicioIdentidadSimulado.conUsuariosDeDemo();

        assertTrue(identidad.existe("user-mateo"), "el usuario activo de la UI");
        assertFalse(identidad.existe("user-desconocido"));
        assertFalse(identidad.existe(null));
        assertTrue(new ServicioIdentidadSimulado(Set.of("user-x")).existe("user-x"));
    }
}
