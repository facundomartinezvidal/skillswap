package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.controllers.impl.SkillSwapControllers;
import ar.edu.uade.skillswap.model.Models.EstadoContratacion;
import ar.edu.uade.skillswap.model.Models.Modalidad;
import ar.edu.uade.skillswap.model.Models.Publicacion;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static ar.edu.uade.skillswap.controllers.EntornoDemo.ANA;
import static ar.edu.uade.skillswap.controllers.EntornoDemo.LUCIA;
import static ar.edu.uade.skillswap.controllers.EntornoDemo.MATEO;
import static ar.edu.uade.skillswap.model.Models.EstadoPublicacion.PUBLICADA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El cliente elige la franja e indica mensaje y objetivo al solicitar, como pide la consigna, a través de
 * {@link PublicacionOperaciones#listarFranjas} y {@link ContratacionOperaciones#solicitar}.
 */
class SolicitarConFranjaTest {

    private final EntornoDemo demo = new EntornoDemo();
    private final SkillSwapControllers c = demo.controllers;
    private final LocalDateTime ahora = LocalDateTime.ofInstant(EntornoDemo.AHORA, ZoneId.systemDefault())
            .truncatedTo(ChronoUnit.HOURS);

    /** Una publicación de Mateo con cupo 1 y franjas en 1 y 2 días, además de la que agrega el formulario (7 días). */
    private Publicacion publicacionConTresFranjas() {
        var borrador = c.publicaciones().crearBorrador(MATEO, "Mentoría de Git", "Ramas y merges", "Tecnología",
                Modalidad.REMOTA, 60, 100);
        c.publicaciones().agregarFranja(MATEO, borrador.id(), ahora.plusDays(2), ahora.plusDays(2).plusHours(1));
        c.publicaciones().agregarFranja(MATEO, borrador.id(), ahora.plusDays(1), ahora.plusDays(1).plusHours(1));
        return c.publicaciones().cambiarEstado(MATEO, borrador.id(), PUBLICADA);
    }

    @Test
    void listarFranjasLasOrdenaYMarcaLasDisponibles() {
        var publicacion = publicacionConTresFranjas();

        var franjas = c.publicaciones().listarFranjas(ANA, publicacion.id());

        assertEquals(3, franjas.size());
        assertEquals(List.of(ahora.plusDays(1), ahora.plusDays(2)),
                franjas.subList(0, 2).stream().map(FranjaVista::inicio).toList());
        assertTrue(franjas.stream().allMatch(f -> f.disponible() && f.lugaresLibres() == 1));
    }

    @Test
    void solicitarReservaLaFranjaElegida() {
        var publicacion = publicacionConTresFranjas();
        var segunda = c.publicaciones().listarFranjas(ANA, publicacion.id()).get(1);

        var contratacion = c.contrataciones().solicitar(ANA, publicacion.id(), segunda.id(),
                "Uso Git solo con la interfaz", "Entender rebase");

        assertEquals(EstadoContratacion.PENDIENTE, contratacion.estado());
        assertEquals(segunda.inicio(), contratacion.fecha());
    }

    /** El cupo se reserva al aceptar (decisión D6): recién ahí la franja deja de estar disponible. */
    @Test
    void alAceptarLaFranjaElegidaDejaDeEstarDisponible() {
        var publicacion = publicacionConTresFranjas();
        var segunda = c.publicaciones().listarFranjas(ANA, publicacion.id()).get(1);
        var contratacion = c.contrataciones().solicitar(ANA, publicacion.id(), segunda.id(), "", "Entender rebase");

        c.contrataciones().aceptar(MATEO, contratacion.id());

        var despues = c.publicaciones().listarFranjas(ANA, publicacion.id());
        assertEquals(0, despues.get(1).lugaresLibres());
        assertFalse(despues.get(1).disponible());
        assertTrue(despues.get(0).disponible());
    }

    @Test
    void elMensajeEsOpcional() {
        var publicacion = publicacionConTresFranjas();
        var franja = c.publicaciones().listarFranjas(ANA, publicacion.id()).get(0);

        var contratacion = c.contrataciones().solicitar(ANA, publicacion.id(), franja.id(), "", "Entender rebase");

        assertEquals(EstadoContratacion.PENDIENTE, contratacion.estado());
    }

    @Test
    void sinObjetivoSeRechaza() {
        var publicacion = publicacionConTresFranjas();
        var franja = c.publicaciones().listarFranjas(ANA, publicacion.id()).get(0);

        var error = assertThrows(OperacionRechazadaException.class,
                () -> c.contrataciones().solicitar(ANA, publicacion.id(), franja.id(), "Hola", " "));

        assertEquals("El objetivo de la solicitud es obligatorio", error.getMessage());
    }

    @Test
    void unaFranjaSinCupoSeRechaza() {
        var publicacion = publicacionConTresFranjas();
        var franja = c.publicaciones().listarFranjas(ANA, publicacion.id()).get(0);
        var deAna = c.contrataciones().solicitar(ANA, publicacion.id(), franja.id(), "", "Entender rebase");
        c.contrataciones().aceptar(MATEO, deAna.id());

        assertThrows(OperacionRechazadaException.class,
                () -> c.contrataciones().solicitar(LUCIA, publicacion.id(), franja.id(), "", "Aprender ramas"));
    }

    @Test
    void unaFranjaQueYaEmpezoNoEstaDisponibleYSeRechaza() {
        var publicacion = publicacionConTresFranjas();
        var primera = c.publicaciones().listarFranjas(ANA, publicacion.id()).get(0);

        demo.clock.adelantar(Duration.ofDays(1).plusMinutes(10));

        assertFalse(c.publicaciones().listarFranjas(ANA, publicacion.id()).get(0).disponible());
        assertThrows(OperacionRechazadaException.class,
                () -> c.contrataciones().solicitar(ANA, publicacion.id(), primera.id(), "", "Entender rebase"));
    }

    @Test
    void unaFranjaDeOtraPublicacionSeRechaza() {
        var publicacion = publicacionConTresFranjas();
        var otra = demo.mateoPublica("Mentoría de Docker", 80);
        var franjaDeOtra = c.publicaciones().listarFranjas(ANA, otra.id()).get(0);

        assertThrows(OperacionRechazadaException.class,
                () -> c.contrataciones().solicitar(ANA, publicacion.id(), franjaDeOtra.id(), "", "Entender rebase"));
    }
}
