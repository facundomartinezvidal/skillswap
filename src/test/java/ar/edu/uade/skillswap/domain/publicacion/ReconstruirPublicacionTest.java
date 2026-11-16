package ar.edu.uade.skillswap.domain.publicacion;

import ar.edu.uade.skillswap.domain.publicacion.decorator.DescuentoPorcentual;
import ar.edu.uade.skillswap.domain.publicacion.exception.CupoAgotadoException;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.shared.TransicionInvalidaException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReconstruirPublicacionTest {

    private static final String AUTOR = "user-ana";
    private static final Instant AHORA = Instant.parse("2026-11-15T12:00:00Z");
    private static final Instant LUNES_10 = Instant.parse("2026-11-16T10:00:00Z");
    private static final Instant LUNES_11 = Instant.parse("2026-11-16T11:00:00Z");

    private Publicacion original;
    private FranjaId franja;

    @BeforeEach
    void publicarConUnLugarReservado() {
        original = Publicacion.crearBorrador(AUTOR, TipoPublicacion.OFERTA);
        original.editar("Mentoría de Java", "Revisión de código", "Programación", Modalidad.REMOTA,
                Duration.ofHours(1), 1, new SkillCoins(100), Set.of("java"));
        franja = original.agregarFranja(LUNES_10, LUNES_11);
        original.agregarPromocion(new DescuentoPorcentual(10));
        original.publicar(AUTOR, AHORA);
        original.reservarCupo(franja);
    }

    /** Lo mismo que hace un repositorio: armarla de nuevo a partir de lo que guardó. */
    private static Publicacion copiaDe(Publicacion p) {
        var franjas = p.franjas().stream()
                .map(f -> FranjaDisponibilidad.reconstruir(f.id(), f.inicio(), f.fin(), f.reservados()))
                .toList();
        return Publicacion.reconstruir(p.id(), p.autorId(), p.tipo(), p.titulo(), p.descripcion(), p.categoria(),
                p.modalidad(), p.duracionEstimada(), p.cupo(), p.precioBase(), p.etiquetas(), p.estado(),
                p.motivoCancelacion(), franjas, p.promociones(), p.historial());
    }

    @Test
    void conservaLosDatosElEstadoLasPromocionesYElHistorial() {
        var copia = copiaDe(original);

        assertEquals(original.id(), copia.id());
        assertEquals(original.titulo(), copia.titulo());
        assertEquals(original.etiquetas(), copia.etiquetas());
        assertEquals("PUBLICADA", copia.estado().nombre());
        assertEquals(new SkillCoins(90), copia.precioFinal());
        assertEquals(original.historial(), copia.historial());
        assertEquals(1, copia.franjas().get(0).reservados());
    }

    @Test
    void sigueAplicandoLasReglasConLoQueYaTeniaReservado() {
        var copia = copiaDe(original);

        assertThrows(CupoAgotadoException.class, () -> copia.reservarCupo(franja));
        assertThrows(TransicionInvalidaException.class, () -> copia.publicar(AUTOR, AHORA));
    }

    @Test
    void cambiarLaCopiaNoCambiaLaOriginal() {
        var copia = copiaDe(original);

        copia.liberarCupo(franja);
        copia.pausar(AUTOR, "vacaciones", AHORA);

        assertEquals(1, original.franjas().get(0).reservados());
        assertEquals("PUBLICADA", original.estado().nombre());
    }

    @Test
    void unaFranjaNoPuedeTenerReservasNegativas() {
        assertThrows(IllegalArgumentException.class,
                () -> FranjaDisponibilidad.reconstruir(FranjaId.generate(), LUNES_10, LUNES_11, -1));
    }
}
