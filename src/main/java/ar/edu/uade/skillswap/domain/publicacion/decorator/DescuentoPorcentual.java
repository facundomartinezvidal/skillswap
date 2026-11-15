package ar.edu.uade.skillswap.domain.publicacion.decorator;

import ar.edu.uade.skillswap.domain.shared.SkillCoins;

import java.util.Objects;

/** Descuenta un porcentaje de 1 a 100. El descuento se redondea hacia abajo. */
public record DescuentoPorcentual(int porcentaje) implements Promocion {

    public DescuentoPorcentual {
        if (porcentaje < 1 || porcentaje > 100) {
            throw new IllegalArgumentException("El porcentaje de descuento debe estar entre 1 y 100: " + porcentaje);
        }
    }

    @Override
    public SkillCoins aplicar(SkillCoins precio) {
        Objects.requireNonNull(precio, "El precio es obligatorio");
        long descuento = Math.multiplyExact(precio.valor(), porcentaje) / 100;
        return new SkillCoins(precio.valor() - descuento);
    }
}
