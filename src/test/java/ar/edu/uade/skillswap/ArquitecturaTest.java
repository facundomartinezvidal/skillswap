package ar.edu.uade.skillswap;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regla de dependencias: el dominio es Java puro. No conoce la aplicación, la UI, los controllers,
 * el mock, los modelos de vista, JPA ni Swing. La aplicación solo conoce al dominio: tampoco depende
 * de la infraestructura ni de la presentación. Se revisa el texto de cada fuente para detectar tanto
 * imports como nombres completamente calificados.
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

    @Test
    void notificacionSoloDependeDeShared() throws IOException {
        var violaciones = violaciones(MAIN.resolve("domain/notificacion"), PROHIBIDOS_EN_NOTIFICACION);

        assertTrue(violaciones.isEmpty(), String.join(System.lineSeparator(), violaciones));
    }

    private static List<String> violaciones(Path directorio, List<String> prohibidos) throws IOException {
        List<Path> fuentes;
        try (var files = Files.walk(directorio)) {
            fuentes = files.filter(file -> file.toString().endsWith(".java")).toList();
        }
        assertFalse(fuentes.isEmpty(), "No hay fuentes en " + directorio);

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
}
