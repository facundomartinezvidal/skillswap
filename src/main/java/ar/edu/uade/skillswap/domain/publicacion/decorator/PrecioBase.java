package ar.edu.uade.skillswap.domain.publicacion.decorator;

import ar.edu.uade.skillswap.domain.shared.SkillCoins;

import java.util.Objects;

/** Componente concreto: el precio que define el autor, sin promociones. */
public record PrecioBase(SkillCoins valor) implements Precio {

    public PrecioBase {
        Objects.requireNonNull(valor, "El precio base es obligatorio");
    }
}
