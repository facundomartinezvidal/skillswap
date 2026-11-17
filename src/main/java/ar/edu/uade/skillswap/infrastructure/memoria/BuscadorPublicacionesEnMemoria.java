package ar.edu.uade.skillswap.infrastructure.memoria;

import ar.edu.uade.skillswap.application.busqueda.BuscadorPublicaciones;
import ar.edu.uade.skillswap.application.busqueda.CriteriosBusqueda;
import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.perfil.PerfilRepository;
import ar.edu.uade.skillswap.domain.publicacion.Publicacion;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionRepository;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Búsqueda en memoria, para tests y para la demo sin base de datos. Filtra y ordena en Java lo que
 * con JPA sería una sola consulta con join a perfiles. Solo devuelve publicaciones PUBLICADAS.
 * <p>
 * Criterios: el texto se busca en título, descripción y etiquetas; la habilidad, en las etiquetas y en
 * las habilidades del autor; el rango de precio se aplica al precio final (con promociones), que es lo
 * que paga el cliente. Relevancia: primero las que tienen el texto en el título, después las más recientes.
 */
public class BuscadorPublicacionesEnMemoria implements BuscadorPublicaciones {

    private static final String PUBLICADA = "PUBLICADA";

    private final PublicacionRepository publicacionRepository;
    private final PerfilRepository perfilRepository;

    public BuscadorPublicacionesEnMemoria(PublicacionRepository publicacionRepository,
                                          PerfilRepository perfilRepository) {
        this.publicacionRepository = Objects.requireNonNull(publicacionRepository);
        this.perfilRepository = Objects.requireNonNull(perfilRepository);
    }

    @Override
    public Pagina<Publicacion> buscar(CriteriosBusqueda criterios, PaginaSolicitada pagina) {
        var publicadas = publicacionRepository.findPublicadas();
        var autores = perfilRepository.findByUserIds(publicadas.stream().map(Publicacion::autorId).toList())
                .stream().collect(Collectors.toMap(Perfil::userId, Function.identity()));
        var encontradas = publicadas.stream()
                .filter(p -> cumple(p, autores.get(p.autorId()), criterios))
                .sorted(orden(criterios, autores))
                .toList();
        return Pagina.de(encontradas, pagina);
    }

    private static boolean cumple(Publicacion p, Perfil autor, CriteriosBusqueda c) {
        return contiene(p, c.texto())
                && (c.categoria() == null || c.categoria().equalsIgnoreCase(p.categoria()))
                && (c.modalidad() == null || c.modalidad() == p.modalidad())
                && (c.precioMinimo() == null || p.precioFinal().valor() >= c.precioMinimo())
                && (c.precioMaximo() == null || p.precioFinal().valor() <= c.precioMaximo())
                && tieneHabilidad(p, autor, c.habilidad())
                && (c.reputacionMinima() == null || reputacion(autor) >= c.reputacionMinima());
    }

    private static boolean contiene(Publicacion p, String texto) {
        if (texto == null || texto.isBlank()) {
            return true;
        }
        var buscado = normalizar(texto);
        return normalizar(p.titulo()).contains(buscado)
                || normalizar(p.descripcion()).contains(buscado)
                || p.etiquetas().stream().anyMatch(etiqueta -> normalizar(etiqueta).contains(buscado));
    }

    private static boolean tieneHabilidad(Publicacion p, Perfil autor, String habilidad) {
        if (habilidad == null || habilidad.isBlank()) {
            return true;
        }
        var buscada = normalizar(habilidad);
        var habilidadesAutor = autor == null ? List.<String>of() : List.copyOf(autor.habilidades());
        return p.etiquetas().stream().anyMatch(e -> normalizar(e).equals(buscada))
                || habilidadesAutor.stream().anyMatch(h -> normalizar(h).equals(buscada));
    }

    private static Comparator<Publicacion> orden(CriteriosBusqueda c, Map<String, Perfil> autores) {
        Comparator<Publicacion> masRecientes = Comparator.comparing(BuscadorPublicacionesEnMemoria::fechaDePublicacion)
                .reversed();
        return switch (c.orden()) {
            case MEJOR_REPUTACION -> Comparator.<Publicacion>comparingDouble(p -> reputacion(autores.get(p.autorId())))
                    .reversed().thenComparing(masRecientes);
            case MENOR_PRECIO -> Comparator.<Publicacion>comparingLong(p -> p.precioFinal().valor())
                    .thenComparing(masRecientes);
            case MAS_RECIENTE -> masRecientes;
            case RELEVANCIA -> Comparator.<Publicacion, Boolean>comparing(p -> !tituloContiene(p, c.texto()))
                    .thenComparing(masRecientes);
        };
    }

    private static boolean tituloContiene(Publicacion p, String texto) {
        return texto != null && !texto.isBlank() && normalizar(p.titulo()).contains(normalizar(texto));
    }

    /** La última vez que pasó a PUBLICADA, según su historial. */
    private static Instant fechaDePublicacion(Publicacion p) {
        return p.historial().stream()
                .filter(cambio -> PUBLICADA.equals(cambio.hacia()))
                .map(cambio -> cambio.fecha())
                .max(Comparator.naturalOrder())
                .orElse(Instant.EPOCH);
    }

    private static double reputacion(Perfil autor) {
        return autor == null ? 0 : autor.reputacion().promedio();
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.toLowerCase(Locale.ROOT).trim();
    }
}
