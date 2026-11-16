package ar.edu.uade.skillswap.application.contratacion;

import ar.edu.uade.skillswap.application.contratacion.dto.ContratacionDTO;
import ar.edu.uade.skillswap.application.contratacion.dto.SolicitarContratacionCommand;
import ar.edu.uade.skillswap.application.evento.PublicadorEventos;
import ar.edu.uade.skillswap.application.shared.exception.PermisoDenegadoException;
import ar.edu.uade.skillswap.application.shared.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.domain.contratacion.Contratacion;
import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.contratacion.Solicitud;
import ar.edu.uade.skillswap.domain.contratacion.exception.AccionNoPermitidaException;
import ar.edu.uade.skillswap.domain.contratacion.exception.AutoSolicitudException;
import ar.edu.uade.skillswap.domain.contratacion.exception.CalificacionDuplicadaException;
import ar.edu.uade.skillswap.domain.contratacion.strategy.PoliticaReintegroFactory;
import ar.edu.uade.skillswap.domain.contratacion.strategy.ReintegroPorAnticipacion;
import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;
import ar.edu.uade.skillswap.domain.perfil.exception.PerfilInactivoException;
import ar.edu.uade.skillswap.domain.publicacion.FranjaId;
import ar.edu.uade.skillswap.domain.publicacion.Modalidad;
import ar.edu.uade.skillswap.domain.publicacion.Publicacion;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionId;
import ar.edu.uade.skillswap.domain.publicacion.TipoPublicacion;
import ar.edu.uade.skillswap.domain.publicacion.decorator.DescuentoPorcentual;
import ar.edu.uade.skillswap.domain.publicacion.exception.CupoAgotadoException;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.shared.TransicionInvalidaException;
import ar.edu.uade.skillswap.domain.wallet.RetencionDePagos;
import ar.edu.uade.skillswap.domain.wallet.Wallet;
import ar.edu.uade.skillswap.domain.wallet.exception.SaldoInsuficienteException;
import ar.edu.uade.skillswap.infrastructure.memoria.ContratacionRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.PerfilRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.PublicacionRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.UnidadDeTrabajoEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.WalletRepositoryEnMemoria;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Casos de uso de la contratación con repositorios en memoria. Cubre los casos de aceptación
 * 3 (solicitar, aceptar y pagar), 4 (último cupo), 5 (saldo insuficiente), 6 (dos políticas de
 * reintegro) y 10 (completar y calificar una vez), más los casos negativos que pide la consigna.
 */
class ContratacionServiceTest {

    private static final String PROVEEDOR = "user-ana";
    private static final String CLIENTE = "user-mateo";
    private static final String OTRO_CLIENTE = "user-lucia";
    private static final String AJENO = "user-otro";
    private static final Instant AHORA = Instant.parse("2026-11-16T12:00:00Z");
    private static final long PRECIO = 100;
    private static final long SALDO_INICIAL = 500;

    private final UnidadDeTrabajoEnMemoria unidadDeTrabajo = new UnidadDeTrabajoEnMemoria();
    private final PublicacionRepositoryEnMemoria publicaciones = new PublicacionRepositoryEnMemoria(unidadDeTrabajo);
    private final ContratacionRepositoryEnMemoria contrataciones = new ContratacionRepositoryEnMemoria(unidadDeTrabajo);
    private final WalletRepositoryEnMemoria wallets = new WalletRepositoryEnMemoria(unidadDeTrabajo);
    private final PerfilRepositoryEnMemoria perfiles = new PerfilRepositoryEnMemoria(unidadDeTrabajo);
    private final PublicadorEventos publicador = new PublicadorEventos();
    private final List<String> eventos = new ArrayList<>();
    private final ContratacionService service = new ContratacionService(contrataciones, publicaciones, wallets,
            perfiles, new RetencionDePagos(),
            new PoliticaReintegroFactory(ReintegroPorAnticipacion.conUmbralesPorDefecto()),
            unidadDeTrabajo, publicador, Clock.fixed(AHORA, ZoneOffset.UTC));

    private Publicacion publicacion;
    /** Empieza en 72 h: si cancela el cliente, recupera el 100 %. */
    private FranjaId en72Horas;
    /** Empieza en 30 h: si cancela el cliente, recupera el 50 %. */
    private FranjaId en30Horas;

    @BeforeEach
    void prepararDatos() {
        for (var usuario : List.of(PROVEEDOR, CLIENTE, OTRO_CLIENTE)) {
            perfiles.save(Perfil.crear(usuario, TipoPerfil.PROFESIONAL, usuario, ZoneId.of("UTC")));
        }
        wallets.save(Wallet.abrir(CLIENTE, new SkillCoins(SALDO_INICIAL), AHORA));
        wallets.save(Wallet.abrir(OTRO_CLIENTE, new SkillCoins(SALDO_INICIAL), AHORA));
        wallets.save(Wallet.abrir(PROVEEDOR, new SkillCoins(0), AHORA));

        publicacion = Publicacion.crearBorrador(PROVEEDOR, TipoPublicacion.OFERTA);
        publicacion.editar("Mentoría de Java", "Revisión de código", "Programación", Modalidad.REMOTA,
                Duration.ofHours(1), 1, new SkillCoins(PRECIO), Set.of("java"));
        en72Horas = publicacion.agregarFranja(AHORA.plus(Duration.ofHours(72)), AHORA.plus(Duration.ofHours(73)));
        en30Horas = publicacion.agregarFranja(AHORA.plus(Duration.ofHours(30)), AHORA.plus(Duration.ofHours(31)));
        publicacion.publicar(PROVEEDOR, AHORA);
        publicaciones.save(publicacion);

        publicador.suscribir(evento -> eventos.add(evento.tipo()));
    }

    // ---------- ayudas para leer el estado guardado y armar el recorrido

    private ContratacionDTO solicitar(String cliente, FranjaId franja) {
        return service.solicitar(new SolicitarContratacionCommand(cliente, publicacion.id().valor().toString(),
                franja.valor().toString(), "Hola, quiero revisar mi proyecto", "Mejorar mis tests"));
    }

    private ContratacionDTO aceptada(FranjaId franja) {
        var contratacion = solicitar(CLIENTE, franja);
        return service.aceptar(PROVEEDOR, contratacion.id());
    }

    private ContratacionDTO confirmada(FranjaId franja) {
        var contratacion = aceptada(franja);
        return service.pagar(CLIENTE, contratacion.id());
    }

    private ContratacionDTO completada() {
        var contratacion = confirmada(en72Horas);
        service.iniciar(PROVEEDOR, contratacion.id());
        return service.completar(PROVEEDOR, contratacion.id());
    }

    private long saldo(String titular) {
        return wallets.findByTitular(titular).orElseThrow().saldo().valor();
    }

    private long garantia() {
        return wallets.garantia().saldo().valor();
    }

    private int reservados(FranjaId franja) {
        return publicaciones.findById(publicacion.id()).orElseThrow().franjas().stream()
                .filter(f -> f.id().equals(franja)).findFirst().orElseThrow().reservados();
    }

    private String estadoGuardado(String contratacionId) {
        return contrataciones.findById(new ContratacionId(UUID.fromString(contratacionId)))
                .orElseThrow().estado().nombre();
    }

    @Nested
    class Solicitar {

        @Test
        void quedaPendienteConElPrecioDeLaPublicacionYAvisaAlProveedor() {
            var contratacion = solicitar(CLIENTE, en72Horas);

            assertEquals("PENDIENTE", contratacion.estado());
            assertEquals(PRECIO, contratacion.precioAcordado());
            assertEquals(PROVEEDOR, contratacion.proveedorId());
            assertEquals("Mentoría de Java", contratacion.tituloPublicacion());
            assertEquals(AHORA.plus(Duration.ofHours(72)), contratacion.inicioSesion());
            assertEquals("PENDIENTE", estadoGuardado(contratacion.id()));
            assertEquals(0, reservados(en72Horas), "el cupo se reserva al aceptar, no al solicitar");
            assertEquals(List.of("SOLICITUD_RECIBIDA"), eventos);
        }

        @Test
        void usaElPrecioFinalConPromociones() {
            publicacion.agregarPromocion(new DescuentoPorcentual(10));
            publicaciones.save(publicacion);

            assertEquals(90, solicitar(CLIENTE, en72Horas).precioAcordado());
        }

        @Test
        void elAutorNoPuedeSolicitarSuPropiaPublicacion() {
            assertThrows(AutoSolicitudException.class, () -> solicitar(PROVEEDOR, en72Horas));
            assertTrue(eventos.isEmpty());
        }

        @Test
        void unaPublicacionPausadaNoAdmiteSolicitudes() {
            publicacion.pausar(PROVEEDOR, "vacaciones", AHORA);
            publicaciones.save(publicacion);

            assertThrows(TransicionInvalidaException.class, () -> solicitar(CLIENTE, en72Horas));
        }

        @Test
        void unaFranjaSinLugarNoAdmiteSolicitudes() {
            aceptada(en72Horas);

            assertThrows(CupoAgotadoException.class, () -> solicitar(OTRO_CLIENTE, en72Horas));
        }

        @Test
        void unClienteDadoDeBajaOSinWalletNoPuedeSolicitar() {
            var mateo = perfiles.findByUserId(CLIENTE).orElseThrow();
            mateo.darDeBaja(CLIENTE, "me voy", AHORA);
            perfiles.save(mateo);
            perfiles.save(Perfil.crear("user-sin-wallet", TipoPerfil.PROFESIONAL, "Sin wallet", ZoneId.of("UTC")));

            assertThrows(PerfilInactivoException.class, () -> solicitar(CLIENTE, en72Horas));
            assertThrows(RecursoNoEncontradoException.class, () -> solicitar("user-sin-wallet", en72Horas));
            assertTrue(eventos.isEmpty());
        }

        @Test
        void unaPublicacionInexistenteSeInformaComoNoEncontrada() {
            var command = new SolicitarContratacionCommand(CLIENTE, UUID.randomUUID().toString(),
                    en72Horas.valor().toString(), "Hola", "Aprender");

            assertThrows(RecursoNoEncontradoException.class, () -> service.solicitar(command));
            assertThrows(RecursoNoEncontradoException.class, () -> service.aceptar(PROVEEDOR, "no-es-un-id"));
        }
    }

    @Nested
    class AceptarYPagar {

        @Test
        void caso3_solicitarAceptarYPagarConSaldoSuficiente() {
            var contratacion = confirmada(en72Horas);

            assertEquals("CONFIRMADA", contratacion.estado());
            assertEquals(SALDO_INICIAL - PRECIO, saldo(CLIENTE));
            assertEquals(PRECIO, garantia(), "el pago queda retenido hasta completar");
            assertEquals(0, saldo(PROVEEDOR));
            assertEquals(1, reservados(en72Horas));
            assertEquals(List.of("SOLICITUD_RECIBIDA", "CONTRATACION_ACEPTADA", "PAGO_CONFIRMADO"), eventos);
            assertEquals(List.of("ACEPTADA", "CONFIRMADA"),
                    contratacion.historial().stream().map(cambio -> cambio.hacia()).toList());
            assertEquals(List.of(PROVEEDOR, CLIENTE),
                    contratacion.historial().stream().map(cambio -> cambio.actorId()).toList());
        }

        @Test
        void caso4_dosAceptacionesNoConsumenElUltimoCupo() {
            var primera = solicitar(CLIENTE, en72Horas);
            var segunda = solicitar(OTRO_CLIENTE, en72Horas);

            service.aceptar(PROVEEDOR, primera.id());

            assertThrows(CupoAgotadoException.class, () -> service.aceptar(PROVEEDOR, segunda.id()));
            assertEquals("PENDIENTE", estadoGuardado(segunda.id()), "la aceptación rechazada no cambió nada");
            assertEquals(1, reservados(en72Horas));
        }

        @Test
        void conLaPublicacionPausadaSeAceptaUnaSolicitudQueYaHabiaLlegado() {
            var contratacion = solicitar(CLIENTE, en72Horas);
            publicacion = publicaciones.findById(publicacion.id()).orElseThrow();
            publicacion.pausar(PROVEEDOR, "vacaciones", AHORA);
            publicaciones.save(publicacion);

            var aceptada = service.aceptar(PROVEEDOR, contratacion.id());

            assertEquals("ACEPTADA", aceptada.estado());
            assertEquals(1, reservados(en72Horas));
            assertThrows(TransicionInvalidaException.class, () -> solicitar(OTRO_CLIENTE, en30Horas),
                    "pausada no admite solicitudes nuevas");
        }

        @Test
        void conLaPublicacionCerradaNoSeAceptaNiSeReprograma() {
            var pendiente = solicitar(CLIENTE, en72Horas);
            var aceptada = solicitar(OTRO_CLIENTE, en72Horas);
            service.aceptar(PROVEEDOR, aceptada.id());
            publicacion = publicaciones.findById(publicacion.id()).orElseThrow();
            publicacion.cerrar(PROVEEDOR, "terminó el ciclo", AHORA);
            publicaciones.save(publicacion);

            assertThrows(TransicionInvalidaException.class, () -> service.aceptar(PROVEEDOR, pendiente.id()));
            assertEquals("PENDIENTE", estadoGuardado(pendiente.id()));
            assertThrows(TransicionInvalidaException.class,
                    () -> service.proponerReprogramacion(OTRO_CLIENTE, aceptada.id(), en30Horas.valor().toString()));
        }

        @Test
        void soloElProveedorAcepta() {
            var contratacion = solicitar(CLIENTE, en72Horas);

            assertThrows(AccionNoPermitidaException.class, () -> service.aceptar(CLIENTE, contratacion.id()));
            assertEquals(0, reservados(en72Horas));
        }

        @Test
        void caso5_unPagoConSaldoInsuficienteNoModificaSaldosNiEstado() {
            wallets.save(Wallet.reconstruir(CLIENTE, new SkillCoins(50)));
            var contratacion = aceptada(en72Horas);
            var movimientosAntes = wallets.findMovimientos(CLIENTE, PaginaSolicitada.primera(10)).total();
            eventos.clear();

            assertThrows(SaldoInsuficienteException.class, () -> service.pagar(CLIENTE, contratacion.id()));

            assertEquals("ACEPTADA", estadoGuardado(contratacion.id()));
            assertEquals(50, saldo(CLIENTE));
            assertEquals(0, garantia());
            assertEquals(movimientosAntes, wallets.findMovimientos(CLIENTE, PaginaSolicitada.primera(10)).total());
            assertTrue(eventos.isEmpty(), "un pago rechazado no notifica");
        }

        @Test
        void unSegundoPagoSeRechazaSinCobrarDosVeces() {
            var contratacion = confirmada(en72Horas);

            assertThrows(TransicionInvalidaException.class, () -> service.pagar(CLIENTE, contratacion.id()));
            assertEquals(SALDO_INICIAL - PRECIO, saldo(CLIENTE));
            assertEquals(PRECIO, garantia());
        }

        @Test
        void soloElClientePaga() {
            var contratacion = aceptada(en72Horas);

            assertThrows(AccionNoPermitidaException.class, () -> service.pagar(PROVEEDOR, contratacion.id()));
            assertEquals(SALDO_INICIAL, saldo(CLIENTE));
        }

        @Test
        void unaContratacionGratuitaSePagaYCompletaSinMoverSaldos() {
            publicacion.agregarPromocion(new DescuentoPorcentual(100));
            publicaciones.save(publicacion);

            var contratacion = completada();

            assertEquals(0, contratacion.precioAcordado());
            assertEquals("COMPLETADA", contratacion.estado());
            assertEquals(SALDO_INICIAL, saldo(CLIENTE));
            assertEquals(0, saldo(PROVEEDOR));
            assertEquals(0, garantia());
        }

        @Test
        void rechazarNoReservaCupoNiMueveFondos() {
            var contratacion = solicitar(CLIENTE, en72Horas);

            var rechazada = service.rechazar(PROVEEDOR, contratacion.id(), "No tengo lugar esa semana");

            assertEquals("RECHAZADA", rechazada.estado());
            assertEquals(0, reservados(en72Horas));
            assertEquals(SALDO_INICIAL, saldo(CLIENTE));
        }
    }

    @Nested
    class Completar {

        @Test
        void elProveedorCobraLoRetenidoAlCompletar() {
            var contratacion = completada();

            assertEquals("COMPLETADA", contratacion.estado());
            assertEquals(PRECIO, saldo(PROVEEDOR));
            assertEquals(0, garantia());
            assertEquals(SALDO_INICIAL - PRECIO, saldo(CLIENTE));
            assertTrue(eventos.contains("SESION_COMPLETADA"));
        }

        @Test
        void noSePuedeCompletarSinIniciar() {
            var contratacion = confirmada(en72Horas);

            assertThrows(TransicionInvalidaException.class, () -> service.completar(PROVEEDOR, contratacion.id()));
            assertEquals(PRECIO, garantia());
            assertEquals(0, saldo(PROVEEDOR));
        }
    }

    @Nested
    class Cancelar {

        @Test
        void caso6_elClienteCancelaCon72HorasYRecuperaTodo() {
            var contratacion = confirmada(en72Horas);

            var cancelada = service.cancelar(CLIENTE, contratacion.id(), "Me surgió un viaje");

            assertEquals("CANCELADA", cancelada.estado());
            assertEquals(PRECIO, cancelada.reintegro().alCliente());
            assertEquals(0, cancelada.reintegro().alProveedor());
            assertEquals(SALDO_INICIAL, saldo(CLIENTE));
            assertEquals(0, saldo(PROVEEDOR));
            assertEquals(0, garantia());
            assertEquals(0, reservados(en72Horas), "el cupo se libera");
        }

        @Test
        void caso6_elClienteCancelaCon30HorasYRecuperaLaMitad() {
            var contratacion = confirmada(en30Horas);

            var cancelada = service.cancelar(CLIENTE, contratacion.id(), "Me surgió un viaje");

            assertEquals(50, cancelada.reintegro().alCliente());
            assertEquals(50, cancelada.reintegro().alProveedor());
            assertEquals(SALDO_INICIAL - 50, saldo(CLIENTE));
            assertEquals(50, saldo(PROVEEDOR));
            assertEquals(0, garantia());
        }

        @Test
        void caso6_siCancelaElProveedorElClienteRecuperaTodoAunqueFalteMenosDe48Horas() {
            var contratacion = confirmada(en30Horas);

            var cancelada = service.cancelar(PROVEEDOR, contratacion.id(), "Estoy enfermo");

            assertEquals(PRECIO, cancelada.reintegro().alCliente());
            assertEquals(SALDO_INICIAL, saldo(CLIENTE));
            assertEquals(0, saldo(PROVEEDOR));
            assertTrue(eventos.contains("CONTRATACION_CANCELADA"));
        }

        @Test
        void antesDelPagoNoSeMuevenFondosPeroSeLiberaElCupo() {
            var contratacion = aceptada(en72Horas);

            var cancelada = service.cancelar(CLIENTE, contratacion.id(), "Ya no lo necesito");

            assertEquals(0, cancelada.reintegro().alCliente());
            assertEquals(SALDO_INICIAL, saldo(CLIENTE));
            assertEquals(0, reservados(en72Horas));
        }

        @Test
        void retirarUnaSolicitudPendienteNoTocaElCupoDeOtros() {
            aceptada(en72Horas);
            var pendiente = solicitar(OTRO_CLIENTE, en30Horas);

            service.cancelar(OTRO_CLIENTE, pendiente.id(), "Me arrepentí");

            assertEquals(1, reservados(en72Horas));
            assertEquals(0, reservados(en30Horas));
        }

        @Test
        void siAlgoFallaDespuesDeMoverFondosNoQuedaNadaGuardado() {
            // Una contratación pagada cuya publicación no está en el repositorio: cancelar reparte los
            // fondos y recién después falla al buscar la publicación para liberar el cupo.
            var huerfana = Contratacion.solicitar(CLIENTE, PROVEEDOR, PublicacionId.generate(),
                    new Solicitud("Hola", "Aprender", en72Horas), AHORA.plus(Duration.ofHours(72)),
                    new SkillCoins(PRECIO), AHORA);
            huerfana.aceptar(PROVEEDOR, AHORA);
            huerfana.pagar(CLIENTE, AHORA);
            contrataciones.save(huerfana);
            wallets.save(Wallet.reconstruir(Wallet.TITULAR_GARANTIA, new SkillCoins(PRECIO)));
            var id = huerfana.id().valor().toString();

            assertThrows(RecursoNoEncontradoException.class, () -> service.cancelar(CLIENTE, id, "Me voy"));

            assertEquals("CONFIRMADA", estadoGuardado(id));
            assertEquals(PRECIO, garantia());
            assertEquals(SALDO_INICIAL, saldo(CLIENTE));
            assertEquals(0, saldo(PROVEEDOR));
        }

        @Test
        void unTerceroNoPuedeCancelar() {
            var contratacion = confirmada(en72Horas);

            assertThrows(AccionNoPermitidaException.class,
                    () -> service.cancelar(AJENO, contratacion.id(), "Porque sí"));
            assertEquals("CONFIRMADA", estadoGuardado(contratacion.id()));
            assertEquals(PRECIO, garantia());
        }
    }

    @Nested
    class Reprogramar {

        @Test
        void alAceptarseCambiaLaFranjaYMueveElCupo() {
            var contratacion = aceptada(en72Horas);

            service.proponerReprogramacion(CLIENTE, contratacion.id(), en30Horas.valor().toString());
            var reprogramada = service.aceptarReprogramacion(PROVEEDOR, contratacion.id());

            assertEquals(en30Horas.valor().toString(), reprogramada.franjaId());
            assertEquals(AHORA.plus(Duration.ofHours(30)), reprogramada.inicioSesion());
            assertNull(reprogramada.propuesta());
            assertEquals(0, reservados(en72Horas));
            assertEquals(1, reservados(en30Horas));
            assertTrue(eventos.containsAll(List.of("REPROGRAMACION_PROPUESTA", "REPROGRAMACION_ACEPTADA")));
        }

        @Test
        void siLaFranjaNuevaSeLlenoLaReprogramacionNoCambiaNada() {
            var contratacion = aceptada(en72Horas);
            service.proponerReprogramacion(CLIENTE, contratacion.id(), en30Horas.valor().toString());
            var otra = solicitar(OTRO_CLIENTE, en30Horas);
            service.aceptar(PROVEEDOR, otra.id());

            assertThrows(CupoAgotadoException.class,
                    () -> service.aceptarReprogramacion(PROVEEDOR, contratacion.id()));

            var guardada = service.verContratacion(CLIENTE, contratacion.id());
            assertEquals(en72Horas.valor().toString(), guardada.franjaId());
            assertNotNull(guardada.propuesta());
            assertEquals(1, reservados(en72Horas));
        }

        @Test
        void conLaPublicacionPausadaSePuedeReprogramar() {
            var contratacion = confirmada(en72Horas);
            publicacion = publicaciones.findById(publicacion.id()).orElseThrow();
            publicacion.pausar(PROVEEDOR, "vacaciones", AHORA);
            publicaciones.save(publicacion);

            service.proponerReprogramacion(PROVEEDOR, contratacion.id(), en30Horas.valor().toString());
            var reprogramada = service.aceptarReprogramacion(CLIENTE, contratacion.id());

            assertEquals(en30Horas.valor().toString(), reprogramada.franjaId());
            assertEquals(1, reservados(en30Horas));
            assertEquals(0, reservados(en72Horas));
        }

        @Test
        void rechazarLaPropuestaLaDescarta() {
            var contratacion = aceptada(en72Horas);
            service.proponerReprogramacion(CLIENTE, contratacion.id(), en30Horas.valor().toString());

            var sinPropuesta = service.rechazarReprogramacion(PROVEEDOR, contratacion.id());

            assertNull(sinPropuesta.propuesta());
            assertEquals(en72Horas.valor().toString(), sinPropuesta.franjaId());
        }
    }

    @Nested
    class Calificar {

        @Test
        void caso10_cadaParteCalificaUnaVezYSeRecalculaLaReputacion() {
            var contratacion = completada();

            var calificacion = service.calificar(CLIENTE, contratacion.id(), 5, "Excelente");
            service.calificar(PROVEEDOR, contratacion.id(), 4, null);

            assertEquals(PROVEEDOR, calificacion.destinatarioId());
            var ana = perfiles.findByUserId(PROVEEDOR).orElseThrow().reputacion();
            assertEquals(5.0, ana.promedio());
            assertEquals(1, ana.cantidad());
            assertEquals(4.0, perfiles.findByUserId(CLIENTE).orElseThrow().reputacion().promedio());
            assertEquals(2, service.verContratacion(CLIENTE, contratacion.id()).calificaciones().size());
        }

        @Test
        void caso10_unaSegundaCalificacionSeRechazaSinTocarLaReputacion() {
            var contratacion = completada();
            service.calificar(CLIENTE, contratacion.id(), 5, "Excelente");

            assertThrows(CalificacionDuplicadaException.class,
                    () -> service.calificar(CLIENTE, contratacion.id(), 1, "Cambié de opinión"));
            assertEquals(1, perfiles.findByUserId(PROVEEDOR).orElseThrow().reputacion().cantidad());
        }

        @Test
        void unTerceroNoPuedeCalificar() {
            var contratacion = completada();

            assertThrows(AccionNoPermitidaException.class,
                    () -> service.calificar(AJENO, contratacion.id(), 5, null));
            assertEquals(0, perfiles.findByUserId(PROVEEDOR).orElseThrow().reputacion().cantidad());
        }

        @Test
        void noSeCalificaUnaContratacionSinCompletar() {
            var contratacion = confirmada(en72Horas);

            assertThrows(TransicionInvalidaException.class,
                    () -> service.calificar(CLIENTE, contratacion.id(), 5, null));
        }
    }

    @Nested
    class Consultas {

        @Test
        void soloLosParticipantesVenElDetalle() {
            var contratacion = confirmada(en72Horas);

            var detalle = service.verContratacion(PROVEEDOR, contratacion.id());

            assertEquals(2, detalle.historial().size());
            assertEquals("PENDIENTE", detalle.historial().get(0).desde());
            assertEquals("CONFIRMADA", detalle.historial().get(1).hacia());
            assertThrows(PermisoDenegadoException.class, () -> service.verContratacion(AJENO, contratacion.id()));
        }

        @Test
        void listaLasDelUsuarioPaginadasDeLaMasNuevaALaMasVieja() {
            var primera = solicitar(CLIENTE, en72Horas);
            var segunda = solicitar(CLIENTE, en30Horas);
            solicitar(OTRO_CLIENTE, en30Horas);

            var pagina = service.listarContrataciones(CLIENTE, PaginaSolicitada.primera(1));

            assertEquals(2, pagina.total());
            assertEquals(List.of(segunda.id()), pagina.elementos().stream().map(ContratacionDTO::id).toList());
            assertEquals(List.of(primera.id()), service.listarContrataciones(CLIENTE, new PaginaSolicitada(1, 1))
                    .elementos().stream().map(ContratacionDTO::id).toList());
            assertEquals(3, service.listarContrataciones(PROVEEDOR, PaginaSolicitada.primera(10)).total());
        }
    }
}
