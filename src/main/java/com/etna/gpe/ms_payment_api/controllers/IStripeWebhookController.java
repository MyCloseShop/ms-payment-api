package com.etna.gpe.ms_payment_api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Interface du contrôleur webhook pour recevoir et traiter les événements Stripe de manière sécurisée.
 */
public interface IStripeWebhookController {

    /**
     * Endpoint webhook pour recevoir les événements Stripe.
     * Vérifie la signature Stripe pour sécuriser l'endpoint.
     *
     * @param payload Corps de la requête webhook
     * @param sigHeader En-tête de signature Stripe
     * @return Réponse HTTP
     */
    ResponseEntity<String> handleStripeEvent(@RequestBody String payload,
                                           @RequestHeader("Stripe-Signature") String sigHeader);
}