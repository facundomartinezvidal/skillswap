package ar.edu.uade.skillswap.domain.shared;

import java.util.Objects;

public record SkillCoins(long valor) {

    public SkillCoins {
        if (valor < 0) {
            throw new IllegalArgumentException("Un monto en SkillCoins no puede ser negativo: " + valor);
        }
    }

    public SkillCoins sumar(SkillCoins otro) {
        Objects.requireNonNull(otro, "El monto a sumar es obligatorio");
        return new SkillCoins(Math.addExact(valor, otro.valor));
    }

    /** Un resultado negativo falla con {@link IllegalArgumentException}, igual que el constructor. */
    public SkillCoins restar(SkillCoins otro) {
        Objects.requireNonNull(otro, "El monto a restar es obligatorio");
        return new SkillCoins(valor - otro.valor);
    }

    public boolean esMenorQue(SkillCoins otro) {
        Objects.requireNonNull(otro, "El monto a comparar es obligatorio");
        return valor < otro.valor;
    }
}
