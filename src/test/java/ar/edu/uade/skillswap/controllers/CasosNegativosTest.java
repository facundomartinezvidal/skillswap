package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.controllers.impl.SkillSwapControllers;
import ar.edu.uade.skillswap.model.Models.EstadoContratacion;
import ar.edu.uade.skillswap.model.Models.EstadoPublicacion;
import ar.edu.uade.skillswap.model.Models.Publicacion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static ar.edu.uade.skillswap.controllers.EntornoDemo.ANA;
import static ar.edu.uade.skillswap.controllers.EntornoDemo.LUCIA;
import static ar.edu.uade.skillswap.controllers.EntornoDemo.MATEO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Casos negativos de la consigna a través de los controllers: cada regla violada llega a la UI como
 * {@link OperacionRechazadaException} con el mensaje del dominio o del caso de uso.
 */
class CasosNegativosTest {

    private final EntornoDemo demo = new EntornoDemo();
    private final SkillSwapControllers c = demo.controllers;
    private final Publicacion mentoria = demo.mateoPublica("Testing con JUnit", 100);

    @Test
    void transicionInvalidaDeUnaPublicacion() {
        var borrador = c.publicaciones().crearBorrador(MATEO, "Borrador", "", "Tecnología", null, 60, 100);

        assertRechazada("No se puede pausar en estado BORRADOR",
                () -> c.publicaciones().cambiarEstado(MATEO, borrador.id(), EstadoPublicacion.PAUSADA));
        assertRechazada("Una publicación no puede volver a BORRADOR",
                () -> c.publicaciones().cambiarEstado(MATEO, mentoria.id(), EstadoPublicacion.BORRADOR));
    }

    @Test
    void transicionInvalidaDeUnaContratacion() {
        var solicitada = c.contrataciones().solicitar(ANA, mentoria.id(), "Aprender");

        assertRechazada("No se puede completar en estado PENDIENTE",
                () -> c.contrataciones().completar(MATEO, solicitada.id()));
        assertRechazada("La contratación está PENDIENTE: la tiene que aceptar el proveedor",
                () -> c.contrataciones().avanzar(ANA, solicitada.id()));
    }

    @Test
    void unaContratacionCompletadaNoAvanzaMas() {
        var completada = demo.completada(ANA, mentoria);

        assertRechazada("La contratación está COMPLETADA: no tiene más pasos",
                () -> c.contrataciones().avanzar(MATEO, completada.id()));
    }

    @Test
    void saldoInsuficiente() {
        var cara = demo.mateoPublica("Mentoría intensiva", 5000);
        var contratacion = c.contrataciones().solicitar(LUCIA, cara.id(), "Aprender");
        c.contrataciones().aceptar(MATEO, contratacion.id());
        var saldo = demo.saldo(LUCIA);

        assertRechazada("Saldo insuficiente: hay " + saldo + " SkillCoins y se necesitan 5000",
                () -> c.contrataciones().pagar(LUCIA, contratacion.id()));
        assertEquals(saldo, demo.saldo(LUCIA));
    }

    @Test
    void cupoAgotadoAlSolicitar() {
        demo.contratadaYPagada(ANA, mentoria);

        var error = assertThrows(OperacionRechazadaException.class,
                () -> c.contrataciones().solicitar(LUCIA, mentoria.id(), "Aprender"));

        assertTrue(error.getMessage().endsWith("ya tiene sus 1 lugares reservados"), error.getMessage());
    }

    @Test
    void doblePago() {
        var pagada = demo.contratadaYPagada(ANA, mentoria);
        var saldo = demo.saldo(ANA);

        assertRechazada("No se puede pagar en estado CONFIRMADA", () -> c.contrataciones().pagar(ANA, pagada.id()));
        assertRechazada("La contratación está CONFIRMADA: la tiene que iniciar el proveedor",
                () -> c.contrataciones().avanzar(ANA, pagada.id()));
        assertEquals(saldo, demo.saldo(ANA), "se cobró una sola vez");
        assertEquals(EstadoContratacion.CONFIRMADA, pagada.estado());
    }

    @Test
    void calificacionNoAutorizada() {
        var pagada = demo.contratadaYPagada(ANA, mentoria);

        assertRechazada("No se puede calificar en estado CONFIRMADA",
                () -> c.calificaciones().calificar(ANA, pagada.id(), 5, null));

        c.contrataciones().iniciar(MATEO, pagada.id());
        c.contrataciones().completar(MATEO, pagada.id());
        assertRechazada("El usuario user-lucia no puede calificar esta contratación",
                () -> c.calificaciones().calificar(LUCIA, pagada.id(), 5, null));
    }

    @Test
    void erroresDeAplicacionYDatosInvalidos() {
        assertRechazada("No existe perfil con id user-nadie", () -> c.perfiles().verPerfil("user-nadie"));
        assertRechazada("El usuario user-ana no tiene permiso para pausar una publicación ajena",
                () -> c.publicaciones().cambiarEstado(ANA, mentoria.id(), EstadoPublicacion.PAUSADA));
        assertRechazada("No existe contratación con id no-es-un-id",
                () -> c.contrataciones().avanzar(MATEO, "no-es-un-id"));
    }

    private static void assertRechazada(String mensaje, Executable operacion) {
        var error = assertThrows(OperacionRechazadaException.class, operacion);
        assertEquals(mensaje, error.getMessage());
    }
}
