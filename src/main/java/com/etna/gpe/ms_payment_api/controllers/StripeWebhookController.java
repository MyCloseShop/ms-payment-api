package com.etna.gpe.ms_payment_api.controllers;

import com.etna.gpe.ms_payment_api.services.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Charge;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur webhook pour recevoir et traiter les événements Stripe de manière sécurisée.
 */
@RestController
@RequestMapping("/stripe")
@Slf4j
public class StripeWebhookController implements IStripeWebhookController {

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final PaymentService paymentService;

    @Autowired
    public StripeWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Endpoint webhook pour recevoir les événements Stripe.
     * Vérifie la signature Stripe pour sécuriser l'endpoint.
     *
     * @param payload Corps de la requête webhook
     * @param sigHeader En-tête de signature Stripe
     * @return Réponse HTTP
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeEvent(@RequestBody String payload,
                                                    @RequestHeader("Stripe-Signature") String sigHeader) {
        log.info("Received Stripe webhook");

        Event event;
        try {
            // Vérification de la signature Stripe pour sécuriser le webhook
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe signature", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Error parsing Stripe webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }

        // Traitement des différents types d'événements Stripe
        String eventType = event.getType();
        log.info("Processing Stripe event: {}", eventType);

        try {
            switch (eventType) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;

                case "charge.refunded":
                    handleChargeRefunded(event);
                    break;

                case "payment_intent.payment_failed":
                    handlePaymentFailed(event);
                    break;

                default:
                    log.info("Unhandled Stripe event type: {}", eventType);
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing Stripe event: {}", eventType, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Processing error");
        }

        return ResponseEntity.ok("Success");
    }

    /**
     * Traite l'événement checkout.session.completed.
     * Indique que le client a complété le paiement avec succès.
     */
    private void handleCheckoutSessionCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if (session != null) {
            log.info("Processing checkout.session.completed for session: {}", session.getId());
            paymentService.handleCheckoutCompleted(session.getId());
        } else {
            log.error("Failed to deserialize checkout.session.completed event");
        }
    }

    /**
     * Traite l'événement charge.refunded.
     * Indique qu'un remboursement a été effectué sur le paiement.
     */
    private void handleChargeRefunded(Event event) {
        Charge charge = (Charge) event.getDataObjectDeserializer().getObject().orElse(null);
        if (charge != null) {
            log.info("Processing charge.refunded for charge: {}", charge.getId());
            paymentService.handlePaymentRefunded(charge.getPaymentIntent());
        } else {
            log.error("Failed to deserialize charge.refunded event");
        }
    }

    /**
     * Traite l'événement payment_intent.payment_failed.
     * Indique qu'un paiement a échoué.
     */
    private void handlePaymentFailed(Event event) {
        log.info("Payment failed event received: {}", event.getId());
        // Implementation future si nécessaire pour gérer les échecs de paiement
    }
}
