package ar.edu.uade.skillswap.application.wallet;

import ar.edu.uade.skillswap.application.shared.exception.PermisoDenegadoException;
import ar.edu.uade.skillswap.application.shared.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.wallet.TipoMovimiento;
import ar.edu.uade.skillswap.domain.wallet.Wallet;
import ar.edu.uade.skillswap.infrastructure.memoria.UnidadDeTrabajoEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.WalletRepositoryEnMemoria;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class WalletServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-16T12:00:00Z");

    private final UnidadDeTrabajoEnMemoria unidadDeTrabajo = new UnidadDeTrabajoEnMemoria();
    private final WalletRepositoryEnMemoria wallets = new WalletRepositoryEnMemoria(unidadDeTrabajo);
    private final WalletService service = new WalletService(wallets, unidadDeTrabajo, Clock.fixed(AHORA, ZoneOffset.UTC));

    @BeforeEach
    void abrirWallet() {
        wallets.save(Wallet.abrir("user-mateo", new SkillCoins(500), AHORA));
    }

    @Test
    void consultaElSaldo() {
        assertEquals(500, service.consultarSaldo("user-mateo"));
    }

    @Test
    void acreditarDemoSumaAlSaldoYQuedaComoMovimiento() {
        assertEquals(750, service.acreditarDemo("user-mateo", 250));

        assertEquals(750, service.consultarSaldo("user-mateo"));
        var movimientos = service.listarMovimientos("user-mateo", PaginaSolicitada.primera(10));
        assertEquals(2, movimientos.total());
        var ultimo = movimientos.elementos().get(0);
        assertEquals(TipoMovimiento.ACREDITACION_DEMO, ultimo.tipo());
        assertEquals(250, ultimo.importe());
        assertEquals(750, ultimo.saldoResultante());
        assertNull(ultimo.contratacionId());
    }

    @Test
    void losMovimientosSePaginanDelMasNuevoAlMasViejo() {
        service.acreditarDemo("user-mateo", 10);
        service.acreditarDemo("user-mateo", 20);

        var segunda = service.listarMovimientos("user-mateo", new PaginaSolicitada(1, 2));

        assertEquals(3, segunda.total());
        assertEquals(1, segunda.elementos().size());
        assertEquals(500, segunda.elementos().get(0).saldoResultante(), "el bono es el más viejo");
    }

    @Test
    void unImporteDeCeroONegativoSeRechazaSinCambiarElSaldo() {
        assertThrows(IllegalArgumentException.class, () -> service.acreditarDemo("user-mateo", 0));
        assertThrows(IllegalArgumentException.class, () -> service.acreditarDemo("user-mateo", -5));
        assertEquals(500, service.consultarSaldo("user-mateo"));
    }

    @Test
    void unaWalletInexistenteSeInformaComoNoEncontrada() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.consultarSaldo("user-nadie"));
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.listarMovimientos("user-nadie", PaginaSolicitada.primera(10)));
    }

    @Test
    void laWalletDeGarantiaNoSeOperaDesdeAca() {
        assertThrows(PermisoDenegadoException.class, () -> service.consultarSaldo(Wallet.TITULAR_GARANTIA));
        assertThrows(PermisoDenegadoException.class, () -> service.acreditarDemo(Wallet.TITULAR_GARANTIA, 100));
    }
}
