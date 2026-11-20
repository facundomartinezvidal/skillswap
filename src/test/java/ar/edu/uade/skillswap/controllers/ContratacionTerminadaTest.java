package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.controllers.impl.SkillSwapControllers;
import ar.edu.uade.skillswap.model.Models.EstadoContratacion;
import ar.edu.uade.skillswap.model.Models.Publicacion;
import org.junit.jupiter.api.Test;

import static ar.edu.uade.skillswap.controllers.EntornoDemo.ANA;
import static ar.edu.uade.skillswap.controllers.EntornoDemo.MATEO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RECHAZADA y CANCELADA son estados finales: se consulta el historial y, tras un rechazo, el cliente pide de nuevo. */
class ContratacionTerminadaTest {

    private final EntornoDemo demo = new EntornoDemo();
    private final SkillSwapControllers c = demo.controllers;
    private final Publicacion mentoria = demo.mateoPublica("Testing con JUnit", 100);

    @Test
    void elHistorialDeUnaRechazadaMuestraQuienYPorQue() {
        var solicitada = c.contrataciones().solicitar(ANA, mentoria.id(), "Aprender");
        c.contrataciones().rechazar(MATEO, solicitada.id(), "Sin agenda esta semana");

        var historial = c.contrataciones().verHistorial(ANA, solicitada.id());

        var rechazo = historial.get(historial.size() - 1);
        assertTrue(rechazo.contains("PENDIENTE → RECHAZADA"));
        assertTrue(rechazo.contains("Sin agenda esta semana"));
    }

    @Test
    void elClienteVuelveASolicitarConUnaContratacionNueva() {
        var solicitada = c.contrataciones().solicitar(ANA, mentoria.id(), "Aprender");
        c.contrataciones().rechazar(MATEO, solicitada.id(), "Sin agenda");

        assertTrue(c.contrataciones().esCliente(ANA, solicitada.id()));
        var nueva = c.contrataciones().volverASolicitar(ANA, solicitada.id());

        assertNotEquals(solicitada.id(), nueva.id());
        assertEquals(EstadoContratacion.PENDIENTE, nueva.estado());
    }

    @Test
    void elProveedorNoVuelveASolicitarNiSeReabreUnaQueNoEstaRechazada() {
        var solicitada = c.contrataciones().solicitar(ANA, mentoria.id(), "Aprender");

        assertThrows(OperacionRechazadaException.class, () -> c.contrataciones().volverASolicitar(ANA, solicitada.id()));
        c.contrataciones().rechazar(MATEO, solicitada.id(), "Sin agenda");
        assertFalse(c.contrataciones().esCliente(MATEO, solicitada.id()));
        assertThrows(OperacionRechazadaException.class,
                () -> c.contrataciones().volverASolicitar(MATEO, solicitada.id()));
    }

    @Test
    void unAjenoNoVeElHistorial() {
        var solicitada = c.contrataciones().solicitar(ANA, mentoria.id(), "Aprender");

        assertThrows(OperacionRechazadaException.class,
                () -> c.contrataciones().verHistorial(EntornoDemo.LUCIA, solicitada.id()));
    }
}
