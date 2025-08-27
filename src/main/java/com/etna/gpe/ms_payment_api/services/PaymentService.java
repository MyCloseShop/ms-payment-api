package com.etna.gpe.ms_payment_api.services;

import com.etna.gpe.ms_payment_api.entity.Payment;

import java.util.UUID;

/**
 * Interface du service de paiement utilisant Stripe Connect.
 */
public interface PaymentService {

    /**
     * Crée une session Stripe Checkout pour un paiement avec destination vers le compte du shop.
     * @param userId ID de l'utilisateur
     * @param shopId ID du shop
     * @param appointmentId ID du rendez-vous (optionnel)
     * @param serviceId ID du service à payer
     * @param amount Montant en cents
     * @param currency Devise (ex: "eur")
     * @return URL de la session Checkout à ouvrir sur le client mobile
     */
    String createCheckoutSession(UUID userId, UUID shopId, UUID appointmentId, UUID serviceId, long amount, String currency);

    /**
     * Traite l'événement de paiement réussi (checkout.session.completed).
     * Met à jour le statut en BD et publie l'événement RabbitMQ.
     * @param sessionId ID de la session Stripe
     * @return Le Payment mis à jour
     */
    Payment handleCheckoutCompleted(String sessionId);

    /**
     * Traite un remboursement initié (ou reçu via webhook).
     * Met à jour le statut en BD et publie l'événement RabbitMQ.
     * @param paymentIntentId ID du PaymentIntent Stripe
     * @return Le Payment mis à jour
     */
    Payment handlePaymentRefunded(String paymentIntentId);

    /**
     * Initie un remboursement via l'API Stripe.
     * @param paymentId ID du paiement
     * @param amount Montant à rembourser en cents
     * @return Le Payment mis à jour
     */
    Payment refundPayment(UUID paymentId, long amount);

    /**
     * Récupère l'ID du compte Stripe connecté pour un shop donné.
     * @param shopId ID du shop
     * @return L'ID du compte Stripe connecté
     */
    String getStripeAccountIdForShop(UUID shopId);

    /**
     * Trouve un paiement par son ID.
     * @param paymentId ID du paiement
     * @return Le paiement trouvé
     */
    Payment findById(UUID paymentId);
    
    /**
     * Trouve un paiement par l'ID du rendez-vous associé.
     * @param appointmentId ID du rendez-vous
     * @return Le paiement trouvé ou null si aucun paiement n'est associé au rendez-vous
     */
    Payment findByAppointmentId(UUID appointmentId);
}
