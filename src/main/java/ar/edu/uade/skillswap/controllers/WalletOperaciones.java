package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.model.Models.Movimiento;

/** Movimientos de la Wallet paginados, del más reciente al más viejo. */
public interface WalletOperaciones {

    PaginaVista<Movimiento> listarMovimientos(String userId, int pagina, int tamanio);
}
