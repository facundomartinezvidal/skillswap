package ar.edu.uade.skillswap.domain.perfil;

import ar.edu.uade.skillswap.domain.perfil.exception.PerfilInactivoException;
import ar.edu.uade.skillswap.domain.shared.Puntaje;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReconstruirPerfilTest {

    private static final Instant AHORA = Instant.parse("2026-11-15T12:00:00Z");
    private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");

    /** Lo mismo que hace un repositorio: armarlo de nuevo a partir de lo que guardó. */
    private static Perfil copiaDe(Perfil p) {
        return Perfil.reconstruir(p.userId(), p.tipo(), p.nombreVisible(), p.biografia(), p.zonaHoraria(),
                p.habilidades(), p.intereses(), p.disponibilidad(), p.canal(), p.reputacion(), p.estaActivo(),
                p.historial());
    }

    private static Perfil perfilCompleto() {
        var perfil = Perfil.crear("user-ana", TipoPerfil.PROFESIONAL, "Ana", BUENOS_AIRES);
        perfil.editarDatos("Ana", "Mentora de Java", BUENOS_AIRES);
        perfil.definirHabilidades(Set.of("java", "testing"));
        perfil.definirIntereses(Set.of("arquitectura"));
        perfil.agregarDisponibilidad(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0));
        perfil.elegirCanal(CanalNotificacion.EMAIL);
        perfil.registrarCalificacion(new Puntaje(5));
        perfil.registrarCalificacion(new Puntaje(4));
        return perfil;
    }

    @Test
    void conservaLosDatosYLaReputacionAcumulada() {
        var original = perfilCompleto();

        var copia = copiaDe(original);

        assertEquals("Mentora de Java", copia.biografia());
        assertEquals(Set.of("java", "testing"), copia.habilidades());
        assertEquals(original.disponibilidad(), copia.disponibilidad());
        assertEquals(CanalNotificacion.EMAIL, copia.canal());
        assertEquals(4.5, copia.reputacion().promedio());
        assertEquals(2, copia.reputacion().cantidad());
        assertTrue(copia.estaActivo());
    }

    @Test
    void unPerfilDadoDeBajaSigueSinEditarsePeroRecibeCalificaciones() {
        var original = perfilCompleto();
        original.darDeBaja("user-ana", "me voy", AHORA);

        var copia = copiaDe(original);

        assertFalse(copia.estaActivo());
        assertEquals(original.historial(), copia.historial());
        assertThrows(PerfilInactivoException.class, () -> copia.elegirCanal(CanalNotificacion.PUSH));
        copia.registrarCalificacion(new Puntaje(3));
        assertEquals(3, copia.reputacion().cantidad());
    }

    @Test
    void cambiarLaCopiaNoCambiaElOriginal() {
        var original = perfilCompleto();
        var copia = copiaDe(original);

        copia.registrarCalificacion(new Puntaje(1));
        copia.agregarDisponibilidad(DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertEquals(2, original.reputacion().cantidad());
        assertEquals(1, original.disponibilidad().size());
    }
}
