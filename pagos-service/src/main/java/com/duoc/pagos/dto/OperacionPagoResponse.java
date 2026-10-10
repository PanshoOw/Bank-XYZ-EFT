package com.duoc.pagos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OperacionPagoResponse(
        String operacionId,
        String tipo,
        Integer cuentaOrigenId,
        Integer cuentaDestinoId,
        BigDecimal monto,
        BigDecimal saldoOrigen,
        BigDecimal saldoDestino,
        String estado,
        String mensaje,
        LocalDateTime fechaHora
) {
}