package com.duoc.bffweb.dto;

import java.math.BigDecimal;

public record ActualizarCuentaBackendRequest(
        String nombre,
        Integer edad,
        String tipo,
        BigDecimal tasaInteres
) {
}