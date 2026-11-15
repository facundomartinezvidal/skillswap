package ar.edu.uade.skillswap.domain.publicacion.decorator;

import ar.edu.uade.skillswap.domain.shared.SkillCoins;

import java.util.Objects;

/** Descuenta un monto fijo en SkillCoins. Si supera el precio, el resultado queda en cero. */
public record DescuentoFijo(SkillCoins monto) implements Promocion {

    public DescuentoFijo {
        Objects.requireNonNull(monto, "El monto del descuento es obligatorio");
        if (monto.valor() == 0) {
            throw new IllegalArgumentException("El monto del descuento debe ser mayor a 0");
        }
    }

    @Override
    public SkillCoins aplicar(SkillCoins precio) {
        Objects.requireNonNull(precio, "El precio es obligatorio");
        return new SkillCoins(Math.max(0, precio.valor() - monto.valor()));
    }
}
