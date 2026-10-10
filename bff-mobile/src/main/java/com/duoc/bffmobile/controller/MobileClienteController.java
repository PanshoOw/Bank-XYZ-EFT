package com.duoc.bffmobile.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.bffmobile.dto.ClienteMobileResponse;
import com.duoc.bffmobile.service.ClienteMobileService;

@RestController
@RequestMapping("/api/mobile/clientes")
public class MobileClienteController {

    private final ClienteMobileService clienteMobileService;

    public MobileClienteController(
            ClienteMobileService clienteMobileService) {

        this.clienteMobileService = clienteMobileService;
    }

    @GetMapping("/{id}")
    public ClienteMobileResponse obtenerCliente(
            @PathVariable Long id) {

        return clienteMobileService.obtenerCliente(id);
    }
}