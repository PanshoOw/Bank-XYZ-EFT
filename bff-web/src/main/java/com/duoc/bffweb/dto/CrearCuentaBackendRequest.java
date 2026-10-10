package com.duoc.bffweb.dto;

import java.math.BigDecimal;

public record CrearCuentaBackendRequest(
        Integer cuentaId,
        String nombre,
        BigDecimal saldoInicial,
        Integer edad,
        String tipo,
        BigDecimal tasaInteres
) {
}