package ar.edu.uade.skillswap.application.wallet;

import ar.edu.uade.skillswap.application.wallet.dto.MovimientoDTO;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.domain.wallet.WalletRepository;

import java.time.Clock;
import java.util.Objects;

/** Consulta de saldo y movimientos, y carga de saldo de demostración. Lo usa el {@code WalletController}. */
public class WalletService {

    private final WalletRepository walletRepository;
    private final Clock clock;

    public WalletService(WalletRepository walletRepository, Clock clock) {
        this.walletRepository = Objects.requireNonNull(walletRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public long consultarSaldo(String userId) {
        throw pendiente();
    }

    public Pagina<MovimientoDTO> listarMovimientos(String userId, PaginaSolicitada pagina) {
        throw pendiente();
    }

    /** Devuelve el saldo resultante. */
    public long acreditarDemo(String userId, long importe) {
        throw pendiente();
    }

    private static UnsupportedOperationException pendiente() {
        return new UnsupportedOperationException("Pendiente: se implementa en un PR siguiente");
    }
}
