package com.duoc.cuentas.dto;

import java.math.BigDecimal;

public record CrearCuentaRequest(
        Integer cuentaId,
        String nombre,
        BigDecimal saldoInicial,
        Integer edad,
        String tipo,
        BigDecimal tasaInteres
) {
}