package com.etna.gpe.ms_payment_api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.UUID;

/**
 * Interface du contrôleur pour gérer l'onboarding des shops avec Stripe Connect.
 */
public interface IStripeConnectController {

    /**
     * Endpoint pour démarrer l'onboarding d'un shop sur Stripe.
     * @param shopId ID du shop à onboarder
     * @return URL d'onboarding Stripe
     */
    ResponseEntity<Map<String, String>> onboardShop(@PathVariable UUID shopId);

    /**
     * Endpoint de retour après onboarding réussi.
     * @param account ID du compte Stripe (optionnel)
     * @return Réponse de succès
     */
    ResponseEntity<Map<String, String>> handleReturn(@RequestParam(required = false) String account);

    /**
     * Endpoint de refresh en cas d'expiration du lien d'onboarding.
     * @param account ID du compte Stripe (optionnel)
     * @return Réponse de reauth
     */
    ResponseEntity<Map<String, String>> handleReauth(@RequestParam(required = false) String account);
}