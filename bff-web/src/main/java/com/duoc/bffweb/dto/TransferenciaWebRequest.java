package com.duoc.bffweb.dto;

import java.math.BigDecimal;

public record TransferenciaWebRequest(
        Integer cuentaOrigenId,
        Integer cuentaDestinoId,
        BigDecimal monto,
        String descripcion) {
}