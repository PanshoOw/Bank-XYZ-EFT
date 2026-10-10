package com.duoc.pagos.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.pagos.dto.DepositoRequest;
import com.duoc.pagos.dto.OperacionPagoResponse;
import com.duoc.pagos.dto.PagoRequest;
import com.duoc.pagos.dto.TransferenciaRequest;
import com.duoc.pagos.service.PagoService;

@RestController
@RequestMapping("/api")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping("/pagos")
    public OperacionPagoResponse realizarPago(
            @RequestBody PagoRequest request) {

        return pagoService.realizarPago(request);
    }

    @PostMapping("/depositos")
    public OperacionPagoResponse realizarDeposito(
            @RequestBody DepositoRequest request) {

        return pagoService.realizarDeposito(request);
    }

    @PostMapping("/transferencias")
    public OperacionPagoResponse realizarTransferencia(
            @RequestBody TransferenciaRequest request) {

        return pagoService.realizarTransferencia(request);
    }
}