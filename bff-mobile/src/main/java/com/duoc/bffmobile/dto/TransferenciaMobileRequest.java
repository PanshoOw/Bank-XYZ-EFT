package com.duoc.bffmobile.dto;

import java.math.BigDecimal;

public record TransferenciaMobileRequest(
        Integer cuentaOrigenId,
        Integer cuentaDestinoId,
        BigDecimal monto,
        String descripcion) {
}