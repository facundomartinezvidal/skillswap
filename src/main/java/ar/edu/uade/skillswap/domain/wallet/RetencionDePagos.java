package ar.edu.uade.skillswap.domain.wallet;

import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;

import java.time.Instant;
import java.util.Objects;

/**
 * Servicio de dominio que mueve fondos entre Wallets a través de la Wallet de garantía.
 * Cada operación agrupa el débito con su crédito: valida todo, debita primero y solo después
 * acredita, así que si el débito falla ninguna Wallet cambia (regla 4).
 */
public final class RetencionDePagos {

    /** Pago: el precio pasa del cliente a la garantía. */
    public void retener(Wallet cliente, Wallet garantia, SkillCoins importe, ContratacionId contratacionId,
                        Instant ahora) {
        requireUsuario(cliente, "cliente");
        requireGarantia(garantia);
        cliente.debitar(importe, "Pago de la contratación", contratacionId, ahora);
        garantia.acreditar(importe, "Pago retenido de la contratación", contratacionId, ahora);
    }

    /** Completar: el precio retenido pasa de la garantía al proveedor. */
    public void liberarAlProveedor(Wallet garantia, Wallet proveedor, SkillCoins importe,
                                   ContratacionId contratacionId, Instant ahora) {
        requireGarantia(garantia);
        requireUsuario(proveedor, "proveedor");
        garantia.debitar(importe, "Liberación del pago retenido", contratacionId, ahora);
        proveedor.acreditar(importe, "Cobro de la contratación completada", contratacionId, ahora);
    }

    /**
     * Cancelar después del pago: la garantía entrega el total retenido, que se reparte entre el
     * reintegro al cliente y lo que cobra el proveedor. Un monto 0 no genera movimiento.
     */
    public void repartir(Wallet garantia, Wallet cliente, Wallet proveedor, SkillCoins retenido,
                         SkillCoins alCliente, SkillCoins alProveedor, ContratacionId contratacionId,
                         Instant ahora) {
        requireGarantia(garantia);
        requireUsuario(cliente, "cliente");
        requireUsuario(proveedor, "proveedor");
        Objects.requireNonNull(retenido, "El total retenido es obligatorio");
        Objects.requireNonNull(alCliente, "El monto para el cliente es obligatorio");
        Objects.requireNonNull(alProveedor, "El monto para el proveedor es obligatorio");
        if (!alCliente.sumar(alProveedor).equals(retenido)) {
            throw new IllegalArgumentException("El reparto (" + alCliente.valor() + " + " + alProveedor.valor()
                    + ") no coincide con el total retenido (" + retenido.valor() + ")");
        }
        garantia.debitar(retenido, "Reparto por cancelación", contratacionId, ahora);
        if (alCliente.valor() > 0) {
            cliente.reintegrar(alCliente, "Reintegro por cancelación", contratacionId, ahora);
        }
        if (alProveedor.valor() > 0) {
            proveedor.acreditar(alProveedor, "Cobro parcial por cancelación", contratacionId, ahora);
        }
    }

    private static void requireGarantia(Wallet garantia) {
        Objects.requireNonNull(garantia, "La Wallet de garantía es obligatoria");
        if (!garantia.esGarantia()) {
            throw new IllegalArgumentException("La Wallet " + garantia.titularId() + " no es la de garantía");
        }
    }

    private static void requireUsuario(Wallet wallet, String rol) {
        Objects.requireNonNull(wallet, "La Wallet del " + rol + " es obligatoria");
        if (wallet.esGarantia()) {
            throw new IllegalArgumentException("La Wallet del " + rol + " no puede ser la de garantía");
        }
    }
}
