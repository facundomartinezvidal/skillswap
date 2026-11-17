package ar.edu.uade.skillswap.infrastructure.persistencia;

import ar.edu.uade.skillswap.application.shared.exception.ConflictoDeConcurrenciaException;
import ar.edu.uade.skillswap.domain.contratacion.Contratacion;
import ar.edu.uade.skillswap.domain.contratacion.Solicitud;
import ar.edu.uade.skillswap.domain.publicacion.Modalidad;
import ar.edu.uade.skillswap.domain.shared.Puntaje;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.wallet.Wallet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Las restricciones de unicidad del esquema son la última defensa: aunque dos operaciones pasen las
 * validaciones del dominio al mismo tiempo (cada una con su copia del agregado), la base rechaza el
 * doble cobro y la calificación duplicada, y la transacción no guarda nada.
 */
class RestriccionesJpaTest extends BaseDeDatosDePrueba {

    private static final String ANA = "user-ana";
    private static final String MATEO = "user-mateo";

    private Contratacion contratacion;

    @BeforeEach
    void unaContratacionCompletada() {
        guardarPerfil(ANA);
        guardarPerfil(MATEO);
        var publicacion = guardarPublicada(ANA, "Java", "Programación", Modalidad.REMOTA, 100, Set.of(), AHORA);
        var franja = publicacion.franjas().get(0);
        contratacion = Contratacion.solicitar(MATEO, ANA, publicacion.id(), new Solicitud(null, "Aprender", franja.id()),
                franja.inicio(), new SkillCoins(100), AHORA);
        contrataciones.save(contratacion);
        wallets.save(Wallet.abrir(MATEO, new SkillCoins(1000), AHORA));
    }

    @Test
    void laBaseRechazaUnSegundoDebitoDelMismoClienteParaLaMismaContratacion() throws SQLException {
        var primerPago = wallets.findByTitular(MATEO).orElseThrow();
        var segundoPago = wallets.findByTitular(MATEO).orElseThrow();
        primerPago.debitar(new SkillCoins(100), "Pago", contratacion.id(), AHORA);
        segundoPago.debitar(new SkillCoins(100), "Pago", contratacion.id(), AHORA);
        wallets.save(primerPago);

        assertThrows(ConflictoDeConcurrenciaException.class, () -> unidadDeTrabajo.ejecutar(() -> {
            wallets.save(segundoPago);
            return null;
        }));

        assertEquals(1, contarFilas("select count(*) from movimientos_wallet where tipo = 'DEBITO'"));
        assertEquals(new SkillCoins(900), wallets.findByTitular(MATEO).orElseThrow().saldo());
    }

    @Test
    void laBaseRechazaUnaSegundaCalificacionDelMismoAutor() throws SQLException {
        contratacion.aceptar(ANA, AHORA);
        contratacion.pagar(MATEO, AHORA);
        contratacion.iniciar(ANA, AHORA);
        contratacion.completar(ANA, AHORA);
        contrataciones.save(contratacion);
        var unaCopia = contrataciones.findById(contratacion.id()).orElseThrow();
        var otraCopia = contrataciones.findById(contratacion.id()).orElseThrow();
        unaCopia.calificar(MATEO, new Puntaje(5), "Excelente", AHORA);
        otraCopia.calificar(MATEO, new Puntaje(1), "Malísima", AHORA);
        contrataciones.save(unaCopia);

        assertThrows(ConflictoDeConcurrenciaException.class, () -> contrataciones.save(otraCopia));

        assertEquals(1, contarFilas("select count(*) from calificaciones"));
        assertEquals(5, contrataciones.findById(contratacion.id()).orElseThrow().calificaciones().get(0).puntaje().valor());
    }
}
