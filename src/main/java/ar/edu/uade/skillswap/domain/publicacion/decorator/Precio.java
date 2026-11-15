package ar.edu.uade.skillswap.domain.publicacion.decorator;

import ar.edu.uade.skillswap.domain.shared.SkillCoins;

/**
 * Patrón Decorator: componente. Un precio puede ser el base o un precio anterior
 * envuelto por una promoción; quien lo consulta no distingue entre ambos.
 */
public interface Precio {

    SkillCoins valor();
}
