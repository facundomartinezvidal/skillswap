package ar.edu.uade.skillswap.infrastructure.persistencia;

import ar.edu.uade.skillswap.application.busqueda.CriteriosBusqueda;
import ar.edu.uade.skillswap.application.busqueda.OrdenBusqueda;
import ar.edu.uade.skillswap.application.busqueda.recomendacion.AfinidadPorHabilidades;
import ar.edu.uade.skillswap.application.contratacion.dto.SolicitarContratacionCommand;
import ar.edu.uade.skillswap.application.perfil.dto.CrearPerfilCommand;
import ar.edu.uade.skillswap.application.publicacion.dto.DatosPublicacionCommand;
import ar.edu.uade.skillswap.application.publicacion.dto.PublicacionDTO;
import ar.edu.uade.skillswap.application.shared.exception.ConflictoDeConcurrenciaException;
import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;
import ar.edu.uade.skillswap.domain.publicacion.Modalidad;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionId;
import ar.edu.uade.skillswap.domain.publicacion.exception.CupoAgotadoException;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.domain.wallet.exception.SaldoInsuficienteException;
import ar.edu.uade.skillswap.infrastructure.configuracion.BackendEnMemoria;
import ar.edu.uade.skillswap.infrastructure.configuracion.BackendJpa;
import ar.edu.uade.skillswap.infrastructure.identidad.ServicioIdentidadSimulado;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El backend completo armado por {@link BackendJpa} contra MySQL: los mismos Services que usa la versión
 * en memoria, ahora con transacciones reales. Cubre el recorrido de la demo, el caso de aceptación 4
 * (dos aceptaciones simultáneas por el último lugar) y el 5 (pago sin saldo que no deja nada guardado).
 */
class BackendJpaTest extends BaseDeDatosDePrueba {

    private static final String ANA = "user-ana";
    private static final String MATEO = "user-mateo";
    private static final String LUCIA = "user-lucia";
    private static final long BONO = BackendEnMemoria.BONO_DEMO.valor();

    private static final RelojQueEsperaALosHilos reloj = new RelojQueEsperaALosHilos(AHORA);
    private static BackendJpa backend;

    @BeforeAll
    static void armarBackend() {
        backend = new BackendJpa(configuracion, reloj, ServicioIdentidadSimulado.conUsuariosDeDemo(),
                BackendEnMemoria.BONO_DEMO, AfinidadPorHabilidades::new);
    }

    @AfterAll
    static void cerrarBackend() {
        if (backend != null) {
            backend.close();
        }
    }

    private void crearPerfiles(String... userIds) {
        for (var userId : userIds) {
            backend.perfilService.crearPerfil(new CrearPerfilCommand(userId, TipoPerfil.PROFESIONAL, userId,
                    ZoneId.of("UTC")));
        }
    }

    /** Ana publica una mentoría con una sola franja, dentro de 3 días. */
    private PublicacionDTO publicarMentoria(int cupo, long precio) {
        var borrador = backend.publicacionService.crearBorrador(ANA, new DatosPublicacionCommand("Mentoría de Java",
                "Revisión de código", "Programación", Modalidad.REMOTA, Duration.ofHours(1), cupo, precio,
                Set.of("java")));
        backend.publicacionService.agregarFranja(ANA, borrador.id(), AHORA.plus(Duration.ofDays(3)),
                AHORA.plus(Duration.ofDays(3)).plusSeconds(3600));
        return backend.publicacionService.publicar(ANA, borrador.id());
    }

    private String solicitar(String clienteId, PublicacionDTO publicacion) {
        return backend.contratacionService.solicitar(new SolicitarContratacionCommand(clienteId, publicacion.id(),
                publicacion.franjas().get(0).id(), "Hola", "Aprender a testear")).id();
    }

    @Test
    void publicarSolicitarAceptarPagarCompletarYCalificar() {
        crearPerfiles(ANA, MATEO);
        var mentoria = publicarMentoria(2, 100);

        var encontradas = backend.busquedaService.buscar(new CriteriosBusqueda("java", "Programación",
                Modalidad.REMOTA, null, 150L, null, null, OrdenBusqueda.RELEVANCIA), PaginaSolicitada.primera(10));
        assertEquals(List.of(mentoria.id()), encontradas.elementos().stream().map(PublicacionDTO::id).toList());

        var id = solicitar(MATEO, mentoria);
        backend.contratacionService.aceptar(ANA, id);
        backend.contratacionService.pagar(MATEO, id);
        assertEquals(BONO - 100, backend.walletService.consultarSaldo(MATEO));
        assertEquals(100, backend.wallets.garantia().saldo().valor(), "el pago queda retenido");
        backend.contratacionService.iniciar(ANA, id);
        var completada = backend.contratacionService.completar(ANA, id);
        backend.contratacionService.calificar(MATEO, id, 5, "Excelente mentora");
        backend.contratacionService.calificar(ANA, id, 4, null);

        assertEquals("COMPLETADA", completada.estado());
        assertEquals(BONO + 100, backend.walletService.consultarSaldo(ANA));
        assertEquals(0, backend.wallets.garantia().saldo().valor());
        var perfilAna = backend.perfilService.verPerfil(ANA);
        assertEquals(5.0, perfilAna.reputacion());
        assertEquals(1, perfilAna.contratacionesCompletadas());
        var detalle = backend.contratacionService.verContratacion(MATEO, id);
        assertEquals(2, detalle.calificaciones().size());
        assertEquals(List.of("SESION_COMPLETADA", "CONTRATACION_ACEPTADA"), backend.notificacionService
                .listar(MATEO, PaginaSolicitada.primera(10)).elementos().stream().map(n -> n.tipo()).toList());
        assertEquals(2, backend.walletService.listarMovimientos(ANA, PaginaSolicitada.primera(10)).total(),
                "el bono de bienvenida y el cobro de la contratación");
    }

    /**
     * Caso de aceptación 4: dos hilos, cada uno en su transacción, aceptan dos solicitudes distintas sobre
     * el único lugar de la franja. El reloj hace que los dos lean el cupo libre antes de que alguno guarde.
     * Gana exactamente uno; el otro falla y su transacción no deja nada guardado.
     */
    @Test
    void dosAceptacionesSimultaneasNoConsumenDosVecesElUltimoLugar() throws Exception {
        crearPerfiles(ANA, MATEO, LUCIA);
        var mentoria = publicarMentoria(1, 100);
        var deMateo = solicitar(MATEO, mentoria);
        var deLucia = solicitar(LUCIA, mentoria);

        reloj.esperarA(2);
        var errores = Collections.synchronizedList(new ArrayList<Throwable>());
        var hilos = Executors.newFixedThreadPool(2);
        List<Callable<Object>> aceptaciones = List.of(
                () -> aceptarAnotandoElError(deMateo, errores),
                () -> aceptarAnotandoElError(deLucia, errores));
        hilos.invokeAll(aceptaciones, 30, TimeUnit.SECONDS);
        hilos.shutdown();

        assertEquals(1, errores.size(), "exactamente una de las dos aceptaciones falla: " + errores);
        var error = errores.get(0);
        assertTrue(error instanceof ConflictoDeConcurrenciaException || error instanceof CupoAgotadoException,
                "falla por concurrencia, no por otra cosa: " + error);
        var publicacion = backend.publicaciones.findById(new PublicacionId(UUID.fromString(mentoria.id()))).orElseThrow();
        assertEquals(1, publicacion.franjas().get(0).reservados(), "sin sobreventa");
        var estados = List.of(estado(deMateo), estado(deLucia));
        assertTrue(estados.contains("ACEPTADA") && estados.contains("PENDIENTE"), "estados: " + estados);
    }

    private Object aceptarAnotandoElError(String contratacionId, List<Throwable> errores) {
        try {
            backend.contratacionService.aceptar(ANA, contratacionId);
        } catch (RuntimeException e) {
            errores.add(e);
        }
        return null;
    }

    /**
     * Revisión de #20: una operación sobre la publicación que se cruza con una aceptación no puede pisar la
     * reserva del cupo. La pausa lee la publicación y queda retenida (por el reloj) antes de guardar; en
     * ese momento la aceptación termina entera y reserva el lugar. Como la pausa lee y guarda en la misma
     * transacción, el bloqueo optimista la hace fallar en lugar de dejar la contratación ACEPTADA sin su
     * lugar reservado (lo que después permitiría vender ese lugar otra vez).
     */
    @Test
    void unaPausaQueSeCruzaConUnaAceptacionNoPisaLaReserva() throws Exception {
        crearPerfiles(ANA, MATEO);
        var mentoria = publicarMentoria(1, 100);
        var deMateo = solicitar(MATEO, mentoria);
        var llego = new CountDownLatch(1);
        var soltar = new CountDownLatch(1);
        reloj.retenerAlProximoHilo(llego, soltar);

        var hilo = Executors.newSingleThreadExecutor();
        var pausa = hilo.submit(() -> backend.publicacionService.pausar(ANA, mentoria.id(), "Vacaciones"));
        assertTrue(llego.await(10, TimeUnit.SECONDS), "la pausa leyó la publicación y quedó retenida");
        backend.contratacionService.aceptar(ANA, deMateo);
        soltar.countDown();
        var error = assertThrows(ExecutionException.class, () -> pausa.get(30, TimeUnit.SECONDS));
        hilo.shutdown();

        assertInstanceOf(ConflictoDeConcurrenciaException.class, error.getCause());
        assertEquals("ACEPTADA", estado(deMateo));
        var publicacion = backend.publicaciones.findById(new PublicacionId(UUID.fromString(mentoria.id())))
                .orElseThrow();
        assertEquals(1, publicacion.franjas().get(0).reservados(), "la reserva de la aceptación sigue en pie");
        assertEquals("PUBLICADA", publicacion.estado().nombre(), "la pausa no se guardó");
    }

    /** Revisión de #20: un dato que la columna no admite llega como un error que la UI sabe mostrar. */
    @Test
    void unTituloMasLargoQueLaColumnaSeRechazaSinGuardarNada() throws SQLException {
        crearPerfiles(ANA);
        var publicacionesAntes = contarFilas("select count(*) from publicaciones");
        var titulo = "x".repeat(181);

        assertThrows(IllegalArgumentException.class, () -> backend.publicacionService.crearBorrador(ANA,
                new DatosPublicacionCommand(titulo, "Descripción", "Programación", Modalidad.REMOTA,
                        Duration.ofHours(1), 1, 100L, Set.of("java"))));

        assertEquals(publicacionesAntes, contarFilas("select count(*) from publicaciones"));
    }

    /** Caso de aceptación 5: el pago sin saldo suficiente no cambia saldos, estado ni movimientos. */
    @Test
    void unPagoSinSaldoSuficienteNoDejaNadaGuardado() throws SQLException {
        crearPerfiles(ANA, MATEO);
        var cara = publicarMentoria(1, BONO + 1);
        var id = solicitar(MATEO, cara);
        backend.contratacionService.aceptar(ANA, id);
        var movimientosAntes = contarFilas("select count(*) from movimientos_wallet");

        assertThrows(SaldoInsuficienteException.class, () -> backend.contratacionService.pagar(MATEO, id));

        assertEquals(BONO, backend.walletService.consultarSaldo(MATEO));
        assertEquals(0, backend.wallets.garantia().saldo().valor());
        assertEquals("ACEPTADA", estado(id));
        assertEquals(movimientosAntes, contarFilas("select count(*) from movimientos_wallet"));
        assertEquals(0, contarFilas("select count(*) from historial_contrataciones where hacia = 'CONFIRMADA'"));
    }

    private String estado(String contratacionId) {
        return backend.contrataciones.findById(new ContratacionId(UUID.fromString(contratacionId))).orElseThrow()
                .estado().nombre();
    }
}
