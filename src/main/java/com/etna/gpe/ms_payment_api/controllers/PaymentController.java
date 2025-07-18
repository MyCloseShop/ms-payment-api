package com.etna.gpe.ms_payment_api.controllers;

import com.etna.gpe.ms_payment_api.dto.CreatePaymentRequest;
import com.etna.gpe.ms_payment_api.dto.PaymentResponse;
import com.etna.gpe.ms_payment_api.services.PaymentService;
import com.etna.gpe.mycloseshop.security_api.entity.JwtUserDetails;
import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Créer un paiement avec Stripe Checkout
     */
    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            Authentication authentication) {
        try {
            // Récupérer l'ID utilisateur depuis le contexte JWT
            UUID userId = getUserIdFromAuthentication(authentication);


            PaymentResponse response = paymentService.createPayment(request, userId);
            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            log.error("Erreur Stripe lors de la création du paiement", e);
            return ResponseEntity.badRequest().body(
                    PaymentResponse.builder()
                            .status("ERROR")
                            .message("Erreur lors de la création du paiement: " + e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Erreur lors de la création du paiement", e);
            return ResponseEntity.internalServerError().body(
                    PaymentResponse.builder()
                            .status("ERROR")
                            .message("Erreur interne: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Vérifier l'éligibilité au remboursement
     */
    @GetMapping("/{paymentId}/refund-eligible")
    public ResponseEntity<Boolean> isRefundEligible(@PathVariable UUID paymentId) {
        try {
            boolean eligible = paymentService.isRefundEligible(paymentId);
            return ResponseEntity.ok(eligible);
        } catch (Exception e) {
            log.error("Erreur lors de la vérification d'éligibilité au remboursement", e);
            return ResponseEntity.badRequest().body(false);
        }
    }

    private UUID getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Utilisateur non authentifié");
        }

        JwtUserDetails userDetails = (JwtUserDetails) authentication.getPrincipal();
        if (userDetails == null || userDetails.userId() == null) {
            throw new IllegalArgumentException("ID utilisateur introuvable dans le contexte d'authentification");
        }

        return UUID.fromString(userDetails.userId());
    }
}
