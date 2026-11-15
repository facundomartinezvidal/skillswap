package ar.edu.uade.skillswap.domain.publicacion.decorator;

import ar.edu.uade.skillswap.domain.shared.SkillCoins;

/**
 * Regla de precio que el autor guarda en su publicación. Cada tipo calcula el
 * precio resultante sin bajar nunca de cero (regla 6). Agregar un tipo nuevo es
 * agregar un record que implemente esta interfaz.
 */
public sealed interface Promocion permits DescuentoPorcentual, DescuentoFijo {

    SkillCoins aplicar(SkillCoins precio);
}
