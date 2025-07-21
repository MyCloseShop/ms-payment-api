package com.etna.gpe.ms_payment_api.controllers;

import com.etna.gpe.ms_payment_api.services.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Contrôleur pour gérer les redirections Stripe (success/cancel).
 * Ces endpoints sont publics car Stripe ne peut pas envoyer de token JWT.
 */
@RestController
@RequestMapping("/payment")
@Slf4j
public class StripeRedirectController implements IStripeRedirectController {

    private final PaymentService paymentService;

    @Autowired
    public StripeRedirectController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Endpoint de redirection après un paiement réussi.
     * Stripe redirige automatiquement ici après un paiement validé.
     *
     * @param sessionId ID de la session Stripe
     * @return Page de confirmation ou redirection
     */
    @GetMapping("/success")
    public ResponseEntity<Map<String, Object>> paymentSuccess(@RequestParam("session_id") String sessionId) {
        log.info("Payment success redirect received for session: {}", sessionId);

        try {
            // Marquer le paiement comme complété et publier l'événement RabbitMQ
            var completedPayment = paymentService.handleCheckoutCompleted(sessionId);

            log.info("Payment {} successfully completed", completedPayment.getId());

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Paiement effectué avec succès !",
                "paymentId", completedPayment.getId().toString(),
                "amount", completedPayment.getAmount(),
                "currency", completedPayment.getCurrency()
            ));

        } catch (Exception e) {
            log.error("Error processing payment success for session: {}", sessionId, e);

            return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", "Erreur lors du traitement du paiement",
                "sessionId", sessionId
            ));
        }
    }

    /**
     * Endpoint de redirection après un paiement annulé.
     * Stripe redirige ici si l'utilisateur annule le paiement.
     */
    @GetMapping("/cancel")
    public ResponseEntity<Map<String, Object>> paymentCancel() {
        log.info("Payment cancel redirect received");

        return ResponseEntity.ok(Map.of(
            "status", "cancelled",
            "message", "Paiement annulé par l'utilisateur"
        ));
    }
}
