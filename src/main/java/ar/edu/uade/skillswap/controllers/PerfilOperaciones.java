package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.domain.perfil.CanalNotificacion;
import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;
import ar.edu.uade.skillswap.model.Models.Perfil;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/** Lo que la consigna pide del perfil y la UI de la cátedra no tiene. Cada usuario opera solo su perfil. */
public interface PerfilOperaciones {

    Perfil crearPerfil(String userId, TipoPerfil tipo, String nombreVisible);

    /** Reemplaza todos los datos editables; un conjunto nulo queda vacío y la biografía es opcional. */
    Perfil editarPerfil(String userId, String nombreVisible, String biografia, ZoneId zonaHoraria,
                        Set<String> habilidades, Set<String> intereses);

    Perfil elegirCanal(String userId, CanalNotificacion canal);

    List<DisponibilidadVista> verDisponibilidad(String userId);

    /** Devuelve la disponibilidad semanal completa después del cambio. */
    List<DisponibilidadVista> agregarDisponibilidad(String userId, DayOfWeek dia, LocalTime inicio, LocalTime fin);

    List<DisponibilidadVista> quitarDisponibilidad(String userId, DayOfWeek dia, LocalTime inicio, LocalTime fin);

    /** Baja lógica: el perfil y su historial quedan, pero ya no se puede editar ni publicar. */
    void darDeBaja(String userId, String motivo);
}
