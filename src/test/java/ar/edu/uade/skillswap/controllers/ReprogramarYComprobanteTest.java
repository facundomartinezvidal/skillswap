package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.controllers.impl.SkillSwapControllers;
import ar.edu.uade.skillswap.model.Models.EstadoContratacion;
import ar.edu.uade.skillswap.model.Models.EstadoPublicacion;
import ar.edu.uade.skillswap.model.Models.Modalidad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static ar.edu.uade.skillswap.controllers.EntornoDemo.ANA;
import static ar.edu.uade.skillswap.controllers.EntornoDemo.MATEO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reprogramar una contratación (una parte propone otra franja y la otra acepta o rechaza) y consultar el
 * comprobante de una completada, a través de {@link ContratacionOperaciones}.
 */
class ReprogramarYComprobanteTest {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EntornoDemo demo = new EntornoDemo();
    private final SkillSwapControllers c = demo.controllers;
    private final LocalDateTime maniana = LocalDateTime.ofInstant(EntornoDemo.AHORA, ZoneId.systemDefault())
            .truncatedTo(ChronoUnit.HOURS).plusDays(1);
    private String contratacion;

    /** Mateo publica con franjas mañana, en 2 y en 4 días; Ana pide la de mañana y Mateo acepta. */
    @BeforeEach
    void ana_tiene_una_contratacion_aceptada() {
        var borrador = c.publicaciones().crearBorrador(MATEO, "Mentoría de Git", "Ramas y merges", "Tecnología",
                Modalidad.REMOTA, 60, 100, 1, List.of(maniana, maniana.plusDays(2), maniana.plusDays(4)));
        c.publicaciones().cambiarEstado(MATEO, borrador.id(), EstadoPublicacion.PUBLICADA);
        var primera = c.publicaciones().listarFranjas(ANA, borrador.id()).get(0);
        contratacion = c.contrataciones().solicitar(ANA, borrador.id(), primera.id(), "", "Aprender ramas").id();
        c.contrataciones().aceptar(MATEO, contratacion);
    }

    @Test
    void paraReprogramarSeOfrecenLasOtrasFranjasDisponibles() {
        var franjas = c.contrataciones().listarFranjasParaReprogramar(ANA, contratacion);

        assertEquals(List.of(maniana.plusDays(2), maniana.plusDays(4)), franjas.stream().map(FranjaVista::inicio).toList());
    }

    @Test
    void unaPartePoneLaPropuestaYLaOtraLaAcepta() {
        var cuarta = c.contrataciones().listarFranjasParaReprogramar(ANA, contratacion).get(1);

        c.contrataciones().proponerReprogramacion(ANA, contratacion, cuarta.id());

        var deAna = c.contrataciones().pasoSiguiente(ANA, contratacion);
        var deMateo = c.contrataciones().pasoSiguiente(MATEO, contratacion);
        assertEquals(cuarta.inicio(), deAna.reprogramacionPropuesta());
        assertTrue(deAna.propuestaPorMi());
        assertTrue(deAna.detalle().endsWith("Propusiste pasar la sesión al " + cuarta.inicio().format(FORMATO)
                + ": falta que Mateo Silva responda."), deAna.detalle());
        assertFalse(deMateo.propuestaPorMi());
        assertTrue(deMateo.detalle().endsWith("Ana Gómez propuso pasar la sesión al "
                + cuarta.inicio().format(FORMATO) + "."), deMateo.detalle());

        var reprogramada = c.contrataciones().aceptarReprogramacion(MATEO, contratacion);

        assertEquals(cuarta.inicio(), reprogramada.fecha());
        assertEquals(EstadoContratacion.ACEPTADA, reprogramada.estado());
        assertNull(c.contrataciones().pasoSiguiente(ANA, contratacion).reprogramacionPropuesta());
    }

    @Test
    void siLaOtraParteLaRechazaLaSesionQuedaComoEstaba() {
        var tercera = c.contrataciones().listarFranjasParaReprogramar(MATEO, contratacion).get(0);
        c.contrataciones().proponerReprogramacion(MATEO, contratacion, tercera.id());

        var sinCambio = c.contrataciones().rechazarReprogramacion(ANA, contratacion);

        assertEquals(maniana, sinCambio.fecha());
        assertNull(c.contrataciones().pasoSiguiente(MATEO, contratacion).reprogramacionPropuesta());
    }

    @Test
    void elComprobanteDeUnaCompletadaTieneImporteFechasYCalificaciones() {
        c.contrataciones().pagar(ANA, contratacion);
        demo.clock.adelantar(java.time.Duration.ofDays(1));
        c.contrataciones().iniciar(MATEO, contratacion);
        c.contrataciones().completar(MATEO, contratacion);
        c.calificaciones().calificar(ANA, contratacion, 5, "Muy clara");

        var comprobante = c.contrataciones().verComprobante(ANA, contratacion);

        assertEquals(List.of("Contratación " + contratacion, "Publicación: Mentoría de Git", "Cliente: Ana Gómez",
                "Proveedor: Mateo Silva", "Sesión: " + maniana.format(FORMATO), "Importe: 100 SkillCoins",
                "Estado: COMPLETADA"), comprobante.subList(0, 7));
        assertTrue(comprobante.stream().anyMatch(l -> l.startsWith("Pagado el ")), comprobante.toString());
        assertTrue(comprobante.stream().anyMatch(l -> l.startsWith("Completada el ")), comprobante.toString());
        assertEquals("Calificación de Ana Gómez: 5/5 · Muy clara", comprobante.get(comprobante.size() - 1));
    }

    @Test
    void elComprobanteDeUnaCanceladaMuestraElReintegro() {
        c.contrataciones().pagar(ANA, contratacion);
        c.contrataciones().cancelar(MATEO, contratacion, "Me surgió un viaje");

        var comprobante = c.contrataciones().verComprobante(ANA, contratacion);

        assertTrue(comprobante.contains("Reintegro: 100 SkillCoins al cliente y 0 al proveedor"), comprobante.toString());
    }
}
