package ar.edu.uade.skillswap.domain.contratacion.strategy;

import ar.edu.uade.skillswap.domain.contratacion.Reintegro;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PoliticaReintegroTest {

    private static final Instant INICIO = Instant.parse("2026-11-20T10:00:00Z");
    private static final SkillCoins PRECIO = new SkillCoins(100);

    private static Reintegro reintegro(long alCliente, long alProveedor) {
        return new Reintegro(new SkillCoins(alCliente), new SkillCoins(alProveedor));
    }

    @Nested
    class PorAnticipacion {

        private final ReintegroPorAnticipacion politica = ReintegroPorAnticipacion.conUmbralesPorDefecto();

        @Test
        void losUmbralesPorDefectoSon48Y24Horas() {
            assertEquals(Duration.ofHours(48), politica.para100());
            assertEquals(Duration.ofHours(24), politica.para50());
        }

        @Test
        void conMasDe48HorasDevuelveTodoAlCliente() {
            assertEquals(reintegro(100, 0), politica.calcular(PRECIO, INICIO, INICIO.minus(Duration.ofDays(5))));
        }

        @Test
        void conExactamente48HorasDevuelveTodoAlCliente() {
            assertEquals(reintegro(100, 0), politica.calcular(PRECIO, INICIO, INICIO.minus(Duration.ofHours(48))));
        }

        @Test
        void unSegundoMenosDe48HorasDevuelveLaMitad() {
            var ahora = INICIO.minus(Duration.ofHours(48)).plusSeconds(1);

            assertEquals(reintegro(50, 50), politica.calcular(PRECIO, INICIO, ahora));
        }

        @Test
        void conExactamente24HorasDevuelveLaMitad() {
            assertEquals(reintegro(50, 50), politica.calcular(PRECIO, INICIO, INICIO.minus(Duration.ofHours(24))));
        }

        @Test
        void unSegundoMenosDe24HorasNoDevuelveNada() {
            var ahora = INICIO.minus(Duration.ofHours(24)).plusSeconds(1);

            assertEquals(reintegro(0, 100), politica.calcular(PRECIO, INICIO, ahora));
        }

        @Test
        void despuesDelInicioNoDevuelveNada() {
            assertEquals(reintegro(0, 100), politica.calcular(PRECIO, INICIO, INICIO.plusSeconds(60)));
        }

        @Test
        void conPrecioImparElClienteRecibeLaMitadHaciaAbajoYElProveedorElResto() {
            var reintegro = politica.calcular(new SkillCoins(101), INICIO, INICIO.minus(Duration.ofHours(30)));

            assertEquals(reintegro(50, 51), reintegro);
        }

        @ParameterizedTest
        @ValueSource(longs = {0, 1, 2, 3, 99, 100, 101, 1_000_001})
        void laSumaSiempreEsElPrecio(long precio) {
            for (var horas : new long[]{0, 12, 24, 36, 48, 72}) {
                var r = politica.calcular(new SkillCoins(precio), INICIO, INICIO.minus(Duration.ofHours(horas)));
                assertEquals(precio, r.alCliente().valor() + r.alProveedor().valor(), precio + " a " + horas + " h");
            }
        }

        @Test
        void losUmbralesSonConfigurables() {
            var politicaCorta = new ReintegroPorAnticipacion(Duration.ofHours(2), Duration.ofHours(1));

            assertEquals(reintegro(100, 0), politicaCorta.calcular(PRECIO, INICIO, INICIO.minus(Duration.ofHours(3))));
            assertEquals(reintegro(50, 50), politicaCorta.calcular(PRECIO, INICIO, INICIO.minus(Duration.ofMinutes(90))));
            assertEquals(reintegro(0, 100), politicaCorta.calcular(PRECIO, INICIO, INICIO.minus(Duration.ofMinutes(30))));
        }

        @Test
        void unUmbralDel50EnCeroEsValido() {
            var politicaSinCero = new ReintegroPorAnticipacion(Duration.ofHours(1), Duration.ZERO);

            assertEquals(reintegro(50, 50), politicaSinCero.calcular(PRECIO, INICIO, INICIO));
        }

        @Test
        void rechazaUmbralesInvalidos() {
            assertThrows(IllegalArgumentException.class,
                    () -> new ReintegroPorAnticipacion(Duration.ofHours(24), Duration.ofHours(24)));
            assertThrows(IllegalArgumentException.class,
                    () -> new ReintegroPorAnticipacion(Duration.ofHours(24), Duration.ofHours(48)));
            assertThrows(IllegalArgumentException.class,
                    () -> new ReintegroPorAnticipacion(Duration.ofHours(24), Duration.ofHours(-1)));
            assertThrows(NullPointerException.class, () -> new ReintegroPorAnticipacion(null, Duration.ZERO));
            assertThrows(NullPointerException.class, () -> new ReintegroPorAnticipacion(Duration.ofHours(1), null));
        }
    }

    @Nested
    class Total {

        @Test
        void siempreDevuelveTodoAlCliente() {
            var politica = new ReintegroTotal();

            assertEquals(reintegro(100, 0), politica.calcular(PRECIO, INICIO, INICIO.minus(Duration.ofDays(5))));
            assertEquals(reintegro(100, 0), politica.calcular(PRECIO, INICIO, INICIO.minus(Duration.ofHours(1))));
            assertEquals(reintegro(101, 0), politica.calcular(new SkillCoins(101), INICIO, INICIO.plusSeconds(1)));
        }
    }

    @Test
    void elReintegroCeroNoMueveNada() {
        assertEquals(reintegro(0, 0), Reintegro.cero());
    }
}
