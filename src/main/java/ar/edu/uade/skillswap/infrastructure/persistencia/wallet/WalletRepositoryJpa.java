package ar.edu.uade.skillswap.infrastructure.persistencia.wallet;

import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.wallet.Movimiento;
import ar.edu.uade.skillswap.domain.wallet.Wallet;
import ar.edu.uade.skillswap.domain.wallet.WalletRepository;
import ar.edu.uade.skillswap.infrastructure.persistencia.UnidadDeTrabajoJpa;

import java.util.Objects;
import java.util.Optional;

/**
 * Repositorio de Wallets con JPA. Guarda el saldo y agrega solo los {@code movimientosNuevos()}: los
 * anteriores ya están en la tabla. Igual que en memoria, cada Wallet leída se guarda una sola vez.
 * La Wallet de garantía la crea la migración V1, porque es parte de la plataforma.
 */
public class WalletRepositoryJpa implements WalletRepository {

    private final UnidadDeTrabajoJpa unidadDeTrabajo;

    public WalletRepositoryJpa(UnidadDeTrabajoJpa unidadDeTrabajo) {
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
    }

    @Override
    public Optional<Wallet> findByTitular(String titularId) {
        return unidadDeTrabajo.conEntityManager(em -> Optional.ofNullable(em.find(WalletEntity.class, titularId))
                .map(e -> Wallet.reconstruir(e.titularId, new SkillCoins(e.saldo))));
    }

    @Override
    public Wallet garantia() {
        return findByTitular(Wallet.TITULAR_GARANTIA).orElseThrow(() -> new IllegalStateException(
                "No existe la Wallet de garantía: ¿se corrieron las migraciones de Flyway?"));
    }

    @Override
    public void save(Wallet wallet) {
        unidadDeTrabajo.conEntityManager(em -> {
            var entity = em.find(WalletEntity.class, wallet.titularId());
            if (entity == null) {
                entity = new WalletEntity(wallet.titularId());
                entity.saldo = wallet.saldo().valor();
                em.persist(entity);
            } else {
                entity.saldo = wallet.saldo().valor();
            }
            for (var movimiento : wallet.movimientosNuevos()) {
                em.persist(new MovimientoEntity(wallet.titularId(), movimiento));
            }
            return null;
        });
    }

    @Override
    public Pagina<Movimiento> findMovimientos(String titularId, PaginaSolicitada pagina) {
        return unidadDeTrabajo.conEntityManager(em -> {
            var movimientos = em.createQuery("select m from MovimientoEntity m where m.titularId = :titular"
                            + " order by m.fecha desc, m.secuencia desc", MovimientoEntity.class)
                    .setParameter("titular", titularId)
                    .setFirstResult(pagina.desde())
                    .setMaxResults(pagina.tamanio())
                    .getResultList();
            var total = em.createQuery("select count(m) from MovimientoEntity m where m.titularId = :titular",
                            Long.class)
                    .setParameter("titular", titularId)
                    .getSingleResult();
            return new Pagina<>(movimientos.stream().map(MovimientoEntity::aDominio).toList(), pagina.numero(),
                    pagina.tamanio(), total);
        });
    }
}
