package ar.edu.uade.skillswap.domain.wallet;

import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.wallet.exception.SaldoInsuficienteException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class RetencionDePagosTest {

    private static final Instant AHORA = Instant.parse("2026-11-14T12:00:00Z");
    private static final SkillCoins PRECIO = new SkillCoins(100);
    private static final long SALDO_CLIENTE = 2500;
    private static final long SALDO_PROVEEDOR = 300;

    private final RetencionDePagos retencion = new RetencionDePagos();
    private final ContratacionId contratacion = ContratacionId.generate();
    private Wallet cliente;
    private Wallet proveedor;
    private Wallet garantia;
    private long totalInicial;

    @BeforeEach
    void abrirWallets() {
        cliente = Wallet.abrir("user-mateo", new SkillCoins(SALDO_CLIENTE), AHORA);
        proveedor = Wallet.abrir("user-ana", new SkillCoins(SALDO_PROVEEDOR), AHORA);
        garantia = Wallet.abrirGarantia();
        totalInicial = total();
    }

    private long total() {
        return Stream.of(cliente, proveedor, garantia).mapToLong(wallet -> wallet.saldo().valor()).sum();
    }

    /** Saldo y cantidad de movimientos de cada Wallet, para comprobar que una operación rechazada no cambió nada. */
    private List<Long> foto() {
        return Stream.of(cliente, proveedor, garantia)
                .flatMap(wallet -> Stream.of(wallet.saldo().valor(), (long) wallet.movimientosNuevos().size()))
                .toList();
    }

    private static Movimiento ultimo(Wallet wallet) {
        var movimientos = wallet.movimientosNuevos();
        return movimientos.get(movimientos.size() - 1);
    }

    private void retenerPrecio() {
        retencion.retener(cliente, garantia, PRECIO, contratacion, AHORA);
    }

    @Test
    void retenerPasaElPrecioDelClienteALaGarantia() {
        retenerPrecio();

        assertEquals(new SkillCoins(SALDO_CLIENTE - 100), cliente.saldo());
        assertEquals(PRECIO, garantia.saldo());
        assertEquals(new SkillCoins(SALDO_PROVEEDOR), proveedor.saldo());
        assertEquals(TipoMovimiento.DEBITO, ultimo(cliente).tipo());
        assertEquals(TipoMovimiento.CREDITO, ultimo(garantia).tipo());
        assertEquals(contratacion, ultimo(cliente).contratacionId());
        assertEquals(contratacion, ultimo(garantia).contratacionId());
        assertEquals(totalInicial, total());
    }

    @Test
    void retenerConSaldoInsuficienteNoCambiaNingunaWallet() {
        var antes = foto();

        assertThrows(SaldoInsuficienteException.class,
                () -> retencion.retener(cliente, garantia, new SkillCoins(SALDO_CLIENTE + 1), contratacion, AHORA));

        assertEquals(antes, foto());
        assertEquals(totalInicial, total());
    }

    @Test
    void liberarAlProveedorDevuelveLaGarantiaASuSaldoAnteriorYPagaAlProveedor() {
        var garantiaAntes = garantia.saldo();
        retenerPrecio();

        retencion.liberarAlProveedor(garantia, proveedor, PRECIO, contratacion, AHORA);

        assertEquals(garantiaAntes, garantia.saldo());
        assertEquals(new SkillCoins(SALDO_PROVEEDOR + 100), proveedor.saldo());
        assertEquals(new SkillCoins(SALDO_CLIENTE - 100), cliente.saldo());
        assertEquals(TipoMovimiento.DEBITO, ultimo(garantia).tipo());
        assertEquals(TipoMovimiento.CREDITO, ultimo(proveedor).tipo());
        assertEquals(totalInicial, total());
    }

    @Test
    void liberarMasDeLoRetenidoNoCambiaNingunaWallet() {
        retenerPrecio();
        var antes = foto();

        assertThrows(SaldoInsuficienteException.class,
                () -> retencion.liberarAlProveedor(garantia, proveedor, new SkillCoins(101), contratacion, AHORA));

        assertEquals(antes, foto());
        assertEquals(totalInicial, total());
    }

    @ParameterizedTest(name = "{0} al cliente y {1} al proveedor")
    @CsvSource({"50, 50", "100, 0", "0, 100"})
    void repartirDejaLosSaldosCuadradosYNoCreaMovimientosDeCero(long alCliente, long alProveedor) {
        retenerPrecio();
        var movimientosCliente = cliente.movimientosNuevos().size();
        var movimientosProveedor = proveedor.movimientosNuevos().size();

        retencion.repartir(garantia, cliente, proveedor, PRECIO, new SkillCoins(alCliente),
                new SkillCoins(alProveedor), contratacion, AHORA);

        assertEquals(new SkillCoins(0), garantia.saldo());
        assertEquals(new SkillCoins(SALDO_CLIENTE - 100 + alCliente), cliente.saldo());
        assertEquals(new SkillCoins(SALDO_PROVEEDOR + alProveedor), proveedor.saldo());
        assertEquals(TipoMovimiento.DEBITO, ultimo(garantia).tipo());
        assertEquals(PRECIO, ultimo(garantia).importe());
        assertEquals(movimientosCliente + (alCliente > 0 ? 1 : 0), cliente.movimientosNuevos().size());
        assertEquals(movimientosProveedor + (alProveedor > 0 ? 1 : 0), proveedor.movimientosNuevos().size());
        if (alCliente > 0) {
            assertEquals(TipoMovimiento.REINTEGRO, ultimo(cliente).tipo());
        }
        if (alProveedor > 0) {
            assertEquals(TipoMovimiento.CREDITO, ultimo(proveedor).tipo());
        }
        Stream.of(cliente, proveedor, garantia)
                .flatMap(wallet -> wallet.movimientosNuevos().stream())
                .forEach(movimiento -> assertTrue(movimiento.importe().valor() > 0));
        assertEquals(totalInicial, total());
    }

    @ParameterizedTest(name = "{0} + {1} no suma el total retenido")
    @CsvSource({"50, 40", "100, 1", "0, 0"})
    void repartirMontosQueNoSumanElTotalSeRechazaSinModificarNada(long alCliente, long alProveedor) {
        retenerPrecio();
        var antes = foto();

        assertThrows(IllegalArgumentException.class, () -> retencion.repartir(garantia, cliente, proveedor, PRECIO,
                new SkillCoins(alCliente), new SkillCoins(alProveedor), contratacion, AHORA));

        assertEquals(antes, foto());
        assertEquals(totalInicial, total());
    }

    @Test
    void repartirMasDeLoQueTieneLaGarantiaNoCambiaNingunaWallet() {
        retenerPrecio();
        var antes = foto();

        assertThrows(SaldoInsuficienteException.class, () -> retencion.repartir(garantia, cliente, proveedor,
                new SkillCoins(200), new SkillCoins(100), new SkillCoins(100), contratacion, AHORA));

        assertEquals(antes, foto());
    }

    @Test
    void cadaWalletTieneUnSoloMovimientoDeCadaTipoPorContratacion() {
        retenerPrecio();
        retencion.repartir(garantia, cliente, proveedor, PRECIO, new SkillCoins(50), new SkillCoins(50),
                contratacion, AHORA);

        for (var wallet : List.of(cliente, proveedor, garantia)) {
            var tipos = wallet.movimientosNuevos().stream()
                    .filter(movimiento -> contratacion.equals(movimiento.contratacionId()))
                    .map(Movimiento::tipo)
                    .toList();
            assertEquals(tipos.stream().distinct().count(), tipos.size(), wallet.titularId());
        }
    }

    @Test
    void exigeLaGarantiaDondeCorrespondeYUnUsuarioDondeNo() {
        var otraWallet = Wallet.abrir("user-otro", new SkillCoins(1000), AHORA);
        var antes = foto();

        assertThrows(IllegalArgumentException.class,
                () -> retencion.retener(cliente, otraWallet, PRECIO, contratacion, AHORA));
        assertThrows(IllegalArgumentException.class,
                () -> retencion.retener(garantia, garantia, PRECIO, contratacion, AHORA));
        assertThrows(IllegalArgumentException.class,
                () -> retencion.liberarAlProveedor(otraWallet, proveedor, PRECIO, contratacion, AHORA));
        assertThrows(IllegalArgumentException.class, () -> retencion.repartir(garantia, garantia, proveedor,
                PRECIO, PRECIO, new SkillCoins(0), contratacion, AHORA));

        assertEquals(antes, foto());
        assertEquals(new SkillCoins(1000), otraWallet.saldo());
    }

    @Test
    void elTotalSeConservaEnElCicloCompletoDePagoYCobro() {
        retenerPrecio();
        assertEquals(totalInicial, total());

        retencion.liberarAlProveedor(garantia, proveedor, PRECIO, contratacion, AHORA);
        assertEquals(totalInicial, total());

        var otraContratacion = ContratacionId.generate();
        retencion.retener(cliente, garantia, new SkillCoins(300), otraContratacion, AHORA);
        retencion.repartir(garantia, cliente, proveedor, new SkillCoins(300), new SkillCoins(150),
                new SkillCoins(150), otraContratacion, AHORA);
        assertEquals(totalInicial, total());
        assertEquals(new SkillCoins(0), garantia.saldo());
    }
}
