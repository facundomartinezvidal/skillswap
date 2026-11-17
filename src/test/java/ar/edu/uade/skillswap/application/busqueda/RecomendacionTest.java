package ar.edu.uade.skillswap.application.busqueda;

import ar.edu.uade.skillswap.application.busqueda.recomendacion.AfinidadPorHabilidades;
import ar.edu.uade.skillswap.application.busqueda.recomendacion.PopularidadReciente;
import ar.edu.uade.skillswap.application.publicacion.dto.PublicacionDTO;
import ar.edu.uade.skillswap.application.shared.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.domain.contratacion.Contratacion;
import ar.edu.uade.skillswap.domain.contratacion.Solicitud;
import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;
import ar.edu.uade.skillswap.domain.publicacion.Modalidad;
import ar.edu.uade.skillswap.domain.publicacion.Publicacion;
import ar.edu.uade.skillswap.domain.publicacion.TipoPublicacion;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.infrastructure.memoria.BuscadorPublicacionesEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.ContratacionRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.PerfilRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.PublicacionRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.UnidadDeTrabajoEnMemoria;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Strategy de recomendación. Cubre el caso de aceptación 7: cambiar la estrategia sin tocar el caso de uso. */
class RecomendacionTest {

    private static final Instant AHORA = Instant.parse("2026-11-16T12:00:00Z");
    private static final String MATEO = "user-mateo";

    private final UnidadDeTrabajoEnMemoria unidadDeTrabajo = new UnidadDeTrabajoEnMemoria();
    private final PublicacionRepositoryEnMemoria publicaciones = new PublicacionRepositoryEnMemoria(unidadDeTrabajo);
    private final PerfilRepositoryEnMemoria perfiles = new PerfilRepositoryEnMemoria(unidadDeTrabajo);
    private final ContratacionRepositoryEnMemoria contrataciones = new ContratacionRepositoryEnMemoria(unidadDeTrabajo);
    private final Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
    private Publicacion java;
    private Publicacion diseno;
    private Publicacion fotografia;

    @BeforeEach
    void prepararDatos() {
        var mateo = Perfil.crear(MATEO, TipoPerfil.PROFESIONAL, "Mateo", ZoneId.of("UTC"));
        mateo.definirIntereses(Set.of("java", "arquitectura"));
        perfiles.save(mateo);
        var ana = Perfil.crear("user-ana", TipoPerfil.PROFESIONAL, "Ana", ZoneId.of("UTC"));
        ana.definirHabilidades(Set.of("Arquitectura"));
        perfiles.save(ana);
        perfiles.save(Perfil.crear("user-luis", TipoPerfil.PROFESIONAL, "Luis", ZoneId.of("UTC")));

        java = publicar("user-ana", "Mentoría de Java", "Programación", Set.of("java"));
        diseno = publicar("user-luis", "Revisión de portfolio", "Diseño", Set.of("ux"));
        fotografia = publicar("user-luis", "Sesión de fotos", "Fotografía", Set.of());
        publicar(MATEO, "Mi propia mentoría de Java", "Programación", Set.of("java"));

        aceptarContrataciones(diseno, 2, AHORA.minus(Duration.ofDays(3)));
        aceptarContrataciones(fotografia, 1, AHORA.minus(Duration.ofDays(5)));
        aceptarContrataciones(java, 3, AHORA.minus(Duration.ofDays(60)));
    }

    private Publicacion publicar(String autor, String titulo, String categoria, Set<String> etiquetas) {
        var p = Publicacion.crearBorrador(autor, TipoPublicacion.OFERTA);
        p.editar(titulo, "Detalle", categoria, Modalidad.REMOTA, Duration.ofHours(1), 5, new SkillCoins(100), etiquetas);
        p.agregarFranja(AHORA.plus(Duration.ofDays(5)), AHORA.plus(Duration.ofDays(5).plusHours(1)));
        p.publicar(autor, AHORA.minus(Duration.ofDays(90)));
        publicaciones.save(p);
        return p;
    }

    private void aceptarContrataciones(Publicacion p, int cuantas, Instant cuando) {
        for (int i = 0; i < cuantas; i++) {
            var c = Contratacion.solicitar("cliente-" + i, p.autorId(), p.id(),
                    new Solicitud("Hola", "Aprender", p.franjas().get(0).id()), AHORA.plus(Duration.ofDays(5)),
                    new SkillCoins(100), cuando);
            c.aceptar(p.autorId(), cuando);
            contrataciones.save(c);
        }
    }

    private BusquedaService serviceCon(EstrategiaRecomendacion estrategia) {
        return new BusquedaService(new BuscadorPublicacionesEnMemoria(publicaciones, perfiles), publicaciones,
                perfiles, estrategia);
    }

    private static List<String> titulos(List<PublicacionDTO> recomendadas) {
        return recomendadas.stream().map(PublicacionDTO::titulo).toList();
    }

    @Test
    void caso7_cambiarLaEstrategiaCambiaLaRecomendacionSinTocarElCasoDeUso() {
        var porAfinidad = serviceCon(new AfinidadPorHabilidades(perfiles)).recomendar(MATEO, 5);
        var porPopularidad = serviceCon(new PopularidadReciente(contrataciones, clock)).recomendar(MATEO, 5);

        assertEquals(List.of("Mentoría de Java"), titulos(porAfinidad));
        assertEquals(List.of("Revisión de portfolio", "Sesión de fotos"), titulos(porPopularidad));
    }

    @Test
    void laAfinidadCuentaInteresesContraEtiquetasCategoriaYHabilidadesDelAutor() {
        var recomendadas = new AfinidadPorHabilidades(perfiles).recomendar(perfiles.findByUserId(MATEO).orElseThrow(),
                List.of(diseno, java, fotografia), 5);

        assertEquals(List.of(java), recomendadas, "java (etiqueta) y arquitectura (habilidad de Ana)");
    }

    @Test
    void laPopularidadSoloCuentaLasAceptadasDentroDeLaVentana() {
        var conVentanaLarga = new PopularidadReciente(contrataciones, clock, Duration.ofDays(90))
                .recomendar(null, List.of(diseno, java, fotografia), 5);

        assertEquals(List.of(java, diseno, fotografia), conVentanaLarga);
        assertEquals(List.of(diseno), new PopularidadReciente(contrataciones, clock).recomendar(null,
                List.of(diseno, java, fotografia), 1), "respeta la cantidad pedida");
    }

    @Test
    void nuncaSeRecomiendanLasPropiasYElUsuarioTieneQueTenerPerfil() {
        var recomendadas = serviceCon(new AfinidadPorHabilidades(perfiles)).recomendar(MATEO, 5);

        assertTrue(recomendadas.stream().noneMatch(p -> p.autorId().equals(MATEO)));
        assertThrows(RecursoNoEncontradoException.class,
                () -> serviceCon(new AfinidadPorHabilidades(perfiles)).recomendar("user-nadie", 5));
        assertThrows(IllegalArgumentException.class,
                () -> serviceCon(new AfinidadPorHabilidades(perfiles)).recomendar(MATEO, 0));
    }
}
