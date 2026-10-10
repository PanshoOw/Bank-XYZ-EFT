package com.duoc.pagos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OperacionPago(
        String operacionId,
        String tipo,
        Integer cuentaOrigenId,
        Integer cuentaDestinoId,
        BigDecimal monto,
        String estado,
        String descripcion,
        LocalDateTime fechaHora
) {
}