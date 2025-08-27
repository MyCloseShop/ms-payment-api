package com.etna.gpe.ms_payment_api.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Contrôleur pour gérer la redirection Stripe après expiration du lien d'onboarding.
 * Ce contrôleur est nécessaire car Stripe redirige vers /stripe/reauth
 * mais notre endpoint principal est sur /stripe/connect/reauth.
 */
@RestController
@RequestMapping("/stripe")
@Slf4j
public class StripeReauthController implements IStripeReauthController {

    /**
     * Endpoint de refresh en cas d'expiration du lien d'onboarding.
     * Cet endpoint est appelé par Stripe lorsque le lien d'onboarding a expiré.
     * 
     * @param account ID du compte Stripe (optionnel)
     * @return Réponse de reauth
     */
    @GetMapping("/reauth")
    public ResponseEntity<Map<String, String>> handleReauth(@RequestParam(required = false) String account) {
        log.info("Shop onboarding reauth required for account: {}", account);
        return ResponseEntity.ok(Map.of("status", "reauth", "message", "Please restart onboarding"));
    }
}