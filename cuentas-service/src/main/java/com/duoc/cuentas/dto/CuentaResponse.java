package com.duoc.cuentas.dto;

import java.math.BigDecimal;

public record CuentaResponse(
        Integer cuentaId,
        String nombre,
        BigDecimal saldoInicial,
        Integer edad,
        String tipo,
        BigDecimal tasaInteres,
        BigDecimal interesCalculado,
        BigDecimal saldoFinal,
        String estado
) {
}