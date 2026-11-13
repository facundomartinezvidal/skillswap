package ar.edu.uade.skillswap.domain.contratacion.strategy;

import ar.edu.uade.skillswap.domain.contratacion.Contratacion;
import ar.edu.uade.skillswap.domain.contratacion.Solicitud;
import ar.edu.uade.skillswap.domain.contratacion.exception.AccionNoPermitidaException;
import ar.edu.uade.skillswap.domain.publicacion.FranjaId;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionId;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PoliticaReintegroFactoryTest {

    private static final String CLIENTE = "user-mateo";
    private static final String PROVEEDOR = "user-ana";

    private final ReintegroPorAnticipacion porAnticipacion = ReintegroPorAnticipacion.conUmbralesPorDefecto();
    private final PoliticaReintegroFactory factory = new PoliticaReintegroFactory(porAnticipacion);
    private final Contratacion contratacion = Contratacion.solicitar(CLIENTE, PROVEEDOR, PublicacionId.generate(),
            new Solicitud("hola", "aprender", FranjaId.generate()), Instant.parse("2026-11-20T10:00:00Z"),
            new SkillCoins(100), Instant.parse("2026-11-11T12:00:00Z"));

    @Test
    void siCancelaElProveedorElClienteRecuperaTodo() {
        assertInstanceOf(ReintegroTotal.class, factory.paraCancelacion(contratacion, PROVEEDOR));
    }

    @Test
    void siCancelaElClienteDependeDeLaAnticipacionConfigurada() {
        assertSame(porAnticipacion, factory.paraCancelacion(contratacion, CLIENTE));
    }

    @Test
    void unAjenoNoPuedePedirPolitica() {
        assertThrows(AccionNoPermitidaException.class, () -> factory.paraCancelacion(contratacion, "user-otro"));
        assertThrows(AccionNoPermitidaException.class, () -> factory.paraCancelacion(contratacion, null));
    }

    @Test
    void exigeLaPoliticaPorAnticipacion() {
        assertThrows(NullPointerException.class, () -> new PoliticaReintegroFactory(null));
    }
}
