package ar.edu.uade.skillswap.domain.publicacion;

import ar.edu.uade.skillswap.domain.publicacion.exception.CupoAgotadoException;
import ar.edu.uade.skillswap.domain.publicacion.exception.FranjaInvalidaException;
import ar.edu.uade.skillswap.domain.publicacion.exception.PublicacionIncompletaException;
import ar.edu.uade.skillswap.domain.shared.CambioEstado;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.shared.TransicionInvalidaException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class PublicacionTest {

    private static final String AUTOR = "user-ana";
    private static final Instant AHORA = Instant.parse("2026-11-11T12:00:00Z");
    private static final Instant LUNES_10 = Instant.parse("2026-11-16T10:00:00Z");
    private static final Instant LUNES_11 = Instant.parse("2026-11-16T11:00:00Z");
    private static final Instant LUNES_12 = Instant.parse("2026-11-16T12:00:00Z");

    private static Publicacion borradorCompleto(int cupo) {
        var publicacion = Publicacion.crearBorrador(AUTOR, TipoPublicacion.OFERTA);
        publicacion.editar("Mentoría de Java", "Revisión de código", "Programación", Modalidad.REMOTA,
                Duration.ofHours(1), cupo, new SkillCoins(100), Set.of("java"));
        publicacion.agregarFranja(LUNES_10, LUNES_11);
        return publicacion;
    }

    private static Publicacion publicada() {
        var publicacion = borradorCompleto(1);
        publicacion.publicar(AUTOR, AHORA);
        return publicacion;
    }

    private static Publicacion pausada() {
        var publicacion = publicada();
        publicacion.pausar(AUTOR, "vacaciones", AHORA);
        return publicacion;
    }

    private static Publicacion cerrada() {
        var publicacion = publicada();
        publicacion.cerrar(AUTOR, null, AHORA);
        return publicacion;
    }

    private static Publicacion cancelada() {
        var publicacion = publicada();
        publicacion.cancelar(AUTOR, "ya no la dicto", AHORA);
        return publicacion;
    }

    @Nested
    class CreacionDelBorrador {

        @Test
        void soloExigeAutorYTipo() {
            var publicacion = Publicacion.crearBorrador(AUTOR, TipoPublicacion.DESAFIO);

            assertEquals(AUTOR, publicacion.autorId());
            assertEquals(TipoPublicacion.DESAFIO, publicacion.tipo());
            assertEquals("BORRADOR", publicacion.estado().nombre());
            assertNotNull(publicacion.id());
            assertNull(publicacion.titulo());
            assertTrue(publicacion.franjas().isEmpty());
            assertTrue(publicacion.historial().isEmpty());
        }

        @Test
        void rechazaAutorFaltante() {
            assertThrows(IllegalArgumentException.class, () -> Publicacion.crearBorrador(null, TipoPublicacion.OFERTA));
            assertThrows(IllegalArgumentException.class, () -> Publicacion.crearBorrador(" ", TipoPublicacion.OFERTA));
        }

        @Test
        void rechazaTipoFaltante() {
            assertThrows(NullPointerException.class, () -> Publicacion.crearBorrador(AUTOR, null));
        }

        @Test
        void aceptaModalidadHibrida() {
            var publicacion = Publicacion.crearBorrador(AUTOR, TipoPublicacion.OFERTA);
            publicacion.editar("t", null, "c", Modalidad.HIBRIDA, null, 1, new SkillCoins(0), null);

            assertEquals(Modalidad.HIBRIDA, publicacion.modalidad());
            assertEquals(Set.of(), publicacion.etiquetas());
        }
    }

    @Nested
    class EdicionYFranjas {

        @Test
        void editarYAgregarFranjaFallanFueraDeBorrador() {
            for (var publicacion : List.of(publicada(), pausada(), cerrada(), cancelada())) {
                assertThrows(TransicionInvalidaException.class, () -> publicacion.editar(
                        "otro", null, "c", Modalidad.REMOTA, null, 1, new SkillCoins(1), null));
                assertThrows(TransicionInvalidaException.class, () -> publicacion.agregarFranja(LUNES_11, LUNES_12));
                assertEquals("Mentoría de Java", publicacion.titulo());
                assertEquals(1, publicacion.franjas().size());
            }
        }

        @Test
        void franjaRechazaInicioIgualOPosteriorAlFin() {
            var publicacion = Publicacion.crearBorrador(AUTOR, TipoPublicacion.OFERTA);

            assertThrows(FranjaInvalidaException.class, () -> publicacion.agregarFranja(LUNES_11, LUNES_10));
            assertThrows(FranjaInvalidaException.class, () -> publicacion.agregarFranja(LUNES_10, LUNES_10));
            assertThrows(FranjaInvalidaException.class, () -> publicacion.agregarFranja(null, LUNES_10));
            assertTrue(publicacion.franjas().isEmpty());
        }

        @Test
        void franjaRechazaSolapamientoConOtraDeLaMismaPublicacion() {
            var publicacion = Publicacion.crearBorrador(AUTOR, TipoPublicacion.OFERTA);
            publicacion.agregarFranja(LUNES_10, LUNES_12);

            assertThrows(FranjaInvalidaException.class,
                    () -> publicacion.agregarFranja(LUNES_11, LUNES_12.plusSeconds(1800)));
            assertThrows(FranjaInvalidaException.class,
                    () -> publicacion.agregarFranja(LUNES_10.plusSeconds(600), LUNES_11));
            assertEquals(1, publicacion.franjas().size());
        }

        @Test
        void franjasContiguasNoSeSolapan() {
            var publicacion = Publicacion.crearBorrador(AUTOR, TipoPublicacion.OFERTA);
            publicacion.agregarFranja(LUNES_10, LUNES_11);
            publicacion.agregarFranja(LUNES_11, LUNES_12);

            assertEquals(2, publicacion.franjas().size());
        }

        @Test
        void franjasEnOtraPublicacionNoCuentanParaElSolapamiento() {
            Publicacion.crearBorrador(AUTOR, TipoPublicacion.OFERTA).agregarFranja(LUNES_10, LUNES_11);
            var otra = Publicacion.crearBorrador(AUTOR, TipoPublicacion.OFERTA);

            assertDoesNotThrow(() -> otra.agregarFranja(LUNES_10, LUNES_11));
        }
    }

    @Nested
    class Publicar {

        @Test
        void unaOfertaCompletaPasaAPublicadaYRegistraElCambio() {
            var publicacion = borradorCompleto(1);

            publicacion.publicar(AUTOR, AHORA);

            assertEquals("PUBLICADA", publicacion.estado().nombre());
            assertEquals(List.of(new CambioEstado("BORRADOR", "PUBLICADA", AHORA, AUTOR, null)),
                    publicacion.historial());
        }

        @Test
        void unBorradorVacioListaTodosLosFaltantes() {
            var publicacion = Publicacion.crearBorrador(AUTOR, TipoPublicacion.OFERTA);

            var ex = assertThrows(PublicacionIncompletaException.class, () -> publicacion.publicar(AUTOR, AHORA));

            assertEquals(List.of("título", "categoría", "precio", "cupo de al menos 1",
                    "al menos una franja de disponibilidad"), ex.faltantes());
            assertEquals("BORRADOR", publicacion.estado().nombre());
            assertTrue(publicacion.historial().isEmpty());
        }

        @Test
        void sinFranjasNiTituloFallaYListaAmbos() {
            var publicacion = Publicacion.crearBorrador(AUTOR, TipoPublicacion.OFERTA);
            publicacion.editar("  ", null, "Programación", Modalidad.REMOTA, null, 2, new SkillCoins(10), null);

            var ex = assertThrows(PublicacionIncompletaException.class, () -> publicacion.publicar(AUTOR, AHORA));

            assertEquals(List.of("título", "al menos una franja de disponibilidad"), ex.faltantes());
        }

        @Test
        void sinCategoriaFalla() {
            var publicacion = borradorCompleto(1);
            publicacion.editar("Mentoría", null, null, Modalidad.REMOTA, null, 1, new SkillCoins(10), null);

            var ex = assertThrows(PublicacionIncompletaException.class, () -> publicacion.publicar(AUTOR, AHORA));

            assertEquals(List.of("categoría"), ex.faltantes());
        }

        @Test
        void conCupoCeroFalla() {
            var publicacion = borradorCompleto(0);

            var ex = assertThrows(PublicacionIncompletaException.class, () -> publicacion.publicar(AUTOR, AHORA));

            assertEquals(List.of("cupo de al menos 1"), ex.faltantes());
        }

        @Test
        void unPrecioNegativoNoSePuedeNiExpresar() {
            // El precio negativo se rechaza al construir SkillCoins, antes de llegar a publicar.
            assertThrows(IllegalArgumentException.class, () -> new SkillCoins(-50));
        }

        @Test
        void precioCeroEsValido() {
            var publicacion = borradorCompleto(1);
            publicacion.editar("Mentoría", null, "Programación", null, null, 1, new SkillCoins(0), null);

            assertDoesNotThrow(() -> publicacion.publicar(AUTOR, AHORA));
        }

        @Test
        void fueraDeBorradorLanzaTransicionInvalidaAntesQueValidarRequisitos() {
            var publicacion = publicada();

            assertThrows(TransicionInvalidaException.class, () -> publicacion.publicar(AUTOR, AHORA));
        }
    }

    @Nested
    class Transiciones {

        @Test
        void pausadaVuelveAPublicadaConReanudar() {
            var publicacion = pausada();
            var despues = AHORA.plusSeconds(3600);

            publicacion.reanudar("user-ana", despues);

            assertEquals("PUBLICADA", publicacion.estado().nombre());
            assertEquals(new CambioEstado("PAUSADA", "PUBLICADA", despues, AUTOR, null),
                    publicacion.historial().get(2));
        }

        @Test
        void elHistorialGuardaCadaTransicionEnOrdenConActorFechaYMotivo() {
            var publicacion = borradorCompleto(1);
            var t1 = AHORA;
            var t2 = AHORA.plusSeconds(60);
            var t3 = AHORA.plusSeconds(120);
            var t4 = AHORA.plusSeconds(180);

            publicacion.publicar(AUTOR, t1);
            publicacion.pausar("user-admin", "revisión", t2);
            publicacion.reanudar(AUTOR, t3);
            publicacion.cerrar(AUTOR, "temporada terminada", t4);

            assertEquals(List.of(
                    new CambioEstado("BORRADOR", "PUBLICADA", t1, AUTOR, null),
                    new CambioEstado("PUBLICADA", "PAUSADA", t2, "user-admin", "revisión"),
                    new CambioEstado("PAUSADA", "PUBLICADA", t3, AUTOR, null),
                    new CambioEstado("PUBLICADA", "CERRADA", t4, AUTOR, "temporada terminada")),
                    publicacion.historial());
        }

        @Test
        void cerradaNoPuedeVolverAPublicarseNiAReanudarse() {
            var publicacion = cerrada();
            var historialAntes = publicacion.historial();

            assertThrows(TransicionInvalidaException.class, () -> publicacion.publicar(AUTOR, AHORA));
            assertThrows(TransicionInvalidaException.class, () -> publicacion.reanudar(AUTOR, AHORA));
            assertThrows(TransicionInvalidaException.class, () -> publicacion.pausar(AUTOR, null, AHORA));
            assertThrows(TransicionInvalidaException.class, () -> publicacion.cerrar(AUTOR, null, AHORA));
            assertThrows(TransicionInvalidaException.class, () -> publicacion.cancelar(AUTOR, "motivo", AHORA));

            assertEquals("CERRADA", publicacion.estado().nombre());
            assertEquals(historialAntes, publicacion.historial());
        }

        @Test
        void canceladaNoAdmiteNingunaTransicion() {
            var publicacion = cancelada();
            var historialAntes = publicacion.historial();

            List<Consumer<Publicacion>> acciones = List.of(
                    p -> p.publicar(AUTOR, AHORA),
                    p -> p.pausar(AUTOR, null, AHORA),
                    p -> p.reanudar(AUTOR, AHORA),
                    p -> p.cerrar(AUTOR, null, AHORA),
                    p -> p.cancelar(AUTOR, "otra vez", AHORA));
            for (var accion : acciones) {
                assertThrows(TransicionInvalidaException.class, () -> accion.accept(publicacion));
            }

            assertEquals("CANCELADA", publicacion.estado().nombre());
            assertEquals(historialAntes, publicacion.historial());
        }

        @Test
        void unaTransicionRechazadaEnBorradorNoDejaRegistro() {
            var publicacion = borradorCompleto(1);

            assertThrows(TransicionInvalidaException.class, () -> publicacion.pausar(AUTOR, null, AHORA));
            assertThrows(TransicionInvalidaException.class, () -> publicacion.reanudar(AUTOR, AHORA));
            assertThrows(TransicionInvalidaException.class, () -> publicacion.cerrar(AUTOR, null, AHORA));

            assertTrue(publicacion.historial().isEmpty());
            assertEquals("BORRADOR", publicacion.estado().nombre());
        }

        @Test
        void unaTransicionSinActorFallaYNoDejaRegistro() {
            var publicacion = publicada();

            assertThrows(IllegalArgumentException.class, () -> publicacion.pausar(" ", null, AHORA));

            assertEquals("PUBLICADA", publicacion.estado().nombre());
            assertEquals(1, publicacion.historial().size());
        }
    }

    @Nested
    class Cancelar {

        @Test
        void cancelarSinMotivoFallaYNoCambiaNada() {
            var publicacion = publicada();

            assertThrows(IllegalArgumentException.class, () -> publicacion.cancelar(AUTOR, null, AHORA));
            assertThrows(IllegalArgumentException.class, () -> publicacion.cancelar(AUTOR, "   ", AHORA));

            assertEquals("PUBLICADA", publicacion.estado().nombre());
            assertEquals(1, publicacion.historial().size());
            assertNull(publicacion.motivoCancelacion());
        }

        @Test
        void cancelarConMotivoLoGuardaEnLaPublicacionYEnElHistorial() {
            var publicacion = Publicacion.crearBorrador(AUTOR, TipoPublicacion.DESAFIO);

            publicacion.cancelar(AUTOR, "desafío duplicado", AHORA);

            assertEquals("CANCELADA", publicacion.estado().nombre());
            assertEquals("desafío duplicado", publicacion.motivoCancelacion());
            assertEquals(new CambioEstado("BORRADOR", "CANCELADA", AHORA, AUTOR, "desafío duplicado"),
                    publicacion.historial().get(0));
        }

        @Test
        void pausadaSePuedeCancelar() {
            var publicacion = pausada();

            publicacion.cancelar(AUTOR, "no vuelvo a dictarla", AHORA);

            assertEquals("CANCELADA", publicacion.estado().nombre());
        }
    }

    @Nested
    class Historial {

        @Test
        void elHistorialDevueltoNoSePuedeModificar() {
            var publicacion = publicada();
            var historial = publicacion.historial();

            assertThrows(UnsupportedOperationException.class,
                    () -> historial.add(new CambioEstado("X", "Y", AHORA, AUTOR, null)));
            assertThrows(UnsupportedOperationException.class, historial::clear);
            assertEquals(1, publicacion.historial().size());
        }

        @Test
        void lasFranjasDevueltasNoSePuedenModificar() {
            var publicacion = borradorCompleto(1);

            assertThrows(UnsupportedOperationException.class, () -> publicacion.franjas().clear());
            assertEquals(1, publicacion.franjas().size());
        }
    }

    @Nested
    class AdmiteSolicitudes {

        @Test
        void soloLaPublicadaAdmiteSolicitudes() {
            assertFalse(borradorCompleto(1).admiteSolicitudes());
            assertTrue(publicada().admiteSolicitudes());
            assertFalse(pausada().admiteSolicitudes());
            assertFalse(cerrada().admiteSolicitudes());
            assertFalse(cancelada().admiteSolicitudes());
        }
    }

    @Nested
    class CupoPorFranja {

        @Test
        void franjaLlenaFallaYOtraFranjaDeLaMismaPublicacionSigueAceptando() {
            var publicacion = Publicacion.crearBorrador(AUTOR, TipoPublicacion.OFERTA);
            publicacion.editar("Mentoría", null, "Programación", Modalidad.REMOTA, null, 1, new SkillCoins(10), null);
            var primera = publicacion.agregarFranja(LUNES_10, LUNES_11);
            var segunda = publicacion.agregarFranja(LUNES_11, LUNES_12);
            publicacion.publicar(AUTOR, AHORA);

            publicacion.reservarCupo(primera);

            assertThrows(CupoAgotadoException.class, () -> publicacion.reservarCupo(primera));
            assertDoesNotThrow(() -> publicacion.reservarCupo(segunda));
            assertEquals(1, reservados(publicacion, primera));
            assertEquals(1, reservados(publicacion, segunda));
        }

        @Test
        void cadaFranjaAdmiteHastaElCupo() {
            var publicacion = borradorCompleto(3);
            publicacion.publicar(AUTOR, AHORA);
            var franja = publicacion.franjas().get(0).id();

            publicacion.reservarCupo(franja);
            publicacion.reservarCupo(franja);
            publicacion.reservarCupo(franja);

            assertThrows(CupoAgotadoException.class, () -> publicacion.reservarCupo(franja));
            assertEquals(3, reservados(publicacion, franja));
            assertTrue(publicacion.franjas().get(0).estaOcupada(publicacion.cupo()));
        }

        @Test
        void reservarFallaSiLaPublicacionNoEstaPublicada() {
            for (var publicacion : List.of(borradorCompleto(1), pausada(), cerrada(), cancelada())) {
                var franja = publicacion.franjas().get(0).id();

                assertThrows(TransicionInvalidaException.class, () -> publicacion.reservarCupo(franja),
                        publicacion.estado().nombre());
                assertEquals(0, reservados(publicacion, franja));
            }
        }

        @Test
        void reservarEnUnaFranjaAjenaFalla() {
            var publicacion = publicada();

            assertThrows(FranjaInvalidaException.class, () -> publicacion.reservarCupo(FranjaId.generate()));
        }

        @Test
        void liberarDevuelveElLugarYNuncaBajaDeCero() {
            var publicacion = publicada();
            var franja = publicacion.franjas().get(0).id();
            publicacion.reservarCupo(franja);

            publicacion.liberarCupo(franja);
            publicacion.liberarCupo(franja);

            assertEquals(0, reservados(publicacion, franja));
            assertDoesNotThrow(() -> publicacion.reservarCupo(franja));
        }

        private int reservados(Publicacion publicacion, FranjaId franjaId) {
            return publicacion.franjas().stream()
                    .filter(f -> f.id().equals(franjaId))
                    .findFirst()
                    .orElseThrow()
                    .reservados();
        }
    }

    @Test
    void elDominioNoImportaJpaNiSwing() throws IOException {
        var domainDir = Path.of("src/main/java/ar/edu/uade/skillswap/domain");
        try (var files = Files.walk(domainDir)) {
            var sourceFiles = files.filter(file -> file.toString().endsWith(".java")).toList();
            assertFalse(sourceFiles.isEmpty(), domainDir.toString());
            for (var file : sourceFiles) {
                var source = Files.readString(file);
                assertFalse(source.contains("jakarta.persistence"), file.toString());
                assertFalse(source.contains("javax.persistence"), file.toString());
                assertFalse(source.contains("javax.swing"), file.toString());
            }
        }
    }
}
