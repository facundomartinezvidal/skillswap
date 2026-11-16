package ar.edu.uade.skillswap.infrastructure.memoria;

import ar.edu.uade.skillswap.domain.contratacion.Contratacion;
import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.contratacion.ContratacionRepository;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionId;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repositorio de contrataciones en memoria, para tests y para la demo sin base de datos.
 * Guarda y devuelve copias, como una base: cambiar lo que devuelve {@code findById} no cambia
 * lo guardado hasta que se llama a {@code save}. Los {@code save} pasan por la {@link UnidadDeTrabajoEnMemoria}.
 */
public class ContratacionRepositoryEnMemoria implements ContratacionRepository {

    private static final String ACEPTADA = "ACEPTADA";
    private static final String COMPLETADA = "COMPLETADA";

    /** En orden de creación; los listados las devuelven de la más nueva a la más vieja. */
    private final Map<ContratacionId, Contratacion> contrataciones = new LinkedHashMap<>();
    private final UnidadDeTrabajoEnMemoria unidadDeTrabajo;

    public ContratacionRepositoryEnMemoria(UnidadDeTrabajoEnMemoria unidadDeTrabajo) {
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
    }

    @Override
    public Optional<Contratacion> findById(ContratacionId id) {
        return Optional.ofNullable(contrataciones.get(id)).map(ContratacionRepositoryEnMemoria::copiar);
    }

    @Override
    public void save(Contratacion contratacion) {
        var copia = copiar(contratacion);
        unidadDeTrabajo.registrar(() -> contrataciones.put(copia.id(), copia));
    }

    @Override
    public Pagina<Contratacion> findByParticipante(String userId, PaginaSolicitada pagina) {
        var delUsuario = new ArrayList<>(contrataciones.values().stream()
                .filter(contratacion -> contratacion.esParticipante(userId))
                .map(ContratacionRepositoryEnMemoria::copiar)
                .toList());
        Collections.reverse(delUsuario);
        return Pagina.de(delUsuario, pagina);
    }

    @Override
    public long countCompletadasDe(String userId) {
        return contrataciones.values().stream()
                .filter(contratacion -> contratacion.esParticipante(userId))
                .filter(contratacion -> COMPLETADA.equals(contratacion.estado().nombre()))
                .count();
    }

    @Override
    public Map<PublicacionId, Long> countAceptadasDesde(Instant desde) {
        return contrataciones.values().stream()
                .filter(contratacion -> contratacion.historial().stream()
                        .anyMatch(cambio -> ACEPTADA.equals(cambio.hacia()) && !cambio.fecha().isBefore(desde)))
                .collect(Collectors.groupingBy(Contratacion::publicacionId, Collectors.counting()));
    }

    private static Contratacion copiar(Contratacion c) {
        return Contratacion.reconstruir(c.id(), c.publicacionId(), c.clienteId(), c.proveedorId(), c.solicitud(),
                c.franjaId(), c.inicioSesion(), c.precioAcordado(), c.estado(), c.propuesta(), c.reintegro(),
                c.historial(), c.calificaciones());
    }
}
