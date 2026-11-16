package ar.edu.uade.skillswap.application.busqueda.recomendacion;

import ar.edu.uade.skillswap.application.busqueda.EstrategiaRecomendacion;
import ar.edu.uade.skillswap.domain.contratacion.ContratacionRepository;
import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.publicacion.Publicacion;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Strategy de recomendación por popularidad reciente: las publicaciones con más contrataciones
 * aceptadas en la ventana de tiempo (por defecto, los últimos 30 días). No mira el perfil del usuario.
 * Las que no tuvieron ninguna en la ventana no se recomiendan.
 */
public class PopularidadReciente implements EstrategiaRecomendacion {

    public static final Duration VENTANA_POR_DEFECTO = Duration.ofDays(30);

    private final ContratacionRepository contratacionRepository;
    private final Clock clock;
    private final Duration ventana;

    public PopularidadReciente(ContratacionRepository contratacionRepository, Clock clock, Duration ventana) {
        this.contratacionRepository = Objects.requireNonNull(contratacionRepository);
        this.clock = Objects.requireNonNull(clock);
        this.ventana = Objects.requireNonNull(ventana);
    }

    public PopularidadReciente(ContratacionRepository contratacionRepository, Clock clock) {
        this(contratacionRepository, clock, VENTANA_POR_DEFECTO);
    }

    @Override
    public List<Publicacion> recomendar(Perfil usuario, List<Publicacion> candidatas, int cantidad) {
        var aceptadas = contratacionRepository.countAceptadasDesde(clock.instant().minus(ventana));
        return candidatas.stream()
                .filter(p -> aceptadas.getOrDefault(p.id(), 0L) > 0)
                .sorted(Comparator.comparing((Publicacion p) -> aceptadas.get(p.id())).reversed())
                .limit(cantidad)
                .toList();
    }
}
