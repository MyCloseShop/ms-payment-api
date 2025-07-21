package com.etna.gpe.ms_payment_api.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration RabbitMQ complète pour les événements de paiement Stripe.
 * Désactivée pendant les tests via @Profile.
 */
@Configuration
@Profile("!test")
@ConditionalOnProperty(name = "spring.rabbitmq.host", matchIfMissing = true)
public class RabbitMQPaymentConfig {

    @Value("${rabbitmq.exchange.payment}")
    private String paymentExchange;

    @Value("${rabbitmq.exchange.appointment}")
    private String appointmentExchange;

    @Value("${rabbitmq.queue.payment-completed}")
    private String paymentCompletedQueue;

    @Value("${rabbitmq.queue.payment-refunded}")
    private String paymentRefundedQueue;

    @Value("${rabbitmq.queue.appointment-confirmed}")
    private String appointmentConfirmedQueue;

    @Value("${rabbitmq.queue.appointment-cancelled}")
    private String appointmentCancelledQueue;

    @Value("${rabbitmq.routing-keys.payment-completed}")
    private String paymentCompletedRoutingKey;

    @Value("${rabbitmq.routing-keys.payment-refunded}")
    private String paymentRefundedRoutingKey;

    @Value("${rabbitmq.routing-keys.appointment-confirmed}")
    private String appointmentConfirmedRoutingKey;

    @Value("${rabbitmq.routing-keys.appointment-cancelled}")
    private String appointmentCancelledRoutingKey;

    // Exchanges
    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(paymentExchange);
    }

    @Bean
    public DirectExchange appointmentExchange() {
        return new DirectExchange(appointmentExchange);
    }

    // Queues
    @Bean
    public Queue paymentCompletedQueue() {
        return QueueBuilder.durable(paymentCompletedQueue).build();
    }

    @Bean
    public Queue paymentRefundedQueue() {
        return QueueBuilder.durable(paymentRefundedQueue).build();
    }

    @Bean
    public Queue appointmentConfirmedQueue() {
        return QueueBuilder.durable(appointmentConfirmedQueue).build();
    }

    @Bean
    public Queue appointmentCancelledQueue() {
        return QueueBuilder.durable(appointmentCancelledQueue).build();
    }

    // Bindings
    @Bean
    public Binding paymentCompletedBinding() {
        return BindingBuilder.bind(paymentCompletedQueue())
                .to(paymentExchange())
                .with(paymentCompletedRoutingKey);
    }

    @Bean
    public Binding paymentRefundedBinding() {
        return BindingBuilder.bind(paymentRefundedQueue())
                .to(paymentExchange())
                .with(paymentRefundedRoutingKey);
    }

    @Bean
    public Binding appointmentConfirmedBinding() {
        return BindingBuilder.bind(appointmentConfirmedQueue())
                .to(appointmentExchange())
                .with(appointmentConfirmedRoutingKey);
    }

    @Bean
    public Binding appointmentCancelledBinding() {
        return BindingBuilder.bind(appointmentCancelledQueue())
                .to(appointmentExchange())
                .with(appointmentCancelledRoutingKey);
    }

    /**
     * Configuration du RabbitTemplate avec convertisseur JSON pour Stripe.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
}
