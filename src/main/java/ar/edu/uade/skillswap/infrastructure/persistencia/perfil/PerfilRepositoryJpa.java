package ar.edu.uade.skillswap.infrastructure.persistencia.perfil;

import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.perfil.PerfilRepository;
import ar.edu.uade.skillswap.infrastructure.persistencia.UnidadDeTrabajoJpa;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Repositorio de perfiles con JPA. Fetch plan de {@link #findByUserIds}: una consulta con
 * {@code join fetch} por colección (habilidades, intereses, disponibilidad, historial) para todo el lote,
 * así listar los autores de una página son 4 consultas y no una por autor.
 */
public class PerfilRepositoryJpa implements PerfilRepository {

    private final UnidadDeTrabajoJpa unidadDeTrabajo;

    public PerfilRepositoryJpa(UnidadDeTrabajoJpa unidadDeTrabajo) {
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
    }

    @Override
    public Optional<Perfil> findByUserId(String userId) {
        return findByUserIds(List.of(userId)).stream().findFirst();
    }

    @Override
    public List<Perfil> findByUserIds(Collection<String> userIds) {
        var ids = userIds.stream().distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return unidadDeTrabajo.conEntityManager(em -> {
            var entidades = em.createQuery("select p from PerfilEntity p left join fetch p.habilidades"
                    + " where p.userId in :ids", PerfilEntity.class).setParameter("ids", ids).getResultList();
            em.createQuery("select p from PerfilEntity p left join fetch p.intereses where p.userId in :ids",
                    PerfilEntity.class).setParameter("ids", ids).getResultList();
            em.createQuery("select p from PerfilEntity p left join fetch p.disponibilidad where p.userId in :ids",
                    PerfilEntity.class).setParameter("ids", ids).getResultList();
            em.createQuery("select p from PerfilEntity p left join fetch p.historial where p.userId in :ids",
                    PerfilEntity.class).setParameter("ids", ids).getResultList();
            return entidades.stream().map(PerfilMapper::aDominio).toList();
        });
    }

    @Override
    public void save(Perfil perfil) {
        unidadDeTrabajo.conEntityManager(em -> {
            var entity = em.find(PerfilEntity.class, perfil.userId());
            if (entity == null) {
                entity = new PerfilEntity(perfil.userId());
                PerfilMapper.copiar(perfil, entity);
                em.persist(entity);
            } else {
                PerfilMapper.copiar(perfil, entity);
            }
            return null;
        });
    }
}
