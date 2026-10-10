package com.duoc.bffweb.dto;

public record ActualizarClienteWebRequest(
        String nombre,
        Integer edad,
        String perfil,
        String estado) {
}