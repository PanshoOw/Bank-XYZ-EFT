package com.duoc.auditoria.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuditoriaEventoConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(AuditoriaEventoConsumer.class);

    @KafkaListener(
            topics = {
                    "retiro-realizado",
                    "pago-realizado",
                    "deposito-realizado",
                    "transferencia-realizada"
            },
            groupId = "auditoria-service",
            concurrency = "3"
    )
    public void procesarEvento(ConsumerRecord<String, String> consumerRecord) {

        log.info(
                "AUDITORIA MICROSERVICIO | topic={} | key={} | particion={} | offset={} | evento={}",
                consumerRecord.topic(),
                consumerRecord.key(),
                consumerRecord.partition(),
                consumerRecord.offset(),
                consumerRecord.value()
        );
    }
}