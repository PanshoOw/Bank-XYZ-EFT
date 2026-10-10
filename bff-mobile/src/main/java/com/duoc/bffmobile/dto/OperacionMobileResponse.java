package com.duoc.bffmobile.dto;

import java.math.BigDecimal;

public record OperacionMobileResponse(
        String operacionId,
        String tipo,
        BigDecimal monto,
        BigDecimal saldoOrigen,
        BigDecimal saldoDestino,
        String estado,
        String mensaje) {
}