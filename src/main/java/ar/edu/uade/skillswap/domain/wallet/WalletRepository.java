package ar.edu.uade.skillswap.domain.wallet;

import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;

import java.util.Optional;

/** Puerto de persistencia de las Wallets. Lo implementa la infraestructura. */
public interface WalletRepository {

    Optional<Wallet> findByTitular(String titularId);

    /** La Wallet de garantía de la plataforma, donde quedan retenidos los pagos. */
    Wallet garantia();

    /** Guarda el saldo y agrega los movimientos nuevos de la Wallet. */
    void save(Wallet wallet);

    /** Historial completo de movimientos del titular, del más reciente al más viejo. */
    Pagina<Movimiento> findMovimientos(String titularId, PaginaSolicitada pagina);
}
