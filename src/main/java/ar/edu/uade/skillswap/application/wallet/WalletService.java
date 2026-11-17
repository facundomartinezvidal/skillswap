package ar.edu.uade.skillswap.application.wallet;

import ar.edu.uade.skillswap.application.shared.exception.PermisoDenegadoException;
import ar.edu.uade.skillswap.application.shared.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.application.wallet.dto.MovimientoDTO;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.wallet.Movimiento;
import ar.edu.uade.skillswap.domain.wallet.Wallet;
import ar.edu.uade.skillswap.domain.wallet.WalletRepository;

import java.time.Clock;
import java.util.Objects;

/**
 * Consulta de saldo y movimientos, y carga de saldo de demostración. Lo usa el {@code WalletController}.
 * Cada usuario opera solo su Wallet; la de garantía de la plataforma no se consulta ni se carga desde acá.
 */
public class WalletService {

    private final WalletRepository walletRepository;
    private final Clock clock;

    public WalletService(WalletRepository walletRepository, Clock clock) {
        this.walletRepository = Objects.requireNonNull(walletRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public long consultarSaldo(String userId) {
        return buscar(userId).saldo().valor();
    }

    /** Historial de movimientos, del más reciente al más viejo. */
    public Pagina<MovimientoDTO> listarMovimientos(String userId, PaginaSolicitada pagina) {
        buscar(userId);
        return walletRepository.findMovimientos(userId, pagina).map(WalletService::aDTO);
    }

    /** Carga saldo de demostración y devuelve el saldo resultante. El importe tiene que ser mayor que 0. */
    public long acreditarDemo(String userId, long importe) {
        var wallet = buscar(userId);
        wallet.acreditarDemo(new SkillCoins(importe), clock.instant());
        walletRepository.save(wallet);
        return wallet.saldo().valor();
    }

    private Wallet buscar(String userId) {
        if (Wallet.TITULAR_GARANTIA.equals(userId)) {
            throw new PermisoDenegadoException(userId, "operar la Wallet de garantía");
        }
        return walletRepository.findByTitular(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException("wallet", userId));
    }

    private static MovimientoDTO aDTO(Movimiento m) {
        var contratacionId = m.contratacionId() == null ? null : m.contratacionId().valor().toString();
        return new MovimientoDTO(m.id().valor().toString(), m.tipo(), m.importeConSigno(),
                m.saldoResultante().valor(), m.concepto(), contratacionId, m.fecha());
    }
}
