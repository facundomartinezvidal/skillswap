package ar.edu.uade.skillswap.aplicacion.puertos;

import ar.edu.uade.skillswap.dominio.publicaciones.Publicacion;

import java.util.Optional;

public interface RepositorioPublicaciones {
    void guardar(Publicacion publicacion);
    Optional<Publicacion> buscarPorId(String publicacionId);
}
