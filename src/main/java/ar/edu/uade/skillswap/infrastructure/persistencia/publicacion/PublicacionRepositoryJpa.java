package ar.edu.uade.skillswap.infrastructure.persistencia.publicacion;

import ar.edu.uade.skillswap.domain.publicacion.Publicacion;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionId;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionRepository;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.infrastructure.persistencia.UnidadDeTrabajoJpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Repositorio de publicaciones con JPA. Usa el {@code EntityManager} de la transacción en curso
 * ({@link UnidadDeTrabajoJpa}), así lo que lee y guarda un caso de uso queda en la misma transacción.
 * <p>
 * Fetch plan: los listados primero buscan solo los ids de la página y después cargan esas
 * publicaciones completas con {@link #cargar}: una consulta con {@code join fetch} por colección, para
 * todo el lote de ids. Son siempre 4 consultas por lote, sin importar cuántas publicaciones haya (no hay
 * N+1). No se traen todas las colecciones en una sola consulta porque son varias listas: Hibernate no lo
 * permite (MultipleBagFetchException) y el producto cartesiano repetiría cada fila muchas veces.
 */
public class PublicacionRepositoryJpa implements PublicacionRepository {

    private static final String PUBLICADA = "PUBLICADA";

    private final UnidadDeTrabajoJpa unidadDeTrabajo;

    public PublicacionRepositoryJpa(UnidadDeTrabajoJpa unidadDeTrabajo) {
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
    }

    @Override
    public Optional<Publicacion> findById(PublicacionId id) {
        return findByIds(List.of(id)).stream().findFirst();
    }

    /**
     * Toda modificación sube la versión de la publicación, aunque solo cambien las reservas de una franja
     * (que están en otra tabla): el agregado es la unidad de consistencia. Si otra transacción guardó la
     * misma publicación desde que esta la leyó, el commit falla y no hay sobreventa del cupo.
     */
    @Override
    public void save(Publicacion publicacion) {
        unidadDeTrabajo.conEntityManager(em -> {
            var id = publicacion.id().valor().toString();
            var entity = em.find(PublicacionEntity.class, id);
            if (entity == null) {
                entity = new PublicacionEntity(id);
                PublicacionMapper.copiar(publicacion, entity);
                em.persist(entity);
            } else {
                PublicacionMapper.copiar(publicacion, entity);
                em.lock(entity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            }
            return null;
        });
    }

    @Override
    public Pagina<Publicacion> findByAutor(String autorId, PaginaSolicitada pagina) {
        return unidadDeTrabajo.conEntityManager(em -> {
            var ids = em.createQuery("select p.id from PublicacionEntity p where p.autorId = :autor"
                            + " order by p.secuencia", String.class)
                    .setParameter("autor", autorId)
                    .setFirstResult(pagina.desde())
                    .setMaxResults(pagina.tamanio())
                    .getResultList();
            var total = em.createQuery("select count(p) from PublicacionEntity p where p.autorId = :autor", Long.class)
                    .setParameter("autor", autorId)
                    .getSingleResult();
            return new Pagina<>(cargar(em, ids), pagina.numero(), pagina.tamanio(), total);
        });
    }

    @Override
    public List<Publicacion> findActivasByAutor(String autorId) {
        return unidadDeTrabajo.conEntityManager(em -> cargar(em, em.createQuery("select p.id from PublicacionEntity p"
                        + " where p.autorId = :autor and p.estado = :estado order by p.secuencia", String.class)
                .setParameter("autor", autorId)
                .setParameter("estado", PUBLICADA)
                .getResultList()));
    }

    @Override
    public List<Publicacion> findByIds(Collection<PublicacionId> ids) {
        var textos = ids.stream().map(id -> id.valor().toString()).distinct().toList();
        return unidadDeTrabajo.conEntityManager(em -> cargar(em, textos));
    }

    @Override
    public List<Publicacion> findPublicadas() {
        return unidadDeTrabajo.conEntityManager(em -> cargar(em, em.createQuery("select p.id from PublicacionEntity p"
                        + " where p.estado = :estado order by p.secuencia", String.class)
                .setParameter("estado", PUBLICADA)
                .getResultList()));
    }

    @Override
    public List<String> categorias() {
        return unidadDeTrabajo.conEntityManager(em -> em.createQuery("select distinct p.categoria"
                        + " from PublicacionEntity p where p.estado = :estado order by p.categoria", String.class)
                .setParameter("estado", PUBLICADA)
                .getResultList());
    }

    /**
     * Carga las publicaciones completas de un lote de ids, en el mismo orden de {@code ids}. Cada consulta
     * inicializa una colección de las mismas entidades (Hibernate las une en el contexto de persistencia).
     */
    static List<Publicacion> cargar(EntityManager em, List<String> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        var entidades = em.createQuery("select p from PublicacionEntity p left join fetch p.franjas"
                        + " where p.id in :ids", PublicacionEntity.class)
                .setParameter("ids", ids)
                .getResultList();
        em.createQuery("select p from PublicacionEntity p left join fetch p.promociones where p.id in :ids",
                PublicacionEntity.class).setParameter("ids", ids).getResultList();
        em.createQuery("select p from PublicacionEntity p left join fetch p.etiquetas where p.id in :ids",
                PublicacionEntity.class).setParameter("ids", ids).getResultList();
        em.createQuery("select p from PublicacionEntity p left join fetch p.historial where p.id in :ids",
                PublicacionEntity.class).setParameter("ids", ids).getResultList();
        return entidades.stream()
                .sorted(Comparator.comparingInt(p -> ids.indexOf(p.id)))
                .map(PublicacionMapper::aDominio)
                .toList();
    }
}
