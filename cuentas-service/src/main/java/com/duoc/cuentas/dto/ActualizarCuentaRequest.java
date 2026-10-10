package com.duoc.cuentas.dto;

import java.math.BigDecimal;

public record ActualizarCuentaRequest(
        String nombre,
        Integer edad,
        String tipo,
        BigDecimal tasaInteres
) {
}