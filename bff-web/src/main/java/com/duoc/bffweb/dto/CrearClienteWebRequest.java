package com.duoc.bffweb.dto;

public record CrearClienteWebRequest(
        Integer cuentaId,
        String nombre,
        Integer edad,
        String perfil,
        String estado) {
}