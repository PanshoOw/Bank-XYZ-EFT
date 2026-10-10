package com.duoc.cuentas.dto;

import java.math.BigDecimal;

public record RetiroResponse(
        Integer cuentaId,
        BigDecimal montoRetirado,
        BigDecimal saldoDisponible,
        String mensaje
) {
}