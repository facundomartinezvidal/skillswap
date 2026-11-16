package ar.edu.uade.skillswap.application.perfil;

/**
 * Puerto hacia el servicio de identidad externo. La consigna lo deja simulado: la infraestructura
 * lo implementa con un adapter mock que conoce los userId de la demo.
 */
public interface ServicioIdentidad {

    boolean existe(String userId);
}
