package com.duoc.pagos.kafka.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.duoc.pagos.kafka.TopicNames;
import com.duoc.pagos.kafka.event.OperacionRealizadaEvent;

@Service
public class OperacionEventProducer {

    private static final Logger log =
            LoggerFactory.getLogger(OperacionEventProducer.class);

    private final KafkaTemplate<String, OperacionRealizadaEvent> kafkaTemplate;

    public OperacionEventProducer(
            KafkaTemplate<String, OperacionRealizadaEvent> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicar(OperacionRealizadaEvent evento) {

        String topic = switch (evento.tipo()) {
            case "PAGO" -> TopicNames.PAGO_REALIZADO;
            case "DEPOSITO" -> TopicNames.DEPOSITO_REALIZADO;
            case "TRANSFERENCIA" -> TopicNames.TRANSFERENCIA_REALIZADA;
            default -> throw new IllegalArgumentException(
                    "Tipo de operación no soportado: " + evento.tipo());
        };

        String key = evento.operacionId();

        kafkaTemplate
                .send(topic, key, evento)
                .whenComplete((resultado, error) -> {

                    if (error != null) {
                        log.error(
                                "Error al publicar evento {}. operacionId={}",
                                topic,
                                evento.operacionId(),
                                error);
                        return;
                    }

                    log.info(
                            "Evento {} publicado. operacionId={}, particion={}, offset={}",
                            topic,
                            evento.operacionId(),
                            resultado.getRecordMetadata().partition(),
                            resultado.getRecordMetadata().offset());
                });
    }
}