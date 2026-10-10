package com.duoc.clientes.dto;

public record ActualizarClienteRequest(
        String nombre,
        Integer edad,
        String perfil,
        String estado
) {
}