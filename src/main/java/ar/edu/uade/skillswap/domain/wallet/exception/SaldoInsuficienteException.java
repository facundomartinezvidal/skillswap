package ar.edu.uade.skillswap.domain.wallet.exception;

import ar.edu.uade.skillswap.domain.shared.DomainException;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;

/** Regla 4: el saldo nunca queda negativo. */
public class SaldoInsuficienteException extends DomainException {

    private final SkillCoins saldo;
    private final SkillCoins importe;

    public SaldoInsuficienteException(SkillCoins saldo, SkillCoins importe) {
        super("Saldo insuficiente: hay " + saldo.valor() + " SkillCoins y se necesitan " + importe.valor());
        this.saldo = saldo;
        this.importe = importe;
    }

    public SkillCoins saldo() {
        return saldo;
    }

    public SkillCoins importe() {
        return importe;
    }
}
