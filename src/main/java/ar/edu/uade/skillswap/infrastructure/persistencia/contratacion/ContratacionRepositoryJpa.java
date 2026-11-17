package ar.edu.uade.skillswap.infrastructure.persistencia.contratacion;

import ar.edu.uade.skillswap.domain.contratacion.Contratacion;
import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.contratacion.ContratacionRepository;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionId;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.infrastructure.persistencia.UnidadDeTrabajoJpa;

import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de contrataciones con JPA. Mismo fetch plan que las publicaciones: los listados buscan
 * primero los ids de la página y después cargan ese lote con una consulta {@code join fetch} por
 * colección (historial y calificaciones), sin N+1.
 */
public class ContratacionRepositoryJpa implements ContratacionRepository {

    private static final String ACEPTADA = "ACEPTADA";
    private static final String COMPLETADA = "COMPLETADA";

    private final UnidadDeTrabajoJpa unidadDeTrabajo;

    public ContratacionRepositoryJpa(UnidadDeTrabajoJpa unidadDeTrabajo) {
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
    }

    @Override
    public Optional<Contratacion> findById(ContratacionId id) {
        return unidadDeTrabajo.conEntityManager(em -> cargar(em, List.of(id.valor().toString())))
                .stream().findFirst();
    }

    @Override
    public void save(Contratacion contratacion) {
        unidadDeTrabajo.conEntityManager(em -> {
            var id = contratacion.id().valor().toString();
            var entity = em.find(ContratacionEntity.class, id);
            if (entity == null) {
                entity = new ContratacionEntity(id);
                ContratacionMapper.copiar(contratacion, entity);
                em.persist(entity);
            } else {
                ContratacionMapper.copiar(contratacion, entity);
            }
            return null;
        });
    }

    @Override
    public Pagina<Contratacion> findByParticipante(String userId, PaginaSolicitada pagina) {
        return unidadDeTrabajo.conEntityManager(em -> {
            var ids = em.createQuery("select c.id from ContratacionEntity c"
                            + " where c.clienteId = :usuario or c.proveedorId = :usuario"
                            + " order by c.secuencia desc", String.class)
                    .setParameter("usuario", userId)
                    .setFirstResult(pagina.desde())
                    .setMaxResults(pagina.tamanio())
                    .getResultList();
            var total = em.createQuery("select count(c) from ContratacionEntity c"
                            + " where c.clienteId = :usuario or c.proveedorId = :usuario", Long.class)
                    .setParameter("usuario", userId)
                    .getSingleResult();
            return new Pagina<>(cargar(em, ids), pagina.numero(), pagina.tamanio(), total);
        });
    }

    @Override
    public long countCompletadasDe(String userId) {
        return unidadDeTrabajo.conEntityManager(em -> em.createQuery("select count(c) from ContratacionEntity c"
                        + " where (c.clienteId = :usuario or c.proveedorId = :usuario) and c.estado = :estado",
                        Long.class)
                .setParameter("usuario", userId)
                .setParameter("estado", COMPLETADA)
                .getSingleResult());
    }

    /** Cuenta cada contratación una sola vez aunque su historial tuviera más de una aceptación. */
    @Override
    public Map<PublicacionId, Long> countAceptadasDesde(Instant desde) {
        return unidadDeTrabajo.conEntityManager(em -> {
            var filas = em.createQuery("select c.publicacionId, count(distinct c.id) from ContratacionEntity c"
                            + " join c.historial h where h.hacia = :estado and h.fecha >= :desde"
                            + " group by c.publicacionId", Object[].class)
                    .setParameter("estado", ACEPTADA)
                    .setParameter("desde", desde)
                    .getResultList();
            var cantidades = new HashMap<PublicacionId, Long>();
            for (var fila : filas) {
                cantidades.put(new PublicacionId(UUID.fromString((String) fila[0])), (Long) fila[1]);
            }
            return cantidades;
        });
    }

    private static List<Contratacion> cargar(EntityManager em, List<String> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        var entidades = em.createQuery("select c from ContratacionEntity c left join fetch c.historial"
                + " where c.id in :ids", ContratacionEntity.class).setParameter("ids", ids).getResultList();
        em.createQuery("select c from ContratacionEntity c left join fetch c.calificaciones where c.id in :ids",
                ContratacionEntity.class).setParameter("ids", ids).getResultList();
        return entidades.stream()
                .sorted(Comparator.comparingInt(c -> ids.indexOf(c.id)))
                .map(ContratacionMapper::aDominio)
                .toList();
    }
}
