package ar.edu.uade.skillswap.domain.wallet;

import java.util.Optional;

/** Puerto de persistencia de las Wallets. El listado paginado de movimientos se agrega en el hito de aplicación. */
public interface WalletRepository {

    Optional<Wallet> findByTitular(String titularId);

    /** La Wallet de garantía de la plataforma, donde quedan retenidos los pagos. */
    Wallet garantia();

    void save(Wallet wallet);
}
