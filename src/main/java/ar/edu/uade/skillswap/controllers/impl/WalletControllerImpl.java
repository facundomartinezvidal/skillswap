package ar.edu.uade.skillswap.controllers.impl;

import ar.edu.uade.skillswap.application.wallet.WalletService;
import ar.edu.uade.skillswap.controllers.PaginaVista;
import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.controllers.WalletOperaciones;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.model.Models.Movimiento;

import java.util.List;
import java.util.Objects;

import static ar.edu.uade.skillswap.controllers.impl.ManejoDeErrores.ejecutar;

public class WalletControllerImpl implements UiControllers.WalletController, WalletOperaciones {

    private final WalletService walletService;

    public WalletControllerImpl(WalletService walletService) {
        this.walletService = Objects.requireNonNull(walletService);
    }

    @Override
    public int consultarSaldo(String userId) {
        return ejecutar(() -> (int) walletService.consultarSaldo(userId));
    }

    /** La UI no pagina: ve la primera página. */
    @Override
    public List<Movimiento> listarMovimientos(String userId) {
        return listarMovimientos(userId, 0, ModelosMapper.TAMANIO_UI).elementos();
    }

    @Override
    public PaginaVista<Movimiento> listarMovimientos(String userId, int pagina, int tamanio) {
        return ejecutar(() -> ModelosMapper.aVista(
                walletService.listarMovimientos(userId, new PaginaSolicitada(pagina, tamanio)), ModelosMapper::aModelo));
    }

    @Override
    public int acreditarDemo(String userId, int importe) {
        return ejecutar(() -> (int) walletService.acreditarDemo(userId, importe));
    }
}
