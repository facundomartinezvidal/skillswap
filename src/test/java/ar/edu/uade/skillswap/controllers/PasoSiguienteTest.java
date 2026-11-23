package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.controllers.impl.SkillSwapControllers;
import ar.edu.uade.skillswap.model.Models.EstadoContratacion;
import org.junit.jupiter.api.Test;

import static ar.edu.uade.skillswap.controllers.EntornoDemo.ANA;
import static ar.edu.uade.skillswap.controllers.EntornoDemo.MATEO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link ContratacionOperaciones#pasoSiguiente} le dice a cada parte qué le toca, y coincide con lo que hace
 * {@code avanzar}: quien tiene acción puede avanzar y quien no la tiene, no.
 */
class PasoSiguienteTest {

    private final EntornoDemo demo = new EntornoDemo();
    private final SkillSwapControllers c = demo.controllers;

    @Test
    void cadaParteVeLoQueLeTocaEnTodoElFlujo() {
        var publicacion = demo.mateoPublica("Mentoría de Git", 100);
        var id = c.contrataciones().solicitar(ANA, publicacion.id(), "Aprender ramas").id();

        assertPaso(MATEO, id, "Proveedor", "Aceptar",
                "Ana Gómez te pidió esta mentoría. Al aceptarla se reserva el lugar en la franja.");
        assertPaso(ANA, id, "Cliente", null, "Esperando que Mateo Silva acepte tu solicitud.");
        avanzaSoloQuienTieneAccion(MATEO, ANA, id, EstadoContratacion.ACEPTADA);

        assertPaso(ANA, id, "Cliente", "Pagar 100 SkillCoins", "Mateo Silva aceptó. Pagá para confirmar la sesión.");
        assertPaso(MATEO, id, "Proveedor", null, "Aceptaste. Esperando el pago de Ana Gómez.");
        avanzaSoloQuienTieneAccion(ANA, MATEO, id, EstadoContratacion.CONFIRMADA);

        assertPaso(MATEO, id, "Proveedor", "Iniciar sesión", "Ana Gómez ya pagó. Iniciá la sesión a la hora de la franja.");
        assertPaso(ANA, id, "Cliente", null, "Pago confirmado. Esperando que Mateo Silva inicie la sesión.");
        avanzaSoloQuienTieneAccion(MATEO, ANA, id, EstadoContratacion.EN_CURSO);

        assertPaso(MATEO, id, "Proveedor", "Completar sesión", "La sesión está en curso. Completala al terminar.");
        assertPaso(ANA, id, "Cliente", null, "La sesión está en curso.");
        avanzaSoloQuienTieneAccion(MATEO, ANA, id, EstadoContratacion.COMPLETADA);

        assertPaso(ANA, id, "Cliente", null, "Sesión completada. Podés calificar a Mateo Silva.");
        assertPaso(MATEO, id, "Proveedor", null, "Sesión completada. Podés calificar a Ana Gómez.");
    }

    @Test
    void rechazadaYCancelada() {
        var publicacion = demo.mateoPublica("Mentoría de Git", 100);
        var rechazada = c.contrataciones().solicitar(ANA, publicacion.id(), "Aprender ramas").id();
        c.contrataciones().rechazar(MATEO, rechazada, "Sin agenda");
        var cancelada = c.contrataciones().solicitar(ANA, publicacion.id(), "Aprender merges").id();
        c.contrataciones().cancelar(ANA, cancelada);

        assertPaso(ANA, rechazada, "Cliente", null, "Mateo Silva rechazó la solicitud.");
        assertPaso(MATEO, rechazada, "Proveedor", null, "Rechazaste la solicitud.");
        assertPaso(ANA, cancelada, "Cliente", null, "La contratación se canceló.");
    }

    private void assertPaso(String userId, String contratacionId, String rol, String accion, String detalle) {
        assertEquals(new PasoSiguienteVista(rol, accion, detalle, null, false),
                c.contrataciones().pasoSiguiente(userId, contratacionId));
    }

    private void avanzaSoloQuienTieneAccion(String conAccion, String sinAccion, String id, EstadoContratacion despues) {
        assertThrows(OperacionRechazadaException.class, () -> c.contrataciones().avanzar(sinAccion, id));
        assertEquals(despues, c.contrataciones().avanzar(conAccion, id).estado());
    }

    @Test
    void quienNoParticipaNoVeLaContratacion() {
        var publicacion = demo.mateoPublica("Mentoría de Git", 100);
        var id = c.contrataciones().solicitar(ANA, publicacion.id(), "Aprender ramas").id();

        assertThrows(OperacionRechazadaException.class, () -> c.contrataciones().pasoSiguiente(EntornoDemo.LUCIA, id));
    }
}
