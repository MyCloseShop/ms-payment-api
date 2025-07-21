package com.etna.gpe.ms_payment_api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Interface du contrôleur pour gérer la redirection Stripe après expiration du lien d'onboarding.
 */
public interface IStripeReauthController {

    /**
     * Endpoint de refresh en cas d'expiration du lien d'onboarding.
     * Cet endpoint est appelé par Stripe lorsque le lien d'onboarding a expiré.
     * 
     * @param account ID du compte Stripe (optionnel)
     * @return Réponse de reauth
     */
    ResponseEntity<Map<String, String>> handleReauth(@RequestParam(required = false) String account);
}