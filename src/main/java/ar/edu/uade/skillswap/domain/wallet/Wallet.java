package ar.edu.uade.skillswap.domain.wallet;

import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.wallet.exception.SaldoInsuficienteException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Raíz del agregado Wallet. Se identifica por el {@code userId} del titular. El saldo nunca es
 * negativo y cada operación registra un {@link Movimiento} inmutable con el saldo resultante.
 * Solo guarda los movimientos creados desde que se cargó; el historial completo lo lista el repositorio.
 * Una operación rechazada no modifica nada.
 */
public final class Wallet {

    /** Titular de la Wallet de garantía de la plataforma, donde quedan retenidos los pagos. */
    public static final String TITULAR_GARANTIA = "skillswap-garantia";

    private static final String CONCEPTO_BONO = "Bono de bienvenida";
    private static final String CONCEPTO_DEMO = "Acreditación de demostración";

    private final String titularId;
    private SkillCoins saldo;
    private final List<Movimiento> movimientosNuevos = new ArrayList<>();

    private Wallet(String titularId, SkillCoins saldo) {
        this.titularId = titularId;
        this.saldo = saldo;
    }

    /** Abre la Wallet de un usuario con el bono de bienvenida; si el bono es mayor que 0 lo registra. */
    public static Wallet abrir(String titularId, SkillCoins bonoBienvenida, Instant ahora) {
        if (titularId == null || titularId.isBlank()) {
            throw new IllegalArgumentException("El titular de la Wallet es obligatorio");
        }
        if (TITULAR_GARANTIA.equals(titularId)) {
            throw new IllegalArgumentException("El titular " + TITULAR_GARANTIA + " está reservado para la garantía");
        }
        Objects.requireNonNull(bonoBienvenida, "El bono de bienvenida es obligatorio");
        Objects.requireNonNull(ahora, "El momento de la apertura es obligatorio");
        var wallet = new Wallet(titularId, new SkillCoins(0));
        if (bonoBienvenida.valor() > 0) {
            wallet.registrar(TipoMovimiento.ACREDITACION_DEMO, bonoBienvenida, CONCEPTO_BONO, null, ahora);
        }
        return wallet;
    }

    public static Wallet abrirGarantia() {
        return new Wallet(TITULAR_GARANTIA, new SkillCoins(0));
    }

    /**
     * Vuelve a armar una Wallet guardada (también la de garantía) con su saldo y sin movimientos nuevos:
     * los anteriores ya están en el repositorio. Lo usan los repositorios (JPA o en memoria).
     */
    public static Wallet reconstruir(String titularId, SkillCoins saldo) {
        if (titularId == null || titularId.isBlank()) {
            throw new IllegalArgumentException("El titular de la Wallet es obligatorio");
        }
        Objects.requireNonNull(saldo, "El saldo es obligatorio");
        return new Wallet(titularId, saldo);
    }

    /** Resta el importe; si el saldo no alcanza lanza {@link SaldoInsuficienteException} sin modificar nada. */
    public Movimiento debitar(SkillCoins importe, String concepto, ContratacionId contratacionId, Instant ahora) {
        validar(importe, concepto, contratacionId, ahora);
        if (saldo.esMenorQue(importe)) {
            throw new SaldoInsuficienteException(saldo, importe);
        }
        return registrar(TipoMovimiento.DEBITO, importe, concepto, contratacionId, ahora);
    }

    public Movimiento acreditar(SkillCoins importe, String concepto, ContratacionId contratacionId, Instant ahora) {
        validar(importe, concepto, contratacionId, ahora);
        return registrar(TipoMovimiento.CREDITO, importe, concepto, contratacionId, ahora);
    }

    public Movimiento reintegrar(SkillCoins importe, String concepto, ContratacionId contratacionId, Instant ahora) {
        validar(importe, concepto, contratacionId, ahora);
        return registrar(TipoMovimiento.REINTEGRO, importe, concepto, contratacionId, ahora);
    }

    /** Carga de saldo de demostración; no está asociada a una contratación. */
    public Movimiento acreditarDemo(SkillCoins importe, Instant ahora) {
        requirePositivo(importe);
        Objects.requireNonNull(ahora, "El momento del movimiento es obligatorio");
        return registrar(TipoMovimiento.ACREDITACION_DEMO, importe, CONCEPTO_DEMO, null, ahora);
    }

    public boolean esGarantia() {
        return TITULAR_GARANTIA.equals(titularId);
    }

    private Movimiento registrar(TipoMovimiento tipo, SkillCoins importe, String concepto,
                                 ContratacionId contratacionId, Instant ahora) {
        var resultante = tipo == TipoMovimiento.DEBITO ? saldo.restar(importe) : saldo.sumar(importe);
        var movimiento = new Movimiento(MovimientoId.generate(), tipo, importe, resultante, concepto,
                contratacionId, ahora);
        movimientosNuevos.add(movimiento);
        saldo = resultante;
        return movimiento;
    }

    private static void validar(SkillCoins importe, String concepto, ContratacionId contratacionId, Instant ahora) {
        requirePositivo(importe);
        if (concepto == null || concepto.isBlank()) {
            throw new IllegalArgumentException("El concepto del movimiento es obligatorio");
        }
        Objects.requireNonNull(contratacionId, "La contratación del movimiento es obligatoria");
        Objects.requireNonNull(ahora, "El momento del movimiento es obligatorio");
    }

    private static void requirePositivo(SkillCoins importe) {
        if (importe == null || importe.valor() == 0) {
            throw new IllegalArgumentException("El importe debe ser mayor que 0");
        }
    }

    public String titularId() {
        return titularId;
    }

    public SkillCoins saldo() {
        return saldo;
    }

    /** Movimientos creados desde que se abrió o cargó la Wallet, en orden. Lista inmodificable. */
    public List<Movimiento> movimientosNuevos() {
        return List.copyOf(movimientosNuevos);
    }
}
