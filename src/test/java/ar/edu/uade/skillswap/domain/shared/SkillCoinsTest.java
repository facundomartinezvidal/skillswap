package ar.edu.uade.skillswap.domain.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillCoinsTest {

    @Test
    void sumarDevuelveUnNuevoMonto() {
        var cien = new SkillCoins(100);

        assertEquals(new SkillCoins(150), cien.sumar(new SkillCoins(50)));
        assertEquals(new SkillCoins(100), cien);
    }

    @Test
    void restarDevuelveLaDiferencia() {
        assertEquals(new SkillCoins(30), new SkillCoins(100).restar(new SkillCoins(70)));
        assertEquals(new SkillCoins(0), new SkillCoins(100).restar(new SkillCoins(100)));
    }

    @Test
    void restarMasDeLoQueHayFallaPorqueNoHayMontosNegativos() {
        assertThrows(IllegalArgumentException.class, () -> new SkillCoins(10).restar(new SkillCoins(11)));
    }

    @Test
    void sumarRechazaElDesbordamiento() {
        assertThrows(ArithmeticException.class, () -> new SkillCoins(Long.MAX_VALUE).sumar(new SkillCoins(1)));
    }

    @Test
    void esMenorQueComparaPorValor() {
        assertTrue(new SkillCoins(9).esMenorQue(new SkillCoins(10)));
        assertFalse(new SkillCoins(10).esMenorQue(new SkillCoins(10)));
        assertFalse(new SkillCoins(11).esMenorQue(new SkillCoins(10)));
    }

    @Test
    void lasOperacionesRechazanNulo() {
        var monto = new SkillCoins(1);

        assertThrows(NullPointerException.class, () -> monto.sumar(null));
        assertThrows(NullPointerException.class, () -> monto.restar(null));
        assertThrows(NullPointerException.class, () -> monto.esMenorQue(null));
    }
}
