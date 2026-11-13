package ar.edu.uade.skillswap.dominio.publicaciones;

import ar.edu.uade.skillswap.dominio.compartido.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PublicacionTest {
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 10, 12, 0);

    @Test
    void noPermitePublicarSinFranjaDeDisponibilidad() {
        Publicacion publicacion = borrador();

        assertThrows(ReglaNegocioException.class,
                () -> publicacion.cambiarEstado(EstadoPublicacion.PUBLICADA, "autor-1", "", AHORA));
    }

    @Test
    void publicaYGuardaHistorialInmutableDeTransiciones() {
        Publicacion publicacion = borrador()
                .agregarDisponibilidad(new FranjaDisponibilidad(AHORA.plusDays(1), AHORA.plusDays(1).plusHours(1)))
                .cambiarEstado(EstadoPublicacion.PUBLICADA, "autor-1", "", AHORA);

        assertEquals(EstadoPublicacion.PUBLICADA, publicacion.estado());
        assertEquals(1, publicacion.historial().size());
        assertThrows(UnsupportedOperationException.class,
                () -> publicacion.historial().clear());
    }

    @Test
    void cerrarEsTerminalYCancelarRequiereMotivo() {
        Publicacion publicada = borrador()
                .agregarDisponibilidad(new FranjaDisponibilidad(AHORA.plusDays(1), AHORA.plusDays(1).plusHours(1)))
                .cambiarEstado(EstadoPublicacion.PUBLICADA, "autor-1", "", AHORA);

        assertThrows(ReglaNegocioException.class,
                () -> publicada.cambiarEstado(EstadoPublicacion.CANCELADA, "autor-1", "", AHORA));
        Publicacion cerrada = publicada.cambiarEstado(EstadoPublicacion.CERRADA, "autor-1", "", AHORA);
        assertThrows(ReglaNegocioException.class,
                () -> cerrada.cambiarEstado(EstadoPublicacion.PUBLICADA, "autor-1", "", AHORA));
    }

    private Publicacion borrador() {
        return Publicacion.crearBorrador("pub-1", "autor-1", TipoPublicacion.OFERTA,
                "Mentoría Java", "Descripción", "Tecnología", Modalidad.REMOTA,
                60, 1, 150, List.of("Java"));
    }
}
