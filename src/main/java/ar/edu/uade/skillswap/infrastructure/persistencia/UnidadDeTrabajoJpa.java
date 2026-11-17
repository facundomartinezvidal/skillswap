package ar.edu.uade.skillswap.infrastructure.persistencia;

import ar.edu.uade.skillswap.application.shared.UnidadDeTrabajo;
import ar.edu.uade.skillswap.application.shared.exception.ConflictoDeConcurrenciaException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import org.hibernate.StaleStateException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.LockAcquisitionException;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Transacción con JPA: {@link #ejecutar} abre un {@link EntityManager}, hace {@code begin}, corre la
 * operación y hace {@code commit}; si algo falla, {@code rollback}. El {@code EntityManager} de la
 * transacción en curso queda en un {@link ThreadLocal}: cada hilo (cada operación de la UI o de un test)
 * tiene el suyo, y los repositorios lo piden con {@link #conEntityManager}.
 * <p>
 * Fuera de una transacción, cada llamada a un repositorio abre una transacción corta propia (un
 * {@code save} suelto se confirma enseguida). Si el bloqueo optimista ({@code @Version}) detecta que
 * otra transacción cambió los mismos datos, se traduce a {@link ConflictoDeConcurrenciaException}.
 */
public class UnidadDeTrabajoJpa implements UnidadDeTrabajo {

    private final EntityManagerFactory entityManagerFactory;
    private final ThreadLocal<EntityManager> enCurso = new ThreadLocal<>();

    public UnidadDeTrabajoJpa(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = Objects.requireNonNull(entityManagerFactory);
    }

    @Override
    public <T> T ejecutar(Supplier<T> operacion) {
        if (enCurso.get() != null) {
            // Ya hay una transacción abierta en este hilo: esta operación forma parte de ella.
            return operacion.get();
        }
        var entityManager = entityManagerFactory.createEntityManager();
        var transaccion = entityManager.getTransaction();
        enCurso.set(entityManager);
        try {
            transaccion.begin();
            var resultado = operacion.get();
            transaccion.commit();
            return resultado;
        } catch (RuntimeException e) {
            if (transaccion.isActive()) {
                transaccion.rollback();
            }
            throw traducir(e);
        } finally {
            enCurso.remove();
            entityManager.close();
        }
    }

    /**
     * Lo usan los repositorios JPA: corre el trabajo con el {@code EntityManager} de la transacción en
     * curso o, si no hay una, en una transacción corta propia.
     */
    public <T> T conEntityManager(Function<EntityManager, T> trabajo) {
        var actual = enCurso.get();
        if (actual != null) {
            return trabajo.apply(actual);
        }
        return ejecutar(() -> trabajo.apply(enCurso.get()));
    }

    /**
     * Traduce los errores de la base a errores que la UI sabe mostrar:
     * <ul>
     *   <li>El bloqueo optimista o un deadlock de MySQL entre dos transacciones: Hibernate lo avisa con
     *   distintas excepciones según dónde lo detecta (en el flush, o en el commit, que además la envuelve).</li>
     *   <li>Una restricción de la base (por ejemplo, un único que impide el doble cobro o la doble
     *   calificación): el dominio ya valida esas reglas, así que si salta la restricción es porque otra
     *   operación hizo lo mismo al mismo tiempo. También es un conflicto de concurrencia.</li>
     *   <li>Un dato que la columna no admite (por ejemplo, un texto más largo que la columna):
     *   {@link IllegalArgumentException} con un mensaje para el usuario.</li>
     * </ul>
     * Las demás excepciones (reglas del dominio, errores de la aplicación) pasan tal cual.
     */
    private static RuntimeException traducir(RuntimeException e) {
        for (Throwable causa = e; causa != null; causa = causa.getCause()) {
            if (causa instanceof OptimisticLockException || causa instanceof StaleStateException
                    || causa instanceof PessimisticLockException || causa instanceof LockAcquisitionException
                    || causa instanceof ConstraintViolationException) {
                var conflicto = new ConflictoDeConcurrenciaException();
                conflicto.initCause(e);
                return conflicto;
            }
            if (causa instanceof DataException) {
                return new IllegalArgumentException(
                        "Algún dato no es válido para guardarse (por ejemplo, un texto demasiado largo)", e);
            }
        }
        return e;
    }
}
