package ar.edu.uade.skillswap.domain.wallet;

import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.wallet.exception.SaldoInsuficienteException;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WalletTest {

    private static final String TITULAR = "user-mateo";
    private static final Instant AHORA = Instant.parse("2026-11-14T12:00:00Z");
    private static final SkillCoins BONO = new SkillCoins(2500);
    private static final ContratacionId CONTRATACION = ContratacionId.generate();

    private static Wallet conBono() {
        return Wallet.abrir(TITULAR, BONO, AHORA);
    }

    @Test
    void abrirConBonoAcreditaElSaldoYRegistraUnaAcreditacionDemo() {
        var wallet = conBono();

        assertEquals(TITULAR, wallet.titularId());
        assertEquals(BONO, wallet.saldo());
        assertFalse(wallet.esGarantia());
        var movimientos = wallet.movimientosNuevos();
        assertEquals(1, movimientos.size());
        var bono = movimientos.get(0);
        assertEquals(TipoMovimiento.ACREDITACION_DEMO, bono.tipo());
        assertEquals(BONO, bono.importe());
        assertEquals(BONO, bono.saldoResultante());
        assertEquals("Bono de bienvenida", bono.concepto());
        assertNull(bono.contratacionId());
        assertEquals(AHORA, bono.fecha());
    }

    @Test
    void abrirSinBonoNoRegistraMovimientos() {
        var wallet = Wallet.abrir(TITULAR, new SkillCoins(0), AHORA);

        assertEquals(new SkillCoins(0), wallet.saldo());
        assertTrue(wallet.movimientosNuevos().isEmpty());
    }

    @Test
    void abrirExigeTitularBonoYFecha() {
        assertThrows(IllegalArgumentException.class, () -> Wallet.abrir(" ", BONO, AHORA));
        assertThrows(IllegalArgumentException.class, () -> Wallet.abrir(null, BONO, AHORA));
        assertThrows(NullPointerException.class, () -> Wallet.abrir(TITULAR, null, AHORA));
        assertThrows(NullPointerException.class, () -> Wallet.abrir(TITULAR, BONO, null));
    }

    @Test
    void unUsuarioNoPuedeUsarElTitularDeLaGarantia() {
        assertThrows(IllegalArgumentException.class, () -> Wallet.abrir(Wallet.TITULAR_GARANTIA, BONO, AHORA));
    }

    @Test
    void laGarantiaEmpiezaVaciaYSinMovimientos() {
        var garantia = Wallet.abrirGarantia();

        assertTrue(garantia.esGarantia());
        assertEquals(Wallet.TITULAR_GARANTIA, garantia.titularId());
        assertEquals(new SkillCoins(0), garantia.saldo());
        assertTrue(garantia.movimientosNuevos().isEmpty());
    }

    @Test
    void debitarRestaDelSaldoYDevuelveElMovimiento() {
        var wallet = conBono();

        var debito = wallet.debitar(new SkillCoins(100), "Pago", CONTRATACION, AHORA);

        assertEquals(new SkillCoins(2400), wallet.saldo());
        assertEquals(TipoMovimiento.DEBITO, debito.tipo());
        assertEquals(new SkillCoins(100), debito.importe());
        assertEquals(new SkillCoins(2400), debito.saldoResultante());
        assertEquals(CONTRATACION, debito.contratacionId());
        assertEquals("Pago", debito.concepto());
        assertEquals(debito, wallet.movimientosNuevos().get(1));
    }

    @Test
    void debitarExactamenteElSaldoLoDejaEnCero() {
        var wallet = conBono();

        wallet.debitar(BONO, "Pago", CONTRATACION, AHORA);

        assertEquals(new SkillCoins(0), wallet.saldo());
    }

    @Test
    void debitarMasQueElSaldoFallaSinModificarNada() {
        var wallet = conBono();
        var movimientosAntes = wallet.movimientosNuevos();

        var error = assertThrows(SaldoInsuficienteException.class,
                () -> wallet.debitar(new SkillCoins(2501), "Pago", CONTRATACION, AHORA));

        assertEquals(BONO, error.saldo());
        assertEquals(new SkillCoins(2501), error.importe());
        assertEquals(BONO, wallet.saldo());
        assertEquals(movimientosAntes, wallet.movimientosNuevos());
    }

    @Test
    void acreditarReintegrarYAcreditarDemoSumanAlSaldo() {
        var wallet = conBono();

        var credito = wallet.acreditar(new SkillCoins(100), "Cobro", CONTRATACION, AHORA);
        var reintegro = wallet.reintegrar(new SkillCoins(50), "Reintegro", CONTRATACION, AHORA);
        var demo = wallet.acreditarDemo(new SkillCoins(25), AHORA);

        assertEquals(TipoMovimiento.CREDITO, credito.tipo());
        assertEquals(TipoMovimiento.REINTEGRO, reintegro.tipo());
        assertEquals(TipoMovimiento.ACREDITACION_DEMO, demo.tipo());
        assertNull(demo.contratacionId());
        assertEquals(new SkillCoins(2675), wallet.saldo());
    }

    @Test
    void cadaMovimientoGuardaElSaldoResultanteEnOrden() {
        var wallet = conBono();

        wallet.debitar(new SkillCoins(500), "Pago", CONTRATACION, AHORA);
        wallet.acreditar(new SkillCoins(200), "Cobro", CONTRATACION, AHORA);
        wallet.reintegrar(new SkillCoins(100), "Reintegro", CONTRATACION, AHORA);
        wallet.acreditarDemo(new SkillCoins(10), AHORA);

        var saldos = wallet.movimientosNuevos().stream().map(m -> m.saldoResultante().valor()).toList();
        assertEquals(List.of(2500L, 2000L, 2200L, 2300L, 2310L), saldos);
        var sumaConSigno = wallet.movimientosNuevos().stream().mapToLong(Movimiento::importeConSigno).sum();
        assertEquals(wallet.saldo().valor(), sumaConSigno);
    }

    @Test
    void losMovimientosNuevosNoSePuedenModificar() {
        var wallet = conBono();
        var movimientos = wallet.movimientosNuevos();

        assertThrows(UnsupportedOperationException.class, movimientos::clear);
        assertThrows(UnsupportedOperationException.class, () -> movimientos.add(movimientos.get(0)));
        assertEquals(1, wallet.movimientosNuevos().size());
    }

    @Test
    void lasOperacionesRechazanImporteCeroONuloSinModificarNada() {
        var wallet = conBono();
        var cero = new SkillCoins(0);

        assertThrows(IllegalArgumentException.class, () -> wallet.debitar(cero, "Pago", CONTRATACION, AHORA));
        assertThrows(IllegalArgumentException.class, () -> wallet.acreditar(cero, "Cobro", CONTRATACION, AHORA));
        assertThrows(IllegalArgumentException.class, () -> wallet.reintegrar(cero, "Reintegro", CONTRATACION, AHORA));
        assertThrows(IllegalArgumentException.class, () -> wallet.acreditarDemo(cero, AHORA));
        assertThrows(IllegalArgumentException.class, () -> wallet.debitar(null, "Pago", CONTRATACION, AHORA));
        assertThrows(IllegalArgumentException.class, () -> wallet.acreditarDemo(null, AHORA));
        assertEquals(BONO, wallet.saldo());
        assertEquals(1, wallet.movimientosNuevos().size());
    }

    @Test
    void lasOperacionesExigenConceptoContratacionYFecha() {
        var wallet = conBono();
        var importe = new SkillCoins(10);

        assertThrows(IllegalArgumentException.class, () -> wallet.acreditar(importe, " ", CONTRATACION, AHORA));
        assertThrows(NullPointerException.class, () -> wallet.acreditar(importe, "Cobro", null, AHORA));
        assertThrows(NullPointerException.class, () -> wallet.debitar(importe, "Pago", CONTRATACION, null));
        assertThrows(NullPointerException.class, () -> wallet.acreditarDemo(importe, null));
        assertEquals(BONO, wallet.saldo());
        assertEquals(1, wallet.movimientosNuevos().size());
    }
}
