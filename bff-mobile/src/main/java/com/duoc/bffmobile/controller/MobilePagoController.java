package com.duoc.bffmobile.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.bffmobile.dto.OperacionMobileResponse;
import com.duoc.bffmobile.dto.PagoMobileRequest;
import com.duoc.bffmobile.dto.TransferenciaMobileRequest;
import com.duoc.bffmobile.service.PagoMobileService;

@RestController
@RequestMapping("/api/mobile")
public class MobilePagoController {

    private final PagoMobileService pagoMobileService;

    public MobilePagoController(
            PagoMobileService pagoMobileService) {

        this.pagoMobileService = pagoMobileService;
    }

    @PostMapping("/pagos")
    public ResponseEntity<OperacionMobileResponse> realizarPago(
            @RequestBody PagoMobileRequest request) {

        return ResponseEntity.ok(
                pagoMobileService.realizarPago(request));
    }

    @PostMapping("/transferencias")
    public ResponseEntity<OperacionMobileResponse> realizarTransferencia(
            @RequestBody TransferenciaMobileRequest request) {

        return ResponseEntity.ok(
                pagoMobileService.realizarTransferencia(request));
    }
}