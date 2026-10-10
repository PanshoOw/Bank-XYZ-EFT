package com.duoc.bffweb.dto;

import java.time.LocalDateTime;

public record ClienteWebResponse(
        Long clienteId,
        Integer cuentaId,
        String nombre,
        Integer edad,
        String perfil,
        String estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion) {
}