package com.duoc.cuentas.controller;

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

import com.duoc.cuentas.dto.ActualizarCuentaRequest;
import com.duoc.cuentas.dto.CrearCuentaRequest;
import com.duoc.cuentas.dto.CuentaResponse;
import com.duoc.cuentas.dto.RetiroRequest;
import com.duoc.cuentas.dto.RetiroResponse;
import com.duoc.cuentas.service.CuentaService;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @GetMapping
    public List<CuentaResponse> obtenerTodas() {
        return cuentaService.obtenerTodas();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CuentaResponse> obtenerPorId(
            @PathVariable Integer id) {

        return cuentaService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CuentaResponse> abrir(
            @RequestBody CrearCuentaRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cuentaService.abrir(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CuentaResponse> actualizar(
            @PathVariable Integer id,
            @RequestBody ActualizarCuentaRequest request) {

        return ResponseEntity.ok(
                cuentaService.actualizar(id, request)
        );
    }

    @PatchMapping("/{id}/cerrar")
    public ResponseEntity<CuentaResponse> cerrar(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                cuentaService.cerrar(id)
        );
    }

    @PostMapping("/{id}/retiro")
    public ResponseEntity<RetiroResponse> retirar(
            @PathVariable Integer id,
            @RequestBody RetiroRequest request) {

        return ResponseEntity.ok(
                cuentaService.retirar(id, request.monto())
        );
    }
}