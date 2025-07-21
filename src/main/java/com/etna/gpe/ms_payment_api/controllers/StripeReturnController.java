package com.etna.gpe.ms_payment_api.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

/**
 * Contrôleur pour gérer la redirection Stripe après onboarding.
 * Ce contrôleur est nécessaire car Stripe redirige vers /stripe/return
 * mais notre endpoint principal est sur /stripe/connect/return.
 */
@RestController
@RequestMapping("/stripe")
@Slf4j
public class StripeReturnController implements IStripeReturnController {

    /**
     * Endpoint de retour après onboarding réussi.
     * Cet endpoint est appelé par Stripe après l'onboarding d'un compte connecté.
     * 
     * @param account ID du compte Stripe (optionnel)
     * @return Réponse de succès
     */
    @GetMapping("/return")
    public ResponseEntity<Map<String, String>> handleReturn(@RequestParam(required = false) String account) {
        log.info("Shop onboarding completed for account: {}", account);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Onboarding completed"));
    }
}