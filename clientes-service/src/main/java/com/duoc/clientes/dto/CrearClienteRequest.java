package com.duoc.clientes.dto;

public record CrearClienteRequest(
        Integer cuentaId,
        String nombre,
        Integer edad,
        String perfil,
        String estado
) {
}