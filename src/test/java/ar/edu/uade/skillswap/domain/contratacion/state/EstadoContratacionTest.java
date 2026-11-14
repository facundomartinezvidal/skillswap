package ar.edu.uade.skillswap.domain.contratacion.state;

import ar.edu.uade.skillswap.domain.shared.TransicionInvalidaException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Matriz completa estado × acción de una contratación. */
class EstadoContratacionTest {

    private static final Map<String, Function<EstadoContratacion, EstadoContratacion>> ACCIONES = Map.of(
            "aceptar", EstadoContratacion::aceptar,
            "rechazar", EstadoContratacion::rechazar,
            "pagar", EstadoContratacion::pagar,
            "iniciar", EstadoContratacion::iniciar,
            "completar", EstadoContratacion::completar,
            "cancelar", EstadoContratacion::cancelar);

    private static final List<EstadoContratacion> ESTADOS = List.of(
            new Pendiente(), new Aceptada(), new Confirmada(), new EnCurso(),
            new Completada(), new Rechazada(), new Cancelada());

    private static final Map<String, Map<String, String>> PERMITIDAS = Map.of(
            "PENDIENTE", Map.of("aceptar", "ACEPTADA", "rechazar", "RECHAZADA", "cancelar", "CANCELADA"),
            "ACEPTADA", Map.of("pagar", "CONFIRMADA", "cancelar", "CANCELADA"),
            "CONFIRMADA", Map.of("iniciar", "EN_CURSO", "cancelar", "CANCELADA"),
            "EN_CURSO", Map.of("completar", "COMPLETADA"));

    static Stream<Arguments> transicionesPermitidas() {
        return ESTADOS.stream().flatMap(estado -> PERMITIDAS.getOrDefault(estado.nombre(), Map.of())
                .entrySet().stream()
                .map(e -> Arguments.of(estado, e.getKey(), e.getValue())));
    }

    static Stream<Arguments> transicionesRechazadas() {
        return ESTADOS.stream().flatMap(estado -> ACCIONES.keySet().stream()
                .filter(accion -> !PERMITIDAS.getOrDefault(estado.nombre(), Map.of()).containsKey(accion))
                .map(accion -> Arguments.of(estado, accion)));
    }

    @ParameterizedTest(name = "{0} --{1}--> {2}")
    @MethodSource("transicionesPermitidas")
    void transicionPermitidaDevuelveElEstadoSiguiente(EstadoContratacion desde, String accion, String hacia) {
        assertEquals(hacia, ACCIONES.get(accion).apply(desde).nombre());
    }

    @ParameterizedTest(name = "{0} no permite {1}")
    @MethodSource("transicionesRechazadas")
    void transicionNoListadaLanzaTransicionInvalida(EstadoContratacion desde, String accion) {
        var ex = assertThrows(TransicionInvalidaException.class, () -> ACCIONES.get(accion).apply(desde));
        assertEquals(desde.nombre(), ex.estado());
        assertEquals(accion, ex.accion());
    }

    @Test
    void laMatrizCubreTodasLasCombinacionesEstadoAccion() {
        assertEquals(8, transicionesPermitidas().count());
        assertEquals(ESTADOS.size() * ACCIONES.size(),
                transicionesPermitidas().count() + transicionesRechazadas().count());
    }

    @Test
    void losEstadosFinalesNoAdmitenNingunaAccion() {
        for (var estado : List.of(new Completada(), new Rechazada(), new Cancelada())) {
            for (var accion : ACCIONES.values()) {
                assertThrows(TransicionInvalidaException.class, () -> accion.apply(estado), estado.nombre());
            }
        }
    }

    @Test
    void soloAceptadaYConfirmadaAdmitenReprogramacion() {
        assertTrue(new Aceptada().admiteReprogramacion());
        assertTrue(new Confirmada().admiteReprogramacion());
        assertFalse(new Pendiente().admiteReprogramacion());
        assertFalse(new EnCurso().admiteReprogramacion());
        assertFalse(new Completada().admiteReprogramacion());
        assertFalse(new Rechazada().admiteReprogramacion());
        assertFalse(new Cancelada().admiteReprogramacion());
    }

    @Test
    void nombresCoincidenConLosEstadosDeLaConsigna() {
        assertEquals(List.of("PENDIENTE", "ACEPTADA", "CONFIRMADA", "EN_CURSO", "COMPLETADA", "RECHAZADA", "CANCELADA"),
                ESTADOS.stream().map(EstadoContratacion::nombre).toList());
    }
}
