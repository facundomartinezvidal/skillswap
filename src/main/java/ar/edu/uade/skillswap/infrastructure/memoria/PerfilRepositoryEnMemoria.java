package ar.edu.uade.skillswap.infrastructure.memoria;

import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.perfil.PerfilRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Repositorio de perfiles en memoria, para tests y para la demo sin base de datos.
 * Guarda y devuelve copias, como una base.
 */
public class PerfilRepositoryEnMemoria implements PerfilRepository {

    private final Map<String, Perfil> perfiles = new HashMap<>();

    @Override
    public Optional<Perfil> findByUserId(String userId) {
        return Optional.ofNullable(perfiles.get(userId)).map(PerfilRepositoryEnMemoria::copiar);
    }

    @Override
    public void save(Perfil perfil) {
        perfiles.put(perfil.userId(), copiar(perfil));
    }

    private static Perfil copiar(Perfil p) {
        return Perfil.reconstruir(p.userId(), p.tipo(), p.nombreVisible(), p.biografia(), p.zonaHoraria(),
                p.habilidades(), p.intereses(), p.disponibilidad(), p.canal(), p.reputacion(), p.estaActivo(),
                p.historial());
    }
}
