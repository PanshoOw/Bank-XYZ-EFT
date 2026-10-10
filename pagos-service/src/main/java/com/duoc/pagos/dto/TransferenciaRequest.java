package com.duoc.pagos.dto;

import java.math.BigDecimal;

public record TransferenciaRequest(
        Integer cuentaOrigenId,
        Integer cuentaDestinoId,
        BigDecimal monto,
        String descripcion
) {
}