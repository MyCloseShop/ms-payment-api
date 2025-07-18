package com.etna.gpe.ms_payment_api.controllers;

import com.etna.gpe.ms_payment_api.entity.ShopStripeAccount;
import com.etna.gpe.ms_payment_api.services.StripeConnectService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/stripe-connect")
@RequiredArgsConstructor
@Slf4j
public class StripeConnectController {

    private final StripeConnectService stripeConnectService;

    /**
     * Créer un compte Stripe Connect pour un shop
     */
    @PostMapping("/shops/{shopId}/create-account")
    public ResponseEntity<?> createStripeAccount(@PathVariable UUID shopId) {
        try {
            ShopStripeAccount account = stripeConnectService.createStripeAccount(shopId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Compte Stripe créé avec succès",
                    "stripeAccountId", account.getStripeAccountId(),
                    "onboardingCompleted", account.getOnboardingCompleted()
            ));
        } catch (StripeException e) {
            log.error("Erreur Stripe lors de la création du compte", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Erreur Stripe: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Erreur lors de la création du compte Stripe", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Erreur interne: " + e.getMessage()
            ));
        }
    }

    /**
     * Générer un lien d'onboarding pour un shop
     */
    @PostMapping("/shops/{shopId}/onboarding-link")
    public ResponseEntity<?> createOnboardingLink(
            @PathVariable UUID shopId,
            @RequestParam String returnUrl,
            @RequestParam String refreshUrl) {
        try {
            String onboardingUrl = stripeConnectService.createOnboardingLink(shopId, returnUrl, refreshUrl);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "onboardingUrl", onboardingUrl
            ));
        } catch (StripeException e) {
            log.error("Erreur Stripe lors de la génération du lien d'onboarding", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Erreur Stripe: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Erreur lors de la génération du lien d'onboarding", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Vérifier le statut d'un compte Stripe Connect
     */
    @GetMapping("/shops/{shopId}/status")
    public ResponseEntity<?> getAccountStatus(@PathVariable UUID shopId) {
        try {
            var account = stripeConnectService.getShopStripeAccount(shopId);
            if (account.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ShopStripeAccount shopAccount = account.get();
            return ResponseEntity.ok(Map.of(
                    "shopId", shopAccount.getShopId(),
                    "stripeAccountId", shopAccount.getStripeAccountId(),
                    "onboardingCompleted", shopAccount.getOnboardingCompleted(),
                    "chargesEnabled", shopAccount.getChargesEnabled(),
                    "payoutsEnabled", shopAccount.getPayoutsEnabled(),
                    "canReceivePayments", stripeConnectService.canReceivePayments(shopId)
            ));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du statut du compte", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Erreur interne: " + e.getMessage()
            ));
        }
    }

    /**
     * Endpoint de callback après onboarding Stripe (optionnel)
     */
    @PostMapping("/onboarding-callback")
    public ResponseEntity<?> onboardingCallback(@RequestParam String accountId) {
        try {
            stripeConnectService.updateAccountStatus(accountId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Statut du compte mis à jour"
            ));
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du statut après onboarding", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
