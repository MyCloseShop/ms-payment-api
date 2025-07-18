package com.etna.gpe.ms_payment_api.services;

import com.etna.gpe.ms_payment_api.entity.Payment;
import com.etna.gpe.ms_payment_api.enums.PaymentStatus;
import com.etna.gpe.ms_payment_api.repositories.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Test
    void shouldFindPaymentByStripeCheckoutSessionId() {
        // Given
        String sessionId = "cs_test_123";
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .stripeCheckoutSessionId(sessionId)
                .status(PaymentStatus.PENDING)
                .userId(UUID.randomUUID())
                .shopId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(50.00))
                .build();

        when(paymentRepository.findByStripeCheckoutSessionId(sessionId))
                .thenReturn(Optional.of(payment));

        // When
        Optional<Payment> result = paymentRepository.findByStripeCheckoutSessionId(sessionId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(sessionId, result.get().getStripeCheckoutSessionId());
        verify(paymentRepository).findByStripeCheckoutSessionId(sessionId);
    }

    @Test
    void shouldReturnEmptyWhenPaymentNotFound() {
        // Given
        String sessionId = "cs_test_nonexistent";
        when(paymentRepository.findByStripeCheckoutSessionId(sessionId))
                .thenReturn(Optional.empty());

        // When
        Optional<Payment> result = paymentRepository.findByStripeCheckoutSessionId(sessionId);

        // Then
        assertFalse(result.isPresent());
        verify(paymentRepository).findByStripeCheckoutSessionId(sessionId);
    }

    @Test
    void shouldCreatePaymentWithCorrectProperties() {
        // Given
        Payment payment = Payment.builder()
                .stripePaymentIntentId("pi_test_123")
                .stripeCheckoutSessionId("cs_test_123")
                .userId(UUID.randomUUID())
                .shopId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(50.00))
                .commissionAmount(BigDecimal.valueOf(5.00))
                .commissionRate(BigDecimal.valueOf(10.0))
                .currency("EUR")
                .status(PaymentStatus.PENDING)
                .description("Test payment")
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // When
        Payment savedPayment = paymentRepository.save(payment);

        // Then
        assertNotNull(savedPayment);
        assertEquals("pi_test_123", savedPayment.getStripePaymentIntentId());
        assertEquals(PaymentStatus.PENDING, savedPayment.getStatus());
        assertEquals(BigDecimal.valueOf(50.00), savedPayment.getAmount());
        assertEquals("EUR", savedPayment.getCurrency());
        verify(paymentRepository).save(payment);
    }
}
