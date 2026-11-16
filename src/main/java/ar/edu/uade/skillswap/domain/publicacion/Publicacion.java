package ar.edu.uade.skillswap.domain.publicacion;

import ar.edu.uade.skillswap.domain.publicacion.decorator.Precio;
import ar.edu.uade.skillswap.domain.publicacion.decorator.PrecioBase;
import ar.edu.uade.skillswap.domain.publicacion.decorator.PrecioConPromocion;
import ar.edu.uade.skillswap.domain.publicacion.decorator.Promocion;
import ar.edu.uade.skillswap.domain.publicacion.exception.CupoAgotadoException;
import ar.edu.uade.skillswap.domain.publicacion.exception.FranjaInvalidaException;
import ar.edu.uade.skillswap.domain.publicacion.exception.PublicacionIncompletaException;
import ar.edu.uade.skillswap.domain.publicacion.state.Borrador;
import ar.edu.uade.skillswap.domain.publicacion.state.EstadoPublicacion;
import ar.edu.uade.skillswap.domain.shared.CambioEstado;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.shared.TransicionInvalidaException;

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
    private final List<Promocion> promociones = new ArrayList<>();
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

    /**
     * Vuelve a armar una publicación guardada, en el estado en que quedó. Lo usan los repositorios
     * (JPA o en memoria). No vuelve a validar reglas: los datos ya las cumplieron al guardarse.
     */
    public static Publicacion reconstruir(PublicacionId id, String autorId, TipoPublicacion tipo, String titulo,
                                         String descripcion, String categoria, Modalidad modalidad,
                                         Duration duracionEstimada, int cupo, SkillCoins precioBase,
                                         Set<String> etiquetas, EstadoPublicacion estado, String motivoCancelacion,
                                         List<FranjaDisponibilidad> franjas, List<Promocion> promociones,
                                         List<CambioEstado> historial) {
        Objects.requireNonNull(id, "El id de la publicación es obligatorio");
        Objects.requireNonNull(autorId, "El autor de la publicación es obligatorio");
        Objects.requireNonNull(tipo, "El tipo de publicación es obligatorio");
        Objects.requireNonNull(estado, "El estado de la publicación es obligatorio");
        var publicacion = new Publicacion(id, autorId, tipo);
        publicacion.titulo = titulo;
        publicacion.descripcion = descripcion;
        publicacion.categoria = categoria;
        publicacion.modalidad = modalidad;
        publicacion.duracionEstimada = duracionEstimada;
        publicacion.cupo = cupo;
        publicacion.precioBase = precioBase;
        publicacion.etiquetas = etiquetas == null ? Set.of() : Set.copyOf(etiquetas);
        publicacion.estado = estado;
        publicacion.motivoCancelacion = motivoCancelacion;
        publicacion.franjas.addAll(franjas);
        publicacion.promociones.addAll(promociones);
        publicacion.historial.addAll(historial);
        return publicacion;
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

    /**
     * La franja elegida para solicitar o reprogramar. Falla si la publicación no admite solicitudes
     * (regla 2), si la franja no es de esta publicación o si ya no tiene lugar (regla 3).
     * No reserva: el cupo se toma recién cuando el proveedor acepta.
     */
    public FranjaDisponibilidad franjaDisponible(FranjaId franjaId) {
        if (!admiteSolicitudes()) {
            throw new TransicionInvalidaException(estado.nombre(), "solicitar");
        }
        var franja = findFranja(franjaId);
        if (franja.estaOcupada(cupo)) {
            throw new CupoAgotadoException(franjaId, cupo);
        }
        return franja;
    }

    /** Toma un lugar de la franja (el cupo es por franja). */
    public void reservarCupo(FranjaId franjaId) {
        if (!admiteSolicitudes()) {
            throw new TransicionInvalidaException(estado.nombre(), "reservar cupo");
        }
        findFranja(franjaId).reservar(cupo);
    }

    /** Devuelve un lugar de la franja. Nunca baja de cero. */
    public void liberarCupo(FranjaId franjaId) {
        findFranja(franjaId).liberar();
    }

    /** Agrega una promoción al final: se aplica después de las que ya estaban. Se pueden repetir. */
    public void agregarPromocion(Promocion promocion) {
        validarQueAdmitePromociones();
        Objects.requireNonNull(promocion, "La promoción es obligatoria");
        promociones.add(promocion);
    }

    /** Quita la primera promoción igual a la indicada; las demás conservan su orden. */
    public void quitarPromocion(Promocion promocion) {
        validarQueAdmitePromociones();
        Objects.requireNonNull(promocion, "La promoción es obligatoria");
        if (!promociones.remove(promocion)) {
            throw new IllegalArgumentException("La promoción no está en esta publicación");
        }
    }

    /**
     * Patrón Decorator: envuelve el precio base con una promoción por vez, en el orden
     * en que se agregaron. Nunca baja de cero porque cada promoción lo garantiza.
     */
    public SkillCoins precioFinal() {
        if (precioBase == null) {
            throw new IllegalStateException("La publicación todavía no tiene precio base");
        }
        Precio precio = new PrecioBase(precioBase);
        for (var promocion : promociones) {
            precio = new PrecioConPromocion(precio, promocion);
        }
        return precio.valor();
    }

    private List<String> requisitosFaltantesParaPublicar() {
        var faltantes = new ArrayList<String>();
        if (titulo == null || titulo.isBlank()) {
            faltantes.add("título");
        }
        if (categoria == null || categoria.isBlank()) {
            faltantes.add("categoría");
        }
        if (modalidad == null) {
            faltantes.add("modalidad");
        }
        if (duracionEstimada == null || duracionEstimada.isZero() || duracionEstimada.isNegative()) {
            faltantes.add("duración mayor a 0");
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

    private void validarQueAdmitePromociones() {
        if (!estado.admitePromociones()) {
            throw new TransicionInvalidaException(estado.nombre(), "modificar promociones");
        }
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

    public List<Promocion> promociones() {
        return List.copyOf(promociones);
    }

    public List<CambioEstado> historial() {
        return List.copyOf(historial);
    }
}
