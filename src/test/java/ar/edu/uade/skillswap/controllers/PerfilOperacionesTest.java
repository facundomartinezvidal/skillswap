package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.controllers.impl.SkillSwapControllers;
import ar.edu.uade.skillswap.model.Models.Modalidad;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static ar.edu.uade.skillswap.controllers.EntornoDemo.ANA;
import static ar.edu.uade.skillswap.controllers.EntornoDemo.MATEO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Edición, disponibilidad semanal y baja del perfil, a través de {@link PerfilOperaciones}. */
class PerfilOperacionesTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final LocalTime NUEVE = LocalTime.of(9, 0);
    private static final LocalTime DOCE = LocalTime.of(12, 0);

    private final EntornoDemo demo = new EntornoDemo();
    private final SkillSwapControllers c = demo.controllers;

    @Test
    void editarElPerfil() {
        var editado = c.perfiles().editarPerfil(MATEO, "Mateo S.", "Mentor de Java y SQL", ZONA,
                Set.of("Java", "SQL"), Set.of("Liderazgo"));

        assertEquals("Mateo S.", editado.nombre());
        assertEquals("Mentor de Java y SQL", editado.bio());
        assertEquals(List.of("Java", "SQL"), editado.habilidades());
        assertEquals(editado, c.perfiles().verPerfil(MATEO));
    }

    @Test
    void editarSinNombreSeRechaza() {
        assertRechazada("El nombre visible es obligatorio",
                () -> c.perfiles().editarPerfil(MATEO, " ", null, ZONA, null, null));
    }

    @Test
    void agregarYQuitarDisponibilidad() {
        var conLunes = c.perfiles().agregarDisponibilidad(MATEO, DayOfWeek.MONDAY, NUEVE, DOCE);
        var sinLunes = c.perfiles().quitarDisponibilidad(MATEO, DayOfWeek.MONDAY, NUEVE, DOCE);

        assertEquals(List.of(new DisponibilidadVista(DayOfWeek.MONDAY, NUEVE, DOCE)), conLunes);
        assertEquals(List.of(), sinLunes);
        assertEquals(List.of(), c.perfiles().verDisponibilidad(MATEO));
    }

    @Test
    void disponibilidadSuperpuestaSeRechaza() {
        c.perfiles().agregarDisponibilidad(MATEO, DayOfWeek.MONDAY, NUEVE, DOCE);

        assertRechazada("La disponibilidad se superpone con otra del mismo día",
                () -> c.perfiles().agregarDisponibilidad(MATEO, DayOfWeek.MONDAY, LocalTime.of(11, 0),
                        LocalTime.of(13, 0)));
        assertEquals(1, c.perfiles().verDisponibilidad(MATEO).size());
    }

    @Test
    void quitarUnaDisponibilidadQueNoExisteSeRechaza() {
        assertRechazada("La disponibilidad no pertenece a este perfil",
                () -> c.perfiles().quitarDisponibilidad(MATEO, DayOfWeek.FRIDAY, NUEVE, DOCE));
    }

    @Test
    void unPerfilDadoDeBajaNoSeEditaNiPublica() {
        c.perfiles().darDeBaja(ANA, "Me voy de la plataforma");

        assertRechazada("El perfil user-ana está dado de baja: no se puede editar los datos",
                () -> c.perfiles().editarPerfil(ANA, "Ana", null, ZONA, null, null));
        assertRechazada("El perfil user-ana está dado de baja: no se puede agregar disponibilidad",
                () -> c.perfiles().agregarDisponibilidad(ANA, DayOfWeek.MONDAY, NUEVE, DOCE));
        assertRechazada("El perfil user-ana está dado de baja: no se puede crear una publicación",
                () -> c.publicaciones().crearBorrador(ANA, "Algo", "", "Tecnología", Modalidad.REMOTA, 60, 10));
        assertEquals("Ana Gómez", c.perfiles().verPerfil(ANA).nombre(), "la baja es lógica");
    }

    @Test
    void darDeBajaSinMotivoODosVecesSeRechaza() {
        assertRechazada("Dar de baja un perfil requiere un motivo", () -> c.perfiles().darDeBaja(ANA, ""));

        c.perfiles().darDeBaja(ANA, "Me voy");

        assertRechazada("No se puede dar de baja en estado INACTIVO", () -> c.perfiles().darDeBaja(ANA, "Otra vez"));
        assertFalse(c.perfiles().verPerfil(ANA).nombre().isBlank());
    }

    private static void assertRechazada(String mensaje, Executable operacion) {
        var error = assertThrows(OperacionRechazadaException.class, operacion);
        assertEquals(mensaje, error.getMessage());
    }
}
