package com.etna.gpe.ms_payment_api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

/**
 * DTO pour les requêtes de création de session de paiement Stripe Checkout.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequestDto {
    private UUID userId;
    private UUID shopId;
    private UUID appointmentId;
    private UUID serviceId;
    private long amount; // montant en cents
    private String currency;
}
