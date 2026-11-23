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
import ar.edu.uade.skillswap.controllers.FranjaVista;
import ar.edu.uade.skillswap.controllers.PasoSiguienteVista;
import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.model.Models.Contratacion;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    @Override
    public Contratacion solicitar(String userId, String publicacionId, String franjaId, String mensaje,
                                  String objetivo) {
        return ejecutar(() -> aModelo(userId, contratacionService.solicitar(
                new SolicitarContratacionCommand(userId, publicacionId, franjaId, mensaje, objetivo))));
    }

    private String franjaParaSolicitar(String userId, String publicacionId) {
        var franjas = publicacionService.verPublicacion(userId, publicacionId).franjas().stream()
                .sorted(Comparator.comparing(FranjaDTO::inicio))
                .toList();
        return franjas.stream()
                .filter(f -> ModelosMapper.estaDisponible(f, clock.instant()))
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
            var paso = pasoDe(actual, userId);
            if (paso == null) {
                throw new OperacionRechazadaException(sinPasoSiguiente(actual.estado()));
            }
            var resultado = switch (paso) {
                case ACEPTAR -> contratacionService.aceptar(userId, contratacionId);
                case PAGAR -> contratacionService.pagar(userId, contratacionId);
                case INICIAR -> contratacionService.iniciar(userId, contratacionId);
                case COMPLETAR -> contratacionService.completar(userId, contratacionId);
            };
            return aModelo(userId, resultado);
        });
    }

    /** Lo que hace {@link #avanzar} según el estado y quién lo pide. */
    private enum Paso { ACEPTAR, PAGAR, INICIAR, COMPLETAR }

    /** El paso que le toca a {@code userId}, o null si no le toca ninguno. */
    private static Paso pasoDe(ContratacionDTO c, String userId) {
        var esProveedor = c.proveedorId().equals(userId);
        var esCliente = c.clienteId().equals(userId);
        return switch (c.estado()) {
            case "PENDIENTE" -> esProveedor ? Paso.ACEPTAR : null;
            case "ACEPTADA" -> esCliente ? Paso.PAGAR : null;
            case "CONFIRMADA" -> esProveedor ? Paso.INICIAR : null;
            case "EN_CURSO" -> esProveedor ? Paso.COMPLETAR : null;
            default -> null;
        };
    }

    @Override
    public PasoSiguienteVista pasoSiguiente(String userId, String contratacionId) {
        return ejecutar(() -> {
            var c = contratacionService.verContratacion(userId, contratacionId);
            var esCliente = c.clienteId().equals(userId);
            var otro = nombreContraparte(userId, c, new HashMap<>());
            var paso = pasoDe(c, userId);
            var accion = paso == null ? null : switch (paso) {
                case ACEPTAR -> "Aceptar";
                case PAGAR -> "Pagar " + c.precioAcordado() + " SkillCoins";
                case INICIAR -> "Iniciar sesión";
                case COMPLETAR -> "Completar sesión";
            };
            var propuesta = c.propuesta();
            var detalle = detalle(c.estado(), esCliente, otro);
            if (propuesta == null) {
                return new PasoSiguienteVista(esCliente ? "Cliente" : "Proveedor", accion, detalle, null, false);
            }
            var porMi = propuesta.propuestaPor().equals(userId);
            var nuevoInicio = ModelosMapper.aFecha(propuesta.inicio());
            var cuando = nuevoInicio.format(FORMATO_FECHA);
            detalle += porMi ? " Propusiste pasar la sesión al " + cuando + ": falta que " + otro + " responda."
                    : " " + otro + " propuso pasar la sesión al " + cuando + ".";
            return new PasoSiguienteVista(esCliente ? "Cliente" : "Proveedor", accion, detalle, nuevoInicio, porMi);
        });
    }

    @Override
    public List<String> verComprobante(String userId, String contratacionId) {
        return ejecutar(() -> {
            var c = contratacionService.verContratacion(userId, contratacionId);
            var nombres = new HashMap<String, String>();
            var lineas = new ArrayList<String>();
            lineas.add("Contratación " + c.id());
            lineas.add("Publicación: " + c.tituloPublicacion());
            lineas.add("Cliente: " + nombres.computeIfAbsent(c.clienteId(), this::nombreVisible));
            lineas.add("Proveedor: " + nombres.computeIfAbsent(c.proveedorId(), this::nombreVisible));
            lineas.add("Sesión: " + ModelosMapper.aFecha(c.inicioSesion()).format(FORMATO_FECHA));
            lineas.add("Importe: " + c.precioAcordado() + " SkillCoins");
            lineas.add("Estado: " + c.estado());
            for (var cambio : c.historial()) {
                switch (cambio.hacia()) {
                    case "CONFIRMADA" -> lineas.add("Pagado el " + fecha(cambio.fecha()));
                    case "COMPLETADA" -> lineas.add("Completada el " + fecha(cambio.fecha()));
                    case "CANCELADA" -> lineas.add("Cancelada el " + fecha(cambio.fecha()));
                    default -> { }
                }
            }
            if (c.reintegro() != null) {
                lineas.add("Reintegro: " + c.reintegro().alCliente() + " SkillCoins al cliente y "
                        + c.reintegro().alProveedor() + " al proveedor");
            }
            for (var calificacion : c.calificaciones()) {
                var comentario = calificacion.comentario() == null || calificacion.comentario().isBlank()
                        ? "" : " · " + calificacion.comentario();
                lineas.add("Calificación de " + nombres.computeIfAbsent(calificacion.autorId(), this::nombreVisible)
                        + ": " + calificacion.puntaje() + "/5" + comentario);
            }
            return List.copyOf(lineas);
        });
    }

    private String fecha(Instant instante) {
        return ModelosMapper.aFecha(instante).format(FORMATO_FECHA);
    }

    @Override
    public List<FranjaVista> listarFranjasParaReprogramar(String userId, String contratacionId) {
        return ejecutar(() -> {
            var c = contratacionService.verContratacion(userId, contratacionId);
            var ahora = clock.instant();
            return publicacionService.verPublicacion(userId, c.publicacionId()).franjas().stream()
                    .filter(f -> !f.id().equals(c.franjaId()) && ModelosMapper.estaDisponible(f, ahora))
                    .sorted(Comparator.comparing(FranjaDTO::inicio))
                    .map(f -> ModelosMapper.aVista(f, ahora))
                    .toList();
        });
    }

    private static String detalle(String estado, boolean esCliente, String otro) {
        return switch (estado) {
            case "PENDIENTE" -> esCliente ? "Esperando que " + otro + " acepte tu solicitud."
                    : otro + " te pidió esta mentoría. Al aceptarla se reserva el lugar en la franja.";
            case "ACEPTADA" -> esCliente ? otro + " aceptó. Pagá para confirmar la sesión."
                    : "Aceptaste. Esperando el pago de " + otro + ".";
            case "CONFIRMADA" -> esCliente ? "Pago confirmado. Esperando que " + otro + " inicie la sesión."
                    : otro + " ya pagó. Iniciá la sesión a la hora de la franja.";
            case "EN_CURSO" -> esCliente ? "La sesión está en curso." : "La sesión está en curso. Completala al terminar.";
            case "COMPLETADA" -> "Sesión completada. Podés calificar a " + otro + ".";
            case "RECHAZADA" -> esCliente ? otro + " rechazó la solicitud." : "Rechazaste la solicitud.";
            case "CANCELADA" -> "La contratación se canceló.";
            default -> "";
        };
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
