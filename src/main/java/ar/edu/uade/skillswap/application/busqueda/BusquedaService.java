package ar.edu.uade.skillswap.application.busqueda;

import ar.edu.uade.skillswap.application.publicacion.PublicacionMapper;
import ar.edu.uade.skillswap.application.publicacion.dto.PublicacionDTO;
import ar.edu.uade.skillswap.application.shared.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.perfil.PerfilRepository;
import ar.edu.uade.skillswap.domain.publicacion.Publicacion;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionRepository;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Búsqueda y recomendaciones. Lo usan el {@code BusquedaController} y el {@code RecomendacionController}.
 * La estrategia de recomendación entra por constructor (Strategy): cambiarla no cambia este caso de uso.
 */
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

    /** Publicaciones PUBLICADAS que cumplen los filtros, ordenadas y paginadas. */
    public Pagina<PublicacionDTO> buscar(CriteriosBusqueda criterios, PaginaSolicitada pagina) {
        var encontradas = buscador.buscar(Objects.requireNonNull(criterios, "Los criterios son obligatorios"), pagina);
        var autores = autoresDe(encontradas.elementos());
        return encontradas.map(p -> PublicacionMapper.aDTO(p, autores.get(p.autorId())));
    }

    /** Recomienda hasta {@code cantidad} publicaciones de otros autores con la estrategia recibida. */
    public List<PublicacionDTO> recomendar(String userId, int cantidad) {
        if (cantidad < 1) {
            throw new IllegalArgumentException("La cantidad a recomendar tiene que ser al menos 1: " + cantidad);
        }
        var usuario = perfilRepository.findByUserId(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException("perfil", userId));
        var candidatas = publicacionRepository.findPublicadas().stream()
                .filter(publicacion -> !publicacion.autorId().equals(userId))
                .toList();
        var recomendadas = estrategia.recomendar(usuario, candidatas, cantidad);
        var autores = autoresDe(recomendadas);
        return recomendadas.stream().map(p -> PublicacionMapper.aDTO(p, autores.get(p.autorId()))).toList();
    }

    public List<String> categorias() {
        return publicacionRepository.categorias();
    }

    /** Los autores de todas las publicaciones de una vez, para no hacer una consulta por publicación. */
    private Map<String, Perfil> autoresDe(List<Publicacion> publicaciones) {
        return perfilRepository.findByUserIds(publicaciones.stream().map(Publicacion::autorId).toList()).stream()
                .collect(Collectors.toMap(Perfil::userId, Function.identity()));
    }
}
