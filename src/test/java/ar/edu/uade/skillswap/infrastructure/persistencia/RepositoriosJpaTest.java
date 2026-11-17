package ar.edu.uade.skillswap.infrastructure.persistencia;

import ar.edu.uade.skillswap.domain.contratacion.Contratacion;
import ar.edu.uade.skillswap.domain.contratacion.Solicitud;
import ar.edu.uade.skillswap.domain.contratacion.strategy.ReintegroPorAnticipacion;
import ar.edu.uade.skillswap.domain.notificacion.Notificacion;
import ar.edu.uade.skillswap.domain.perfil.CanalNotificacion;
import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;
import ar.edu.uade.skillswap.domain.publicacion.Modalidad;
import ar.edu.uade.skillswap.domain.publicacion.Publicacion;
import ar.edu.uade.skillswap.domain.publicacion.TipoPublicacion;
import ar.edu.uade.skillswap.domain.publicacion.decorator.DescuentoFijo;
import ar.edu.uade.skillswap.domain.publicacion.decorator.DescuentoPorcentual;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.domain.shared.Puntaje;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.wallet.Movimiento;
import ar.edu.uade.skillswap.domain.wallet.RetencionDePagos;
import ar.edu.uade.skillswap.domain.wallet.TipoMovimiento;
import ar.edu.uade.skillswap.domain.wallet.Wallet;
import ar.edu.uade.skillswap.domain.contratacion.evento.SolicitudRecibida;
import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Guardar y volver a leer cada agregado con todos sus datos, y los listados paginados de los repositorios. */
class RepositoriosJpaTest extends BaseDeDatosDePrueba {

    private static final String ANA = "user-ana";
    private static final String MATEO = "user-mateo";

    @Test
    void unPerfilVuelveConTodosSusDatos() {
        var perfil = Perfil.crear(ANA, TipoPerfil.ORGANIZACION, "Ana", ZoneId.of("America/Argentina/Buenos_Aires"));
        perfil.editarDatos("Ana López", "Mentora de Java", ZoneId.of("America/Argentina/Buenos_Aires"));
        perfil.definirHabilidades(Set.of("Java", "SQL"));
        perfil.definirIntereses(Set.of("Diseño"));
        perfil.agregarDisponibilidad(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));
        perfil.agregarDisponibilidad(DayOfWeek.FRIDAY, LocalTime.of(14, 30), LocalTime.of(18, 0));
        perfil.elegirCanal(CanalNotificacion.PUSH);
        perfil.registrarCalificacion(new Puntaje(5));
        perfil.registrarCalificacion(new Puntaje(4));
        perfiles.save(perfil);
        perfil.darDeBaja(ANA, "Se muda", AHORA);
        perfiles.save(perfil);

        var leido = perfiles.findByUserId(ANA).orElseThrow();

        assertEquals(TipoPerfil.ORGANIZACION, leido.tipo());
        assertEquals("Ana López", leido.nombreVisible());
        assertEquals("Mentora de Java", leido.biografia());
        assertEquals(ZoneId.of("America/Argentina/Buenos_Aires"), leido.zonaHoraria());
        assertEquals(Set.of("Java", "SQL"), leido.habilidades());
        assertEquals(Set.of("Diseño"), leido.intereses());
        assertEquals(perfil.disponibilidad(), leido.disponibilidad());
        assertEquals(CanalNotificacion.PUSH, leido.canal());
        assertEquals(perfil.reputacion(), leido.reputacion());
        assertFalse(leido.estaActivo());
        assertEquals(perfil.historial(), leido.historial());
    }

    @Test
    void quitarDisponibilidadYCambiarHabilidadesSeGuarda() {
        var perfil = guardarPerfil(ANA);
        perfil.agregarDisponibilidad(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        perfil.agregarDisponibilidad(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        perfil.definirHabilidades(Set.of("Java"));
        perfiles.save(perfil);

        var leido = perfiles.findByUserId(ANA).orElseThrow();
        leido.quitarDisponibilidad(leido.disponibilidad().get(0));
        leido.definirHabilidades(Set.of("Python"));
        perfiles.save(leido);

        var final_ = perfiles.findByUserId(ANA).orElseThrow();
        assertEquals(List.of(DayOfWeek.TUESDAY), final_.disponibilidad().stream().map(d -> d.dia()).toList());
        assertEquals(Set.of("Python"), final_.habilidades());
    }

    @Test
    void findByUserIdsTraeSoloLosQueExisten() {
        guardarPerfil(ANA);
        guardarPerfil(MATEO);

        var encontrados = perfiles.findByUserIds(List.of(ANA, MATEO, "user-inexistente", ANA));

        assertEquals(Set.of(ANA, MATEO), Set.copyOf(encontrados.stream().map(Perfil::userId).toList()));
        assertTrue(perfiles.findByUserId("user-inexistente").isEmpty());
    }

    @Test
    void unaPublicacionVuelveConFranjasPromocionesEtiquetasEHistorial() {
        guardarPerfil(ANA);
        var publicacion = Publicacion.crearBorrador(ANA, TipoPublicacion.DESAFIO);
        publicacion.editar("Mentoría de Java", "Revisión de código", "Programación", Modalidad.HIBRIDA,
                Duration.ofMinutes(90), 2, new SkillCoins(100), Set.of("java", "backend"));
        var primera = publicacion.agregarFranja(AHORA.plus(Duration.ofDays(1)), AHORA.plus(Duration.ofDays(1)).plusSeconds(3600));
        publicacion.agregarFranja(AHORA.plus(Duration.ofDays(2)), AHORA.plus(Duration.ofDays(2)).plusSeconds(3600));
        publicacion.agregarPromocion(new DescuentoPorcentual(10));
        publicacion.agregarPromocion(new DescuentoFijo(new SkillCoins(20)));
        publicacion.publicar(ANA, AHORA);
        publicacion.reservarCupo(primera);
        publicacion.pausar(ANA, "Vacaciones", AHORA.plusSeconds(60));
        publicaciones.save(publicacion);

        var leida = publicaciones.findById(publicacion.id()).orElseThrow();

        assertEquals(ANA, leida.autorId());
        assertEquals(TipoPublicacion.DESAFIO, leida.tipo());
        assertEquals("Mentoría de Java", leida.titulo());
        assertEquals("Revisión de código", leida.descripcion());
        assertEquals("Programación", leida.categoria());
        assertEquals(Modalidad.HIBRIDA, leida.modalidad());
        assertEquals(Duration.ofMinutes(90), leida.duracionEstimada());
        assertEquals(2, leida.cupo());
        assertEquals(new SkillCoins(100), leida.precioBase());
        assertEquals(Set.of("java", "backend"), leida.etiquetas());
        assertEquals("PAUSADA", leida.estado().nombre());
        assertEquals(List.of(new DescuentoPorcentual(10), new DescuentoFijo(new SkillCoins(20))), leida.promociones());
        assertEquals(new SkillCoins(70), leida.precioFinal(), "100 con 10 % y después −20");
        assertEquals(publicacion.historial(), leida.historial());
        assertEquals(2, leida.franjas().size());
        for (int i = 0; i < 2; i++) {
            assertEquals(publicacion.franjas().get(i).id(), leida.franjas().get(i).id());
            assertEquals(publicacion.franjas().get(i).inicio(), leida.franjas().get(i).inicio());
            assertEquals(publicacion.franjas().get(i).fin(), leida.franjas().get(i).fin());
            assertEquals(publicacion.franjas().get(i).reservados(), leida.franjas().get(i).reservados());
        }
    }

    @Test
    void cambiarLasPromocionesYCancelarSeGuarda() {
        guardarPerfil(ANA);
        var publicacion = guardarPublicada(ANA, "Mentoría", "Programación", Modalidad.REMOTA, 100, Set.of(), AHORA);
        publicacion.agregarPromocion(new DescuentoFijo(new SkillCoins(10)));
        publicacion.agregarPromocion(new DescuentoPorcentual(50));
        publicaciones.save(publicacion);

        var leida = publicaciones.findById(publicacion.id()).orElseThrow();
        leida.quitarPromocion(new DescuentoFijo(new SkillCoins(10)));
        leida.cancelar(ANA, "Ya no la doy", AHORA.plusSeconds(10));
        publicaciones.save(leida);

        var final_ = publicaciones.findById(publicacion.id()).orElseThrow();
        assertEquals(List.of(new DescuentoPorcentual(50)), final_.promociones());
        assertEquals("CANCELADA", final_.estado().nombre());
        assertEquals("Ya no la doy", final_.motivoCancelacion());
    }

    @Test
    void unBorradorIncompletoSeGuardaConNulos() {
        guardarPerfil(ANA);
        var borrador = Publicacion.crearBorrador(ANA, TipoPublicacion.OFERTA);
        publicaciones.save(borrador);

        var leido = publicaciones.findById(borrador.id()).orElseThrow();

        assertEquals("BORRADOR", leido.estado().nombre());
        assertNull(leido.titulo());
        assertNull(leido.precioBase());
        assertNull(leido.modalidad());
        assertTrue(leido.franjas().isEmpty());
    }

    @Test
    void listadosDePublicaciones() {
        guardarPerfil(ANA);
        guardarPerfil(MATEO);
        var java = guardarPublicada(ANA, "Java", "Programación", Modalidad.REMOTA, 100, Set.of(), AHORA);
        var sql = guardarPublicada(ANA, "SQL", "Datos", Modalidad.REMOTA, 100, Set.of(), AHORA);
        var borrador = Publicacion.crearBorrador(ANA, TipoPublicacion.OFERTA);
        publicaciones.save(borrador);
        var deMateo = guardarPublicada(MATEO, "Excel", "Oficina", Modalidad.PRESENCIAL, 50, Set.of(), AHORA);

        var primeraPagina = publicaciones.findByAutor(ANA, new PaginaSolicitada(0, 2));
        var segundaPagina = publicaciones.findByAutor(ANA, new PaginaSolicitada(1, 2));

        assertEquals(3, primeraPagina.total());
        assertEquals(List.of(java.id(), sql.id()), primeraPagina.elementos().stream().map(Publicacion::id).toList());
        assertEquals(List.of(borrador.id()), segundaPagina.elementos().stream().map(Publicacion::id).toList());
        assertEquals(List.of(java.id(), sql.id()),
                publicaciones.findActivasByAutor(ANA).stream().map(Publicacion::id).toList());
        assertEquals(3, publicaciones.findPublicadas().size());
        assertEquals(List.of(deMateo.id(), java.id()), publicaciones.findByIds(List.of(deMateo.id(), java.id()))
                .stream().map(Publicacion::id).toList(), "en el orden pedido");
        assertEquals(List.of("Datos", "Oficina", "Programación"), publicaciones.categorias());
    }

    @Test
    void unaContratacionVuelveConPropuestaReintegroHistorialYCalificaciones() {
        guardarPerfil(ANA);
        guardarPerfil(MATEO);
        var publicacion = guardarPublicada(ANA, "Java", "Programación", Modalidad.REMOTA, 100, Set.of(), AHORA);
        var franja = publicacion.franjas().get(0);
        var contratacion = Contratacion.solicitar(MATEO, ANA, publicacion.id(),
                new Solicitud(null, "Aprender a testear", franja.id()), franja.inicio(), new SkillCoins(100), AHORA);
        contratacion.aceptar(ANA, AHORA);
        contratacion.pagar(MATEO, AHORA.plusSeconds(1));
        contratacion.cancelar(MATEO, "Me surgió un viaje", ReintegroPorAnticipacion.conUmbralesPorDefecto(),
                AHORA.plusSeconds(2));
        contrataciones.save(contratacion);

        var leida = contrataciones.findById(contratacion.id()).orElseThrow();

        assertEquals(publicacion.id(), leida.publicacionId());
        assertEquals(MATEO, leida.clienteId());
        assertEquals(ANA, leida.proveedorId());
        assertEquals(contratacion.solicitud(), leida.solicitud());
        assertEquals(franja.id(), leida.franjaId());
        assertEquals(franja.inicio(), leida.inicioSesion());
        assertEquals(new SkillCoins(100), leida.precioAcordado());
        assertEquals("CANCELADA", leida.estado().nombre());
        assertEquals(contratacion.reintegro(), leida.reintegro());
        assertNull(leida.propuesta());
        assertEquals(contratacion.historial(), leida.historial());
        assertTrue(leida.extraerEventos().isEmpty(), "los eventos ya se publicaron al guardar");
    }

    @Test
    void laPropuestaDeReprogramacionYLasCalificacionesSeGuardan() {
        guardarPerfil(ANA);
        guardarPerfil(MATEO);
        var publicacion = Publicacion.crearBorrador(ANA, TipoPublicacion.OFERTA);
        publicacion.editar("Java", "Mentoría", "Programación", Modalidad.REMOTA, Duration.ofMinutes(60), 1,
                new SkillCoins(100), Set.of());
        var franja = publicacion.agregarFranja(AHORA.plus(Duration.ofDays(3)), AHORA.plus(Duration.ofDays(3)).plusSeconds(3600));
        var otra = publicacion.agregarFranja(AHORA.plus(Duration.ofDays(4)), AHORA.plus(Duration.ofDays(4)).plusSeconds(3600));
        publicacion.publicar(ANA, AHORA);
        publicaciones.save(publicacion);
        var contratacion = Contratacion.solicitar(MATEO, ANA, publicacion.id(), new Solicitud("Hola", "Aprender", franja),
                AHORA.plus(Duration.ofDays(3)), new SkillCoins(100), AHORA);
        contratacion.aceptar(ANA, AHORA);
        contratacion.proponerReprogramacion(MATEO, otra, AHORA.plus(Duration.ofDays(4)), AHORA);
        contrataciones.save(contratacion);

        var conPropuesta = contrataciones.findById(contratacion.id()).orElseThrow();
        assertEquals(contratacion.propuesta(), conPropuesta.propuesta());

        conPropuesta.aceptarReprogramacion(ANA, AHORA);
        conPropuesta.pagar(MATEO, AHORA);
        conPropuesta.iniciar(ANA, AHORA);
        conPropuesta.completar(ANA, AHORA);
        conPropuesta.calificar(MATEO, new Puntaje(5), "Excelente", AHORA);
        contrataciones.save(conPropuesta);
        var completada = contrataciones.findById(contratacion.id()).orElseThrow();
        completada.calificar(ANA, new Puntaje(4), null, AHORA.plusSeconds(1));
        contrataciones.save(completada);

        var leida = contrataciones.findById(contratacion.id()).orElseThrow();
        assertNull(leida.propuesta());
        assertEquals(otra, leida.franjaId());
        assertEquals(franja, leida.solicitud().franjaElegida(), "la solicitud conserva la franja original");
        assertEquals(AHORA.plus(Duration.ofDays(4)), leida.inicioSesion());
        assertEquals("COMPLETADA", leida.estado().nombre());
        assertEquals(Set.copyOf(completada.calificaciones()), Set.copyOf(leida.calificaciones()));
        assertEquals(1, contrataciones.countCompletadasDe(ANA));
        assertEquals(1, contrataciones.countCompletadasDe(MATEO));
        assertEquals(0, contrataciones.countCompletadasDe("user-lucia"));
    }

    @Test
    void contratacionesDeUnParticipanteDeLaMasNuevaALaMasVieja() {
        guardarPerfil(ANA);
        guardarPerfil(MATEO);
        guardarPerfil("user-lucia");
        var publicacion = guardarPublicada(ANA, "Java", "Programación", Modalidad.REMOTA, 100, Set.of(), AHORA);
        var primera = solicitar(MATEO, publicacion);
        var segunda = solicitar(MATEO, publicacion);
        var deLucia = solicitar("user-lucia", publicacion);

        var deMateo = contrataciones.findByParticipante(MATEO, new PaginaSolicitada(0, 1));
        var deAna = contrataciones.findByParticipante(ANA, PaginaSolicitada.primera(10));

        assertEquals(2, deMateo.total());
        assertEquals(List.of(segunda.id()), deMateo.elementos().stream().map(Contratacion::id).toList());
        assertEquals(List.of(deLucia.id(), segunda.id(), primera.id()),
                deAna.elementos().stream().map(Contratacion::id).toList());
    }

    @Test
    void countAceptadasDesdeCuentaPorPublicacionSegunElHistorial() {
        guardarPerfil(ANA);
        guardarPerfil(MATEO);
        var java = guardarPublicada(ANA, "Java", "Programación", Modalidad.REMOTA, 100, Set.of(), AHORA);
        var sql = guardarPublicada(ANA, "SQL", "Datos", Modalidad.REMOTA, 100, Set.of(), AHORA);
        aceptar(solicitar(MATEO, java), AHORA.minus(Duration.ofDays(40)));
        aceptar(solicitar(MATEO, java), AHORA.minus(Duration.ofDays(5)));
        aceptar(solicitar(MATEO, java), AHORA.minus(Duration.ofDays(1)));
        aceptar(solicitar(MATEO, sql), AHORA.minus(Duration.ofDays(2)));
        solicitar(MATEO, sql);

        var aceptadas = contrataciones.countAceptadasDesde(AHORA.minus(Duration.ofDays(30)));

        assertEquals(Map.of(java.id(), 2L, sql.id(), 1L), aceptadas);
    }

    @Test
    void unaWalletGuardaElSaldoYAgregaSoloLosMovimientosNuevos() {
        guardarPerfil(ANA);
        guardarPerfil(MATEO);
        var publicacion = guardarPublicada(ANA, "Java", "Programación", Modalidad.REMOTA, 100, Set.of(), AHORA);
        var contratacion = solicitar(MATEO, publicacion);
        wallets.save(Wallet.abrir(MATEO, new SkillCoins(1000), AHORA));

        var mateo = wallets.findByTitular(MATEO).orElseThrow();
        var garantia = wallets.garantia();
        assertTrue(mateo.movimientosNuevos().isEmpty());
        new RetencionDePagos().retener(mateo, garantia, new SkillCoins(100), contratacion.id(), AHORA.plusSeconds(1));
        wallets.save(mateo);
        wallets.save(garantia);
        mateo = wallets.findByTitular(MATEO).orElseThrow();
        mateo.acreditarDemo(new SkillCoins(50), AHORA.plusSeconds(2));
        wallets.save(mateo);

        assertEquals(new SkillCoins(950), wallets.findByTitular(MATEO).orElseThrow().saldo());
        assertEquals(new SkillCoins(100), wallets.garantia().saldo());
        var movimientos = wallets.findMovimientos(MATEO, new PaginaSolicitada(0, 2));
        assertEquals(3, movimientos.total());
        assertEquals(List.of(TipoMovimiento.ACREDITACION_DEMO, TipoMovimiento.DEBITO),
                movimientos.elementos().stream().map(Movimiento::tipo).toList(), "del más reciente al más viejo");
        var debito = movimientos.elementos().get(1);
        assertEquals(new SkillCoins(100), debito.importe());
        assertEquals(new SkillCoins(900), debito.saldoResultante());
        assertEquals(contratacion.id(), debito.contratacionId());
        assertEquals(AHORA.plusSeconds(1), debito.fecha());
        assertEquals("Pago de la contratación", debito.concepto());
        assertNull(wallets.findMovimientos(MATEO, new PaginaSolicitada(1, 2)).elementos().get(0).contratacionId(),
                "el bono de bienvenida no es de una contratación");
        assertTrue(wallets.findByTitular("user-sin-wallet").isEmpty());
    }

    @Test
    void laBandejaDeNotificacionesSePaginaYMarcaLeidas() {
        var evento1 = new SolicitudRecibida(ContratacionId.generate(), MATEO, ANA, AHORA);
        var evento2 = new SolicitudRecibida(ContratacionId.generate(), MATEO, ANA, AHORA.plusSeconds(1));
        var primera = Notificacion.para(ANA, evento1);
        var segunda = Notificacion.para(ANA, evento2);
        notificaciones.save(primera);
        notificaciones.save(segunda);
        primera.marcarLeida(ANA);
        notificaciones.save(primera);

        var bandeja = notificaciones.findByDestinatario(ANA, new PaginaSolicitada(0, 1));
        var leida = notificaciones.findById(primera.id()).orElseThrow();

        assertEquals(2, bandeja.total());
        assertEquals(List.of(segunda.id()), bandeja.elementos().stream().map(Notificacion::id).toList());
        assertTrue(leida.estaLeida());
        assertEquals(primera.tipo(), leida.tipo());
        assertEquals(primera.titulo(), leida.titulo());
        assertEquals(primera.detalle(), leida.detalle());
        assertEquals(AHORA, leida.fecha());
        assertEquals(List.of(segunda.id()), notificaciones.findNoLeidas(ANA).stream().map(Notificacion::id).toList());
        assertTrue(notificaciones.findByDestinatario(MATEO, PaginaSolicitada.primera(5)).elementos().isEmpty());
    }

    private static Contratacion solicitar(String clienteId, Publicacion publicacion) {
        var franja = publicacion.franjas().get(0);
        var contratacion = Contratacion.solicitar(clienteId, publicacion.autorId(), publicacion.id(),
                new Solicitud(null, "Aprender", franja.id()), franja.inicio(), publicacion.precioFinal(), AHORA);
        contrataciones.save(contratacion);
        return contratacion;
    }

    private static void aceptar(Contratacion contratacion, Instant cuando) {
        contratacion.aceptar(contratacion.proveedorId(), cuando);
        contrataciones.save(contratacion);
    }
}
