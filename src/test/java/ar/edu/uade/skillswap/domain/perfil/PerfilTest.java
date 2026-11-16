package ar.edu.uade.skillswap.domain.perfil;

import ar.edu.uade.skillswap.domain.perfil.exception.DisponibilidadInvalidaException;
import ar.edu.uade.skillswap.domain.perfil.exception.PerfilInactivoException;
import ar.edu.uade.skillswap.domain.shared.CambioEstado;
import ar.edu.uade.skillswap.domain.shared.Puntaje;
import ar.edu.uade.skillswap.domain.shared.TransicionInvalidaException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PerfilTest {

    private static final String USUARIO = "user-ana";
    private static final String ADMIN = "user-admin";
    private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final Instant AHORA = Instant.parse("2026-11-14T12:00:00Z");
    private static final LocalTime NUEVE = LocalTime.of(9, 0);
    private static final LocalTime DIEZ = LocalTime.of(10, 0);
    private static final LocalTime ONCE = LocalTime.of(11, 0);

    private static Perfil nuevo() {
        return Perfil.crear(USUARIO, TipoPerfil.PROFESIONAL, "Ana Pérez", BUENOS_AIRES);
    }

    private static Perfil dadoDeBaja() {
        var perfil = nuevo();
        perfil.darDeBaja(USUARIO, "Ya no ofrezco mentorías", AHORA);
        return perfil;
    }

    @Nested
    class Crear {

        @Test
        void arrancaActivoSinCalificacionesConCanalInternoYSinDatosOpcionales() {
            var perfil = nuevo();

            assertEquals(USUARIO, perfil.userId());
            assertEquals(TipoPerfil.PROFESIONAL, perfil.tipo());
            assertEquals("Ana Pérez", perfil.nombreVisible());
            assertEquals(BUENOS_AIRES, perfil.zonaHoraria());
            assertTrue(perfil.estaActivo());
            assertEquals(Reputacion.inicial(), perfil.reputacion());
            assertEquals(CanalNotificacion.INTERNA, perfil.canal());
            assertEquals("", perfil.biografia());
            assertEquals(Set.of(), perfil.habilidades());
            assertEquals(Set.of(), perfil.intereses());
            assertEquals(List.of(), perfil.disponibilidad());
            assertEquals(List.of(), perfil.historial());
        }

        @Test
        void rechazaDatosObligatoriosFaltantes() {
            assertThrows(IllegalArgumentException.class,
                    () -> Perfil.crear(null, TipoPerfil.PROFESIONAL, "Ana", BUENOS_AIRES));
            assertThrows(IllegalArgumentException.class,
                    () -> Perfil.crear(" ", TipoPerfil.PROFESIONAL, "Ana", BUENOS_AIRES));
            assertThrows(NullPointerException.class, () -> Perfil.crear(USUARIO, null, "Ana", BUENOS_AIRES));
            assertThrows(IllegalArgumentException.class,
                    () -> Perfil.crear(USUARIO, TipoPerfil.ORGANIZACION, "", BUENOS_AIRES));
            assertThrows(NullPointerException.class,
                    () -> Perfil.crear(USUARIO, TipoPerfil.ORGANIZACION, "Acme", null));
        }
    }

    @Nested
    class Edicion {

        @Test
        void editarDatosReemplazaNombreBiografiaYZonaHoraria() {
            var perfil = nuevo();

            perfil.editarDatos("Ana P.", "Desarrolladora Java", MADRID);

            assertEquals("Ana P.", perfil.nombreVisible());
            assertEquals("Desarrolladora Java", perfil.biografia());
            assertEquals(MADRID, perfil.zonaHoraria());
        }

        @Test
        void editarDatosExigeNombreYZonaPeroLaBiografiaEsOpcional() {
            var perfil = nuevo();

            assertThrows(IllegalArgumentException.class, () -> perfil.editarDatos(" ", "bio", MADRID));
            assertThrows(NullPointerException.class, () -> perfil.editarDatos("Ana", "bio", null));
            assertEquals("Ana Pérez", perfil.nombreVisible());

            perfil.editarDatos("Ana", null, MADRID);
            assertEquals("", perfil.biografia());
        }

        @Test
        void habilidadesEInteresesSeCopianYQuedanInmodificables() {
            var perfil = nuevo();
            var habilidades = new HashSet<>(Set.of("Java", "SQL"));

            perfil.definirHabilidades(habilidades);
            perfil.definirIntereses(Set.of("Arquitectura"));
            habilidades.add("Cobol");

            assertEquals(Set.of("Java", "SQL"), perfil.habilidades());
            assertEquals(Set.of("Arquitectura"), perfil.intereses());
            assertThrows(UnsupportedOperationException.class, () -> perfil.habilidades().add("Go"));
            assertThrows(UnsupportedOperationException.class, () -> perfil.intereses().clear());
        }

        @Test
        void habilidadesEInteresesVaciosOEnBlancoSeRechazan() {
            var perfil = nuevo();
            perfil.definirHabilidades(Set.of("Java"));

            assertThrows(IllegalArgumentException.class, () -> perfil.definirHabilidades(Set.of("Java", " ")));
            assertThrows(IllegalArgumentException.class, () -> perfil.definirIntereses(Set.of("")));
            assertThrows(IllegalArgumentException.class,
                    () -> perfil.definirHabilidades(new HashSet<>(Arrays.asList("Java", null))));
            assertThrows(NullPointerException.class, () -> perfil.definirIntereses(null));

            assertEquals(Set.of("Java"), perfil.habilidades());
            assertEquals(Set.of(), perfil.intereses());
        }

        @Test
        void elegirCanalCambiaLaPreferenciaDeNotificacion() {
            var perfil = nuevo();

            perfil.elegirCanal(CanalNotificacion.EMAIL);

            assertEquals(CanalNotificacion.EMAIL, perfil.canal());
            assertThrows(NullPointerException.class, () -> perfil.elegirCanal(null));
            assertEquals(CanalNotificacion.EMAIL, perfil.canal());
        }
    }

    @Nested
    class Disponibilidad {

        @Test
        void franjasIgualesEnDiasDistintosSeAceptan() {
            var perfil = nuevo();

            perfil.agregarDisponibilidad(DayOfWeek.MONDAY, NUEVE, DIEZ);
            perfil.agregarDisponibilidad(DayOfWeek.TUESDAY, NUEVE, DIEZ);
            perfil.agregarDisponibilidad(DayOfWeek.MONDAY, DIEZ, ONCE);

            assertEquals(List.of(
                    new DisponibilidadSemanal(DayOfWeek.MONDAY, NUEVE, DIEZ),
                    new DisponibilidadSemanal(DayOfWeek.TUESDAY, NUEVE, DIEZ),
                    new DisponibilidadSemanal(DayOfWeek.MONDAY, DIEZ, ONCE)), perfil.disponibilidad());
        }

        @Test
        void unaFranjaSuperpuestaElMismoDiaSeRechaza() {
            var perfil = nuevo();
            perfil.agregarDisponibilidad(DayOfWeek.MONDAY, NUEVE, ONCE);

            assertThrows(DisponibilidadInvalidaException.class,
                    () -> perfil.agregarDisponibilidad(DayOfWeek.MONDAY, DIEZ, LocalTime.of(12, 0)));
            assertThrows(DisponibilidadInvalidaException.class,
                    () -> perfil.agregarDisponibilidad(DayOfWeek.MONDAY, NUEVE, ONCE));

            assertEquals(1, perfil.disponibilidad().size());
        }

        @Test
        void unaFranjaConInicioIgualOPosteriorAlFinSeRechaza() {
            var perfil = nuevo();

            assertThrows(DisponibilidadInvalidaException.class,
                    () -> perfil.agregarDisponibilidad(DayOfWeek.MONDAY, DIEZ, DIEZ));
            assertThrows(DisponibilidadInvalidaException.class,
                    () -> perfil.agregarDisponibilidad(DayOfWeek.MONDAY, ONCE, NUEVE));

            assertEquals(List.of(), perfil.disponibilidad());
        }

        @Test
        void quitarDisponibilidadLaSacaYFallaSiNoPertenece() {
            var perfil = nuevo();
            perfil.agregarDisponibilidad(DayOfWeek.MONDAY, NUEVE, DIEZ);

            perfil.quitarDisponibilidad(new DisponibilidadSemanal(DayOfWeek.MONDAY, NUEVE, DIEZ));

            assertEquals(List.of(), perfil.disponibilidad());
            assertThrows(DisponibilidadInvalidaException.class,
                    () -> perfil.quitarDisponibilidad(new DisponibilidadSemanal(DayOfWeek.MONDAY, NUEVE, DIEZ)));
        }

        @Test
        void laDisponibilidadDevueltaNoSePuedeModificar() {
            var perfil = nuevo();
            perfil.agregarDisponibilidad(DayOfWeek.MONDAY, NUEVE, DIEZ);

            assertThrows(UnsupportedOperationException.class, () -> perfil.disponibilidad().clear());
            assertEquals(1, perfil.disponibilidad().size());
        }
    }

    @Nested
    class ReputacionDelPerfil {

        @Test
        void registrarCalificacionAcumulaLaReputacion() {
            var perfil = nuevo();

            perfil.registrarCalificacion(new Puntaje(5));
            perfil.registrarCalificacion(new Puntaje(4));

            assertEquals(4.5, perfil.reputacion().promedio());
            assertEquals(2, perfil.reputacion().cantidad());
        }

        @Test
        void registrarCalificacionExigePuntaje() {
            var perfil = nuevo();

            assertThrows(NullPointerException.class, () -> perfil.registrarCalificacion(null));
            assertEquals(Reputacion.inicial(), perfil.reputacion());
        }

        @Test
        void soloRegistrarCalificacionPuedeCambiarLaReputacion() {
            // Solo métodos de instancia: Perfil.reconstruir recibe la reputación guardada, pero arma un
            // perfil nuevo desde el repositorio; no cambia la de un perfil existente.
            var publicos = Arrays.stream(Perfil.class.getMethods())
                    .filter(m -> m.getDeclaringClass() == Perfil.class)
                    .filter(m -> !Modifier.isStatic(m.getModifiers()))
                    .toList();

            var queRecibenReputacionOPuntaje = publicos.stream()
                    .filter(m -> Arrays.stream(m.getParameterTypes())
                            .anyMatch(t -> t == Reputacion.class || t == Puntaje.class))
                    .map(Method::getName)
                    .toList();

            assertEquals(List.of("registrarCalificacion"), queRecibenReputacionOPuntaje);
            assertTrue(publicos.stream().noneMatch(m -> m.getName().startsWith("set")));
            assertTrue(Arrays.stream(Perfil.class.getDeclaredConstructors())
                    .noneMatch(c -> Modifier.isPublic(c.getModifiers())));
            assertTrue(Arrays.stream(Perfil.class.getDeclaredFields())
                    .filter(f -> f.getType() == Reputacion.class)
                    .allMatch(f -> Modifier.isPrivate(f.getModifiers())));
        }
    }

    @Nested
    class Baja {

        @Test
        void laBajaQuedaEnElHistorialConActorYMotivo() {
            var perfil = nuevo();

            perfil.darDeBaja(ADMIN, "Incumplimiento de normas", AHORA);

            assertFalse(perfil.estaActivo());
            assertEquals(List.of(new CambioEstado("ACTIVO", "INACTIVO", AHORA, ADMIN, "Incumplimiento de normas")),
                    perfil.historial());
        }

        @Test
        void unaSegundaBajaSeRechazaYNoDejaRastro() {
            var perfil = dadoDeBaja();
            var historial = perfil.historial();

            var ex = assertThrows(TransicionInvalidaException.class,
                    () -> perfil.darDeBaja(USUARIO, "otra vez", AHORA.plusSeconds(60)));

            assertEquals("INACTIVO", ex.estado());
            assertEquals(historial, perfil.historial());
        }

        @Test
        void laBajaExigeMotivoActorYFecha() {
            var perfil = nuevo();

            assertThrows(IllegalArgumentException.class, () -> perfil.darDeBaja(USUARIO, " ", AHORA));
            assertThrows(IllegalArgumentException.class, () -> perfil.darDeBaja(null, "motivo", AHORA));
            assertThrows(NullPointerException.class, () -> perfil.darDeBaja(USUARIO, "motivo", null));

            assertTrue(perfil.estaActivo());
            assertEquals(List.of(), perfil.historial());
        }

        @Test
        void unPerfilDadoDeBajaRechazaLasEdicionesSinCambiarNada() {
            var perfil = dadoDeBaja();
            Map<String, Executable> ediciones = new LinkedHashMap<>();
            ediciones.put("editarDatos", () -> perfil.editarDatos("Otro", "bio", MADRID));
            ediciones.put("definirHabilidades", () -> perfil.definirHabilidades(Set.of("Java")));
            ediciones.put("definirIntereses", () -> perfil.definirIntereses(Set.of("Go")));
            ediciones.put("agregarDisponibilidad", () -> perfil.agregarDisponibilidad(DayOfWeek.MONDAY, NUEVE, DIEZ));
            ediciones.put("quitarDisponibilidad",
                    () -> perfil.quitarDisponibilidad(new DisponibilidadSemanal(DayOfWeek.MONDAY, NUEVE, DIEZ)));
            ediciones.put("elegirCanal", () -> perfil.elegirCanal(CanalNotificacion.PUSH));

            ediciones.forEach((nombre, edicion) -> {
                var ex = assertThrows(PerfilInactivoException.class, edicion, nombre);
                assertEquals(USUARIO, ex.userId());
            });

            assertEquals("Ana Pérez", perfil.nombreVisible());
            assertEquals(BUENOS_AIRES, perfil.zonaHoraria());
            assertEquals(Set.of(), perfil.habilidades());
            assertEquals(Set.of(), perfil.intereses());
            assertEquals(List.of(), perfil.disponibilidad());
            assertEquals(CanalNotificacion.INTERNA, perfil.canal());
        }

        @Test
        void unPerfilDadoDeBajaSigueAceptandoCalificaciones() {
            var perfil = dadoDeBaja();

            perfil.registrarCalificacion(new Puntaje(3));

            assertEquals(1, perfil.reputacion().cantidad());
            assertEquals(3.0, perfil.reputacion().promedio());
        }

        @Test
        void elHistorialDevueltoNoSePuedeModificar() {
            var perfil = dadoDeBaja();

            assertThrows(UnsupportedOperationException.class, () -> perfil.historial().clear());
            assertEquals(1, perfil.historial().size());
        }
    }
}
