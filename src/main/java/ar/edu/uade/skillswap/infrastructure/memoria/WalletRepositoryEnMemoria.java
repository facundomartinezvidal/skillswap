package ar.edu.uade.skillswap.infrastructure.memoria;

import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.wallet.Movimiento;
import ar.edu.uade.skillswap.domain.wallet.Wallet;
import ar.edu.uade.skillswap.domain.wallet.WalletRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Repositorio de Wallets en memoria, para tests y para la demo sin base de datos.
 * Guarda el saldo de cada titular y agrega sus movimientos nuevos a un historial, como lo haría
 * la tabla de movimientos. Devuelve copias sin movimientos nuevos, igual que una Wallet recién leída.
 * Los {@code save} pasan por la {@link UnidadDeTrabajoEnMemoria}.
 */
public class WalletRepositoryEnMemoria implements WalletRepository {

    private final Map<String, SkillCoins> saldos = new HashMap<>();
    private final Map<String, List<Movimiento>> movimientos = new HashMap<>();
    private final UnidadDeTrabajoEnMemoria unidadDeTrabajo;

    public WalletRepositoryEnMemoria(UnidadDeTrabajoEnMemoria unidadDeTrabajo) {
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
        save(Wallet.abrirGarantia());
    }

    @Override
    public Optional<Wallet> findByTitular(String titularId) {
        return Optional.ofNullable(saldos.get(titularId)).map(saldo -> Wallet.reconstruir(titularId, saldo));
    }

    @Override
    public Wallet garantia() {
        return findByTitular(Wallet.TITULAR_GARANTIA).orElseThrow();
    }

    /** Guarda el saldo y agrega los movimientos nuevos. Cada Wallet leída se guarda una sola vez. */
    @Override
    public void save(Wallet wallet) {
        var titular = wallet.titularId();
        var saldo = wallet.saldo();
        var nuevos = wallet.movimientosNuevos();
        unidadDeTrabajo.registrar(() -> {
            saldos.put(titular, saldo);
            movimientos.computeIfAbsent(titular, t -> new ArrayList<>()).addAll(nuevos);
        });
    }

    @Override
    public Pagina<Movimiento> findMovimientos(String titularId, PaginaSolicitada pagina) {
        var delTitular = new ArrayList<>(movimientos.getOrDefault(titularId, List.of()));
        Collections.reverse(delTitular);
        return Pagina.de(delTitular, pagina);
    }
}
