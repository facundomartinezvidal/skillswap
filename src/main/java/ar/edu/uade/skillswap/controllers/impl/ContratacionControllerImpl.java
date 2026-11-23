package ar.edu.uade.skillswap.controllers.impl;

import ar.edu.uade.skillswap.application.contratacion.ContratacionService;
import ar.edu.uade.skillswap.application.contratacion.dto.CambioEstadoDTO;
import ar.edu.uade.skillswap.application.contratacion.dto.ContratacionDTO;
import ar.edu.uade.skillswap.application.contratacion.dto.SolicitarContratacionCommand;
import ar.edu.uade.skillswap.application.perfil.PerfilService;
import ar.edu.uade.skillswap.application.publicacion.PublicacionService;
import ar.edu.uade.skillswap.application.publicacion.dto.FranjaDTO;
import ar.edu.uade.skillswap.application.shared.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.controllers.ContratacionOperaciones;
import ar.edu.uade.skillswap.controllers.OperacionRechazadaException;
import ar.edu.uade.skillswap.controllers.PaginaVista;
import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.model.Models.Contratacion;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static ar.edu.uade.skillswap.controllers.impl.ManejoDeErrores.ejecutar;

public class ContratacionControllerImpl implements UiControllers.ContratacionController, ContratacionOperaciones {

    /** La UI de la cátedra no pide motivo al cancelar. */
    static final String MOTIVO_POR_DEFECTO = "Cambio desde la aplicación";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ContratacionService contratacionService;
    private final PublicacionService publicacionService;
    private final PerfilService perfilService;
    private final Clock clock;

    public ContratacionControllerImpl(ContratacionService contratacionService, PublicacionService publicacionService,
                                      PerfilService perfilService, Clock clock) {
        this.contratacionService = Objects.requireNonNull(contratacionService);
        this.publicacionService = Objects.requireNonNull(publicacionService);
        this.perfilService = Objects.requireNonNull(perfilService);
        this.clock = Objects.requireNonNull(clock);
    }

    /** La UI no pagina: ve la primera página. */
    @Override
    public List<Contratacion> listarContrataciones(String userId) {
        return listarContrataciones(userId, 0, ModelosMapper.TAMANIO_UI).elementos();
    }

    @Override
    public PaginaVista<Contratacion> listarContrataciones(String userId, int pagina, int tamanio) {
        return ejecutar(() -> {
            // Un mismo nombre aparece en varias filas: se busca una sola vez por persona.
            var nombres = new HashMap<String, String>();
            var contrataciones = contratacionService.listarContrataciones(userId, new PaginaSolicitada(pagina, tamanio));
            return ModelosMapper.aVista(contrataciones,
                    c -> ModelosMapper.aModelo(c, nombreContraparte(userId, c, nombres)));
        });
    }

    /**
     * La UI no deja elegir franja ni escribir un mensaje: se usa la primera franja que todavía no empezó
     * y tiene lugar, y el objetivo también como mensaje. Si no hay ninguna, se manda la primera para que
     * el dominio explique por qué no se puede (cupo agotado o franja ya empezada).
     */
    @Override
    public Contratacion solicitar(String userId, String publicacionId, String objetivo) {
        return ejecutar(() -> {
            var command = new SolicitarContratacionCommand(userId, publicacionId,
                    franjaParaSolicitar(userId, publicacionId), objetivo, objetivo);
            return aModelo(userId, contratacionService.solicitar(command));
        });
    }

    private String franjaParaSolicitar(String userId, String publicacionId) {
        var franjas = publicacionService.verPublicacion(userId, publicacionId).franjas().stream()
                .sorted(Comparator.comparing(FranjaDTO::inicio))
                .toList();
        return franjas.stream()
                .filter(f -> f.lugaresLibres() > 0 && f.inicio().isAfter(clock.instant()))
                .findFirst()
                .or(() -> franjas.stream().findFirst())
                .orElseThrow(() -> new OperacionRechazadaException("La publicación no tiene franjas disponibles"))
                .id();
    }

    /**
     * La UI tiene un solo botón "Avanzar estado". El paso siguiente depende del estado y de quién lo pide:
     * PENDIENTE y proveedor acepta, ACEPTADA y cliente paga, CONFIRMADA y proveedor inicia, EN_CURSO y
     * proveedor completa. Cualquier otra combinación se rechaza con un mensaje claro.
     */
    @Override
    public Contratacion avanzar(String userId, String contratacionId) {
        return ejecutar(() -> {
            var actual = contratacionService.verContratacion(userId, contratacionId);
            var esProveedor = actual.proveedorId().equals(userId);
            var esCliente = actual.clienteId().equals(userId);
            ContratacionDTO resultado;
            if ("PENDIENTE".equals(actual.estado()) && esProveedor) {
                resultado = contratacionService.aceptar(userId, contratacionId);
            } else if ("ACEPTADA".equals(actual.estado()) && esCliente) {
                resultado = contratacionService.pagar(userId, contratacionId);
            } else if ("CONFIRMADA".equals(actual.estado()) && esProveedor) {
                resultado = contratacionService.iniciar(userId, contratacionId);
            } else if ("EN_CURSO".equals(actual.estado()) && esProveedor) {
                resultado = contratacionService.completar(userId, contratacionId);
            } else {
                throw new OperacionRechazadaException(sinPasoSiguiente(actual.estado()));
            }
            return aModelo(userId, resultado);
        });
    }

    private static String sinPasoSiguiente(String estado) {
        return switch (estado) {
            case "PENDIENTE" -> "La contratación está PENDIENTE: la tiene que aceptar el proveedor";
            case "ACEPTADA" -> "La contratación está ACEPTADA: la tiene que pagar el cliente";
            case "CONFIRMADA" -> "La contratación está CONFIRMADA: la tiene que iniciar el proveedor";
            case "EN_CURSO" -> "La contratación está EN_CURSO: la tiene que completar el proveedor";
            default -> "La contratación está " + estado + ": no tiene más pasos";
        };
    }

    @Override
    public Contratacion cancelar(String userId, String contratacionId) {
        return cancelar(userId, contratacionId, MOTIVO_POR_DEFECTO);
    }

    @Override
    public Contratacion cancelar(String userId, String contratacionId, String motivo) {
        return ejecutar(() -> aModelo(userId, contratacionService.cancelar(userId, contratacionId, motivo)));
    }

    @Override
    public Contratacion aceptar(String userId, String contratacionId) {
        return ejecutar(() -> aModelo(userId, contratacionService.aceptar(userId, contratacionId)));
    }

    @Override
    public Contratacion rechazar(String userId, String contratacionId, String motivo) {
        return ejecutar(() -> aModelo(userId, contratacionService.rechazar(userId, contratacionId, motivo)));
    }

    @Override
    public Contratacion pagar(String userId, String contratacionId) {
        return ejecutar(() -> aModelo(userId, contratacionService.pagar(userId, contratacionId)));
    }

    @Override
    public Contratacion iniciar(String userId, String contratacionId) {
        return ejecutar(() -> aModelo(userId, contratacionService.iniciar(userId, contratacionId)));
    }

    @Override
    public Contratacion completar(String userId, String contratacionId) {
        return ejecutar(() -> aModelo(userId, contratacionService.completar(userId, contratacionId)));
    }

    @Override
    public Contratacion proponerReprogramacion(String userId, String contratacionId, String nuevaFranjaId) {
        return ejecutar(() -> aModelo(userId,
                contratacionService.proponerReprogramacion(userId, contratacionId, nuevaFranjaId)));
    }

    @Override
    public Contratacion aceptarReprogramacion(String userId, String contratacionId) {
        return ejecutar(() -> aModelo(userId, contratacionService.aceptarReprogramacion(userId, contratacionId)));
    }

    @Override
    public Contratacion rechazarReprogramacion(String userId, String contratacionId) {
        return ejecutar(() -> aModelo(userId, contratacionService.rechazarReprogramacion(userId, contratacionId)));
    }

    @Override
    public List<String> verHistorial(String userId, String contratacionId) {
        return ejecutar(() -> {
            var nombres = new HashMap<String, String>();
            return contratacionService.verContratacion(userId, contratacionId).historial().stream()
                    .map(cambio -> lineaDeHistorial(cambio, nombres))
                    .toList();
        });
    }

    @Override
    public boolean esCliente(String userId, String contratacionId) {
        return ejecutar(() -> contratacionService.verContratacion(userId, contratacionId).clienteId().equals(userId));
    }

    /** Quién puede y desde qué estado lo decide el dominio; acá solo se elige la franja, como en {@link #solicitar}. */
    @Override
    public Contratacion volverASolicitar(String userId, String contratacionId) {
        return ejecutar(() -> {
            var anterior = contratacionService.verContratacion(userId, contratacionId);
            var franja = franjaParaSolicitar(userId, anterior.publicacionId());
            return aModelo(userId, contratacionService.volverASolicitar(userId, contratacionId, franja,
                    "Vuelvo a solicitar después del rechazo"));
        });
    }

    private String lineaDeHistorial(CambioEstadoDTO cambio, Map<String, String> nombres) {
        var fecha = LocalDateTime.ofInstant(cambio.fecha(), clock.getZone()).format(FORMATO_FECHA);
        var desde = cambio.desde() == null ? "" : cambio.desde() + " → ";
        var actor = nombres.computeIfAbsent(cambio.actorId(), this::nombreVisible);
        var motivo = cambio.motivo() == null || cambio.motivo().isBlank() ? "" : " · " + cambio.motivo();
        return fecha + " · " + desde + cambio.hacia() + " · " + actor + motivo;
    }

    /** El actor puede no tener perfil (por ejemplo, un proceso del sistema): se muestra su id. */
    private String nombreVisible(String userId) {
        try {
            return perfilService.verPerfil(userId).nombreVisible();
        } catch (RecursoNoEncontradoException e) {
            return userId;
        }
    }

    private Contratacion aModelo(String userId, ContratacionDTO contratacion) {
        return ModelosMapper.aModelo(contratacion, nombreContraparte(userId, contratacion, new HashMap<>()));
    }

    /** La UI muestra el nombre de la otra parte: el proveedor para el cliente y el cliente para el proveedor. */
    private String nombreContraparte(String userId, ContratacionDTO c, Map<String, String> nombres) {
        var contraparte = c.clienteId().equals(userId) ? c.proveedorId() : c.clienteId();
        return nombres.computeIfAbsent(contraparte, id -> perfilService.verPerfil(id).nombreVisible());
    }
}
