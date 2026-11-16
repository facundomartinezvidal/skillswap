package ar.edu.uade.skillswap.infrastructure.persistencia.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Tabla {@code wallets}, identificada por el titular (también la de garantía, que no es de ningún perfil).
 * Los movimientos no son una colección de esta entidad: la Wallet del dominio solo agrega movimientos
 * nuevos, y el historial se lista paginado con una consulta aparte.
 */
@Entity
@Table(name = "wallets")
public class WalletEntity {

    @Id
    @Column(name = "titular_id")
    String titularId;

    @Column(nullable = false)
    long saldo;

    /** La garantía la tocan todos los pagos: dos pagos a la vez no pueden pisarse el saldo. */
    @Version
    long version;

    protected WalletEntity() {
    }

    WalletEntity(String titularId) {
        this.titularId = titularId;
    }
}
