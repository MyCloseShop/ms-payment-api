package com.etna.gpe.ms_payment_api.repositories;

import com.etna.gpe.ms_payment_api.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour gérer les opérations de base de données sur les paiements.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Trouve un paiement par son ID de session Stripe.
     * @param sessionId L'ID de la session Stripe
     * @return Le paiement correspondant, si trouvé
     */
    Optional<Payment> findByStripeSessionId(String sessionId);

    /**
     * Trouve un paiement par son ID de PaymentIntent Stripe.
     * @param paymentIntentId L'ID du PaymentIntent Stripe
     * @return Le paiement correspondant, si trouvé
     */
    Optional<Payment> findByStripePaymentIntentId(String paymentIntentId);

    /**
     * Trouve un paiement par son ID de charge Stripe.
     * @param chargeId L'ID de la charge Stripe
     * @return Le paiement correspondant, si trouvé
     */
    Optional<Payment> findByStripeChargeId(String chargeId);
    
    /**
     * Trouve un paiement par son ID de rendez-vous.
     * @param appointmentId L'ID du rendez-vous
     * @return Le paiement correspondant, si trouvé
     */
    Optional<Payment> findByAppointmentId(UUID appointmentId);
}
