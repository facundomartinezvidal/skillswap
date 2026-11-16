package ar.edu.uade.skillswap.infrastructure.memoria;

import ar.edu.uade.skillswap.domain.publicacion.FranjaDisponibilidad;
import ar.edu.uade.skillswap.domain.publicacion.Publicacion;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionId;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionRepository;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repositorio de publicaciones en memoria, para tests y para la demo sin base de datos.
 * Guarda y devuelve copias, como una base: cambiar lo que devuelve {@code findById} no cambia
 * lo guardado hasta que se llama a {@code save}.
 */
public class PublicacionRepositoryEnMemoria implements PublicacionRepository {

    private final Map<PublicacionId, Publicacion> publicaciones = new LinkedHashMap<>();

    @Override
    public Optional<Publicacion> findById(PublicacionId id) {
        return Optional.ofNullable(publicaciones.get(id)).map(PublicacionRepositoryEnMemoria::copiar);
    }

    @Override
    public void save(Publicacion publicacion) {
        publicaciones.put(publicacion.id(), copiar(publicacion));
    }

    @Override
    public Pagina<Publicacion> findByAutor(String autorId, PaginaSolicitada pagina) {
        var delAutor = publicaciones.values().stream()
                .filter(publicacion -> publicacion.autorId().equals(autorId))
                .map(PublicacionRepositoryEnMemoria::copiar)
                .toList();
        return Pagina.de(delAutor, pagina);
    }

    @Override
    public List<Publicacion> findPublicadas() {
        return publicaciones.values().stream()
                .filter(Publicacion::admiteSolicitudes)
                .map(PublicacionRepositoryEnMemoria::copiar)
                .toList();
    }

    @Override
    public List<String> categorias() {
        return publicaciones.values().stream()
                .filter(Publicacion::admiteSolicitudes)
                .map(Publicacion::categoria)
                .distinct()
                .sorted()
                .toList();
    }

    private static Publicacion copiar(Publicacion p) {
        var franjas = p.franjas().stream()
                .map(f -> FranjaDisponibilidad.reconstruir(f.id(), f.inicio(), f.fin(), f.reservados()))
                .toList();
        return Publicacion.reconstruir(p.id(), p.autorId(), p.tipo(), p.titulo(), p.descripcion(), p.categoria(),
                p.modalidad(), p.duracionEstimada(), p.cupo(), p.precioBase(), p.etiquetas(), p.estado(),
                p.motivoCancelacion(), franjas, p.promociones(), p.historial());
    }
}
