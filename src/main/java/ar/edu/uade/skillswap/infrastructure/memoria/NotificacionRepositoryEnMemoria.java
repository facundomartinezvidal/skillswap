package ar.edu.uade.skillswap.infrastructure.memoria;

import ar.edu.uade.skillswap.domain.notificacion.Notificacion;
import ar.edu.uade.skillswap.domain.notificacion.NotificacionId;
import ar.edu.uade.skillswap.domain.notificacion.NotificacionRepository;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Bandeja de notificaciones en memoria, para tests y para la demo sin base de datos.
 * Guarda y devuelve copias, como una base. Los {@code save} pasan por la {@link UnidadDeTrabajoEnMemoria}.
 */
public class NotificacionRepositoryEnMemoria implements NotificacionRepository {

    /** En orden de creación; la bandeja las devuelve de la más nueva a la más vieja. */
    private final Map<NotificacionId, Notificacion> notificaciones = new LinkedHashMap<>();
    private final UnidadDeTrabajoEnMemoria unidadDeTrabajo;

    public NotificacionRepositoryEnMemoria(UnidadDeTrabajoEnMemoria unidadDeTrabajo) {
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
    }

    @Override
    public Optional<Notificacion> findById(NotificacionId id) {
        return Optional.ofNullable(notificaciones.get(id)).map(NotificacionRepositoryEnMemoria::copiar);
    }

    @Override
    public void save(Notificacion notificacion) {
        var copia = copiar(notificacion);
        unidadDeTrabajo.registrar(() -> notificaciones.put(copia.id(), copia));
    }

    @Override
    public Pagina<Notificacion> findByDestinatario(String destinatarioId, PaginaSolicitada pagina) {
        var bandeja = new ArrayList<>(delDestinatario(destinatarioId));
        Collections.reverse(bandeja);
        return Pagina.de(bandeja, pagina);
    }

    @Override
    public List<Notificacion> findNoLeidas(String destinatarioId) {
        return delDestinatario(destinatarioId).stream()
                .filter(notificacion -> !notificacion.estaLeida())
                .toList();
    }

    private List<Notificacion> delDestinatario(String destinatarioId) {
        return notificaciones.values().stream()
                .filter(notificacion -> notificacion.destinatarioId().equals(destinatarioId))
                .map(NotificacionRepositoryEnMemoria::copiar)
                .toList();
    }

    private static Notificacion copiar(Notificacion n) {
        return Notificacion.reconstruir(n.id(), n.destinatarioId(), n.tipo(), n.titulo(), n.detalle(), n.fecha(),
                n.estaLeida());
    }
}
