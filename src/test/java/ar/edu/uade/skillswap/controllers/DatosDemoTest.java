package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.controllers.impl.SkillSwapControllers;
import ar.edu.uade.skillswap.model.Models.Contratacion;
import ar.edu.uade.skillswap.model.Models.EstadoContratacion;
import ar.edu.uade.skillswap.model.Models.EstadoPublicacion;
import ar.edu.uade.skillswap.model.Models.FiltrosBusqueda;
import ar.edu.uade.skillswap.model.Models.Publicacion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ar.edu.uade.skillswap.controllers.EntornoDemo.LUCIA;
import static ar.edu.uade.skillswap.controllers.EntornoDemo.MATEO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Los datos de demo que ve la UI de la cátedra, y la demo en vivo hecha sobre ellos. */
class DatosDemoTest {

    private static final FiltrosBusqueda TODO = new FiltrosBusqueda("", "Todas", null, 0, "Relevancia");

    private final EntornoDemo demo = new EntornoDemo();
    private final SkillSwapControllers c = demo.controllers;

    @Test
    void cargarDosVecesNoDuplicaNada() {
        var publicaciones = c.busqueda().buscar(TODO).size();
        var contrataciones = c.contrataciones().listarContrataciones(MATEO).size();
        var saldo = demo.saldo(MATEO);

        var cargoOtraVez = demo.datosDemo.cargar();

        assertFalse(cargoOtraVez);
        assertEquals(publicaciones, c.busqueda().buscar(TODO).size());
        assertEquals(contrataciones, c.contrataciones().listarContrataciones(MATEO).size());
        assertEquals(saldo, demo.saldo(MATEO));
    }

    @Test
    void tieneLosProveedoresYCategoriasDelMock() {
        var proveedores = c.busqueda().buscar(TODO).stream().map(Publicacion::proveedor).collect(Collectors.toSet());

        assertEquals(Set.of("Sofía Méndez", "Tomás Vidal", "Camila Torres", "Nicolás Paz", "Laura Rossi",
                "Martín Aguirre", "Mateo Silva"), proveedores);
        assertEquals(List.of("Todas", "Creatividad", "Diseño", "Negocios", "Productividad", "Tecnología"),
                c.busqueda().categorias());
        assertEquals(3, c.busqueda().recomendar(MATEO).size());
    }

    @Test
    void mateoTienePerfilWalletNotificacionesYContratacionesEnDistintosEstados() {
        var perfil = c.perfiles().verPerfil(MATEO);
        var estados = c.contrataciones().listarContrataciones(MATEO).stream()
                .map(Contratacion::estado).collect(Collectors.toSet());

        assertEquals("Mateo Silva", perfil.nombre());
        assertEquals(5.0, perfil.reputacion());
        assertTrue(perfil.habilidades().contains("Java"));
        assertEquals(Set.of(EstadoContratacion.PENDIENTE, EstadoContratacion.ACEPTADA, EstadoContratacion.CONFIRMADA,
                EstadoContratacion.COMPLETADA, EstadoContratacion.CANCELADA), estados);
        assertTrue(demo.saldo(MATEO) > 1000);
        assertFalse(c.wallet().listarMovimientos(MATEO).isEmpty());
        assertFalse(c.notificaciones().listarNotificaciones(MATEO).isEmpty());
        assertEquals(List.of(EstadoPublicacion.BORRADOR, EstadoPublicacion.PUBLICADA),
                c.publicaciones().listarPropias(MATEO).stream().map(Publicacion::estado).sorted().toList());
    }

    /** La demo en vivo, como user-mateo desde la UI: cada "Avanzar estado" hace el paso que le toca. */
    @Test
    void laDemoFuncionaSobreLosDatosCargados() {
        var borrador = c.publicaciones().listarPropias(MATEO).stream()
                .filter(p -> p.estado() == EstadoPublicacion.BORRADOR).findFirst().orElseThrow();
        var publicada = c.publicaciones().cambiarEstado(MATEO, borrador.id(), EstadoPublicacion.PUBLICADA);
        assertEquals(EstadoPublicacion.PUBLICADA, publicada.estado());

        var pendiente = contratacion("Ana Gómez", EstadoContratacion.PENDIENTE);
        assertEquals(EstadoContratacion.ACEPTADA, c.contrataciones().avanzar(MATEO, pendiente.id()).estado());

        var aceptada = contratacion("Camila Torres", EstadoContratacion.ACEPTADA);
        assertEquals(EstadoContratacion.CONFIRMADA, c.contrataciones().avanzar(MATEO, aceptada.id()).estado());

        var confirmada = contratacion("Lucía Fernández", EstadoContratacion.CONFIRMADA);
        var saldoMateo = demo.saldo(MATEO);
        assertEquals(EstadoContratacion.EN_CURSO, c.contrataciones().avanzar(MATEO, confirmada.id()).estado());
        assertEquals(EstadoContratacion.COMPLETADA, c.contrataciones().avanzar(MATEO, confirmada.id()).estado());
        assertEquals(saldoMateo + confirmada.precio(), demo.saldo(MATEO), "cobra lo retenido al completar");

        var mateo = c.calificaciones().calificar(LUCIA, confirmada.id(), 4, "Muy buena");
        assertEquals(3, mateo.resenas());
    }

    private Contratacion contratacion(String contraparte, EstadoContratacion estado) {
        return c.contrataciones().listarContrataciones(MATEO).stream()
                .filter(x -> x.contraparte().equals(contraparte) && x.estado() == estado)
                .findFirst().orElseThrow();
    }
}
