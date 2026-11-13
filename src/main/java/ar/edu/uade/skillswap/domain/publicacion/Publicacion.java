package ar.edu.uade.skillswap.domain.publicacion;

import ar.edu.uade.skillswap.domain.shared.SkillCoins;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Raíz del agregado Publicacion. Delega las transiciones en su {@link EstadoPublicacion}
 * y registra cada cambio válido en un historial que solo crece.
 */
public final class Publicacion {

    private final PublicacionId id;
    private final String autorId;
    private final TipoPublicacion tipo;
    private String titulo;
    private String descripcion;
    private String categoria;
    private Modalidad modalidad;
    private Duration duracionEstimada;
    private int cupo;
    private SkillCoins precioBase;
    private Set<String> etiquetas = Set.of();
    private EstadoPublicacion estado = new Borrador();
    private String motivoCancelacion;
    private final List<FranjaDisponibilidad> franjas = new ArrayList<>();
    private final List<CambioEstado> historial = new ArrayList<>();

    private Publicacion(PublicacionId id, String autorId, TipoPublicacion tipo) {
        this.id = id;
        this.autorId = autorId;
        this.tipo = tipo;
    }

    public static Publicacion crearBorrador(String autorId, TipoPublicacion tipo) {
        if (autorId == null || autorId.isBlank()) {
            throw new IllegalArgumentException("El autor de la publicación es obligatorio");
        }
        Objects.requireNonNull(tipo, "El tipo de publicación es obligatorio");
        return new Publicacion(PublicacionId.generate(), autorId, tipo);
    }

    /** Reemplaza el contenido editable. Solo en BORRADOR; los datos pueden quedar incompletos. */
    public void editar(String titulo, String descripcion, String categoria, Modalidad modalidad,
                       Duration duracionEstimada, int cupo, SkillCoins precioBase, Set<String> etiquetas) {
        estado.editar();
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.modalidad = modalidad;
        this.duracionEstimada = duracionEstimada;
        this.cupo = cupo;
        this.precioBase = precioBase;
        this.etiquetas = etiquetas == null ? Set.of() : Set.copyOf(etiquetas);
    }

    public FranjaId agregarFranja(Instant inicio, Instant fin) {
        estado.editar();
        var nueva = new FranjaDisponibilidad(FranjaId.generate(), inicio, fin);
        for (var existente : franjas) {
            if (existente.seSolapaCon(nueva)) {
                throw new FranjaInvalidaException("La franja se superpone con otra franja de la publicación");
            }
        }
        franjas.add(nueva);
        return nueva.id();
    }

    public void publicar(String actorId, Instant ahora) {
        var siguiente = estado.publicar();
        var faltantes = requisitosFaltantesParaPublicar();
        if (!faltantes.isEmpty()) {
            throw new PublicacionIncompletaException(faltantes);
        }
        transitionTo(siguiente, actorId, null, ahora);
    }

    public void pausar(String actorId, String motivo, Instant ahora) {
        transitionTo(estado.pausar(), actorId, motivo, ahora);
    }

    public void reanudar(String actorId, Instant ahora) {
        transitionTo(estado.reanudar(), actorId, null, ahora);
    }

    public void cerrar(String actorId, String motivo, Instant ahora) {
        transitionTo(estado.cerrar(), actorId, motivo, ahora);
    }

    public void cancelar(String actorId, String motivo, Instant ahora) {
        var siguiente = estado.cancelar();
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Cancelar una publicación requiere un motivo");
        }
        transitionTo(siguiente, actorId, motivo, ahora);
        this.motivoCancelacion = motivo;
    }

    public boolean admiteSolicitudes() {
        return estado.admiteSolicitudes();
    }

    /** Toma un lugar de la franja (el cupo es por franja). */
    public void reservarCupo(FranjaId franjaId) {
        if (!admiteSolicitudes()) {
            throw new TransicionInvalidaException(estado.nombre(), "reservar cupo en");
        }
        findFranja(franjaId).reservar(cupo);
    }

    /** Devuelve un lugar de la franja. Nunca baja de cero. */
    public void liberarCupo(FranjaId franjaId) {
        findFranja(franjaId).liberar();
    }

    private List<String> requisitosFaltantesParaPublicar() {
        var faltantes = new ArrayList<String>();
        if (titulo == null || titulo.isBlank()) {
            faltantes.add("título");
        }
        if (categoria == null || categoria.isBlank()) {
            faltantes.add("categoría");
        }
        if (precioBase == null) {
            faltantes.add("precio");
        }
        if (cupo < 1) {
            faltantes.add("cupo de al menos 1");
        }
        if (franjas.isEmpty()) {
            faltantes.add("al menos una franja de disponibilidad");
        }
        return faltantes;
    }

    private void transitionTo(EstadoPublicacion siguiente, String actorId, String motivo, Instant ahora) {
        var cambio = new CambioEstado(estado.nombre(), siguiente.nombre(), ahora, actorId, motivo);
        historial.add(cambio);
        estado = siguiente;
    }

    private FranjaDisponibilidad findFranja(FranjaId franjaId) {
        return franjas.stream()
                .filter(f -> f.id().equals(franjaId))
                .findFirst()
                .orElseThrow(() -> new FranjaInvalidaException("La franja no pertenece a esta publicación"));
    }

    public PublicacionId id() {
        return id;
    }

    public String autorId() {
        return autorId;
    }

    public TipoPublicacion tipo() {
        return tipo;
    }

    public String titulo() {
        return titulo;
    }

    public String descripcion() {
        return descripcion;
    }

    public String categoria() {
        return categoria;
    }

    public Modalidad modalidad() {
        return modalidad;
    }

    public Duration duracionEstimada() {
        return duracionEstimada;
    }

    public int cupo() {
        return cupo;
    }

    public SkillCoins precioBase() {
        return precioBase;
    }

    public Set<String> etiquetas() {
        return etiquetas;
    }

    public EstadoPublicacion estado() {
        return estado;
    }

    public String motivoCancelacion() {
        return motivoCancelacion;
    }

    public List<FranjaDisponibilidad> franjas() {
        return List.copyOf(franjas);
    }

    public List<CambioEstado> historial() {
        return List.copyOf(historial);
    }
}
