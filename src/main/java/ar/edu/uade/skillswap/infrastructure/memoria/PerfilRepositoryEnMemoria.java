package ar.edu.uade.skillswap.infrastructure.memoria;

import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.perfil.PerfilRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Repositorio de perfiles en memoria, para tests y para la demo sin base de datos.
 * Guarda y devuelve copias, como una base. Los {@code save} pasan por la {@link UnidadDeTrabajoEnMemoria}.
 */
public class PerfilRepositoryEnMemoria implements PerfilRepository {

    private final Map<String, Perfil> perfiles = new HashMap<>();
    private final UnidadDeTrabajoEnMemoria unidadDeTrabajo;

    public PerfilRepositoryEnMemoria(UnidadDeTrabajoEnMemoria unidadDeTrabajo) {
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
    }

    @Override
    public Optional<Perfil> findByUserId(String userId) {
        return Optional.ofNullable(perfiles.get(userId)).map(PerfilRepositoryEnMemoria::copiar);
    }

    @Override
    public List<Perfil> findByUserIds(Collection<String> userIds) {
        return userIds.stream()
                .distinct()
                .map(perfiles::get)
                .filter(Objects::nonNull)
                .map(PerfilRepositoryEnMemoria::copiar)
                .toList();
    }

    @Override
    public void save(Perfil perfil) {
        var copia = copiar(perfil);
        unidadDeTrabajo.registrar(() -> perfiles.put(copia.userId(), copia));
    }

    private static Perfil copiar(Perfil p) {
        return Perfil.reconstruir(p.userId(), p.tipo(), p.nombreVisible(), p.biografia(), p.zonaHoraria(),
                p.habilidades(), p.intereses(), p.disponibilidad(), p.canal(), p.reputacion(), p.estaActivo(),
                p.historial());
    }
}
