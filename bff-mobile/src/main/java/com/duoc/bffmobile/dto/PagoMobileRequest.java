package com.duoc.bffmobile.dto;

import java.math.BigDecimal;

public record PagoMobileRequest(
        Integer cuentaId,
        BigDecimal monto,
        String descripcion) {
}