package ar.edu.uade.skillswap.domain.publicacion.decorator;

import ar.edu.uade.skillswap.domain.shared.SkillCoins;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Cadenas del patrón Decorator con 0, 1 y varias promociones. */
class PrecioTest {

    private static final SkillCoins CIEN = new SkillCoins(100);

    private static Precio cadena(SkillCoins base, Promocion... promociones) {
        Precio precio = new PrecioBase(base);
        for (var promocion : promociones) {
            precio = new PrecioConPromocion(precio, promocion);
        }
        return precio;
    }

    @Test
    void sinPromocionesElValorEsElBase() {
        assertEquals(CIEN, cadena(CIEN).valor());
    }

    @Test
    void conUnaPromocionAplicaEsaPromocion() {
        assertEquals(new SkillCoins(90), cadena(CIEN, new DescuentoPorcentual(10)).valor());
    }

    @Test
    void cadaDecoradorAplicaSobreElResultadoDelAnterior() {
        assertEquals(new SkillCoins(70),
                cadena(CIEN, new DescuentoPorcentual(10), new DescuentoFijo(new SkillCoins(20))).valor());
        assertEquals(new SkillCoins(72),
                cadena(CIEN, new DescuentoFijo(new SkillCoins(20)), new DescuentoPorcentual(10)).valor());
    }

    @Test
    void variosDescuentosQueSuperanElPrecioDejanCeroSinExcepcion() {
        var precio = cadena(CIEN, new DescuentoFijo(new SkillCoins(80)), new DescuentoFijo(new SkillCoins(50)));

        assertEquals(new SkillCoins(0), precio.valor());
    }

    @Test
    void unaPromocionPosteriorAlCeroLoMantieneEnCero() {
        var precio = cadena(CIEN, new DescuentoPorcentual(100), new DescuentoFijo(new SkillCoins(10)));

        assertEquals(new SkillCoins(0), precio.valor());
    }

    @Test
    void rechazaPartesNulas() {
        assertThrows(NullPointerException.class, () -> new PrecioBase(null));
        assertThrows(NullPointerException.class, () -> new PrecioConPromocion(null, new DescuentoPorcentual(10)));
        assertThrows(NullPointerException.class, () -> new PrecioConPromocion(new PrecioBase(CIEN), null));
    }
}
