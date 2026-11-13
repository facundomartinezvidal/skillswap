package ar.edu.uade.skillswap.domain.contratacion;

import ar.edu.uade.skillswap.domain.publicacion.FranjaId;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SolicitudTest {

    private static final FranjaId FRANJA = FranjaId.generate();

    @Test
    void rechazaObjetivoNulo() {
        var ex = assertThrows(IllegalArgumentException.class, () -> new Solicitud("hola", null, FRANJA));

        assertEquals("El objetivo de la solicitud es obligatorio", ex.getMessage());
    }

    @Test
    void rechazaObjetivoEnBlanco() {
        assertThrows(IllegalArgumentException.class, () -> new Solicitud("hola", "   ", FRANJA));
        assertThrows(IllegalArgumentException.class, () -> new Solicitud("hola", "", FRANJA));
    }

    @Test
    void elMensajeEsOpcional() {
        var solicitud = assertDoesNotThrow(() -> new Solicitud(null, "Mejorar mis tests", FRANJA));

        assertNull(solicitud.mensaje());
        assertEquals("Mejorar mis tests", solicitud.objetivo());
    }

    @Test
    void rechazaFranjaElegidaNula() {
        assertThrows(NullPointerException.class, () -> new Solicitud("hola", "aprender", null));
    }
}
