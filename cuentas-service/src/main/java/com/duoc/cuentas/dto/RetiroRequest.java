package com.duoc.cuentas.dto;

import java.math.BigDecimal;

public record RetiroRequest(
        BigDecimal monto
) {
}