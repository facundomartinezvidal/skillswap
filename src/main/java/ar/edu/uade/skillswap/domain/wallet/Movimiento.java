package ar.edu.uade.skillswap.domain.wallet;

import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;

import java.time.Instant;
import java.util.Objects;

/**
 * Registro inmutable de un cambio de saldo. El importe siempre es positivo; el signo lo da el tipo.
 * {@code contratacionId} es nulo en las acreditaciones de demostración.
 */
public record Movimiento(MovimientoId id, TipoMovimiento tipo, SkillCoins importe, SkillCoins saldoResultante,
                         String concepto, ContratacionId contratacionId, Instant fecha) {

    public Movimiento {
        Objects.requireNonNull(id, "El id del movimiento es obligatorio");
        Objects.requireNonNull(tipo, "El tipo de movimiento es obligatorio");
        Objects.requireNonNull(importe, "El importe del movimiento es obligatorio");
        if (importe.valor() == 0) {
            throw new IllegalArgumentException("El importe de un movimiento debe ser mayor que 0");
        }
        Objects.requireNonNull(saldoResultante, "El saldo resultante es obligatorio");
        if (concepto == null || concepto.isBlank()) {
            throw new IllegalArgumentException("El concepto del movimiento es obligatorio");
        }
        Objects.requireNonNull(fecha, "La fecha del movimiento es obligatoria");
    }

    /** Importe con signo: negativo para los débitos, positivo para el resto. */
    public long importeConSigno() {
        return tipo == TipoMovimiento.DEBITO ? -importe.valor() : importe.valor();
    }
}
