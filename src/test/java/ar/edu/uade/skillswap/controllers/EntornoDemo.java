package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.application.busqueda.recomendacion.AfinidadPorHabilidades;
import ar.edu.uade.skillswap.controllers.impl.SkillSwapControllers;
import ar.edu.uade.skillswap.infrastructure.configuracion.BackendEnMemoria;
import ar.edu.uade.skillswap.infrastructure.configuracion.DatosDemo;
import ar.edu.uade.skillswap.infrastructure.identidad.ServicioIdentidadSimulado;
import ar.edu.uade.skillswap.model.Models.Contratacion;
import ar.edu.uade.skillswap.model.Models.Modalidad;
import ar.edu.uade.skillswap.model.Models.Publicacion;

import java.time.Instant;

import static ar.edu.uade.skillswap.model.Models.EstadoPublicacion.PUBLICADA;

/**
 * La aplicación armada como en {@code main --memoria}, con un reloj fijo: backend en memoria, datos de
 * demo y los controllers reales. Los tests actúan como la UI, siempre a través de los controllers.
 */
final class EntornoDemo {

    static final Instant AHORA = Instant.parse("2026-11-16T12:00:00Z");
    static final String MATEO = DatosDemo.MATEO;
    static final String ANA = DatosDemo.ANA;
    static final String LUCIA = DatosDemo.LUCIA;

    final RelojDePrueba clock = new RelojDePrueba(AHORA);
    final BackendEnMemoria backend = new BackendEnMemoria(clock, ServicioIdentidadSimulado.conUsuariosDeDemo(),
            BackendEnMemoria.BONO_DEMO, AfinidadPorHabilidades::new);
    final DatosDemo datosDemo = new DatosDemo(backend.perfilService, backend.publicacionService,
            backend.contratacionService, backend.walletService, clock);
    final SkillSwapControllers controllers = new SkillSwapControllers(backend.perfilService,
            backend.publicacionService, backend.busquedaService, backend.contratacionService, backend.walletService,
            backend.notificacionService, clock);

    EntornoDemo() {
        datosDemo.cargar();
    }

    /** Mateo publica desde la UI: crea el borrador con el formulario y aprieta "Publicar". */
    Publicacion mateoPublica(String titulo, int precio) {
        var borrador = controllers.publicaciones().crearBorrador(MATEO, titulo, "Descripción de " + titulo,
                "Tecnología", Modalidad.REMOTA, 60, precio);
        return controllers.publicaciones().cambiarEstado(MATEO, borrador.id(), PUBLICADA);
    }

    /** Cliente solicita, Mateo acepta y el cliente paga, todo con el botón "Avanzar estado". */
    Contratacion contratadaYPagada(String cliente, Publicacion publicacion) {
        var contratacion = controllers.contrataciones().solicitar(cliente, publicacion.id(), "Aprender");
        controllers.contrataciones().avanzar(MATEO, contratacion.id());
        return controllers.contrataciones().avanzar(cliente, contratacion.id());
    }

    Contratacion completada(String cliente, Publicacion publicacion) {
        var contratacion = contratadaYPagada(cliente, publicacion);
        controllers.contrataciones().avanzar(MATEO, contratacion.id());
        return controllers.contrataciones().avanzar(MATEO, contratacion.id());
    }

    int saldo(String userId) {
        return controllers.wallet().consultarSaldo(userId);
    }
}
