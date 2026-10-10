package com.duoc.clientes.model;

import java.time.LocalDateTime;

public record Cliente(
        Long clienteId,
        Integer cuentaId,
        String nombre,
        Integer edad,
        String perfil,
        String estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}