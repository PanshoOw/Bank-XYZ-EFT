package com.duoc.bffweb.dto;

import java.math.BigDecimal;

public record ActualizarCuentaWebRequest(
        String nombreCliente,
        Integer edadCliente,
        String tipoCuenta,
        BigDecimal tasaInteres
) {
}