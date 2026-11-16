package ar.edu.uade.skillswap.controllers.impl;

import ar.edu.uade.skillswap.application.wallet.WalletService;
import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models.Movimiento;

import java.util.List;
import java.util.Objects;

import static ar.edu.uade.skillswap.controllers.impl.ManejoDeErrores.ejecutar;

public class WalletControllerImpl implements UiControllers.WalletController {

    private final WalletService walletService;

    public WalletControllerImpl(WalletService walletService) {
        this.walletService = Objects.requireNonNull(walletService);
    }

    @Override
    public int consultarSaldo(String userId) {
        return ejecutar(() -> (int) walletService.consultarSaldo(userId));
    }

    @Override
    public List<Movimiento> listarMovimientos(String userId) {
        return ejecutar(() -> walletService.listarMovimientos(userId, ModelosMapper.PRIMERA_PAGINA)
                .elementos().stream()
                .map(ModelosMapper::aModelo)
                .toList());
    }

    @Override
    public int acreditarDemo(String userId, int importe) {
        return ejecutar(() -> (int) walletService.acreditarDemo(userId, importe));
    }
}
