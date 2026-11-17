package ar.edu.uade.skillswap.domain.perfil;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de los perfiles. Un usuario tiene un solo perfil, identificado por su userId. */
public interface PerfilRepository {

    Optional<Perfil> findByUserId(String userId);

    /** Varios perfiles de una vez (los que existan), para no hacer una consulta por autor al listar. */
    List<Perfil> findByUserIds(Collection<String> userIds);

    void save(Perfil perfil);
}
