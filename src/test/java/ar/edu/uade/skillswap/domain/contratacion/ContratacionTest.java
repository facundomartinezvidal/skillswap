package ar.edu.uade.skillswap.domain.contratacion;

import ar.edu.uade.skillswap.domain.contratacion.exception.AccionNoPermitidaException;
import ar.edu.uade.skillswap.domain.contratacion.exception.AutoSolicitudException;
import ar.edu.uade.skillswap.domain.contratacion.exception.ReprogramacionInvalidaException;
import ar.edu.uade.skillswap.domain.contratacion.strategy.PoliticaReintegro;
import ar.edu.uade.skillswap.domain.contratacion.strategy.PoliticaReintegroFactory;
import ar.edu.uade.skillswap.domain.contratacion.strategy.ReintegroPorAnticipacion;
import ar.edu.uade.skillswap.domain.contratacion.strategy.ReintegroTotal;
import ar.edu.uade.skillswap.domain.publicacion.FranjaId;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionId;
import ar.edu.uade.skillswap.domain.shared.CambioEstado;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.shared.TransicionInvalidaException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ContratacionTest {

    private static final String CLIENTE = "user-mateo";
    private static final String PROVEEDOR = "user-ana";
    private static final String AJENO = "user-otro";
    private static final Instant AHORA = Instant.parse("2026-11-11T12:00:00Z");
    private static final Instant INICIO = Instant.parse("2026-11-20T10:00:00Z");
    private static final Instant OTRO_INICIO = Instant.parse("2026-11-21T10:00:00Z");
    private static final FranjaId FRANJA = FranjaId.generate();
    private static final FranjaId OTRA_FRANJA = FranjaId.generate();
    private static final SkillCoins PRECIO = new SkillCoins(100);
    private static final PoliticaReintegroFactory FACTORY =
            new PoliticaReintegroFactory(ReintegroPorAnticipacion.conUmbralesPorDefecto());
    private static final PoliticaReintegro POLITICA_CLIENTE = ReintegroPorAnticipacion.conUmbralesPorDefecto();

    private static Contratacion pendiente() {
        return Contratacion.solicitar(CLIENTE, PROVEEDOR, PublicacionId.generate(),
                new Solicitud("Quiero revisar mi código", "Mejorar mis tests", FRANJA), INICIO, PRECIO, AHORA);
    }

    private static Contratacion aceptada() {
        var contratacion = pendiente();
        contratacion.aceptar(PROVEEDOR, AHORA);
        return contratacion;
    }

    private static Contratacion confirmada() {
        var contratacion = aceptada();
        contratacion.pagar(CLIENTE, AHORA);
        return contratacion;
    }

    private static Contratacion enCurso() {
        var contratacion = confirmada();
        contratacion.iniciar(PROVEEDOR, INICIO);
        return contratacion;
    }

    private static Contratacion completada() {
        var contratacion = enCurso();
        contratacion.completar(PROVEEDOR, INICIO.plusSeconds(3600));
        return contratacion;
    }

    private static Contratacion rechazada() {
        var contratacion = pendiente();
        contratacion.rechazar(PROVEEDOR, "sin disponibilidad", AHORA);
        return contratacion;
    }

    private static Contratacion cancelada() {
        var contratacion = aceptada();
        contratacion.cancelar(CLIENTE, "cambié de planes", POLITICA_CLIENTE, AHORA);
        return contratacion;
    }

    /** Cada acción ejecutada por el participante que tiene el rol correcto. */
    private static Map<String, Consumer<Contratacion>> accionesConRolCorrecto() {
        var acciones = new LinkedHashMap<String, Consumer<Contratacion>>();
        acciones.put("aceptar", c -> c.aceptar(PROVEEDOR, AHORA));
        acciones.put("rechazar", c -> c.rechazar(PROVEEDOR, "no", AHORA));
        acciones.put("pagar", c -> c.pagar(CLIENTE, AHORA));
        acciones.put("iniciar", c -> c.iniciar(PROVEEDOR, AHORA));
        acciones.put("completar", c -> c.completar(PROVEEDOR, AHORA));
        acciones.put("cancelar", c -> c.cancelar(CLIENTE, "motivo", POLITICA_CLIENTE, AHORA));
        return acciones;
    }

    private static void assertSinCambios(Contratacion contratacion, String estado, List<CambioEstado> historial) {
        assertEquals(estado, contratacion.estado().nombre());
        assertEquals(historial, contratacion.historial());
    }

    @Nested
    class Solicitar {

        @Test
        void empiezaPendienteConLaFranjaElegidaYSinHistorial() {
            var contratacion = pendiente();

            assertEquals("PENDIENTE", contratacion.estado().nombre());
            assertEquals(FRANJA, contratacion.franjaId());
            assertEquals(FRANJA, contratacion.solicitud().franjaElegida());
            assertEquals(INICIO, contratacion.inicioSesion());
            assertEquals(PRECIO, contratacion.precioAcordado());
            assertEquals(CLIENTE, contratacion.clienteId());
            assertEquals(PROVEEDOR, contratacion.proveedorId());
            assertNotNull(contratacion.id());
            assertNotNull(contratacion.publicacionId());
            assertNull(contratacion.propuesta());
            assertNull(contratacion.reintegro());
            assertTrue(contratacion.historial().isEmpty());
        }

        @Test
        void elAutorNoPuedeSolicitarSuPropiaPublicacion() {
            assertThrows(AutoSolicitudException.class, () -> Contratacion.solicitar(PROVEEDOR, PROVEEDOR,
                    PublicacionId.generate(), new Solicitud("m", "o", FRANJA), INICIO, PRECIO, AHORA));
        }

        @Test
        void rechazaDatosObligatoriosFaltantes() {
            var publicacion = PublicacionId.generate();
            var solicitud = new Solicitud("m", "o", FRANJA);

            assertThrows(IllegalArgumentException.class,
                    () -> Contratacion.solicitar(" ", PROVEEDOR, publicacion, solicitud, INICIO, PRECIO, AHORA));
            assertThrows(IllegalArgumentException.class,
                    () -> Contratacion.solicitar(CLIENTE, null, publicacion, solicitud, INICIO, PRECIO, AHORA));
            assertThrows(NullPointerException.class,
                    () -> Contratacion.solicitar(CLIENTE, PROVEEDOR, null, solicitud, INICIO, PRECIO, AHORA));
            assertThrows(NullPointerException.class,
                    () -> Contratacion.solicitar(CLIENTE, PROVEEDOR, publicacion, null, INICIO, PRECIO, AHORA));
            assertThrows(NullPointerException.class,
                    () -> Contratacion.solicitar(CLIENTE, PROVEEDOR, publicacion, solicitud, null, PRECIO, AHORA));
            assertThrows(NullPointerException.class,
                    () -> Contratacion.solicitar(CLIENTE, PROVEEDOR, publicacion, solicitud, INICIO, null, AHORA));
            assertThrows(NullPointerException.class, () -> new Solicitud("m", "o", null));
        }

        @Test
        void esParticipanteSoloParaClienteYProveedor() {
            var contratacion = pendiente();

            assertTrue(contratacion.esParticipante(CLIENTE));
            assertTrue(contratacion.esParticipante(PROVEEDOR));
            assertFalse(contratacion.esParticipante(AJENO));
            assertFalse(contratacion.esParticipante(null));
        }
    }

    @Nested
    class FlujoFeliz {

        @Test
        void recorreTodosLosEstadosYRegistraCadaCambioConActorFechaYMotivo() {
            var contratacion = pendiente();
            var t1 = AHORA.plusSeconds(60);
            var t2 = AHORA.plusSeconds(120);
            var t3 = INICIO;
            var t4 = INICIO.plusSeconds(3600);

            contratacion.aceptar(PROVEEDOR, t1);
            contratacion.pagar(CLIENTE, t2);
            contratacion.iniciar(PROVEEDOR, t3);
            contratacion.completar(PROVEEDOR, t4);

            assertEquals("COMPLETADA", contratacion.estado().nombre());
            assertEquals(List.of(
                    new CambioEstado("PENDIENTE", "ACEPTADA", t1, PROVEEDOR, null),
                    new CambioEstado("ACEPTADA", "CONFIRMADA", t2, CLIENTE, null),
                    new CambioEstado("CONFIRMADA", "EN_CURSO", t3, PROVEEDOR, null),
                    new CambioEstado("EN_CURSO", "COMPLETADA", t4, PROVEEDOR, null)),
                    contratacion.historial());
        }

        @Test
        void rechazarGuardaElMotivoEnElHistorial() {
            var contratacion = rechazada();

            assertEquals("RECHAZADA", contratacion.estado().nombre());
            assertEquals(List.of(new CambioEstado("PENDIENTE", "RECHAZADA", AHORA, PROVEEDOR, "sin disponibilidad")),
                    contratacion.historial());
        }
    }

    @Nested
    class TransicionesInvalidas {

        @Test
        void cadaEstadoRechazaLasAccionesNoPermitidasSinDejarRastro() {
            Map<String, List<String>> permitidas = Map.of(
                    "PENDIENTE", List.of("aceptar", "rechazar", "cancelar"),
                    "ACEPTADA", List.of("pagar", "cancelar"),
                    "CONFIRMADA", List.of("iniciar", "cancelar"),
                    "EN_CURSO", List.of("completar"));
            List<Supplier<Contratacion>> fabricas = List.of(ContratacionTest::pendiente, ContratacionTest::aceptada,
                    ContratacionTest::confirmada, ContratacionTest::enCurso, ContratacionTest::completada,
                    ContratacionTest::rechazada, ContratacionTest::cancelada);

            int rechazadas = 0;
            for (var fabrica : fabricas) {
                for (var accion : accionesConRolCorrecto().entrySet()) {
                    var contratacion = fabrica.get();
                    var estado = contratacion.estado().nombre();
                    if (permitidas.getOrDefault(estado, List.of()).contains(accion.getKey())) {
                        continue;
                    }
                    var historial = contratacion.historial();

                    assertThrows(TransicionInvalidaException.class, () -> accion.getValue().accept(contratacion),
                            estado + " " + accion.getKey());
                    assertSinCambios(contratacion, estado, historial);
                    rechazadas++;
                }
            }
            assertEquals(7 * 6 - 8, rechazadas);
        }

        @Test
        void unSegundoPagoSeRechazaYNoDejaRastro() {
            var contratacion = confirmada();
            var historial = contratacion.historial();

            assertThrows(TransicionInvalidaException.class, () -> contratacion.pagar(CLIENTE, AHORA));

            assertSinCambios(contratacion, "CONFIRMADA", historial);
        }

        @Test
        void noSePuedePagarAntesDeQueElProveedorAcepte() {
            var contratacion = pendiente();

            assertThrows(TransicionInvalidaException.class, () -> contratacion.pagar(CLIENTE, AHORA));

            assertSinCambios(contratacion, "PENDIENTE", List.of());
        }
    }

    @Nested
    class Roles {

        @Test
        void soloElProveedorAceptaYRechaza() {
            for (var actor : new String[]{CLIENTE, AJENO, null}) {
                var contratacion = pendiente();

                assertThrows(AccionNoPermitidaException.class, () -> contratacion.aceptar(actor, AHORA));
                assertThrows(AccionNoPermitidaException.class, () -> contratacion.rechazar(actor, "no", AHORA));

                assertSinCambios(contratacion, "PENDIENTE", List.of());
            }
        }

        @Test
        void soloElClientePaga() {
            for (var actor : new String[]{PROVEEDOR, AJENO, null}) {
                var contratacion = aceptada();
                var historial = contratacion.historial();

                assertThrows(AccionNoPermitidaException.class, () -> contratacion.pagar(actor, AHORA));

                assertSinCambios(contratacion, "ACEPTADA", historial);
            }
        }

        @Test
        void soloElProveedorInicia() {
            for (var actor : new String[]{CLIENTE, AJENO}) {
                var contratacion = confirmada();
                var historial = contratacion.historial();

                assertThrows(AccionNoPermitidaException.class, () -> contratacion.iniciar(actor, INICIO));

                assertSinCambios(contratacion, "CONFIRMADA", historial);
            }
        }

        @Test
        void soloElProveedorCompleta() {
            for (var actor : new String[]{CLIENTE, AJENO}) {
                var contratacion = enCurso();
                var historial = contratacion.historial();

                assertThrows(AccionNoPermitidaException.class, () -> contratacion.completar(actor, INICIO));

                assertSinCambios(contratacion, "EN_CURSO", historial);
            }
        }

        @Test
        void unAjenoNoPuedeCancelarEnNingunEstadoCancelable() {
            for (var contratacion : List.of(pendiente(), aceptada(), confirmada())) {
                var estado = contratacion.estado().nombre();
                var historial = contratacion.historial();

                assertThrows(AccionNoPermitidaException.class,
                        () -> contratacion.cancelar(AJENO, "motivo", new ReintegroTotal(), AHORA), estado);

                assertSinCambios(contratacion, estado, historial);
                assertNull(contratacion.reintegro());
            }
        }

        @Test
        void enPendienteElProveedorNoCancelaSinoQueRechaza() {
            var contratacion = pendiente();

            assertThrows(AccionNoPermitidaException.class,
                    () -> contratacion.cancelar(PROVEEDOR, "motivo", new ReintegroTotal(), AHORA));

            assertSinCambios(contratacion, "PENDIENTE", List.of());
        }
    }

    @Nested
    class Cancelar {

        @Test
        void elClienteRetiraLaSolicitudPendienteSinReintegro() {
            var contratacion = pendiente();

            var reintegro = contratacion.cancelar(CLIENTE, "ya no la necesito", POLITICA_CLIENTE, AHORA);

            assertEquals(Reintegro.cero(), reintegro);
            assertEquals(reintegro, contratacion.reintegro());
            assertEquals("CANCELADA", contratacion.estado().nombre());
            assertEquals(List.of(new CambioEstado("PENDIENTE", "CANCELADA", AHORA, CLIENTE, "ya no la necesito")),
                    contratacion.historial());
        }

        @Test
        void enAceptadaCualquieraDeLasPartesCancelaSinReintegroPorqueNoSePago() {
            for (var actor : new String[]{CLIENTE, PROVEEDOR}) {
                var contratacion = aceptada();
                var politica = FACTORY.paraCancelacion(contratacion, actor);

                var reintegro = contratacion.cancelar(actor, "imprevisto", politica, INICIO.minusSeconds(60));

                assertEquals(Reintegro.cero(), reintegro, actor);
                assertEquals("CANCELADA", contratacion.estado().nombre());
                assertEquals(new CambioEstado("ACEPTADA", "CANCELADA", INICIO.minusSeconds(60), actor, "imprevisto"),
                        contratacion.historial().get(1));
            }
        }

        @Test
        void confirmadaCanceladaPorElClienteConMasDe48HorasDevuelveTodo() {
            assertReintegroAlCancelar(CLIENTE, INICIO.minus(Duration.ofHours(48)), 100, 0);
        }

        @Test
        void confirmadaCanceladaPorElClienteEntre24Y48HorasDevuelveLaMitad() {
            assertReintegroAlCancelar(CLIENTE, INICIO.minus(Duration.ofHours(30)), 50, 50);
        }

        @Test
        void confirmadaCanceladaPorElClienteConMenosDe24HorasNoDevuelveNada() {
            assertReintegroAlCancelar(CLIENTE, INICIO.minus(Duration.ofHours(2)), 0, 100);
        }

        @Test
        void confirmadaCanceladaPorElProveedorDevuelveTodoAunSinAnticipacion() {
            assertReintegroAlCancelar(PROVEEDOR, INICIO.minus(Duration.ofHours(2)), 100, 0);
        }

        private void assertReintegroAlCancelar(String actor, Instant ahora, long alCliente, long alProveedor) {
            var contratacion = confirmada();
            var politica = FACTORY.paraCancelacion(contratacion, actor);

            var reintegro = contratacion.cancelar(actor, "motivo", politica, ahora);

            var esperado = new Reintegro(new SkillCoins(alCliente), new SkillCoins(alProveedor));
            assertEquals(esperado, reintegro);
            assertEquals(esperado, contratacion.reintegro());
            assertEquals("CANCELADA", contratacion.estado().nombre());
            assertEquals(new CambioEstado("CONFIRMADA", "CANCELADA", ahora, actor, "motivo"),
                    contratacion.historial().get(2));
        }

        @Test
        void cancelarSinMotivoFallaYNoCambiaNada() {
            var contratacion = confirmada();
            var historial = contratacion.historial();

            assertThrows(IllegalArgumentException.class,
                    () -> contratacion.cancelar(CLIENTE, null, POLITICA_CLIENTE, AHORA));
            assertThrows(IllegalArgumentException.class,
                    () -> contratacion.cancelar(CLIENTE, "  ", POLITICA_CLIENTE, AHORA));

            assertSinCambios(contratacion, "CONFIRMADA", historial);
            assertNull(contratacion.reintegro());
        }

        @Test
        void cancelarSinPoliticaFallaYNoCambiaNada() {
            var contratacion = confirmada();
            var historial = contratacion.historial();

            assertThrows(NullPointerException.class, () -> contratacion.cancelar(CLIENTE, "motivo", null, AHORA));

            assertSinCambios(contratacion, "CONFIRMADA", historial);
        }

        @Test
        void unaContratacionCanceladaNoSePuedeVolverACancelar() {
            var contratacion = cancelada();
            var historial = contratacion.historial();
            var reintegro = contratacion.reintegro();

            assertThrows(TransicionInvalidaException.class,
                    () -> contratacion.cancelar(PROVEEDOR, "otra vez", new ReintegroTotal(), AHORA));

            assertSinCambios(contratacion, "CANCELADA", historial);
            assertSame(reintegro, contratacion.reintegro());
        }
    }

    @Nested
    class Reprogramacion {

        @Test
        void unaParteProponeYLaOtraAceptaAplicandoFranjaEInicio() {
            var contratacion = confirmada();
            var historial = contratacion.historial();

            contratacion.proponerReprogramacion(CLIENTE, OTRA_FRANJA, OTRO_INICIO, AHORA);
            assertEquals(new PropuestaReprogramacion(CLIENTE, OTRA_FRANJA, OTRO_INICIO), contratacion.propuesta());

            var anterior = contratacion.aceptarReprogramacion(PROVEEDOR, AHORA);

            assertEquals(FRANJA, anterior);
            assertEquals(OTRA_FRANJA, contratacion.franjaId());
            assertEquals(OTRO_INICIO, contratacion.inicioSesion());
            assertEquals(FRANJA, contratacion.solicitud().franjaElegida());
            assertNull(contratacion.propuesta());
            assertSinCambios(contratacion, "CONFIRMADA", historial);
        }

        @Test
        void tambienSePuedeProponerEnAceptadaYLaPuedeProponerElProveedor() {
            var contratacion = aceptada();

            contratacion.proponerReprogramacion(PROVEEDOR, OTRA_FRANJA, OTRO_INICIO, AHORA);

            assertEquals(FRANJA, contratacion.aceptarReprogramacion(CLIENTE, AHORA));
            assertEquals(OTRA_FRANJA, contratacion.franjaId());
            assertEquals("ACEPTADA", contratacion.estado().nombre());
        }

        @Test
        void quienProponeNoPuedeAceptarNiRechazarSuPropiaPropuesta() {
            var contratacion = confirmada();
            contratacion.proponerReprogramacion(CLIENTE, OTRA_FRANJA, OTRO_INICIO, AHORA);
            var propuesta = contratacion.propuesta();

            assertThrows(AccionNoPermitidaException.class, () -> contratacion.aceptarReprogramacion(CLIENTE, AHORA));
            assertThrows(AccionNoPermitidaException.class, () -> contratacion.rechazarReprogramacion(CLIENTE, AHORA));

            assertEquals(propuesta, contratacion.propuesta());
            assertEquals(FRANJA, contratacion.franjaId());
        }

        @Test
        void unAjenoNoPuedeProponerAceptarNiRechazar() {
            var contratacion = confirmada();

            assertThrows(AccionNoPermitidaException.class,
                    () -> contratacion.proponerReprogramacion(AJENO, OTRA_FRANJA, OTRO_INICIO, AHORA));
            assertNull(contratacion.propuesta());

            contratacion.proponerReprogramacion(CLIENTE, OTRA_FRANJA, OTRO_INICIO, AHORA);
            assertThrows(AccionNoPermitidaException.class, () -> contratacion.aceptarReprogramacion(AJENO, AHORA));
            assertThrows(AccionNoPermitidaException.class, () -> contratacion.rechazarReprogramacion(AJENO, AHORA));
            assertNotNull(contratacion.propuesta());
        }

        @Test
        void rechazarLaPropuestaLaDescartaSinCambiarLaFranja() {
            var contratacion = confirmada();
            contratacion.proponerReprogramacion(CLIENTE, OTRA_FRANJA, OTRO_INICIO, AHORA);

            contratacion.rechazarReprogramacion(PROVEEDOR, AHORA);

            assertNull(contratacion.propuesta());
            assertEquals(FRANJA, contratacion.franjaId());
            assertEquals(INICIO, contratacion.inicioSesion());
        }

        @Test
        void soloPuedeHaberUnaPropuestaPendiente() {
            var contratacion = confirmada();
            contratacion.proponerReprogramacion(CLIENTE, OTRA_FRANJA, OTRO_INICIO, AHORA);

            assertThrows(ReprogramacionInvalidaException.class,
                    () -> contratacion.proponerReprogramacion(PROVEEDOR, FranjaId.generate(), OTRO_INICIO, AHORA));

            assertEquals(new PropuestaReprogramacion(CLIENTE, OTRA_FRANJA, OTRO_INICIO), contratacion.propuesta());
        }

        @Test
        void trasResolverUnaPropuestaSePuedeProponerOtra() {
            var contratacion = confirmada();
            contratacion.proponerReprogramacion(CLIENTE, OTRA_FRANJA, OTRO_INICIO, AHORA);
            contratacion.rechazarReprogramacion(PROVEEDOR, AHORA);

            assertDoesNotThrow(() -> contratacion.proponerReprogramacion(PROVEEDOR, OTRA_FRANJA, OTRO_INICIO, AHORA));
        }

        @Test
        void laFranjaPropuestaTieneQueSerDistintaDeLaActual() {
            var contratacion = confirmada();

            assertThrows(ReprogramacionInvalidaException.class,
                    () -> contratacion.proponerReprogramacion(CLIENTE, FRANJA, OTRO_INICIO, AHORA));

            assertNull(contratacion.propuesta());
        }

        @Test
        void soloAntesDelInicioDeLaSesion() {
            var contratacion = confirmada();

            assertThrows(ReprogramacionInvalidaException.class,
                    () -> contratacion.proponerReprogramacion(CLIENTE, OTRA_FRANJA, OTRO_INICIO, INICIO));
            assertThrows(ReprogramacionInvalidaException.class,
                    () -> contratacion.proponerReprogramacion(CLIENTE, OTRA_FRANJA, OTRO_INICIO, INICIO.plusSeconds(1)));
            assertNull(contratacion.propuesta());
            assertDoesNotThrow(
                    () -> contratacion.proponerReprogramacion(CLIENTE, OTRA_FRANJA, OTRO_INICIO, INICIO.minusSeconds(1)));
        }

        @Test
        void aceptarDespuesDelInicioFallaYConservaLaPropuesta() {
            var contratacion = confirmada();
            contratacion.proponerReprogramacion(CLIENTE, OTRA_FRANJA, OTRO_INICIO, AHORA);

            assertThrows(ReprogramacionInvalidaException.class,
                    () -> contratacion.aceptarReprogramacion(PROVEEDOR, INICIO));

            assertEquals(FRANJA, contratacion.franjaId());
            assertEquals(INICIO, contratacion.inicioSesion());
            assertNotNull(contratacion.propuesta());
        }

        @Test
        void aceptarORechazarSinPropuestaFalla() {
            var contratacion = confirmada();

            assertThrows(ReprogramacionInvalidaException.class, () -> contratacion.aceptarReprogramacion(PROVEEDOR, AHORA));
            assertThrows(ReprogramacionInvalidaException.class, () -> contratacion.rechazarReprogramacion(PROVEEDOR, AHORA));
        }

        @Test
        void fueraDeAceptadaYConfirmadaNoSePuedeProponer() {
            for (var contratacion : List.of(pendiente(), enCurso(), completada(), rechazada(), cancelada())) {
                var estado = contratacion.estado().nombre();

                assertThrows(TransicionInvalidaException.class,
                        () -> contratacion.proponerReprogramacion(CLIENTE, OTRA_FRANJA, OTRO_INICIO, AHORA), estado);
                assertNull(contratacion.propuesta(), estado);
            }
        }

        @Test
        void unaPropuestaPendienteNoSePuedeAceptarSiLaContratacionYaSeCancelo() {
            var contratacion = confirmada();
            contratacion.proponerReprogramacion(CLIENTE, OTRA_FRANJA, OTRO_INICIO, AHORA);
            contratacion.cancelar(CLIENTE, "imprevisto", POLITICA_CLIENTE, AHORA);

            assertThrows(TransicionInvalidaException.class, () -> contratacion.aceptarReprogramacion(PROVEEDOR, AHORA));

            assertEquals(FRANJA, contratacion.franjaId());
        }

        @Test
        void laReprogramacionNoEsUnCambioDeEstadoYNoSeRegistraEnElHistorial() {
            var contratacion = aceptada();
            var historial = contratacion.historial();

            contratacion.proponerReprogramacion(CLIENTE, OTRA_FRANJA, OTRO_INICIO, AHORA);
            contratacion.aceptarReprogramacion(PROVEEDOR, AHORA);

            assertSinCambios(contratacion, "ACEPTADA", historial);
        }
    }

    @Nested
    class Historial {

        @Test
        void elHistorialDevueltoNoSePuedeModificar() {
            var contratacion = confirmada();
            var historial = contratacion.historial();

            assertThrows(UnsupportedOperationException.class,
                    () -> historial.add(new CambioEstado("X", "Y", AHORA, CLIENTE, null)));
            assertThrows(UnsupportedOperationException.class, historial::clear);
            assertEquals(2, contratacion.historial().size());
        }

        @Test
        void unaTransicionSinFechaFallaYNoDejaRegistro() {
            var contratacion = pendiente();

            assertThrows(NullPointerException.class, () -> contratacion.aceptar(PROVEEDOR, null));

            assertSinCambios(contratacion, "PENDIENTE", List.of());
        }
    }
}
