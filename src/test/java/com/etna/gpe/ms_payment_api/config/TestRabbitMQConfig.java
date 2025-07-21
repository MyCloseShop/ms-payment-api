package com.etna.gpe.ms_payment_api.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

/**
 * Configuration de test pour mocker RabbitMQ et éviter les dépendances ConnectionFactory.
 */
@TestConfiguration
@Profile("test")
public class TestRabbitMQConfig {

    /**
     * Mock du RabbitTemplate pour les tests.
     * @Primary pour remplacer le bean RabbitTemplate de RabbitMQPaymentConfig
     */
    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate() {
        return mock(RabbitTemplate.class);
    }
}
