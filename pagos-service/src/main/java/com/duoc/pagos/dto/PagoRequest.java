package com.duoc.pagos.dto;

import java.math.BigDecimal;

public record PagoRequest(
        Integer cuentaId,
        BigDecimal monto,
        String descripcion
) {
}