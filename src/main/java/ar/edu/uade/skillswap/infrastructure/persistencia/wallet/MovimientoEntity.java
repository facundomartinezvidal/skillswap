package ar.edu.uade.skillswap.infrastructure.persistencia.wallet;

import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.wallet.Movimiento;
import ar.edu.uade.skillswap.domain.wallet.MovimientoId;
import ar.edu.uade.skillswap.domain.wallet.TipoMovimiento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Tabla {@code movimientos_wallet}. Un movimiento se inserta una vez y nunca se modifica. La restricción
 * única (contratación, titular, tipo) hace que la base rechace un doble cobro aunque el dominio fallara.
 */
@Entity
@Table(name = "movimientos_wallet")
public class MovimientoEntity {

    @Id
    String id;

    /** Orden de alta; desempata movimientos con la misma fecha al listar. Lo completa la base. */
    @Column(nullable = false, insertable = false, updatable = false)
    Long secuencia;

    @Column(name = "titular_id", nullable = false)
    String titularId;

    @Column(name = "contratacion_id")
    String contratacionId;

    @Column(nullable = false)
    String tipo;

    @Column(nullable = false)
    long importe;

    @Column(name = "saldo_resultante", nullable = false)
    long saldoResultante;

    @Column(nullable = false)
    String concepto;

    @Column(nullable = false)
    Instant fecha;

    protected MovimientoEntity() {
    }

    MovimientoEntity(String titularId, Movimiento m) {
        this.id = m.id().valor().toString();
        this.titularId = titularId;
        this.contratacionId = m.contratacionId() == null ? null : m.contratacionId().valor().toString();
        this.tipo = m.tipo().name();
        this.importe = m.importe().valor();
        this.saldoResultante = m.saldoResultante().valor();
        this.concepto = m.concepto();
        this.fecha = m.fecha();
    }

    Movimiento aDominio() {
        return new Movimiento(new MovimientoId(UUID.fromString(id)), TipoMovimiento.valueOf(tipo),
                new SkillCoins(importe), new SkillCoins(saldoResultante), concepto,
                contratacionId == null ? null : new ContratacionId(UUID.fromString(contratacionId)), fecha);
    }
}
