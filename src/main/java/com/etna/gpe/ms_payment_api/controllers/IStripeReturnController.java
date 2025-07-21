package com.etna.gpe.ms_payment_api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Interface du contrôleur pour gérer la redirection Stripe après onboarding.
 */
public interface IStripeReturnController {

    /**
     * Endpoint de retour après onboarding réussi.
     * Cet endpoint est appelé par Stripe après l'onboarding d'un compte connecté.
     * 
     * @param account ID du compte Stripe (optionnel)
     * @return Réponse de succès
     */
    ResponseEntity<Map<String, String>> handleReturn(@RequestParam(required = false) String account);
}