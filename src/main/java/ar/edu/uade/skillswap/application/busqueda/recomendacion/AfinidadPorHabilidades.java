package ar.edu.uade.skillswap.application.busqueda.recomendacion;

import ar.edu.uade.skillswap.application.busqueda.EstrategiaRecomendacion;
import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.perfil.PerfilRepository;
import ar.edu.uade.skillswap.domain.publicacion.Publicacion;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Strategy de recomendación por afinidad: cuántos intereses y habilidades del usuario coinciden con la
 * categoría y las etiquetas de la publicación y con las habilidades de su autor. Recomienda las de más
 * coincidencias; las que no tienen ninguna no se recomiendan.
 */
public class AfinidadPorHabilidades implements EstrategiaRecomendacion {

    private final PerfilRepository perfilRepository;

    public AfinidadPorHabilidades(PerfilRepository perfilRepository) {
        this.perfilRepository = Objects.requireNonNull(perfilRepository);
    }

    @Override
    public List<Publicacion> recomendar(Perfil usuario, List<Publicacion> candidatas, int cantidad) {
        var buscado = normalizar(usuario.intereses());
        buscado.addAll(normalizar(usuario.habilidades()));
        var autores = perfilRepository.findByUserIds(candidatas.stream().map(Publicacion::autorId).toList())
                .stream().collect(Collectors.toMap(Perfil::userId, Function.identity()));
        Map<Publicacion, Long> afinidad = candidatas.stream()
                .collect(Collectors.toMap(Function.identity(), p -> coincidencias(p, autores.get(p.autorId()), buscado)));
        return candidatas.stream()
                .filter(p -> afinidad.get(p) > 0)
                .sorted(Comparator.comparing(afinidad::get).reversed())
                .limit(cantidad)
                .toList();
    }

    private static long coincidencias(Publicacion p, Perfil autor, Set<String> buscado) {
        var ofrece = normalizar(p.etiquetas());
        if (p.categoria() != null) {
            ofrece.add(p.categoria().toLowerCase(Locale.ROOT));
        }
        if (autor != null) {
            ofrece.addAll(normalizar(autor.habilidades()));
        }
        return ofrece.stream().filter(buscado::contains).count();
    }

    private static Set<String> normalizar(Set<String> valores) {
        var normalizados = new HashSet<String>();
        valores.forEach(valor -> normalizados.add(valor.toLowerCase(Locale.ROOT).trim()));
        return normalizados;
    }
}
