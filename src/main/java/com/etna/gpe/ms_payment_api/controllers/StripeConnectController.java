package com.etna.gpe.ms_payment_api.controllers;

import com.etna.gpe.ms_payment_api.services.IStripeConnectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Contrôleur pour gérer l'onboarding des shops avec Stripe Connect.
 */
@RestController
@RequestMapping("/stripe/connect")
@Slf4j
public class StripeConnectController implements IStripeConnectController {

    private final IStripeConnectService stripeConnectService;
    
    @Value("${stripe.api.version}")
    private String stripeApiVersion;

    @Autowired
    public StripeConnectController(IStripeConnectService stripeConnectService) {
        this.stripeConnectService = stripeConnectService;
    }

    /**
     * Endpoint pour démarrer l'onboarding d'un shop sur Stripe.
     * @param shopId ID du shop à onboarder
     * @return URL d'onboarding Stripe
     */
    @RequestMapping(value = "/onboard/{shopId}", method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<Map<String, String>> onboardShop(@PathVariable UUID shopId) {
        log.info("Starting Stripe onboarding for shop: {}", shopId);

        String onboardingUrl = stripeConnectService.onboardShop(shopId);

        return ResponseEntity.ok(Map.of(
            "onboarding_url", onboardingUrl,
            "shop_id", shopId.toString()
        ));
    }

    /**
     * Endpoint de retour après onboarding réussi.
     */
    @GetMapping("/return")
    public ResponseEntity<Map<String, String>> handleReturn(@RequestParam(required = false) String account) {
        log.info("Shop onboarding completed for account: {}", account);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Onboarding completed"));
    }

    /**
     * Endpoint de refresh en cas d'expiration du lien d'onboarding.
     */
    @GetMapping("/reauth")
    public ResponseEntity<Map<String, String>> handleReauth(@RequestParam(required = false) String account) {
        log.info("Shop onboarding reauth required for account: {}", account);
        return ResponseEntity.ok(Map.of("status", "reauth", "message", "Please restart onboarding"));
    }
}
