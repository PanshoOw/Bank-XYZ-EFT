package com.duoc.pagos.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import com.duoc.pagos.kafka.TopicNames;

@Configuration
public class KafkaTopicsConfiguration {

    private NewTopic topic(String name) {
        return TopicBuilder.name(name)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic pagoRealizado() {
        return topic(TopicNames.PAGO_REALIZADO);
    }

    @Bean
    NewTopic depositoRealizado() {
        return topic(TopicNames.DEPOSITO_REALIZADO);
    }

    @Bean
    NewTopic transferenciaRealizada() {
        return topic(TopicNames.TRANSFERENCIA_REALIZADA);
    }
}