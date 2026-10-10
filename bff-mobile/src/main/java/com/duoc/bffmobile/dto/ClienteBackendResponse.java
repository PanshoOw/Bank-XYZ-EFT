package com.duoc.bffmobile.dto;

import java.time.LocalDateTime;

public record ClienteBackendResponse(
        Long clienteId,
        Integer cuentaId,
        String nombre,
        Integer edad,
        String perfil,
        String estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion) {
}