package ar.edu.uade.skillswap.domain.perfil;

import ar.edu.uade.skillswap.domain.perfil.exception.DisponibilidadInvalidaException;
import ar.edu.uade.skillswap.domain.shared.Puntaje;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class ValoresPerfilTest {

    private static final LocalTime NUEVE = LocalTime.of(9, 0);
    private static final LocalTime DIEZ = LocalTime.of(10, 0);
    private static final LocalTime ONCE = LocalTime.of(11, 0);

    @Test
    void reputacionArrancaSinCalificacionesYConPromedioCero() {
        var inicial = Reputacion.inicial();

        assertEquals(0, inicial.cantidad());
        assertEquals(0, inicial.sumaPuntajes());
        assertEquals(0.0, inicial.promedio());
    }

    @Test
    void reputacionConCincoYCuatroDaPromedioCuatroYMedio() {
        var reputacion = Reputacion.inicial().registrar(new Puntaje(5)).registrar(new Puntaje(4));

        assertEquals(4.5, reputacion.promedio());
        assertEquals(2, reputacion.cantidad());
        assertEquals(9, reputacion.sumaPuntajes());
    }

    @Test
    void registrarDevuelveUnaReputacionNuevaSinModificarLaAnterior() {
        var inicial = Reputacion.inicial();

        inicial.registrar(new Puntaje(5));

        assertEquals(Reputacion.inicial(), inicial);
    }

    @Test
    void reputacionRechazaValoresInconsistentes() {
        assertThrows(IllegalArgumentException.class, () -> new Reputacion(0, -1));
        assertThrows(IllegalArgumentException.class, () -> new Reputacion(5, 0));
        assertThrows(IllegalArgumentException.class, () -> new Reputacion(11, 2));
        assertThrows(IllegalArgumentException.class, () -> new Reputacion(1, 2));
        assertThrows(NullPointerException.class, () -> Reputacion.inicial().registrar(null));
    }

    @Test
    void disponibilidadRechazaInicioIgualOPosteriorAlFin() {
        assertThrows(DisponibilidadInvalidaException.class,
                () -> new DisponibilidadSemanal(DayOfWeek.MONDAY, DIEZ, DIEZ));
        assertThrows(DisponibilidadInvalidaException.class,
                () -> new DisponibilidadSemanal(DayOfWeek.MONDAY, ONCE, DIEZ));
    }

    @Test
    void disponibilidadRechazaDatosFaltantes() {
        assertThrows(DisponibilidadInvalidaException.class, () -> new DisponibilidadSemanal(null, NUEVE, DIEZ));
        assertThrows(DisponibilidadInvalidaException.class,
                () -> new DisponibilidadSemanal(DayOfWeek.MONDAY, null, DIEZ));
        assertThrows(DisponibilidadInvalidaException.class,
                () -> new DisponibilidadSemanal(DayOfWeek.MONDAY, NUEVE, null));
    }

    @Test
    void disponibilidadSeSolapaSoloElMismoDiaYConIntervalosSemiabiertos() {
        var lunesNueveADiez = new DisponibilidadSemanal(DayOfWeek.MONDAY, NUEVE, DIEZ);

        assertTrue(lunesNueveADiez.seSolapaCon(new DisponibilidadSemanal(DayOfWeek.MONDAY, LocalTime.of(9, 30), ONCE)));
        assertTrue(lunesNueveADiez.seSolapaCon(new DisponibilidadSemanal(DayOfWeek.MONDAY, NUEVE, DIEZ)));
        assertFalse(lunesNueveADiez.seSolapaCon(new DisponibilidadSemanal(DayOfWeek.MONDAY, DIEZ, ONCE)));
        assertFalse(lunesNueveADiez.seSolapaCon(new DisponibilidadSemanal(DayOfWeek.TUESDAY, NUEVE, DIEZ)));
    }

    @Test
    void tipoPerfilEsProfesionalUOrganizacion() {
        assertArrayEquals(new TipoPerfil[]{TipoPerfil.PROFESIONAL, TipoPerfil.ORGANIZACION}, TipoPerfil.values());
    }

    @Test
    void canalesDeNotificacionSonEmailPushEInterna() {
        assertArrayEquals(new CanalNotificacion[]{CanalNotificacion.EMAIL, CanalNotificacion.PUSH,
                CanalNotificacion.INTERNA}, CanalNotificacion.values());
    }
}
