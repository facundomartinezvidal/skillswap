package ar.edu.uade.skillswap.domain.wallet;

import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.wallet.exception.SaldoInsuficienteException;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ReconstruirWalletTest {

    private static final Instant AHORA = Instant.parse("2026-11-15T12:00:00Z");

    @Test
    void conservaElSaldoYArrancaSinMovimientosNuevos() {
        var wallet = Wallet.reconstruir("user-mateo", new SkillCoins(500));

        assertEquals("user-mateo", wallet.titularId());
        assertEquals(new SkillCoins(500), wallet.saldo());
        assertTrue(wallet.movimientosNuevos().isEmpty());
        assertFalse(wallet.esGarantia());
    }

    @Test
    void sigueRechazandoElSaldoNegativo() {
        var wallet = Wallet.reconstruir("user-mateo", new SkillCoins(500));

        assertThrows(SaldoInsuficienteException.class,
                () -> wallet.debitar(new SkillCoins(600), "Pago", ContratacionId.generate(), AHORA));
        assertEquals(new SkillCoins(500), wallet.saldo());
    }

    @Test
    void tambienReconstruyeLaGarantia() {
        var garantia = Wallet.reconstruir(Wallet.TITULAR_GARANTIA, new SkillCoins(300));

        assertTrue(garantia.esGarantia());
        assertEquals(new SkillCoins(300), garantia.saldo());
    }

    @Test
    void exigeTitularYSaldo() {
        assertThrows(IllegalArgumentException.class, () -> Wallet.reconstruir(" ", new SkillCoins(0)));
        assertThrows(NullPointerException.class, () -> Wallet.reconstruir("user-mateo", null));
    }
}
