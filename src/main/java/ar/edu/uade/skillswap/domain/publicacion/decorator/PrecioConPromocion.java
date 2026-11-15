package ar.edu.uade.skillswap.domain.publicacion.decorator;

import ar.edu.uade.skillswap.domain.shared.SkillCoins;

import java.util.Objects;

/** Decorador: aplica una promoción sobre el valor del precio que envuelve. */
public final class PrecioConPromocion implements Precio {

    private final Precio envuelto;
    private final Promocion promocion;

    public PrecioConPromocion(Precio envuelto, Promocion promocion) {
        this.envuelto = Objects.requireNonNull(envuelto, "El precio a envolver es obligatorio");
        this.promocion = Objects.requireNonNull(promocion, "La promoción es obligatoria");
    }

    @Override
    public SkillCoins valor() {
        return promocion.aplicar(envuelto.valor());
    }
}
