package ar.edu.uade.skillswap.domain.publicacion;

import ar.edu.uade.skillswap.domain.publicacion.exception.CupoAgotadoException;
import ar.edu.uade.skillswap.domain.publicacion.exception.FranjaInvalidaException;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.shared.TransicionInvalidaException;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Reglas 2 y 3 al elegir la franja de una solicitud: publicación publicada y franja con lugar. */
class FranjaDisponibleTest {

    private static final String AUTOR = "user-ana";
    private static final Instant AHORA = Instant.parse("2026-11-15T12:00:00Z");
    private static final Instant LUNES_10 = Instant.parse("2026-11-16T10:00:00Z");
    private static final Instant LUNES_11 = Instant.parse("2026-11-16T11:00:00Z");

    private Publicacion publicacion;
    private FranjaId franja;

    private void crearBorrador(int cupo) {
        publicacion = Publicacion.crearBorrador(AUTOR, TipoPublicacion.OFERTA);
        publicacion.editar("Mentoría de Java", "Revisión de código", "Programación", Modalidad.REMOTA,
                Duration.ofHours(1), cupo, new SkillCoins(100), Set.of("java"));
        franja = publicacion.agregarFranja(LUNES_10, LUNES_11);
    }

    @Test
    void devuelveLaFranjaElegidaSinReservarla() {
        crearBorrador(2);
        publicacion.publicar(AUTOR, AHORA);

        var elegida = publicacion.franjaDisponible(franja);

        assertEquals(franja, elegida.id());
        assertEquals(LUNES_10, elegida.inicio());
        assertEquals(0, elegida.reservados());
    }

    @Test
    void unaPublicacionNoPublicadaNoAdmiteSolicitudes() {
        crearBorrador(2);

        var error = assertThrows(TransicionInvalidaException.class, () -> publicacion.franjaDisponible(franja));
        assertEquals("BORRADOR", error.estado());

        publicacion.publicar(AUTOR, AHORA);
        publicacion.pausar(AUTOR, "vacaciones", AHORA);
        assertThrows(TransicionInvalidaException.class, () -> publicacion.franjaDisponible(franja));
    }

    @Test
    void unaFranjaDeOtraPublicacionSeRechaza() {
        crearBorrador(2);
        publicacion.publicar(AUTOR, AHORA);

        assertThrows(FranjaInvalidaException.class, () -> publicacion.franjaDisponible(FranjaId.generate()));
    }

    @Test
    void unaFranjaSinLugarSeRechaza() {
        crearBorrador(1);
        publicacion.publicar(AUTOR, AHORA);
        publicacion.reservarCupo(franja);

        assertThrows(CupoAgotadoException.class, () -> publicacion.franjaDisponible(franja));
    }
}
