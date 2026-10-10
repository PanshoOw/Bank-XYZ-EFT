package com.duoc.clientes.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.clientes.dto.ActualizarClienteRequest;
import com.duoc.clientes.dto.CrearClienteRequest;
import com.duoc.clientes.model.Cliente;
import com.duoc.clientes.service.ClienteService;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public List<Cliente> listar() {
        return clienteService.listar();
    }

    @GetMapping("/{clienteId}")
    public Cliente obtener(
            @PathVariable Long clienteId) {

        return clienteService.obtenerPorId(clienteId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Cliente crear(
            @RequestBody CrearClienteRequest request) {

        return clienteService.crear(request);
    }

    @PutMapping("/{clienteId}")
    public Cliente actualizar(
            @PathVariable Long clienteId,
            @RequestBody ActualizarClienteRequest request) {

        return clienteService.actualizar(
                clienteId,
                request
        );
    }
}