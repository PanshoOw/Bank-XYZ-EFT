package com.duoc.pagos.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OperacionRealizadaEvent(
        String operacionId,
        String tipo,
        Integer cuentaOrigenId,
        Integer cuentaDestinoId,
        BigDecimal monto,
        BigDecimal saldoOrigen,
        BigDecimal saldoDestino,
        LocalDateTime fechaHora
) {
}