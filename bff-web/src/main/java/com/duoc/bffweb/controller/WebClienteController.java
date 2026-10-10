package com.duoc.bffweb.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.bffweb.dto.ActualizarClienteWebRequest;
import com.duoc.bffweb.dto.ClienteWebResponse;
import com.duoc.bffweb.dto.CrearClienteWebRequest;
import com.duoc.bffweb.service.ClienteWebService;

@RestController
@RequestMapping("/api/web/clientes")
public class WebClienteController {

    private final ClienteWebService clienteWebService;

    public WebClienteController(
            ClienteWebService clienteWebService) {

        this.clienteWebService = clienteWebService;
    }

    @GetMapping
    public List<ClienteWebResponse> obtenerClientes() {
        return clienteWebService.obtenerClientes();
    }

    @GetMapping("/{id}")
    public ClienteWebResponse obtenerCliente(
            @PathVariable Long id) {

        return clienteWebService.obtenerCliente(id);
    }

    @PostMapping
    public ResponseEntity<ClienteWebResponse> crearCliente(
            @RequestBody CrearClienteWebRequest request) {

        ClienteWebResponse cliente =
                clienteWebService.crearCliente(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cliente);
    }

    @PutMapping("/{id}")
    public ClienteWebResponse actualizarCliente(
            @PathVariable Long id,
            @RequestBody ActualizarClienteWebRequest request) {

        return clienteWebService.actualizarCliente(
                id,
                request);
    }
}