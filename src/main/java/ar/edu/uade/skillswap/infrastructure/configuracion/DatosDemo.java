package ar.edu.uade.skillswap.infrastructure.configuracion;

import ar.edu.uade.skillswap.application.contratacion.ContratacionService;
import ar.edu.uade.skillswap.application.contratacion.dto.SolicitarContratacionCommand;
import ar.edu.uade.skillswap.application.perfil.PerfilService;
import ar.edu.uade.skillswap.application.perfil.dto.CrearPerfilCommand;
import ar.edu.uade.skillswap.application.perfil.dto.EditarPerfilCommand;
import ar.edu.uade.skillswap.application.publicacion.PublicacionService;
import ar.edu.uade.skillswap.application.publicacion.dto.DatosPublicacionCommand;
import ar.edu.uade.skillswap.application.publicacion.dto.PromocionDTO;
import ar.edu.uade.skillswap.application.publicacion.dto.PublicacionDTO;
import ar.edu.uade.skillswap.application.shared.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.application.wallet.WalletService;
import ar.edu.uade.skillswap.domain.perfil.CanalNotificacion;
import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;
import ar.edu.uade.skillswap.domain.publicacion.Modalidad;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;

/**
 * Datos de demostración: los usuarios y publicaciones que mostraba el mock de la cátedra, con perfiles,
 * reputación, contrataciones en distintos estados, Wallets con movimientos y notificaciones.
 * <p>
 * Usa solo los Services públicos, así que carga lo mismo con el backend en memoria que con JPA, y todo
 * pasa por las reglas del dominio (la reputación sale de calificaciones reales, las notificaciones de
 * los eventos). Las franjas se calculan desde el reloj recibido y quedan en el futuro, para que la demo
 * pueda solicitar, aceptar y pagar sobre estos datos.
 */
public class DatosDemo {

    public static final String MATEO = "user-mateo";
    public static final String ANA = "user-ana";
    public static final String LUCIA = "user-lucia";
    public static final String SOFIA = "user-sofia";
    public static final String TOMAS = "user-tomas";
    public static final String CAMILA = "user-camila";
    public static final String NICOLAS = "user-nicolas";
    public static final String LAURA = "user-laura";
    public static final String MARTIN = "user-martin";

    /** Saldo extra que Mateo carga en la demo, además del bono de bienvenida. */
    static final long CARGA_MATEO = 1500;

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final int CUPO = 4;

    private final PerfilService perfiles;
    private final PublicacionService publicaciones;
    private final ContratacionService contrataciones;
    private final WalletService wallets;
    private final Instant base;

    public DatosDemo(PerfilService perfiles, PublicacionService publicaciones, ContratacionService contrataciones,
                     WalletService wallets, Clock clock) {
        this.perfiles = Objects.requireNonNull(perfiles);
        this.publicaciones = Objects.requireNonNull(publicaciones);
        this.contrataciones = Objects.requireNonNull(contrataciones);
        this.wallets = Objects.requireNonNull(wallets);
        this.base = clock.instant().truncatedTo(ChronoUnit.HOURS);
    }

    /**
     * Carga los datos una sola vez: si {@code user-mateo} ya tiene perfil (por ejemplo, en una base que
     * ya se cargó), no hace nada. Devuelve si cargó.
     */
    public boolean cargar() {
        if (existePerfil(MATEO)) {
            return false;
        }
        cargarPerfiles();
        var java = publicar(MATEO, "Mentoría de Java inicial", "Primeros pasos con Java: sintaxis, objetos y tests.",
                "Tecnología", Modalidad.REMOTA, 60, 100, Set.of("Java", "POO"));
        borrador(MATEO, "Consultas SQL desde cero", "SELECT, JOIN y GROUP BY con ejercicios sobre una base real.",
                "Tecnología", Modalidad.REMOTA, 60, 120, Set.of("SQL", "Datos"));
        var entrevistas = publicar(SOFIA, "Preparación para entrevistas técnicas",
                "Simulacro completo, feedback y plan personalizado para tu próxima entrevista.", "Tecnología",
                Modalidad.REMOTA, 60, 180, Set.of("Java", "Entrevistas", "Backend"));
        var portfolio = publicar(TOMAS, "Revisión profesional de portfolio",
                "Mejorá la narrativa y presentación de tus proyectos para destacarte.", "Diseño", Modalidad.REMOTA,
                45, 120, Set.of("UX", "Portfolio", "Carrera"));
        var arquitectura = publicar(CAMILA, "Arquitectura limpia aplicada",
                "Sesión práctica para ordenar dependencias y casos de uso en proyectos Java.", "Tecnología",
                Modalidad.HIBRIDA, 90, 260, Set.of("Clean Architecture", "SOLID", "Java"));
        var fotografia = publicar(NICOLAS, "Fotografía de producto con celular",
                "Iluminación, encuadre y edición simple para catálogos y redes.", "Creatividad", Modalidad.PRESENCIAL,
                75, 150, Set.of("Fotografía", "Contenido"));
        var liderazgo = publicar(LAURA, "Mentoría de liderazgo para equipos",
                "Herramientas concretas para conversaciones, feedback y delegación.", "Negocios", Modalidad.REMOTA,
                60, 220, Set.of("Liderazgo", "Equipos"));
        var excel = publicar(MARTIN, "Excel para decisiones de negocio",
                "Tablas dinámicas, escenarios y un dashboard listo para reutilizar.", "Productividad",
                Modalidad.REMOTA, 90, 140, Set.of("Excel", "Datos"));
        promocion(SOFIA, entrevistas, PromocionDTO.Tipo.FIJO, 20);
        promocion(TOMAS, portfolio, PromocionDTO.Tipo.PORCENTUAL, 10);
        promocion(CAMILA, arquitectura, PromocionDTO.Tipo.PORCENTUAL, 10);

        wallets.acreditarDemo(MATEO, CARGA_MATEO);

        // Historial: contrataciones completadas y calificadas, que arman la reputación de cada uno.
        completadaYCalificada(MATEO, liderazgo, 2, 5, 5);
        completadaYCalificada(ANA, entrevistas, 2, 5, 5);
        completadaYCalificada(ANA, fotografia, 2, 5, 4);
        completadaYCalificada(LUCIA, arquitectura, 2, 5, 5);
        completadaYCalificada(LUCIA, portfolio, 2, 4, 5);
        completadaYCalificada(LUCIA, excel, 2, 4, 4);
        completadaYCalificada(ANA, java, 2, 5, 5);

        // Contrataciones abiertas de Mateo, una por cada paso que se puede mostrar con "Avanzar estado".
        var confirmadaConTomas = solicitar(MATEO, portfolio, 4);
        contrataciones.aceptar(TOMAS, confirmadaConTomas);
        contrataciones.pagar(MATEO, confirmadaConTomas);

        var aceptadaPorCamila = solicitar(MATEO, arquitectura, 4);
        contrataciones.aceptar(CAMILA, aceptadaPorCamila);

        solicitar(ANA, java, 4);

        var confirmadaPorLucia = solicitar(LUCIA, java, 6);
        contrataciones.aceptar(MATEO, confirmadaPorLucia);
        contrataciones.pagar(LUCIA, confirmadaPorLucia);

        var canceladaConMartin = solicitar(MATEO, excel, 6);
        contrataciones.aceptar(MARTIN, canceladaConMartin);
        contrataciones.pagar(MATEO, canceladaConMartin);
        contrataciones.cancelar(MATEO, canceladaConMartin, "Se me superpuso con una reunión");
        return true;
    }

    private void cargarPerfiles() {
        perfil(MATEO, "Mateo Silva", "Construyo productos digitales y acompaño a personas que quieren crecer en tecnología.",
                Set.of("Java", "Clean Architecture", "Mentoría", "SQL", "Diseño OO"), Set.of("Portfolio", "Liderazgo"));
        perfil(ANA, "Ana Gómez", "Estudiante de sistemas, preparando su primera búsqueda laboral.",
                Set.of("Python"), Set.of("Java", "Entrevistas"));
        perfil(LUCIA, "Lucía Fernández", "Desarrolladora full stack que quiere pasar a liderar equipos.",
                Set.of("JavaScript", "React"), Set.of("Arquitectura", "Liderazgo"));
        perfil(SOFIA, "Sofía Méndez", "Backend developer y entrevistadora técnica.",
                Set.of("Java", "Entrevistas", "Backend"), Set.of());
        perfil(TOMAS, "Tomás Vidal", "Diseñador UX con diez años de experiencia en producto.",
                Set.of("UX", "Portfolio"), Set.of());
        perfil(CAMILA, "Camila Torres", "Arquitecta de software, fan de los casos de uso bien separados.",
                Set.of("Clean Architecture", "SOLID", "Java"), Set.of());
        perfil(NICOLAS, "Nicolás Paz", "Fotógrafo de producto para marcas chicas.",
                Set.of("Fotografía"), Set.of());
        perfil(LAURA, "Laura Rossi", "Coach de equipos y ex gerenta de ingeniería.",
                Set.of("Liderazgo", "Equipos"), Set.of());
        perfil(MARTIN, "Martín Aguirre", "Analista de datos y docente de Excel.",
                Set.of("Excel", "Datos"), Set.of());
        // Dos canales distintos para que se vean los dos adapters simulados.
        perfiles.elegirCanal(ANA, CanalNotificacion.EMAIL);
        perfiles.elegirCanal(LAURA, CanalNotificacion.PUSH);
    }

    private boolean existePerfil(String userId) {
        try {
            perfiles.verPerfil(userId);
            return true;
        } catch (RecursoNoEncontradoException e) {
            return false;
        }
    }

    private void perfil(String userId, String nombre, String bio, Set<String> habilidades, Set<String> intereses) {
        perfiles.crearPerfil(new CrearPerfilCommand(userId, TipoPerfil.PROFESIONAL, nombre, ZONA));
        perfiles.editarPerfil(userId, new EditarPerfilCommand(nombre, bio, ZONA, habilidades, intereses));
    }

    /** Borrador completo, con franjas en 2, 4 y 6 días, listo para publicar desde la UI. */
    private PublicacionDTO borrador(String autor, String titulo, String descripcion, String categoria,
                                    Modalidad modalidad, int minutos, long precio, Set<String> etiquetas) {
        var duracion = Duration.ofMinutes(minutos);
        var borrador = publicaciones.crearBorrador(autor, new DatosPublicacionCommand(titulo, descripcion, categoria,
                modalidad, duracion, CUPO, precio, etiquetas));
        for (var dias : new int[] {2, 4, 6}) {
            publicaciones.agregarFranja(autor, borrador.id(), enDias(dias), enDias(dias).plus(duracion));
        }
        return borrador;
    }

    private PublicacionDTO publicar(String autor, String titulo, String descripcion, String categoria,
                                    Modalidad modalidad, int minutos, long precio, Set<String> etiquetas) {
        var borrador = borrador(autor, titulo, descripcion, categoria, modalidad, minutos, precio, etiquetas);
        return publicaciones.publicar(autor, borrador.id());
    }

    private void promocion(String autor, PublicacionDTO publicacion, PromocionDTO.Tipo tipo, long valor) {
        publicaciones.agregarPromocion(autor, publicacion.id(), new PromocionDTO(tipo, valor));
    }

    private String solicitar(String cliente, PublicacionDTO publicacion, int diasHastaLaFranja) {
        var inicio = enDias(diasHastaLaFranja);
        var franja = publicacion.franjas().stream()
                .filter(f -> f.inicio().equals(inicio))
                .findFirst()
                .orElseThrow();
        return contrataciones.solicitar(new SolicitarContratacionCommand(cliente, publicacion.id(), franja.id(),
                "Hola, me interesa tu propuesta", "Mejorar en " + publicacion.categoria())).id();
    }

    private void completadaYCalificada(String cliente, PublicacionDTO publicacion, int diasHastaLaFranja,
                                       int puntajeAlProveedor, int puntajeAlCliente) {
        var proveedor = publicacion.autorId();
        var id = solicitar(cliente, publicacion, diasHastaLaFranja);
        contrataciones.aceptar(proveedor, id);
        contrataciones.pagar(cliente, id);
        contrataciones.iniciar(proveedor, id);
        contrataciones.completar(proveedor, id);
        contrataciones.calificar(cliente, id, puntajeAlProveedor, "Muy recomendable");
        contrataciones.calificar(proveedor, id, puntajeAlCliente, "Llegó con todo preparado");
    }

    private Instant enDias(int dias) {
        return base.plus(Duration.ofDays(dias));
    }
}
