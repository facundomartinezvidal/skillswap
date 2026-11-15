package ar.edu.uade.skillswap.domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PuntajeTest {

    @ParameterizedTest(name = "acepta {0}")
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void aceptaValoresDe1A5(int valor) {
        assertEquals(valor, new Puntaje(valor).valor());
    }

    @ParameterizedTest(name = "rechaza {0}")
    @ValueSource(ints = {-1, 0, 6, 10})
    void rechazaValoresFueraDeRango(int valor) {
        assertThrows(IllegalArgumentException.class, () -> new Puntaje(valor));
    }

    @Test
    void seComparaPorValor() {
        assertEquals(new Puntaje(4), new Puntaje(4));
    }
}
