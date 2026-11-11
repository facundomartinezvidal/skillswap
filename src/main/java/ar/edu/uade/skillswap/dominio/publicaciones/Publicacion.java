package ar.edu.uade.skillswap.dominio.publicaciones;

import ar.edu.uade.skillswap.dominio.compartido.ReglaNegocioException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Publicacion {
    private final String id;
    private final String autorId;
    private final TipoPublicacion tipo;
    private final String titulo;
    private final String descripcion;
    private final String categoria;
    private final Modalidad modalidad;
    private final int duracionMinutos;
    private final int cupo;
    private final long precioSkillCoins;
    private final List<String> etiquetas;
    private final List<FranjaDisponibilidad> disponibilidad;
    private final EstadoPublicacion estado;
    private final List<HistorialEstadoPublicacion> historial;

    private Publicacion(
            String id,
            String autorId,
            TipoPublicacion tipo,
            String titulo,
            String descripcion,
            String categoria,
            Modalidad modalidad,
            int duracionMinutos,
            int cupo,
            long precioSkillCoins,
            List<String> etiquetas,
            List<FranjaDisponibilidad> disponibilidad,
            EstadoPublicacion estado,
            List<HistorialEstadoPublicacion> historial) {
        this.id = textoObligatorio(id, "El id de la publicación es obligatorio");
        this.autorId = textoObligatorio(autorId, "El autor es obligatorio");
        this.tipo = Objects.requireNonNull(tipo, "El tipo de publicación es obligatorio");
        this.titulo = textoOpcional(titulo);
        this.descripcion = textoOpcional(descripcion);
        this.categoria = textoOpcional(categoria);
        this.modalidad = Objects.requireNonNull(modalidad, "La modalidad es obligatoria");
        if (duracionMinutos <= 0) {
            throw new IllegalArgumentException("La duración debe ser mayor que cero");
        }
        if (cupo <= 0) {
            throw new IllegalArgumentException("El cupo debe ser mayor que cero");
        }
        if (precioSkillCoins < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        this.duracionMinutos = duracionMinutos;
        this.cupo = cupo;
        this.precioSkillCoins = precioSkillCoins;
        this.etiquetas = List.copyOf(Objects.requireNonNull(etiquetas, "Las etiquetas son obligatorias"));
        this.disponibilidad = List.copyOf(Objects.requireNonNull(disponibilidad, "La disponibilidad es obligatoria"));
        this.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
        this.historial = List.copyOf(Objects.requireNonNull(historial, "El historial es obligatorio"));
    }

    public static Publicacion crearBorrador(
            String id,
            String autorId,
            TipoPublicacion tipo,
            String titulo,
            String descripcion,
            String categoria,
            Modalidad modalidad,
            int duracionMinutos,
            int cupo,
            long precioSkillCoins,
            List<String> etiquetas) {
        return new Publicacion(id, autorId, tipo, titulo, descripcion, categoria, modalidad,
                duracionMinutos, cupo, precioSkillCoins, etiquetas, List.of(),
                EstadoPublicacion.BORRADOR, List.of());
    }

    public Publicacion agregarDisponibilidad(FranjaDisponibilidad franja) {
        exigirEstado(EstadoPublicacion.BORRADOR);
        Objects.requireNonNull(franja, "La franja es obligatoria");
        if (disponibilidad.contains(franja)) {
            throw new ReglaNegocioException("La franja ya está agregada");
        }
        List<FranjaDisponibilidad> nuevasFranjas = new ArrayList<>(disponibilidad);
        nuevasFranjas.add(franja);
        return copiar(estado, nuevasFranjas, historial);
    }

    public Publicacion cambiarEstado(EstadoPublicacion nuevoEstado, String actorId, String motivo, LocalDateTime fecha) {
        Objects.requireNonNull(nuevoEstado, "El nuevo estado es obligatorio");
        Objects.requireNonNull(fecha, "La fecha es obligatoria");
        String actor = textoObligatorio(actorId, "El actor es obligatorio");
        String razon = textoOpcional(motivo);

        if (!transicionPermitida(estado, nuevoEstado)) {
            throw new ReglaNegocioException("Transición inválida de " + estado + " a " + nuevoEstado);
        }
        if (nuevoEstado == EstadoPublicacion.PUBLICADA) {
            validarListaParaPublicar();
        }
        if (nuevoEstado == EstadoPublicacion.CANCELADA && razon.isBlank()) {
            throw new ReglaNegocioException("La cancelación requiere un motivo");
        }

        List<HistorialEstadoPublicacion> nuevoHistorial = new ArrayList<>(historial);
        nuevoHistorial.add(new HistorialEstadoPublicacion(estado, nuevoEstado, fecha, actor, razon));
        return copiar(nuevoEstado, disponibilidad, nuevoHistorial);
    }

    private boolean transicionPermitida(EstadoPublicacion actual, EstadoPublicacion destino) {
        return switch (actual) {
            case BORRADOR -> destino == EstadoPublicacion.PUBLICADA || destino == EstadoPublicacion.CANCELADA;
            case PUBLICADA -> destino == EstadoPublicacion.PAUSADA
                    || destino == EstadoPublicacion.CERRADA || destino == EstadoPublicacion.CANCELADA;
            case PAUSADA -> destino == EstadoPublicacion.PUBLICADA
                    || destino == EstadoPublicacion.CERRADA || destino == EstadoPublicacion.CANCELADA;
            case CERRADA, CANCELADA -> false;
        };
    }

    private void validarListaParaPublicar() {
        if (titulo.isBlank()) {
            throw new ReglaNegocioException("La publicación necesita un título");
        }
        if (categoria.isBlank()) {
            throw new ReglaNegocioException("La publicación necesita una categoría");
        }
        if (precioSkillCoins < 0) {
            throw new ReglaNegocioException("El precio no puede ser negativo");
        }
        if (disponibilidad.isEmpty()) {
            throw new ReglaNegocioException("La publicación necesita al menos una franja de disponibilidad");
        }
    }

    private void exigirEstado(EstadoPublicacion requerido) {
        if (estado != requerido) {
            throw new ReglaNegocioException("La operación requiere el estado " + requerido);
        }
    }

    private Publicacion copiar(
            EstadoPublicacion nuevoEstado,
            List<FranjaDisponibilidad> nuevasFranjas,
            List<HistorialEstadoPublicacion> nuevoHistorial) {
        return new Publicacion(id, autorId, tipo, titulo, descripcion, categoria, modalidad,
                duracionMinutos, cupo, precioSkillCoins, etiquetas, nuevasFranjas,
                nuevoEstado, nuevoHistorial);
    }

    private static String textoObligatorio(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.trim();
    }

    private static String textoOpcional(String valor) {
        return valor == null ? "" : valor.trim();
    }

    public String id() { return id; }
    public String autorId() { return autorId; }
    public TipoPublicacion tipo() { return tipo; }
    public String titulo() { return titulo; }
    public String descripcion() { return descripcion; }
    public String categoria() { return categoria; }
    public Modalidad modalidad() { return modalidad; }
    public int duracionMinutos() { return duracionMinutos; }
    public int cupo() { return cupo; }
    public long precioSkillCoins() { return precioSkillCoins; }
    public List<String> etiquetas() { return etiquetas; }
    public List<FranjaDisponibilidad> disponibilidad() { return disponibilidad; }
    public EstadoPublicacion estado() { return estado; }
    public List<HistorialEstadoPublicacion> historial() { return historial; }
}
