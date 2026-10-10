package com.duoc.bffweb.dto;

import java.math.BigDecimal;

public record DepositoWebRequest(
        Integer cuentaId,
        BigDecimal monto,
        String descripcion) {
}