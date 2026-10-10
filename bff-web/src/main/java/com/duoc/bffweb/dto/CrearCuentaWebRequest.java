package com.duoc.bffweb.dto;

import java.math.BigDecimal;

public record CrearCuentaWebRequest(
        Integer cuentaId,
        String nombreCliente,
        BigDecimal saldoInicial,
        Integer edadCliente,
        String tipoCuenta,
        BigDecimal tasaInteres
) {
}