package ar.edu.uade.skillswap.infrastructure.persistencia.publicacion;

import ar.edu.uade.skillswap.domain.publicacion.FranjaDisponibilidad;
import ar.edu.uade.skillswap.domain.publicacion.FranjaId;
import ar.edu.uade.skillswap.domain.publicacion.Modalidad;
import ar.edu.uade.skillswap.domain.publicacion.Publicacion;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionId;
import ar.edu.uade.skillswap.domain.publicacion.TipoPublicacion;
import ar.edu.uade.skillswap.domain.publicacion.decorator.DescuentoFijo;
import ar.edu.uade.skillswap.domain.publicacion.decorator.DescuentoPorcentual;
import ar.edu.uade.skillswap.domain.publicacion.decorator.Promocion;
import ar.edu.uade.skillswap.domain.publicacion.state.Borrador;
import ar.edu.uade.skillswap.domain.publicacion.state.Cancelada;
import ar.edu.uade.skillswap.domain.publicacion.state.Cerrada;
import ar.edu.uade.skillswap.domain.publicacion.state.EstadoPublicacion;
import ar.edu.uade.skillswap.domain.publicacion.state.Pausada;
import ar.edu.uade.skillswap.domain.publicacion.state.Publicada;
import ar.edu.uade.skillswap.domain.shared.CambioEstado;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.infrastructure.persistencia.shared.CambioEstadoEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Convierte entre {@link Publicacion} (dominio) y {@link PublicacionEntity} (tabla), en los dos sentidos. */
final class PublicacionMapper {

    private static final String PUBLICADA = "PUBLICADA";

    private PublicacionMapper() {
    }

    /** Arma el agregado con {@code reconstruir}. Las colecciones de la entidad ya tienen que estar cargadas. */
    static Publicacion aDominio(PublicacionEntity e) {
        return Publicacion.reconstruir(
                new PublicacionId(UUID.fromString(e.id)),
                e.autorId,
                TipoPublicacion.valueOf(e.tipo),
                e.titulo,
                e.descripcion,
                e.categoria,
                e.modalidad == null ? null : Modalidad.valueOf(e.modalidad),
                e.duracionMinutos == null ? null : Duration.ofMinutes(e.duracionMinutos),
                e.cupo,
                e.precioBase == null ? null : new SkillCoins(e.precioBase),
                e.etiquetas,
                estado(e.estado),
                e.motivoCancelacion,
                e.franjas.stream().map(f -> FranjaDisponibilidad.reconstruir(new FranjaId(UUID.fromString(f.id)),
                        f.inicio, f.fin, f.reservados)).toList(),
                e.promociones.stream().map(PublicacionMapper::promocion).toList(),
                e.historial.stream().map(CambioEstadoEntity::aDominio).toList());
    }

    /**
     * Copia el estado del agregado en la entidad (nueva o ya guardada). Las colecciones se actualizan en
     * el lugar, sin borrar y volver a insertar todo: las franjas por id, las promociones por posición, y
     * del historial solo se agregan los cambios nuevos (solo crece).
     */
    static void copiar(Publicacion p, PublicacionEntity e) {
        e.autorId = p.autorId();
        e.tipo = p.tipo().name();
        e.titulo = p.titulo();
        e.descripcion = p.descripcion();
        e.categoria = p.categoria();
        e.modalidad = p.modalidad() == null ? null : p.modalidad().name();
        e.duracionMinutos = p.duracionEstimada() == null ? null : (int) p.duracionEstimada().toMinutes();
        e.cupo = p.cupo();
        e.precioBase = p.precioBase() == null ? null : p.precioBase().valor();
        e.precioFinal = p.precioBase() == null ? null : p.precioFinal().valor();
        e.publicadaEn = ultimaPublicacion(p);
        e.estado = p.estado().nombre();
        e.motivoCancelacion = p.motivoCancelacion();
        if (!e.etiquetas.equals(p.etiquetas())) {
            e.etiquetas.clear();
            e.etiquetas.addAll(p.etiquetas());
        }
        copiarFranjas(p, e);
        copiarPromociones(p, e);
        var historial = p.historial();
        for (int i = e.historial.size(); i < historial.size(); i++) {
            e.historial.add(new HistorialPublicacionEntity(e, historial.get(i)));
        }
    }

    private static void copiarFranjas(Publicacion p, PublicacionEntity e) {
        for (var franja : p.franjas()) {
            var id = franja.id().valor().toString();
            var guardada = e.franjas.stream().filter(f -> f.id.equals(id)).findFirst().orElse(null);
            if (guardada == null) {
                guardada = new FranjaEntity(id, e, franja.inicio(), franja.fin());
                e.franjas.add(guardada);
            }
            guardada.reservados = franja.reservados();
        }
    }

    /** Por posición: la fila i siempre tiene orden i, así nunca se repite (publicacion_id, orden). */
    private static void copiarPromociones(Publicacion p, PublicacionEntity e) {
        var promociones = p.promociones();
        for (int i = 0; i < promociones.size(); i++) {
            if (i == e.promociones.size()) {
                e.promociones.add(new PromocionEntity(e, i));
            }
            var fila = e.promociones.get(i);
            if (promociones.get(i) instanceof DescuentoPorcentual porcentual) {
                fila.tipo = PromocionEntity.PORCENTUAL;
                fila.valor = porcentual.porcentaje();
            } else if (promociones.get(i) instanceof DescuentoFijo fijo) {
                fila.tipo = PromocionEntity.FIJO;
                fila.valor = fijo.monto().valor();
            }
        }
        while (e.promociones.size() > promociones.size()) {
            e.promociones.remove(e.promociones.size() - 1);
        }
    }

    private static Promocion promocion(PromocionEntity fila) {
        return switch (fila.tipo) {
            case PromocionEntity.PORCENTUAL -> new DescuentoPorcentual((int) fila.valor);
            case PromocionEntity.FIJO -> new DescuentoFijo(new SkillCoins(fila.valor));
            default -> throw new IllegalStateException("Tipo de promoción desconocido: " + fila.tipo);
        };
    }

    static EstadoPublicacion estado(String nombre) {
        return switch (nombre) {
            case "BORRADOR" -> new Borrador();
            case "PUBLICADA" -> new Publicada();
            case "PAUSADA" -> new Pausada();
            case "CERRADA" -> new Cerrada();
            case "CANCELADA" -> new Cancelada();
            default -> throw new IllegalStateException("Estado de publicación desconocido: " + nombre);
        };
    }

    private static Instant ultimaPublicacion(Publicacion p) {
        Instant ultima = null;
        for (CambioEstado cambio : p.historial()) {
            if (PUBLICADA.equals(cambio.hacia()) && (ultima == null || cambio.fecha().isAfter(ultima))) {
                ultima = cambio.fecha();
            }
        }
        return ultima;
    }
}
