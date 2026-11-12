package ar.edu.uade.skillswap.dominio.publicacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Matriz completa estado × acción según el diagrama del plan (D1, D2). */
class EstadoPublicacionTest {

    private static final Map<String, Function<EstadoPublicacion, EstadoPublicacion>> ACCIONES = Map.of(
            "publicar", EstadoPublicacion::publicar,
            "pausar", EstadoPublicacion::pausar,
            "reanudar", EstadoPublicacion::reanudar,
            "cerrar", EstadoPublicacion::cerrar,
            "cancelar", EstadoPublicacion::cancelar);

    static Stream<Arguments> transicionesPermitidas() {
        return Stream.of(
                Arguments.of(new Borrador(), "publicar", "PUBLICADA"),
                Arguments.of(new Borrador(), "cancelar", "CANCELADA"),
                Arguments.of(new Publicada(), "pausar", "PAUSADA"),
                Arguments.of(new Publicada(), "cerrar", "CERRADA"),
                Arguments.of(new Publicada(), "cancelar", "CANCELADA"),
                Arguments.of(new Pausada(), "reanudar", "PUBLICADA"),
                Arguments.of(new Pausada(), "cerrar", "CERRADA"),
                Arguments.of(new Pausada(), "cancelar", "CANCELADA"));
    }

    static Stream<Arguments> transicionesRechazadas() {
        return Stream.of(
                Arguments.of(new Borrador(), "pausar"),
                Arguments.of(new Borrador(), "reanudar"),
                Arguments.of(new Borrador(), "cerrar"),
                Arguments.of(new Publicada(), "publicar"),
                Arguments.of(new Publicada(), "reanudar"),
                Arguments.of(new Pausada(), "publicar"),
                Arguments.of(new Pausada(), "pausar"),
                Arguments.of(new Cerrada(), "publicar"),
                Arguments.of(new Cerrada(), "pausar"),
                Arguments.of(new Cerrada(), "reanudar"),
                Arguments.of(new Cerrada(), "cerrar"),
                Arguments.of(new Cerrada(), "cancelar"),
                Arguments.of(new Cancelada(), "publicar"),
                Arguments.of(new Cancelada(), "pausar"),
                Arguments.of(new Cancelada(), "reanudar"),
                Arguments.of(new Cancelada(), "cerrar"),
                Arguments.of(new Cancelada(), "cancelar"));
    }

    @ParameterizedTest(name = "{0} --{1}--> {2}")
    @MethodSource("transicionesPermitidas")
    void transicionPermitidaDevuelveElEstadoSiguiente(EstadoPublicacion desde, String accion, String hacia) {
        assertEquals(hacia, ACCIONES.get(accion).apply(desde).nombre());
    }

    @ParameterizedTest(name = "{0} no permite {1}")
    @MethodSource("transicionesRechazadas")
    void transicionNoListadaLanzaTransicionInvalida(EstadoPublicacion desde, String accion) {
        var ex = assertThrows(TransicionInvalidaException.class, () -> ACCIONES.get(accion).apply(desde));
        assertEquals(desde.nombre(), ex.estado());
        assertEquals(accion, ex.accion());
    }

    @Test
    void laMatrizCubreTodasLasCombinacionesEstadoAccion() {
        long estados = 5;
        assertEquals(estados * ACCIONES.size(), transicionesPermitidas().count() + transicionesRechazadas().count());
    }

    @Test
    void soloBorradorPermiteEditar() {
        assertDoesNotThrow(() -> new Borrador().editar());
        for (var estado : new EstadoPublicacion[]{new Publicada(), new Pausada(), new Cerrada(), new Cancelada()}) {
            assertThrows(TransicionInvalidaException.class, estado::editar, estado.nombre());
        }
    }

    @Test
    void soloPublicadaAdmiteSolicitudes() {
        assertTrue(new Publicada().admiteSolicitudes());
        assertFalse(new Borrador().admiteSolicitudes());
        assertFalse(new Pausada().admiteSolicitudes());
        assertFalse(new Cerrada().admiteSolicitudes());
        assertFalse(new Cancelada().admiteSolicitudes());
    }

    @Test
    void nombresCoincidenConLosEstadosDeLaConsigna() {
        assertEquals("BORRADOR", new Borrador().nombre());
        assertEquals("PUBLICADA", new Publicada().nombre());
        assertEquals("PAUSADA", new Pausada().nombre());
        assertEquals("CERRADA", new Cerrada().nombre());
        assertEquals("CANCELADA", new Cancelada().nombre());
    }
}
