package ar.edu.uade.skillswap.application.busqueda;

import ar.edu.uade.skillswap.application.publicacion.dto.PublicacionDTO;
import ar.edu.uade.skillswap.domain.perfil.PerfilRepository;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionRepository;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;

import java.util.List;
import java.util.Objects;

/** Búsqueda y recomendaciones. Lo usan el {@code BusquedaController} y el {@code RecomendacionController}. */
public class BusquedaService {

    private final BuscadorPublicaciones buscador;
    private final PublicacionRepository publicacionRepository;
    private final PerfilRepository perfilRepository;
    private final EstrategiaRecomendacion estrategia;

    public BusquedaService(BuscadorPublicaciones buscador, PublicacionRepository publicacionRepository,
                           PerfilRepository perfilRepository, EstrategiaRecomendacion estrategia) {
        this.buscador = Objects.requireNonNull(buscador);
        this.publicacionRepository = Objects.requireNonNull(publicacionRepository);
        this.perfilRepository = Objects.requireNonNull(perfilRepository);
        this.estrategia = Objects.requireNonNull(estrategia);
    }

    public Pagina<PublicacionDTO> buscar(CriteriosBusqueda criterios, PaginaSolicitada pagina) {
        throw pendiente();
    }

    /** Recomienda publicaciones de otros autores con la estrategia recibida por constructor. */
    public List<PublicacionDTO> recomendar(String userId, int cantidad) {
        throw pendiente();
    }

    public List<String> categorias() {
        throw pendiente();
    }

    private static UnsupportedOperationException pendiente() {
        return new UnsupportedOperationException("Pendiente: se implementa en un PR siguiente");
    }
}
