package com.duoc.bffmobile.dto;

public record ClienteMobileResponse(
        Long clienteId,
        Integer cuentaId,
        String nombre,
        String perfil,
        String estado) {
}