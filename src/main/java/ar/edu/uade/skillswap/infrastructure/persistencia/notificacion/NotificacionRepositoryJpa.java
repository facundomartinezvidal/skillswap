package ar.edu.uade.skillswap.infrastructure.persistencia.notificacion;

import ar.edu.uade.skillswap.domain.notificacion.Notificacion;
import ar.edu.uade.skillswap.domain.notificacion.NotificacionId;
import ar.edu.uade.skillswap.domain.notificacion.NotificacionRepository;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.infrastructure.persistencia.UnidadDeTrabajoJpa;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Bandeja de notificaciones con JPA. La notificación no tiene colecciones: cada listado es una consulta. */
public class NotificacionRepositoryJpa implements NotificacionRepository {

    private final UnidadDeTrabajoJpa unidadDeTrabajo;

    public NotificacionRepositoryJpa(UnidadDeTrabajoJpa unidadDeTrabajo) {
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
    }

    @Override
    public Optional<Notificacion> findById(NotificacionId id) {
        return unidadDeTrabajo.conEntityManager(em -> Optional.ofNullable(
                em.find(NotificacionEntity.class, id.valor().toString())).map(NotificacionEntity::aDominio));
    }

    @Override
    public void save(Notificacion notificacion) {
        unidadDeTrabajo.conEntityManager(em -> {
            var entity = em.find(NotificacionEntity.class, notificacion.id().valor().toString());
            if (entity == null) {
                entity = new NotificacionEntity(notificacion);
                em.persist(entity);
            }
            entity.leida = notificacion.estaLeida();
            return null;
        });
    }

    @Override
    public Pagina<Notificacion> findByDestinatario(String destinatarioId, PaginaSolicitada pagina) {
        return unidadDeTrabajo.conEntityManager(em -> {
            var notificaciones = em.createQuery("select n from NotificacionEntity n"
                            + " where n.destinatarioId = :destinatario order by n.fecha desc, n.secuencia desc",
                            NotificacionEntity.class)
                    .setParameter("destinatario", destinatarioId)
                    .setFirstResult(pagina.desde())
                    .setMaxResults(pagina.tamanio())
                    .getResultList();
            var total = em.createQuery("select count(n) from NotificacionEntity n"
                            + " where n.destinatarioId = :destinatario", Long.class)
                    .setParameter("destinatario", destinatarioId)
                    .getSingleResult();
            return new Pagina<>(notificaciones.stream().map(NotificacionEntity::aDominio).toList(), pagina.numero(),
                    pagina.tamanio(), total);
        });
    }

    @Override
    public List<Notificacion> findNoLeidas(String destinatarioId) {
        return unidadDeTrabajo.conEntityManager(em -> em.createQuery("select n from NotificacionEntity n"
                        + " where n.destinatarioId = :destinatario and n.leida = false order by n.secuencia",
                        NotificacionEntity.class)
                .setParameter("destinatario", destinatarioId)
                .getResultList()
                .stream().map(NotificacionEntity::aDominio).toList());
    }
}
