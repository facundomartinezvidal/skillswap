package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.application.busqueda.BusquedaService;
import ar.edu.uade.skillswap.application.busqueda.recomendacion.PopularidadReciente;
import ar.edu.uade.skillswap.controllers.impl.RecomendacionControllerImpl;
import ar.edu.uade.skillswap.controllers.impl.SkillSwapControllers;
import ar.edu.uade.skillswap.domain.perfil.CanalNotificacion;
import ar.edu.uade.skillswap.infrastructure.memoria.BuscadorPublicacionesEnMemoria;
import ar.edu.uade.skillswap.model.Models.EstadoContratacion;
import ar.edu.uade.skillswap.model.Models.FiltrosBusqueda;
import ar.edu.uade.skillswap.model.Models.Modalidad;
import ar.edu.uade.skillswap.model.Models.Publicacion;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static ar.edu.uade.skillswap.controllers.EntornoDemo.ANA;
import static ar.edu.uade.skillswap.controllers.EntornoDemo.LUCIA;
import static ar.edu.uade.skillswap.controllers.EntornoDemo.MATEO;
import static ar.edu.uade.skillswap.model.Models.EstadoPublicacion.BORRADOR;
import static ar.edu.uade.skillswap.model.Models.EstadoPublicacion.PUBLICADA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los 10 casos de aceptación de la consigna, de punta a punta: controllers, casos de uso y backend en
 * memoria con los datos de demo. Los errores llegan como {@link OperacionRechazadaException} con un
 * mensaje que la UI puede mostrar.
 */
class CasosDeAceptacionTest {

    private final EntornoDemo demo = new EntornoDemo();
    private final SkillSwapControllers c = demo.controllers;

    @Test
    void caso1_publicarUnaMentoriaValidaYEncontrarlaConBusquedaFiltrada() {
        var publicada = demo.mateoPublica("Testing con JUnit", 90);

        var encontradas = c.busqueda().buscar(new FiltrosBusqueda("junit", "Tecnología", Modalidad.REMOTA, 100,
                "Menor precio"));
        var demasiadoCaras = c.busqueda().buscar(new FiltrosBusqueda("junit", "Todas", null, 50, "Relevancia"));

        assertEquals(PUBLICADA, publicada.estado());
        assertEquals(List.of(publicada.id()), encontradas.stream().map(Publicacion::id).toList());
        assertEquals(60, encontradas.get(0).duracionMinutos());
        assertEquals("Mateo Silva", encontradas.get(0).proveedor());
        assertTrue(demasiadoCaras.isEmpty());
    }

    @Test
    void caso2_rechazarLaPublicacionDeUnaOfertaIncompleta() {
        var borrador = c.publicaciones().crearBorrador(MATEO, "Sin duración", "", "Tecnología", Modalidad.REMOTA,
                0, 100);

        var error = assertThrows(OperacionRechazadaException.class,
                () -> c.publicaciones().cambiarEstado(MATEO, borrador.id(), PUBLICADA));

        assertEquals("No se puede publicar. Falta: duración mayor a 0, al menos una franja de disponibilidad",
                error.getMessage());
        assertEquals(BORRADOR, c.publicaciones().listarPropias(MATEO).stream()
                .filter(p -> p.id().equals(borrador.id())).findFirst().orElseThrow().estado());
    }

    @Test
    void caso3_solicitarAceptarYPagarConSaldoSuficiente() {
        var mentoria = demo.mateoPublica("Testing con JUnit", 150);
        var saldoAna = demo.saldo(ANA);

        var solicitada = c.contrataciones().solicitar(ANA, mentoria.id(), "Aprender a testear");
        var aceptada = c.contrataciones().avanzar(MATEO, solicitada.id());
        var pagada = c.contrataciones().avanzar(ANA, solicitada.id());

        assertEquals(EstadoContratacion.PENDIENTE, solicitada.estado());
        assertEquals("Mateo Silva", solicitada.contraparte());
        assertEquals(EstadoContratacion.ACEPTADA, aceptada.estado());
        assertEquals("Ana Gómez", aceptada.contraparte());
        assertEquals(EstadoContratacion.CONFIRMADA, pagada.estado());
        assertEquals(saldoAna - 150, demo.saldo(ANA));
        assertEquals(-150, c.wallet().listarMovimientos(ANA).get(0).importe());
    }

    @Test
    void caso4_dosAceptacionesNoConsumenElUltimoCupo() {
        var mentoria = demo.mateoPublica("Testing con JUnit", 100);
        var deAna = c.contrataciones().solicitar(ANA, mentoria.id(), "Aprender");
        var deLucia = c.contrataciones().solicitar(LUCIA, mentoria.id(), "Aprender");

        c.contrataciones().avanzar(MATEO, deAna.id());
        var error = assertThrows(OperacionRechazadaException.class,
                () -> c.contrataciones().avanzar(MATEO, deLucia.id()));

        assertTrue(error.getMessage().endsWith("ya tiene sus 1 lugares reservados"), error.getMessage());
        assertEquals(EstadoContratacion.PENDIENTE, estadoDe(LUCIA, deLucia.id()));
    }

    @Test
    void caso5_unPagoConSaldoInsuficienteNoModificaSaldosNiEstado() {
        var cara = demo.mateoPublica("Mentoría intensiva", 5000);
        var contratacion = c.contrataciones().solicitar(ANA, cara.id(), "Aprender");
        c.contrataciones().avanzar(MATEO, contratacion.id());
        var saldoAna = demo.saldo(ANA);
        var saldoMateo = demo.saldo(MATEO);

        var error = assertThrows(OperacionRechazadaException.class,
                () -> c.contrataciones().avanzar(ANA, contratacion.id()));

        assertEquals("Saldo insuficiente: hay " + saldoAna + " SkillCoins y se necesitan 5000", error.getMessage());
        assertEquals(saldoAna, demo.saldo(ANA));
        assertEquals(saldoMateo, demo.saldo(MATEO));
        assertEquals(EstadoContratacion.ACEPTADA, estadoDe(ANA, contratacion.id()));
    }

    @Test
    void caso6_cancelarYCalcularElReintegroConDosPoliticasDistintas() {
        var borrador = c.publicaciones().crearBorrador(MATEO, "Mentoría express", "Una hora", "Tecnología",
                Modalidad.REMOTA, 60, 100);
        var en30Horas = LocalDateTime.ofInstant(EntornoDemo.AHORA.plus(Duration.ofHours(30)), ZoneId.systemDefault());
        c.publicaciones().agregarFranja(MATEO, borrador.id(), en30Horas, en30Horas.plusHours(1));
        var mentoria = c.publicaciones().cambiarEstado(MATEO, borrador.id(), PUBLICADA);
        var saldoAna = demo.saldo(ANA);
        var saldoLucia = demo.saldo(LUCIA);

        // Cancela el cliente con 30 h de anticipación: le vuelve el 50 %.
        var deAna = demo.contratadaYPagada(ANA, mentoria);
        var canceladaPorAna = c.contrataciones().cancelar(ANA, deAna.id(), "Me surgió un viaje");
        // Cancela el proveedor: al cliente le vuelve todo.
        var deLucia = demo.contratadaYPagada(LUCIA, mentoria);
        var canceladaPorMateo = c.contrataciones().cancelar(MATEO, deLucia.id());

        assertEquals(deAna.fecha(), deLucia.fecha(), "las dos usan la franja de 30 h");
        assertEquals(EstadoContratacion.CANCELADA, canceladaPorAna.estado());
        assertEquals(EstadoContratacion.CANCELADA, canceladaPorMateo.estado());
        assertEquals(saldoAna - 50, demo.saldo(ANA));
        assertEquals(saldoLucia, demo.saldo(LUCIA));
    }

    @Test
    void caso7_cambiarLaEstrategiaDeRecomendacionSinCambiarElCasoDeUso() {
        var b = demo.backend;
        var porPopularidad = new RecomendacionControllerImpl(new BusquedaService(
                new BuscadorPublicacionesEnMemoria(b.publicaciones, b.perfiles), b.publicaciones, b.perfiles,
                new PopularidadReciente(b.contrataciones, demo.clock)));

        var afinidad = titulos(c.recomendaciones().recomendar(ANA));
        var popularidad = titulos(porPopularidad.recomendar(ANA));

        // Ana tiene interés en Java y Entrevistas: la afinidad pone primero la de entrevistas técnicas.
        assertEquals("Preparación para entrevistas técnicas", afinidad.get(0));
        // La popularidad no mira el perfil: esa tuvo una sola aceptación en los últimos 30 días y hay
        // varias con dos, así que no entra entre las tres más populares.
        assertEquals(3, popularidad.size());
        assertFalse(popularidad.contains("Preparación para entrevistas técnicas"), popularidad.toString());
        assertNotEquals(afinidad, popularidad);
    }

    @Test
    void caso8_dosTiposDeNotificacionAnteUnMismoEvento() {
        c.perfiles().elegirCanal(MATEO, CanalNotificacion.PUSH);
        var mentoria = demo.mateoPublica("Testing con JUnit", 100);
        var contratacion = demo.contratadaYPagada(ANA, mentoria);
        c.contrataciones().avanzar(MATEO, contratacion.id());
        var correos = demo.backend.email.enviados().size();
        var pushes = demo.backend.push.enviados().size();

        c.contrataciones().avanzar(MATEO, contratacion.id());

        assertEquals(correos + 1, demo.backend.email.enviados().size(), "Ana eligió email");
        assertEquals(pushes + 1, demo.backend.push.enviados().size(), "Mateo eligió push");
        assertEquals("SESION_COMPLETADA", c.notificaciones().listarNotificaciones(ANA).get(0).tipo());
        assertEquals("SESION_COMPLETADA", c.notificaciones().listarNotificaciones(MATEO).get(0).tipo());
    }

    @Test
    void caso9_promocionesAcumulablesSinPrecioNegativo() {
        var borrador = c.publicaciones().crearBorrador(MATEO, "Mentoría promocionada", "Con descuento",
                "Tecnología", Modalidad.REMOTA, 60, 100);

        var conPorcentaje = c.publicaciones().agregarDescuentoPorcentual(MATEO, borrador.id(), 50);
        var conAmbas = c.publicaciones().agregarDescuentoFijo(MATEO, borrador.id(), 80);
        c.publicaciones().cambiarEstado(MATEO, borrador.id(), PUBLICADA);

        assertEquals(50, conPorcentaje.precio());
        assertEquals(0, conAmbas.precio());
        var encontrada = c.busqueda().buscar(new FiltrosBusqueda("promocionada", "Todas", null, 0, "Relevancia"));
        assertEquals(0, encontrada.get(0).precio());
        assertTrue(encontrada.get(0).destacada(), "con promoción se muestra destacada");
    }

    @Test
    void caso10_completarCalificarUnaSolaVezYRecalcularLaReputacion() {
        var mentoria = demo.mateoPublica("Testing con JUnit", 100);
        var antes = c.perfiles().verPerfil(MATEO);
        var completada = demo.completada(ANA, mentoria);

        var calificado = c.calificaciones().calificar(ANA, completada.id(), 2, "Le faltó preparación");
        var error = assertThrows(OperacionRechazadaException.class,
                () -> c.calificaciones().calificar(ANA, completada.id(), 5, "Cambié de opinión"));

        assertEquals(EstadoContratacion.COMPLETADA, completada.estado());
        assertEquals(5.0, antes.reputacion());
        assertEquals(2, antes.resenas());
        assertEquals(3, calificado.resenas());
        assertEquals(4.0, calificado.reputacion(), "(5 + 5 + 2) / 3");
        assertEquals(antes.proyectosCompletados() + 1, calificado.proyectosCompletados());
        assertEquals("El usuario user-ana ya calificó esta contratación", error.getMessage());
        assertEquals(4.0, c.perfiles().verPerfil(MATEO).reputacion());
    }

    private EstadoContratacion estadoDe(String userId, String contratacionId) {
        return c.contrataciones().listarContrataciones(userId).stream()
                .filter(x -> x.id().equals(contratacionId))
                .findFirst().orElseThrow().estado();
    }

    private static List<String> titulos(List<Publicacion> publicaciones) {
        return publicaciones.stream().map(Publicacion::titulo).toList();
    }
}
