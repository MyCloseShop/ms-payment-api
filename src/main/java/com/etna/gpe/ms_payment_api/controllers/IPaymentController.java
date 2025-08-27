package com.etna.gpe.ms_payment_api.controllers;

import com.etna.gpe.ms_payment_api.dto.CheckoutRequestDto;
import com.etna.gpe.ms_payment_api.dto.RefundRequestDto;
import com.etna.gpe.ms_payment_api.entity.Payment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.UUID;

/**
 * Interface du contrôleur REST pour gérer les paiements Stripe Connect.
 */
public interface IPaymentController {

    /**
     * Endpoint pour créer une session de paiement Stripe Checkout.
     * @param dto Données de la requête de paiement
     * @return URL de la session Checkout
     */
    ResponseEntity<Map<String, String>> createCheckout(@RequestBody CheckoutRequestDto dto);

    /**
     * Endpoint optionnel pour initier un remboursement depuis l'application.
     * @param paymentId ID du paiement à rembourser
     * @param dto Données du remboursement
     * @return Le paiement mis à jour
     */
    ResponseEntity<Payment> refundPayment(@PathVariable UUID paymentId, @RequestBody RefundRequestDto dto);

    /**
     * Endpoint pour récupérer un paiement par son ID.
     * @param paymentId ID du paiement
     * @return Le paiement trouvé
     */
    ResponseEntity<Payment> getPayment(@PathVariable UUID paymentId);
    
    /**
     * Endpoint pour vérifier le statut de paiement d'un rendez-vous.
     * @param appointmentId ID du rendez-vous
     * @return Le paiement associé au rendez-vous ou 404 si aucun paiement n'est trouvé
     */
    ResponseEntity<Payment> getPaymentByAppointment(@PathVariable UUID appointmentId);
}