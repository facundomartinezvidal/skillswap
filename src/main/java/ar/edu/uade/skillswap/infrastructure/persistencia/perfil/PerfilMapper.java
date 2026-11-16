package ar.edu.uade.skillswap.infrastructure.persistencia.perfil;

import ar.edu.uade.skillswap.domain.perfil.CanalNotificacion;
import ar.edu.uade.skillswap.domain.perfil.DisponibilidadSemanal;
import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.perfil.Reputacion;
import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;
import ar.edu.uade.skillswap.infrastructure.persistencia.shared.CambioEstadoEntity;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.Set;

/** Convierte entre {@link Perfil} (dominio) y {@link PerfilEntity} (tabla), en los dos sentidos. */
final class PerfilMapper {

    private PerfilMapper() {
    }

    /** Arma el agregado con {@code reconstruir}. Las colecciones de la entidad ya tienen que estar cargadas. */
    static Perfil aDominio(PerfilEntity e) {
        return Perfil.reconstruir(
                e.userId,
                TipoPerfil.valueOf(e.tipo),
                e.nombreVisible,
                e.biografia,
                ZoneId.of(e.zonaHoraria),
                e.habilidades,
                e.intereses,
                e.disponibilidad.stream().map(d -> new DisponibilidadSemanal(DayOfWeek.of(d.diaSemana), d.horaInicio,
                        d.horaFin)).toList(),
                CanalNotificacion.valueOf(e.canal),
                new Reputacion(e.reputacionSuma, e.reputacionCantidad),
                e.activo,
                e.historial.stream().map(CambioEstadoEntity::aDominio).toList());
    }

    /**
     * Copia el estado del agregado en la entidad (nueva o ya guardada). La disponibilidad se actualiza por
     * posición y del historial solo se agregan los cambios nuevos (solo crece).
     */
    static void copiar(Perfil p, PerfilEntity e) {
        e.tipo = p.tipo().name();
        e.nombreVisible = p.nombreVisible();
        e.biografia = p.biografia();
        e.zonaHoraria = p.zonaHoraria().getId();
        e.canal = p.canal().name();
        e.reputacionSuma = p.reputacion().sumaPuntajes();
        e.reputacionCantidad = p.reputacion().cantidad();
        e.activo = p.estaActivo();
        reemplazar(e.habilidades, p.habilidades());
        reemplazar(e.intereses, p.intereses());

        var disponibilidad = p.disponibilidad();
        for (int i = 0; i < disponibilidad.size(); i++) {
            if (i == e.disponibilidad.size()) {
                e.disponibilidad.add(new DisponibilidadEntity(e));
            }
            var fila = e.disponibilidad.get(i);
            fila.diaSemana = disponibilidad.get(i).dia().getValue();
            fila.horaInicio = disponibilidad.get(i).inicio();
            fila.horaFin = disponibilidad.get(i).fin();
        }
        while (e.disponibilidad.size() > disponibilidad.size()) {
            e.disponibilidad.remove(e.disponibilidad.size() - 1);
        }

        var historial = p.historial();
        for (int i = e.historial.size(); i < historial.size(); i++) {
            e.historial.add(new HistorialPerfilEntity(e, historial.get(i)));
        }
    }

    /** Solo toca la colección si cambió, para no reescribir las filas en cada {@code save}. */
    private static void reemplazar(Set<String> guardados, Set<String> nuevos) {
        if (!guardados.equals(nuevos)) {
            guardados.clear();
            guardados.addAll(nuevos);
        }
    }
}
