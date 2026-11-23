package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.model.Models.Modalidad;
import ar.edu.uade.skillswap.model.Models.Publicacion;

import java.time.LocalDateTime;
import java.util.List;

/** Datos de la publicación que la UI de la cátedra no carga: cupo, franjas y promociones. */
public interface PublicacionOperaciones {

    /** Las publicaciones propias, en todos los estados, paginadas. */
    PaginaVista<Publicacion> listarPropias(String userId, int pagina, int tamanio);

    /**
     * El borrador nace con las franjas que elige el autor, cada una de la duración de la sesión. Más franjas se
     * agregan con {@link #agregarFranja} mientras siga en borrador.
     */
    Publicacion crearBorrador(String userId, String titulo, String descripcion, String categoria, Modalidad modalidad,
                              int duracion, int precio, int cupo, List<LocalDateTime> iniciosDeFranjas);

    /** Las franjas de la publicación, de la más próxima a la más lejana, para elegir una al solicitar. */
    List<FranjaVista> listarFranjas(String userId, String publicacionId);

    Publicacion agregarFranja(String userId, String publicacionId, LocalDateTime inicio, LocalDateTime fin);

    Publicacion agregarDescuentoPorcentual(String userId, String publicacionId, int porcentaje);

    Publicacion agregarDescuentoFijo(String userId, String publicacionId, int skillCoins);
}
