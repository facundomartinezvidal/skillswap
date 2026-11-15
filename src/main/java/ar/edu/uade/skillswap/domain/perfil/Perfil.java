package ar.edu.uade.skillswap.domain.perfil;

import ar.edu.uade.skillswap.domain.perfil.exception.DisponibilidadInvalidaException;
import ar.edu.uade.skillswap.domain.perfil.exception.PerfilInactivoException;
import ar.edu.uade.skillswap.domain.shared.CambioEstado;
import ar.edu.uade.skillswap.domain.shared.Puntaje;
import ar.edu.uade.skillswap.domain.shared.TransicionInvalidaException;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Raíz del agregado Perfil. Se identifica por el {@code userId} del servicio de identidad.
 * La reputación solo cambia con {@link #registrarCalificacion}; la baja es lógica y queda en el
 * historial. Un perfil dado de baja no se edita, pero conserva sus operaciones históricas.
 */
public final class Perfil {

    private static final String ACTIVO = "ACTIVO";
    private static final String INACTIVO = "INACTIVO";

    private final String userId;
    private final TipoPerfil tipo;
    private String nombreVisible;
    private String biografia = "";
    private ZoneId zonaHoraria;
    private Set<String> habilidades = Set.of();
    private Set<String> intereses = Set.of();
    private final List<DisponibilidadSemanal> disponibilidad = new ArrayList<>();
    private CanalNotificacion canal = CanalNotificacion.INTERNA;
    private Reputacion reputacion = Reputacion.inicial();
    private boolean activo = true;
    private final List<CambioEstado> historial = new ArrayList<>();

    private Perfil(String userId, TipoPerfil tipo, String nombreVisible, ZoneId zonaHoraria) {
        this.userId = userId;
        this.tipo = tipo;
        this.nombreVisible = nombreVisible;
        this.zonaHoraria = zonaHoraria;
    }

    /** Crea el perfil activo, sin calificaciones y con el canal de notificación interno. */
    public static Perfil crear(String userId, TipoPerfil tipo, String nombreVisible, ZoneId zonaHoraria) {
        requireText(userId, "El usuario del perfil es obligatorio");
        Objects.requireNonNull(tipo, "El tipo de perfil es obligatorio");
        requireText(nombreVisible, "El nombre visible es obligatorio");
        Objects.requireNonNull(zonaHoraria, "La zona horaria es obligatoria");
        return new Perfil(userId, tipo, nombreVisible, zonaHoraria);
    }

    /** Reemplaza los datos básicos. La biografía es opcional. */
    public void editarDatos(String nombreVisible, String biografia, ZoneId zonaHoraria) {
        requireActivo("editar los datos");
        requireText(nombreVisible, "El nombre visible es obligatorio");
        Objects.requireNonNull(zonaHoraria, "La zona horaria es obligatoria");
        this.nombreVisible = nombreVisible;
        this.biografia = biografia == null ? "" : biografia;
        this.zonaHoraria = zonaHoraria;
    }

    public void definirHabilidades(Set<String> habilidades) {
        requireActivo("definir las habilidades");
        this.habilidades = copyOfTexts(habilidades, "habilidades");
    }

    public void definirIntereses(Set<String> intereses) {
        requireActivo("definir los intereses");
        this.intereses = copyOfTexts(intereses, "intereses");
    }

    public void agregarDisponibilidad(DayOfWeek dia, LocalTime inicio, LocalTime fin) {
        requireActivo("agregar disponibilidad");
        var nueva = new DisponibilidadSemanal(dia, inicio, fin);
        for (var existente : disponibilidad) {
            if (existente.seSolapaCon(nueva)) {
                throw new DisponibilidadInvalidaException("La disponibilidad se superpone con otra del mismo día");
            }
        }
        disponibilidad.add(nueva);
    }

    public void quitarDisponibilidad(DisponibilidadSemanal franja) {
        requireActivo("quitar disponibilidad");
        if (!disponibilidad.remove(franja)) {
            throw new DisponibilidadInvalidaException("La disponibilidad no pertenece a este perfil");
        }
    }

    public void elegirCanal(CanalNotificacion canal) {
        requireActivo("elegir el canal de notificación");
        this.canal = Objects.requireNonNull(canal, "El canal de notificación es obligatorio");
    }

    /**
     * Suma a la reputación el puntaje de una calificación que ya validó la contratación.
     * Se admite aunque el perfil esté dado de baja, porque las operaciones históricas se conservan.
     */
    public void registrarCalificacion(Puntaje puntaje) {
        this.reputacion = reputacion.registrar(puntaje);
    }

    /** Baja lógica: el perfil no se borra y la baja queda en el historial con actor y motivo. */
    public void darDeBaja(String actorId, String motivo, Instant ahora) {
        if (!activo) {
            throw new TransicionInvalidaException(INACTIVO, "dar de baja");
        }
        requireText(motivo, "Dar de baja un perfil requiere un motivo");
        historial.add(new CambioEstado(ACTIVO, INACTIVO, ahora, actorId, motivo));
        this.activo = false;
    }

    public boolean estaActivo() {
        return activo;
    }

    private void requireActivo(String accion) {
        if (!activo) {
            throw new PerfilInactivoException(userId, accion);
        }
    }

    private static Set<String> copyOfTexts(Set<String> values, String nombre) {
        Objects.requireNonNull(values, "El conjunto de " + nombre + " es obligatorio");
        for (var value : values) {
            requireText(value, "Ningún valor de " + nombre + " puede estar vacío");
        }
        return Set.copyOf(values);
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public String userId() {
        return userId;
    }

    public TipoPerfil tipo() {
        return tipo;
    }

    public String nombreVisible() {
        return nombreVisible;
    }

    public String biografia() {
        return biografia;
    }

    public ZoneId zonaHoraria() {
        return zonaHoraria;
    }

    public Set<String> habilidades() {
        return habilidades;
    }

    public Set<String> intereses() {
        return intereses;
    }

    public List<DisponibilidadSemanal> disponibilidad() {
        return List.copyOf(disponibilidad);
    }

    public CanalNotificacion canal() {
        return canal;
    }

    public Reputacion reputacion() {
        return reputacion;
    }

    public List<CambioEstado> historial() {
        return List.copyOf(historial);
    }
}
