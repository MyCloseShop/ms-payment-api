package com.etna.gpe.ms_payment_api.services;

import com.etna.gpe.ms_payment_api.entity.Payment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange.payment}")
    private String paymentExchange;

    @Value("${rabbitmq.exchange.appointment}")
    private String appointmentExchange;

    @Value("${rabbitmq.routing-keys.payment-completed}")
    private String paymentCompletedRoutingKey;

    @Value("${rabbitmq.routing-keys.appointment-confirmed}")
    private String appointmentConfirmedRoutingKey;

    /**
     * Publier un événement de paiement complété
     */
    public void publishPaymentCompleted(Payment payment) {
        try {
            Map<String, Object> message = Map.of(
                    "eventType", "PAYMENT_COMPLETED",
                    "paymentId", payment.getId(),
                    "userId", payment.getUserId(),
                    "shopId", payment.getShopId(),
                    "appointmentId", payment.getAppointmentId() != null ? payment.getAppointmentId() : "",
                    "amount", payment.getAmount(),
                    "commissionAmount", payment.getCommissionAmount(),
                    "currency", payment.getCurrency(),
                    "stripePaymentIntentId", payment.getStripePaymentIntentId(),
                    "timestamp", LocalDateTime.now().toString()
            );

            String jsonMessage = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(paymentExchange, paymentCompletedRoutingKey, jsonMessage);

            log.info("Événement PAYMENT_COMPLETED publié pour le paiement: {}", payment.getId());

            // Publier aussi un événement pour confirmer le rendez-vous
            publishAppointmentConfirmed(payment);

        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la sérialisation du message de paiement complété", e);
        }
    }

    /**
     * Publier un événement de confirmation de rendez-vous
     */
    private void publishAppointmentConfirmed(Payment payment) {
        try {
            Map<String, Object> message = Map.of(
                    "eventType", "APPOINTMENT_CONFIRMED",
                    "paymentId", payment.getId(),
                    "userId", payment.getUserId(),
                    "shopId", payment.getShopId(),
                    "appointmentId", payment.getAppointmentId() != null ? payment.getAppointmentId() : "",
                    "amount", payment.getAmount(),
                    "timestamp", LocalDateTime.now().toString()
            );

            String jsonMessage = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(appointmentExchange, appointmentConfirmedRoutingKey, jsonMessage);

            log.info("Événement APPOINTMENT_CONFIRMED publié pour le paiement: {}", payment.getId());

        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la sérialisation du message de confirmation de rendez-vous", e);
        }
    }
}
