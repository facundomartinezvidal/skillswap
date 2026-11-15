package ar.edu.uade.skillswap.domain.publicacion.decorator;

import ar.edu.uade.skillswap.domain.shared.SkillCoins;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PromocionTest {

    private static SkillCoins coins(long valor) {
        return new SkillCoins(valor);
    }

    @Nested
    class Porcentual {

        @Test
        void diezPorCientoDe100Da90() {
            assertEquals(coins(90), new DescuentoPorcentual(10).aplicar(coins(100)));
        }

        @Test
        void cienPorCientoDejaElPrecioEnCero() {
            assertEquals(coins(0), new DescuentoPorcentual(100).aplicar(coins(100)));
        }

        @Test
        void elDescuentoSeRedondeaHaciaAbajo() {
            assertEquals(coins(50), new DescuentoPorcentual(50).aplicar(coins(99)));
        }

        @Test
        void sobreUnPrecioCeroDaCero() {
            assertEquals(coins(0), new DescuentoPorcentual(30).aplicar(coins(0)));
        }

        @ParameterizedTest(name = "{0} %")
        @ValueSource(ints = {0, 101, -5})
        void rechazaPorcentajesFueraDe1A100(int porcentaje) {
            assertThrows(IllegalArgumentException.class, () -> new DescuentoPorcentual(porcentaje));
        }

        @Test
        void aceptaLosExtremos1Y100() {
            assertDoesNotThrow(() -> new DescuentoPorcentual(1));
            assertDoesNotThrow(() -> new DescuentoPorcentual(100));
        }
    }

    @Nested
    class Fijo {

        @Test
        void restaElMonto() {
            assertEquals(coins(80), new DescuentoFijo(coins(20)).aplicar(coins(100)));
        }

        @Test
        void unMontoMayorAlPrecioDejaCeroSinExcepcion() {
            assertEquals(coins(0), new DescuentoFijo(coins(150)).aplicar(coins(100)));
        }

        @Test
        void unMontoIgualAlPrecioDejaCero() {
            assertEquals(coins(0), new DescuentoFijo(coins(100)).aplicar(coins(100)));
        }

        @Test
        void rechazaMontoCero() {
            assertThrows(IllegalArgumentException.class, () -> new DescuentoFijo(coins(0)));
        }

        @Test
        void rechazaMontoNulo() {
            assertThrows(NullPointerException.class, () -> new DescuentoFijo(null));
        }
    }

    @Test
    void lasPromocionesIgualesSeComparanPorValor() {
        assertEquals(new DescuentoPorcentual(10), new DescuentoPorcentual(10));
        assertEquals(new DescuentoFijo(coins(20)), new DescuentoFijo(coins(20)));
        assertNotEquals(new DescuentoPorcentual(10), new DescuentoPorcentual(20));
    }
}
