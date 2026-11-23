package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.controllers.impl.SkillSwapControllers;
import ar.edu.uade.skillswap.model.Models.EstadoPublicacion;
import ar.edu.uade.skillswap.model.Models.Modalidad;
import ar.edu.uade.skillswap.model.Models.Publicacion;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static ar.edu.uade.skillswap.controllers.EntornoDemo.MATEO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las franjas las define el autor: al crear el borrador y después mientras siga en borrador, a través de
 * {@link PublicacionOperaciones}.
 */
class FranjasDelAutorTest {

    private final EntornoDemo demo = new EntornoDemo();
    private final SkillSwapControllers c = demo.controllers;
    private final LocalDateTime maniana = LocalDateTime.ofInstant(EntornoDemo.AHORA, ZoneId.systemDefault())
            .truncatedTo(ChronoUnit.HOURS).plusDays(1);

    private Publicacion borradorDeGit(int cupo) {
        return borradorDeGit(cupo, List.of(maniana));
    }

    private Publicacion borradorDeGit(int cupo, List<LocalDateTime> inicios) {
        return c.publicaciones().crearBorrador(MATEO, "Mentoría de Git", "Ramas y merges", "Tecnología",
                Modalidad.REMOTA, 90, 100, cupo, inicios);
    }

    @Test
    void elBorradorNaceConLaFranjaQueEligeElAutor() {
        var borrador = borradorDeGit(3);

        var franjas = c.publicaciones().listarFranjas(MATEO, borrador.id());

        assertEquals(EstadoPublicacion.BORRADOR, borrador.estado());
        assertEquals(List.of(new FranjaVista(franjas.get(0).id(), maniana, maniana.plusMinutes(90), 3, true)),
                franjas);
    }

    @Test
    void alCrearSeCarganVariasFranjas() {
        var borrador = borradorDeGit(2, List.of(maniana.plusDays(3), maniana, maniana.plusDays(1)));

        var franjas = c.publicaciones().listarFranjas(MATEO, borrador.id());

        assertEquals(List.of(maniana, maniana.plusDays(1), maniana.plusDays(3)),
                franjas.stream().map(FranjaVista::inicio).toList());
        assertTrue(franjas.stream().allMatch(f -> f.lugaresLibres() == 2));
    }

    /** Sin franjas el borrador se crea igual (el dominio lo permite); al publicar se exige al menos una. */
    @Test
    void sinFranjasSeCreaElBorradorPeroNoSePublica() {
        var borrador = borradorDeGit(1, List.of());

        assertEquals(List.of(), c.publicaciones().listarFranjas(MATEO, borrador.id()));
        assertThrows(OperacionRechazadaException.class,
                () -> c.publicaciones().cambiarEstado(MATEO, borrador.id(), EstadoPublicacion.PUBLICADA));
    }

    @Test
    void siUnaFranjaSeSuperponeElBorradorQuedaConLasAnterioresYLoAvisa() {
        var error = assertThrows(OperacionRechazadaException.class,
                () -> borradorDeGit(1, List.of(maniana, maniana.plusMinutes(30))));

        assertTrue(error.getMessage().startsWith("El borrador se creó, pero no la franja del "), error.getMessage());
        assertTrue(error.getMessage().contains("La franja se superpone con otra franja de la publicación"));
        var creado = c.publicaciones().listarPropias(MATEO).stream()
                .filter(p -> p.titulo().equals("Mentoría de Git")).findFirst().orElseThrow();
        assertEquals(List.of(maniana),
                c.publicaciones().listarFranjas(MATEO, creado.id()).stream().map(FranjaVista::inicio).toList());
    }

    @Test
    void enBorradorSeAgreganMasFranjasYDespuesSePublica() {
        var borrador = borradorDeGit(1);
        c.publicaciones().agregarFranja(MATEO, borrador.id(), maniana.plusDays(2), maniana.plusDays(2).plusMinutes(90));

        var publicada = c.publicaciones().cambiarEstado(MATEO, borrador.id(), EstadoPublicacion.PUBLICADA);

        assertEquals(EstadoPublicacion.PUBLICADA, publicada.estado());
        assertEquals(List.of(maniana, maniana.plusDays(2)),
                c.publicaciones().listarFranjas(MATEO, borrador.id()).stream().map(FranjaVista::inicio).toList());
    }

    @Test
    void unaFranjaSuperpuestaSeRechaza() {
        var borrador = borradorDeGit(1);

        var error = assertThrows(OperacionRechazadaException.class, () -> c.publicaciones()
                .agregarFranja(MATEO, borrador.id(), maniana.plusMinutes(30), maniana.plusMinutes(120)));

        assertEquals("La franja se superpone con otra franja de la publicación", error.getMessage());
    }

    @Test
    void unaVezPublicadaNoSeAgreganFranjas() {
        var borrador = borradorDeGit(1);
        c.publicaciones().cambiarEstado(MATEO, borrador.id(), EstadoPublicacion.PUBLICADA);

        assertThrows(OperacionRechazadaException.class, () -> c.publicaciones()
                .agregarFranja(MATEO, borrador.id(), maniana.plusDays(3), maniana.plusDays(3).plusMinutes(90)));
    }

    /** El borrador puede estar incompleto; el cupo de al menos 1 se exige al publicar. */
    @Test
    void sinLugaresNoSePublica() {
        var borrador = borradorDeGit(0);

        var error = assertThrows(OperacionRechazadaException.class,
                () -> c.publicaciones().cambiarEstado(MATEO, borrador.id(), EstadoPublicacion.PUBLICADA));

        assertTrue(error.getMessage().contains("cupo de al menos 1"), error.getMessage());
    }
}
