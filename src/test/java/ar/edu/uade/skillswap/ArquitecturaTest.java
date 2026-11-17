package ar.edu.uade.skillswap;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regla de dependencias: el dominio es Java puro. No conoce la aplicación, la UI, los controllers,
 * el mock, los modelos de vista, JPA ni Swing. La aplicación solo conoce al dominio: tampoco depende
 * de la infraestructura ni de la presentación. Se revisa el texto de cada fuente para detectar tanto
 * imports como nombres completamente calificados. Los controllers solo conocen Services, DTOs, modelos de
 * vista y tipos simples del dominio. La infraestructura no lanza excepciones de negocio.
 */
class ArquitecturaTest {

    private static final Path MAIN = Path.of("src/main/java/ar/edu/uade/skillswap");

    private static final List<String> PROHIBIDOS_EN_DOMAIN = List.of(
            "ar.edu.uade.skillswap.application.",
            "ar.edu.uade.skillswap.infrastructure.",
            "ar.edu.uade.skillswap.controllers.",
            "ar.edu.uade.skillswap.ui.",
            "ar.edu.uade.skillswap.mock.",
            "ar.edu.uade.skillswap.model.",
            "jakarta.persistence.",
            "javax.persistence.",
            "javax.swing.");

    private static final List<String> PROHIBIDOS_EN_APPLICATION = List.of(
            "ar.edu.uade.skillswap.infrastructure.",
            "ar.edu.uade.skillswap.controllers.",
            "ar.edu.uade.skillswap.ui.",
            "ar.edu.uade.skillswap.mock.",
            "ar.edu.uade.skillswap.model.",
            "jakarta.persistence.",
            "javax.persistence.",
            "org.hibernate.",
            "java.sql.",
            "javax.swing.");

    /** Notificación es genérica: conoce los eventos solo por {@code domain/shared/EventoDominio}. */
    private static final List<String> PROHIBIDOS_EN_NOTIFICACION = List.of(
            "ar.edu.uade.skillswap.domain.contratacion.",
            "ar.edu.uade.skillswap.domain.publicacion.");

    /** Los controllers son delgados: llaman a los Services, nunca a repositorios, JPA ni infraestructura. */
    private static final List<String> PROHIBIDOS_EN_CONTROLLERS = List.of(
            "ar.edu.uade.skillswap.infrastructure.",
            "ar.edu.uade.skillswap.ui.",
            "ar.edu.uade.skillswap.mock.",
            "Repository",
            "jakarta.persistence.",
            "javax.persistence.",
            "org.hibernate.",
            "java.sql.",
            "javax.swing.");

    /** Del dominio, los controllers solo usan tipos simples: enums, la excepción base y la página pedida. */
    private static final Set<String> DOMINIO_PERMITIDO_EN_CONTROLLERS = Set.of(
            "ar.edu.uade.skillswap.domain.shared.DomainException",
            "ar.edu.uade.skillswap.domain.shared.Pagina",
            "ar.edu.uade.skillswap.domain.shared.PaginaSolicitada",
            "ar.edu.uade.skillswap.domain.publicacion.Modalidad",
            "ar.edu.uade.skillswap.domain.perfil.TipoPerfil",
            "ar.edu.uade.skillswap.domain.perfil.CanalNotificacion");

    private static final Pattern TIPO_DEL_DOMINIO = Pattern.compile("ar\\.edu\\.uade\\.skillswap\\.domain\\.[\\w.]+");

    @Test
    void losControllersNoUsanRepositoriosNiInfraestructuraNiJpa() throws IOException {
        var violaciones = violaciones(MAIN.resolve("controllers"), PROHIBIDOS_EN_CONTROLLERS);

        assertTrue(violaciones.isEmpty(), String.join(System.lineSeparator(), violaciones));
    }

    @Test
    void losControllersSoloUsanTiposSimplesDelDominio() throws IOException {
        var violaciones = new ArrayList<String>();
        for (var fuente : fuentes(MAIN.resolve("controllers"))) {
            var usados = TIPO_DEL_DOMINIO.matcher(Files.readString(fuente));
            while (usados.find()) {
                var tipo = usados.group();
                var permitido = DOMINIO_PERMITIDO_EN_CONTROLLERS.stream()
                        .anyMatch(p -> tipo.equals(p) || tipo.startsWith(p + "."));
                if (!permitido) {
                    violaciones.add(fuente + " usa " + tipo);
                }
            }
        }

        assertTrue(violaciones.isEmpty(), String.join(System.lineSeparator(), violaciones));
    }

    @Test
    void elDominioNoDependeDeUiControllersMockModelJpaNiSwing() throws IOException {
        var violaciones = violaciones(MAIN.resolve("domain"), PROHIBIDOS_EN_DOMAIN);

        assertTrue(violaciones.isEmpty(), String.join(System.lineSeparator(), violaciones));
    }

    @Test
    void laAplicacionSoloDependeDelDominio() throws IOException {
        var violaciones = violaciones(MAIN.resolve("application"), PROHIBIDOS_EN_APPLICATION);

        assertTrue(violaciones.isEmpty(), String.join(System.lineSeparator(), violaciones));
    }

    /**
     * Los {@code reconstruir(...)} del dominio arman un agregado en cualquier estado sin pasar por sus
     * transiciones. Solo los usan los repositorios (infraestructura); la aplicación usa las operaciones.
     */
    @Test
    void laAplicacionNoReconstruyeAgregados() throws IOException {
        var violaciones = violaciones(MAIN.resolve("application"), List.of(".reconstruir("));

        assertTrue(violaciones.isEmpty(), String.join(System.lineSeparator(), violaciones));
    }

    @Test
    void notificacionSoloDependeDeShared() throws IOException {
        var violaciones = violaciones(MAIN.resolve("domain/notificacion"), PROHIBIDOS_EN_NOTIFICACION);

        assertTrue(violaciones.isEmpty(), String.join(System.lineSeparator(), violaciones));
    }

    /**
     * La infraestructura no toma decisiones de negocio: no lanza excepciones del dominio ni las de negocio de
     * la aplicación. Solo puede traducir un error técnico a {@code ConflictoDeConcurrenciaException}.
     */
    @Test
    void laInfraestructuraNoLanzaExcepcionesDeNegocio() throws IOException {
        var deNegocio = new ArrayList<String>();
        for (var fuente : fuentes(MAIN.resolve("domain"))) {
            agregarSiEsExcepcion(fuente, deNegocio);
        }
        for (var fuente : fuentes(MAIN.resolve("application"))) {
            agregarSiEsExcepcion(fuente, deNegocio);
        }
        deNegocio.remove("ConflictoDeConcurrenciaException");
        assertFalse(deNegocio.isEmpty(), "No se encontraron excepciones de negocio");

        var violaciones = new ArrayList<String>();
        var lanzadas = Pattern.compile("throw\\s+new\\s+(\\w+)");
        for (var fuente : fuentes(MAIN.resolve("infrastructure"))) {
            var throwNew = lanzadas.matcher(Files.readString(fuente));
            while (throwNew.find()) {
                if (deNegocio.contains(throwNew.group(1))) {
                    violaciones.add(fuente + " lanza " + throwNew.group(1));
                }
            }
        }

        assertTrue(violaciones.isEmpty(), String.join(System.lineSeparator(), violaciones));
    }

    private static void agregarSiEsExcepcion(Path fuente, List<String> excepciones) {
        var nombre = fuente.getFileName().toString().replace(".java", "");
        if (nombre.endsWith("Exception")) {
            excepciones.add(nombre);
        }
    }

    private static List<String> violaciones(Path directorio, List<String> prohibidos) throws IOException {
        var fuentes = fuentes(directorio);
        var violaciones = new ArrayList<String>();
        for (var fuente : fuentes) {
            var codigo = Files.readString(fuente);
            for (var prohibido : prohibidos) {
                if (codigo.contains(prohibido)) {
                    violaciones.add(fuente + " depende de " + prohibido);
                }
            }
        }
        return violaciones;
    }

    private static List<Path> fuentes(Path directorio) throws IOException {
        List<Path> fuentes;
        try (var files = Files.walk(directorio)) {
            fuentes = files.filter(file -> file.toString().endsWith(".java")).toList();
        }
        assertFalse(fuentes.isEmpty(), "No hay fuentes en " + directorio);
        return fuentes;
    }
}
