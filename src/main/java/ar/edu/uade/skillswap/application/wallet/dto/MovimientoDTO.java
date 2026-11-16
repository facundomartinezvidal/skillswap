package ar.edu.uade.skillswap.application.wallet.dto;

import ar.edu.uade.skillswap.domain.wallet.TipoMovimiento;

import java.time.Instant;

/**
 * Un movimiento de la Wallet. {@code importe} tiene signo: negativo en los débitos.
 * {@code contratacionId} es nulo en las acreditaciones de demostración.
 */
public record MovimientoDTO(String id, TipoMovimiento tipo, long importe, long saldoResultante, String concepto,
                            String contratacionId, Instant fecha) {
}
