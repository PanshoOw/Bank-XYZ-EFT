package com.duoc.pagos.kafka;

public class TopicNames {

    private TopicNames() {
    }

    public static final String PAGO_REALIZADO =
            "pago-realizado";

    public static final String DEPOSITO_REALIZADO =
            "deposito-realizado";

    public static final String TRANSFERENCIA_REALIZADA =
            "transferencia-realizada";
}