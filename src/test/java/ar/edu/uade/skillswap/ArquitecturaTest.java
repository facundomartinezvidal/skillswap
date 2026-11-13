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
 * Regla de dependencias: el dominio es Java puro. No conoce la UI, los controllers, el mock,
 * los modelos de vista, JPA ni Swing. Se revisa el texto de cada fuente para detectar tanto
 * imports como nombres completamente calificados.
 */
class ArquitecturaTest {

    private static final Path MAIN = Path.of("src/main/java/ar/edu/uade/skillswap");

    private static final List<String> PROHIBIDOS_EN_DOMAIN = List.of(
            "ar.edu.uade.skillswap.controllers.",
            "ar.edu.uade.skillswap.ui.",
            "ar.edu.uade.skillswap.mock.",
            "ar.edu.uade.skillswap.model.",
            "jakarta.persistence.",
            "javax.persistence.",
            "javax.swing.");

    @Test
    void elDominioNoDependeDeUiControllersMockModelJpaNiSwing() throws IOException {
        var violaciones = violaciones(MAIN.resolve("domain"), PROHIBIDOS_EN_DOMAIN);

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
