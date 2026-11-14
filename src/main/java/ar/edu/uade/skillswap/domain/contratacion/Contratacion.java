package ar.edu.uade.skillswap.domain.contratacion;

import ar.edu.uade.skillswap.domain.contratacion.exception.AccionNoPermitidaException;
import ar.edu.uade.skillswap.domain.contratacion.exception.AutoSolicitudException;
import ar.edu.uade.skillswap.domain.contratacion.exception.ReprogramacionInvalidaException;
import ar.edu.uade.skillswap.domain.contratacion.state.Confirmada;
import ar.edu.uade.skillswap.domain.contratacion.state.EstadoContratacion;
import ar.edu.uade.skillswap.domain.contratacion.state.Pendiente;
import ar.edu.uade.skillswap.domain.contratacion.strategy.PoliticaReintegro;
import ar.edu.uade.skillswap.domain.publicacion.FranjaId;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionId;
import ar.edu.uade.skillswap.domain.shared.CambioEstado;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.shared.TransicionInvalidaException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Raíz del agregado Contratacion. Valida el rol del actor, delega las transiciones en su
 * {@link EstadoContratacion} y registra cada cambio válido en un historial que solo crece.
 * Una operación rechazada no modifica nada.
 */
public final class Contratacion {

    private final ContratacionId id;
    private final PublicacionId publicacionId;
    private final String clienteId;
    private final String proveedorId;
    private final Solicitud solicitud;
    private FranjaId franjaId;
    private Instant inicioSesion;
    private final SkillCoins precioAcordado;
    private EstadoContratacion estado = new Pendiente();
    private PropuestaReprogramacion propuesta;
    private Reintegro reintegro;
    private final List<CambioEstado> historial = new ArrayList<>();

    private Contratacion(ContratacionId id, PublicacionId publicacionId, String clienteId, String proveedorId,
                         Solicitud solicitud, Instant inicioSesion, SkillCoins precioAcordado) {
        this.id = id;
        this.publicacionId = publicacionId;
        this.clienteId = clienteId;
        this.proveedorId = proveedorId;
        this.solicitud = solicitud;
        this.franjaId = solicitud.franjaElegida();
        this.inicioSesion = inicioSesion;
        this.precioAcordado = precioAcordado;
    }

    /** Crea la contratación en PENDIENTE. Igual que una publicación, la creación no se registra en el historial. */
    public static Contratacion solicitar(String clienteId, String proveedorId, PublicacionId publicacionId,
                                         Solicitud solicitud, Instant inicioSesion, SkillCoins precio,
                                         Instant ahora) {
        requireText(clienteId, "El cliente es obligatorio");
        requireText(proveedorId, "El proveedor es obligatorio");
        Objects.requireNonNull(publicacionId, "La publicación es obligatoria");
        Objects.requireNonNull(solicitud, "La solicitud es obligatoria");
        Objects.requireNonNull(inicioSesion, "El inicio de la sesión es obligatorio");
        Objects.requireNonNull(precio, "El precio acordado es obligatorio");
        Objects.requireNonNull(ahora, "El momento de la solicitud es obligatorio");
        if (clienteId.equals(proveedorId)) {
            throw new AutoSolicitudException(clienteId);
        }
        return new Contratacion(ContratacionId.generate(), publicacionId, clienteId, proveedorId,
                solicitud, inicioSesion, precio);
    }

    public void aceptar(String actorId, Instant ahora) {
        requireProveedor(actorId, "aceptar");
        transitionTo(estado.aceptar(), actorId, null, ahora);
    }

    public void rechazar(String actorId, String motivo, Instant ahora) {
        requireProveedor(actorId, "rechazar");
        transitionTo(estado.rechazar(), actorId, motivo, ahora);
    }

    /** Solo desde ACEPTADA: un segundo pago encuentra la contratación CONFIRMADA y se rechaza. */
    public void pagar(String actorId, Instant ahora) {
        requireCliente(actorId, "pagar");
        transitionTo(estado.pagar(), actorId, null, ahora);
    }

    public void iniciar(String actorId, Instant ahora) {
        requireProveedor(actorId, "iniciar");
        transitionTo(estado.iniciar(), actorId, null, ahora);
    }

    public void completar(String actorId, Instant ahora) {
        requireProveedor(actorId, "completar");
        transitionTo(estado.completar(), actorId, null, ahora);
    }

    /**
     * Cancela y devuelve cómo se reparte lo pagado. En PENDIENTE solo el cliente puede cancelar
     * (retira la solicitud; el proveedor usa {@link #rechazar}). Antes del pago el reintegro es cero;
     * en CONFIRMADA lo calcula la política recibida.
     */
    public Reintegro cancelar(String actorId, String motivo, PoliticaReintegro politica, Instant ahora) {
        requireParticipante(actorId, "cancelar");
        var siguiente = estado.cancelar();
        if (estado instanceof Pendiente && !clienteId.equals(actorId)) {
            throw new AccionNoPermitidaException(actorId, "cancelar");
        }
        requireText(motivo, "Cancelar una contratación requiere un motivo");
        Objects.requireNonNull(politica, "La política de reintegro es obligatoria");
        var calculado = estado instanceof Confirmada
                ? politica.calcular(precioAcordado, inicioSesion, ahora)
                : Reintegro.cero();
        transitionTo(siguiente, actorId, motivo, ahora);
        this.reintegro = calculado;
        return calculado;
    }

    /** Propone otra franja. No cambia el estado; queda pendiente hasta que el otro participante responda. */
    public void proponerReprogramacion(String actorId, FranjaId nuevaFranja, Instant nuevoInicio, Instant ahora) {
        requireParticipante(actorId, "proponer una reprogramación de");
        requireAdmiteReprogramacion("proponer una reprogramación");
        requireAntesDelInicio(ahora);
        if (propuesta != null) {
            throw new ReprogramacionInvalidaException("Ya hay una propuesta de reprogramación pendiente");
        }
        Objects.requireNonNull(nuevaFranja, "La franja propuesta es obligatoria");
        if (nuevaFranja.equals(franjaId)) {
            throw new ReprogramacionInvalidaException("La franja propuesta es la misma que la actual");
        }
        this.propuesta = new PropuestaReprogramacion(actorId, nuevaFranja, nuevoInicio);
    }

    /** Aplica la propuesta y devuelve la franja anterior, para que el caso de uso libere su cupo. */
    public FranjaId aceptarReprogramacion(String actorId, Instant ahora) {
        requireContraparteDeLaPropuesta(actorId, "aceptar la reprogramación de");
        requireAdmiteReprogramacion("aceptar una reprogramación");
        requireAntesDelInicio(ahora);
        var anterior = franjaId;
        this.franjaId = propuesta.franjaId();
        this.inicioSesion = propuesta.inicio();
        this.propuesta = null;
        return anterior;
    }

    public void rechazarReprogramacion(String actorId, Instant ahora) {
        requireContraparteDeLaPropuesta(actorId, "rechazar la reprogramación de");
        this.propuesta = null;
    }

    public boolean esParticipante(String userId) {
        return clienteId.equals(userId) || proveedorId.equals(userId);
    }

    private void transitionTo(EstadoContratacion siguiente, String actorId, String motivo, Instant ahora) {
        var cambio = new CambioEstado(estado.nombre(), siguiente.nombre(), ahora, actorId, motivo);
        historial.add(cambio);
        estado = siguiente;
    }

    private void requireCliente(String actorId, String accion) {
        if (!clienteId.equals(actorId)) {
            throw new AccionNoPermitidaException(actorId, accion);
        }
    }

    private void requireProveedor(String actorId, String accion) {
        if (!proveedorId.equals(actorId)) {
            throw new AccionNoPermitidaException(actorId, accion);
        }
    }

    private void requireParticipante(String actorId, String accion) {
        if (!esParticipante(actorId)) {
            throw new AccionNoPermitidaException(actorId, accion);
        }
    }

    private void requireContraparteDeLaPropuesta(String actorId, String accion) {
        requireParticipante(actorId, accion);
        if (propuesta == null) {
            throw new ReprogramacionInvalidaException("No hay una propuesta de reprogramación pendiente");
        }
        if (propuesta.propuestaPor().equals(actorId)) {
            throw new AccionNoPermitidaException(actorId, accion);
        }
    }

    private void requireAdmiteReprogramacion(String accion) {
        if (!estado.admiteReprogramacion()) {
            throw new TransicionInvalidaException(estado.nombre(), accion);
        }
    }

    private void requireAntesDelInicio(Instant ahora) {
        Objects.requireNonNull(ahora, "El momento de la operación es obligatorio");
        if (!ahora.isBefore(inicioSesion)) {
            throw new ReprogramacionInvalidaException("La sesión ya comenzó: no se puede reprogramar");
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public ContratacionId id() {
        return id;
    }

    public PublicacionId publicacionId() {
        return publicacionId;
    }

    public String clienteId() {
        return clienteId;
    }

    public String proveedorId() {
        return proveedorId;
    }

    public Solicitud solicitud() {
        return solicitud;
    }

    public FranjaId franjaId() {
        return franjaId;
    }

    public Instant inicioSesion() {
        return inicioSesion;
    }

    public SkillCoins precioAcordado() {
        return precioAcordado;
    }

    public EstadoContratacion estado() {
        return estado;
    }

    /** Propuesta de reprogramación pendiente, o {@code null} si no hay. */
    public PropuestaReprogramacion propuesta() {
        return propuesta;
    }

    /** Reintegro calculado al cancelar, o {@code null} si no se canceló. */
    public Reintegro reintegro() {
        return reintegro;
    }

    public List<CambioEstado> historial() {
        return List.copyOf(historial);
    }
}
