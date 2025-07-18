package com.etna.gpe.ms_payment_api.controllers;

import com.etna.gpe.ms_payment_api.config.StripeConfig;
import com.etna.gpe.ms_payment_api.services.PaymentService;
import com.etna.gpe.ms_payment_api.services.StripeConnectService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stripe-webhook")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final PaymentService paymentService;
    private final StripeConnectService stripeConnectService;
    private final StripeConfig stripeConfig;

    /**
     * Endpoint webhook pour recevoir les événements Stripe
     */
    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            // Vérifier la signature du webhook pour la sécurité
            event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.error("Signature webhook Stripe invalide: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Signature invalide");
        } catch (Exception e) {
            log.error("Erreur lors du parsing du webhook Stripe: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erreur de parsing");
        }

        // Traiter l'événement selon son type
        try {
            handleEvent(event);
            return ResponseEntity.ok("Événement traité avec succès");
        } catch (Exception e) {
            log.error("Erreur lors du traitement de l'événement Stripe: {} - {}", event.getType(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur de traitement");
        }
    }

    /**
     * Traiter les différents types d'événements Stripe
     */
    private void handleEvent(Event event) {
        log.info("Traitement de l'événement Stripe: {}", event.getType());

        switch (event.getType()) {
            case "checkout.session.completed":
                handleCheckoutSessionCompleted(event);
                break;

            case "payment_intent.succeeded":
                handlePaymentIntentSucceeded(event);
                break;

            case "account.updated":
                handleAccountUpdated(event);
                break;

            default:
                log.info("Événement Stripe non géré: {}", event.getType());
        }
    }

    /**
     * Gérer la finalisation d'une session Checkout
     */
    private void handleCheckoutSessionCompleted(Event event) {
        try {
            StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
            if (stripeObject instanceof Session session) {
                log.info("Session Checkout complétée: {}", session.getId());

                // Confirmer le paiement en base
                paymentService.confirmPayment(session.getId());

                log.info("Paiement confirmé avec succès pour la session: {}", session.getId());
            }
        } catch (Exception e) {
            log.error("Erreur lors du traitement de checkout.session.completed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Gérer le succès d'un PaymentIntent
     */
    private void handlePaymentIntentSucceeded(Event event) {
        try {
            StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
            if (stripeObject instanceof com.stripe.model.PaymentIntent paymentIntent) {
                log.info("PaymentIntent réussi: {}", paymentIntent.getId());
                // Logique supplémentaire si nécessaire
            }
        } catch (Exception e) {
            log.error("Erreur lors du traitement de payment_intent.succeeded: {}", e.getMessage());
        }
    }

    /**
     * Gérer la mise à jour d'un compte Connect
     */
    private void handleAccountUpdated(Event event) {
        try {
            StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
            if (stripeObject instanceof com.stripe.model.Account account) {
                log.info("Compte Connect mis à jour: {}", account.getId());

                // Mettre à jour le statut du compte en base
                stripeConnectService.updateAccountStatus(account.getId());

                log.info("Statut du compte mis à jour: {}", account.getId());
            }
        } catch (Exception e) {
            log.error("Erreur lors du traitement de account.updated: {}", e.getMessage());
        }
    }
}
