package ar.edu.uade.skillswap.dominio.contrataciones;

import ar.edu.uade.skillswap.dominio.compartido.ReglaNegocioException;
import ar.edu.uade.skillswap.dominio.publicaciones.FranjaDisponibilidad;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ContratacionTest {
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 10, 12, 0);

    @Test
    void recorreFlujoFelizYRegistraCadaCambio() {
        Contratacion contratacion = solicitar()
                .aceptar("proveedor-1", AHORA)
                .confirmarPago("cliente-1", AHORA.plusMinutes(1))
                .iniciar("proveedor-1", AHORA.plusHours(1))
                .completar("proveedor-1", AHORA.plusHours(2));

        assertEquals(EstadoContratacion.COMPLETADA, contratacion.estado());
        assertEquals(4, contratacion.historial().size());
    }

    @Test
    void rechazaAccionesDePersonasQueNoSonParticipantes() {
        assertThrows(ReglaNegocioException.class,
                () -> solicitar().aceptar("cliente-1", AHORA));
    }

    @Test
    void noPermiteCompletarUnaContratacionPendiente() {
        Contratacion contratacion = solicitar();
        assertThrows(ReglaNegocioException.class,
                () -> contratacion.completar("proveedor-1", AHORA));
    }

    @Test
    void noPermiteCancelarUnaSolicitudPendiente() {
        assertThrows(ReglaNegocioException.class,
                () -> solicitar().cancelar("cliente-1", "Ya no puedo", AHORA));
    }

    private Contratacion solicitar() {
        return Contratacion.solicitar(
                "con-1", "pub-1", "cliente-1", "proveedor-1", "Hola",
                "Quiero practicar entrevistas",
                new FranjaDisponibilidad(AHORA.plusDays(1), AHORA.plusDays(1).plusHours(1)),
                150);
    }
}
