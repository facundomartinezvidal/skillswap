package ar.edu.uade.skillswap;

import ar.edu.uade.skillswap.application.busqueda.BusquedaService;
import ar.edu.uade.skillswap.application.busqueda.recomendacion.AfinidadPorHabilidades;
import ar.edu.uade.skillswap.application.contratacion.ContratacionService;
import ar.edu.uade.skillswap.application.notificacion.NotificacionService;
import ar.edu.uade.skillswap.application.perfil.PerfilService;
import ar.edu.uade.skillswap.application.publicacion.PublicacionService;
import ar.edu.uade.skillswap.application.wallet.WalletService;
import ar.edu.uade.skillswap.controllers.OperacionRechazadaException;
import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.controllers.impl.SkillSwapControllers;
import ar.edu.uade.skillswap.infrastructure.configuracion.BackendEnMemoria;
import ar.edu.uade.skillswap.infrastructure.configuracion.DatosDemo;
import ar.edu.uade.skillswap.infrastructure.identidad.ServicioIdentidadSimulado;
import ar.edu.uade.skillswap.ui.SkillSwapView;
import ar.edu.uade.skillswap.ui.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;

/**
 * Punto de entrada. Arma el backend, carga los datos de demo y le pasa los controllers reales a la UI
 * de la cátedra.
 * <ul>
 *   <li>{@code --memoria}: backend en memoria (plan B de la demo, sin MySQL).</li>
 *   <li>{@code --screenshot <archivo>}: guarda la pantalla de inicio en un PNG y termina.</li>
 * </ul>
 */
public final class SkillSwapApplication {
    private SkillSwapApplication() {}

    public static void main(String[] args) throws Exception {
        Theme.install();
        var argumentos = Arrays.asList(args);
        var clock = Clock.systemDefaultZone();
        UiControllers controllers = crearControllers(argumentos, clock);
        int screenshot = argumentos.indexOf("--screenshot");
        if (screenshot >= 0 && screenshot + 1 < args.length) {
            SkillSwapView view = new SkillSwapView(controllers);
            view.setSize(1365, 768); view.doLayout(); layoutTree(view);
            BufferedImage image = new BufferedImage(1365,768,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);view.printAll(g);g.dispose();
            ImageIO.write(image,"png",new File(args[screenshot + 1])); return;
        }
        mostrarErroresEnPantalla();
        SwingUtilities.invokeLater(() -> {
            JFrame frame=new JFrame("SkillSwap");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setMinimumSize(new Dimension(1120,700));frame.setSize(1365,820);frame.setLocationRelativeTo(null);
            frame.setContentPane(new SkillSwapView(controllers));frame.setVisible(true);
        });
    }

    /** {@code --memoria} usa el backend en memoria; sin argumento, el de JPA con MySQL. */
    static SkillSwapControllers crearControllers(List<String> argumentos, Clock clock) {
        if (argumentos.contains("--memoria")) {
            return conBackendEnMemoria(clock);
        }
        return conBackendEnMemoria(clock); // TODO integración: BackendJpa
    }

    private static SkillSwapControllers conBackendEnMemoria(Clock clock) {
        var backend = new BackendEnMemoria(clock, ServicioIdentidadSimulado.conUsuariosDeDemo(),
                BackendEnMemoria.BONO_DEMO, AfinidadPorHabilidades::new);
        return armar(backend.perfilService, backend.publicacionService, backend.busquedaService,
                backend.contratacionService, backend.walletService, backend.notificacionService, clock);
    }

    /** Igual para cualquier backend: carga los datos de demo (solo la primera vez) y arma los controllers. */
    private static SkillSwapControllers armar(PerfilService perfiles, PublicacionService publicaciones,
                                              BusquedaService busqueda, ContratacionService contrataciones,
                                              WalletService wallets, NotificacionService notificaciones, Clock clock) {
        new DatosDemo(perfiles, publicaciones, contrataciones, wallets, clock).cargar();
        return new SkillSwapControllers(perfiles, publicaciones, busqueda, contrataciones, wallets, notificaciones,
                clock);
    }

    /**
     * La vista de la cátedra no atrapa excepciones: lo que se escapa de un botón llega al manejador del
     * hilo de Swing. Ahí se muestra el mensaje del controller en un cuadro de diálogo, sin tocar la vista.
     */
    private static void mostrarErroresEnPantalla() {
        Thread.setDefaultUncaughtExceptionHandler((hilo, error) -> {
            String mensaje;
            if (error instanceof OperacionRechazadaException) {
                mensaje = error.getMessage();
            } else {
                error.printStackTrace();
                mensaje = "Ocurrió un error inesperado: " + error.getMessage();
            }
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, mensaje,
                    "No se pudo completar la operación", JOptionPane.WARNING_MESSAGE));
        });
    }

    private static void layoutTree(Container c) {
        c.doLayout();
        for(Component x:c.getComponents()) if(x instanceof Container child) layoutTree(child);
    }
}
