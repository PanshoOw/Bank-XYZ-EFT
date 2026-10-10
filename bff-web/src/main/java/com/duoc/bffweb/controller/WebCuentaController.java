package com.duoc.bffweb.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.bffweb.dto.ActualizarCuentaWebRequest;
import com.duoc.bffweb.dto.CrearCuentaWebRequest;
import com.duoc.bffweb.dto.CuentaWebResponse;
import com.duoc.bffweb.service.CuentaWebService;

@RestController
@RequestMapping("/api/web/cuentas")
public class WebCuentaController {

    private final CuentaWebService cuentaWebService;

    public WebCuentaController(
            CuentaWebService cuentaWebService) {

        this.cuentaWebService = cuentaWebService;
    }

    @GetMapping
    public List<CuentaWebResponse> obtenerCuentas() {
        return cuentaWebService.obtenerCuentas();
    }

    @GetMapping("/{id}")
    public CuentaWebResponse obtenerCuenta(
            @PathVariable Integer id) {

        return cuentaWebService.obtenerCuenta(id);
    }

    @PostMapping
    public ResponseEntity<CuentaWebResponse> abrirCuenta(
            @RequestBody CrearCuentaWebRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cuentaWebService.abrirCuenta(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CuentaWebResponse> actualizarCuenta(
            @PathVariable Integer id,
            @RequestBody ActualizarCuentaWebRequest request) {

        return ResponseEntity.ok(
                cuentaWebService.actualizarCuenta(id, request)
        );
    }

    @PatchMapping("/{id}/cerrar")
    public ResponseEntity<CuentaWebResponse> cerrarCuenta(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                cuentaWebService.cerrarCuenta(id)
        );
    }
}