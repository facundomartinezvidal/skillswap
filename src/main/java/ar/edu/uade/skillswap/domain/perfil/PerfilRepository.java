package ar.edu.uade.skillswap.domain.perfil;

import java.util.Optional;

/** Puerto de persistencia de los perfiles. Un usuario tiene un solo perfil, identificado por su userId. */
public interface PerfilRepository {

    Optional<Perfil> findByUserId(String userId);

    void save(Perfil perfil);
}
