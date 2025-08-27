package com.etna.gpe.ms_payment_api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Interface du contrôleur pour gérer les redirections Stripe (success/cancel).
 */
public interface IStripeRedirectController {

    /**
     * Endpoint de redirection après un paiement réussi.
     * Stripe redirige automatiquement ici après un paiement validé.
     *
     * @param sessionId ID de la session Stripe
     * @return Page de confirmation ou redirection
     */
    ResponseEntity<Map<String, Object>> paymentSuccess(@RequestParam("session_id") String sessionId);

    /**
     * Endpoint de redirection après un paiement annulé.
     * Stripe redirige ici si l'utilisateur annule le paiement.
     */
    ResponseEntity<Map<String, Object>> paymentCancel();
}