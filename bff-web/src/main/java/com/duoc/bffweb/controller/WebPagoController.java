package com.duoc.bffweb.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.bffweb.dto.DepositoWebRequest;
import com.duoc.bffweb.dto.OperacionPagoWebResponse;
import com.duoc.bffweb.dto.PagoWebRequest;
import com.duoc.bffweb.dto.TransferenciaWebRequest;
import com.duoc.bffweb.service.PagoWebService;

@RestController
@RequestMapping("/api/web")
public class WebPagoController {

    private final PagoWebService pagoWebService;

    public WebPagoController(
            PagoWebService pagoWebService) {

        this.pagoWebService = pagoWebService;
    }

    @PostMapping("/pagos")
    public ResponseEntity<OperacionPagoWebResponse> realizarPago(
            @RequestBody PagoWebRequest request) {

        return ResponseEntity.ok(
                pagoWebService.realizarPago(request));
    }

    @PostMapping("/depositos")
    public ResponseEntity<OperacionPagoWebResponse> realizarDeposito(
            @RequestBody DepositoWebRequest request) {

        return ResponseEntity.ok(
                pagoWebService.realizarDeposito(request));
    }

    @PostMapping("/transferencias")
    public ResponseEntity<OperacionPagoWebResponse> realizarTransferencia(
            @RequestBody TransferenciaWebRequest request) {

        return ResponseEntity.ok(
                pagoWebService.realizarTransferencia(request));
    }
}