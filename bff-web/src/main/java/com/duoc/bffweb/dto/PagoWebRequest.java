package com.duoc.bffweb.dto;

import java.math.BigDecimal;

public record PagoWebRequest(
        Integer cuentaId,
        BigDecimal monto,
        String descripcion) {
}