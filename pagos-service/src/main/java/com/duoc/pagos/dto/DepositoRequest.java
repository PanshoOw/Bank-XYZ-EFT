package com.duoc.pagos.dto;

import java.math.BigDecimal;

public record DepositoRequest(
        Integer cuentaId,
        BigDecimal monto,
        String descripcion
) {
}