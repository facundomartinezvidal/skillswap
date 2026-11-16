package ar.edu.uade.skillswap.application.contratacion;

import ar.edu.uade.skillswap.application.contratacion.dto.CalificacionDTO;
import ar.edu.uade.skillswap.application.contratacion.dto.ContratacionDTO;
import ar.edu.uade.skillswap.application.contratacion.dto.SolicitarContratacionCommand;
import ar.edu.uade.skillswap.application.evento.PublicadorEventos;
import ar.edu.uade.skillswap.application.shared.Ids;
import ar.edu.uade.skillswap.application.shared.UnidadDeTrabajo;
import ar.edu.uade.skillswap.application.shared.exception.PermisoDenegadoException;
import ar.edu.uade.skillswap.application.shared.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.domain.contratacion.Contratacion;
import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.contratacion.ContratacionRepository;
import ar.edu.uade.skillswap.domain.contratacion.Solicitud;
import ar.edu.uade.skillswap.domain.contratacion.strategy.PoliticaReintegroFactory;
import ar.edu.uade.skillswap.domain.perfil.PerfilRepository;
import ar.edu.uade.skillswap.domain.perfil.exception.PerfilInactivoException;
import ar.edu.uade.skillswap.domain.publicacion.FranjaId;
import ar.edu.uade.skillswap.domain.publicacion.Publicacion;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionId;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionRepository;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.domain.shared.Puntaje;
import ar.edu.uade.skillswap.domain.wallet.RetencionDePagos;
import ar.edu.uade.skillswap.domain.wallet.Wallet;
import ar.edu.uade.skillswap.domain.wallet.WalletRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Casos de uso de la contratación: el recorrido solicitar, aceptar, pagar, iniciar, completar y
 * calificar, más rechazar, cancelar con reintegro y reprogramar. Lo usan el {@code ContratacionController}
 * y el {@code CalificacionController}.
 * <p>
 * Coordina agregados que se conocen solo por id; las reglas viven en ellos. Las operaciones que tocan
 * más de un agregado corren en una {@link UnidadDeTrabajo}: o se guarda todo o nada. Igual, por
 * claridad, primero se modifican todos los agregados (cualquiera puede rechazar la operación) y los
 * {@code save} van juntos al final. Los eventos se publican después de confirmar, así una operación
 * rechazada no notifica a nadie.
 */
public class ContratacionService {

    private final ContratacionRepository contratacionRepository;
    private final PublicacionRepository publicacionRepository;
    private final WalletRepository walletRepository;
    private final PerfilRepository perfilRepository;
    private final RetencionDePagos retencionDePagos;
    private final PoliticaReintegroFactory politicaReintegroFactory;
    private final UnidadDeTrabajo unidadDeTrabajo;
    private final PublicadorEventos publicadorEventos;
    private final Clock clock;

    public ContratacionService(ContratacionRepository contratacionRepository,
                               PublicacionRepository publicacionRepository, WalletRepository walletRepository,
                               PerfilRepository perfilRepository, RetencionDePagos retencionDePagos,
                               PoliticaReintegroFactory politicaReintegroFactory, UnidadDeTrabajo unidadDeTrabajo,
                               PublicadorEventos publicadorEventos, Clock clock) {
        this.contratacionRepository = Objects.requireNonNull(contratacionRepository);
        this.publicacionRepository = Objects.requireNonNull(publicacionRepository);
        this.walletRepository = Objects.requireNonNull(walletRepository);
        this.perfilRepository = Objects.requireNonNull(perfilRepository);
        this.retencionDePagos = Objects.requireNonNull(retencionDePagos);
        this.politicaReintegroFactory = Objects.requireNonNull(politicaReintegroFactory);
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
        this.publicadorEventos = Objects.requireNonNull(publicadorEventos);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Crea la contratación en PENDIENTE con el precio final de la publicación (con promociones).
     * El cliente tiene que tener perfil activo y Wallet. La publicación valida que admita solicitudes y
     * que la franja no haya empezado y tenga lugar; el cupo se reserva recién al aceptar.
     */
    public ContratacionDTO solicitar(SolicitarContratacionCommand command) {
        validarCliente(command.clienteId());
        var publicacion = buscarPublicacion(new PublicacionId(Ids.uuid("publicación", command.publicacionId())));
        var franja = publicacion.franjaParaSolicitar(new FranjaId(Ids.uuid("franja", command.franjaId())), ahora());
        var solicitud = new Solicitud(command.mensaje(), command.objetivo(), franja.id());
        var contratacion = Contratacion.solicitar(command.clienteId(), publicacion.autorId(), publicacion.id(),
                solicitud, franja.inicio(), publicacion.precioFinal(), ahora());
        contratacionRepository.save(contratacion);
        publicarEventos(contratacion);
        return ContratacionMapper.aDTO(contratacion, publicacion.titulo());
    }

    /**
     * El proveedor acepta y se reserva el cupo de la franja, en una transacción. Funciona aunque la
     * publicación esté pausada: pausar corta las solicitudes nuevas, no las que ya llegaron.
     */
    public ContratacionDTO aceptar(String userId, String contratacionId) {
        var contratacion = unidadDeTrabajo.ejecutar(() -> {
            var c = buscar(contratacionId);
            var publicacion = buscarPublicacion(c.publicacionId());
            c.aceptar(userId, ahora());
            publicacion.reservarCupo(c.franjaId());

            publicacionRepository.save(publicacion);
            contratacionRepository.save(c);
            return c;
        });
        publicarEventos(contratacion);
        return aDTO(contratacion);
    }

    public ContratacionDTO rechazar(String userId, String contratacionId, String motivo) {
        var contratacion = buscar(contratacionId);
        contratacion.rechazar(userId, motivo, ahora());
        contratacionRepository.save(contratacion);
        publicarEventos(contratacion);
        return aDTO(contratacion);
    }

    /**
     * El cliente paga: el precio pasa de su Wallet a la de garantía, en una transacción (regla 4).
     * Un segundo pago lo rechaza el estado de la contratación, así que no hay doble cobro.
     */
    public ContratacionDTO pagar(String userId, String contratacionId) {
        var contratacion = unidadDeTrabajo.ejecutar(() -> {
            var c = buscar(contratacionId);
            c.pagar(userId, ahora());
            if (c.precioAcordado().valor() > 0) {
                var cliente = buscarWallet(c.clienteId());
                var garantia = walletRepository.garantia();
                retencionDePagos.retener(cliente, garantia, c.precioAcordado(), c.id(), ahora());
                walletRepository.save(cliente);
                walletRepository.save(garantia);
            }
            contratacionRepository.save(c);
            return c;
        });
        publicarEventos(contratacion);
        return aDTO(contratacion);
    }

    public ContratacionDTO iniciar(String userId, String contratacionId) {
        var contratacion = buscar(contratacionId);
        contratacion.iniciar(userId, ahora());
        contratacionRepository.save(contratacion);
        publicarEventos(contratacion);
        return aDTO(contratacion);
    }

    /** El proveedor completa y cobra lo que estaba retenido en la garantía, en una transacción. */
    public ContratacionDTO completar(String userId, String contratacionId) {
        var contratacion = unidadDeTrabajo.ejecutar(() -> {
            var c = buscar(contratacionId);
            c.completar(userId, ahora());
            if (c.precioAcordado().valor() > 0) {
                var garantia = walletRepository.garantia();
                var proveedor = buscarWallet(c.proveedorId());
                retencionDePagos.liberarAlProveedor(garantia, proveedor, c.precioAcordado(), c.id(), ahora());
                walletRepository.save(garantia);
                walletRepository.save(proveedor);
            }
            contratacionRepository.save(c);
            return c;
        });
        publicarEventos(contratacion);
        return aDTO(contratacion);
    }

    /**
     * Cancela con la política de reintegro que corresponde a quien cancela (Strategy elegida por la
     * Simple Factory del dominio). Si estaba pagada, la garantía reparte lo retenido; si ocupaba cupo,
     * se libera. Todo en una transacción, y los {@code save} recién al final.
     */
    public ContratacionDTO cancelar(String userId, String contratacionId, String motivo) {
        var contratacion = unidadDeTrabajo.ejecutar(() -> {
            var c = buscar(contratacionId);
            var ocupabaCupo = c.ocupaCupo();
            var politica = politicaReintegroFactory.paraCancelacion(c, userId);
            var reintegro = c.cancelar(userId, motivo, politica, ahora());

            var walletsModificadas = new ArrayList<Wallet>();
            var aRepartir = reintegro.alCliente().sumar(reintegro.alProveedor());
            if (aRepartir.valor() > 0) {
                var garantia = walletRepository.garantia();
                var cliente = buscarWallet(c.clienteId());
                var proveedor = buscarWallet(c.proveedorId());
                retencionDePagos.repartir(garantia, cliente, proveedor, aRepartir, reintegro.alCliente(),
                        reintegro.alProveedor(), c.id(), ahora());
                walletsModificadas.add(garantia);
                walletsModificadas.add(cliente);
                walletsModificadas.add(proveedor);
            }
            Publicacion publicacion = null;
            if (ocupabaCupo) {
                publicacion = buscarPublicacion(c.publicacionId());
                publicacion.liberarCupo(c.franjaId());
            }

            walletsModificadas.forEach(walletRepository::save);
            if (publicacion != null) {
                publicacionRepository.save(publicacion);
            }
            contratacionRepository.save(c);
            return c;
        });
        publicarEventos(contratacion);
        return aDTO(contratacion);
    }

    /**
     * Propone otra franja de la misma publicación, que no haya empezado y tenga lugar. Funciona aunque
     * la publicación esté pausada, porque la contratación ya existe. Responde la otra parte.
     */
    public ContratacionDTO proponerReprogramacion(String userId, String contratacionId, String nuevaFranjaId) {
        var contratacion = buscar(contratacionId);
        var publicacion = buscarPublicacion(contratacion.publicacionId());
        var franja = publicacion.franjaDisponible(new FranjaId(Ids.uuid("franja", nuevaFranjaId)), ahora());
        contratacion.proponerReprogramacion(userId, franja.id(), franja.inicio(), ahora());
        contratacionRepository.save(contratacion);
        publicarEventos(contratacion);
        return ContratacionMapper.aDTO(contratacion, publicacion.titulo());
    }

    /** Aplica la propuesta: reserva la franja nueva y libera la anterior, en una transacción. */
    public ContratacionDTO aceptarReprogramacion(String userId, String contratacionId) {
        var contratacion = unidadDeTrabajo.ejecutar(() -> {
            var c = buscar(contratacionId);
            var publicacion = buscarPublicacion(c.publicacionId());
            var anterior = c.aceptarReprogramacion(userId, ahora());
            publicacion.reservarCupo(c.franjaId());
            publicacion.liberarCupo(anterior);

            publicacionRepository.save(publicacion);
            contratacionRepository.save(c);
            return c;
        });
        publicarEventos(contratacion);
        return aDTO(contratacion);
    }

    public ContratacionDTO rechazarReprogramacion(String userId, String contratacionId) {
        var contratacion = buscar(contratacionId);
        contratacion.rechazarReprogramacion(userId, ahora());
        contratacionRepository.save(contratacion);
        publicarEventos(contratacion);
        return aDTO(contratacion);
    }

    /** Califica a la otra parte y suma el puntaje a su reputación, en una transacción. */
    public CalificacionDTO calificar(String userId, String contratacionId, int puntaje, String comentario) {
        var calificacion = unidadDeTrabajo.ejecutar(() -> {
            var c = buscar(contratacionId);
            var nueva = c.calificar(userId, new Puntaje(puntaje), comentario, ahora());
            var destinatario = perfilRepository.findByUserId(nueva.destinatarioId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("perfil", nueva.destinatarioId()));
            destinatario.registrarCalificacion(nueva.puntaje());

            perfilRepository.save(destinatario);
            contratacionRepository.save(c);
            return nueva;
        });
        return ContratacionMapper.aDTO(calificacion);
    }

    /**
     * Las contrataciones del usuario como cliente o como proveedor, de la más nueva a la más vieja.
     * Los títulos de las publicaciones se buscan todos juntos, para no hacer una consulta por contratación.
     */
    public Pagina<ContratacionDTO> listarContrataciones(String userId, PaginaSolicitada pagina) {
        var contrataciones = contratacionRepository.findByParticipante(userId, pagina);
        var ids = contrataciones.elementos().stream().map(Contratacion::publicacionId).toList();
        var titulos = publicacionRepository.findByIds(ids).stream()
                .collect(Collectors.toMap(Publicacion::id, Publicacion::titulo));
        return contrataciones.map(c -> ContratacionMapper.aDTO(c, titulos.get(c.publicacionId())));
    }

    /** Detalle con historial y comprobante. Solo para los participantes. */
    public ContratacionDTO verContratacion(String userId, String contratacionId) {
        var contratacion = buscar(contratacionId);
        if (!contratacion.esParticipante(userId)) {
            throw new PermisoDenegadoException(userId, "ver esta contratación");
        }
        return aDTO(contratacion);
    }

    /** Quien solicita tiene que tener perfil activo y Wallet, para poder pagar después. */
    private void validarCliente(String clienteId) {
        var perfil = perfilRepository.findByUserId(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("perfil", clienteId));
        if (!perfil.estaActivo()) {
            throw new PerfilInactivoException(clienteId, "solicitar una contratación");
        }
        buscarWallet(clienteId);
    }

    private Contratacion buscar(String contratacionId) {
        var id = new ContratacionId(Ids.uuid("contratación", contratacionId));
        return contratacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("contratación", contratacionId));
    }

    private Publicacion buscarPublicacion(PublicacionId id) {
        return publicacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("publicación", id.valor().toString()));
    }

    private Wallet buscarWallet(String titularId) {
        return walletRepository.findByTitular(titularId)
                .orElseThrow(() -> new RecursoNoEncontradoException("wallet", titularId));
    }

    /** Se llama en todas las operaciones, aunque hoy algunas no emitan eventos, para no perder uno nuevo. */
    private void publicarEventos(Contratacion contratacion) {
        publicadorEventos.publicar(contratacion.extraerEventos());
    }

    private ContratacionDTO aDTO(Contratacion contratacion) {
        var titulo = publicacionRepository.findById(contratacion.publicacionId())
                .map(Publicacion::titulo)
                .orElse(null);
        return ContratacionMapper.aDTO(contratacion, titulo);
    }

    private Instant ahora() {
        return clock.instant();
    }
}
