package ar.edu.uade.skillswap.application.busqueda;

import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.publicacion.Publicacion;

import java.util.List;

/**
 * Patrón Strategy: cómo se eligen las publicaciones a recomendar. Hay dos intercambiables
 * (afinidad por habilidades y popularidad reciente); el {@link BusquedaService} no cambia al cambiarla.
 */
public interface EstrategiaRecomendacion {

    /** Elige hasta {@code cantidad} publicaciones de las candidatas, de la más a la menos recomendada. */
    List<Publicacion> recomendar(Perfil usuario, List<Publicacion> candidatas, int cantidad);
}
