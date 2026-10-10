package com.duoc.pagos.model;

import java.math.BigDecimal;

public record CuentaSaldo(
        Integer cuentaId,
        BigDecimal saldo,
        String estado,
        String tipo
) {
}